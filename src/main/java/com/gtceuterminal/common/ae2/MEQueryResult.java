package com.gtceuterminal.common.ae2;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;

import java.util.HashMap;
import java.util.Map;

public final class MEQueryResult {

    public static final MEQueryResult EMPTY = new MEQueryResult(null, null);

    private final IGrid grid;
    private final KeyCounter cachedInventory;

    private MEQueryResult(IGrid grid, KeyCounter cachedInventory) {
        this.grid            = grid;
        this.cachedInventory = cachedInventory;
    }

    public static MEQueryResult resolve(net.minecraft.world.entity.player.Player player) {
        if (!MENetworkScanner.isAE2Available()) return EMPTY;

        IGrid grid = findLinkedGrid(player);
        if (grid == null) return EMPTY;

        KeyCounter inventory = grid.getStorageService().getCachedInventory();
        return new MEQueryResult(grid, inventory);
    }

    public boolean hasGrid() {
        return grid != null;
    }

    public IGrid getGrid() {
        return grid;
    }

    public long getAvailable(Item item) {
        if (cachedInventory == null) return 0;
        return cachedInventory.get(AEItemKey.of(item));
    }

    public boolean isCraftable(Item item) {
        if (grid == null) return false;
        return grid.getCraftingService().isCraftable(AEItemKey.of(item));
    }

    private static IGrid findLinkedGrid(net.minecraft.world.entity.player.Player player) {
        IGrid grid = tryStack(player.getMainHandItem(), player);
        if (grid != null) return grid;

        grid = tryStack(player.getOffhandItem(), player);
        if (grid != null) return grid;

        for (ItemStack stack : player.getInventory().items) {
            grid = tryStack(stack, player);
            if (grid != null) return grid;
        }
        return null;
    }

    private static IGrid tryStack(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (stack.isEmpty()) return null;
        return WirelessTerminalHandler.getLinkedGrid(stack, player.level(), player);
    }
}
