package com.gtceuterminal.common.compat.integrations;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.api.TerminalAPI;
import com.gtceuterminal.common.config.ComponentEntry;
import com.gtceuterminal.common.config.ComponentRegistry;
import com.gtceuterminal.common.multiblock.ComponentGroup;

import com.gregtechceu.gtceu.api.GTValues;

import com.gtceuterminal.common.util.MiscUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.fml.ModList;

public final class StarTCoreIntegration {
    public static final String SOLAR_CELLS       = "start_solar_cells";
    public static final String FUSION_REFLECTORS = "start_fusion_reflectors";
    public static final String FUSION_COILS      = "start_fusion_coils";
    public static final String THREADING_HELIXES = "start_threading_helixes";
    public static final String VACUUM_PUMPS      = "start_vacuum_pumps";

    private StarTCoreIntegration() {}

    public static void init() {
        if (!MiscUtil.isStarTCoreLoaded) {
            GTCEUTerminalMod.LOGGER.debug("[StarTCoreIntegration] start_core not installed, skipping.");
            return;
        }
        GTCEUTerminalMod.LOGGER.info("[StarTCoreIntegration] start_core detected, registering components.");
        registerSolarCells();
        registerFusionReflectors();
        registerFusionCoils();
        registerThreadingHelixes();
        registerVacuumPumps();
    }

    private static void registerSolarCells() {
        TerminalAPI.registerGroup(
                ComponentGroup.builder(SOLAR_CELLS, "Solar Cell")
                        .color(0xFFFFD700)
                        .energyHandler()
                        .registryCategory(SOLAR_CELLS)
                        .detector(b -> {
                            String id = BuiltInRegistries.BLOCK.getKey(b).toString();
                            return id.startsWith("start_core:") && id.endsWith("_solar_cell");
                        })
                        .build()
        );

        int[][] cells = {
                { GTValues.EV,  4 },
                { GTValues.IV,  5 },
                { GTValues.LuV, 6 },
                { GTValues.ZPM, 7 },
                { GTValues.UV,  8 },
                { GTValues.UHV, 9 },
        };
        for (int[] cell : cells) {
            int tier = cell[0];
            TerminalAPI.registerComponent(SOLAR_CELLS,
                    ComponentEntry.builder(
                            "start_core:" + GTValues.VN[tier].toLowerCase() + "_solar_cell",
                            GTValues.VNF[tier] + " Solar Cell",
                            GTValues.VN[tier], tier).build());
        }
    }

