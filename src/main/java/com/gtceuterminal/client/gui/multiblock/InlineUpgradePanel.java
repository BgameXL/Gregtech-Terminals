package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.common.multiblock.ComponentGroupRegistry;
import com.gtceuterminal.common.multiblock.ComponentInfo;
import com.gtceuterminal.common.multiblock.ComponentInfoGroup;
import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.material.ComponentUpgradeHelper;
import com.gtceuterminal.common.autocraft.AnalysisResult;
import com.gtceuterminal.common.ae2.MENetworkScanner;
import com.gtceuterminal.common.network.CPacketComponentUpgrade;
import com.gtceuterminal.common.network.CPacketRequestUpgradeAnalysis;
import com.gtceuterminal.common.network.CPacketRequestUpgradeAvailability;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.gtceuterminal.client.gui.dialog.UpgradeCandidateGrid;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.common.ae2.WirelessTerminalHandler;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.gtceuterminal.client.gui.widget.TerminalButton;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@OnlyIn(Dist.CLIENT)
public final class InlineUpgradePanel {

    private static final int HEADER_H  = 36;
    private static final int FOOTER_H  = 36;
    private static final int PAD       = 8;
    
    private static final int TIER_BTN_MIN_W = 20;
    private static final int TIER_BTN_MAX_W = 44;
    private static final int TIER_BTN_H     = 22;
    private static final int TIER_GAP       = 2;

    private static final int C_SUCCESS  = 0xFF00FF00;
    private static final int C_WHITE    = 0xFFFFFFFF;
    private static final int C_GRAY     = 0xFFAAAAAA;

    private static final AtomicInteger AVAILABILITY_REQUEST_IDS = new AtomicInteger(1);
    private static final Map<Integer, PanelState> AVAILABILITY_LISTENERS = new ConcurrentHashMap<>();

    private InlineUpgradePanel() {}

    public static void handleAvailabilityResult(int requestId, AnalysisResult result) {
        PanelState state = AVAILABILITY_LISTENERS.remove(requestId);
        if (state != null) {
            state.receiveAvailabilityResult(requestId, result);
        }
    }

    static WidgetGroup build(
            int x, int y, int w, int h,
            ComponentInfoGroup group,
            MultiblockInfo mb,
            Player player,
            ItemTheme theme,
            Runnable onBack,
            int colorBgDark, int colorBgMed, int colorBgLight,
            int colorBorder, int colorHover
    ) {
        return new PanelState(x, y, w, h, group, mb, player, theme, onBack,
                colorBgDark, colorBgMed, colorBgLight, colorBorder, colorHover).root;
    }

    private static final class PanelState {

        final WidgetGroup root;

        private final int w, h;
        private final ComponentInfoGroup group;
        private final MultiblockInfo     mb;
        private final Player             player;
        private final ItemTheme          theme;
        private final Runnable           onBack;
        private final int colorBgDark, colorBgMed, colorBgLight, colorBorder, colorHover;

        private com.gtceuterminal.common.upgrade.UniversalUpgradeCatalog catalog;

        private Integer selectedTier      = null;
        private String  selectedUpgradeId = null;

        // Mutable sub-panels
        private WidgetGroup optionsPanel;
        private WidgetGroup availabilityPanel;
        private WidgetGroup tierButtonsRow;
        private DraggableScrollableWidgetGroup optionsScroll;
        private int pendingAvailabilityRequestId = -1;
        private boolean availabilityLoading = false;
        private AnalysisResult availabilityResult = null;

        PanelState(int x, int y, int w, int h,
                   ComponentInfoGroup group, MultiblockInfo mb,
                   Player player, ItemTheme theme, Runnable onBack,
                   int colorBgDark, int colorBgMed, int colorBgLight,
                   int colorBorder, int colorHover) {
            this.w            = w;
            this.h            = h;
            this.group        = group;
            this.mb           = mb;
            this.player       = player;
            this.theme        = theme;
            this.onBack       = onBack;
            this.colorBgDark  = colorBgDark;
            this.colorBgMed   = colorBgMed;
            this.colorBgLight = colorBgLight;
            this.colorBorder  = colorBorder;
            this.colorHover   = colorHover;

            if (mb.getController() instanceof MultiblockControllerMachine mmc) {
                try {
                    catalog = com.gtceuterminal.common.upgrade.UniversalUpgradeCatalogBuilder.build(mmc, player.level());
                } catch (Exception ignored) {}
            }

            root = new WidgetGroup(x, y, w, h);
            root.setBackground(new ColorRectTexture(colorBgDark));
            build();
        }

