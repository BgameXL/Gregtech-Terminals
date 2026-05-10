package com.gtceuterminal.common.pattern;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gtceuterminal.common.ae2.MENetworkExtractor;
import com.gtceuterminal.common.ae2.MENetworkFluidHandlerWrapper;
import com.gtceuterminal.common.ae2.WirelessTerminalHandler;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.config.ManagerSettings;
import com.gtceuterminal.common.ae2.MENetworkItemExtractor;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gtceuterminal.common.compat.GTCEuCompat;
import com.gtceuterminal.common.compat.GTCEuCompat.PredicateCountMap;
import com.gregtechceu.gtceu.common.block.CoilBlock;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.Set;
import java.util.HashSet;

/**
 * Advanced Auto-Builder for GTCEu Multiblocks (rebased on GTCEu BlockPattern.autoBuild)
 * - noHatchMode: avoids placing MultiblockPartMachine blocks (hatches/buses/etc.)
 * - repeatCount: overrides repetitions per slice (clamped to [min.max] when max exists, Integrate in tierMode)
 * - tierMode: used as a coil tier selector (tierMode-1). If no match, falls back to default candidates.
 */
public class AdvancedAutoBuilder {

    private static final Direction[] FACINGS = { Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN };
    private static final Direction[] FACINGS_H = { Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST };

    private static Field F_BLOCK_MATCHES;
    private static Field F_AISLE_REP;
    private static Field F_STRUCTURE_DIR;
    private static Field F_CENTER_OFFSET;

    private static boolean REFLECTION_READY = false;

    private static void ensureReflection() {
        if (REFLECTION_READY) return;
        try {
            F_BLOCK_MATCHES = BlockPattern.class.getDeclaredField("blockMatches");
            F_BLOCK_MATCHES.setAccessible(true);

            F_AISLE_REP = BlockPattern.class.getDeclaredField("aisleRepetitions");
            F_AISLE_REP.setAccessible(true);

            F_STRUCTURE_DIR = BlockPattern.class.getDeclaredField("structureDir");
            F_STRUCTURE_DIR.setAccessible(true);

            F_CENTER_OFFSET = BlockPattern.class.getDeclaredField("centerOffset");
            F_CENTER_OFFSET.setAccessible(true);

            REFLECTION_READY = true;
        } catch (Throwable t) {
            REFLECTION_READY = false;
            GTCEUTerminalMod.LOGGER.error("AdvancedAutoBuilder: Failed to init reflection for BlockPattern fields", t);
        }
    }

