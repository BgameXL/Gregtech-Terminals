package com.gtceuterminal.common.compat.integrations;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.api.TerminalAPI;
import com.gtceuterminal.common.multiblock.ComponentGroup;
import com.gtceuterminal.common.util.MiscUtil;

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class TFGIntegration {

    public static final String MOD_ID = "tfg";

    public static final String SMR_HATCH_GROUP = "tfg_smr_fluid_hatch";
    public static final String ME_PORT_GROUP = "tfg_me_redstone_port";

    private static final String SMR_FLUID_INPUT = "smr_fluid_input";
    private static final String ME_REDSTONE_PORT = "me_redstone_port";

    private static final String SMR_HATCH_ID = "tfg:uv_smr_fluid_import_hatch";
    private static final String ME_PORT_ID = "tfg:me_assembler_redstone_port";

    private static Map<String, PartAbility> abilityCache;

    private TFGIntegration() {
    }

    public static void init() {
        if (!MiscUtil.isTFGLoaded) {
            GTCEUTerminalMod.LOGGER.debug("[TFGIntegration] tfg not installed, skipping.");
            return;
        }
        GTCEUTerminalMod.LOGGER.info("[TFGIntegration] tfg detected, registering components.");
        registerSmrFluidImportHatch();
        registerMeAssemblerRedstonePort();
    }

    private static void registerSmrFluidImportHatch() {
        TerminalAPI.registerGroup(
                ComponentGroup.builder(SMR_HATCH_GROUP, "SMR Fluid Import Hatch")
                        .color(0x4169E1)
                        .fluidHandler()
                        .notUpgradeable()
                        .detector(abilityDetector(SMR_FLUID_INPUT, SMR_HATCH_ID))
                        .build(),
                SMR_FLUID_INPUT);
    }

    private static void registerMeAssemblerRedstonePort() {
        TerminalAPI.registerGroup(
                ComponentGroup.builder(ME_PORT_GROUP, "ME Assembler Redstone Port")
                        .color(0x2BC4A5)
                        .special()
                        .notUpgradeable()
                        .detector(abilityDetector(ME_REDSTONE_PORT, ME_PORT_ID))
                        .build(),
                ME_REDSTONE_PORT);
    }

    private static Predicate<Block> abilityDetector(String abilityName, String blockId) {
        PartAbility ability = resolveAbility(abilityName);
        if (ability != null) {
            return ability::isApplicable;
        }
        GTCEUTerminalMod.LOGGER.warn("[TFGIntegration] ability '{}' not resolved, falling back to block id '{}'",
                abilityName, blockId);
        return block -> BuiltInRegistries.BLOCK.getKey(block).toString().equals(blockId);
    }

    private static PartAbility resolveAbility(String abilityName) {
        Map<String, PartAbility> cache = abilityCache;
        if (cache == null) {
            cache = loadAbilities();
            abilityCache = cache;
        }
        return cache.get(abilityName);
    }

    private static Map<String, PartAbility> loadAbilities() {
        Map<String, PartAbility> out = new HashMap<>();
        try {
            Class<?> holder = Class.forName("su.terrafirmagreg.core.common.data.tfgt.TFGPartAbility");
            for (Field f : holder.getDeclaredFields()) {
                if (f.getType() != PartAbility.class || !Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                PartAbility ability = (PartAbility) f.get(null);
                if (ability != null) out.put(ability.getName(), ability);
            }
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.warn("[TFGIntegration] could not read TFGPartAbility: {}", t.getMessage());
        }
        return out;
    }
}