        private void build() {
            root.clearAllWidgets();
            root.addWidget(buildHeader());
            buildOptionsPanel();
            root.addWidget(buildFooter());
        }

        private WidgetGroup buildHeader() {
            WidgetGroup header = new WidgetGroup(0, 0, w, HEADER_H);
            header.setBackground(new ColorRectTexture(colorBgMed));

            int backW = 64;
            ButtonWidget back = TerminalButton.action(PAD, (HEADER_H - 18) / 2, backW, 18,
                    "§7← back", colorBgDark, colorBorder, cd -> {
                        cleanupAvailabilityRequest();
                        onBack.run();
                    });
            header.addWidget(back);

            String title = group.getGroup().displayName + "  §7×" + group.getCount();
            LabelWidget lbl = new LabelWidget(backW + PAD * 2, HEADER_H / 2 - 4, "§f" + title);
            lbl.setTextColor(C_WHITE);
            header.addWidget(lbl);

            ComponentInfo rep = group.getRepresentative();
            if (rep != null) {
                LabelWidget sub = new LabelWidget(backW + PAD * 2, HEADER_H / 2 + 6,
                        "§8currently " + rep.getTierName());
                sub.setTextColor(C_GRAY);
                header.addWidget(sub);
            }

            header.addWidget(new ImageWidget(0, HEADER_H - 1, w, 1, new ColorRectTexture(colorBorder)));
            return header;
        }

        private void buildOptionsPanel() {
            if (optionsPanel != null) root.removeWidget(optionsPanel);

            int optY = HEADER_H + 2;
            int optH = h - optY - FOOTER_H - 2;
            optionsPanel = new WidgetGroup(0, optY, w, optH);
            optionsPanel.setBackground(new ColorRectTexture(colorBgDark));

            ComponentInfo rep = group.getRepresentative();
            if (rep == null) { root.addWidget(optionsPanel); return; }

            boolean special = rep.getGroup() == ComponentGroupRegistry.MAINTENANCE
                    || rep.getGroup() == ComponentGroupRegistry.COIL;

            int innerW = w - PAD * 2;
            boolean showAvailability = MENetworkScanner.isAE2Available();
            int statusW = showAvailability ? Math.min(178, Math.max(150, innerW / 3)) : 0;
            int gap = showAvailability ? 8 : 0;
            int listW = innerW - statusW - gap;

            if (special) {
                LabelWidget lbl = new LabelWidget(PAD, 6,
                        Component.translatable("gui.gtceuterminal.inline_upgrade.select_option").getString());
                lbl.setTextColor(C_GRAY);
                optionsPanel.addWidget(lbl);

                optionsScroll = makeScroll(PAD, 20, listW, optH - 24);
                optionsPanel.addWidget(optionsScroll);
                if (showAvailability) {
                    availabilityPanel = buildAvailabilityPanel(PAD + listW + gap, 20, statusW, optH - 24);
                    optionsPanel.addWidget(availabilityPanel);
                } else {
                    availabilityPanel = null;
                }
                autoSelectFirstExplicitOption(rep);
                populateScroll(rep);
                requestAvailabilityPreview();
            } else {
                LabelWidget tierLbl = new LabelWidget(PAD, 6,
                        Component.translatable("gui.gtceuterminal.inline_upgrade.target_tier").getString());
                tierLbl.setTextColor(C_GRAY);
                optionsPanel.addWidget(tierLbl);
                
                int tierRowH = tierRowHeight(availableTiers(rep).size(), innerW);
                tierButtonsRow = new WidgetGroup(PAD, 18, innerW, tierRowH);
                optionsPanel.addWidget(tierButtonsRow);
                populateTierRow(rep);

                int divY = 18 + tierRowH + 3;
                optionsPanel.addWidget(new ImageWidget(PAD, divY, innerW, 1,
                        new ColorRectTexture(colorBorder)));

                LabelWidget varLbl = new LabelWidget(PAD, divY + 4,
                        Component.translatable("gui.gtceuterminal.inline_upgrade.variant").getString());
                varLbl.setTextColor(C_GRAY);
                optionsPanel.addWidget(varLbl);

                optionsScroll = makeScroll(PAD, divY + 16, listW, optH - divY - 20);
                optionsPanel.addWidget(optionsScroll);
                if (showAvailability) {
                    availabilityPanel = buildAvailabilityPanel(PAD + listW + gap, divY + 16, statusW, optH - divY - 20);
                    optionsPanel.addWidget(availabilityPanel);
                } else {
                    availabilityPanel = null;
                }

                if (selectedTier == null) {
                    optionsScroll.addWidget(new LabelWidget(4, 4,
                            Component.translatable("gui.gtceuterminal.inline_upgrade.select_tier_above").getString()));
                } else {
                    autoSelectFirstExplicitOption(rep);
                    populateScroll(rep);
                    requestAvailabilityPreview();
                }
            }

            root.addWidget(optionsPanel);
        }

