package com.gtceuterminal.integration.kjs.events;

import com.gtceuterminal.api.TerminalAPI;

import dev.latvian.mods.kubejs.event.EventJS;

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
}