package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.style.Bevel3DTexture;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalUI;
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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MultiStructureManagerUI {

    private static final int TAB_H       = 24;
    private static final int CARD_INSET  = 2;
    private static final int LEFT_W      = 210;
    private static final int DIVIDER_W   = 1;
    private static final int SCAN_RADIUS = 32;
    private static final String GLYPH_THEME = "✦";

    private int GUI_W, GUI_H;

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
        TerminalUI.use(this.theme);
        scanMultiblocks();
    }

    public MultiStructureManagerUI(MultiStructureManagerUIFactory.Holder holder, Player player) {
        this.uiHolder     = holder;
        this.player       = player;
        this.terminalItem = holder.getTerminalItem();
        this.theme        = ItemTheme.load(this.terminalItem);
        TerminalUI.use(this.theme);
        scanMultiblocks();
    }

    @Deprecated
    public MultiStructureManagerUI(IUIHolder holder, Player player) {
        this.uiHolder     = holder;
        this.player       = player;
        this.terminalItem = ItemStack.EMPTY;
        this.theme        = ItemTheme.loadFromPlayer(player);
        TerminalUI.use(this.theme);
        scanMultiblocks();
    }

    private void scanMultiblocks() {
        this.multiblocks = MultiblockScanner.scanNearbyMultiblocks(player, player.level(), SCAN_RADIUS);
        GTCEUTerminalMod.LOGGER.info("MultiStructureManagerUI: scanned {} multiblocks", multiblocks.size());
    }

    public ModularUI createUI() {
        GUI_W = 640;
        GUI_H = 460;

        WidgetGroup main = new WidgetGroup(0, 0, GUI_W, GUI_H);
        this.rootGroup = main;
        main.setBackground(new ColorRectTexture(0x00000000));

        if (!theme.isNativeStyle()) {
            main.addWidget(new WallpaperWidget(0, 0, GUI_W, GUI_H, () -> this.theme));
        }

        main.addWidget(buildBodyCard());
        addThemeButton(main);

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

    private WidgetGroup buildBodyCard() {
        WidgetGroup card = new WidgetGroup(2, 2, GUI_W - 4, GUI_H - 4);
        card.setBackground(TerminalUI.cardBg());
        this.bodyGroup = card;

        int innerW = (GUI_W - 4) - CARD_INSET * 2;
        int innerH = (GUI_H - 4) - CARD_INSET * 2;
        this.bodyH  = innerH;
        this.rightX = CARD_INSET + LEFT_W + DIVIDER_W;
        this.rightW = innerW - LEFT_W - DIVIDER_W;

        card.addWidget(buildHeaderTab());

        int listY = CARD_INSET + TAB_H;
        int listH = innerH - TAB_H;
        DraggableScrollableWidgetGroup[] scrollRef = new DraggableScrollableWidgetGroup[1];
        WidgetGroup[] footerRef = new WidgetGroup[1];
        WidgetGroup leftPanel = MultiblockListPanel.build(
                CARD_INSET, listY, LEFT_W, listH,
                selectedIndex, multiblocks,
                this::onEntryClicked,
                this::onHighlight,
                this::onRename,
                this::getSelectedMultiblock,
                footerRef,
                scrollRef);
        this.listScroll = scrollRef[0];
        card.addWidget(leftPanel);
        card.addWidget(footerRef[0]);

        card.addWidget(new ImageWidget(CARD_INSET + LEFT_W, CARD_INSET, DIVIDER_W, innerH,
                new ColorRectTexture(Palette.N0)));

        this.detailPanel = MultiblockDetailPanel.buildEmpty(rightX, CARD_INSET, rightW, innerH);
        card.addWidget(detailPanel);

        return card;
    }

    private WidgetGroup buildHeaderTab() {
        Font font = Minecraft.getInstance().font;
        ItemStack terminalRef = terminalItem.isEmpty() ? player.getMainHandItem() : terminalItem;
        String title = Component.translatable("gui.gtceuterminal.multiblock_manager.nearby_title",
                multiblocks.size()).getString();

        int iconSize = 16, padL = 5, gapIcon = 5, padR = 10;
        int tabW = padL + iconSize + gapIcon + font.width(title) + padR;
        int iconY = (TAB_H - iconSize) / 2;

        WidgetGroup tab = new WidgetGroup(CARD_INSET, CARD_INSET, tabW, TAB_H);
        tab.setBackground(new Bevel3DTexture(TerminalUI.tokens().panel()));

        boolean placedBundleIcon = false;
        if (theme.isBundleStyle()) {
            com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                    com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
            if (bundle != null && bundle.iconTexture() != null) {
                tab.addWidget(new ImageWidget(padL, iconY, iconSize, iconSize, bundle.iconTexture()));
                placedBundleIcon = true;
            }
        }
        if (!placedBundleIcon) {
            tab.addWidget(HeaderItemIcon.build(padL, iconY, iconSize, terminalRef,
                    theme.isNativeStyle() ? 0xFF2A2A2A : theme.accent(0x55),
                    theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF)));
        }

        LabelWidget titleLbl = new LabelWidget(padL + iconSize + gapIcon, (TAB_H - 8) / 2, title);
        titleLbl.setTextColor(theme.isBundleStyle() ? theme.labelColor() : Palette.N13);
        tab.addWidget(titleLbl);
        return tab;
    }

    private void addThemeButton(WidgetGroup parent) {
        ItemStack terminalRef = terminalItem.isEmpty() ? player.getMainHandItem() : terminalItem;
        int size = 18;
        int x = GUI_W - 2 - CARD_INSET - 4 - size;
        int y = 2 + CARD_INSET + 3;

        ButtonWidget themeBtn = TerminalButton.icon3d(x, y, size, GLYPH_THEME,
                cd -> ThemeEditorDialog.open(rootGroup, ItemTheme.load(terminalRef)));
        themeBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.theme_settings").getString());
        parent.addWidget(themeBtn);
    }

    private MultiblockInfo getSelectedMultiblock() {
        return (selectedIndex >= 0 && selectedIndex < multiblocks.size())
                ? multiblocks.get(selectedIndex) : null;
    }

    private void onEntryClicked(int index) {
        selectedIndex = index;
        MultiblockInfo fresh = MultiblockScanner.scanSingle(
                multiblocks.get(index), player, player.level());

        repopulateList();
        swapDetailPanel(MultiblockDetailPanel.build(
                rightX, CARD_INSET, rightW, bodyH, fresh, player, theme,
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
                rightX, CARD_INSET, rightW, bodyH,
                group, mb, player, theme,
                () -> swapDetailPanel(MultiblockDetailPanel.build(
                        rightX, CARD_INSET, rightW, bodyH, mb, player, theme,
                        this::openUpgradeDialog))));
    }

    private void refreshUI() {
        scanMultiblocks();
        selectedIndex = -1;
        repopulateList();
        swapDetailPanel(MultiblockDetailPanel.buildEmpty(rightX, CARD_INSET, rightW, bodyH));
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
                this::onEntryClicked);
    }

    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder) {
        return new MultiStructureManagerUI(heldHolder).createUI();
    }

    public static ModularUI create(MultiStructureManagerUIFactory.Holder holder, Player player) {
        return new MultiStructureManagerUI(holder, player).createUI();
    }
}