        private void populateTierRow(ComponentInfo rep) {
            tierButtonsRow.clearAllWidgets();
            List<Integer> tiers = availableTiers(rep);
            if (tiers.isEmpty()) return;
            int innerW = tierButtonsRow.getSizeWidth();
            int cols = tierCols(tiers.size(), innerW);
            int btnW = Math.min(TIER_BTN_MAX_W, (innerW - TIER_GAP * (cols - 1)) / cols);
            for (int i = 0; i < tiers.size(); i++) {
                int tier = tiers.get(i);
                int xp = (i % cols) * (btnW + TIER_GAP);
                int yp = (i / cols) * (TIER_BTN_H + TIER_GAP);
                boolean sel = selectedTier != null && selectedTier == tier;
                int bg = sel ? 0x6600FF00 : colorBgLight, brd = sel ? C_SUCCESS : colorBorder;
                TextTexture txt = new TextTexture("§f" + UpgradeCandidateGrid.tierName(tier))
                        .setWidth(btnW).setType(TextTexture.TextType.NORMAL);
                final int t = tier;
                ButtonWidget btn = new ButtonWidget(xp, yp, btnW, TIER_BTN_H,
                        new GuiTextureGroup(new ColorRectTexture(bg), new ColorBorderTexture(1, brd), txt),
                        cd -> onTierClicked(t));
                btn.setHoverTexture(new GuiTextureGroup(new ColorRectTexture(bg),
                        new ColorBorderTexture(1, C_WHITE), txt));
                tierButtonsRow.addWidget(btn);
            }
        }
        
        private static int tierCols(int count, int innerW) {
            if (count <= 0) return 1;
            int cols = Math.max(1, (innerW + TIER_GAP) / (TIER_BTN_MIN_W + TIER_GAP));
            return Math.min(count, cols);
        }

        private static int tierRowHeight(int count, int innerW) {
            if (count <= 0) return TIER_BTN_H;
            int cols = tierCols(count, innerW);
            int rows = (count + cols - 1) / cols;
            return rows * (TIER_BTN_H + TIER_GAP) - TIER_GAP;
        }

        private void populateScroll(ComponentInfo rep) {
            optionsScroll.clearAllWidgets();
            List<UpgradeCandidateGrid.UpgradeOption> options;
            if (rep.getGroup() == ComponentGroupRegistry.MAINTENANCE) {
                options = explicitVariantOptions(rep);
                buildVariantList(options);
                return;
            }
            if (rep.getGroup() == ComponentGroupRegistry.COIL) {
                options = explicitVariantOptions(rep);
                buildVariantList(options);
                return;
            }

            options = explicitVariantOptions(rep);
            if (!options.isEmpty()) {
                buildVariantList(options);
            } else {
                buildFallbackList(rep);
            }
        }

