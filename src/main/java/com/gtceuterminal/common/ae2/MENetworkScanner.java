package com.gtceuterminal.common.ae2;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.compat.AE2Reflection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class MENetworkScanner {

    private static boolean ae2Available = false;
    private static boolean ae2Checked = false;

    public static boolean isAE2Available() {
        if (!ae2Checked) {
            try {
                Class.forName("appeng.api.networking.IGrid"); // kept for isAE2Available check
                ae2Available = true;
                GTCEUTerminalMod.LOGGER.info("AE2 detected - ME Network integration enabled");
            } catch (ClassNotFoundException e) {
                ae2Available = false;
                GTCEUTerminalMod.LOGGER.info("AE2 not detected - ME Network integration disabled");
            }
            ae2Checked = true;
        }
        return ae2Available;
    }

    public static List<BlockPos> findNearbyMENodes(Player player, Level level, int radius) {
        List<BlockPos> nodes = new ArrayList<>();

        if (!isAE2Available()) {
            return nodes;
        }

        BlockPos playerPos = player.blockPosition();

        try {

            for (BlockPos pos : BlockPos.betweenClosed(
                    playerPos.offset(-radius, -radius, -radius),
                    playerPos.offset(radius, radius, radius)
            )) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null && isMENetworkBlock(be)) {
                    nodes.add(pos.immutable());
                }
            }

            GTCEUTerminalMod.LOGGER.debug("Found {} ME network nodes near player", nodes.size());

        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Error scanning for ME networks", e);
        }

        return nodes;
    }

    private static boolean isMENetworkBlock(BlockEntity be) {
        return AE2Reflection.isGridHost(be);
    }

    public static long countItemInMENetwork(Player player, Level level, BlockPos nodePos, Item item) {
        if (!isAE2Available()) {
            return 0;
        }

        try {
            BlockEntity be = level.getBlockEntity(nodePos);
            if (be == null) return 0;
            Object inventory = AE2Reflection.resolveInventory(be);
            if (inventory == null) return 0;
            ItemStack probe = new ItemStack(item);
            ItemStack found = AE2Reflection.extract(inventory, be, probe, Long.MAX_VALUE, true);
            long count = found != null ? found.getCount() : 0;
            GTCEUTerminalMod.LOGGER.debug("Found {} of {} in ME network at {}", count, item, nodePos);
            return count;
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Error counting items in ME network", e);
            return 0;
        }
    }

    public static long getTotalInMENetworks(Player player, Level level, Item item, int radius) {
        if (!isAE2Available()) {
            return 0;
        }

        List<BlockPos> nodes = findNearbyMENodes(player, level, radius);
        long total = 0;

        for (BlockPos nodePos : nodes) {
            long count = countItemInMENetwork(player, level, nodePos, item);
            if (count > 0) {
                total += count;
                break;
            }
        }

        return total;
    }

    public static boolean isItemLinked(net.minecraft.world.item.ItemStack stack) {
        if (!isAE2Available()) return false;
        return WirelessTerminalHandler.isLinked(stack);
    }

    public static boolean isItemInRange(net.minecraft.world.item.ItemStack stack,
                                        Level level,
                                        net.minecraft.world.entity.player.Player player) {
        if (!isAE2Available()) return false;
        return WirelessTerminalHandler.isInRange(stack, level, player);
    }
}