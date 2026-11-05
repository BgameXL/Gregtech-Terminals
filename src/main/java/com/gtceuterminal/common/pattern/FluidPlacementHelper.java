package com.gtceuterminal.common.pattern;

import com.gtceuterminal.GTCEUTerminalMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Helper class to handle fluid placement logic, including checks for valid placement
// handling of special cases (like water in the nether), and integration with player inventory and ME Network fluid storage.
public class FluidPlacementHelper {

    public static boolean tryPlaceFluid(
            @NotNull Level world,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull Fluid fluid,
            @Nullable PlayerFluidReservoir reservoir
    ) {
        if (!(fluid instanceof FlowingFluid)) {
            return false;
        }

        BlockState currentState = world.getBlockState(pos);

        if (!canPlaceFluid(world, pos, currentState, fluid)) {
            return false;
        }

        if (player.isCreative()) {
            return placeFluidBlock(world, pos, currentState, fluid, player);
        }

        if (reservoir == null || !reservoir.extract(fluid, 1000)) {
            return false;
        }

        if (placeFluidBlock(world, pos, currentState, fluid, player)) {
            return true;
        }

        int leftover = reservoir.insert(new FluidStack(fluid, 1000));
        if (leftover > 0) {
            GTCEUTerminalMod.LOGGER.warn("Extracted 1000 mB of {} but placement failed at {}; {} mB could not be refunded",
                    fluid, pos, leftover);
        }
        return false;
    }


    private static boolean canPlaceFluid(Level world, BlockPos pos, BlockState state, Fluid fluid) {
        if (state == fluid.defaultFluidState().createLegacyBlock()) {
            return false;
        }

        if (state.isAir()) {
            return true;
        }

        if (state.canBeReplaced(fluid)) {
            return true;
        }

        if (state.getBlock() instanceof LiquidBlockContainer container) {
            return container.canPlaceLiquid(world, pos, state, fluid);
        }

        return false;
    }


    private static boolean placeFluidBlock(Level world, BlockPos pos, BlockState currentState,
                                           Fluid fluid, @Nullable Player player) {
        if (shouldVaporize(world, pos, fluid)) {
            playVaporizationEffect(world, pos, player);
            return true;
        }

        if (currentState.getBlock() instanceof LiquidBlockContainer container && fluid == Fluids.WATER) {
            if (container.placeLiquid(world, pos, currentState, ((FlowingFluid) fluid).getSource(false))) {
                playEmptySound(world, pos, fluid, player);
                return true;
            }
            return false;
        }

        if (currentState.canBeReplaced(fluid) && !currentState.liquid()) {
            world.destroyBlock(pos, true);
        }

        BlockState fluidState = fluid.defaultFluidState().createLegacyBlock();
        if (world.setBlock(pos, fluidState, Block.UPDATE_ALL_IMMEDIATE) &&
                fluidState.getFluidState().isSource()) {
            playEmptySound(world, pos, fluid, player);
            return true;
        }

        return false;
    }

    private static boolean shouldVaporize(Level world, BlockPos pos, Fluid fluid) {
        return world.dimensionType().ultraWarm() && fluid.is(FluidTags.WATER);
    }


    private static void playVaporizationEffect(Level world, BlockPos pos, @Nullable Player player) {
        world.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);

        for (int i = 0; i < 8; i++) {
            double x = pos.getX() + world.random.nextDouble();
            double y = pos.getY() + world.random.nextDouble();
            double z = pos.getZ() + world.random.nextDouble();
            world.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }


    private static void playEmptySound(Level world, BlockPos pos, Fluid fluid, @Nullable Player player) {
        var soundEvent = fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        world.playSound(player, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}