    public static boolean autoBuild(
            @NotNull Player player,
            @NotNull IMultiController controller,
            @NotNull ManagerSettings.AutoBuildSettings settings
    ) {
        try {
            ensureReflection();
            if (!REFLECTION_READY) return false;

            BlockPattern pattern = controller.getPattern();
            MultiblockState worldState = controller.getMultiblockState();
            Level world = player.level();

            TraceabilityPredicate[][][] blockMatches = (TraceabilityPredicate[][][]) F_BLOCK_MATCHES.get(pattern);
            int[][] aisleRepetitions = (int[][]) F_AISLE_REP.get(pattern);
            RelativeDirection[] structureDir = (RelativeDirection[]) F_STRUCTURE_DIR.get(pattern);
            int[] centerOffset = (int[]) F_CENTER_OFFSET.get(pattern);

            if (blockMatches == null || aisleRepetitions == null || structureDir == null || centerOffset == null) {
                GTCEUTerminalMod.LOGGER.error("AdvancedAutoBuilder: pattern internals are null (blockMatches/aisleRepetitions/structureDir/centerOffset)");
                return false;
            }

            int minZ = -centerOffset[4];
            // Save position cache before clean() so schematic copy/preview still works after auto-build
            it.unimi.dsi.fastutil.longs.LongOpenHashSet savedCache =
                    new it.unimi.dsi.fastutil.longs.LongOpenHashSet(worldState.cache);
            GTCEuCompat.clean(worldState);

            BlockPos centerPos = controller.self().getPos();
            Direction facing = controller.self().getFrontFacing();
            Direction upwardsFacing = controller.self().getUpwardsFacing();
            boolean isFlipped = controller.self().isFlipped();

            // GTCEuCompat wrapper: Map type changed between GTCEu 1.6.4 and 7.0.0+
            PredicateCountMap cacheGlobal = GTCEuCompat.getGlobalCount(worldState);
            PredicateCountMap cacheLayer  = GTCEuCompat.getLayerCount(worldState);

            Map<BlockPos, Object> blocks = new HashMap<>();
            Set<BlockPos> placedByUs = new HashSet<>();
            blocks.put(centerPos, controller);

            int placedCount = 0;

            IFluidHandler fluidStorage = null;
            try {
                fluidStorage = getMENetworkFluidStorage(player);
            } catch (Exception e) {
                GTCEUTerminalMod.LOGGER.debug("No ME Network fluid storage available for auto-build");
            }

            // In GTCEu these are fields; we derive them from the array
            final int fingerLength = blockMatches.length;
            final int thumbLength  = fingerLength > 0 ? blockMatches[0].length : 0;
            final int palmLength   = thumbLength  > 0 ? blockMatches[0][0].length : 0;

            for (int c = 0, z = minZ++, r; c < fingerLength; c++) {

                int repsForSlice = getRepetitionsForSlice(c, aisleRepetitions, settings.repeatCount);

                for (r = 0; r < repsForSlice; r++) {
                    cacheLayer.clear();

                    for (int b = 0, y = -centerOffset[1]; b < thumbLength; b++, y++) {
                        for (int a = 0, x = -centerOffset[0]; a < palmLength; a++, x++) {

                            TraceabilityPredicate predicate = blockMatches[c][b][a];
                            BlockPos pos = setActualRelativeOffset(structureDir, x, y, z, facing, upwardsFacing, isFlipped)
                                    .offset(centerPos.getX(), centerPos.getY(), centerPos.getZ());

                            GTCEuCompat.update(worldState, pos, predicate);

                            if (!world.isEmptyBlock(pos)) {
                                blocks.put(pos, world.getBlockState(pos));
                                // Count limited predicates for blocks that are already placed
                                for (SimplePredicate limit : predicate.limited) {
                                    limit.testLimited(worldState);
                                }
                            } else {
                                BlockInfo[] infos = pickInfosForPredicate(predicate, cacheGlobal, cacheLayer);

                                List<ItemStack> candidates = new ArrayList<>();
                                if (infos != null) {
                                    for (BlockInfo info : infos) {
                                        if (!info.getBlockState().isAir()) {
                                            candidates.add(info.getItemStackForm());
                                        }
                                    }
                                }

                                candidates = enrichWithCustomComponents(candidates, predicate);

                                if (settings.noHatchMode != 0) {
                                    candidates = filterOutMultiblockParts(candidates);
                                }
                                candidates = applyCoilTierPreference(candidates, settings.tierMode);

                                // Fluid block handling
                                boolean isFluidBlock = false;
                                Fluid targetFluid = null;

                                for (ItemStack candidate : candidates) {
                                    BlockState candidateState = null;
                                    if (candidate.getItem() instanceof BlockItem bi) {
                                        candidateState = bi.getBlock().defaultBlockState();
                                    }
                                    if (candidateState != null && candidateState.getFluidState().isSource()) {
                                        isFluidBlock = true;
                                        targetFluid = candidateState.getFluidState().getType();
                                        break;
                                    }
                                }

                                if (isFluidBlock && targetFluid != null) {
                                    boolean placed = false;

                                    if (!player.isCreative()) {
                                        LazyOptional<IItemHandler> playerInvCap = player.getCapability(ForgeCapabilities.ITEM_HANDLER);
                                        IItemHandler playerInventory = playerInvCap.resolve().orElse(null);
                                        placed = FluidPlacementHelper.tryPlaceFluid(
                                                world, pos, player, targetFluid, playerInventory, fluidStorage);
                                    } else {
                                        placed = FluidPlacementHelper.tryPlaceFluid(
                                                world, pos, player, targetFluid, null, null);
                                    }

                                    if (placed) {
                                        placedByUs.add(pos);
                                        placedCount++;
                                        blocks.put(pos, world.getBlockState(pos));
                                    }
                                    continue;
                                }
                                ItemStack found = null;
                                int foundSlot = -1;
                                IItemHandler handler = null;

                                if (!player.isCreative()) {
                                    IntObjectPair<IItemHandler> foundHandler = getMatchStackWithHandler(
                                            candidates,
                                            player.getCapability(ForgeCapabilities.ITEM_HANDLER),
                                            player,
                                            settings.isUseAE);

                                    if (foundHandler != null) {
                                        foundSlot = foundHandler.firstInt();
                                        handler = foundHandler.second();
                                        found = handler.getStackInSlot(foundSlot).copy();
                                    }
                                } else {
                                    for (ItemStack candidate : candidates) {
                                        found = candidate.copy();
                                        if (!found.isEmpty() && found.getItem() instanceof BlockItem) break;
                                        found = null;
                                    }
                                }

                                if (found == null) {
                                    continue;
                                }

                                BlockItem itemBlock = (BlockItem) found.getItem();
                                BlockPlaceContext context = new BlockPlaceContext(
                                        world, player, InteractionHand.MAIN_HAND, found,
                                        BlockHitResult.miss(player.getEyePosition(0), Direction.UP, pos));

                                InteractionResult interactionResult = itemBlock.place(context);
                                if (interactionResult != InteractionResult.FAIL) {
                                    placedByUs.add(pos);
                                    placedCount++;
                                    if (handler != null) handler.extractItem(foundSlot, 1, false);
                                }

                                if (world.getBlockEntity(pos) instanceof IMachineBlockEntity mbe) {
                                    blocks.put(pos, mbe.getMetaMachine());
                                } else {
                                    blocks.put(pos, world.getBlockState(pos));
                                }
                            }
                        }
                    }
                    z++;
                }
            }

            // Post-placement facing adjustment
            Direction frontFacing = controller.self().getFrontFacing();
            blocks.forEach((pos, block) -> {
                if (block instanceof IMultiController) return;

                if (block instanceof BlockState state && placedByUs.contains(pos)) {
                    resetFacing(pos, state, frontFacing,
                            (p, f) -> {
                                Object object = blocks.get(p.relative(f));
                                return object == null ||
                                        (object instanceof BlockState bs && bs.getBlock() == Blocks.AIR);
                            },
                            newState -> world.setBlock(pos, newState, 3));
                } else if (block instanceof MetaMachine machine) {
                    resetFacing(pos, machine.getBlockState(), frontFacing,
                            (p, f) -> {
                                Object object = blocks.get(p.relative(f));
                                if (object == null || (object instanceof BlockState bs && bs.isAir())) {
                                    return machine.isFacingValid(f);
                                }
                                return false;
                            },
                            newState -> world.setBlock(pos, newState, 3));
                }
            });

            return placedCount > 0;

        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.error("AdvancedAutoBuilder failed", t);
            return false;
        }
    }

