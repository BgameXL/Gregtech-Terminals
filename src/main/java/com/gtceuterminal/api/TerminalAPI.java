package com.gtceuterminal.api;

import com.gtceuterminal.common.config.ComponentEntry;
import com.gtceuterminal.common.config.ComponentRegistry;

import java.util.Map;

public final class TerminalAPI {

    private TerminalAPI() {}


    public static void registerCoil(String blockId, String displayName, int tier, int temperature) {
        ComponentRegistry.addExternal(ComponentRegistry.COILS,
                ComponentEntry.builder(blockId, displayName, "T" + tier, tier)
                        .attr("coilType", displayName)
                        .attr("heatCapacity", temperature + "K")
                        .build());
    }

    public static void registerEnergyHatch(String blockId, String displayName,
                                           String hatchType, String amperage, int tier) {
        ComponentRegistry.addExternal(ComponentRegistry.ENERGY_HATCHES,
                ComponentEntry.builder(blockId, displayName, tierName(tier), tier)
                        .attr("hatchType", hatchType)
                        .attr("amperage",  amperage)
                        .build());
    }

    public static void registerFluidHatch(String blockId, String displayName,
                                          String hatchType, String capacity, int tier) {
        ComponentRegistry.addExternal(ComponentRegistry.FLUID_HATCHES,
                ComponentEntry.builder(blockId, displayName, tierName(tier), tier)
                        .attr("hatchType", hatchType)
                        .attr("capacity",  capacity)
                        .build());
    }

    public static void registerBus(String blockId, String displayName, String hatchType, int tier) {
        ComponentRegistry.addExternal(ComponentRegistry.BUSES,
                ComponentEntry.builder(blockId, displayName, tierName(tier), tier)
                        .attr("hatchType", hatchType)
                        .build());
    }

    public static void registerLaserHatch(String blockId, String displayName,
                                          String hatchType, String amperage, int tier) {
        ComponentRegistry.addExternal(ComponentRegistry.LASER_HATCHES,
                ComponentEntry.builder(blockId, displayName, tierName(tier), tier)
                        .attr("hatchType", hatchType)
                        .attr("amperage",  amperage)
                        .build());
    }

    public static void registerWirelessHatch(String blockId, String displayName,
                                             String hatchType, String amperage, int tier) {
        var builder = ComponentEntry.builder(blockId, displayName, tierName(tier), tier)
                .attr("hatchType", hatchType);
        if (amperage != null) builder.attr("amperage", amperage);
        ComponentRegistry.addExternal(ComponentRegistry.WIRELESS_HATCHES, builder.build());
    }

    public static void registerMufflerHatch(String blockId, String displayName, int tier) {
        ComponentRegistry.addExternal(ComponentRegistry.MUFFLER_HATCHES,
                ComponentEntry.builder(blockId, displayName, tierName(tier), tier).build());
    }

    public static void registerDualHatch(String blockId, String displayName, String hatchType, int tier) {
        ComponentRegistry.addExternal(ComponentRegistry.DUAL_HATCHES,
                ComponentEntry.builder(blockId, displayName, tierName(tier), tier)
                        .attr("hatchType", hatchType)
                        .build());
    }

    public static void registerComponent(String category, ComponentEntry entry) {
        ComponentRegistry.addExternal(category, entry);
    }

    private static String tierName(int tier) {
        if (tier >= 0 && tier < com.gregtechceu.gtceu.api.GTValues.VN.length)
            return com.gregtechceu.gtceu.api.GTValues.VN[tier];
        return "T" + tier;
    }

    public static void registerGroup(com.gtceuterminal.common.multiblock.ComponentGroup group,
                                     String... abilityNames) {
        com.gtceuterminal.common.multiblock.ComponentGroupRegistry.register(group, abilityNames);
    }
}