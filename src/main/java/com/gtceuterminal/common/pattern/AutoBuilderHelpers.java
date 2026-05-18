package com.gtceuterminal.common.pattern;

import com.gtceuterminal.common.compat.GTCEuCompat.PredicateCountMap;
import com.gtceuterminal.common.config.ManagerSettings;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import com.gtceuterminal.common.compat.GTCEuCompat;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

final class AutoBuilderHelpers {

    private static final Direction[] FACINGS   = { Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN };
    private static final Direction[] FACINGS_H = { Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST };

    private AutoBuilderHelpers() {}

    static int getRepetitionsForSlice(int slice, int[][] aisleRepetitions, int repeatCount) {
        if (aisleRepetitions == null || slice < 0 || slice >= aisleRepetitions.length) return 1;
        int min = aisleRepetitions[slice][0];
        int max = aisleRepetitions[slice][1];
        if (repeatCount == 0) return Math.max(1, min);
        int desired = Math.max(repeatCount, min);
        if (max >= min && max > 0) desired = Math.min(desired, max);
        return Math.max(1, desired);
    }

    static BlockInfo[] pickInfosForPredicate(TraceabilityPredicate predicate,
                                              PredicateCountMap cacheGlobal,
                                              PredicateCountMap cacheLayer) {
        boolean find = false;
        BlockInfo[] infos = new BlockInfo[0];

        for (SimplePredicate limit : predicate.limited) {
            if (limit.minLayerCount > 0) {
                int curr = cacheLayer.getInt(limit);
                if (curr < limit.minLayerCount && (limit.maxLayerCount == -1 || curr < limit.maxLayerCount)) {
                    cacheLayer.addTo(limit, 1);
                } else continue;
                infos = limit.candidates == null ? null : limit.candidates.get();
                find = true;
                break;
            } else continue;
        }

        if (!find) {
            for (SimplePredicate limit : predicate.limited) {
                if (limit.minCount > 0) {
                    int curr = cacheGlobal.getInt(limit);
                    if (curr < limit.minCount && (limit.maxCount == -1 || curr < limit.maxCount)) {
                        cacheGlobal.addTo(limit, 1);
                    } else continue;
                    infos = limit.candidates == null ? null : limit.candidates.get();
                    find = true;
                    break;
                } else continue;
            }
        }

        if (!find) {
            for (SimplePredicate limit : predicate.limited) {
                if (limit.maxLayerCount != -1 && cacheLayer.getOrDefault(limit, Integer.MAX_VALUE) == limit.maxLayerCount) continue;
                if (limit.maxCount != -1 && cacheGlobal.getOrDefault(limit, Integer.MAX_VALUE) == limit.maxCount) continue;
                cacheLayer.addTo(limit, 1);
                cacheGlobal.addTo(limit, 1);
                infos = ArrayUtils.addAll(infos, limit.candidates == null ? null : limit.candidates.get());
            }
            for (SimplePredicate common : predicate.common) {
                infos = ArrayUtils.addAll(infos, common.candidates == null ? null : common.candidates.get());
            }
        }

        return infos;
    }

