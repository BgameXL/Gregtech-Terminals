package com.gtceuterminal.common.ae2;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.util.DimensionalBlockPos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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

    public static boolean isOurLinkedTerminal(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof com.gtceuterminal.common.item.MultiStructureManagerItem
                    || stack.getItem() instanceof com.gtceuterminal.common.item.SchematicInterfaceItem)
                && isLinked(stack);
    }

    public static boolean isOurLinkedManager(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof com.gtceuterminal.common.item.MultiStructureManagerItem
                && isLinked(stack);
    }

    @Nullable
    public static IGrid getLinkedGrid(ItemStack stack, Level level, Player player) {
        if (!isLinked(stack)) {
            return null;
        }

        GlobalPos globalPos = getLinkedPosition(stack);
        if (globalPos == null) {
            return null;
        }

        Level wapLevel = resolveLevel(level, player, globalPos);
        if (wapLevel == null) {
            return null;
        }

        BlockEntity be = wapLevel.getBlockEntity(globalPos.pos());
        if (!(be instanceof IWirelessAccessPoint linkedWap)) {
            return null;
        }

        IGrid grid = linkedWap.getGrid();
        if (grid == null) {
            return null;
        }

        return anyAccessPointInRange(grid, player) ? grid : null;
    }

    @Nullable
    private static Level resolveLevel(Level fallback, Player player, GlobalPos globalPos) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            ServerLevel sl = server.getLevel(globalPos.dimension());
            if (sl != null) return sl;
        }
        return fallback != null && fallback.dimension().equals(globalPos.dimension()) ? fallback : null;
    }

    private static boolean anyAccessPointInRange(IGrid grid, Player player) {
        for (Class<?> cls : grid.getMachineClasses()) {
            if (!IWirelessAccessPoint.class.isAssignableFrom(cls)) continue;
            for (Object machine : grid.getMachines(cls)) {
                if (machine instanceof IWirelessAccessPoint wap && isWapInRange(wap, player)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isWapInRange(IWirelessAccessPoint wap, Player player) {
        if (!wap.isActive()) return false;
        DimensionalBlockPos loc = wap.getLocation();
        if (loc == null || loc.getLevel() != player.level()) return false;
        double range = wap.getRange();
        BlockPos pos = loc.getPos();
        double dx = pos.getX() - player.getX();
        double dy = pos.getY() - player.getY();
        double dz = pos.getZ() - player.getZ();
        return (dx * dx + dy * dy + dz * dz) < range * range;
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
