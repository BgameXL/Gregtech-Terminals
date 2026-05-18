package com.gtceuterminal.common.ae2;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.compat.AE2Reflection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MENetworkExtractor {

    public static long extractFromMENetwork(Player player, Level level, BlockPos nodePos, Item item, long amount) {
        if (!MENetworkScanner.isAE2Available()) {
            return 0;
        }

        try {
            BlockEntity be = level.getBlockEntity(nodePos);
            if (be == null) return 0;

            Object inventory = AE2Reflection.resolveInventory(be);
            if (inventory == null) return 0;

            ItemStack toExtract = new ItemStack(item, 1);
            ItemStack extracted = AE2Reflection.extract(inventory, be, toExtract, amount, false);
            if (extracted == null || extracted.isEmpty()) return 0;

            long count = extracted.getCount();
            long rem = count;
            while (rem > 0) {
                int batch = (int) Math.min(rem, 64);
                ItemStack batch_stack = new ItemStack(item, batch);
                if (!player.getInventory().add(batch_stack)) player.drop(batch_stack, false);
                rem -= batch;
            }
            return count;

        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Error extracting items from ME network", e);
            return 0;
        }
    }

    public static long extractFromNearbyMENetwork(Player player, Level level, Item item, long amount, int radius) {
        if (!MENetworkScanner.isAE2Available()) {
            return 0;
        }

        var nodes = MENetworkScanner.findNearbyMENodes(player, level, radius);

        for (BlockPos nodePos : nodes) {
            long extracted = extractFromMENetwork(player, level, nodePos, item, amount);
            if (extracted > 0) {
                return extracted;
            }
        }

        return 0;
    }

    @org.jetbrains.annotations.Nullable
    public static net.minecraft.world.item.ItemStack tryExtractCandidateFromLinkedTerminal(
            java.util.List<net.minecraft.world.item.ItemStack> candidates,
            Player player) {

        if (!MENetworkScanner.isAE2Available()) return null;

        try {
            java.util.List<net.minecraft.world.item.ItemStack> toCheck = new java.util.ArrayList<>();
            toCheck.add(player.getMainHandItem());
            toCheck.add(player.getOffhandItem());
            for (net.minecraft.world.item.ItemStack s : player.getInventory().items) toCheck.add(s);

            for (net.minecraft.world.item.ItemStack stack : toCheck) {
                if (stack.isEmpty()) continue;
                if (!(stack.getItem() instanceof appeng.items.tools.powered.WirelessTerminalItem terminalItem)) continue;
                if (!WirelessTerminalHandler.isLinked(stack)) continue;

                appeng.api.networking.IGrid grid =
                        terminalItem.getLinkedGrid(stack, player.level(), player);
                if (grid == null) continue;

                var storage = grid.getStorageService().getInventory();
                for (net.minecraft.world.item.ItemStack candidate : candidates) {
                    long extracted = storage.extract(
                            appeng.api.stacks.AEItemKey.of(candidate), 1,
                            appeng.api.config.Actionable.MODULATE, null);
                    if (extracted > 0) {
                        return candidate.copy();
                    }
                }
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("MENetworkExtractor.tryExtractCandidateFromLinkedTerminal failed", e);
        }
        return null;
    }
}