    static BlockPos setActualRelativeOffset(RelativeDirection[] structureDir,
                                             int x, int y, int z,
                                             Direction facing, Direction upwardsFacing,
                                             boolean isFlipped) {
        int[] c0 = new int[]{ x, y, z }, c1 = new int[3];

        if (facing == Direction.UP || facing == Direction.DOWN) {
            Direction of = facing == Direction.DOWN ? upwardsFacing : upwardsFacing.getOpposite();
            for (int i = 0; i < 3; i++) {
                switch (GTCEuCompat.getActualDirection(structureDir[i], of)) {
                    case UP    -> c1[1] =  c0[i];
                    case DOWN  -> c1[1] = -c0[i];
                    case WEST  -> c1[0] = -c0[i];
                    case EAST  -> c1[0] =  c0[i];
                    case NORTH -> c1[2] = -c0[i];
                    case SOUTH -> c1[2] =  c0[i];
                }
            }
            int xOff = upwardsFacing.getStepX(), zOff = upwardsFacing.getStepZ(), tmp;
            if (xOff == 0) { tmp = c1[2]; c1[2] = zOff > 0 ? c1[1] : -c1[1]; c1[1] = zOff > 0 ? -tmp : tmp; }
            else           { tmp = c1[0]; c1[0] = xOff > 0 ? c1[1] : -c1[1]; c1[1] = xOff > 0 ? -tmp : tmp; }
            if (isFlipped) {
                if (upwardsFacing == Direction.NORTH || upwardsFacing == Direction.SOUTH) c1[0] = -c1[0];
                else c1[2] = -c1[2];
            }
        } else {
            for (int i = 0; i < 3; i++) {
                switch (GTCEuCompat.getActualDirection(structureDir[i], facing)) {
                    case UP    -> c1[1] =  c0[i];
                    case DOWN  -> c1[1] = -c0[i];
                    case WEST  -> c1[0] = -c0[i];
                    case EAST  -> c1[0] =  c0[i];
                    case NORTH -> c1[2] = -c0[i];
                    case SOUTH -> c1[2] =  c0[i];
                }
            }
            if (upwardsFacing == Direction.WEST || upwardsFacing == Direction.EAST) {
                int xOff = upwardsFacing == Direction.EAST ? facing.getClockWise().getStepX() : facing.getClockWise().getOpposite().getStepX();
                int zOff = upwardsFacing == Direction.EAST ? facing.getClockWise().getStepZ() : facing.getClockWise().getOpposite().getStepZ();
                int tmp;
                if (xOff == 0) { tmp = c1[2]; c1[2] = zOff > 0 ? -c1[1] : c1[1]; c1[1] = zOff > 0 ? tmp : -tmp; }
                else           { tmp = c1[0]; c1[0] = xOff > 0 ? -c1[1] : c1[1]; c1[1] = xOff > 0 ? tmp : -tmp; }
            } else if (upwardsFacing == Direction.SOUTH) {
                c1[1] = -c1[1];
                if (facing.getStepX() == 0) c1[0] = -c1[0]; else c1[2] = -c1[2];
            }
            if (isFlipped) {
                if (upwardsFacing == Direction.NORTH || upwardsFacing == Direction.SOUTH) {
                    if (facing == Direction.NORTH || facing == Direction.SOUTH) c1[0] = -c1[0]; else c1[2] = -c1[2];
                } else { c1[1] = -c1[1]; }
            }
        }
        return new BlockPos(c1[0], c1[1], c1[2]);
    }

