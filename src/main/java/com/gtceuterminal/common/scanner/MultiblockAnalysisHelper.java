package com.gtceuterminal.common.scanner;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.config.ComponentPattern;
import com.gtceuterminal.common.config.ComponentPatternRegistry;
import com.gtceuterminal.common.multiblock.ComponentGroup;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MultiblockAnalysisHelper {

    private MultiblockAnalysisHelper() {}

    public static UniversalMultiblockScanner.DetectedMultiblock analyzeMultiblock(
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
        } catch (Exception e) {
            return "Unknown Multiblock";
        }
    }

    private static String getMultiblockModId(MultiblockControllerMachine controller) {
        try {
            var def = controller.getDefinition();
            if (def != null && def.getId() != null) return def.getId().getNamespace();
        } catch (Exception ignored) {}
        return "unknown";
    }

    private static int getMultiblockTier(MultiblockControllerMachine controller) {
        try {
            var parts = controller.getParts();
            if (parts != null && !parts.isEmpty()) {
                int maxTier = parts.stream()
                        .filter(p -> p != null && p.self() != null && p.self().getDefinition() != null)
                        .mapToInt(p -> p.self().getDefinition().getTier())
                        .max().orElse(-1);
                if (maxTier >= 0) return maxTier;
            }
        } catch (Exception ignored) {}
        try {
            var def = controller.getDefinition();
            if (def != null && def.getTier() > 0) return def.getTier();
        } catch (Exception ignored) {}
        return 0;
    }

    static Map<String, List<UniversalMultiblockScanner.ComponentData>> extractAllComponents(
            MultiblockControllerMachine controller, Level level) {
        Map<String, List<UniversalMultiblockScanner.ComponentData>> components = new HashMap<>();
        Set<BlockPos> scanned = new HashSet<>();

        try {
            var parts = controller.getParts();
            if (parts == null || parts.isEmpty()) {
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

    // Scans a small radius for wireless/addon parts not returned by getParts().
    // Uses ComponentGroupRegistry (PartAbility-based) instead of string matching.
    private static void detectWirelessAndAddon(
            MultiblockControllerMachine controller, Level level,
            Map<String, List<UniversalMultiblockScanner.ComponentData>> components,
            Set<BlockPos> scanned) {
        try {
            BlockPos cp = controller.getPos();
            int r = 5;
            for (BlockPos pos : BlockPos.betweenClosed(cp.offset(-r, -r, -r), cp.offset(r, r, r))) {
                if (scanned.contains(pos)) continue;
                BlockEntity be = level.getBlockEntity(pos);
                if (!(be instanceof IMachineBlockEntity mbe)) continue;
                MetaMachine machine = mbe.getMetaMachine();
                if (machine == null || machine.getDefinition() == null) continue;
                if (!(machine instanceof MultiblockPartMachine part)) continue;
                if (!isLinkedToController(part, controller.getPos())) continue;

                // Filter: only non-standard parts not covered by getParts() are relevant here
                ComponentGroup group = ComponentGroupRegistry.detectFromBlock(machine.getBlockState().getBlock());
                if (group == ComponentGroupRegistry.UNKNOWN) continue;
                if (!group.isEnergyHandler && !group.isDataHandler) continue;

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
        return part.getControllers().stream()
                .anyMatch(c -> c.self().getPos().equals(wanted));
    }

    static UniversalMultiblockScanner.ComponentData analyzeComponent(MetaMachine machine, Level level) {
        try {
            String type = detectComponentType(machine);
            var def = machine.getDefinition();
            int tier    = def != null ? def.getTier() : 0;
            String name = def != null
                    ? net.minecraft.network.chat.Component.translatable(def.getDescriptionId()).getString()
                    : "Unknown";
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
                ComponentGroup group = ComponentGroupRegistry.detectFromBlock(machine.getBlockState().getBlock());
                if (group != ComponentGroupRegistry.UNKNOWN) {
                    String amp = BlockIdClassifier.detectAmperage(id);
                    return amp != null ? amp + " " + group.displayName : group.id;
                }
            }

            Optional<ComponentPattern> pattern = ComponentPatternRegistry.findMatch(id);
            if (pattern.isPresent()) {
                String displayName = pattern.get().getDisplayName();
                String amp = BlockIdClassifier.detectAmperage(id);
                if (amp != null && !displayName.contains(amp)) displayName = amp + " " + displayName;
                return displayName;
            }

            String detected = BlockIdClassifier.detectByImprovedAnalysis(id);
            if (detected != null) return detected;

        } catch (Exception ignored) {}
        return ComponentGroupRegistry.UNKNOWN.id;
    }

    static void extractStructureComponents(
            MultiblockControllerMachine controller, Level level,
            Map<String, List<UniversalMultiblockScanner.ComponentData>> components) {
        try {
            Set<BlockPos> positions = MultiblockBounds.getMultiblockBlocks(controller, level);
            for (BlockPos pos : positions) {
                UniversalMultiblockScanner.ComponentData data =
                        BlockIdClassifier.identifyStructureBlock(level.getBlockState(pos), pos, level);
                if (data != null) {
                    components.computeIfAbsent(data.getCategory(), k -> new ArrayList<>()).add(data);
                }
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Could not extract structure components: {}", e.getMessage());
        }
    }
}