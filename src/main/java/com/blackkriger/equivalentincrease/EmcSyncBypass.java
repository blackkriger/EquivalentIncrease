package com.blackkriger.equivalentincrease;

import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.network.packets.TransmutationEmcSyncPKT;
import net.minecraft.entity.player.EntityPlayerMP;

public final class EmcSyncBypass {

    public static boolean enabled = true;
    public static boolean fromCondenser = false;

    private EmcSyncBypass() {}

    public static boolean tryImmediate(EntityPlayerMP player, double emc) {
        if (enabled && !fromCondenser) {
            PacketHandler.sendTo(new TransmutationEmcSyncPKT(emc), player);
        }
        return false;
    }
}
