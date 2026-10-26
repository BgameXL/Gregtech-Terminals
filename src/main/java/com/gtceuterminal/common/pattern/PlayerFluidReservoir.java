package com.gtceuterminal.common.pattern;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import org.jetbrains.annotations.Nullable;

public final class PlayerFluidReservoir {

    private final Player player;
    private final IFluidHandler me;

    public PlayerFluidReservoir(Player player, @Nullable IFluidHandler me) {
        this.player = player;
        this.me = me;
    }

    public boolean extract(Fluid fluid, int mb) {
        if (fluid == null || mb <= 0) return false;
        FluidStack req = new FluidStack(fluid, mb);

        if (me != null && me.drain(req, IFluidHandler.FluidAction.SIMULATE).getAmount() >= mb) {
            me.drain(req, IFluidHandler.FluidAction.EXECUTE);
            return true;
        }

        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            ItemStack one = s.copy();
            one.setCount(1);
            IFluidHandlerItem h = FluidUtil.getFluidHandler(one).orElse(null);
            if (h == null) continue;
            if (h.drain(req, IFluidHandler.FluidAction.SIMULATE).getAmount() < mb) continue;
            h.drain(req, IFluidHandler.FluidAction.EXECUTE);
            replaceOne(i, s, h.getContainer());
            return true;
        }
        return false;
    }

    public int insert(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        FluidStack remaining = stack.copy();

        if (me != null) {
            int accepted = me.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
            remaining.shrink(accepted);
            if (remaining.isEmpty()) return 0;
        }

        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            ItemStack one = s.copy();
            one.setCount(1);
            IFluidHandlerItem h = FluidUtil.getFluidHandler(one).orElse(null);
            if (h == null) continue;
            int filled = h.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
            if (filled <= 0) continue;
            replaceOne(i, s, h.getContainer());
            remaining.shrink(filled);
        }
        return remaining.getAmount();
    }

    private void replaceOne(int slot, ItemStack original, ItemStack result) {
        if (original.getCount() == 1) {
            player.getInventory().setItem(slot, result);
            return;
        }
        original.shrink(1);
        player.getInventory().setItem(slot, original);
        if (result != null && !result.isEmpty() && !player.getInventory().add(result)) {
            player.drop(result, false);
        }
    }
}
