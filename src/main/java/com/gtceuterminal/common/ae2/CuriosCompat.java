package com.gtceuterminal.common.ae2;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

public class CuriosCompat {
    public static List<ItemStack> getEquippedItems(Player player) {
        List<ItemStack> result = new ArrayList<>();
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            for (int i = 0; i < handler.getEquippedCurios().getSlots(); i++) {
                ItemStack stack = handler.getEquippedCurios().getStackInSlot(i);
                if (!stack.isEmpty()) result.add(stack);
            }
        });
        return result;
    }
}
