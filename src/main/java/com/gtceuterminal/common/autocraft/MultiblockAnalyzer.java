package com.gtceuterminal.common.autocraft;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.material.ComponentUpgradeHelper;
import com.gtceuterminal.common.multiblock.ComponentInfo;
import com.gtceuterminal.common.pattern.AdvancedAutoBuilder;
import com.gtceuterminal.common.config.ManagerSettings;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gtceuterminal.common.compat.BlockPatternReflection;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gtceuterminal.common.compat.GTCEuCompat;
import com.gtceuterminal.common.compat.GTCEuCompat.PredicateCountMap;
import com.lowdragmc.lowdraglib.utils.BlockInfo;


import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.gtceuterminal.common.pattern.CandidateFilter;

import net.minecraft.world.item.BlockItem;

import java.lang.reflect.Field;
import java.util.*;

public final class MultiblockAnalyzer {

    private MultiblockAnalyzer() {}

    public static AnalysisResult analyzeForBuild(Player player,
                                                 IMultiController controller,
                                                 ManagerSettings.AutoBuildSettings settings) {
        BlockPatternReflection.ensure();
        if (!BlockPatternReflection.READY) return null;

        try {
            BlockPattern pattern        = controller.getPattern();
            MultiblockState worldState  = controller.getMultiblockState();

            TraceabilityPredicate[][][] blockMatches = (TraceabilityPredicate[][][]) BlockPatternReflection.F_BLOCK_MATCHES.get(pattern);
            int[]                       centerOff    = (int[])                       BlockPatternReflection.F_CENTER_OFFSET.get(pattern);
            int[][]                     aisleReps    = pattern.aisleRepetitions;
            RelativeDirection[]         structDir    = pattern.structureDir;

            if (blockMatches == null) return null;

            BlockPos  centerPos     = controller.self().getPos();
            var       facing        = controller.self().getFrontFacing();
            var       upFacing      = controller.self().getUpwardsFacing();
            boolean   isFlipped     = controller.self().isFlipped();
            int       minZ          = -centerOff[4];

            PredicateCountMap cacheGlobal = GTCEuCompat.getGlobalCount(worldState);
            PredicateCountMap cacheLayer = GTCEuCompat.getLayerCount(worldState);
            GTCEuCompat.clean(worldState);

            Map<Item, Integer> needed = new LinkedHashMap<>();

            for (int c = 0, z = minZ++; c < blockMatches.length; c++) {
                int reps = getRepetitions(c, aisleReps, settings.repeatCount);
                for (int r = 0; r < reps; r++, z++) {
                    cacheLayer.clear();
                    for (int b = 0, y = -centerOff[1]; b < blockMatches[c].length; b++, y++) {
                        for (int a = 0, x = -centerOff[0]; a < blockMatches[c][b].length; a++, x++) {
                            TraceabilityPredicate pred = blockMatches[c][b][a];
                            BlockPos pos = relativeOffset(structDir, x, y, z, facing, upFacing, isFlipped)
                                    .offset(centerPos);

                            if (!player.level().isEmptyBlock(pos)) {
                                GTCEuCompat.update(worldState, pos, pred);
                                for (SimplePredicate lim : pred.limited) lim.testLimited(worldState);
                                continue;
                            }

                            BlockInfo[] infos = pickInfos(pred, cacheGlobal, cacheLayer);
                            List<ItemStack> candidates = new java.util.ArrayList<>();
                            if (infos != null) for (BlockInfo info : infos)
                                if (info != null && !info.getBlockState().isAir()) candidates.add(info.getItemStackForm());

                            candidates = CandidateFilter.enrichWithCustomComponents(candidates, pred);
                            if (settings.noHatchMode != 0) candidates = CandidateFilter.filterOutMultiblockParts(candidates);
                            candidates = CandidateFilter.applyCoilTierPreference(candidates, settings.tierMode);

                            ItemStack candidate = firstUsableBlockItem(candidates);
                            if (candidate == null || candidate.isEmpty()) continue;

                            needed.merge(candidate.getItem(), 1, Integer::sum);
                        }
                    }
                }
            }

            return buildEntries(needed, player, controller.self().getPos(), 0);

        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("MultiblockAnalyzer.analyzeForBuild failed", e);
            return null;
        }
    }

