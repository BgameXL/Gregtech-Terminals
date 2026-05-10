package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.client.gui.widget.WallpaperWidget;
import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.multiblock.MultiblockScanner;
import com.gtceuterminal.common.multiblock.MultiblockStatus;
import com.gtceuterminal.client.gui.factory.MultiStructureManagerUIFactory;
import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.client.highlight.MultiblockHighlighter;
import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.network.CPacketSetCustomMultiblockName;
import com.gtceuterminal.common.network.TerminalNetwork;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class MultiStructureManagerUI {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int MARGIN     = 16;
    private static final int HEADER_H   = 30;
    private static final int FOOTER_H   = 28;
    private static final int PAD        = 8;
    private static final int ENTRY_H    = 28;
    private static final int ENTRY_STEP = 30;
    private static final int SCAN_RADIUS = 32;

    private int GUI_W;
    private int GUI_H;

    // ── Colors (overridden from theme) ────────────────────────────────────────
    private int COLOR_BG_DARK;
    private int COLOR_BG_MEDIUM;
    private int COLOR_BG_LIGHT;
    private int COLOR_BORDER_LIGHT;
    private static final int COLOR_BORDER_DARK = 0xFF0A0A0A;
    private static final int COLOR_TEXT_WHITE  = 0xFFFFFFFF;
    private static final int COLOR_TEXT_GRAY   = 0xFFAAAAAA;
    private static final int COLOR_HOVER       = 0x40FFFFFF;

    private final IUIHolder uiHolder;
    private final Player    player;
    private final net.minecraft.world.item.ItemStack terminalItem;
    private       ItemTheme theme;

    private List<MultiblockInfo> multiblocks = new ArrayList<>();
    private int selectedIndex = -1;

    private ModularUI                    gui;
    private WidgetGroup                  rootGroup;   // always set — rename dialog needs this
    private DraggableScrollableWidgetGroup multiblockScroll;

    // ── Constructors ──────────────────────────────────────────────────────────
    public MultiStructureManagerUI(HeldItemUIFactory.HeldItemHolder heldHolder) {
        this.uiHolder     = heldHolder;
        this.player       = heldHolder.player;
        this.terminalItem = heldHolder.held;
        applyTheme(ItemTheme.load(this.terminalItem));
        scanMultiblocks();
    }

    public MultiStructureManagerUI(MultiStructureManagerUIFactory.Holder holder, Player player) {
        this.uiHolder     = holder;
        this.player       = player;
        this.terminalItem = holder.getTerminalItem();
        applyTheme(ItemTheme.load(this.terminalItem));
        scanMultiblocks();
    }

    @Deprecated
    public MultiStructureManagerUI(IUIHolder holder, Player player) {
        this.uiHolder     = holder;
        this.player       = player;
        this.terminalItem = net.minecraft.world.item.ItemStack.EMPTY;
        applyTheme(ItemTheme.loadFromPlayer(player));
        scanMultiblocks();
    }

    private void applyTheme(ItemTheme t) {
        this.theme         = t;
        COLOR_BG_DARK      = t.bgColor;
        COLOR_BG_MEDIUM    = t.panelColor;
        COLOR_BG_LIGHT     = t.isNativeStyle() ? 0xFF3A3A3A : t.accent(0xAA);
        COLOR_BORDER_LIGHT = t.isNativeStyle() ? 0xFF555555 : t.accent(0xFF);
    }

    // ── Scan ──────────────────────────────────────────────────────────────────
    private void scanMultiblocks() {
        this.multiblocks = MultiblockScanner.scanNearbyMultiblocks(player, player.level(), SCAN_RADIUS);
        GTCEUTerminalMod.LOGGER.info("MultiStructureManagerUI: scanned {} multiblocks", multiblocks.size());
    }

    // ── UI construction ───────────────────────────────────────────────────────
    public ModularUI createUI() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        GUI_W = Math.min(600, screenW - MARGIN * 2);
        GUI_H = Math.min(480, screenH - MARGIN * 2);

        WidgetGroup main = new WidgetGroup(0, 0, GUI_W, GUI_H);
        this.rootGroup = main;

        // Background / wallpaper
        main.setBackground(new ColorRectTexture(0x00000000));
        if (!theme.isNativeStyle()) {
            main.addWidget(new WallpaperWidget(0, 0, GUI_W, GUI_H, () -> this.theme));
        }

        main.addWidget(buildOuterBorder());
        main.addWidget(buildHeader());
        main.addWidget(buildListPanel());
        main.addWidget(buildFooter());

        this.gui = new ModularUI(new Size(GUI_W, GUI_H), uiHolder, player);
        gui.widget(main);
        gui.background(theme.modularUIBackground());

        // Register the parade renderer AFTER gui is built so guiLeft/guiTop are known
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

        // ContainerScreenEvent.Render.Foreground fires with posestack already
        // translated by (leftPos, topPos) — so coordinates are GUI-panel-relative.
        // The orbit center is simply the center of the panel at (GUI_W/2, GUI_H/2).
        parade.setGuiCenter(GUI_W / 2f, GUI_H / 2f);

        com.gtceuterminal.client.ClientEvents.setActiveParade(parade, theme.paradeMode);
    }

    // ── Outer border ──────────────────────────────────────────────────────────
    private WidgetGroup buildOuterBorder() {
        WidgetGroup g = new WidgetGroup(0, 0, GUI_W, GUI_H);
        g.addWidget(new ImageWidget(0,         0,          GUI_W, 2,     new ColorRectTexture(COLOR_BORDER_LIGHT)));
        g.addWidget(new ImageWidget(0,         0,          2,     GUI_H, new ColorRectTexture(COLOR_BORDER_LIGHT)));
        g.addWidget(new ImageWidget(GUI_W - 2, 0,          2,     GUI_H, new ColorRectTexture(COLOR_BORDER_DARK)));
        g.addWidget(new ImageWidget(0,         GUI_H - 2,  GUI_W, 2,     new ColorRectTexture(COLOR_BORDER_DARK)));
        return g;
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private WidgetGroup buildHeader() {
        WidgetGroup header = new WidgetGroup(2, 2, GUI_W - 4, HEADER_H);
        header.setBackground(theme.headerTexture());

        // Bundle icon — shown left of the title when a modpack theme is active
        int titleX = 10;
        if (theme.isBundleStyle()) {
            com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                    com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
            if (bundle != null) {
                com.lowdragmc.lowdraglib.gui.texture.IGuiTexture icon = bundle.iconTexture();
                if (icon != null) {
                    int iconSize = 20;
                    int iconY    = (HEADER_H - iconSize) / 2;
                    header.addWidget(new ImageWidget(titleX, iconY, iconSize, iconSize, icon));
                    titleX += iconSize + 4;
                }
            }
        }

        // Title
        LabelWidget title = new LabelWidget(titleX, 10,
                Component.translatable("gui.gtceuterminal.multiblock_manager.nearby_title",
                        multiblocks.size()).getString());
        title.setTextColor(theme.isBundleStyle() ? theme.labelColor() : COLOR_TEXT_WHITE);
        header.addWidget(title);

        // ↻ Refresh button
        ButtonWidget refreshBtn = new ButtonWidget(GUI_W - 52, 7, 16, 16,
                new ColorRectTexture(0x00000000),
                cd -> refreshUI());
        refreshBtn.setButtonTexture(new TextTexture("§7↻").setWidth(16).setType(TextTexture.TextType.NORMAL));
        refreshBtn.setHoverTexture(new ColorRectTexture(COLOR_HOVER));
        refreshBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.multiblock_manager.refresh_tooltip").getString());
        header.addWidget(refreshBtn);

        
        ButtonWidget gearBtn = new ButtonWidget(GUI_W - 30, 7, 16, 16,
                new ColorRectTexture(0x00000000),
                cd -> ThemeEditorDialog.open(rootGroup, ItemTheme.load(terminalItem.isEmpty() ? player.getMainHandItem() : terminalItem)));
        gearBtn.setButtonTexture(new TextTexture("§7⚙").setWidth(16).setType(TextTexture.TextType.NORMAL));
        gearBtn.setHoverTexture(new ColorRectTexture(COLOR_HOVER));
        gearBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.theme_settings").getString());
        header.addWidget(gearBtn);

        return header;
    }

    // ── List panel ────────────────────────────────────────────────────────────
    private WidgetGroup buildListPanel() {
        int listY = 2 + HEADER_H + 4;
        int listH = GUI_H - listY - FOOTER_H - 8;
        WidgetGroup panel = new WidgetGroup(PAD, listY, GUI_W - PAD * 2, listH);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(COLOR_BG_DARK),
                new ColorBorderTexture(1, COLOR_BORDER_DARK)));

        // Scroll area fills the panel
        int scrollW = GUI_W - PAD * 2 - 14; // leave room for scrollbar
        DraggableScrollableWidgetGroup scroll =
                new DraggableScrollableWidgetGroup(2, 2, scrollW, listH - 4);
        this.multiblockScroll = scroll;
        scroll.setYScrollBarWidth(8);
        scroll.setYBarStyle(
                new ColorRectTexture(COLOR_BORDER_DARK),
                new ColorRectTexture(COLOR_BORDER_LIGHT));

        populateList();

        panel.addWidget(scroll);
        return panel;
    }

    private void populateList() {
        if (multiblockScroll == null) return;
        multiblockScroll.clearAllWidgets();

        if (multiblocks.isEmpty()) {
            LabelWidget empty = new LabelWidget(10, 12,
                    Component.translatable("gui.gtceuterminal.multiblock_manager.none_found").getString());
            empty.setTextColor(COLOR_TEXT_GRAY);
            multiblockScroll.addWidget(empty);
            return;
        }

        int yPos = 2;
        for (int i = 0; i < multiblocks.size(); i++) {
            multiblockScroll.addWidget(buildEntry(multiblocks.get(i), i, yPos));
            yPos += ENTRY_STEP;
        }
    }

    // ── Single list entry ─────────────────────────────────────────────────────
    private WidgetGroup buildEntry(MultiblockInfo mb, int index, int yPos) {
        int entryW = GUI_W - PAD * 2 - 18; // scrollW - 2px padding each side

        WidgetGroup entry = new WidgetGroup(0, yPos, entryW, ENTRY_H);

        boolean isSelected = (index == selectedIndex);
        entry.setBackground(new ColorRectTexture(isSelected ? COLOR_BG_LIGHT : COLOR_BG_MEDIUM));

        int clickW = entryW - 72;
        ButtonWidget clickBtn = new ButtonWidget(0, 0, clickW, ENTRY_H,
                new ColorRectTexture(0x00000000),
                cd -> {
                    selectedIndex = index;
                    openComponentDetail(mb);
                });
        clickBtn.setHoverTexture(new ColorRectTexture(COLOR_HOVER));
        entry.addWidget(clickBtn);

        // Status pill text — "● Active" in status color
        MultiblockStatus status = mb.getStatus();
        String pillText = "§" + colorCodeFor(status) + "● " + status.getDisplayName();
        LabelWidget pill = new LabelWidget(6, 5, pillText);
        entry.addWidget(pill);

        // Resolve translation key → human-readable name (e.g. "block.gtceu.multi_smelter" → "Multi Smelter")
        String rawName = mb.getName();
        String resolvedName = Component.translatable(rawName).getString();
        // If translation key didn't resolve (getString() == key), title-case the last segment
        if (resolvedName.equals(rawName) && rawName.contains(".")) {
            String last = rawName.substring(rawName.lastIndexOf('.') + 1);
            resolvedName = Character.toUpperCase(last.charAt(0)) + last.substring(1).replace('_', ' ');
        }
        String displayName = truncate(resolvedName, 28);
        // If multi-mod pack, tint non-GTCEu machines in their mod color
        String nameText = mb.isVanillaGTCEu()
                ? "§f" + displayName
                : "§" + colorCodeFor(mb.getModColor()) + displayName;
        LabelWidget nameLabel = new LabelWidget(6, 16, nameText);
        entry.addWidget(nameLabel);

        LabelWidget distLabel = new LabelWidget(entryW - 70, 10, mb.getDistanceString());
        distLabel.setTextColor(COLOR_TEXT_GRAY);
        entry.addWidget(distLabel);

        ButtonWidget highlightBtn = new ButtonWidget(entryW - 48, 6, 20, 16,
                new ColorRectTexture(0x00000000),
                cd -> toggleHighlight(mb));
        highlightBtn.setButtonTexture(
                new TextTexture("§e◉").setWidth(20).setType(TextTexture.TextType.NORMAL));
        highlightBtn.setHoverTexture(new ColorRectTexture(0x33FFFF00));
        highlightBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.multiblock_manager.highlight_tooltip").getString());
        entry.addWidget(highlightBtn);

        ButtonWidget renameBtn = new ButtonWidget(entryW - 26, 6, 20, 16,
                new ColorRectTexture(0x00000000),
                cd -> openRenameDialog(mb));
        renameBtn.setButtonTexture(
                new TextTexture("§7✎").setWidth(20).setType(TextTexture.TextType.NORMAL));
        renameBtn.setHoverTexture(new ColorRectTexture(0x33FFFFFF));
        renameBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.multiblock_manager.rename_tooltip").getString());
        entry.addWidget(renameBtn);

        return entry;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private WidgetGroup buildFooter() {
        int footerY = GUI_H - FOOTER_H - 2;
        WidgetGroup footer = new WidgetGroup(PAD, footerY, GUI_W - PAD * 2, FOOTER_H);
        footer.setBackground(theme.panelTexture());

        // Summary: "X multiblocks within Ym"
        LabelWidget summary = new LabelWidget(8, 8,
                Component.translatable("gui.gtceuterminal.multiblock_manager.footer_summary",
                        multiblocks.size(), SCAN_RADIUS).getString());
        summary.setTextColor(COLOR_TEXT_GRAY);
        footer.addWidget(summary);

        return footer;
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void toggleHighlight(MultiblockInfo mb) {
        int durationMs = com.gtceuterminal.common.config.ItemsConfig.getMgrHighlightDurationMs();
        int color      = com.gtceuterminal.common.config.ItemsConfig.getMgrHighlightColor();
        MultiblockHighlighter.highlight(mb, color, durationMs);
        if (player instanceof net.minecraft.client.player.LocalPlayer lp) {
            lp.closeContainer();
        }
    }

    private void openRenameDialog(MultiblockInfo mb) {
        if (rootGroup == null) return;

        final int DW = 260;
        final int DH = 92;
        final int DX = (GUI_W - DW) / 2;
        final int DY = (GUI_H - DH) / 2;

        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(0xA0000000));
        dialog.setClickClose(false);

        // Disable list interaction while dialog is open
        if (multiblockScroll != null) multiblockScroll.setActive(false);

        WidgetGroup panel = new WidgetGroup(DX, DY, DW, DH);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF1E1E1E),
                ColorPattern.GRAY.borderTexture(-1)));
        dialog.addWidget(panel);

        // Title bar
        WidgetGroup titleBar = new WidgetGroup(0, 0, DW, 20);
        titleBar.setBackground(new ColorRectTexture(0xFF2A2A2A));
        panel.addWidget(titleBar);

        LabelWidget titleLbl = new LabelWidget(8, 5,
                Component.translatable("gui.gtceuterminal.multiblock_manager.rename_dialog.title").getString());
        titleLbl.setTextColor(0xFFFFAA00);
        titleBar.addWidget(titleLbl);

        // Sub-label: machine type
        LabelWidget sub = new LabelWidget(8, 24, "§7" + truncate(mb.getMachineTypeName(), 35));
        sub.setTextColor(COLOR_TEXT_GRAY);
        panel.addWidget(sub);

        // Text field
        TextFieldWidget textField = new TextFieldWidget(8, 38, DW - 16, 16, null, s -> {});
        textField.setMaxStringLength(32);
        textField.setBordered(true);
        textField.setCurrentString(mb.getCustomDisplayName());
        panel.addWidget(textField);

        int btnW = (DW - 20) / 2;

        // Confirm
        ButtonWidget confirmBtn = new ButtonWidget(8, 60, btnW, 18,
                new ColorRectTexture(0xFF1A4A1A),
                cd -> {
                    String dimId = LinkedMachineData.dimId(player.level());
                    String key   = mb.posKey(dimId);
                    String name  = textField.getCurrentString().trim();
                    TerminalNetwork.CHANNEL.sendToServer(
                            new CPacketSetCustomMultiblockName(null, key, name, false));
                    mb.setCustomDisplayName(name);
                    dialog.close();
                    if (multiblockScroll != null) multiblockScroll.setActive(true);
                    populateList();
                });
        confirmBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.multiblock_manager.rename_dialog.confirm").getString())
                .setWidth(btnW).setType(TextTexture.TextType.NORMAL));
        confirmBtn.setHoverTexture(new ColorRectTexture(0xFF1E6A1E));
        panel.addWidget(confirmBtn);

        // Clear
        ButtonWidget clearBtn = new ButtonWidget(DW / 2 + 2, 60, btnW, 18,
                new ColorRectTexture(0xFF3A2A2A),
                cd -> {
                    String dimId = LinkedMachineData.dimId(player.level());
                    String key   = mb.posKey(dimId);
                    TerminalNetwork.CHANNEL.sendToServer(
                            new CPacketSetCustomMultiblockName(null, key, "", true));
                    mb.setCustomDisplayName(null);
                    dialog.close();
                    if (multiblockScroll != null) multiblockScroll.setActive(true);
                    populateList();
                });
        clearBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.multiblock_manager.rename_dialog.clear_name").getString())
                .setWidth(btnW).setType(TextTexture.TextType.NORMAL));
        clearBtn.setHoverTexture(new ColorRectTexture(0xFF5A3A3A));
        panel.addWidget(clearBtn);
    }

    private void openComponentDetail(MultiblockInfo multiblock) {
        if (multiblockScroll != null) multiblockScroll.setActive(false);

        new com.gtceuterminal.client.gui.dialog.ComponentDetailDialog(
                gui.mainGroup,
                player,
                multiblock,
                () -> {
                    if (multiblockScroll != null) multiblockScroll.setActive(true);
                }
        );
    }

    private void refreshUI() {
        scanMultiblocks();
        selectedIndex = -1;
        populateList();

        // Rebuild header so the count updates
        if (rootGroup != null) {
        }

        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.multiblock_manager.refreshed_found",
                        multiblocks.size()),
                true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private static char colorCodeFor(MultiblockStatus status) {
        return switch (status) {
            case ACTIVE          -> 'a'; // green
            case IDLE            -> 'e'; // yellow
            case NEEDS_MAINTENANCE -> '6'; // gold
            case NO_POWER        -> 'c'; // red
            case DISABLED        -> '8'; // dark gray
            case UNFORMED        -> 'c'; // red
            case OUTPUT_FULL     -> 'b'; // aqua
        };
    }

    private static char colorCodeFor(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b =  argb        & 0xFF;
        if (g > 200 && r < 100 && b < 100) return 'a'; // green  → monifactory
        if (r > 200 && g > 200 && b < 100) return 'e'; // yellow
        if (r < 100 && g > 200 && b > 200) return 'b'; // aqua   → tfg
        if (r > 200 && g < 100 && b > 200) return 'd'; // purple → astrogreg
        if (r > 200 && g > 100 && b < 50)  return '6'; // gold   → phoenix
        return '7'; // gray fallback
    }

    // ── Static factory methods ────────────────────────────────────────────────

    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder) {
        return new MultiStructureManagerUI(heldHolder).createUI();
    }

    public static ModularUI create(MultiStructureManagerUIFactory.Holder holder, Player player) {
        return new MultiStructureManagerUI(holder, player).createUI();
    }
}