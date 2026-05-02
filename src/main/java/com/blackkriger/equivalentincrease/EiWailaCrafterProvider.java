package com.blackkriger.equivalentincrease;

import com.mordenkainen.equivalentenergistics.blocks.crafter.tiles.TileEMCCrafterBase;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.util.List;

public class EiWailaCrafterProvider implements IWailaDataProvider {

    private static final String EI_TAG = "EI_Waila";
    private static final String HAS_TOME = "hasTome";
    private static final String REDIRECT = "redirect";

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Override
    public List getWailaHead(ItemStack itemStack, List currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return currenttip;
    }

    @Override
    public List getWailaBody(ItemStack itemStack, List currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        NBTTagCompound nbt = accessor.getNBTData();
        if (nbt == null || !nbt.hasKey(EI_TAG)) return currenttip;
        NBTTagCompound ei = nbt.getCompoundTag(EI_TAG);
        if (!ei.getBoolean(HAS_TOME)) return currenttip;
        boolean balance = ei.getBoolean(REDIRECT);
        currenttip.add("EMC Routing: "
                + (balance ? EnumChatFormatting.GREEN + "Balance" : EnumChatFormatting.YELLOW + "Storage"));
        return currenttip;
    }

    @Override
    public List getWailaTail(ItemStack itemStack, List currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return currenttip;
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world,
                                     int x, int y, int z) {
        if (!(tile instanceof TileEMCCrafterBase)) return tag;
        ItemStack tome = ((TileEMCCrafterBase) tile).getCurrentTome();
        NBTTagCompound ei = new NBTTagCompound();
        if (tome != null && tome.hasTagCompound()) {
            ei.setBoolean(HAS_TOME, true);
            ei.setBoolean(REDIRECT, tome.getTagCompound().getBoolean(EmcRouter.REDIRECT_KEY));
        } else {
            ei.setBoolean(HAS_TOME, false);
        }
        tag.setTag(EI_TAG, ei);
        return tag;
    }
}