    public static AnalysisResult analyzeForUpgrade(Player player,
                                                   List<ComponentInfo> components,
                                                   int targetTier,
                                                   String upgradeId,
                                                   BlockPos controllerPos) {
        Map<Item, Integer> needed = new LinkedHashMap<>();
        List<BlockPos>     positions = new ArrayList<>();

        for (ComponentInfo comp : components) {
            positions.add(comp.getPosition());
            Map<Item, Integer> perComp = (upgradeId != null && !upgradeId.isBlank())
                    ? ComponentUpgradeHelper.getUpgradeItemsForBlockId(upgradeId)
                    : ComponentUpgradeHelper.getUpgradeItems(comp, targetTier);
            perComp.forEach((item, count) -> needed.merge(item, count, Integer::sum));
        }

        try {
            AnalysisResult base = buildEntries(needed, player, controllerPos, 0);
            if (base == null) return null;
            return new AnalysisResult(base.entries, controllerPos, targetTier, upgradeId, positions);
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("MultiblockAnalyzer.analyzeForUpgrade failed", e);
            return null;
        }
    }

    private static AnalysisResult buildEntries(Map<Item, Integer> needed,
                                               Player player,
                                               BlockPos controllerPos,
                                               int isUseAE) {
        List<AnalysisResult.Entry> entries = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : needed.entrySet()) {
            entries.add(new AnalysisResult.Entry(new ItemStack(e.getKey(), e.getValue()), 0, false));
        }
        return new AnalysisResult(entries, controllerPos);
    }

    private static int getRepetitions(int slice, int[][] aisleReps, int repeatCount) {
        if (aisleReps == null || slice >= aisleReps.length) return 1;
        int min = aisleReps[slice][0];
        int max = aisleReps[slice][1];
        if (repeatCount == 0) return Math.max(1, min);
        int d = Math.max(repeatCount, min);
        if (max >= min && max > 0) d = Math.min(d, max);
        return Math.max(1, d);
    }

    private static BlockInfo[] pickInfos(TraceabilityPredicate pred,
                                         PredicateCountMap global,
                                         PredicateCountMap layer) {
        for (SimplePredicate lim : pred.limited) {
            if (lim.minLayerCount > 0) {
                int cur = layer.getInt(lim);
                if (cur < lim.minLayerCount && (lim.maxLayerCount == -1 || cur < lim.maxLayerCount)) {
                    layer.addTo(lim, 1);
                    return lim.candidates == null ? null : lim.candidates.get();
                }
            }
        }
        for (SimplePredicate lim : pred.limited) {
            if (lim.minCount > 0) {
                int cur = global.getInt(lim);
                if (cur < lim.minCount && (lim.maxCount == -1 || cur < lim.maxCount)) {
                    global.addTo(lim, 1);
                    return lim.candidates == null ? null : lim.candidates.get();
                }
            }
        }
        BlockInfo[] infos = new BlockInfo[0];
        for (SimplePredicate lim : pred.limited) {
            if (lim.maxLayerCount != -1 && layer.getOrDefault(lim, Integer.MAX_VALUE) == lim.maxLayerCount) continue;
            if (lim.maxCount     != -1 && global.getOrDefault(lim, Integer.MAX_VALUE) == lim.maxCount)     continue;
            layer.addTo(lim, 1);
            global.addTo(lim, 1);
            if (lim.candidates != null) infos = merge(infos, lim.candidates.get());
        }
        for (SimplePredicate com : pred.common) {
            if (com.candidates != null) infos = merge(infos, com.candidates.get());
        }
        return infos;
    }

    private static BlockInfo[] merge(BlockInfo[] a, BlockInfo[] b) {
        if (b == null) return a;
        BlockInfo[] result = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static ItemStack firstBlockItem(BlockInfo[] infos) {
        if (infos == null) return null;
        for (BlockInfo info : infos) {
            if (info == null || info.getBlockState().isAir()) continue;
            ItemStack s = info.getItemStackForm();
            if (!s.isEmpty()) return s;
        }
        return null;
    }

    private static ItemStack firstUsableBlockItem(List<ItemStack> candidates) {
        for (ItemStack s : candidates) {
            if (!s.isEmpty() && s.getItem() instanceof BlockItem) return s;
        }
        return null;
    }

    private static BlockPos relativeOffset(RelativeDirection[] dir,
                                           int x, int y, int z,
                                           net.minecraft.core.Direction facing,
                                           net.minecraft.core.Direction upFacing,
                                           boolean flipped) {
        int[] c0 = {x, y, z}, c1 = new int[3];
        var up = net.minecraft.core.Direction.UP;
        var down = net.minecraft.core.Direction.DOWN;

        if (facing == up || facing == down) {
            net.minecraft.core.Direction of = facing == down
                    ? upFacing : upFacing.getOpposite();
            for (int i = 0; i < 3; i++) {
                switch (GTCEuCompat.getActualDirection(dir[i], of)) {
                    case UP    -> c1[1] =  c0[i];
                    case DOWN  -> c1[1] = -c0[i];
                    case WEST  -> c1[0] = -c0[i];
                    case EAST  -> c1[0] =  c0[i];
                    case NORTH -> c1[2] = -c0[i];
                    case SOUTH -> c1[2] =  c0[i];
                }
            }
            int xOff = upFacing.getStepX(), zOff = upFacing.getStepZ(), tmp;
            if (xOff == 0) { tmp=c1[2]; c1[2]=zOff>0?c1[1]:-c1[1]; c1[1]=zOff>0?-tmp:tmp; }
            else           { tmp=c1[0]; c1[0]=xOff>0?c1[1]:-c1[1]; c1[1]=xOff>0?-tmp:tmp; }
            if (flipped) { if (upFacing==net.minecraft.core.Direction.NORTH||upFacing==net.minecraft.core.Direction.SOUTH) c1[0]=-c1[0]; else c1[2]=-c1[2]; }
        } else {
            for (int i = 0; i < 3; i++) {
                switch (GTCEuCompat.getActualDirection(dir[i], facing)) {
                    case UP    -> c1[1] =  c0[i];
                    case DOWN  -> c1[1] = -c0[i];
                    case WEST  -> c1[0] = -c0[i];
                    case EAST  -> c1[0] =  c0[i];
                    case NORTH -> c1[2] = -c0[i];
                    case SOUTH -> c1[2] =  c0[i];
                }
            }
            if (upFacing==net.minecraft.core.Direction.WEST||upFacing==net.minecraft.core.Direction.EAST) {
                int xOff=upFacing==net.minecraft.core.Direction.EAST?facing.getClockWise().getStepX():facing.getClockWise().getOpposite().getStepX();
                int zOff=upFacing==net.minecraft.core.Direction.EAST?facing.getClockWise().getStepZ():facing.getClockWise().getOpposite().getStepZ();
                int tmp;
                if (xOff==0){tmp=c1[2];c1[2]=zOff>0?-c1[1]:c1[1];c1[1]=zOff>0?tmp:-tmp;}
                else        {tmp=c1[0];c1[0]=xOff>0?-c1[1]:c1[1];c1[1]=xOff>0?tmp:-tmp;}
            } else if (upFacing==net.minecraft.core.Direction.SOUTH) {
                c1[1]=-c1[1];
                if (facing.getStepX()==0) c1[0]=-c1[0]; else c1[2]=-c1[2];
            }
            if (flipped) {
                if (upFacing==net.minecraft.core.Direction.NORTH||upFacing==net.minecraft.core.Direction.SOUTH) {
                    if (facing==net.minecraft.core.Direction.NORTH||facing==net.minecraft.core.Direction.SOUTH) c1[0]=-c1[0]; else c1[2]=-c1[2];
                } else c1[1]=-c1[1];
            }
        }
        return new BlockPos(c1[0], c1[1], c1[2]);
    }
}