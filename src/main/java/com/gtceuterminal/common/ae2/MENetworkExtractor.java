package com.gtceuterminal.common.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.util.MiscUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MENetworkExtractor {

    @Nullable
    public static ItemStack tryExtractCandidateFromLinkedTerminal(
            List<ItemStack> candidates, Player player) {

        if (!MENetworkScanner.isAE2Available()) return null;

        try {
            List<ItemStack> toCheck = new ArrayList<>();
            toCheck.add(player.getMainHandItem());
            toCheck.add(player.getOffhandItem());
            toCheck.addAll(player.getInventory().items);
            if (MiscUtil.isCuriosLoaded) toCheck.addAll(CuriosCompat.getEquippedItems(player));

            for (ItemStack stack : toCheck) {
                if (stack.isEmpty()) continue;
                if (!WirelessTerminalHandler.isOurLinkedManager(stack)) continue;

                IGrid grid = WirelessTerminalHandler.getLinkedGrid(stack, player.level(), player);
                if (grid == null) continue;

                var storage = grid.getStorageService().getInventory();
                for (ItemStack candidate : candidates) {
                    long extracted = storage.extract(
                            AEItemKey.of(candidate), 1,
                            Actionable.MODULATE, null);
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