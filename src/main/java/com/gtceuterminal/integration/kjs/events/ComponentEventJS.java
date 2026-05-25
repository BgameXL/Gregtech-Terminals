package com.gtceuterminal.integration.kjs.events;

import com.gtceuterminal.api.TerminalAPI;
import com.gtceuterminal.common.config.ComponentEntry;
import com.gtceuterminal.common.multiblock.ComponentGroup;

import dev.latvian.mods.kubejs.event.EventJS;

import java.util.Map;

public class ComponentEventJS extends EventJS {

    public void addCoil(String blockId, String displayName, int tier, int temperature) {
        TerminalAPI.registerCoil(blockId, displayName, tier, temperature);
    }

    public void addEnergyHatch(String blockId, String displayName, String hatchType, String amperage, int tier) {
        TerminalAPI.registerEnergyHatch(blockId, displayName, hatchType, amperage, tier);
    }

    public void addFluidHatch(String blockId, String displayName, String hatchType, String capacity, int tier) {
        TerminalAPI.registerFluidHatch(blockId, displayName, hatchType, capacity, tier);
    }

    public void addBus(String blockId, String displayName, String hatchType, int tier) {
        TerminalAPI.registerBus(blockId, displayName, hatchType, tier);
    }

    public void addLaserHatch(String blockId, String displayName, String hatchType, String amperage, int tier) {
        TerminalAPI.registerLaserHatch(blockId, displayName, hatchType, amperage, tier);
    }

    public void addWirelessHatch(String blockId, String displayName, String hatchType, String amperage, int tier) {
        TerminalAPI.registerWirelessHatch(blockId, displayName, hatchType, amperage, tier);
    }

    public void addMufflerHatch(String blockId, String displayName, int tier) {
        TerminalAPI.registerMufflerHatch(blockId, displayName, tier);
    }

    public void addDualHatch(String blockId, String displayName, String hatchType, int tier) {
        TerminalAPI.registerDualHatch(blockId, displayName, hatchType, tier);
    }

    public void addComponent(String category, String blockId, String displayName, int tier, Map<String, String> attrs) {
        var builder = ComponentEntry.builder(blockId, displayName, tierName(tier), tier);
        if (attrs != null) attrs.forEach(builder::attr);
        TerminalAPI.registerComponent(category, builder.build());
    }

    public void addComponent(String category, String blockId, String displayName, int tier) {
        addComponent(category, blockId, displayName, tier, null);
    }

    public void addGroup(String id, String displayName, int color, String handlerType,
                         String registryCategory, boolean upgradeable) {
        var builder = ComponentGroup.builder(id, displayName).color(color);
        if (!upgradeable) builder.notUpgradeable();
        if (registryCategory != null && !registryCategory.isBlank()) builder.registryCategory(registryCategory);
        if (handlerType != null) {
            switch (handlerType.toLowerCase()) {
                case "energy"  -> builder.energyHandler();
                case "fluid"   -> builder.fluidHandler();
                case "item"    -> builder.itemHandler();
                case "data"    -> builder.dataHandler();
                case "special" -> builder.special();
            }
        }
        TerminalAPI.registerGroup(builder.build());
    }

    private static String tierName(int tier) {
        if (tier >= 0 && tier < com.gregtechceu.gtceu.api.GTValues.VN.length)
            return com.gregtechceu.gtceu.api.GTValues.VN[tier];
        return "T" + tier;
    }
}