    // ── Candidate filtering — delegated to CandidateFilter ────────────────────
    private static List<ItemStack> filterOutMultiblockParts(List<ItemStack> c)                              { return CandidateFilter.filterOutMultiblockParts(c); }
    private static List<ItemStack> applyCoilTierPreference(List<ItemStack> c, int tierMode)                { return CandidateFilter.applyCoilTierPreference(c, tierMode); }
    private static List<ItemStack> enrichWithCustomComponents(List<ItemStack> c, TraceabilityPredicate p)  { return CandidateFilter.enrichWithCustomComponents(c, p); }

    private static int getRepetitionsForSlice(int slice, int[][] aisleRepetitions, int repeatCount) {
        if (aisleRepetitions == null || slice < 0 || slice >= aisleRepetitions.length) return 1;

        int min = aisleRepetitions[slice][0];
        int max = aisleRepetitions[slice][1];

        if (repeatCount == 0) return Math.max(1, min);

        int desired = repeatCount;
        desired = Math.max(desired, min);

        // Some patterns use max == min, others use max > min, others may use max == -1.
        if (max >= min && max > 0) desired = Math.min(desired, max);

        return Math.max(1, desired);
    }

    private static BlockInfo[] pickInfosForPredicate(TraceabilityPredicate predicate,
                                                     PredicateCountMap cacheGlobal,
                                                     PredicateCountMap cacheLayer) {
        boolean find = false;
        BlockInfo[] infos = new BlockInfo[0];

        // 1) limited with minLayerCount
        for (SimplePredicate limit : predicate.limited) {
            if (limit.minLayerCount > 0) {
                int curr = cacheLayer.getInt(limit);
                if (curr < limit.minLayerCount && (limit.maxLayerCount == -1 || curr < limit.maxLayerCount)) {
                    cacheLayer.addTo(limit, 1);
                } else {
                    continue;
                }
            } else continue;

            infos = limit.candidates == null ? null : limit.candidates.get();
            find = true;
            break;
        }

        // 2) limited with minCount (global)
        if (!find) {
            for (SimplePredicate limit : predicate.limited) {
                if (limit.minCount > 0) {
                    int curr = cacheGlobal.getInt(limit);
                    if (curr < limit.minCount && (limit.maxCount == -1 || curr < limit.maxCount)) {
                        cacheGlobal.addTo(limit, 1);
                    } else {
                        continue;
                    }
                } else continue;

                infos = limit.candidates == null ? null : limit.candidates.get();
                find = true;
                break;
            }
        }

        // 3) everything else (max checks) + common
        if (!find) {
            for (SimplePredicate limit : predicate.limited) {
                if (limit.maxLayerCount != -1 && cacheLayer.getOrDefault(limit, Integer.MAX_VALUE) == limit.maxLayerCount) {
                    continue;
                }
                if (limit.maxCount != -1 && cacheGlobal.getOrDefault(limit, Integer.MAX_VALUE) == limit.maxCount) {
                    continue;
                }
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

    private static BlockPos setActualRelativeOffset(RelativeDirection[] structureDir,
                                                    int x, int y, int z,
                                                    Direction facing, Direction upwardsFacing,
                                                    boolean isFlipped) {
        int[] c0 = new int[] { x, y, z }, c1 = new int[3];

        if (facing == Direction.UP || facing == Direction.DOWN) {
            Direction of = facing == Direction.DOWN ? upwardsFacing : upwardsFacing.getOpposite();
            for (int i = 0; i < 3; i++) {
                switch (GTCEuCompat.getActualDirection(structureDir[i], of)) {
                    case UP -> c1[1] = c0[i];
                    case DOWN -> c1[1] = -c0[i];
                    case WEST -> c1[0] = -c0[i];
                    case EAST -> c1[0] = c0[i];
                    case NORTH -> c1[2] = -c0[i];
                    case SOUTH -> c1[2] = c0[i];
                }
            }
            int xOffset = upwardsFacing.getStepX();
            int zOffset = upwardsFacing.getStepZ();
            int tmp;
            if (xOffset == 0) {
                tmp = c1[2];
                c1[2] = zOffset > 0 ? c1[1] : -c1[1];
                c1[1] = zOffset > 0 ? -tmp : tmp;
            } else {
                tmp = c1[0];
                c1[0] = xOffset > 0 ? c1[1] : -c1[1];
                c1[1] = xOffset > 0 ? -tmp : tmp;
            }
            if (isFlipped) {
                if (upwardsFacing == Direction.NORTH || upwardsFacing == Direction.SOUTH) {
                    c1[0] = -c1[0]; // flip X-axis
                } else {
                    c1[2] = -c1[2]; // flip Z-axis
                }
            }
        } else {
            for (int i = 0; i < 3; i++) {
                switch (GTCEuCompat.getActualDirection(structureDir[i], facing)) {
                    case UP -> c1[1] = c0[i];
                    case DOWN -> c1[1] = -c0[i];
                    case WEST -> c1[0] = -c0[i];
                    case EAST -> c1[0] = c0[i];
                    case NORTH -> c1[2] = -c0[i];
                    case SOUTH -> c1[2] = c0[i];
                }
            }
            if (upwardsFacing == Direction.WEST || upwardsFacing == Direction.EAST) {
                int xOffset = upwardsFacing == Direction.EAST ? facing.getClockWise().getStepX() :
                        facing.getClockWise().getOpposite().getStepX();
                int zOffset = upwardsFacing == Direction.EAST ? facing.getClockWise().getStepZ() :
                        facing.getClockWise().getOpposite().getStepZ();
                int tmp;
                if (xOffset == 0) {
                    tmp = c1[2];
                    c1[2] = zOffset > 0 ? -c1[1] : c1[1];
                    c1[1] = zOffset > 0 ? tmp : -tmp;
                } else {
                    tmp = c1[0];
                    c1[0] = xOffset > 0 ? -c1[1] : c1[1];
                    c1[1] = xOffset > 0 ? tmp : -tmp;
                }
            } else if (upwardsFacing == Direction.SOUTH) {
                c1[1] = -c1[1];
                if (facing.getStepX() == 0) {
                    c1[0] = -c1[0];
                } else {
                    c1[2] = -c1[2];
                }
            }
            if (isFlipped) {
                if (upwardsFacing == Direction.NORTH || upwardsFacing == Direction.SOUTH) {
                    if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                        c1[0] = -c1[0]; // flip X-axis
                    } else {
                        c1[2] = -c1[2]; // flip Z-axis
                    }
                } else {
                    c1[1] = -c1[1]; // flip Y-axis
                }
            }
        }
        return new BlockPos(c1[0], c1[1], c1[2]);
    }

    private static void resetFacing(BlockPos pos, BlockState blockState, Direction facing,
                                    BiPredicate<BlockPos, Direction> checker, Consumer<BlockState> consumer) {
        if (blockState.hasProperty(BlockStateProperties.FACING)) {
            tryFacings(blockState, pos, checker, consumer, BlockStateProperties.FACING,
                    facing == null ? FACINGS : ArrayUtils.addAll(new Direction[] { facing }, FACINGS));
        } else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            tryFacings(blockState, pos, checker, consumer, BlockStateProperties.HORIZONTAL_FACING,
                    facing == null || facing.getAxis() == Direction.Axis.Y ? FACINGS_H :
                            ArrayUtils.addAll(new Direction[] { facing }, FACINGS_H));
        }
    }

    private static void tryFacings(BlockState blockState, BlockPos pos, BiPredicate<BlockPos, Direction> checker,
                                   Consumer<BlockState> consumer, Property<Direction> property, Direction[] facings) {
        Direction found = null;
        for (Direction f : facings) {
            if (checker.test(pos, f)) {
                found = f;
                break;
            }
        }
        if (found == null) found = Direction.NORTH;
        consumer.accept(blockState.setValue(property, found));
    }

    @Nullable
    private static IntObjectPair<IItemHandler> getMatchStackWithHandler(
            List<ItemStack> candidates,
            LazyOptional<IItemHandler> cap,
            Player player,
            int isUseAE) {

        IItemHandler handler = cap.resolve().orElse(null);
        if (handler == null) {
            return null;
        }

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (isUseAE == 1 && WirelessTerminalHandler.isWirelessTerminal(stack)
                    && WirelessTerminalHandler.isLinked(stack)) {

                ItemStack extracted = MENetworkExtractor.tryExtractCandidateFromLinkedTerminal(
                        candidates, player);
                if (extracted != null) {
                    net.minecraft.core.NonNullList<ItemStack> stacks =
                            net.minecraft.core.NonNullList.withSize(1, extracted);
                    net.minecraftforge.items.IItemHandler tempHandler =
                            new net.minecraftforge.items.ItemStackHandler(stacks);
                    GTCEUTerminalMod.LOGGER.debug("Extracted {} from ME Network via terminal",
                            extracted.getItem().getDescription().getString());
                    return it.unimi.dsi.fastutil.ints.IntObjectPair.of(0, tempHandler);
                }
            }

            if (candidates.stream().anyMatch(candidate ->
                    ItemStack.isSameItemSameTags(candidate, stack)) &&
                    !stack.isEmpty() &&
                    stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
                return it.unimi.dsi.fastutil.ints.IntObjectPair.of(i, handler);
            }
        }

        return null;
    }
    @Nullable
    private static IFluidHandler getMENetworkFluidStorage(@NotNull Player player) {
        // Delegate to MENetworkFluidHandlerWrapper — keeps all appeng.* inside the ae2 package
        return MENetworkFluidHandlerWrapper.getFromPlayer(player);
    }

