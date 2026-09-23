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

    private static final int TITLE_Y = 2;
    private static final int TITLE_H = 30;
    private static final int BODY_Y = TITLE_Y + TITLE_H - 1;
    private static final int BODY_H = 456;
    private static final int CARD_INSET = 2;
    private static final int LEFT_W = 210;
    private static final int DIVIDER_W = 1;
    private static final int SCAN_RADIUS = 32;
    private static final String GLYPH_THEME = "✦";

    private int GUI_W, GUI_H;

    private final IUIHolder uiHolder;
    private final Player player;
    private final ItemStack terminalItem;
    private final ItemTheme theme;

    private List<MultiblockInfo> multiblocks = new ArrayList<>();
    private int selectedIndex = -1;

    private ModularUI gui;
    private WidgetGroup rootGroup;
    private WidgetGroup bodyGroup;
    private DraggableScrollableWidgetGroup listScroll;
    private WidgetGroup detailPanel;

    private int rightX, rightW, bodyH;

    public MultiStructureManagerUI(HeldItemUIFactory.HeldItemHolder heldHolder) {
        this.uiHolder = heldHolder;
        this.player = heldHolder.player;
        this.terminalItem = heldHolder.held;
        this.theme = ItemTheme.load(this.terminalItem);
        TerminalUI.use(this.theme);
        scanMultiblocks();
    }

    public MultiStructureManagerUI(MultiStructureManagerUIFactory.Holder holder, Player player) {
        this.uiHolder = holder;
        this.player = player;
        this.terminalItem = holder.getTerminalItem();
        this.theme = ItemTheme.load(this.terminalItem);
        TerminalUI.use(this.theme);
        scanMultiblocks();
    }

    @Deprecated
    public MultiStructureManagerUI(IUIHolder holder, Player player) {
        this.uiHolder = holder;
        this.player = player;
        this.terminalItem = ItemStack.EMPTY;
        this.theme = ItemTheme.loadFromPlayer(player);
        TerminalUI.use(this.theme);
        scanMultiblocks();
    }

    private void scanMultiblocks() {
        this.multiblocks = MultiblockScanner.scanNearbyMultiblocks(player, player.level(), SCAN_RADIUS);
        GTCEUTerminalMod.LOGGER.info("MultiStructureManagerUI: scanned {} multiblocks", multiblocks.size());
    }

    public ModularUI createUI() {
        GUI_W = 640;
        GUI_H = BODY_Y + BODY_H + 2;

        WidgetGroup main = new WidgetGroup(0, 0, GUI_W, GUI_H);
        this.rootGroup = main;
        main.setBackground(new ColorRectTexture(Palette.CLEAR));

        if (!theme.isNativeStyle()) {
            main.addWidget(new WallpaperWidget(2, BODY_Y, GUI_W - 4, BODY_H, () -> this.theme));
        }

        main.addWidget(buildTitleCard());
        main.addWidget(buildBodyCard());

        this.gui = new ModularUI(new Size(GUI_W, GUI_H), uiHolder, player);
        gui.widget(main);
        gui.background(new ColorRectTexture(Palette.CLEAR));

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
        WidgetGroup card = new WidgetGroup(2, BODY_Y, GUI_W - 4, BODY_H);
        card.setBackground(TerminalUI.cardBg());
        this.bodyGroup = card;

        int innerW = (GUI_W - 4) - CARD_INSET * 2;
        int innerH = BODY_H - CARD_INSET * 2;
        this.bodyH = innerH;
        this.rightX = CARD_INSET + LEFT_W + DIVIDER_W;
        this.rightW = innerW - LEFT_W - DIVIDER_W;

        DraggableScrollableWidgetGroup[] scrollRef = new DraggableScrollableWidgetGroup[1];
        WidgetGroup[] footerRef = new WidgetGroup[1];
        WidgetGroup leftPanel = MultiblockListPanel.build(
                CARD_INSET, CARD_INSET, LEFT_W, innerH,
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

    private WidgetGroup buildTitleCard() {
        Font font = Minecraft.getInstance().font;
        ItemStack terminalRef = terminalItem.isEmpty() ? player.getMainHandItem() : terminalItem;
        String itemName = terminalRef.getHoverName().getString();
        String toolName = Component.translatable("gui.gtceuterminal.multiblock_manager.nearby_title",
                multiblocks.size()).getString();

        int iconSize = 20;
        int buttonSize = 20;
        int padL = 6;
        int padR = 6;
        int itemX = padL + iconSize + 6;
        int desiredW = itemX + font.width(itemName) + 18 + font.width(toolName)
                + 8 + buttonSize + padR;
        int cardW = Math.min(GUI_W - 4, Math.max(360, desiredW));
        int buttonX = cardW - padR - buttonSize;
        int toolX = buttonX - 8 - font.width(toolName);
        int labelY = (TITLE_H - 8) / 2;
        int iconY = (TITLE_H - iconSize) / 2;

        WidgetGroup card = new WidgetGroup(2, TITLE_Y, cardW, TITLE_H);
        card.setBackground(new Bevel3DTexture(TerminalUI.tokens().panel()));

        boolean placedBundleIcon = false;
        if (theme.isBundleStyle()) {
            com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                    com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
            if (bundle != null && bundle.iconTexture() != null) {
                card.addWidget(new ImageWidget(padL, iconY, iconSize, iconSize, bundle.iconTexture()));
                placedBundleIcon = true;
            }
        }
        if (!placedBundleIcon) {
            card.addWidget(HeaderItemIcon.build(padL, iconY, iconSize, terminalRef,
                    theme.isNativeStyle() ? Palette.N4 : theme.accent(0x55),
                    theme.isNativeStyle() ? Palette.N8 : theme.accent(0xFF)));
        }

        int itemMaxWidth = Math.max(0, toolX - 18 - itemX);
        if (font.width(itemName) > itemMaxWidth) {
            String ellipsis = "…";
            itemName = font.substrByWidth(Component.literal(itemName),
                    Math.max(0, itemMaxWidth - font.width(ellipsis))).getString() + ellipsis;
        }

        LabelWidget itemLabel = new LabelWidget(itemX, labelY, itemName);
        itemLabel.setColor(theme.isBundleStyle() ? theme.labelColor() : Palette.N13);
        card.addWidget(itemLabel);

        LabelWidget toolLabel = new LabelWidget(toolX, labelY, toolName);
        toolLabel.setColor(TerminalUI.tokens().textMuted());
        card.addWidget(toolLabel);

        ButtonWidget themeBtn = TerminalButton.icon3d(buttonX, (TITLE_H - buttonSize) / 2,
                buttonSize, GLYPH_THEME,
                cd -> ThemeEditorDialog.open(rootGroup, ItemTheme.load(terminalRef)));
        themeBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.theme_settings").getString());
        card.addWidget(themeBtn);
        return card;
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
                LEFT_W,
                this::onEntryClicked);
    }

    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder) {
        return new MultiStructureManagerUI(heldHolder).createUI();
    }

    public static ModularUI create(MultiStructureManagerUIFactory.Holder holder, Player player) {
        return new MultiStructureManagerUI(holder, player).createUI();
    }
}
