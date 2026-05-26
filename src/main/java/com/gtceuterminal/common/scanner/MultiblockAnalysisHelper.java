package com.gtceuterminal.common.scanner;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.compat.MultiblockMachineReflection;
import com.gtceuterminal.common.config.ComponentPattern;
import com.gtceuterminal.common.config.ComponentPatternRegistry;
import com.gtceuterminal.common.multiblock.ComponentGroupRegistry;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class MultiblockAnalysisHelper {

    private MultiblockAnalysisHelper() {}

    static UniversalMultiblockScanner.DetectedMultiblock analyzeMultiblock(
            MultiblockControllerMachine controller, BlockPos pos, Level level) {
        try {
            String name  = getMultiblockName(controller);
            String modId = getMultiblockModId(controller);
            int tier     = getMultiblockTier(controller);
            Map<String, List<UniversalMultiblockScanner.ComponentData>> components =
                    extractAllComponents(controller, level);
            return new UniversalMultiblockScanner.DetectedMultiblock(name, modId, pos, tier, components, controller);
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Error analyzing multiblock at {}: {}", pos, e.getMessage());
            return null;
        }
    }

    private static String getMultiblockName(MultiblockControllerMachine controller) {
        try {
            var def = controller.getDefinition();
            if (def != null) return def.getDescriptionId();
            String s = controller.toString();
            return (s != null && !s.isEmpty()) ? s : "Unknown Multiblock";
        } catch (Exception e) { return "Unknown Multiblock"; }
    }

    private static String getMultiblockModId(MultiblockControllerMachine controller) {
        try {
            var def = controller.getDefinition();
            if (def != null) { var rl = def.getId(); if (rl != null) return rl.getNamespace(); }
            return "gtceu";
        } catch (Exception e) { return "unknown"; }
    }

    private static int getMultiblockTier(MultiblockControllerMachine controller) {
        try {
            var def = controller.getDefinition();
            if (def != null) return def.getTier();
        } catch (Exception e) {
            try {
                var parts = controller.getParts();
                if (parts != null && !parts.isEmpty()) {
                    return parts.stream()
                            .filter(p -> p != null && p.self() != null && p.self().getDefinition() != null)
                            .mapToInt(p -> p.self().getDefinition().getTier())
                            .max().orElse(0);
                }
            } catch (Exception e2) {
            }
        }
        return 0;
    }

    static Map<String, List<UniversalMultiblockScanner.ComponentData>> extractAllComponents(
            MultiblockControllerMachine controller, Level level) {
        Map<String, List<UniversalMultiblockScanner.ComponentData>> components = new HashMap<>();
        Set<BlockPos> scanned = new java.util.HashSet<>();

        try {
            var parts = controller.getParts();
            if (parts == null || parts.isEmpty()) {
                GTCEUTerminalMod.LOGGER.warn("Multiblock has no parts, scanning for wireless components...");
                detectWirelessAndAddon(controller, level, components, scanned);
                return components;
            }

            for (var part : parts) {
                if (part == null || part.self() == null) continue;
                UniversalMultiblockScanner.ComponentData data = analyzeComponent(part.self(), level);
                if (data != null) {
                    components.computeIfAbsent(data.getCategory(), k -> new ArrayList<>()).add(data);
                    scanned.add(part.self().getPos());
                }
            }

            extractStructureComponents(controller, level, components);
            detectWirelessAndAddon(controller, level, components, scanned);

        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Error extracting components: {}", e.getMessage());
        }

        return components;
    }

    private static void detectWirelessAndAddon(
            MultiblockControllerMachine controller, Level level,
            Map<String, List<UniversalMultiblockScanner.ComponentData>> components,
            Set<BlockPos> scanned) {
        try {
            BlockPos cp = controller.getPos();
            int r = 5;
            for (BlockPos pos : BlockPos.betweenClosed(
                    cp.offset(-r, -r, -r), cp.offset(r, r, r))) {
                if (scanned.contains(pos)) continue;
                BlockEntity be = level.getBlockEntity(pos);
                if (!(be instanceof IMachineBlockEntity mbe)) continue;
                MetaMachine machine = mbe.getMetaMachine();
                if (machine == null || machine.getDefinition() == null) continue;

                String blockId = machine.getDefinition().getId().toString().toLowerCase();

                if (!(machine instanceof MultiblockPartMachine part)) continue;
                if (!isLinkedToController(part, controller.getPos())) continue;

                boolean relevant = blockId.contains("wireless") && blockId.contains("energy")
                        || blockId.contains("laser") || blockId.contains("data")
                        || blockId.contains("optical") || blockId.contains("computation");

                if (!relevant) continue;

                UniversalMultiblockScanner.ComponentData data = analyzeComponent(machine, level);
                if (data != null) {
                    components.computeIfAbsent(data.getCategory(), k -> new ArrayList<>()).add(data);
                    scanned.add(pos.immutable());
                }
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Error detecting wireless/addon components: {}", e.getMessage());
        }
    }

    private static boolean isLinkedToController(MultiblockPartMachine part, BlockPos wanted) {
        return MultiblockMachineReflection.getLinkedControllerPos(part)
                .map(pos -> pos.equals(wanted))
                .orElse(false);
    }

    static UniversalMultiblockScanner.ComponentData analyzeComponent(MetaMachine machine, Level level) {
        try {
            String type = detectComponentType(machine);
            int tier = 0;
            var def = machine.getDefinition();
            if (def != null) tier = def.getTier();
            String name = def != null ? def.getDescriptionId() : "Unknown";
            return new UniversalMultiblockScanner.ComponentData(type, name, tier, machine.getPos());
        } catch (Exception e) {
            return null;
        }
    }

    private static String detectComponentType(MetaMachine machine) {
        try {
            var def = machine.getDefinition();
            if (def == null) return ComponentGroupRegistry.UNKNOWN.id;
            String id = def.getId().toString().toLowerCase();

            if (machine instanceof MultiblockPartMachine) {
                var block = machine.getBlockState().getBlock();
                com.gtceuterminal.common.multiblock.ComponentGroup group =
                        ComponentGroupRegistry.detectFromBlock(block);
                if (group != ComponentGroupRegistry.UNKNOWN) {
                    String amperage = BlockIdClassifier.detectAmperage(id);
                    if (amperage != null) return amperage + " " + group.displayName;
                    return group.id;
                }
            }

            Optional<ComponentPattern> patternOpt = ComponentPatternRegistry.findMatch(id);
            if (patternOpt.isPresent()) {
                ComponentPattern pattern = patternOpt.get();
                String displayName = pattern.getDisplayName();
                String amperage = BlockIdClassifier.detectAmperage(id);
                if (amperage != null && !displayName.contains(amperage)) displayName = amperage + " " + displayName;
                return displayName;
            }

            String detected = BlockIdClassifier.detectByImprovedAnalysis(id);
            if (detected != null) return detected;

            return ComponentGroupRegistry.UNKNOWN.id;

        } catch (Exception e) {
            return ComponentGroupRegistry.UNKNOWN.id;
        }
    }

    static void extractStructureComponents(
            MultiblockControllerMachine controller, Level level,
            Map<String, List<UniversalMultiblockScanner.ComponentData>> components) {
        try {
            Set<BlockPos> positions = MultiblockBounds.getMultiblockBlocks(controller, level);
            int found = 0;
            for (BlockPos pos : positions) {
                UniversalMultiblockScanner.ComponentData data =
                        BlockIdClassifier.identifyStructureBlock(level.getBlockState(pos), pos, level);
                if (data != null) {
                    components.computeIfAbsent(data.getCategory(), k -> new ArrayList<>()).add(data);
                    found++;
                }
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Could not extract structure components: {}", e.getMessage());
        }
    }
}