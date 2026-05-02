package com.blackkriger.equivalentincrease;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayerMP;

public final class EiNetwork {

    public static SimpleNetworkWrapper CHANNEL;

    private EiNetwork() {}

    public static void init() {
        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("ei");
        CHANNEL.registerMessage(EmcLivePKT.Handler.class, EmcLivePKT.class, 0, Side.CLIENT);
    }

    public static void sendEmcLive(EntityPlayerMP player, double newEmc) {
        if (CHANNEL != null) {
            CHANNEL.sendTo(new EmcLivePKT(newEmc), player);
        }
    }
}
