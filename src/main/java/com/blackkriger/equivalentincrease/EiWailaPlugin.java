package com.blackkriger.equivalentincrease;

import com.mordenkainen.equivalentenergistics.blocks.crafter.tiles.TileEMCCrafterBase;
import mcp.mobius.waila.api.IWailaRegistrar;

public final class EiWailaPlugin {

    private EiWailaPlugin() {}

    public static void register(IWailaRegistrar registrar) {
        EiWailaCrafterProvider provider = new EiWailaCrafterProvider();
        registrar.registerBodyProvider(provider, TileEMCCrafterBase.class);
        registrar.registerNBTProvider(provider, TileEMCCrafterBase.class);
        System.out.println("[EquivalentIncrease] Waila body+NBT provider registered for TileEMCCrafterBase");
    }
}