    private static ItemStack computeFallbackCasing(
            TraceabilityPredicate[][][] blockMatches,
            Set<net.minecraft.world.level.block.Block> hatchBlocks,
            net.minecraft.world.level.block.Block controllerBlock
    ) {
        Object2IntOpenHashMap<net.minecraft.world.level.block.Block> freq = new Object2IntOpenHashMap<>();

        for (int z = 0; z < blockMatches.length; z++) {
            for (int y = 0; y < blockMatches[z].length; y++) {
                for (int x = 0; x < blockMatches[z][y].length; x++) {
                    TraceabilityPredicate p = blockMatches[z][y][x];
                    if (p == null) continue;

                    // check limited + common
                    addCasingCandidates(freq, p.limited, hatchBlocks, controllerBlock);
                    addCasingCandidates(freq, p.common, hatchBlocks, controllerBlock);
                }
            }
        }

        net.minecraft.world.level.block.Block best = null;
        int bestCount = 0;

        for (var e : freq.object2IntEntrySet()) {
            int c = e.getIntValue();
            if (c > bestCount) {
                bestCount = c;
                best = e.getKey();
            }
        }

        if (best == null) return ItemStack.EMPTY;
        ItemStack st = new ItemStack(best.asItem());
        return st.isEmpty() ? ItemStack.EMPTY : st;
    }