        private void buildVariantList(List<UpgradeCandidateGrid.UpgradeOption> options) {
            if (options.isEmpty()) {
                LabelWidget empty = new LabelWidget(4, 4,
                        Component.translatable("gui.gtceuterminal.inline_upgrade.no_variants").getString());
                empty.setTextColor(C_GRAY);
                optionsScroll.addWidget(empty);
                return;
            }

            int rowW = optionsScroll.getSizeWidth() - 10;
            int rowH = 30;
            int y = 0;
            for (UpgradeCandidateGrid.UpgradeOption option : options) {
                boolean selected = selectedUpgradeId != null
                        ? option.blockId().equals(selectedUpgradeId)
                        : option.blockId().isBlank();
                optionsScroll.addWidget(makeVariantRow(option, 0, y, rowW, rowH, selected));
                y += rowH + 4;
            }
        }

        private void buildFallbackList(ComponentInfo rep) {
            if (selectedTier == null) {
                LabelWidget empty = new LabelWidget(4, 4,
                        Component.translatable("gui.gtceuterminal.inline_upgrade.select_tier_above").getString());
                empty.setTextColor(C_GRAY);
                optionsScroll.addWidget(empty);
                return;
            }

            int rowW = optionsScroll.getSizeWidth() - 10;
            String name = ComponentUpgradeHelper.getUpgradeName(rep, selectedTier);
            if (name == null || name.isBlank()) {
                name = UpgradeCandidateGrid.tierName(selectedTier) + " " + rep.getGroup().displayName;
            }
            boolean selected = selectedUpgradeId == null || selectedUpgradeId.isBlank();
            UpgradeCandidateGrid.UpgradeOption option =
                    new UpgradeCandidateGrid.UpgradeOption("", name, selectedTier);
            optionsScroll.addWidget(makeVariantRow(option, 0, 0, rowW, 30, selected));
        }

        private ButtonWidget makeVariantRow(UpgradeCandidateGrid.UpgradeOption option,
                                            int x, int y, int rowW, int rowH,
                                            boolean selected) {
            int bg = selected ? 0x6600FF00 : colorBgLight;
            int brd = selected ? C_SUCCESS : colorBorder;
            String name = truncate(option.displayName(), Math.max(18, rowW / 6));
            String tier = UpgradeCandidateGrid.tierName(option.tier());
            TextTexture txt = new TextTexture("§f" + name + "\n§7" + tier)
                    .setWidth(rowW).setType(TextTexture.TextType.NORMAL);
            ButtonWidget btn = new ButtonWidget(x, y, rowW, rowH,
                    new GuiTextureGroup(new ColorRectTexture(bg), new ColorBorderTexture(1, brd), txt),
                    cd -> selectUpgrade(option.blockId(), option.tier()));
            btn.setHoverTexture(new GuiTextureGroup(
                    new ColorRectTexture(bg), new ColorBorderTexture(1, C_WHITE), txt));
            return btn;
        }

        private void onTierClicked(int tier) {
            selectedTier      = tier;
            selectedUpgradeId = null;
            ComponentInfo rep = group.getRepresentative();
            availabilityResult = null;
            availabilityLoading = false;
            if (rep != null) {
                autoSelectFirstExplicitOption(rep);
                populateTierRow(rep);
                populateScroll(rep);
                requestAvailabilityPreview();
            }
        }

        private void selectUpgrade(String upgradeId, int tier) {
            selectedUpgradeId = upgradeId != null && !upgradeId.isBlank() ? upgradeId : null;
            selectedTier      = tier;
            ComponentInfo rep = group.getRepresentative();
            if (rep != null) populateScroll(rep);
            requestAvailabilityPreview();
        }

