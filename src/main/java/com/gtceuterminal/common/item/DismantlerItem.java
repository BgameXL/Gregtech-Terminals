package com.gtceuterminal.common.item;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gtceuterminal.client.item.ClientTooltipHelper;
import com.gtceuterminal.common.ae2.MENetworkScanner;
import com.gtceuterminal.common.network.SPacketOpenDismantlerUI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DismantlerItem extends Item {

    public DismantlerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();

        BlockEntity be = level.getBlockEntity(clickedPos);

        if (be instanceof IMachineBlockEntity mbe) {
            var metaMachine = mbe.getMetaMachine();
            if (metaMachine instanceof MultiblockControllerMachine controller && controller.isFormed()) {
                if (!level.isClientSide && player instanceof ServerPlayer sp) {
                    SPacketOpenDismantlerUI.openFor(sp, context.getItemInHand(), clickedPos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        if (!level.isClientSide) {
            player.displayClientMessage(
                    Component.translatable("item.gtceuterminal.dismantler.message.not_formed_controller"),
                    true
            );
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Level level,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(Component.translatable(
                "item.gtceuterminal.dismantler.tooltip.tool"
        ));
        tooltipComponents.add(Component.literal(""));
        if (MENetworkScanner.isAE2Available()) {
            if (MENetworkScanner.isItemLinked(stack)) {
                tooltipComponents.add(Component.translatable(
                        "item.gtceuterminal.dismantler.tooltip.linked"
                ));
                if (level != null && level.isClientSide) {
                    ClientTooltipHelper.appendAE2RangeTooltip(stack, level, tooltipComponents);
                }
            } else {
                tooltipComponents.add(Component.translatable(
                        "item.gtceuterminal.dismantler.tooltip.not_linked"
                ));
                tooltipComponents.add(Component.translatable(
                        "item.gtceuterminal.dismantler.tooltip.place_in_me_wireless_access_point"
                ));
            }
            tooltipComponents.add(Component.literal(""));
        }
        tooltipComponents.add(Component.translatable(
                "item.gtceuterminal.dismantler.tooltip.open_dismantler_gui"
        ));
    }
}