    static void resetFacing(BlockPos pos, BlockState blockState, Direction facing,
                             BiPredicate<BlockPos, Direction> checker, Consumer<BlockState> consumer) {
        if (blockState.hasProperty(BlockStateProperties.FACING)) {
            tryFacings(blockState, pos, checker, consumer, BlockStateProperties.FACING,
                    facing == null ? FACINGS : ArrayUtils.addAll(new Direction[]{ facing }, FACINGS));
        } else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            tryFacings(blockState, pos, checker, consumer, BlockStateProperties.HORIZONTAL_FACING,
                    facing == null || facing.getAxis() == Direction.Axis.Y ? FACINGS_H :
                            ArrayUtils.addAll(new Direction[]{ facing }, FACINGS_H));
        }
    }

    private static void tryFacings(BlockState blockState, BlockPos pos, BiPredicate<BlockPos, Direction> checker,
                                    Consumer<BlockState> consumer, Property<Direction> property, Direction[] facings) {
        Direction found = null;
        for (Direction f : facings) { if (checker.test(pos, f)) { found = f; break; } }
        if (found == null) found = Direction.NORTH;
        consumer.accept(blockState.setValue(property, found));
    }

    static ItemStack computeFallbackCasing(TraceabilityPredicate[][][] blockMatches,
                                            Set<net.minecraft.world.level.block.Block> hatchBlocks,
                                            net.minecraft.world.level.block.Block controllerBlock) {
        Object2IntOpenHashMap<net.minecraft.world.level.block.Block> freq = new Object2IntOpenHashMap<>();
        for (TraceabilityPredicate[][] slice : blockMatches)
            for (TraceabilityPredicate[] row : slice)
                for (TraceabilityPredicate p : row) {
                    if (p == null) continue;
                    addCasingCandidates(freq, p.limited, hatchBlocks, controllerBlock);
                    addCasingCandidates(freq, p.common,  hatchBlocks, controllerBlock);
                }

        net.minecraft.world.level.block.Block best = null; int bestCount = 0;
        for (var e : freq.object2IntEntrySet()) { if (e.getIntValue() > bestCount) { bestCount = e.getIntValue(); best = e.getKey(); } }
        if (best == null) return ItemStack.EMPTY;
        ItemStack st = new ItemStack(best.asItem());
        return st.isEmpty() ? ItemStack.EMPTY : st;
    }

    private static void addCasingCandidates(Object2IntOpenHashMap<net.minecraft.world.level.block.Block> freq,
                                             List<SimplePredicate> list,
                                             Set<net.minecraft.world.level.block.Block> hatchBlocks,
                                             net.minecraft.world.level.block.Block controllerBlock) {
        if (list == null) return;
        for (SimplePredicate sp : list) {
            if (sp == null || sp.candidates == null) continue;
            BlockInfo[] infos = sp.candidates.get();
            if (infos == null) continue;
            for (BlockInfo info : infos) {
                BlockState state = info.getBlockState();
                if (state == null) continue;
                var b = state.getBlock();
                if (b == Blocks.AIR || b == controllerBlock || b instanceof CoilBlock) continue;
                if (!hatchBlocks.isEmpty() && hatchBlocks.contains(b)) continue;
                freq.addTo(b, 1);
            }
        }
    }

    static Map<Item, Integer> preCalculateMaterials(
            Player player, IMultiController controller,
            ManagerSettings.AutoBuildSettings settings,
            TraceabilityPredicate[][][] blockMatches, int[][] aisleRepetitions,
            RelativeDirection[] structureDir, int[] centerOffset,
            PredicateCountMap cacheGlobal, PredicateCountMap cacheLayer,
            Map<BlockPos, Object> blocks, BlockPos centerPos,
            Direction facing, Direction upwardsFacing, boolean isFlipped, Level world
    ) {
        Map<Item, Integer> required = new HashMap<>();
        int minZ = -centerOffset[4];
        final int fingerLength = blockMatches.length;
        final int thumbLength  = fingerLength > 0 ? blockMatches[0].length : 0;
        final int palmLength   = thumbLength  > 0 ? blockMatches[0][0].length : 0;

        for (int c = 0, z = minZ++, r; c < fingerLength; c++) {
            int reps = getRepetitionsForSlice(c, aisleRepetitions, settings.repeatCount);
            for (r = 0; r < reps; r++) {
                for (int b = 0, y = -centerOffset[1]; b < thumbLength; b++, y++) {
                    for (int a = 0, x = -centerOffset[0]; a < palmLength; a++, x++) {
                        TraceabilityPredicate predicate = blockMatches[c][b][a];
                        BlockPos pos = setActualRelativeOffset(structureDir, x, y, z, facing, upwardsFacing, isFlipped)
                                .offset(centerPos.getX(), centerPos.getY(), centerPos.getZ());
                        if (!world.isEmptyBlock(pos)) continue;

                        BlockInfo[] infos = pickInfosForPredicate(predicate, cacheGlobal, cacheLayer);
                        List<ItemStack> candidates = new ArrayList<>();
                        if (infos != null) for (BlockInfo info : infos) if (!info.getBlockState().isAir()) candidates.add(info.getItemStackForm());

                        candidates = CandidateFilter.enrichWithCustomComponents(candidates, predicate);
                        if (settings.noHatchMode != 0) candidates = CandidateFilter.filterOutMultiblockParts(candidates);
                        candidates = CandidateFilter.applyCoilTierPreference(candidates, settings.tierMode);

                        boolean isFluidBlock = false;
                        for (ItemStack cand : candidates) {
                            if (cand.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().getFluidState().isSource()) {
                                isFluidBlock = true; break;
                            }
                        }
                        if (isFluidBlock) continue;

                        for (ItemStack cand : candidates) {
                            if (!cand.isEmpty() && cand.getItem() instanceof BlockItem) {
                                required.merge(cand.getItem(), 1, Integer::sum);
                                break;
                            }
                        }
                    }
                }
                z++;
            }
        }
        return required;
    }
}