        private List<Integer> availableTiers(ComponentInfo rep) {
            java.util.TreeSet<Integer> tiers = new java.util.TreeSet<>();
            if (catalog != null) {
                var opt = catalog.get(rep.getPosition());
                if (opt != null && opt.candidates() != null) {
                    String cur = UpgradeCandidateGrid.blockId(rep);
                    for (var c : opt.candidates()) {
                        if (c == null || c.blockId() == null) continue;
                        if (cur != null && cur.equalsIgnoreCase(c.blockId())) continue;
                        tiers.add(c.tier());
                    }
                }
            }
            if (tiers.isEmpty()) {
                tiers.addAll(ComponentUpgradeHelper.getAvailableTiers(rep.getGroup()));
                tiers.remove(rep.getTier());
            }
            return new ArrayList<>(tiers);
        }

        private WidgetGroup buildAvailabilityPanel(int x, int y, int panelW, int panelH) {
            WidgetGroup panel = new WidgetGroup(x, y, panelW, panelH);
            panel.setBackground(new GuiTextureGroup(
                    new ColorRectTexture(0x66000000),
                    new ColorBorderTexture(1, colorBorder)));
            populateAvailabilityPanel(panel);
            return panel;
        }

        private void refreshAvailabilityPanel() {
            if (availabilityPanel == null) return;
            availabilityPanel.clearAllWidgets();
            populateAvailabilityPanel(availabilityPanel);
        }

        private void populateAvailabilityPanel(WidgetGroup panel) {
            int panelW = panel.getSizeWidth();
            int panelH = panel.getSizeHeight();

            LabelWidget title = new LabelWidget(8, 6,
                    Component.translatable("gui.gtceuterminal.inline_upgrade.me_network").getString());
            title.setTextColor(C_GRAY);
            panel.addWidget(title);

            if (!MENetworkScanner.isAE2Available()) {
                addStatusText(panel, 8, 22, "§8AE2 unavailable");
                return;
            }

            if (!hasSelectedUpgrade()) {
                addStatusText(panel, 8, 22, "§8select a variant");
                return;
            }

            if (!hasLinkedTerminal()) {
                addStatusText(panel, 8, 22, "§8link a wireless terminal");
                return;
            }

            if (availabilityLoading) {
                addStatusText(panel, 8, 22, "§7checking storage...");
                return;
            }

            if (availabilityResult == null) {
                addStatusText(panel, 8, 22, "§8select a variant");
                return;
            }

            if (availabilityResult.entries.isEmpty()) {
                addStatusText(panel, 8, 22, "§cno target item found");
                return;
            }

            AnalysisResult.Entry entry = availabilityResult.entries.get(0);
            String itemName = truncate(entry.stack.getHoverName().getString(), Math.max(16, panelW / 6));
            addStatusText(panel, 8, 24, "§f" + itemName);
            addStatusText(panel, 8, 38, "§7needed §f" + entry.needed() + " §8for ×" + group.getCount());

            boolean ready = entry.hasAll();
            int countColor = ready ? 0xFF6FCF6F : 0xFFE0A050;
            LabelWidget count = new LabelWidget(8, 54,
                    "ME §f" + entry.inME + " §7· Inv §f" + entry.inInventory + " §7/ " + entry.needed());
            count.setTextColor(countColor);
            panel.addWidget(count);

            WidgetGroup badge = new WidgetGroup(8, 70, panelW - 16, 16);
            int badgeBg;
            int badgeBorder;
            String badgeText;
            if (ready) {
                badgeBg = 0xFF153A18;
                badgeBorder = 0xFF2E7D32;
                badgeText = entry.inInventory >= entry.needed() ? "§ain inventory" : "§aavailable";
            } else if (entry.totalAvailable() > 0) {
                badgeBg = 0xFF3A2A0A;
                badgeBorder = 0xFF9A6A18;
                badgeText = "§6partial";
            } else {
                badgeBg = 0xFF3A1010;
                badgeBorder = 0xFF8A2020;
                badgeText = "§cmissing";
            }
            badge.setBackground(new GuiTextureGroup(new ColorRectTexture(badgeBg), new ColorBorderTexture(1, badgeBorder)));
            badge.addWidget(new LabelWidget(5, 4, badgeText));
            panel.addWidget(badge);

            if (!ready) {
                int btnY = Math.max(92, panelH - 26);
                ButtonWidget ac = TerminalButton.action(8, btnY, panelW - 16, 20,
                        Component.translatable("gui.gtceuterminal.component_upgrade_dialog.actions.auto_craft")
                                .getString(),
                        0xFF1A3A6B, 0xFF2E75B6, cd -> performAutoCraft());
                panel.addWidget(ac);
            }
        }

