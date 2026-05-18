package com.gtceuterminal.common.compat;

import com.gtceuterminal.GTCEUTerminalMod;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;

import net.minecraft.world.entity.player.Player;

public final class UIFactoryReflection {

    private UIFactoryReflection() {}

    public static <H> ModularUI invokeCreate(String className, Class<H> holderType, H holder, Player player) {
        try {
            Class<?> uiClass = Class.forName(className);
            var m = uiClass.getMethod("create", holderType, Player.class);
            return (ModularUI) m.invoke(null, holder, player);
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.error("UIFactoryReflection: failed to invoke {}.create()", className, t);
            throw new RuntimeException("Failed to create UI: " + className, t);
        }
    }

    public static ModularUI invokeCreateUI(Object instance) {
        try {
            var m = instance.getClass().getMethod("createUI");
            return (ModularUI) m.invoke(instance);
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.error("UIFactoryReflection: failed to invoke createUI() on {}", instance.getClass().getSimpleName(), t);
            throw new RuntimeException("Failed to invoke createUI()", t);
        }
    }
}