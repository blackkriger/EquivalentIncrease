package com.blackkriger.equivalentincrease;

import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.network.packets.TransmutationEmcSyncPKT;
import net.minecraft.entity.player.EntityPlayerMP;

public final class EmcSyncBypass {

    public static boolean enabled = false;
    public static boolean exceptCondenser = true;
    public static boolean fromCondenser = false;

    private EmcSyncBypass() {}

    public static boolean tryImmediate(EntityPlayerMP player, double emc) {
        if (enabled || (exceptCondenser && !fromCondenser)) {
            PacketHandler.sendTo(new TransmutationEmcSyncPKT(emc), player);
            return true;
        }
        return false;
    }
}