        private void addStatusText(WidgetGroup panel, int x, int y, String text) {
            LabelWidget label = new LabelWidget(x, y, text);
            label.setTextColor(C_GRAY);
            panel.addWidget(label);
        }

        private void requestAvailabilityPreview() {
            cleanupAvailabilityRequest();
            availabilityResult = null;
            availabilityLoading = false;

            if (availabilityPanel == null || !MENetworkScanner.isAE2Available()) {
                return;
            }
            if (!hasSelectedUpgrade() || !hasLinkedTerminal()) {
                refreshAvailabilityPanel();
                return;
            }

            int requestId = AVAILABILITY_REQUEST_IDS.getAndIncrement();
            pendingAvailabilityRequestId = requestId;
            availabilityLoading = true;
            AVAILABILITY_LISTENERS.put(requestId, this);
            refreshAvailabilityPanel();

            TerminalNetwork.CHANNEL.sendToServer(new CPacketRequestUpgradeAvailability(
                    requestId,
                    new ArrayList<>(group.getComponents()),
                    selectedTier != null ? selectedTier : -1,
                    selectedUpgradeId != null ? selectedUpgradeId : "",
                    mb.getControllerPos()));
        }

        private void receiveAvailabilityResult(int requestId, AnalysisResult result) {
            if (requestId != pendingAvailabilityRequestId) return;
            pendingAvailabilityRequestId = -1;
            availabilityLoading = false;
            availabilityResult = result;
            refreshAvailabilityPanel();
        }

        private void cleanupAvailabilityRequest() {
            if (pendingAvailabilityRequestId >= 0) {
                AVAILABILITY_LISTENERS.remove(pendingAvailabilityRequestId);
                pendingAvailabilityRequestId = -1;
            }
        }

        private boolean hasSelectedUpgrade() {
            if (selectedUpgradeId != null && !selectedUpgradeId.isBlank()) return true;
            ComponentInfo rep = group.getRepresentative();
            return selectedTier != null && rep != null && !hasExplicitVariantOptions(rep);
        }

        private boolean hasLinkedTerminal() {
            return MENetworkScanner.isAE2Available() && WirelessTerminalHandler.isLinked(findTerminal());
        }

        private boolean hasExplicitVariantOptions(ComponentInfo rep) {
            return !explicitVariantOptions(rep).isEmpty();
        }

        private List<UpgradeCandidateGrid.UpgradeOption> explicitVariantOptions(ComponentInfo rep) {
            if (rep == null) return List.of();
            if (rep.getGroup() == ComponentGroupRegistry.MAINTENANCE) {
                return UpgradeCandidateGrid.maintenanceCandidates(rep);
            }
            if (rep.getGroup() == ComponentGroupRegistry.COIL) {
                return UpgradeCandidateGrid.coilCandidates(rep);
            }
            if (selectedTier == null) return List.of();
            return UpgradeCandidateGrid.universalCandidates(rep, selectedTier, catalog);
        }

        private boolean autoSelectFirstExplicitOption(ComponentInfo rep) {
            List<UpgradeCandidateGrid.UpgradeOption> options = explicitVariantOptions(rep);
            if (options.isEmpty()) return false;

            if (selectedUpgradeId != null && !selectedUpgradeId.isBlank()) {
                for (UpgradeCandidateGrid.UpgradeOption option : options) {
                    if (option.blockId().equals(selectedUpgradeId)) {
                        selectedTier = option.tier();
                        return true;
                    }
                }
            }

            UpgradeCandidateGrid.UpgradeOption first = options.get(0);
            selectedUpgradeId = first.blockId() != null && !first.blockId().isBlank() ? first.blockId() : null;
            selectedTier = first.tier();
            return selectedUpgradeId != null;
        }