    private static void addCasingCandidates(
            Object2IntOpenHashMap<net.minecraft.world.level.block.Block> freq,
            List<SimplePredicate> list,
            Set<net.minecraft.world.level.block.Block> hatchBlocks,
            net.minecraft.world.level.block.Block controllerBlock
    ) {
        if (list == null) return;

        for (SimplePredicate sp : list) {
            if (sp == null || sp.candidates == null) continue;
            BlockInfo[] infos = sp.candidates.get();
            if (infos == null) continue;

            for (BlockInfo info : infos) {
                BlockState state = info.getBlockState();
                if (state == null) continue;

                var b = state.getBlock();
                if (b == Blocks.AIR) continue;
                if (b == controllerBlock) continue;
                if (b instanceof CoilBlock) continue;

                if (!hatchBlocks.isEmpty() && hatchBlocks.contains(b)) continue;

                freq.addTo(b, 1);
            }
        }
    }

    // Pre-calculate all materials needed for construction.
    private static Map<Item, Integer> preCalculateMaterials(
            Player player,
            IMultiController controller,
            ManagerSettings.AutoBuildSettings settings,
            TraceabilityPredicate[][][] blockMatches,
            int[][] aisleRepetitions,
            RelativeDirection[] structureDir,
            int[] centerOffset,
            PredicateCountMap cacheGlobal,
            PredicateCountMap cacheLayer,
            Map<BlockPos, Object> blocks,
            BlockPos centerPos,
            Direction facing,
            Direction upwardsFacing,
            boolean isFlipped,
            Level world
    ) {
        Map<Item, Integer> required = new HashMap<>();

        int minZ = -centerOffset[4];
        final int fingerLength = blockMatches.length;
        final int thumbLength = fingerLength > 0 ? blockMatches[0].length : 0;
        final int palmLength = (thumbLength > 0) ? blockMatches[0][0].length : 0;

        for (int c = 0, z = minZ++, r; c < fingerLength; c++) {
            int repsForSlice = getRepetitionsForSlice(c, aisleRepetitions, settings.repeatCount);

            for (r = 0; r < repsForSlice; r++) {
                for (int b = 0, y = -centerOffset[1]; b < thumbLength; b++, y++) {
                    for (int a = 0, x = -centerOffset[0]; a < palmLength; a++, x++) {
                        TraceabilityPredicate predicate = blockMatches[c][b][a];
                        BlockPos pos = setActualRelativeOffset(structureDir, x, y, z, facing, upwardsFacing, isFlipped)
                                .offset(centerPos.getX(), centerPos.getY(), centerPos.getZ());

                        if (!world.isEmptyBlock(pos)) {
                            continue;
                        }

                        BlockInfo[] infos = pickInfosForPredicate(predicate, cacheGlobal, cacheLayer);
                        List<ItemStack> candidates = new ArrayList<>();

                        if (infos != null) {
                            for (BlockInfo info : infos) {
                                if (!info.getBlockState().isAir()) {
                                    candidates.add(info.getItemStackForm());
                                }
                            }
                        }

                        candidates = enrichWithCustomComponents(candidates, predicate);

                        if (settings.noHatchMode != 0) {
                            candidates = filterOutMultiblockParts(candidates);
                        }
                        candidates = applyCoilTierPreference(candidates, settings.tierMode);

                        boolean isFluidBlock = false;
                        for (ItemStack candidate : candidates) {
                            if (candidate.getItem() instanceof BlockItem bi) {
                                BlockState candidateState = bi.getBlock().defaultBlockState();
                                if (candidateState.getFluidState().isSource()) {
                                    isFluidBlock = true;
                                    break;
                                }
                            }
                        }

                        if (isFluidBlock) {
                            continue;
                        }

                        for (ItemStack candidate : candidates) {
                            if (!candidate.isEmpty() && candidate.getItem() instanceof BlockItem) {
                                Item item = candidate.getItem();
                                required.merge(item, 1, Integer::sum);
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