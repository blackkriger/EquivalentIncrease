package com.blackkriger.equivalentincrease;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

@Mod(
        modid = EquivalentIncrease.MODID,
        name = "Equivalent Increase",
        version = "1.2",
        dependencies = "required-after:equivalentenergistics;required-after:ProjectE",
        acceptableRemoteVersions = "*"
)
public class EquivalentIncrease {

    public static final String MODID = "equivalentincrease";

    private static final String TOME_CLASS = "com.mordenkainen.equivalentenergistics.items.ItemEMCBook";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Configuration cfg = new Configuration(event.getSuggestedConfigurationFile());
        cfg.load();
        EmcSyncBypass.enabled = cfg.getBoolean(
                "bypassEMCdebounce", "general", true,
                "When true, non-condenser EMC changes (transmutation crafts, learning items, etc.) bypass FMPE's EmcSyncThrottler debounce and update the GUI instantly. Condenser routing always goes through the debounce for stability.");
        if (cfg.hasChanged()) cfg.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        if (Loader.isModLoaded("Waila")) {
            FMLInterModComms.sendMessage("Waila", "register",
                    "com.blackkriger.equivalentincrease.EiWailaPlugin.register");
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent ev) {
        ItemStack stack = ev.itemStack;
        if (stack == null) return;
        Item item = stack.getItem();
        if (item == null || !TOME_CLASS.equals(item.getClass().getName())) return;
        if (!stack.hasTagCompound()) return;
        NBTTagCompound nbt = stack.getTagCompound();
        if (!nbt.hasKey(EmcRouter.OWNER_UUID_KEY)) return;

        boolean redirect = nbt.getBoolean(EmcRouter.REDIRECT_KEY);
        ev.toolTip.add("EMC routing: "
                + (redirect ? EnumChatFormatting.GREEN + "Balance"
                            : EnumChatFormatting.YELLOW + "Storage"));
    }
}
