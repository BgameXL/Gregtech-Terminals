package com.gtceuterminal.common.multiblock;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class DismantleScanner {

    public static ScanResult scanMultiblock(Level level, MultiblockControllerMachine controller) {
        BlockPos controllerPos = controller.getPos();
        Set<BlockPos> allBlocks = new HashSet<>();
        Map<Block, Integer> blockCounts = new HashMap<>();

        MultiblockState state = controller.getMultiblockState();
        if (state != null) {
            try {
                Collection<BlockPos> cache = state.getCache();
                if (cache != null && !cache.isEmpty()) {
                    allBlocks.addAll(cache);
                }
            } catch (Throwable ignored) {
            }
            if (allBlocks.isEmpty()) {
                controller.getParts().forEach(part -> allBlocks.add(part.self().getPos()));
            }

            allBlocks.add(controllerPos);

            for (BlockPos pos : allBlocks) {
                BlockState blockState = level.getBlockState(pos);
                if (blockState.isAir()) continue;

                Block block = blockState.getBlock();
                blockCounts.put(block, blockCounts.getOrDefault(block, 0) + 1);
            }
        }

        return new ScanResult(controllerPos, allBlocks, blockCounts);
    }

    public static class ScanResult {
        private final BlockPos controllerPos;
        private final Set<BlockPos> allBlocks;
        private final Map<Block, Integer> blockCounts;
        private final int totalBlocks;

        public ScanResult(BlockPos controllerPos, Set<BlockPos> allBlocks,
                          Map<Block, Integer> blockCounts) {
            this.controllerPos = controllerPos;
            this.allBlocks = allBlocks;
            this.blockCounts = blockCounts;
            this.totalBlocks = allBlocks.size();
        }

        public static ScanResult empty() {
            return new ScanResult(BlockPos.ZERO, Collections.emptySet(), Collections.emptyMap());
        }

        public BlockPos getControllerPos() {
            return controllerPos;
        }

        public Set<BlockPos> getAllBlocks() {
            return allBlocks;
        }

        public Map<Block, Integer> getBlockCounts() {
            return blockCounts;
        }

        public int getTotalBlocks() {
            return totalBlocks;
        }
    }
}
