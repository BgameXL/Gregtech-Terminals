package com.gtceuterminal.common.ae2;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class WirelessTerminalHandler {

    private static final String TAG_ACCESS_POINT_POS = "accessPoint";
    public static boolean isWirelessTerminal(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof appeng.items.tools.powered.WirelessTerminalItem || isLinked(stack));
    }

    public static boolean isLinked(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_ACCESS_POINT_POS);
    }

    @Nullable
    public static IGrid getLinkedGrid(ItemStack stack, Level level, Player player) {
        if (stack.getItem() instanceof appeng.items.tools.powered.WirelessTerminalItem terminal) {
            IGrid grid = terminal.getLinkedGrid(stack, level, player);
            if (grid != null) return grid;
        }

        if (!isLinked(stack)) {
            return null;
        }

        GlobalPos globalPos = getLinkedPosition(stack);
        if (globalPos == null) {
            return null;
        }

        if (!level.dimension().equals(globalPos.dimension())) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(globalPos.pos());
        if (!(be instanceof IWirelessAccessPoint accessPoint)) {
            return null;
        }

        if (!accessPoint.isActive()) {
            return null;
        }

        double range = accessPoint.getRange();
        BlockPos playerPos = player.blockPosition();
        double distanceSq = globalPos.pos().distSqr(playerPos);

        if (distanceSq > (range * range)) {
            return null;
        }

        IGrid grid = accessPoint.getGrid();

        return grid;
    }

    public static boolean isInRange(ItemStack stack, Level level, Player player) {
        if (!isLinked(stack)) {
            return false;
        }

        GlobalPos globalPos = getLinkedPosition(stack);
        if (globalPos == null) {
            return false;
        }

        if (!level.dimension().equals(globalPos.dimension())) {
            return false;
        }

        BlockEntity be = level.getBlockEntity(globalPos.pos());
        if (!(be instanceof IWirelessAccessPoint accessPoint)) {
            return false;
        }

        if (!accessPoint.isActive()) {
            return false;
        }

        double range = accessPoint.getRange();
        BlockPos playerPos = player.blockPosition();
        double distanceSq = globalPos.pos().distSqr(playerPos);

        return distanceSq <= (range * range);
    }

    // Get the linked position from NBT
    @Nullable
    private static GlobalPos getLinkedPosition(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ACCESS_POINT_POS)) {
            return null;
        }

        return GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_ACCESS_POINT_POS))
                .result()
                .orElse(null);
    }
}
