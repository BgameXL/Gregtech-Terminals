package com.gtceuterminal.common.ae2;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MENetworkExtractor {

    @Nullable
    public static ItemStack tryExtractCandidateFromLinkedTerminal(
            List<ItemStack> candidates, Player player) {

        if (!MENetworkScanner.isAE2Available()) return null;

        try {
            List<ItemStack> toCheck = new java.util.ArrayList<>();
            toCheck.add(player.getMainHandItem());
            toCheck.add(player.getOffhandItem());
            toCheck.addAll(player.getInventory().items);

            for (ItemStack stack : toCheck) {
                if (stack.isEmpty()) continue;
                if (!(stack.getItem() instanceof appeng.items.tools.powered.WirelessTerminalItem terminalItem)) continue;
                if (!WirelessTerminalHandler.isLinked(stack)) continue;

                appeng.api.networking.IGrid grid =
                        terminalItem.getLinkedGrid(stack, player.level(), player);
                if (grid == null) continue;

                var storage = grid.getStorageService().getInventory();
                for (ItemStack candidate : candidates) {
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