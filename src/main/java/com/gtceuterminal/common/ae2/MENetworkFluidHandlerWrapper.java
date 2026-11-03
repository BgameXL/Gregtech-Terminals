package com.gtceuterminal.common.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.networking.storage.IStorageService;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.PlayerSource;
import com.gtceuterminal.GTCEUTerminalMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MENetworkFluidHandlerWrapper implements IFluidHandler {

    private final MEStorage inventory;
    private final IActionSource actionSource;

    public MENetworkFluidHandlerWrapper(MEStorage inventory, IActionSource actionSource) {
        this.inventory = inventory;
        this.actionSource = actionSource;
    }

    public static MENetworkFluidHandlerWrapper fromGrid(IGrid grid, IActionSource actionSource) {
        IStorageService storage = grid.getStorageService();
        if (storage == null) {
            return null;
        }
        return new MENetworkFluidHandlerWrapper(storage.getInventory(), actionSource);
    }

    @Nullable
    public static MENetworkFluidHandlerWrapper fromTerminal(ItemStack terminalStack, Player player) {
        if (!MENetworkScanner.isAE2Available()) return null;

        try {
            IGrid grid = AccessPointLink.getLinkedGrid(terminalStack, player.level(), player);
            if (grid == null) grid = AccessPointLink.getGridFromCarriedTerminal(player, player.level());
            if (grid == null) return null;
            return fromGrid(grid, new PlayerSource(player, null));
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("MENetworkFluidHandlerWrapper.fromTerminal failed", e);
            return null;
        }
    }

    @Nullable
    public static MENetworkFluidHandlerWrapper getFromPlayer(Player player) {
        if (!MENetworkScanner.isAE2Available()) return null;

        try {
            IGrid grid = AccessPointLink.getGridFromCarriedTerminal(player, player.level());
            if (grid == null) return null;
            return fromGrid(grid, new PlayerSource(player, null));
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("MENetworkFluidHandlerWrapper.getFromPlayer failed", e);
        }
        return null;
    }

    @Override
    public int getTanks() {
        return 0;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
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
        if (resource == null || resource.isEmpty()) {
            return 0;
        }

        try {
            long dropletsOffered = resource.getAmount() * 81L;
            AEFluidKey fluidKey = AEFluidKey.of(resource.getFluid());

            long inserted;
            if (action == FluidAction.SIMULATE) {
                inserted = inventory.insert(fluidKey, dropletsOffered, Actionable.SIMULATE, actionSource);
            } else {
                inserted = inventory.insert(fluidKey, dropletsOffered, Actionable.MODULATE, actionSource);
            }

            return (int) (inserted / 81);

        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Error inserting fluid into ME Network", e);
        }

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
        return FluidStack.EMPTY;
    }
}
