package com.gtceuterminal.common.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class MENetworkItemExtractor {

    /**
     * Extracts all required items from the ME network.
     * On partial failure rolls back and returns false.
     */
    public static boolean extractItems(IGrid grid, Map<Item, Integer> required, Player player) {
        if (grid == null) return false;

        IStorageService storage = grid.getStorageService();
        IActionSource   src     = IActionSource.ofPlayer(player);

        // Verify before touching anything
        for (Map.Entry<Item, Integer> e : required.entrySet()) {
            long available = storage.getCachedInventory().get(AEItemKey.of(e.getKey()));
            if (available < e.getValue()) return false;
        }

        Map<Item, Long> extracted = new HashMap<>();
        for (Map.Entry<Item, Integer> e : required.entrySet()) {
            AEItemKey key    = AEItemKey.of(e.getKey());
            long      amount = e.getValue();
            long      got    = storage.getInventory().extract(key, amount, Actionable.MODULATE, src);

            if (got < amount) {
                rollback(storage, extracted, src);
                return false;
            }
            extracted.put(e.getKey(), got);
        }
        return true;
    }

    /**
     * Tries ME network first, then falls back to player inventory.
     */
    public static ExtractResult tryExtractFromMEOrInventory(IGrid grid, Map<Item, Integer> required, Player player) {
        if (grid != null && extractItems(grid, required, player)) {
            return new ExtractResult(true, ExtractionSource.ME_NETWORK);
        }
        if (extractFromPlayerInventory(player, required)) {
            return new ExtractResult(true, ExtractionSource.PLAYER_INVENTORY);
        }
        return new ExtractResult(false, ExtractionSource.NONE);
    }

    /**
     * Overload for callers that pass a terminal ItemStack instead of an already-resolved grid.
     * Resolves the grid from the player's inventory via {@link MEQueryResult}.
     */
    public static ExtractResult tryExtractFromMEOrInventory(ItemStack terminalStack,
                                                            net.minecraft.world.level.Level level,
                                                            Player player,
                                                            Map<Item, Integer> required) {
        MEQueryResult me = MEQueryResult.resolve(player);
        IGrid grid = me.hasGrid() ? resolveGrid(terminalStack, level, player) : null;
        return tryExtractFromMEOrInventory(grid, required, player);
    }

    /**
     * Returns how many of {@code item} are available in the ME network reachable from {@code terminalStack}.
     * Returns 0 if AE2 is absent, the terminal is unlinked, or out of range.
     */
    public static long checkItemAvailability(ItemStack terminalStack,
                                             net.minecraft.world.level.Level level,
                                             Player player,
                                             Item item) {
        if (!MENetworkScanner.isAE2Available()) return 0;
        if (!(terminalStack.getItem() instanceof appeng.items.tools.powered.WirelessTerminalItem terminal)) return 0;
        IGrid grid = terminal.getLinkedGrid(terminalStack, level, null);
        if (grid == null) return 0;
        return grid.getStorageService().getCachedInventory().get(appeng.api.stacks.AEItemKey.of(item));
    }

    private static IGrid resolveGrid(ItemStack terminalStack,
                                     net.minecraft.world.level.Level level,
                                     Player player) {
        if (terminalStack.getItem() instanceof appeng.items.tools.powered.WirelessTerminalItem terminal) {
            return terminal.getLinkedGrid(terminalStack, level, null);
        }
        return null;
    }

    // ------------------------------------------------------------------

    private static void rollback(IStorageService storage, Map<Item, Long> extracted, IActionSource src) {
        for (Map.Entry<Item, Long> e : extracted.entrySet()) {
            if (e.getValue() <= 0) continue;
            AEItemKey key = AEItemKey.of(e.getKey());
            storage.getInventory().insert(key, e.getValue(), Actionable.MODULATE, src);
        }
    }

    private static boolean extractFromPlayerInventory(Player player, Map<Item, Integer> required) {
        for (Map.Entry<Item, Integer> e : required.entrySet()) {
            if (countInInventory(player, e.getKey()) < e.getValue()) return false;
        }
        for (Map.Entry<Item, Integer> e : required.entrySet()) {
            int remaining = e.getValue();
            for (ItemStack slot : player.getInventory().items) {
                if (remaining <= 0) break;
                if (slot.getItem() == e.getKey()) {
                    int take = Math.min(remaining, slot.getCount());
                    slot.shrink(take);
                    remaining -= take;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private static int countInInventory(Player player, Item item) {
        int count = 0;
        for (ItemStack slot : player.getInventory().items) {
            if (slot.getItem() == item) count += slot.getCount();
        }
        return count;
    }

    public static class ExtractResult {
        public final boolean         success;
        public final ExtractionSource source;

        public ExtractResult(boolean success, ExtractionSource source) {
            this.success = success;
            this.source  = source;
        }
    }

    public enum ExtractionSource {
        ME_NETWORK, PLAYER_INVENTORY, NONE
    }
}