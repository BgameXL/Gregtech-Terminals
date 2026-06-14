package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.gui.factory.DismantlerItemUIFactory;
import com.gtceuterminal.client.gui.widget.WallpaperWidget;
import com.gtceuterminal.common.multiblock.DismantleScanner;
import com.gtceuterminal.common.network.CPacketDismantle;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DismantlerUI {

    private static final int GUI_W    = 600;
    private static final int GUI_H    = 480;
    private static final int HEADER_H = 30;
    private static final int INFO_H   = 40;
    private static final int FOOTER_H = 34;
    private static final int PAD      = 10;
    private static final int SPLIT_X  = 255;

    private static final int CONTENT_Y = 2 + HEADER_H + 4;
    private static final int CONTENT_H = GUI_H - CONTENT_Y - INFO_H - FOOTER_H - 6;
    private static final int INFO_Y    = CONTENT_Y + CONTENT_H + 4;
    private static final int FOOTER_Y  = GUI_H - FOOTER_H - 2;

    private static final int C_BORDER_DARK = 0xFF0A0A0A;

    private final IUIHolder uiHolder;
    private final ItemStack itemStack;
    private final BlockPos  controllerPos;
    private final Player    player;
    private final ItemTheme theme;
    private final int colorBgLight, colorBgMedium, colorBorderLight;

    private final DismantleScanner.ScanResult scanResult;
    private WidgetGroup mainGroup;

    public DismantlerUI(HeldItemUIFactory.HeldItemHolder heldHolder, BlockPos controllerPos) {
        this.uiHolder      = heldHolder;
        this.itemStack     = heldHolder.held;
        this.controllerPos = controllerPos;
        this.player        = heldHolder.player;
        this.theme         = ItemTheme.load(this.itemStack);
        this.colorBgLight    = theme.isNativeStyle() ? 0xFF3A3A3A : theme.accent(0xAA);
        this.colorBgMedium   = theme.panelColor;
        this.colorBorderLight = theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF);
        this.scanResult    = computeScanResult(player, controllerPos);
    }

    public DismantlerUI(DismantlerItemUIFactory.Holder holder, Player player) {
        this.uiHolder      = holder;
        this.itemStack     = holder.item;
        this.controllerPos = holder.controllerPos;
        this.player        = player;
        this.theme         = ItemTheme.load(this.itemStack);
        this.colorBgLight    = theme.isNativeStyle() ? 0xFF3A3A3A : theme.accent(0xAA);
        this.colorBgMedium   = theme.panelColor;
        this.colorBorderLight = theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF);
        this.scanResult    = buildScanResult(player, holder.controllerPos, holder.scannedPositions);
    }

    private static DismantleScanner.ScanResult buildScanResult(
            Player player, BlockPos controllerPos, java.util.Set<BlockPos> preScanned) {
        if (!preScanned.isEmpty()) {
            var level = player.level();
            Map<Block, Integer> counts = new HashMap<>();
            Map<Block, List<BlockPos>> positions = new HashMap<>();
            for (BlockPos pos : preScanned) {
                var state = level.getBlockState(pos);
                if (state.isAir()) continue;
                var block = state.getBlock();
                counts.merge(block, 1, Integer::sum);
                positions.computeIfAbsent(block, k -> new ArrayList<>()).add(pos);
            }
            return new DismantleScanner.ScanResult(controllerPos, preScanned, counts, positions);
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

    public ModularUI createUI() {
        this.mainGroup = new WidgetGroup(0, 0, GUI_W, GUI_H);
        mainGroup.setBackground(new ColorRectTexture(0x00000000));

        if (!theme.isNativeStyle()) {
            mainGroup.addWidget(new WallpaperWidget(0, 0, GUI_W, GUI_H, () -> this.theme));
        }

        mainGroup.addWidget(buildOuterBorder());
        mainGroup.addWidget(DismantlerHeader.build(
                GUI_W, HEADER_H, controllerPos, itemStack, theme, mainGroup, player, this::closeUI));

        int previewW = SPLIT_X - PAD - 4;
        mainGroup.addWidget(DismantlerPreviewPanel.build(
                PAD, CONTENT_Y, previewW, CONTENT_H,
                colorBgLight, C_BORDER_DARK,
                scanResult, controllerPos, player));

        mainGroup.addWidget(DismantlerBlockListPanel.build(
                SPLIT_X, CONTENT_Y, GUI_W - SPLIT_X - PAD, CONTENT_H,
                colorBgLight, C_BORDER_DARK, scanResult));

        mainGroup.addWidget(DismantlerInfoBar.build(
                PAD, INFO_Y, GUI_W - PAD * 2, INFO_H,
                colorBgMedium, C_BORDER_DARK, scanResult, player));

        mainGroup.addWidget(DismantlerFooter.build(
                PAD, FOOTER_Y, GUI_W - PAD * 2, FOOTER_H,
                colorBgMedium, colorBorderLight,
                this::performDismantle, this::closeUI));

        setupParade();
        return buildModularUI(mainGroup);
    }

    private void setupParade() {
        com.gtceuterminal.client.ClientEvents.clearActiveParade();
        if (!theme.isBundleStyle()) return;
        com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
        if (bundle == null) return;
        com.gtceuterminal.client.gui.widget.MultiblockParadeWidget parade =
                bundle.createParadeWidget(0, 0, GUI_W, GUI_H);
        if (parade == null || parade.isEmpty()) return;
        parade.setGuiCenter(GUI_W / 2f, GUI_H / 2f);
        com.gtceuterminal.client.ClientEvents.setActiveParade(parade, theme.paradeMode);
    }

    private WidgetGroup buildOuterBorder() {
        WidgetGroup g = new WidgetGroup(0, 0, GUI_W, GUI_H);
        g.addWidget(new ImageWidget(0,         0,          GUI_W, 2,     new ColorRectTexture(colorBorderLight)));
        g.addWidget(new ImageWidget(0,         0,          2,     GUI_H, new ColorRectTexture(colorBorderLight)));
        g.addWidget(new ImageWidget(GUI_W - 2, 0,          2,     GUI_H, new ColorRectTexture(C_BORDER_DARK)));
        g.addWidget(new ImageWidget(0,         GUI_H - 2,  GUI_W, 2,     new ColorRectTexture(C_BORDER_DARK)));
        return g;
    }

    private ModularUI buildModularUI(WidgetGroup content) {
        ModularUI ui = new ModularUI(new Size(GUI_W, GUI_H), uiHolder, player);
        ui.widget(content);
        ui.background(theme.modularUIBackground());
        return ui;
    }

    private void performDismantle() {
        GTCEUTerminalMod.LOGGER.info("Dismantling multiblock at {}", controllerPos);
        TerminalNetwork.CHANNEL.sendToServer(new CPacketDismantle(controllerPos));
        closeUI();
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.dismantler.chat.success"), false);
    }

    private void closeUI() {
        if (player.containerMenu != null) player.closeContainer();
    }

    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder, BlockPos controllerPos) {
        return new DismantlerUI(heldHolder, controllerPos).createUI();
    }

    public static ModularUI create(DismantlerItemUIFactory.Holder holder, Player player) {
        return new DismantlerUI(holder, player).createUI();
    }
}