package com.gtceuterminal.common.pattern;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.compat.BlockPatternReflection;
import com.gtceuterminal.common.compat.ExpandableMultiblock;
import com.gtceuterminal.common.compat.GTCEuCompat;
import com.gtceuterminal.common.compat.GTCEuCompat.PredicateCountMap;
import com.gtceuterminal.common.config.ManagerSettings;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the block layout of a multiblock from its pattern, reusing the same candidate selection as the auto-builder.
 */
public final class MultiblockGhostBuilder {

    private MultiblockGhostBuilder() {
    }

    public static Map<BlockPos, BlockState> build(Level level, IMultiController controller,
                                                  ManagerSettings.AutoBuildSettings settings) {
        Map<BlockPos, BlockState> out = new HashMap<>();
        try {
            BlockPatternReflection.ensure();
            if (!BlockPatternReflection.READY) return out;

            ExpandableMultiblock.Restore restore =
                    ExpandableMultiblock.applySize(controller.self(), settings.expandRadius, settings.expandHeight);
            BlockPattern pattern;
            try {
                pattern = controller.getPattern();
            } finally {
                if (restore != null) restore.close();
            }
            if (pattern == null) return out;

            TraceabilityPredicate[][][] blockMatches = (TraceabilityPredicate[][][]) BlockPatternReflection.F_BLOCK_MATCHES.get(pattern);
            int[] centerOffset = (int[]) BlockPatternReflection.F_CENTER_OFFSET.get(pattern);
            int[][] aisleRepetitions = pattern.aisleRepetitions;
            RelativeDirection[] structureDir = pattern.structureDir;
            if (blockMatches == null || centerOffset == null || aisleRepetitions == null || structureDir == null)
                return out;

            MultiblockState state = new MultiblockState(level, controller.self().getPos());
            GTCEuCompat.clean(state);
            PredicateCountMap cacheGlobal = GTCEuCompat.getGlobalCount(state);
            PredicateCountMap cacheLayer = GTCEuCompat.getLayerCount(state);

            Direction facing = controller.self().getFrontFacing();
            Direction upwardsFacing = controller.self().getUpwardsFacing();
            boolean isFlipped = AutoBuilderHelpers.resolveFlip(controller, settings);

            final int fingerLength = blockMatches.length;
            final int thumbLength = fingerLength > 0 ? blockMatches[0].length : 0;
            final int palmLength = thumbLength > 0 ? blockMatches[0][0].length : 0;

            int minZ = -centerOffset[4];
            for (int c = 0, z = minZ++, r; c < fingerLength; c++) {
                int repsForSlice = AutoBuilderHelpers.getRepetitionsForSlice(c, aisleRepetitions, settings.repeatCount);
                for (r = 0; r < repsForSlice; r++) {
                    cacheLayer.clear();
                    for (int b = 0, y = -centerOffset[1]; b < thumbLength; b++, y++) {
                        for (int a = 0, x = -centerOffset[0]; a < palmLength; a++, x++) {
                            TraceabilityPredicate predicate = blockMatches[c][b][a];
                            BlockPos off = AutoBuilderHelpers.setActualRelativeOffset(
                                    structureDir, x, y, z, facing, upwardsFacing, isFlipped);
                            if (off.equals(BlockPos.ZERO)) continue;
                            BlockState bs = pickRepresentative(predicate, cacheGlobal, cacheLayer, settings);
                            if (bs != null && !bs.isAir()) out.put(off, bs);
                        }
                    }
                    z++;
                }
            }
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("MultiblockGhostBuilder failed: {}", t.getMessage());
        }
        return out;
    }

    private static BlockState pickRepresentative(TraceabilityPredicate predicate,
                                                 PredicateCountMap cacheGlobal, PredicateCountMap cacheLayer,
                                                 ManagerSettings.AutoBuildSettings settings) {
        BlockInfo[] infos = AutoBuilderHelpers.pickInfosForPredicate(predicate, cacheGlobal, cacheLayer);
        if (infos == null) return null;

        BlockState fluidState = null;
        List<ItemStack> candidates = new ArrayList<>();
        for (BlockInfo info : infos) {
            if (info == null) continue;
            BlockState s = info.getBlockState();
            if (s.isAir()) continue;
            if (s.getFluidState().isSource()) {
                if (fluidState == null) fluidState = s;
            } else {
                candidates.add(info.getItemStackForm());
            }
        }

        candidates = CandidateFilter.enrichWithCustomComponents(candidates, predicate);
        if (settings.noHatchMode != 0) candidates = CandidateFilter.filterOutMultiblockParts(candidates);
        candidates = CandidateSelector.excludeOptionalAllowances(candidates, predicate);
        candidates = CandidateSelector.applyTierPreference(candidates, settings);

        for (ItemStack candidate : candidates) {
            if (candidate.getItem() instanceof BlockItem bi) return bi.getBlock().defaultBlockState();
        }
        return fluidState;
    }
}