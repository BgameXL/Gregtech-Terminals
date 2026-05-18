package com.gtceuterminal.common.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import com.gtceuterminal.GTCEUTerminalMod;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MENetworkFluidHandlerWrapper implements IFluidHandler {

    private final MEStorage inventory;
    private final IActionSource actionSource;
    private List<FluidStack> cachedFluids;
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 1000; // 1 second cache

    public MENetworkFluidHandlerWrapper(MEStorage inventory, IActionSource actionSource) {
        this.inventory = inventory;
        this.actionSource = actionSource;
        this.cachedFluids = new ArrayList<>();
        updateCache();
    }

    public static MENetworkFluidHandlerWrapper fromGrid(IGrid grid, IActionSource actionSource) {
        IStorageService storage = grid.getStorageService();
        if (storage == null) {
            return null;
        }
        return new MENetworkFluidHandlerWrapper(storage.getInventory(), actionSource);
    }

    @org.jetbrains.annotations.Nullable
    public static MENetworkFluidHandlerWrapper getFromPlayer(net.minecraft.world.entity.player.Player player) {
        if (!MENetworkScanner.isAE2Available()) return null;

        try {
            java.util.List<net.minecraft.world.item.ItemStack> toCheck = new java.util.ArrayList<>();
            toCheck.add(player.getMainHandItem());
            toCheck.add(player.getOffhandItem());
            for (net.minecraft.world.item.ItemStack s : player.getInventory().items) toCheck.add(s);

            for (net.minecraft.world.item.ItemStack stack : toCheck) {
                if (stack.isEmpty()) continue;
                if (!WirelessTerminalHandler.isLinked(stack)) continue;

                IGrid grid = WirelessTerminalHandler.getLinkedGrid(stack, player.level(), player);
                if (grid == null) continue;

                IActionSource actionSource = new appeng.me.helpers.PlayerSource(player, null);
                MENetworkFluidHandlerWrapper wrapper = fromGrid(grid, actionSource);
                if (wrapper != null) {
                    GTCEUTerminalMod.LOGGER.debug("Connected to ME Network fluid storage via terminal");
                    return wrapper;
                }
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("MENetworkFluidHandlerWrapper.getFromPlayer failed", e);
        }
        return null;
    }

    private void updateCache() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate < CACHE_DURATION) {
            return;
        }

        cachedFluids.clear();

        try {
            KeyCounter availableStacks = new KeyCounter();
            inventory.getAvailableStacks(availableStacks);

            for (var entry : availableStacks) {
                AEKey key = entry.getKey();
                long amount = entry.getLongValue();

                if (key instanceof AEFluidKey fluidKey && amount > 0) {
                    int amountMB = (int) (amount / 81);

                    if (amountMB > 0) {
                        FluidStack stack = new FluidStack(fluidKey.getFluid(), amountMB);
                        cachedFluids.add(stack);
                    }
                }
            }

            lastCacheUpdate = now;

        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Error updating ME Network fluid cache", e);
        }
    }

    @Override
    public int getTanks() {
        updateCache();
        return cachedFluids.size();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        updateCache();
        if (tank < 0 || tank >= cachedFluids.size()) {
            return FluidStack.EMPTY;
        }
        return cachedFluids.get(tank).copy();
    }

    @Override
    public int getTankCapacity(int tank) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource == null || resource.isEmpty()) {
            return FluidStack.EMPTY;
        }

        try {
            long dropletsRequested = resource.getAmount() * 81L;

            AEFluidKey fluidKey = AEFluidKey.of(resource.getFluid());

            long extracted;
            if (action == FluidAction.SIMULATE) {
                extracted = inventory.extract(
                        fluidKey,
                        dropletsRequested,
                        Actionable.SIMULATE,
                        actionSource
                );
            } else {
                extracted = inventory.extract(
                        fluidKey,
                        dropletsRequested,
                        Actionable.MODULATE,
                        actionSource
                );
                lastCacheUpdate = 0;
            }

            int extractedMB = (int) (extracted / 81);

            if (extractedMB > 0) {
                return new FluidStack(resource.getFluid(), extractedMB);
            }

        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Error draining fluid from ME Network", e);
        }

        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        updateCache();
        if (cachedFluids.isEmpty()) {
            return FluidStack.EMPTY;
        }

        FluidStack first = cachedFluids.get(0);
        FluidStack toDrain = new FluidStack(first.getFluid(), Math.min(maxDrain, first.getAmount()));

        return drain(toDrain, action);
    }

    public boolean hasFluid(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) {
            return false;
        }

        updateCache();

        for (FluidStack stored : cachedFluids) {
            if (stored.getFluid() == fluid.getFluid() && stored.getAmount() >= fluid.getAmount()) {
                return true;
            }
        }

        return false;
    }

    public int getFluidAmount(net.minecraft.world.level.material.Fluid fluid) {
        updateCache();

        for (FluidStack stored : cachedFluids) {
            if (stored.getFluid() == fluid) {
                return stored.getAmount();
            }
        }

        return 0;
    }
}