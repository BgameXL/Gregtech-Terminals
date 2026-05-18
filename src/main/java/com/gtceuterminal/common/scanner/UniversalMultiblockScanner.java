package com.gtceuterminal.common.scanner;

import com.gtceuterminal.GTCEUTerminalMod;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Universal Multiblock Detection System.
 * Detects ANY multiblock using the GTCEu API, regardless of the mod that added it.
 */
public class UniversalMultiblockScanner {

    public static List<DetectedMultiblock> scanForAllMultiblocks(Level level, BlockPos center, int radius) {
        List<DetectedMultiblock> found = new ArrayList<>();
        Set<BlockPos> scannedControllers = new HashSet<>();

        int minX = center.getX() - radius, maxX = center.getX() + radius;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight(), center.getY() + radius);
        int minZ = center.getZ() - radius, maxZ = center.getZ() + radius;

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (scannedControllers.contains(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof IMachineBlockEntity mbe)) continue;
            MetaMachine meta = mbe.getMetaMachine();
            if (!(meta instanceof MultiblockControllerMachine controller)) continue;

            BlockPos immutable = pos.immutable();
            scannedControllers.add(immutable);

            if (isMultiblockFormed(controller)) {
                DetectedMultiblock detected = MultiblockAnalysisHelper.analyzeMultiblock(controller, immutable, level);
                if (detected != null) {
                    found.add(detected);
                    GTCEUTerminalMod.LOGGER.info("Found multiblock: {} at {}", detected.getName(), immutable);
                }
            }
        }

        GTCEUTerminalMod.LOGGER.info("Total multiblocks found: {}", found.size());
        return found;
    }

    private static boolean isMultiblockFormed(MultiblockControllerMachine controller) {
        try {
            if (controller.isFormed()) return true;
            MultiblockState state = controller.getMultiblockState();
            if (state != null && state.isNeededFlip()) return true;
            var parts = controller.getParts();
            return parts != null && !parts.isEmpty();
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("Error checking if multiblock is formed: {}", e.getMessage());
            return false;
        }
    }

    public static Set<BlockPos> getMultiblockBlocksPublic(MultiblockControllerMachine controller, Level level) {
        return MultiblockBounds.getMultiblockBlocks(controller, level);
    }

    public static class ComponentData {
        private final String category;
        private final String name;
        private final int tier;
        private final BlockPos position;

        public ComponentData(String category, String name, int tier, BlockPos position) {
            this.category = category; this.name = name; this.tier = tier; this.position = position;
        }

        public String getCategory() { return category; }
        public String getName()     { return name; }
        public int getTier()        { return tier; }
        public BlockPos getPosition() { return position; }

        @Override
        public String toString() { return String.format("%s (T%d) at %s", name, tier, position); }
    }

    public static class DetectedMultiblock {
        private final String name;
        private final String modId;
        private final BlockPos position;
        private final int tier;
        private final Map<String, List<ComponentData>> components;
        private final MultiblockControllerMachine controller;

        public DetectedMultiblock(String name, String modId, BlockPos position, int tier,
                                  Map<String, List<ComponentData>> components,
                                  MultiblockControllerMachine controller) {
            this.name = name; this.modId = modId; this.position = position;
            this.tier = tier; this.components = components; this.controller = controller;
        }

        public String getName()     { return name; }
        public String getModId()    { return modId; }
        public BlockPos getPosition() { return position; }
        public int getTier()        { return tier; }
        public Map<String, List<ComponentData>> getComponents() { return components; }
        public MultiblockControllerMachine getController() { return controller; }

        public int getTotalComponentCount() { return components.values().stream().mapToInt(List::size).sum(); }
        public List<String> getComponentCategories() { return new ArrayList<>(components.keySet()); }
        public boolean isFromMod(String modId) { return this.modId.equals(modId); }

        @Override
        public String toString() {
            return String.format("%s (%s) at %s - %d components", name, modId, position, getTotalComponentCount());
        }
    }
}