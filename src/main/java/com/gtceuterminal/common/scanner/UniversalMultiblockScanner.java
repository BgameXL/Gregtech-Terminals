package com.gtceuterminal.common.scanner;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;
import java.util.function.BiConsumer;

public class UniversalMultiblockScanner {

    public static List<DetectedMultiblock> scanForAllMultiblocks(Level level, BlockPos center, int radius) {
        List<DetectedMultiblock> found = new ArrayList<>();

        int minX = center.getX() - radius, maxX = center.getX() + radius;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight(), center.getY() + radius);
        int minZ = center.getZ() - radius, maxZ = center.getZ() + radius;

        forEachMachineEntity(level, minX, minY, minZ, maxX, maxY, maxZ, (pos, mbe) -> {
            if (!(mbe.getMetaMachine() instanceof MultiblockControllerMachine controller)) return;
            if (isFormed(controller)) {
                DetectedMultiblock detected = MultiblockAnalysisHelper.analyzeMultiblock(controller, pos, level);
                if (detected != null) found.add(detected);
            } else {
                found.add(DetectedMultiblock.unformed(controller, pos));
            }
        });

        if (ServerConfig.isDebugLoggingEnabled())
            GTCEUTerminalMod.LOGGER.info("UniversalMultiblockScanner: found {} multiblocks in radius {} from {}", found.size(), radius, center);
        return found;
    }

    static void forEachMachineEntity(Level level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                     BiConsumer<BlockPos, IMachineBlockEntity> action) {
        int minCX = minX >> 4, maxCX = maxX >> 4;
        int minCZ = minZ >> 4, maxCZ = maxZ >> 4;
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    if (pos.getX() < minX || pos.getX() > maxX
                            || pos.getY() < minY || pos.getY() > maxY
                            || pos.getZ() < minZ || pos.getZ() > maxZ) continue;
                    if (entry.getValue() instanceof IMachineBlockEntity mbe) {
                        action.accept(pos, mbe);
                    }
                }
            }
        }
    }

    private static boolean isFormed(MultiblockControllerMachine controller) {
        if (controller.isFormed()) return true;
        try {
            var parts = controller.getParts();
            return parts != null && !parts.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public static Set<BlockPos> getMultiblockBlocks(MultiblockControllerMachine controller, Level level) {
        return MultiblockBounds.getMultiblockBlocks(controller, level);
    }

    public record ComponentData(String category, String name, int tier, BlockPos position) {

        @Override
            public String toString() {
                return String.format("%s (T%d) at %s", name, tier, position);
            }
        }

    public static class DetectedMultiblock {
        private final String name;
        private final String modId;
        private final BlockPos position;
        private final int tier;
        private final Map<String, List<ComponentData>> components;
        private final MultiblockControllerMachine controller;
        private boolean formed = true;
        public DetectedMultiblock(String name, String modId, BlockPos position, int tier,
                                  Map<String, List<ComponentData>> components,
                                  MultiblockControllerMachine controller) {
            this.name = name;
            this.modId = modId;
            this.position = position;
            this.tier = tier;
            this.components = components;
            this.controller = controller;
        }

        public static DetectedMultiblock unformed(MultiblockControllerMachine controller, BlockPos pos) {
            String name = "Unknown Multiblock";
            String modId = "unknown";
            int tier = 0;
            try {
                var def = controller.getDefinition();
                if (def != null) name = def.getDescriptionId();
            } catch (Exception ignored) {
            }
            try {
                var def = controller.getDefinition();
                if (def != null && def.getId() != null) modId = def.getId().getNamespace();
            } catch (Exception ignored) {
            }
            try {
                var def = controller.getDefinition();
                if (def != null && def.getTier() > 0) tier = def.getTier();
            } catch (Exception ignored) {
            }
            DetectedMultiblock stub = new DetectedMultiblock(
                    name, modId, pos, tier, new java.util.HashMap<>(), controller);
            stub.setFormed(false);
            return stub;
        }

        public boolean isFormed() {
            return formed;
        }

        public void setFormed(boolean formed) {
            this.formed = formed;
        }

        public String getName() {
            return name;
        }

        public String getModId() {
            return modId;
        }

        public BlockPos getPosition() {
            return position;
        }

        public int getTier() {
            return tier;
        }

        public Map<String, List<ComponentData>> getComponents() {
            return components;
        }

        public MultiblockControllerMachine getController() {
            return controller;
        }

        public int getTotalComponentCount() {
            return components.values().stream().mapToInt(List::size).sum();
        }

        public List<String> getComponentCategories() {
            return new ArrayList<>(components.keySet());
        }

        public boolean isFromMod(String modId) {
            return this.modId.equals(modId);
        }

        @Override
        public String toString() {
            return String.format("%s (%s) at %s - %d components", name, modId, position, getTotalComponentCount());
        }
    }
}