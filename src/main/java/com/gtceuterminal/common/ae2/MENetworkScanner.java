package com.gtceuterminal.common.ae2;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MENetworkScanner {

    private static boolean ae2Available = false;
    private static boolean ae2Checked = false;

    public static boolean isAE2Available() {
        if (!ae2Checked) {
            try {
                Class.forName("appeng.api.networking.IGrid");
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

    public static boolean isItemLinked(ItemStack stack) {
        if (!isAE2Available()) return false;
        return WirelessTerminalHandler.isLinked(stack);
    }

    public static boolean isItemInRange(ItemStack stack, Level level, Player player) {
        if (!isAE2Available()) return false;
        return WirelessTerminalHandler.isInRange(stack, level, player);
    }
}