package com.gtceuterminal.common.scanner;

import com.gtceuterminal.GTCEUTerminalMod;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

final class MultiblockBounds {

    private static final int MAX_SCAN_SIZE_XZ = 48;
    private static final int MAX_SCAN_SIZE_Y  = 48;
    private static final int BOUNDS_PADDING   = 2;

    private MultiblockBounds() {}

    static Set<BlockPos> getMultiblockBlocks(MultiblockControllerMachine controller, Level level) {
        try {
            var cache = controller.getMultiblockState().getCache();
            if (cache != null && !cache.isEmpty()) {
                Set<BlockPos> positions = new HashSet<>(cache);
                positions.add(controller.getPos().immutable());
                return positions;
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.debug("MultiblockBounds: cache unavailable, falling back to flood fill: {}", e.getMessage());
        }
        return floodFill(controller, level);
    }

    private static Set<BlockPos> floodFill(MultiblockControllerMachine controller, Level level) {
        Set<BlockPos> positions = new HashSet<>();
        BlockPos controllerPos = controller.getPos();

        try {
            var anchors = new java.util.ArrayList<BlockPos>();
            anchors.add(controllerPos.immutable());

            var parts = controller.getParts();
            if (parts != null) {
                for (var part : parts) {
                    if (part != null && part.self() != null) {
                        BlockPos p = part.self().getPos().immutable();
                        anchors.add(p);
                        positions.add(p);
                    }
                }
            }

            int anchorMinX = anchors.stream().mapToInt(BlockPos::getX).min().orElse(controllerPos.getX());
            int anchorMinY = anchors.stream().mapToInt(BlockPos::getY).min().orElse(controllerPos.getY());
            int anchorMinZ = anchors.stream().mapToInt(BlockPos::getZ).min().orElse(controllerPos.getZ());
            int anchorMaxX = anchors.stream().mapToInt(BlockPos::getX).max().orElse(controllerPos.getX());
            int anchorMaxY = anchors.stream().mapToInt(BlockPos::getY).max().orElse(controllerPos.getY());
            int anchorMaxZ = anchors.stream().mapToInt(BlockPos::getZ).max().orElse(controllerPos.getZ());

            int minX = Math.min(anchorMinX - BOUNDS_PADDING, controllerPos.getX() - MAX_SCAN_SIZE_XZ);
            int minY = Math.min(anchorMinY - BOUNDS_PADDING, controllerPos.getY() - MAX_SCAN_SIZE_Y);
            int minZ = Math.min(anchorMinZ - BOUNDS_PADDING, controllerPos.getZ() - MAX_SCAN_SIZE_XZ);
            int maxX = Math.max(anchorMaxX + BOUNDS_PADDING, controllerPos.getX() + MAX_SCAN_SIZE_XZ);
            int maxY = Math.max(anchorMaxY + BOUNDS_PADDING, controllerPos.getY() + MAX_SCAN_SIZE_Y);
            int maxZ = Math.max(anchorMaxZ + BOUNDS_PADDING, controllerPos.getZ() + MAX_SCAN_SIZE_XZ);

            final int fMinX = minX, fMinY = minY, fMinZ = minZ;
            final int fMaxX = maxX, fMaxY = maxY, fMaxZ = maxZ;

            if (isCandidate(level.getBlockState(controllerPos))) positions.add(controllerPos.immutable());

            ArrayDeque<BlockPos> queue   = new ArrayDeque<>();
            Set<BlockPos>        visited = new HashSet<>();
            for (BlockPos a : anchors) if (a != null && visited.add(a)) queue.add(a);

            while (!queue.isEmpty()) {
                BlockPos cur = queue.poll();
                for (Direction dir : Direction.values()) {
                    BlockPos next = cur.relative(dir);
                    int nx = next.getX(), ny = next.getY(), nz = next.getZ();
                    if (nx < fMinX || nx > fMaxX || ny < fMinY || ny > fMaxY || nz < fMinZ || nz > fMaxZ) continue;
                    if (!visited.add(next)) continue;
                    if (!isCandidate(level.getBlockState(next))) continue;
                    positions.add(next.immutable());
                    queue.add(next);
                }
            }

        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("MultiblockBounds: flood fill error", e);
        }

        return positions;
    }

    private static boolean isCandidate(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (com.gtceuterminal.common.multiblock.ComponentGroupRegistry.detectFromBlock(state.getBlock())
                != com.gtceuterminal.common.multiblock.ComponentGroupRegistry.UNKNOWN) return true;
        try {
            String ns = state.getBlock().builtInRegistryHolder().key().location().getNamespace();
            return !"minecraft".equals(ns);
        } catch (IllegalStateException e) {
            GTCEUTerminalMod.LOGGER.debug("MultiblockBounds: could not read block namespace: {}", e.getMessage());
            return false;
        }
    }
}
