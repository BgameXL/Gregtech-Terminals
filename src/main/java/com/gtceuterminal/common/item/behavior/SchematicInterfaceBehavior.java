package com.gtceuterminal.common.item.behavior;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.util.SchematicUtils;

import com.gtceuterminal.client.gui.factory.SchematicItemUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;

public class SchematicInterfaceBehavior {

    public InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level     level    = context.getLevel();
        BlockPos  blockPos = context.getClickedPos();
        ItemStack stack    = context.getItemInHand();

        if (player.isShiftKeyDown()) {
            MetaMachine machine = MetaMachine.getMachine(level, blockPos);
            if (machine instanceof IMultiController controller && controller.isFormed()) {
                if (!level.isClientSide) {
                    SchematicCopier.copyMultiblock(controller, stack, player, level);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                GTCEUTerminalMod.LOGGER.info("Server: opening Schematic UI");
                SchematicItemUIFactory.INSTANCE.openUI(sp, stack);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            BlockPos anchor = blockPos;
            BlockState clicked = level.getBlockState(blockPos);
            if (clicked != null && !clicked.isAir() && !clicked.canBeReplaced())
                anchor = blockPos.relative(context.getClickedFace());
            SchematicPaster.pasteSchematic(stack, player, level, anchor);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                GTCEUTerminalMod.LOGGER.info("Server: opening Schematic UI");
                SchematicItemUIFactory.INSTANCE.openUI(sp, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (hasClipboard(stack) && !level.isClientSide) {
            CompoundTag itemTag   = stack.getTag();
            SchematicData clipboard = SchematicData.fromNBT(
                    itemTag.getCompound("Clipboard"), level.registryAccess());
            double   distance  = SchematicUtils.calculateOptimalDistance(clipboard);
            BlockPos targetPos = SchematicUtils.getTargetPlacementPos(player, distance);
            SchematicPaster.pasteSchematic(stack, player, level, targetPos);
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    private boolean hasClipboard(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Clipboard")) return false;
        CompoundTag clip = tag.getCompound("Clipboard");
        return clip.contains("Blocks") && !clip.getList("Blocks", 10).isEmpty();
    }
}