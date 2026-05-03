package com.blackkriger.equivalentincrease;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import com.mordenkainen.equivalentenergistics.blocks.crafter.tiles.TileEMCCrafter;
import com.mordenkainen.equivalentenergistics.blocks.crafter.tiles.TileEMCCrafterAdv;
import com.mordenkainen.equivalentenergistics.blocks.crafter.tiles.TileEMCCrafterBase;
import com.mordenkainen.equivalentenergistics.blocks.crafter.tiles.TileEMCCrafterExt;
import com.mordenkainen.equivalentenergistics.blocks.crafter.tiles.TileEMCCrafterUlt;
import com.mordenkainen.equivalentenergistics.integration.ae2.cache.storage.IEMCStorageGrid;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public final class EmcRouter {

    public static final String REDIRECT_KEY = "EI_RedirectMode";
    public static final String OWNER_UUID_KEY = "OwnerUUID";

    private static final String LOG = "[EquivalentIncrease] ";
    private static final Class<?>[] CRAFTER_CLASSES = new Class<?>[]{
            TileEMCCrafter.class,
            TileEMCCrafterAdv.class,
            TileEMCCrafterExt.class,
            TileEMCCrafterUlt.class
    };
    private static long routeCalls = 0L;
    private static long lastTraceMs = 0L;

    private EmcRouter() {}

    public static boolean tryToggleRedirect(ItemStack stack, World world, EntityPlayer player) {
        if (stack == null || player == null) return false;
        if (player.isSneaking()) return false;
        if (!stack.hasTagCompound()) return false;
        NBTTagCompound nbt = stack.getTagCompound();
        if (!nbt.hasKey(OWNER_UUID_KEY)) return false;
        String owner = nbt.getString(OWNER_UUID_KEY);
        if (owner == null || owner.isEmpty()) return false;
        if (!owner.equals(player.getUniqueID().toString())) return false;

        if (world == null || world.isRemote) return true;

        boolean newMode = !nbt.getBoolean(REDIRECT_KEY);
        nbt.setBoolean(REDIRECT_KEY, newMode);
        System.out.println(LOG + "tome toggled: owner=" + owner + " mode=" + (newMode ? "Balance" : "Storage")
                + " stack.id=" + System.identityHashCode(stack));
        player.addChatMessage(new ChatComponentText(
                "EMC routing switched to "
                        + (newMode
                            ? EnumChatFormatting.GREEN + "Balance"
                            : EnumChatFormatting.YELLOW + "Storage")));
        return true;
    }

    public static double routeOrStore(IEMCStorageGrid grid, double amount, Actionable mode) {
        boolean trace = shouldTrace();
        routeCalls++;
        if (trace) {
            System.out.println(LOG + "routeOrStore #" + routeCalls + " amount=" + amount + " mode=" + mode
                    + " grid=" + (grid == null ? "null" : grid.getClass().getSimpleName()));
        }
        if (mode == Actionable.MODULATE && amount > 0.0D) {
            UUID target = findRedirectTarget(grid, trace);
            if (trace) {
                System.out.println(LOG + "  → target=" + target);
            }
            if (target != null && applyEmcToPlayer(target, amount, trace)) {
                if (trace) System.out.println(LOG + "  → routed " + amount + " to player " + target);
                return amount;
            }
        }
        if (trace) System.out.println(LOG + "  → falling back to grid.addEMC");
        return grid.addEMC(amount, mode);
    }

    private static boolean shouldTrace() {
        long now = System.currentTimeMillis();
        if (now - lastTraceMs >= 2000L) {
            lastTraceMs = now;
            return true;
        }
        return false;
    }

    private static UUID findRedirectTarget(IEMCStorageGrid grid, boolean trace) {
        try {
            if (grid == null) {
                if (trace) System.out.println(LOG + "  findRedirectTarget: grid is null");
                return null;
            }
            IGrid g = grid.getGrid();
            if (g == null) {
                if (trace) System.out.println(LOG + "  findRedirectTarget: grid.getGrid() is null");
                return null;
            }
            int seen = 0, withTome = 0, redirected = 0;
            for (Class<?> cls : CRAFTER_CLASSES) {
                @SuppressWarnings("unchecked")
                IMachineSet crafters = g.getMachines((Class) cls);
                if (crafters == null) continue;
                for (IGridNode node : crafters) {
                    if (node == null) continue;
                    seen++;
                    IGridHost host = node.getMachine();
                    if (!(host instanceof TileEMCCrafterBase)) continue;
                    ItemStack tome = ((TileEMCCrafterBase) host).getCurrentTome();
                    if (tome == null) continue;
                    if (!tome.hasTagCompound()) continue;
                    withTome++;
                    NBTTagCompound nbt = tome.getTagCompound();
                    boolean redirect = nbt.getBoolean(REDIRECT_KEY);
                    if (trace) {
                        System.out.println(LOG + "  " + cls.getSimpleName() + "#" + seen
                                + " tome nbt: redirect=" + redirect
                                + " hasOwner=" + nbt.hasKey(OWNER_UUID_KEY)
                                + " owner=" + (nbt.hasKey(OWNER_UUID_KEY) ? nbt.getString(OWNER_UUID_KEY) : "<none>"));
                    }
                    if (!redirect) continue;
                    redirected++;
                    if (!nbt.hasKey(OWNER_UUID_KEY)) continue;
                    String owner = nbt.getString(OWNER_UUID_KEY);
                    if (owner == null || owner.isEmpty()) continue;
                    try {
                        return UUID.fromString(owner);
                    } catch (IllegalArgumentException ex) {
                        if (trace) System.out.println(LOG + "  bad UUID on tome: " + owner);
                        return null;
                    }
                }
            }
            if (trace) {
                System.out.println(LOG + "  findRedirectTarget: scanned crafters=" + seen
                        + " withTome=" + withTome + " redirected=" + redirected + " — no target");
            }
        } catch (Throwable t) {
            System.err.println(LOG + "findRedirectTarget threw: " + t);
            t.printStackTrace();
        }
        return null;
    }

    private static boolean applyEmcToPlayer(UUID uuid, double amount, boolean trace) {
        if (uuid == null || amount <= 0.0D) return false;
        try {
            EntityPlayerMP player = findOnlinePlayer(uuid);
            if (player != null && player.openContainer instanceof TransmutationContainer) {
                TransmutationContainer tc = (TransmutationContainer) player.openContainer;
                EmcSyncBypass.fromCondenser = true;
                try {
                    tc.transmutationInventory.addEmc(amount);
                } finally {
                    EmcSyncBypass.fromCondenser = false;
                }
                if (trace) System.out.println(LOG + "  applyEmcToPlayer (GUI open) ok: " + uuid
                        + " inv.emc → " + tc.transmutationInventory.emc);
                return true;
            }
            double cur = ProjectEAPI.getTransmutationProxy().getEMC(uuid);
            ProjectEAPI.getTransmutationProxy().setEMC(uuid, cur + amount);
            if (trace) System.out.println(LOG + "  applyEmcToPlayer ok: " + uuid + " " + cur + " → " + (cur + amount));
            return true;
        } catch (Throwable t) {
            System.err.println(LOG + "applyEmcToPlayer threw for " + uuid + ": " + t);
            t.printStackTrace();
            return false;
        }
    }

    private static EntityPlayerMP findOnlinePlayer(UUID uuid) {
        try {
            MinecraftServer srv = MinecraftServer.getServer();
            if (srv == null) return null;
            @SuppressWarnings("unchecked")
            List<EntityPlayerMP> players = srv.getConfigurationManager().playerEntityList;
            for (EntityPlayerMP p : players) {
                if (p.getUniqueID().equals(uuid)) return p;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static double availOrRoute(IEMCStorageGrid grid) {
        try {
            if (grid != null && findRedirectTarget(grid, false) != null) {
                return 1.0E15D;
            }
        } catch (Throwable ignored) {}
        return grid == null ? 0.0D : grid.getAvail();
    }
}
