package com.gtceuterminal.common.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.me.helpers.PlayerSource;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class MENetworkItemExtractor {

    public static boolean hasItems(IGrid grid, Map<Item, Integer> required) {
        if (grid == null) {
            return false;
        }

        IStorageService storage = grid.getStorageService();
        if (storage == null) {
            return false;
        }

        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            Item item = entry.getKey();
            int requiredAmount = entry.getValue();

            ItemStack stack = new ItemStack(item);
            AEItemKey key = AEItemKey.of(stack);

            long available = storage.getInventory().getAvailableStacks().get(key);

            if (available < requiredAmount) {
                return false;
            }
        }

        return true;
    }

    public static long getItemCount(IGrid grid, Item item) {
        if (grid == null) {
            return 0;
        }

        IStorageService storage = grid.getStorageService();
        if (storage == null) {
            return 0;
        }

        ItemStack stack = new ItemStack(item);
        AEItemKey key = AEItemKey.of(stack);

        return storage.getInventory().getAvailableStacks().get(key);
    }

    public static long checkItemAvailability(
            ItemStack stack,
            net.minecraft.world.level.Level level,
            Player player,
            Item item) {

        if (WirelessTerminalHandler.isLinked(stack)) {
            IGrid grid = WirelessTerminalHandler.getLinkedGrid(stack, level, player);

            if (grid != null) {
                long count = getItemCount(grid, item);
                if (count > 0) {
                    GTCEUTerminalMod.LOGGER.debug("ME Network has {} of {}",
                            count, item.getDescription().getString());
                }
                return count;
            }
        }

        return 0;
    }


    public static boolean extractItems(IGrid grid, Map<Item, Integer> required, Player player) {
        if (grid == null) {
            GTCEUTerminalMod.LOGGER.warn("Cannot extract: grid is null");
            return false;
        }

        IStorageService storage = grid.getStorageService();
        if (storage == null) {
            GTCEUTerminalMod.LOGGER.warn("Cannot extract: storage service is null");
            return false;
        }

        if (!hasItems(grid, required)) {
            GTCEUTerminalMod.LOGGER.info("ME Network missing required items");
            for (Map.Entry<Item, Integer> entry : required.entrySet()) {
                long available = getItemCount(grid, entry.getKey());
                if (available < entry.getValue()) {
                    GTCEUTerminalMod.LOGGER.info("  - {}: need {}, have {}",
                            entry.getKey().getDescription().getString(),
                            entry.getValue(),
                            available);
                }
            }
            return false;
        }

        IActionSource actionSource = new PlayerSource(player, null);

        Map<Item, Long> extracted = new HashMap<>();
        int totalExtracted = 0;

        GTCEUTerminalMod.LOGGER.info("=== Extracting from ME Network ===");

        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            Item item = entry.getKey();
            int requiredAmount = entry.getValue();

            ItemStack stack = new ItemStack(item);
            AEItemKey key = AEItemKey.of(stack);

            long extractedAmount = storage.getInventory().extract(
                    key,
                    requiredAmount,
                    Actionable.MODULATE,
                    actionSource
            );

            if (extractedAmount < requiredAmount) {
                GTCEUTerminalMod.LOGGER.warn("Failed to extract {}: needed {}, got {}",
                        item.getDescription().getString(), requiredAmount, extractedAmount);
                rollbackExtraction(storage, extracted, actionSource);
                return false;
            }

            extracted.put(item, extractedAmount);
            totalExtracted += extractedAmount;

            GTCEUTerminalMod.LOGGER.info("  ✓ Extracted {} x{}",
                    item.getDescription().getString(), extractedAmount);
        }

        GTCEUTerminalMod.LOGGER.info("=== Total extracted: {} items from ME Network ===", totalExtracted);
        return true;
    }

    private static void rollbackExtraction(IStorageService storage, Map<Item, Long> extracted, IActionSource actionSource) {
        GTCEUTerminalMod.LOGGER.info("Rolling back extraction...");

        for (Map.Entry<Item, Long> entry : extracted.entrySet()) {
            Item item = entry.getKey();
            long amount = entry.getValue();

            if (amount > 0) {
                ItemStack stack = new ItemStack(item, (int)amount);
                AEItemKey key = AEItemKey.of(stack);

                storage.getInventory().insert(key, amount, Actionable.MODULATE, actionSource);
                GTCEUTerminalMod.LOGGER.info("  Returned {} x{}",
                        item.getDescription().getString(), amount);
            }
        }
    }

    public static ExtractResult tryExtractFromMEOrInventory(
            ItemStack stack,
            net.minecraft.world.level.Level level,
            Player player,
            Map<Item, Integer> required) {

        GTCEUTerminalMod.LOGGER.info("=== Attempting Item Extraction ===");
        GTCEUTerminalMod.LOGGER.info("Player: {}", player.getName().getString());
        GTCEUTerminalMod.LOGGER.info("Items needed: {}", required.size());

        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            GTCEUTerminalMod.LOGGER.info("  - {} x{}",
                    entry.getKey().getDescription().getString(), entry.getValue());
        }

        if (WirelessTerminalHandler.isLinked(stack)) {
            GTCEUTerminalMod.LOGGER.info("Wireless terminal is LINKED, checking ME Network...");

            IGrid grid = WirelessTerminalHandler.getLinkedGrid(stack, level, player);

            if (grid != null) {
                GTCEUTerminalMod.LOGGER.info("Grid connection: ACTIVE");

                if (extractItems(grid, required, player)) {
                    GTCEUTerminalMod.LOGGER.info("✓ SUCCESS: Extracted from ME Network");
                    return new ExtractResult(true, ExtractionSource.ME_NETWORK);
                } else {
                    GTCEUTerminalMod.LOGGER.info("✗ FAILED: ME Network doesn't have required items");
                }
            } else {
                GTCEUTerminalMod.LOGGER.warn("Grid connection: FAILED (out of range or not powered?)");
            }
        } else {
            GTCEUTerminalMod.LOGGER.info("Wireless terminal is NOT LINKED");
        }

        GTCEUTerminalMod.LOGGER.info("Trying player inventory as fallback...");

        if (extractFromPlayerInventory(player, required)) {
            GTCEUTerminalMod.LOGGER.info("✓ SUCCESS: Extracted from Player Inventory");
            return new ExtractResult(true, ExtractionSource.PLAYER_INVENTORY);
        }

        GTCEUTerminalMod.LOGGER.warn("✗ FAILED: Not enough items in inventory or ME Network");
        return new ExtractResult(false, ExtractionSource.NONE);
    }

    private static boolean extractFromPlayerInventory(Player player, Map<Item, Integer> required) {
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            int count = countItemInInventory(player, entry.getKey());
            if (count < entry.getValue()) {
                GTCEUTerminalMod.LOGGER.info("  Missing {}: need {}, have {}",
                        entry.getKey().getDescription().getString(),
                        entry.getValue(),
                        count);
                return false;
            }
        }

        int totalExtracted = 0;
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            Item item = entry.getKey();
            int remaining = entry.getValue();

            for (ItemStack invStack : player.getInventory().items) {
                if (remaining <= 0) break;

                if (invStack.getItem() == item) {
                    int toRemove = Math.min(remaining, invStack.getCount());
                    invStack.shrink(toRemove);
                    remaining -= toRemove;
                    totalExtracted += toRemove;
                }
            }

            if (remaining > 0) {
                return false;
            }

            GTCEUTerminalMod.LOGGER.info("  ✓ Extracted {} x{} from inventory",
                    item.getDescription().getString(), entry.getValue());
        }

        GTCEUTerminalMod.LOGGER.info("Total extracted: {} items from inventory", totalExtracted);
        return true;
    }

    private static int countItemInInventory(Player player, Item item) {
        int count = 0;

        for (ItemStack invStack : player.getInventory().items) {
            if (invStack.getItem() == item) {
                count += invStack.getCount();
            }
        }

        return count;
    }

    public static class ExtractResult {
        public final boolean success;
        public final ExtractionSource source;

        public ExtractResult(boolean success, ExtractionSource source) {
            this.success = success;
            this.source = source;
        }
    }

    public enum ExtractionSource {
        ME_NETWORK,
        PLAYER_INVENTORY,
        NONE
    }
}