package com.gtceuterminal.common.pattern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gregtechceu.gtceu.api.fluids.GTFluid;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.item.GTBucketItem;
import com.gtceuterminal.GTCEUTerminalMod;

import java.util.Optional;

// Helper class to handle fluid placement logic, including checks for valid placement
// handling of special cases (like water in the nether), and integration with player inventory and ME Network fluid storage.
public class FluidPlacementHelper {

    public static boolean tryPlaceFluid(
            @NotNull Level world,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull Fluid fluid,
            @Nullable IItemHandler playerInventory,
            @Nullable IFluidHandler fluidStorage
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

        boolean bucketFound = false;
        if (playerInventory != null) {
            for (int i = 0; i < playerInventory.getSlots(); i++) {
                ItemStack stack = playerInventory.getStackInSlot(i);
                if (stack.getItem() instanceof BucketItem bucketItem) {
                    if (bucketItem.getFluid() == fluid) {
                        if (placeFluidBlock(world, pos, currentState, fluid, player)) {
                            playerInventory.extractItem(i, 1, false);
                            bucketFound = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!bucketFound && fluidStorage != null) {
            FluidStack requiredFluid = new FluidStack(fluid, 1000);

            FluidStack drained = fluidStorage.drain(requiredFluid, IFluidHandler.FluidAction.SIMULATE);
            if (drained.getAmount() >= 1000) {
                if (placeFluidBlock(world, pos, currentState, fluid, player)) {
                    fluidStorage.drain(requiredFluid, IFluidHandler.FluidAction.EXECUTE);
                    return true;
                }
            }
        }

        return bucketFound;
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
            container.placeLiquid(world, pos, currentState, ((FlowingFluid) fluid).getSource(false));
            playEmptySound(world, pos, fluid, player);
            return true;
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
        if (world.dimensionType().ultraWarm() && fluid.is(FluidTags.WATER)) {
            return true;
        }

        return false;
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


    @Nullable
    public static Fluid findAvailableFluid(IItemHandler playerInventory, @Nullable IFluidHandler fluidStorage) {
        if (playerInventory != null) {
            for (int i = 0; i < playerInventory.getSlots(); i++) {
                ItemStack stack = playerInventory.getStackInSlot(i);
                if (stack.getItem() instanceof BucketItem bucketItem) {
                    Fluid fluid = bucketItem.getFluid();
                    if (fluid != Fluids.EMPTY) {
                        return fluid;
                    }
                }
            }
        }

        if (fluidStorage != null) {
            for (int tank = 0; tank < fluidStorage.getTanks(); tank++) {
                FluidStack fluidInTank = fluidStorage.getFluidInTank(tank);
                if (!fluidInTank.isEmpty() && fluidInTank.getAmount() >= 1000) {
                    return fluidInTank.getFluid();
                }
            }
        }

        return null;
    }
}