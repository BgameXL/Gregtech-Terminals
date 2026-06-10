package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
import com.gtceuterminal.client.gui.widget.HeaderItemIcon;
import com.gtceuterminal.client.gui.widget.WallpaperWidget;
import com.gtceuterminal.client.highlight.MultiblockHighlighter;
import com.gtceuterminal.common.config.ItemsConfig;
import com.gtceuterminal.common.gui.factory.MultiStructureManagerUIFactory;
import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.multiblock.MultiblockScanner;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MultiStructureManagerUI {

    private static final int MARGIN      = 16;
    private static final int HEADER_H    = 30;
    private static final int LEFT_W      = 210;
    private static final int DIVIDER_W   = 1;
    private static final int SCAN_RADIUS = 32;

    private static final int C_BORDER_DARK = 0xFF0A0A0A;
    private static final int C_TEXT_WHITE  = 0xFFFFFFFF;
    private static final int C_TEXT_GRAY   = 0xFFAAAAAA;
    private static final int C_HOVER       = 0x40FFFFFF;

    private int GUI_W, GUI_H;
    private int colorBgDark, colorBgMedium, colorBgLight, colorBorderLight;

    private final IUIHolder  uiHolder;
    private final Player     player;
    private final ItemStack  terminalItem;
    private final ItemTheme  theme;

    private List<MultiblockInfo> multiblocks = new ArrayList<>();
    private int selectedIndex = -1;

    private ModularUI gui;
    private WidgetGroup rootGroup;
    private WidgetGroup bodyGroup;
    private DraggableScrollableWidgetGroup listScroll;
    private WidgetGroup detailPanel;

    private int rightX, rightW, bodyH;

    public MultiStructureManagerUI(HeldItemUIFactory.HeldItemHolder heldHolder) {
        this.uiHolder     = heldHolder;
        this.player       = heldHolder.player;
        this.terminalItem = heldHolder.held;
        this.theme        = ItemTheme.load(this.terminalItem);
        applyThemeColors();
        scanMultiblocks();
    }

    public MultiStructureManagerUI(MultiStructureManagerUIFactory.Holder holder, Player player) {
        this.uiHolder     = holder;
        this.player       = player;
        this.terminalItem = holder.getTerminalItem();
        this.theme        = ItemTheme.load(this.terminalItem);
        applyThemeColors();
        scanMultiblocks();
    }

    @Deprecated
    public MultiStructureManagerUI(IUIHolder holder, Player player) {
        this.uiHolder     = holder;
        this.player       = player;
        this.terminalItem = ItemStack.EMPTY;
        this.theme        = ItemTheme.loadFromPlayer(player);
        applyThemeColors();
        scanMultiblocks();
    }

    private void applyThemeColors() {
        colorBgDark      = theme.bgColor;
        colorBgMedium    = theme.panelColor;
        colorBgLight     = theme.isNativeStyle() ? 0xFF3A3A3A : theme.accent(0xAA);
        colorBorderLight = theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF);
    }

    private void scanMultiblocks() {
        this.multiblocks = MultiblockScanner.scanNearbyMultiblocks(player, player.level(), SCAN_RADIUS);
        GTCEUTerminalMod.LOGGER.info("MultiStructureManagerUI: scanned {} multiblocks", multiblocks.size());
    }

    public ModularUI createUI() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        GUI_W = Math.min(640, mc.getWindow().getGuiScaledWidth()  - MARGIN * 2);
        GUI_H = Math.min(460, mc.getWindow().getGuiScaledHeight() - MARGIN * 2);

        this.bodyH  = GUI_H - 2 - HEADER_H - 2;
        this.rightX = LEFT_W + DIVIDER_W;
        this.rightW = GUI_W - 4 - LEFT_W - DIVIDER_W;

        WidgetGroup main = new WidgetGroup(0, 0, GUI_W, GUI_H);
        this.rootGroup = main;
        main.setBackground(new ColorRectTexture(0x00000000));

        if (!theme.isNativeStyle()) {
            main.addWidget(new WallpaperWidget(0, 0, GUI_W, GUI_H, () -> this.theme));
        }

        main.addWidget(buildOuterBorder());
        main.addWidget(buildHeader());
        main.addWidget(buildBody());

        this.gui = new ModularUI(new Size(GUI_W, GUI_H), uiHolder, player);
        gui.widget(main);
        gui.background(theme.modularUIBackground());

        setupParade();
        return gui;
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
        g.addWidget(new ImageWidget(0,         0,         GUI_W, 2,     new ColorRectTexture(colorBorderLight)));
        g.addWidget(new ImageWidget(0,         0,         2,     GUI_H, new ColorRectTexture(colorBorderLight)));
        g.addWidget(new ImageWidget(GUI_W - 2, 0,         2,     GUI_H, new ColorRectTexture(C_BORDER_DARK)));
        g.addWidget(new ImageWidget(0,         GUI_H - 2, GUI_W, 2,     new ColorRectTexture(C_BORDER_DARK)));
        return g;
    }

    private WidgetGroup buildHeader() {
        WidgetGroup header = new WidgetGroup(2, 2, GUI_W - 4, HEADER_H);
        header.setBackground(theme.headerTexture());

        int titleX = 10;
        ItemStack terminalRef = terminalItem.isEmpty() ? player.getMainHandItem() : terminalItem;
        int iconSize = 20, iconY = (HEADER_H - iconSize) / 2;
        if (theme.isBundleStyle()) {
            com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                    com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
            if (bundle != null) {
                com.lowdragmc.lowdraglib.gui.texture.IGuiTexture bundleIcon = bundle.iconTexture();
                if (bundleIcon != null) {
                    header.addWidget(new ImageWidget(titleX, iconY, iconSize, iconSize, bundleIcon));
                    titleX += iconSize + 4;
                }
            }
        } else {
            header.addWidget(HeaderItemIcon.build(titleX, iconY, iconSize, terminalRef,
                    theme.isNativeStyle() ? 0xFF2A2A2A : theme.accent(0x55),
                    theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF)));
            titleX += iconSize + 6;
        }

        LabelWidget title = new LabelWidget(titleX, 10,
                Component.translatable("gui.gtceuterminal.multiblock_manager.nearby_title",
                        multiblocks.size()).getString());
        title.setTextColor(theme.isBundleStyle() ? theme.labelColor() : C_TEXT_WHITE);
        header.addWidget(title);

        ButtonWidget settingsBtn = TerminalButton.ghostIcon(GUI_W - 50, 7, 14, "sliders",
                cd -> ManagerSettingsUI.openDialog(rootGroup, GUI_W, GUI_H, terminalRef, player));
        settingsBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.manager_settings.title").getString());
        header.addWidget(settingsBtn);

        ButtonWidget gearBtn = TerminalButton.ghostIcon(GUI_W - 30, 7, 14, "config",
                cd -> ThemeEditorDialog.open(rootGroup, ItemTheme.load(terminalRef)));
        gearBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.theme_settings").getString());
        header.addWidget(gearBtn);

        return header;
    }

    private MultiblockInfo getSelectedMultiblock() {
        return (selectedIndex >= 0 && selectedIndex < multiblocks.size())
                ? multiblocks.get(selectedIndex) : null;
    }

    private WidgetGroup buildBody() {
        int bodyY = 2 + HEADER_H;
        WidgetGroup body = new WidgetGroup(2, bodyY, GUI_W - 4, bodyH);
        this.bodyGroup = body;

        DraggableScrollableWidgetGroup[] scrollRef = new DraggableScrollableWidgetGroup[1];
        WidgetGroup[] footerRef = new WidgetGroup[1];
        WidgetGroup leftPanel = MultiblockListPanel.build(
                0, 0, LEFT_W, bodyH,
                selectedIndex, multiblocks,
                colorBgDark, colorBgMedium, colorBgLight, C_BORDER_DARK, colorBorderLight,
                C_TEXT_GRAY, C_HOVER,
                this::onEntryClicked,
                this::onHighlight,
                this::onRename,
                this::getSelectedMultiblock,
                footerRef,
                scrollRef);
        this.listScroll = scrollRef[0];
        body.addWidget(leftPanel);
        body.addWidget(footerRef[0]);

        body.addWidget(new ImageWidget(LEFT_W, 0, DIVIDER_W, bodyH,
                new ColorRectTexture(C_BORDER_DARK)));

        this.detailPanel = MultiblockDetailPanel.buildEmpty(
                rightX, 0, rightW, bodyH,
                colorBgDark, colorBgMedium, colorBgLight, C_BORDER_DARK, colorBorderLight,
                C_TEXT_GRAY);
        body.addWidget(detailPanel);

        return body;
    }

    private void onEntryClicked(int index) {
        selectedIndex = index;
        MultiblockInfo fresh = MultiblockScanner.scanSingle(
                multiblocks.get(index), player, player.level());

        repopulateList();
        swapDetailPanel(MultiblockDetailPanel.build(
                rightX, 0, rightW, bodyH, fresh, player, theme,
                colorBgDark, colorBgMedium, colorBgLight, C_BORDER_DARK, colorBorderLight,
                C_TEXT_GRAY, C_HOVER,
                this::openUpgradeDialog));
    }

    private void onHighlight(MultiblockInfo mb) {
        MultiblockHighlighter.highlight(mb, ItemsConfig.getMgrHighlightColor(),
                ItemsConfig.getMgrHighlightDurationMs());
        if (player instanceof net.minecraft.client.player.LocalPlayer lp) lp.closeContainer();
    }

    private void onRename(MultiblockInfo mb) {
        if (rootGroup == null) return;
        MultiblockRenameDialog.open(rootGroup, GUI_W, GUI_H, mb, player,
                listScroll, this::repopulateList);
    }

    private void openUpgradeDialog(com.gtceuterminal.common.multiblock.ComponentInfoGroup group,
                                   MultiblockInfo mb) {
        swapDetailPanel(InlineUpgradePanel.build(
                rightX, 0, rightW, bodyH,
                group, mb, player, theme,
                () -> swapDetailPanel(MultiblockDetailPanel.build(
                        rightX, 0, rightW, bodyH, mb, player, theme,
                        colorBgDark, colorBgMedium, colorBgLight, C_BORDER_DARK, colorBorderLight,
                        C_TEXT_GRAY, C_HOVER,
                        this::openUpgradeDialog)),
                colorBgDark, colorBgMedium, colorBgLight, colorBorderLight, C_HOVER));
    }

    private void refreshUI() {
        scanMultiblocks();
        selectedIndex = -1;
        repopulateList();
        swapDetailPanel(MultiblockDetailPanel.buildEmpty(
                rightX, 0, rightW, bodyH,
                colorBgDark, colorBgMedium, colorBgLight, C_BORDER_DARK, colorBorderLight,
                C_TEXT_GRAY));
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.multiblock_manager.refreshed_found",
                        multiblocks.size()), true);
    }

    private void swapDetailPanel(WidgetGroup newPanel) {
        if (bodyGroup != null && detailPanel != null) {
            bodyGroup.removeWidget(detailPanel);
        }
        this.detailPanel = newPanel;
        if (bodyGroup != null) {
            bodyGroup.addWidget(detailPanel);
        }
    }

    private void repopulateList() {
        if (listScroll == null) return;
        MultiblockListPanel.repopulate(listScroll, multiblocks, selectedIndex,
                LEFT_W - 14,
                colorBgMedium, colorBgLight, C_TEXT_GRAY, C_HOVER,
                this::onEntryClicked);
    }

    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder) {
        return new MultiStructureManagerUI(heldHolder).createUI();
    }

    public static ModularUI create(MultiStructureManagerUIFactory.Holder holder, Player player) {
        return new MultiStructureManagerUI(holder, player).createUI();
    }
}