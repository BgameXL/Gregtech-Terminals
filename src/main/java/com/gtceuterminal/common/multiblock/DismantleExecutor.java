package com.gtceuterminal.common.multiblock;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import com.gtceuterminal.common.config.ItemsConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DismantleExecutor {


    // Dismantle the multiblock and give items to the player
    public static boolean dismantleMultiblock(ServerLevel level,
                                              ServerPlayer player,
                                              MultiblockControllerMachine controller) {
        DismantleScanner.ScanResult scanResult = DismantleScanner.scanMultiblock(level, controller);

        List<ItemStack> items = new ArrayList<>();
        BlockPos controllerPos = controller.getPos();

        Set<BlockPos> skipPositions = new HashSet<>();
        for (BlockPos pos : scanResult.getAllBlocks()) {
            BlockState st = level.getBlockState(pos);
            if (st.getBlock() instanceof DoorBlock
                    && st.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                skipPositions.add(pos);
            }
        }

        for (BlockPos pos : scanResult.getAllBlocks()) {
            if (pos.equals(controllerPos)) continue;
            if (skipPositions.contains(pos)) continue;
            BlockState bsCheck = level.getBlockState(pos);
            if (ItemsConfig.isDismantlerBlacklisted(bsCheck.getBlock())) continue;
            ItemStack refund = createRefundStack(level, pos);
            mergeInto(items, refund);
        }

        BlockState controllerBs = level.getBlockState(controllerPos);
        if (!ItemsConfig.isDismantlerBlacklisted(controllerBs.getBlock())) {
            ItemStack controllerRefund = createRefundStack(level, controllerPos);
            mergeInto(items, controllerRefund);
        }

        for (BlockPos pos : scanResult.getAllBlocks()) {
            if (pos.equals(controllerPos)) continue;
            BlockState bsBreak = level.getBlockState(pos);
            if (ItemsConfig.isDismantlerBlacklisted(bsBreak.getBlock())) continue;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }

        BlockState controllerBreakBs = level.getBlockState(controllerPos);
        if (!ItemsConfig.isDismantlerBlacklisted(controllerBreakBs.getBlock())) {
            level.setBlock(controllerPos, Blocks.AIR.defaultBlockState(), 3);
        }

        for (ItemStack stack : items) {
            if (!player.getInventory().add(stack)) {
                ItemEntity itemEntity = new ItemEntity(
                        level,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        stack.copy()
                );
                level.addFreshEntity(itemEntity);
            }
        }

        return true;
    }

    private static ItemStack createRefundStack(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return ItemStack.EMPTY;

        if (state.getBlock().asItem() == Items.AIR) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(state.getBlock().asItem());
        if (stack.isEmpty()) return ItemStack.EMPTY;

        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            try {
                be.saveToItem(stack);

                cleanNBTForStacking(stack);
            } catch (Throwable ignored) {}
        }
        return stack;
    }

    private static void cleanNBTForStacking(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
        if (blockEntityTag.isEmpty()) return;

        CompoundTag cleanTag = new CompoundTag();

        if (blockEntityTag.contains("CoverContainer")) {
            cleanTag.put("CoverContainer", blockEntityTag.get("CoverContainer").copy());
        }
        if (blockEntityTag.contains("Covers")) {
            cleanTag.put("Covers", blockEntityTag.get("Covers").copy());
        }

        if (blockEntityTag.contains("Upgrades")) {
            cleanTag.put("Upgrades", blockEntityTag.get("Upgrades").copy());
        }

        if (blockEntityTag.contains("WorkingEnabled")) {
            cleanTag.putBoolean("WorkingEnabled", blockEntityTag.getBoolean("WorkingEnabled"));
        }

        if (blockEntityTag.contains("Tier")) {
            cleanTag.putInt("Tier", blockEntityTag.getInt("Tier"));
        }
        if (blockEntityTag.contains("Material")) {
            cleanTag.putString("Material", blockEntityTag.getString("Material"));
        }

        if (blockEntityTag.contains("CustomName")) {
            cleanTag.putString("CustomName", blockEntityTag.getString("CustomName"));
        }

        if (!cleanTag.isEmpty()) {
            tag.put("BlockEntityTag", cleanTag);
        } else {
            tag.remove("BlockEntityTag");
        }
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    // Combines identical stacks (same item + same NBT) respecting maxStackSize.
    private static void mergeInto(List<ItemStack> out, ItemStack in) {
        if (in == null || in.isEmpty()) return;

        while (!in.isEmpty()) {
            boolean merged = false;

            for (ItemStack existing : out) {
                if (!existing.isEmpty()
                        && sameItemAndTag(existing, in)
                        && existing.getCount() < existing.getMaxStackSize()) {

                    int canMove = existing.getMaxStackSize() - existing.getCount();
                    int toMove = Math.min(canMove, in.getCount());

                    existing.grow(toMove);
                    in.shrink(toMove);

                    merged = true;
                    if (in.isEmpty()) return;
                }
            }

            if (!merged) {
                int take = Math.min(in.getMaxStackSize(), in.getCount());
                ItemStack part = in.copy();
                part.setCount(take);
                out.add(part);
                in.shrink(take);
            }
        }
    }

    private static boolean sameItemAndTag(ItemStack a, ItemStack b) {
        if (a.getItem() != b.getItem()) return false;
        return Objects.equals(a.getTag(), b.getTag());
    }

    public static int getAvailableInventorySlots(ServerPlayer player) {
        int emptySlots = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                emptySlots++;
            }
        }
        return emptySlots;
    }
}