    private static void registerFusionReflectors() {
        TerminalAPI.registerGroup(
                ComponentGroup.builder(FUSION_REFLECTORS, "Fusion Reflector")
                        .color(0xFF00CCFF)
                        .energyHandler()
                        .registryCategory(FUSION_REFLECTORS)
                        .detector(b -> {
                            String id = BuiltInRegistries.BLOCK.getKey(b).toString();
                            return id.startsWith("start_core:") && id.contains("reflector");
                        })
                        .build()
        );

        // Reflectors are registered dynamically in StarTAPI.FUSION_REFLECTORS.
        try {
            Class<?> apiClass = Class.forName("com.startechnology.start_core.api.StarTAPI");
            java.util.Map<?, ?> reflectors = (java.util.Map<?, ?>) apiClass.getField("FUSION_REFLECTORS").get(null);
            for (var entry : reflectors.entrySet()) {
                Object type = entry.getKey();
                int tier = (int) type.getClass().getMethod("getTier").invoke(type);
                String name = (String) type.getClass().getMethod("getName").invoke(type);
                Object blockSupplier = entry.getValue();
                Object block = ((java.util.function.Supplier<?>) blockSupplier).get();
                String blockId = BuiltInRegistries.BLOCK.getKey((net.minecraft.world.level.block.Block) block).toString();

                TerminalAPI.registerComponent(FUSION_REFLECTORS,
                        ComponentEntry.builder(blockId, GTValues.VNF[tier] + " " + name, GTValues.VN[tier], tier).build());
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("[StarTCoreIntegration] Could not read FUSION_REFLECTORS: {}", e.getMessage());
        }
    }

    private static void registerFusionCoils() {
        TerminalAPI.registerGroup(
                ComponentGroup.builder(FUSION_COILS, "Fusion Coil")
                        .color(0xFF00FFCC)
                        .energyHandler()
                        .registryCategory(FUSION_COILS)
                        .detector(b -> {
                            String id = BuiltInRegistries.BLOCK.getKey(b).toString();
                            return id.startsWith("start_core:") && id.contains("fusion_coil");
                        })
                        .build()
        );

        TerminalAPI.registerComponent(FUSION_COILS,
                ComponentEntry.builder("start_core:advanced_fusion_coil",
                        "Advanced Fusion Coil", GTValues.VN[GTValues.UV], GTValues.UV).build());
        TerminalAPI.registerComponent(FUSION_COILS,
                ComponentEntry.builder("start_core:auxiliary_fusion_coil_mk1",
                        "Auxiliary Fusion Coil MK I", GTValues.VN[GTValues.UHV], GTValues.UHV).build());
        TerminalAPI.registerComponent(FUSION_COILS,
                ComponentEntry.builder("start_core:auxiliary_fusion_coil_mk2",
                        "Auxiliary Fusion Coil MK II", GTValues.VN[GTValues.UIV], GTValues.UIV).build());
    }

    private static void registerThreadingHelixes() {
        TerminalAPI.registerGroup(
                ComponentGroup.builder(THREADING_HELIXES, "Threading Helix")
                        .color(0xFF9B59B6)
                        .special()
                        .registryCategory(THREADING_HELIXES)
                        .detector(b -> {
                            String id = BuiltInRegistries.BLOCK.getKey(b).toString();
                            return id.startsWith("start_core:") && id.endsWith("_thread_helix");
                        })
                        .build()
        );

        String[][] helixes = {
                { "uev_supreme_thread_helix",       "UEV Supreme Thread Helix",       String.valueOf(GTValues.UEV) },
                { "uxv_supreme_thread_helix",       "UXV Supreme Thread Helix",       String.valueOf(GTValues.UXV) },
                { "max_supreme_thread_helix",       "MAX Supreme Thread Helix",       String.valueOf(GTValues.MAX) },
                { "uhv_overdrive_thread_helix",     "UHV Overdrive Thread Helix",     String.valueOf(GTValues.UHV) },
                { "uiv_overdrive_thread_helix",     "UIV Overdrive Thread Helix",     String.valueOf(GTValues.UIV) },
                { "opv_overdrive_thread_helix",     "OPV Overdrive Thread Helix",     String.valueOf(GTValues.OpV) },
                { "uhv_coprocessor_thread_helix",   "UHV Coprocessor Thread Helix",   String.valueOf(GTValues.UHV) },
                { "uiv_coprocessor_thread_helix",   "UIV Coprocessor Thread Helix",   String.valueOf(GTValues.UIV) },
                { "opv_coprocessor_thread_helix",   "OPV Coprocessor Thread Helix",   String.valueOf(GTValues.OpV) },
                { "uhv_weaving_thread_helix",       "UHV Weaving Thread Helix",       String.valueOf(GTValues.UHV) },
                { "uiv_weaving_thread_helix",       "UIV Weaving Thread Helix",       String.valueOf(GTValues.UIV) },
                { "opv_weaving_thread_helix",       "OPV Weaving Thread Helix",       String.valueOf(GTValues.OpV) },
        };
        for (String[] h : helixes) {
            int tier = Integer.parseInt(h[2]);
            TerminalAPI.registerComponent(THREADING_HELIXES,
                    ComponentEntry.builder("start_core:" + h[0], h[1], GTValues.VN[tier], tier).build());
        }
    }

    private static void registerVacuumPumps() {
        TerminalAPI.registerGroup(
                ComponentGroup.builder(VACUUM_PUMPS, "Vacuum Pump")
                        .color(0xFF3498DB)
                        .special()
                        .registryCategory(VACUUM_PUMPS)
                        .detector(b -> {
                            String id = BuiltInRegistries.BLOCK.getKey(b).toString();
                            return id.startsWith("start_core:") && id.endsWith("_vacuum_pump");
                        })
                        .build()
        );

        int[] tiers = { GTValues.ZPM, GTValues.UV, GTValues.UHV, GTValues.UEV, GTValues.UIV };
        for (int tier : tiers) {
            TerminalAPI.registerComponent(VACUUM_PUMPS,
                    ComponentEntry.builder(
                            "start_core:" + GTValues.VN[tier].toLowerCase() + "_vacuum_pump",
                            GTValues.VNF[tier] + " Vacuum Pump",
                            GTValues.VN[tier], tier).build());
        }
    }
}