        private boolean ensureUpgradeSelection() {
            ComponentInfo rep = group.getRepresentative();
            if (rep == null) return false;
            if (selectedUpgradeId != null && !selectedUpgradeId.isBlank()) return true;
            if (autoSelectFirstExplicitOption(rep)) {
                if (tierButtonsRow != null) populateTierRow(rep);
                if (optionsScroll != null) populateScroll(rep);
                refreshAvailabilityPanel();
                return true;
            }
            return selectedTier != null;
        }

        private WidgetGroup buildFooter() {
            int footerY = h - FOOTER_H;
            WidgetGroup footer = new WidgetGroup(0, footerY, w, FOOTER_H);
            footer.addWidget(new ImageWidget(0, 0, w, 1, new ColorRectTexture(colorBorder)));

            int btnH = 22, btnY = (FOOTER_H - btnH) / 2;
            int confirmW = 130;
            ButtonWidget confirm = TerminalButton.action(PAD, btnY, confirmW, btnH,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.actions.confirm_change")
                            .getString(),
                    0xFF2E7D32, C_SUCCESS, cd -> performUpgrade());
            footer.addWidget(confirm);

            return footer;
        }

        private void performUpgrade() {
            if (!ensureUpgradeSelection()) {
                player.displayClientMessage(Component.translatable(
                        "gui.gtceuterminal.component_upgrade_dialog.chat.select_upgrade_option_first"), true);
                return;
            }
            List<net.minecraft.core.BlockPos> positions = new ArrayList<>();
            for (ComponentInfo c : group.getComponents()) positions.add(c.getPosition());
            TerminalNetwork.CHANNEL.sendToServer(new CPacketComponentUpgrade(
                    positions, selectedTier, selectedUpgradeId, mb.getControllerPos()));
            player.displayClientMessage(Component.translatable(
                    "gui.gtceuterminal.component_upgrade_dialog.chat.changing_components",
                    group.getCount()), true);
            cleanupAvailabilityRequest();
            onBack.run();
        }

        private void performAutoCraft() {
            if (!ensureUpgradeSelection()) {
                player.displayClientMessage(Component.translatable(
                        "gui.gtceuterminal.component_upgrade_dialog.chat.select_upgrade_option_first"), true);
                return;
            }
            TerminalNetwork.CHANNEL.sendToServer(new CPacketRequestUpgradeAnalysis(
                    new ArrayList<>(group.getComponents()),
                    selectedTier != null ? selectedTier : -1,
                    selectedUpgradeId != null ? selectedUpgradeId : "",
                    mb.getControllerPos()));
            player.displayClientMessage(Component.translatable(
                    "gui.gtceuterminal.component_upgrade_dialog.chat.analyzing_me_network"), true);
            cleanupAvailabilityRequest();
            onBack.run();
        }

        // Manager-only: this panel belongs to the Multi-Structure Manager, so its ME availability
        // must come from a linked manager — not a linked Schematic Interface in the inventory.
        private ItemStack findTerminal() {
            for (ItemStack s : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
                if (WirelessTerminalHandler.isOurLinkedManager(s)) return s;
            }
            for (ItemStack s : player.getInventory().items) {
                if (WirelessTerminalHandler.isOurLinkedManager(s)) return s;
            }
            return ItemStack.EMPTY;
        }

        private DraggableScrollableWidgetGroup makeScroll(int x, int y, int w, int h) {
            DraggableScrollableWidgetGroup s = new DraggableScrollableWidgetGroup(x, y, w, h);
            s.setYScrollBarWidth(6);
            s.setYBarStyle(new ColorRectTexture(0xFF0A0A0A), new ColorRectTexture(colorBorder));
            return s;
        }

        private String truncate(String s, int max) {
            if (s == null) return "";
            return s.length() > max ? s.substring(0, max - 1) + "…" : s;
        }
    }
}
