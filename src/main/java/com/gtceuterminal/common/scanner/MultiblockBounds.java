package com.gtceuterminal.common.scanner;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.config.ComponentRegistry;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class MultiblockBounds {

    private static final int MAX_SCAN_SIZE_XZ = 48;
    private static final int MAX_SCAN_SIZE_Y  = 48;
    private static final int BOUNDS_PADDING   = 2;

    private MultiblockBounds() {}

    static Set<BlockPos> getMultiblockBlocks(MultiblockControllerMachine controller, Level level) {
        Set<BlockPos> positions = new HashSet<>();
        BlockPos controllerPos = controller.getPos();

        try {
            List<BlockPos> anchors = new ArrayList<>();
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

            Bounds b = Bounds.fromAnchors(anchors, BOUNDS_PADDING)
                             .clampToMaxSize(controllerPos, MAX_SCAN_SIZE_XZ, MAX_SCAN_SIZE_Y);

            if (isCandidate(level.getBlockState(controllerPos))) positions.add(controllerPos.immutable());

            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();
            for (BlockPos a : anchors) if (a != null && visited.add(a)) queue.add(a);

            while (!queue.isEmpty()) {
                BlockPos cur = queue.poll();
                for (Direction dir : Direction.values()) {
                    BlockPos next = cur.relative(dir);
                    if (!b.contains(next) || !visited.add(next)) continue;
                    if (!isCandidate(level.getBlockState(next))) continue;
                    positions.add(next.immutable());
                    queue.add(next);
                }
            }

            GTCEUTerminalMod.LOGGER.debug("Found {} blocks via flood fill", positions.size());

        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Error in getMultiblockBlocks", e);
        }

        return positions;
    }

    private static boolean isCandidate(BlockState state) {
        if (state == null || state.isAir()) return false;
        try {
            String ns = state.getBlock().builtInRegistryHolder().key().location().getNamespace();
            if ("gtceu".equals(ns)) return true;
        } catch (IllegalStateException e) {
            GTCEUTerminalMod.LOGGER.debug("MultiblockBounds: could not read block namespace: {}", e.getMessage());
        }
        return ComponentRegistry.coilTier(state) >= 0;
    }

    private static final class Bounds {
        int minX, maxX, minY, maxY, minZ, maxZ;

        boolean contains(BlockPos pos) {
            if (pos == null) return false;
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        static Bounds fromAnchors(List<BlockPos> anchors, int padding) {
            Bounds b = new Bounds();
            b.minX = b.minY = b.minZ = Integer.MAX_VALUE;
            b.maxX = b.maxY = b.maxZ = Integer.MIN_VALUE;
            for (BlockPos p : anchors) {
                b.minX = Math.min(b.minX, p.getX()); b.maxX = Math.max(b.maxX, p.getX());
                b.minY = Math.min(b.minY, p.getY()); b.maxY = Math.max(b.maxY, p.getY());
                b.minZ = Math.min(b.minZ, p.getZ()); b.maxZ = Math.max(b.maxZ, p.getZ());
            }
            b.minX -= padding; b.maxX += padding;
            b.minY -= padding; b.maxY += padding;
            b.minZ -= padding; b.maxZ += padding;
            return b;
        }

        Bounds clampToMaxSize(BlockPos center, int maxXZ, int maxY) {
            if ((maxX - minX + 1) > maxXZ) { int h = maxXZ / 2; minX = center.getX() - h; maxX = center.getX() + h; }
            if ((maxZ - minZ + 1) > maxXZ) { int h = maxXZ / 2; minZ = center.getZ() - h; maxZ = center.getZ() + h; }
            if ((maxY - minY + 1) > maxY)  { int h = maxY  / 2; minY = center.getY() - h; maxY = center.getY() + h; }
            return this;
        }
    }
}