package com.gtceuterminal.common.pattern;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.ae2.MENetworkExtractor;
import com.gtceuterminal.common.ae2.MENetworkFluidHandlerWrapper;
import com.gtceuterminal.common.compat.BlockPatternReflection;
import com.gtceuterminal.common.compat.GTCEuCompat;
import com.gtceuterminal.common.compat.GTCEuCompat.PredicateCountMap;
import com.gtceuterminal.common.config.ManagerSettings;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import it.unimi.dsi.fastutil.ints.IntObjectPair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Advanced Auto-Builder for GTCEu Multiblocks (rebased on GTCEu BlockPattern.autoBuild)
 * - noHatchMode: avoids placing MultiblockPartMachine blocks (hatches/buses/etc.)
 * - repeatCount: overrides repetitions per slice (clamped to [min,max] when max exists)
 * - tierMode: coil tier selector (tierMode-1). Falls back to default candidates if no match.
 */
public class AdvancedAutoBuilder {

    public static boolean autoBuild(
            @NotNull Player player,
            @NotNull IMultiController controller,
            @NotNull ManagerSettings.AutoBuildSettings settings
    ) {
        try {
            BlockPatternReflection.ensure();
            if (!BlockPatternReflection.READY) return false;

            BlockPattern pattern = controller.getPattern();
            MultiblockState worldState = controller.getMultiblockState();
            Level world = player.level();

            TraceabilityPredicate[][][] blockMatches = (TraceabilityPredicate[][][]) BlockPatternReflection.F_BLOCK_MATCHES.get(pattern);
            int[] centerOffset = (int[]) BlockPatternReflection.F_CENTER_OFFSET.get(pattern);
            int[][] aisleRepetitions = pattern.aisleRepetitions;
            RelativeDirection[] structureDir = pattern.structureDir;

            if (blockMatches == null || aisleRepetitions == null || structureDir == null || centerOffset == null) {
                GTCEUTerminalMod.LOGGER.error("AdvancedAutoBuilder: pattern internals are null");
                return false;
            }

            int minZ = -centerOffset[4];
            GTCEuCompat.clean(worldState);

            BlockPos centerPos = controller.self().getPos();
            Direction facing = controller.self().getFrontFacing();
            Direction upwardsFacing = controller.self().getUpwardsFacing();
            boolean isFlipped = controller.self().isFlipped();

            PredicateCountMap cacheGlobal = GTCEuCompat.getGlobalCount(worldState);
            PredicateCountMap cacheLayer  = GTCEuCompat.getLayerCount(worldState);

            Map<BlockPos, Object> blocks = new HashMap<>();
            Set<BlockPos> placedByUs = new HashSet<>();
            blocks.put(centerPos, controller);

            int placedCount = 0;

            IFluidHandler fluidStorage = null;
            try { fluidStorage = getMENetworkFluidStorage(player); } catch (Exception ignored) {}

            final int fingerLength = blockMatches.length;
            final int thumbLength  = fingerLength > 0 ? blockMatches[0].length : 0;
            final int palmLength   = thumbLength  > 0 ? blockMatches[0][0].length : 0;

            for (int c = 0, z = minZ++, r; c < fingerLength; c++) {
                int repsForSlice = AutoBuilderHelpers.getRepetitionsForSlice(c, aisleRepetitions, settings.repeatCount);

                for (r = 0; r < repsForSlice; r++) {
                    cacheLayer.clear();

                    for (int b = 0, y = -centerOffset[1]; b < thumbLength; b++, y++) {
                        for (int a = 0, x = -centerOffset[0]; a < palmLength; a++, x++) {
                            TraceabilityPredicate predicate = blockMatches[c][b][a];
                            BlockPos pos = AutoBuilderHelpers.setActualRelativeOffset(
                                            structureDir, x, y, z, facing, upwardsFacing, isFlipped)
                                    .offset(centerPos.getX(), centerPos.getY(), centerPos.getZ());

                            GTCEuCompat.update(worldState, pos, predicate);

                            if (!world.isEmptyBlock(pos)) {
                                blocks.put(pos, world.getBlockState(pos));
                                for (SimplePredicate limit : predicate.limited) limit.testLimited(worldState);
                            } else {
                                BlockInfo[] infos = AutoBuilderHelpers.pickInfosForPredicate(predicate, cacheGlobal, cacheLayer);
                                List<ItemStack> candidates = new ArrayList<>();
                                if (infos != null) for (BlockInfo info : infos) if (!info.getBlockState().isAir()) candidates.add(info.getItemStackForm());

                                candidates = CandidateFilter.enrichWithCustomComponents(candidates, predicate);
                                if (settings.noHatchMode != 0) candidates = CandidateFilter.filterOutMultiblockParts(candidates);
                                candidates = CandidateFilter.applyCoilTierPreference(candidates, settings.tierMode);

                                // Fluid block handling
                                boolean isFluidBlock = false;
                                net.minecraft.world.level.material.Fluid targetFluid = null;
                                for (ItemStack cand : candidates) {
                                    if (cand.getItem() instanceof BlockItem bi) {
                                        BlockState cs = bi.getBlock().defaultBlockState();
                                        if (cs.getFluidState().isSource()) { isFluidBlock = true; targetFluid = cs.getFluidState().getType(); break; }
                                    }
                                }

                                if (isFluidBlock && targetFluid != null) {
                                    boolean placed = player.isCreative()
                                            ? FluidPlacementHelper.tryPlaceFluid(world, pos, player, targetFluid, null, null)
                                            : FluidPlacementHelper.tryPlaceFluid(world, pos, player, targetFluid,
                                            player.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null), fluidStorage);
                                    if (placed) { placedByUs.add(pos); placedCount++; blocks.put(pos, world.getBlockState(pos)); }
                                    continue;
                                }

                                ItemStack found = null;
                                int foundSlot = -1;
                                IItemHandler handler = null;

                                if (!player.isCreative()) {
                                    IntObjectPair<IItemHandler> foundHandler = getMatchStackWithHandler(
                                            candidates, player.getCapability(ForgeCapabilities.ITEM_HANDLER), player, settings.isUseAE);
                                    if (foundHandler != null) {
                                        foundSlot = foundHandler.firstInt();
                                        handler = foundHandler.second();
                                        found = handler.getStackInSlot(foundSlot).copy();
                                    }
                                } else {
                                    for (ItemStack cand : candidates) {
                                        found = cand.copy();
                                        if (!found.isEmpty() && found.getItem() instanceof BlockItem) break;
                                        found = null;
                                    }
                                }

                                if (found == null) continue;

                                BlockItem itemBlock = (BlockItem) found.getItem();
                                BlockPlaceContext context = new BlockPlaceContext(world, player, InteractionHand.MAIN_HAND, found,
                                        BlockHitResult.miss(player.getEyePosition(0), Direction.UP, pos));
                                InteractionResult result = itemBlock.place(context);
                                if (result != InteractionResult.FAIL) {
                                    placedByUs.add(pos);
                                    placedCount++;
                                    if (handler != null) handler.extractItem(foundSlot, 1, false);
                                }

                                if (world.getBlockEntity(pos) instanceof IMachineBlockEntity mbe) blocks.put(pos, mbe.getMetaMachine());
                                else blocks.put(pos, world.getBlockState(pos));
                            }
                        }
                    }
                    z++;
                }
            }

            // Post-placement facing
            Direction frontFacing = controller.self().getFrontFacing();
            blocks.forEach((pos, block) -> {
                if (block instanceof IMultiController) return;
                if (block instanceof BlockState state && placedByUs.contains(pos)) {
                    AutoBuilderHelpers.resetFacing(pos, state, frontFacing,
                            (p, f) -> { Object o = blocks.get(p.relative(f)); return o == null || (o instanceof BlockState bs && bs.getBlock() == net.minecraft.world.level.block.Blocks.AIR); },
                            newState -> world.setBlock(pos, newState, 3));
                } else if (block instanceof MetaMachine machine) {
                    AutoBuilderHelpers.resetFacing(pos, machine.getBlockState(), frontFacing,
                            (p, f) -> { Object o = blocks.get(p.relative(f)); return (o == null || (o instanceof BlockState bs && bs.isAir())) && machine.isFacingValid(f); },
                            newState -> world.setBlock(pos, newState, 3));
                }
            });

            return placedCount > 0;

        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.error("AdvancedAutoBuilder failed", t);
            return false;
        }
    }

    @Nullable
    private static IntObjectPair<IItemHandler> getMatchStackWithHandler(
            List<ItemStack> candidates,
            net.minecraftforge.common.util.LazyOptional<IItemHandler> cap,
            Player player, int isUseAE) {

        IItemHandler handler = cap.resolve().orElse(null);
        if (handler == null) return null;

        if (isUseAE == 1) {
            ItemStack extracted = MENetworkExtractor.tryExtractCandidateFromLinkedTerminal(candidates, player);
            if (extracted != null) {
                NonNullList<ItemStack> stacks = NonNullList.withSize(1, extracted);
                return IntObjectPair.of(0, new ItemStackHandler(stacks));
            }
            return null;
        }

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (candidates.stream().anyMatch(c -> ItemStack.isSameItemSameTags(c, stack))
                    && stack.getItem() instanceof BlockItem) {
                return IntObjectPair.of(i, handler);
            }
        }
        return null;
    }

    @Nullable
    private static IFluidHandler getMENetworkFluidStorage(@NotNull Player player) {
        return MENetworkFluidHandlerWrapper.getFromPlayer(player);
    }
}