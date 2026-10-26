package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
import com.gtceuterminal.client.gui.widget.HeaderItemIcon;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.client.gui.widget.TerminalTabs;
import com.gtceuterminal.client.gui.widget.WallpaperWidget;
import com.gtceuterminal.client.highlight.MultiblockHighlighter;
import com.gtceuterminal.common.config.ClientConfig;
import com.gtceuterminal.common.config.ItemsConfig;
import com.gtceuterminal.common.gui.factory.MultiStructureManagerUIFactory;
import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.multiblock.MultiblockScanner;
import com.gtceuterminal.common.theme.ItemTheme;
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.*;
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
    private static final int WIDE_W = 640;
    private static final int SETTINGS_W = 280;
    private static final int SETTINGS_BODY_H = 184;
    private static final int HEADER_GAP = 4;
    private static final int SETTINGS_CARD_W = 142;
    private static final int CARD_INSET = 2;
    private static final int LEFT_W = 210;
    private static final int DIVIDER_W = 1;
    private final IUIHolder uiHolder;
    private final Player player;
    private final ItemStack terminalItem;
    private final ItemTheme theme;
    private int GUI_W, GUI_H;
    private List<MultiblockInfo> multiblocks = new ArrayList<>();
    private int selectedIndex = -1;

    private ModularUI gui;
    private WidgetGroup rootGroup;
    private WidgetGroup bodyGroup;
    private DraggableScrollableWidgetGroup listScroll;
    private WidgetGroup detailPanel;
    private ManagerSettingsUI managerSettingsUI;
    private View activeView = View.MULTIBLOCKS;

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

    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder) {
        return new MultiStructureManagerUI(heldHolder).createUI();
    }

    public static ModularUI create(MultiStructureManagerUIFactory.Holder holder, Player player) {
        return new MultiStructureManagerUI(holder, player).createUI();
    }

    private void scanMultiblocks() {
        this.multiblocks = MultiblockScanner.scanNearbyMultiblocks(player, player.level(), ClientConfig.getMultiblockScanRadius());
        GTCEUTerminalMod.LOGGER.debug("MultiStructureManagerUI: scanned {} multiblocks", multiblocks.size());
    }

    public ModularUI createUI() {
        applyViewDimensions();

        WidgetGroup main = new WidgetGroup(0, 0, GUI_W, GUI_H);
        this.rootGroup = main;
        main.setBackground(TerminalUI.transparent());

        this.gui = new ModularUI(new Size(GUI_W, GUI_H), uiHolder, player);
        gui.widget(main);
        gui.background(TerminalUI.transparent());

        rebuildContent();
        return gui;
    }

    private int currentBodyH() {
        return activeView == View.SETTINGS ? SETTINGS_BODY_H : BODY_H;
    }

    private void applyViewDimensions() {
        GUI_W = activeView == View.SETTINGS ? SETTINGS_W : WIDE_W;
        GUI_H = BODY_Y + currentBodyH() + 2;
    }

    private void rebuildContent() {
        if (rootGroup == null) return;
        rootGroup.clearAllWidgets();

        if (!theme.isNativeStyle()) {
            rootGroup.addWidget(new WallpaperWidget(2, BODY_Y, GUI_W - 4, currentBodyH(), () -> this.theme));
        }

        rootGroup.addWidget(activeView == View.SETTINGS ? buildSettingsHeader() : buildHeaderCards());
        rootGroup.addWidget(activeView == View.SETTINGS ? buildSettingsBodyCard() : buildMultiblockBodyCard());
    }

    private WidgetGroup buildMultiblockBodyCard() {
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
                TerminalUI.divider()));

        MultiblockInfo selected = getSelectedMultiblock();
        this.detailPanel = selected == null
                ? MultiblockDetailPanel.buildEmpty(rightX, CARD_INSET, rightW, innerH)
                : MultiblockDetailPanel.build(
                rightX, CARD_INSET, rightW, innerH, selected, player, theme,
                this::openUpgradeDialog);
        card.addWidget(detailPanel);

        return card;
    }

    private WidgetGroup buildHeaderCards() {
        Font font = Minecraft.getInstance().font;
        ItemStack terminalRef = terminalItem.isEmpty() ? player.getMainHandItem() : terminalItem;
        String itemName = terminalRef.getHoverName().getString();
        String toolName = Component.translatable("gui.gtceuterminal.multiblock_manager.nearby_title",
                multiblocks.size()).getString();

        int iconSize = 20;
        int padL = 6;
        int padR = 6;
        int itemX = padL + iconSize + 6;
        int desiredW = itemX + Math.max(font.width(itemName), font.width(toolName)) + padR;
        int maxMainW = GUI_W - 4 - HEADER_GAP - SETTINGS_CARD_W - HEADER_GAP - TerminalTabs.ACTION_CARD_W;
        int cardW = Math.min(maxMainW, Math.max(213, desiredW));
        int textMaxWidth = Math.max(0, cardW - padR - itemX);
        int titleY = 6;
        int toolY = 17;
        int centerLabelY = (TITLE_H - 8) / 2;
        int iconY = (TITLE_H - iconSize) / 2;

        WidgetGroup headers = new WidgetGroup(0, 0, GUI_W, TITLE_Y + TITLE_H);
        WidgetGroup card = TerminalTabs.tabCard(2, TITLE_Y, cardW, TITLE_H,
                activeView == View.MULTIBLOCKS, cd -> switchView(View.MULTIBLOCKS));

        var bundleIcon = TerminalUI.bundleIcon();
        if (bundleIcon != null) {
            card.addWidget(new ImageWidget(padL, iconY, iconSize, iconSize, bundleIcon));
        } else {
            card.addWidget(HeaderItemIcon.build(padL, iconY, iconSize, terminalRef,
                    TerminalUI.tokens().panelDisabled(), TerminalUI.tokens().border()));
        }

        if (font.width(itemName) > textMaxWidth) {
            String ellipsis = "…";
            itemName = font.substrByWidth(Component.literal(itemName),
                    Math.max(0, textMaxWidth - font.width(ellipsis))).getString() + ellipsis;
        }
        if (font.width(toolName) > textMaxWidth) {
            String ellipsis = "…";
            toolName = font.substrByWidth(Component.literal(toolName),
                    Math.max(0, textMaxWidth - font.width(ellipsis))).getString() + ellipsis;
        }

        LabelWidget itemLabel = new LabelWidget(itemX, titleY, itemName);
        itemLabel.setColor(TerminalUI.itemLabelColor());
        card.addWidget(itemLabel);

        LabelWidget toolLabel = new LabelWidget(itemX, toolY, toolName);
        toolLabel.setColor(TerminalUI.tokens().textMuted());
        card.addWidget(toolLabel);

        headers.addWidget(card);

        int settingsX = 2 + cardW + HEADER_GAP;
        WidgetGroup settingsCard = TerminalTabs.tabCard(settingsX, TITLE_Y, SETTINGS_CARD_W, TITLE_H,
                activeView == View.SETTINGS, cd -> switchView(View.SETTINGS));

        LabelWidget settingsLabel = new LabelWidget(10, centerLabelY,
                Component.translatable("gui.gtceuterminal.manager_settings.title").getString());
        settingsLabel.setColor(TerminalUI.tokens().textStrong());
        settingsCard.addWidget(settingsLabel);
        headers.addWidget(settingsCard);

        headers.addWidget(TerminalTabs.actionCard(GUI_W - 2 - TerminalTabs.ACTION_CARD_W, TITLE_Y, TITLE_H,
                cd -> ThemeEditorDialog.open(rootGroup, ItemTheme.load(terminalRef)),
                cd -> {
                    if (player instanceof net.minecraft.client.player.LocalPlayer lp) lp.closeContainer();
                }));
        return headers;
    }

    private void switchView(View view) {
        if (view == activeView || rootGroup == null) return;
        activeView = view;
        applyViewDimensions();
        rebuildContent();
        if (gui != null) gui.setSize(GUI_W, GUI_H);
    }

    private WidgetGroup buildSettingsBodyCard() {
        WidgetGroup card = new WidgetGroup(2, BODY_Y, GUI_W - 4, SETTINGS_BODY_H);
        card.setBackground(TerminalUI.cardBg());
        this.bodyGroup = card;
        this.listScroll = null;
        this.detailPanel = null;

        if (managerSettingsUI == null) {
            ItemStack terminalRef = terminalItem.isEmpty() ? player.getMainHandItem() : terminalItem;
            managerSettingsUI = new ManagerSettingsUI(terminalRef, player);
        }
        card.addWidget(managerSettingsUI.buildEmbeddedPanel(
                CARD_INSET, CARD_INSET,
                GUI_W - 4 - CARD_INSET * 2,
                SETTINGS_BODY_H - CARD_INSET * 2));
        return card;
    }

    private WidgetGroup buildSettingsHeader() {
        WidgetGroup headers = new WidgetGroup(0, 0, GUI_W, TITLE_Y + TITLE_H);
        int cardW = GUI_W - 4;
        WidgetGroup card = new WidgetGroup(2, TITLE_Y, cardW, TITLE_H);
        card.addWidget(new ImageWidget(0, 0, cardW, TITLE_H,
                TerminalUI.bevel(TerminalUI.tokens().panel())));

        ButtonWidget back = TerminalButton.icon3d(6, (TITLE_H - 18) / 2, 18, TerminalButton.ICON_ARROW_LEFT,
                cd -> switchView(View.MULTIBLOCKS));
        card.addWidget(back);

        LabelWidget title = new LabelWidget(6 + 18 + 8, (TITLE_H - 8) / 2,
                Component.translatable("gui.gtceuterminal.manager_settings.title").getString());
        title.setColor(TerminalUI.tokens().textStrong());
        card.addWidget(title);

        headers.addWidget(card);
        return headers;
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
        swapDetailPanel(MultiblockUpgradePanel.build(
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

    private enum View {
        MULTIBLOCKS,
        SETTINGS
    }
}
