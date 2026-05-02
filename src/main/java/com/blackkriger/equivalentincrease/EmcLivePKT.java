package com.blackkriger.equivalentincrease;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class EmcLivePKT implements IMessage {

    public double newEmc;

    public EmcLivePKT() {}

    public EmcLivePKT(double newEmc) {
        this.newEmc = newEmc;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        newEmc = buf.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(newEmc);
    }

    public static class Handler implements IMessageHandler<EmcLivePKT, IMessage> {
        @Override
        public IMessage onMessage(EmcLivePKT msg, MessageContext ctx) {
            EntityPlayer p = Minecraft.getMinecraft().thePlayer;
            if (p == null) return null;
            if (p.openContainer instanceof TransmutationContainer) {
                ((TransmutationContainer) p.openContainer).transmutationInventory.emc = msg.newEmc;
            }
            return null;
        }
    }
}
