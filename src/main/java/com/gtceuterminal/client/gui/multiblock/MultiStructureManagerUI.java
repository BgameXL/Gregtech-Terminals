package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
import com.gtceuterminal.client.gui.widget.WallpaperWidget;
import com.gtceuterminal.client.highlight.MultiblockHighlighter;
import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.multiblock.MultiblockScanner;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.common.gui.factory.MultiStructureManagerUIFactory;
import com.gtceuterminal.common.config.ItemsConfig;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiStructureManagerUI {

    private static final int MARGIN      = 16;
    private static final int HEADER_H    = 30;
    private static final int FOOTER_H    = 28;
    private static final int PAD         = 8;
    private static final int ENTRY_H     = 28;
    private static final int ENTRY_STEP  = 30;
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
    private DraggableScrollableWidgetGroup multiblockScroll;

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
        colorBgDark     = theme.bgColor;
        colorBgMedium   = theme.panelColor;
        colorBgLight    = theme.isNativeStyle() ? 0xFF3A3A3A : theme.accent(0xAA);
        colorBorderLight = theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF);
    }

    private void scanMultiblocks() {
        this.multiblocks = MultiblockScanner.scanNearbyMultiblocks(player, player.level(), SCAN_RADIUS);
        GTCEUTerminalMod.LOGGER.info("MultiStructureManagerUI: scanned {} multiblocks", multiblocks.size());
    }

    public ModularUI createUI() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        GUI_W = Math.min(600, mc.getWindow().getGuiScaledWidth()  - MARGIN * 2);
        GUI_H = Math.min(480, mc.getWindow().getGuiScaledHeight() - MARGIN * 2);

        WidgetGroup main = new WidgetGroup(0, 0, GUI_W, GUI_H);
        this.rootGroup = main;
        main.setBackground(new ColorRectTexture(0x00000000));

        if (!theme.isNativeStyle()) {
            main.addWidget(new WallpaperWidget(0, 0, GUI_W, GUI_H, () -> this.theme));
        }

        main.addWidget(buildOuterBorder());
        main.addWidget(buildHeader());
        main.addWidget(buildList());
        main.addWidget(buildFooter());

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
        g.addWidget(new ImageWidget(0,         0,          GUI_W, 2,     new ColorRectTexture(colorBorderLight)));
        g.addWidget(new ImageWidget(0,         0,          2,     GUI_H, new ColorRectTexture(colorBorderLight)));
        g.addWidget(new ImageWidget(GUI_W - 2, 0,          2,     GUI_H, new ColorRectTexture(C_BORDER_DARK)));
        g.addWidget(new ImageWidget(0,         GUI_H - 2,  GUI_W, 2,     new ColorRectTexture(C_BORDER_DARK)));
        return g;
    }

    private WidgetGroup buildHeader() {
        WidgetGroup header = new WidgetGroup(2, 2, GUI_W - 4, HEADER_H);
        header.setBackground(theme.headerTexture());

        int titleX = 10;
        if (theme.isBundleStyle()) {
            com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                    com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
            if (bundle != null) {
                com.lowdragmc.lowdraglib.gui.texture.IGuiTexture icon = bundle.iconTexture();
                if (icon != null) {
                    int iconSize = 20, iconY = (HEADER_H - iconSize) / 2;
                    header.addWidget(new ImageWidget(titleX, iconY, iconSize, iconSize, icon));
                    titleX += iconSize + 4;
                }
            }
        }

        LabelWidget title = new LabelWidget(titleX, 10,
                Component.translatable("gui.gtceuterminal.multiblock_manager.nearby_title",
                        multiblocks.size()).getString());
        title.setTextColor(theme.isBundleStyle() ? theme.labelColor() : C_TEXT_WHITE);
        header.addWidget(title);

        ButtonWidget refreshBtn = new ButtonWidget(GUI_W - 52, 7, 16, 16,
                new ColorRectTexture(0x00000000), cd -> refreshUI());
        refreshBtn.setButtonTexture(new TextTexture("§7↻").setWidth(16).setType(TextTexture.TextType.NORMAL));
        refreshBtn.setHoverTexture(new ColorRectTexture(C_HOVER));
        refreshBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.multiblock_manager.refresh_tooltip").getString());
        header.addWidget(refreshBtn);

        ItemStack terminalRef = terminalItem.isEmpty() ? player.getMainHandItem() : terminalItem;
        ButtonWidget gearBtn = new ButtonWidget(GUI_W - 30, 7, 16, 16,
                new ColorRectTexture(0x00000000),
                cd -> ThemeEditorDialog.open(rootGroup, ItemTheme.load(terminalRef)));
        gearBtn.setButtonTexture(new TextTexture("§7⚙").setWidth(16).setType(TextTexture.TextType.NORMAL));
        gearBtn.setHoverTexture(new ColorRectTexture(C_HOVER));
        gearBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.theme_settings").getString());
        header.addWidget(gearBtn);

        return header;
    }

    private WidgetGroup buildList() {
        int listY = 2 + HEADER_H + 4;
        int listH = GUI_H - listY - FOOTER_H - 8;
        int scrollW = GUI_W - PAD * 2 - 14;

        DraggableScrollableWidgetGroup[] ref = new DraggableScrollableWidgetGroup[1];
        WidgetGroup panel = MultiblockListPanel.build(
                PAD, listY, GUI_W - PAD * 2, listH,
                ENTRY_H, ENTRY_STEP, selectedIndex, multiblocks,
                colorBgDark, colorBgMedium, colorBgLight, C_BORDER_DARK, colorBorderLight,
                C_TEXT_GRAY, C_HOVER,
                this::onEntryClicked,
                this::onHighlight,
                this::onRename,
                ref);
        this.multiblockScroll = ref[0];
        return panel;
    }

    private WidgetGroup buildFooter() {
        int footerY = GUI_H - FOOTER_H - 2;
        WidgetGroup footer = new WidgetGroup(PAD, footerY, GUI_W - PAD * 2, FOOTER_H);
        footer.setBackground(theme.panelTexture());

        LabelWidget summary = new LabelWidget(8, 8,
                Component.translatable("gui.gtceuterminal.multiblock_manager.footer_summary",
                        multiblocks.size(), SCAN_RADIUS).getString());
        summary.setTextColor(C_TEXT_GRAY);
        footer.addWidget(summary);

        return footer;
    }

    private void onEntryClicked(int index) {
        selectedIndex = index;
        openComponentDetail(multiblocks.get(index));
    }

    private void onHighlight(MultiblockInfo mb) {
        int durationMs = ItemsConfig.getMgrHighlightDurationMs();
        int color      = ItemsConfig.getMgrHighlightColor();
        MultiblockHighlighter.highlight(mb, color, durationMs);
        if (player instanceof net.minecraft.client.player.LocalPlayer lp) lp.closeContainer();
    }

    private void onRename(MultiblockInfo mb) {
        if (rootGroup == null) return;
        MultiblockRenameDialog.open(rootGroup, GUI_W, GUI_H, mb, player,
                multiblockScroll, this::repopulateList);
    }

    private void openComponentDetail(MultiblockInfo mb) {
        if (multiblockScroll != null) multiblockScroll.setActive(false);
        MultiblockInfo fresh = MultiblockScanner.scanSingle(mb, player, player.level());
        new com.gtceuterminal.client.gui.dialog.ComponentDetailDialog(
                gui.mainGroup, player, fresh,
                () -> { if (multiblockScroll != null) multiblockScroll.setActive(true); },
                theme);
    }

    private void refreshUI() {
        scanMultiblocks();
        selectedIndex = -1;
        repopulateList();
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.multiblock_manager.refreshed_found",
                        multiblocks.size()), true);
    }

    private void repopulateList() {
        if (multiblockScroll == null) return;
        int scrollW = GUI_W - PAD * 2 - 14;
        MultiblockListPanel.repopulate(multiblockScroll, multiblocks, selectedIndex,
                scrollW - 4, ENTRY_H, ENTRY_STEP,
                colorBgMedium, colorBgLight, C_TEXT_GRAY, C_HOVER,
                this::onEntryClicked, this::onHighlight, this::onRename);
    }

    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder) {
        return new MultiStructureManagerUI(heldHolder).createUI();
    }

    public static ModularUI create(MultiStructureManagerUIFactory.Holder holder, Player player) {
        return new MultiStructureManagerUI(holder, player).createUI();
    }
}