package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.widget.WallpaperWidget;
import com.gtceuterminal.common.gui.factory.DismantlerItemUIFactory;
import com.gtceuterminal.common.multiblock.DismantleScanner;
import com.gtceuterminal.common.network.CPacketDismantle;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.gtceuterminal.common.theme.ItemTheme;
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Size;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class DismantlerUI {

    private static final int GUI_W = 600;
    private static final int GUI_H = 480;
    private static final int HEADER_H = 30;
    private static final int BODY_Y = HEADER_H + 1;
    private static final int BODY_H = GUI_H - BODY_Y - 2;
    private static final int CARD_INSET = 2;
    private static final int INNER_W = GUI_W - 4 - CARD_INSET * 2;
    private static final int INNER_H = BODY_H - CARD_INSET * 2;
    private static final int LEFT_W = 240;
    private static final int DIVIDER = 1;
    private static final int RIGHT_X = CARD_INSET + LEFT_W + DIVIDER;
    private static final int RIGHT_W = INNER_W - LEFT_W - DIVIDER;
    private static final int CONTENT_H = 367;
    private static final int INFO_H = 40;
    private static final int FOOTER_H = 34;
    private static final int INFO_Y = CARD_INSET + CONTENT_H + DIVIDER;
    private static final int FOOTER_Y = INFO_Y + INFO_H + DIVIDER;

    private final IUIHolder uiHolder;
    private final ItemStack itemStack;
    private final BlockPos controllerPos;
    private final Player player;
    private final ItemTheme theme;

    private final DismantleScanner.ScanResult scanResult;
    private WidgetGroup mainGroup;

    public DismantlerUI(HeldItemUIFactory.HeldItemHolder heldHolder, BlockPos controllerPos) {
        this.uiHolder = heldHolder;
        this.itemStack = heldHolder.held;
        this.controllerPos = controllerPos;
        this.player = heldHolder.player;
        this.theme = ItemTheme.load(this.itemStack);
        TerminalUI.use(this.theme);
        this.scanResult = computeScanResult(player, controllerPos);
    }

    public DismantlerUI(DismantlerItemUIFactory.Holder holder, Player player) {
        this.uiHolder = holder;
        this.itemStack = holder.item;
        this.controllerPos = holder.controllerPos;
        this.player = player;
        this.theme = ItemTheme.load(this.itemStack);
        TerminalUI.use(this.theme);
        this.scanResult = buildScanResult(player, holder.controllerPos, holder.scannedPositions);
    }

    private static DismantleScanner.ScanResult buildScanResult(
            Player player, BlockPos controllerPos, java.util.Set<BlockPos> preScanned) {
        if (!preScanned.isEmpty()) {
            var level = player.level();
            Map<Block, Integer> counts = new HashMap<>();
            for (BlockPos pos : preScanned) {
                var state = level.getBlockState(pos);
                if (state.isAir()) continue;
                var block = state.getBlock();
                counts.merge(block, 1, Integer::sum);
            }
            return new DismantleScanner.ScanResult(controllerPos, preScanned, counts,
                    DismantleScanner.collectCoverStacks(level, preScanned));
        }
        return computeScanResult(player, controllerPos);
    }

    private static DismantleScanner.ScanResult computeScanResult(Player player, BlockPos pos) {
        try {
            var be = player.level().getBlockEntity(pos);
            if (be instanceof com.gregtechceu.gtceu.api.machine.IMachineBlockEntity mbe) {
                var meta = mbe.getMetaMachine();
                if (meta instanceof com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine ctrl) {
                    return DismantleScanner.scanMultiblock(player.level(), ctrl);
                }
            }
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.warn("DismantlerUI: could not scan multiblock at {}", pos, t);
        }
        return DismantleScanner.ScanResult.empty();
    }

    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder, BlockPos controllerPos) {
        return new DismantlerUI(heldHolder, controllerPos).createUI();
    }

    public static ModularUI create(DismantlerItemUIFactory.Holder holder, Player player) {
        return new DismantlerUI(holder, player).createUI();
    }

    public ModularUI createUI() {
        this.mainGroup = new WidgetGroup(0, 0, GUI_W, GUI_H);
        mainGroup.setBackground(TerminalUI.transparent());

        if (!theme.isNativeStyle()) {
            mainGroup.addWidget(new WallpaperWidget(2, BODY_Y, GUI_W - 4, BODY_H, () -> this.theme));
        }

        mainGroup.addWidget(DismantlerHeader.build(
                GUI_W, HEADER_H, controllerPos, itemStack, theme, mainGroup, this::closeUI));

        WidgetGroup body = new WidgetGroup(2, BODY_Y, GUI_W - 4, BODY_H);
        body.setBackground(TerminalUI.cardBG());

        body.addWidget(DismantlerPreviewPanel.build(
                CARD_INSET, CARD_INSET, LEFT_W, CONTENT_H,
                scanResult, controllerPos, player));

        body.addWidget(DismantlerBlockListPanel.build(
                RIGHT_X, CARD_INSET, RIGHT_W, CONTENT_H - 2, scanResult));

        body.addWidget(new ImageWidget(CARD_INSET, CARD_INSET + CONTENT_H, INNER_W, DIVIDER,
                TerminalUI.divider()));

        body.addWidget(DismantlerInfoBar.build(
                CARD_INSET, INFO_Y, INNER_W, INFO_H, scanResult, player));

        body.addWidget(new ImageWidget(CARD_INSET, INFO_Y + INFO_H, INNER_W, DIVIDER,
                TerminalUI.divider()));

        body.addWidget(DismantlerFooter.build(
                CARD_INSET, FOOTER_Y, INNER_W, FOOTER_H,
                this::performDismantle, this::closeUI));
        mainGroup.addWidget(body);
        return buildModularUI(mainGroup);
    }

    private ModularUI buildModularUI(WidgetGroup content) {
        ModularUI ui = new ModularUI(new Size(GUI_W, GUI_H), uiHolder, player);
        ui.widget(content);
        ui.background(TerminalUI.transparent());
        return ui;
    }

    private void performDismantle() {
        GTCEUTerminalMod.LOGGER.debug("Dismantling multiblock at {}", controllerPos);
        TerminalNetwork.CHANNEL.sendToServer(new CPacketDismantle(controllerPos));
        closeUI();
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.dismantler.chat.success"), false);
    }

    private void closeUI() {
        if (player.containerMenu != null) player.closeContainer();
    }
}
