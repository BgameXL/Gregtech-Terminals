package com.gtceuterminal.client.gui.dialog;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.multiblock.ComponentDetailUI;
import com.gtceuterminal.client.gui.widget.LDLMaterialListWidget;
import com.gtceuterminal.common.ae2.WirelessTerminalHandler;
import com.gtceuterminal.common.config.MaintenanceHatchConfig;
import com.gtceuterminal.common.material.ComponentUpgradeHelper;
import com.gtceuterminal.common.material.MaterialAvailability;
import com.gtceuterminal.common.material.MaterialCalculator;
import com.gtceuterminal.common.multiblock.ComponentGroup;
import com.gtceuterminal.common.multiblock.ComponentInfo;
import com.gtceuterminal.common.multiblock.ComponentType;
import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.network.CPacketComponentUpgrade;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.common.upgrade.UniversalUpgradeCatalog;
import com.gtceuterminal.common.upgrade.UniversalUpgradeCatalogBuilder;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ComponentUpgradeDialog extends DialogWidget {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int DIALOG_W  = 460;
    private static final int DIALOG_H  = 430;
    private static final int HEADER_H  = 28;
    private static final int INFO_H    = 38;
    private static final int OPTIONS_H = 80;
    private static final int PAD       = 10;

    // Y positions inside content (computed from constants)
    private static final int INFO_Y    = 2 + HEADER_H + 3;
    private static final int OPTIONS_Y = INFO_Y + INFO_H + 4;
    // MAT_Y uses two-zone height (130) as default
    private static final int MAT_Y     = OPTIONS_Y + 130 + 4;
    private static final int BUTTONS_Y = DIALOG_H - 36;
    private static final int MAT_H     = BUTTONS_Y - MAT_Y - 4;

    // ── Colors ────────────────────────────────────────────────────────────────
    private int COLOR_BG_DARK;
    private int COLOR_BG_MEDIUM;
    private int COLOR_BG_LIGHT;
    private int COLOR_BORDER_LIGHT;
    private static final int COLOR_BORDER_DARK = 0xFF0A0A0A;
    private static final int COLOR_TEXT_WHITE  = 0xFFFFFFFF;
    private static final int COLOR_TEXT_GRAY   = 0xFFAAAAAA;
    private static final int COLOR_SUCCESS     = 0xFF00FF00;
    private static final int COLOR_ERROR       = 0xFFFF0000;

    // ── State ─────────────────────────────────────────────────────────────────
    private ItemTheme theme;
    private final ComponentGroup   group;
    private final MultiblockInfo   multiblock;
    private final Player           player;
    private final DialogWidget     parentDialog;
    @SuppressWarnings("unused")
    private final ComponentDetailUI parentUI;

    private UniversalUpgradeCatalog universalCatalog;

    private Integer  tierFilter      = null; // null = show all
    private int      selectedTier    = -1;
    private String   selectedUpgradeId = null;

    private List<MaterialAvailability> materials;

    private WidgetGroup content;
    private WidgetGroup tierSelectionPanel;
    private WidgetGroup materialsPanel;

    private DraggableScrollableWidgetGroup optionsScroll;
    private ComponentInfo currentRep;

    // ── Constructors ──────────────────────────────────────────────────────────
    public ComponentUpgradeDialog(
            WidgetGroup parent,
            ComponentDetailUI parentUI,
            DialogWidget parentDialog,
            ComponentGroup group,
            MultiblockInfo multiblock,
            Player player) {
        this(parent, parentUI, parentDialog, group, multiblock, player, null);
    }

    public ComponentUpgradeDialog(
            WidgetGroup parent,
            ComponentDetailUI parentUI,
            DialogWidget parentDialog,
            ComponentGroup group,
            MultiblockInfo multiblock,
            Player player,
            ItemTheme passedTheme) {
        super(parent, true);
        this.parentUI     = parentUI;
        this.parentDialog = parentDialog;
        this.group        = group;
        this.multiblock   = multiblock;
        this.player       = player;
        this.theme        = passedTheme != null ? passedTheme : ItemTheme.loadFromPlayer(player);
        applyTheme();
        initDialog();
    }

    private void applyTheme() {
        COLOR_BG_DARK      = theme.bgColor;
        COLOR_BG_MEDIUM    = theme.panelColor;
        COLOR_BG_LIGHT     = theme.isNativeStyle() ? 0xFF3A3A3A : theme.accent(0xAA);
        COLOR_BORDER_LIGHT = theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF);
    }

    @Override
    public void close() {
        super.close();
        if (parentDialog != null) parentDialog.setActive(true);
    }

    // ── Layout init ───────────────────────────────────────────────────────────
    private void initDialog() {
        // Build universal catalog before building the UI
        var controller = multiblock.getController();
        if (controller instanceof MultiblockControllerMachine mmc) {
            universalCatalog = UniversalUpgradeCatalogBuilder.build(mmc, player.level());
        }

        var mc   = Minecraft.getInstance();
        int sw   = mc.screen != null ? mc.screen.width  : mc.getWindow().getGuiScaledWidth();
        int sh   = mc.screen != null ? mc.screen.height : mc.getWindow().getGuiScaledHeight();
        int margin = 10;
        int maxW = sw - margin * 2;
        int maxH = sh - margin * 2;
        int viewW = Math.min(DIALOG_W, maxW);
        int viewH = Math.min(DIALOG_H, maxH);

        int screenX = Mth.clamp((sw - viewW) / 2, margin, sw - viewW - margin);
        int screenY = Mth.clamp((sh - viewH) / 2, margin, sh - viewH - margin);
        Position abs = parent.getPosition();
        setSelfPosition(new Position(screenX - abs.x, screenY - abs.y));
        setSize(new Size(viewW, viewH));
        setBackground(theme.backgroundTexture());

        // Build full-size content
        this.content = buildContent(DIALOG_W, DIALOG_H);

        if (DIALOG_W <= maxW && DIALOG_H <= maxH) {
            // Fits — flatten into this dialog directly
            for (Widget w : new ArrayList<>(content.widgets)) {
                addWidget(w);
            }
            // Keep content reference valid for refresh methods
        } else {
            // Needs scroll wrapper
            DraggableScrollableWidgetGroup viewport =
                    new DraggableScrollableWidgetGroup(0, 0, viewW, viewH);
            viewport.setYScrollBarWidth(8);
            viewport.setYBarStyle(
                    new ColorRectTexture(COLOR_BORDER_DARK),
                    new ColorRectTexture(COLOR_BORDER_LIGHT));
            viewport.addWidget(content);
            addWidget(viewport);
        }
    }

    // ── Content builder ───────────────────────────────────────────────────────
    private WidgetGroup buildContent(int cW, int cH) {
        WidgetGroup root = new WidgetGroup(0, 0, cW, cH);
        root.setBackground(theme.backgroundTexture());

        // Outer border
        if (!theme.isNativeStyle()) {
            root.addWidget(new ImageWidget(0,      0,      cW, 2,  new ColorRectTexture(COLOR_BORDER_LIGHT)));
            root.addWidget(new ImageWidget(0,      0,      2,  cH, new ColorRectTexture(COLOR_BORDER_LIGHT)));
            root.addWidget(new ImageWidget(cW - 2, 0,      2,  cH, new ColorRectTexture(COLOR_BORDER_DARK)));
            root.addWidget(new ImageWidget(0,      cH - 2, cW, 2,  new ColorRectTexture(COLOR_BORDER_DARK)));
        }

        root.addWidget(buildHeader(cW));
        root.addWidget(buildInfoPanel(cW));

        this.tierSelectionPanel = buildOptionsPanel(cW);
        root.addWidget(tierSelectionPanel);

        this.materialsPanel = buildMaterialsPanel(cW);
        root.addWidget(materialsPanel);

        root.addWidget(buildButtons(cW, cH));

        return root;
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private WidgetGroup buildHeader(int cW) {
        WidgetGroup header = new WidgetGroup(2, 2, cW - 4, HEADER_H);
        header.setBackground(theme.headerTexture());

        String title = Component.translatable(
                "gui.gtceuterminal.component_upgrade_dialog.title",
                group.getType().getDisplayNameComponent()
        ).getString();
        LabelWidget titleLabel = new LabelWidget(10, 9, "§f" + title);
        titleLabel.setTextColor(COLOR_TEXT_WHITE);
        header.addWidget(titleLabel);

        
        ButtonWidget closeBtn = new ButtonWidget(cW - 30, 4, 22, 20,
                new GuiTextureGroup(
                        new ColorRectTexture(COLOR_BG_MEDIUM),
                        new ColorBorderTexture(1, COLOR_BORDER_LIGHT)),
                cd -> close());
        closeBtn.setButtonTexture(
                new TextTexture("§c✕").setWidth(22).setType(TextTexture.TextType.NORMAL));
        closeBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(0xFFAA0000),
                new ColorBorderTexture(1, COLOR_TEXT_WHITE)));
        header.addWidget(closeBtn);

        return header;
    }

    // ── Info panel ────────────────────────────────────────────────────────────
    private WidgetGroup buildInfoPanel(int cW) {
        WidgetGroup panel = new WidgetGroup(PAD, INFO_Y, cW - PAD * 2, INFO_H);
        panel.setBackground(theme.panelTexture());

        ComponentInfo rep = group.getRepresentative();
        if (rep == null) return panel;

        String tierName = (rep.getType() == ComponentType.COIL)
                ? ComponentType.getCoilTierName(rep.getTier())
                : rep.getTierName();

        // "×4 Energy Hatch  —  Current tier: HV"
        String line1 = Component.translatable(
                "gui.gtceuterminal.component_upgrade_dialog.info.count_components",
                group.getCount()
        ).getString();
        String line2 = Component.translatable(
                "gui.gtceuterminal.component_upgrade_dialog.info.current_tier",
                tierName
        ).getString();

        LabelWidget l1 = new LabelWidget(10, 6,  "§f" + line1);
        LabelWidget l2 = new LabelWidget(10, 19, "§7" + line2);
        l1.setTextColor(COLOR_TEXT_WHITE);
        l2.setTextColor(COLOR_TEXT_GRAY);
        panel.addWidget(l1);
        panel.addWidget(l2);

        return panel;
    }

    // ── Options panel (tier / upgrade selection) ──────────────────────────────
    private static final int OPTIONS_H_TWO_ZONE = 130;
    // Scroll reference for the tier button row (to update selection highlight)
    private WidgetGroup tierButtonsRow;

    private WidgetGroup buildOptionsPanel(int cW) {
        ComponentInfo rep = group.getRepresentative();

        boolean isSpecialType = rep != null && (rep.getType() == ComponentType.MAINTENANCE
                || rep.getType() == ComponentType.COIL);
        int panelH = isSpecialType ? OPTIONS_H : OPTIONS_H_TWO_ZONE;

        WidgetGroup panel = new WidgetGroup(PAD, OPTIONS_Y, cW - PAD * 2, panelH);
        panel.setBackground(theme.panelTexture());

        if (rep == null) return panel;
        this.currentRep = rep;

        int scrollW = cW - PAD * 2 - 20;

        if (isSpecialType) {
            // Single-zone: just the variant grid
            LabelWidget label = new LabelWidget(10, 5,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.tier_selection.label_upgrade_option").getString());
            label.setTextColor(COLOR_TEXT_WHITE);
            panel.addWidget(label);

            DraggableScrollableWidgetGroup scroll =
                    new DraggableScrollableWidgetGroup(10, 20, scrollW, panelH - 28);
            scroll.setYScrollBarWidth(6);
            scroll.setYBarStyle(new ColorRectTexture(COLOR_BORDER_DARK), new ColorRectTexture(0xFF888888));
            panel.addWidget(scroll);
            this.optionsScroll = scroll;
            populateOptionsScroll(rep);

        } else {
            LabelWidget tierLabel = new LabelWidget(10, 5,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.tier_selection.label_target_tier").getString());
            tierLabel.setTextColor(COLOR_TEXT_WHITE);
            panel.addWidget(tierLabel);

            int tierRowH = 22;
            WidgetGroup tierRow = new WidgetGroup(10, 18, scrollW, tierRowH);
            panel.addWidget(tierRow);

            List<Integer> availableTiers = getAvailableTiersFromCatalog(rep);
            int spacing = 2;
            int btnW = availableTiers.isEmpty() ? 30
                    : Math.min(40, (scrollW - spacing * (availableTiers.size() - 1)) / availableTiers.size());
            int btnH = tierRowH;
            int xPos = 0;
            for (int tier : availableTiers) {
                boolean sel = (tierFilter != null && tierFilter == tier);
                int bg     = sel ? 0x6600FF00 : COLOR_BG_LIGHT;
                int border = sel ? 0xFF00FF00 : COLOR_BORDER_LIGHT;
                TextTexture txt = new TextTexture("§f" + safeTierName(tier))
                        .setWidth(btnW).setType(TextTexture.TextType.NORMAL);
                final int t = tier;
                ButtonWidget btn = new ButtonWidget(xPos, 0, btnW, btnH,
                        new GuiTextureGroup(new ColorRectTexture(bg), new ColorBorderTexture(1, border), txt),
                        cd -> onTierButtonClicked(t));
                btn.setHoverTexture(new GuiTextureGroup(
                        new ColorRectTexture(bg), new ColorBorderTexture(1, COLOR_TEXT_WHITE), txt));
                tierRow.addWidget(btn);
                xPos += btnW + spacing;
            }
            this.tierButtonsRow = tierRow;

            int divY = 18 + tierRowH + 3;
            panel.addWidget(new ImageWidget(10, divY, scrollW, 1, new ColorRectTexture(COLOR_BORDER_LIGHT)));

            int varY = divY + 4;
            int varH = panelH - varY - 4;

            DraggableScrollableWidgetGroup scroll =
                    new DraggableScrollableWidgetGroup(10, varY, scrollW, varH);
            scroll.setYScrollBarWidth(6);
            scroll.setYBarStyle(new ColorRectTexture(COLOR_BORDER_DARK), new ColorRectTexture(0xFF888888));
            panel.addWidget(scroll);
            this.optionsScroll = scroll;

            if (tierFilter != null) {
                populateOptionsScroll(rep);
            } else {
                scroll.addWidget(new LabelWidget(4, 4, "§7Select a tier above"));
            }
        }

        return panel;
    }

    private void populateTierButtons(ComponentInfo rep, WidgetGroup tierRow) {
        tierRow.clearAllWidgets();
        List<Integer> availableTiers = getAvailableTiersFromCatalog(rep);
        int spacing = 2;
        int scrollW = DIALOG_W - PAD * 2 - 20;
        int btnW = availableTiers.isEmpty() ? 30
                : Math.min(40, (scrollW - spacing * (availableTiers.size() - 1)) / availableTiers.size());
        int btnH = 22;
        int xPos = 0;
        for (int tier : availableTiers) {
            boolean sel = (tierFilter != null && tierFilter == tier);
            int bg     = sel ? 0x6600FF00 : COLOR_BG_LIGHT;
            int border = sel ? 0xFF00FF00 : COLOR_BORDER_LIGHT;
            TextTexture txt = new TextTexture("§f" + safeTierName(tier))
                    .setWidth(btnW).setType(TextTexture.TextType.NORMAL);
            final int t = tier;
            ButtonWidget btn = new ButtonWidget(xPos, 0, btnW, btnH,
                    new GuiTextureGroup(new ColorRectTexture(bg), new ColorBorderTexture(1, border), txt),
                    cd -> onTierButtonClicked(t));
            btn.setHoverTexture(new GuiTextureGroup(
                    new ColorRectTexture(bg), new ColorBorderTexture(1, COLOR_TEXT_WHITE), txt));
            tierRow.addWidget(btn);
            xPos += btnW + spacing;
        }
    }

    private List<Integer> getAvailableTiersFromCatalog(ComponentInfo rep) {
        java.util.TreeSet<Integer> tiers = new java.util.TreeSet<>();
        if (universalCatalog != null) {
            var opt = universalCatalog.get(rep.getPosition());
            if (opt != null && opt.candidates() != null) {
                String currentId = blockId(rep);
                for (var c : opt.candidates()) {
                    if (c == null || c.blockId() == null) continue;
                    if (currentId != null && currentId.equalsIgnoreCase(c.blockId())) continue;
                    tiers.add(c.tier());
                }
            }
        }
        if (tiers.isEmpty()) {
            tiers.addAll(ComponentUpgradeHelper.getAvailableTiers(rep.getType()));
            tiers.remove(rep.getTier());
        }
        return new ArrayList<>(tiers);
    }

    private void populateOptionsScroll(ComponentInfo rep) {
        if (optionsScroll == null) return;
        optionsScroll.clearAllWidgets();

        if (rep.getType() == ComponentType.MAINTENANCE) {
            buildMaintenanceGrid(rep, optionsScroll);
            return;
        }
        if (rep.getType() == ComponentType.COIL) {
            buildCoilGrid(rep, optionsScroll);
            return;
        }
        if (!buildUniversalCandidatesGrid(rep, optionsScroll, tierFilter)) {
            List<Integer> tiers = ComponentUpgradeHelper.getAvailableTiers(rep.getType());
            buildTierGrid(rep, optionsScroll, tiers);
        }
    }


    // ── Materials panel ───────────────────────────────────────────────────────
    private WidgetGroup buildMaterialsPanel(int cW) {
        WidgetGroup panel = new WidgetGroup(PAD, MAT_Y, cW - PAD * 2, MAT_H);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(COLOR_BG_DARK),
                new ColorBorderTexture(1, COLOR_BORDER_DARK)));

        LabelWidget sectionLabel = new LabelWidget(10, 5,
                Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.required_label").getString());
        sectionLabel.setTextColor(COLOR_TEXT_WHITE);
        panel.addWidget(sectionLabel);

        // Placeholder shown before anything is selected
        LabelWidget placeholder = new LabelWidget(10, 22,
                Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.select_option_to_see").getString());
        placeholder.setTextColor(COLOR_TEXT_GRAY);
        panel.addWidget(placeholder);

        return panel;
    }

    private void refreshMaterialsPanel() {
        if (materialsPanel == null || content == null) return;
        content.removeWidget(materialsPanel);

        int cW = DIALOG_W;
        WidgetGroup panel = new WidgetGroup(PAD, MAT_Y, cW - PAD * 2, MAT_H);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(COLOR_BG_DARK),
                new ColorBorderTexture(1, COLOR_BORDER_DARK)));

        LabelWidget sectionLabel = new LabelWidget(10, 5,
                Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.required_label").getString());
        sectionLabel.setTextColor(COLOR_TEXT_WHITE);
        panel.addWidget(sectionLabel);

        if (selectedTier == -1) {
            // Nothing selected yet
            LabelWidget placeholder = new LabelWidget(10, 22,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.select_option_to_see").getString());
            placeholder.setTextColor(COLOR_TEXT_GRAY);
            panel.addWidget(placeholder);
        } else if (player.isCreative()) {
            LabelWidget creative = new LabelWidget(10, 22,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.creative_mode").getString());
            creative.setTextColor(COLOR_SUCCESS);
            panel.addWidget(creative);
            if (materials != null && !materials.isEmpty()) {
                panel.addWidget(new LDLMaterialListWidget(0, 38, cW - PAD * 2, MAT_H - 42, materials));
            }
        } else if (materials != null && !materials.isEmpty()) {
            panel.addWidget(new LDLMaterialListWidget(0, 20, cW - PAD * 2, MAT_H - 24, materials));
        } else {
            LabelWidget none = new LabelWidget(10, 22,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.select_option_to_see").getString());
            none.setTextColor(COLOR_TEXT_GRAY);
            panel.addWidget(none);
        }

        this.materialsPanel = panel;
        content.addWidget(materialsPanel);

        if (!(widgets.contains(content))) {
            removeWidget(getWidgetOfType(WidgetGroup.class, MAT_Y));
            addWidget(materialsPanel);
        }
    }

    private WidgetGroup getWidgetOfType(Class<WidgetGroup> cls, int y) {
        for (Widget w : new ArrayList<>(widgets)) {
            if (cls.isInstance(w) && w.getPosition().y == y) return (WidgetGroup) w;
        }
        return null;
    }

    // ── Action buttons ────────────────────────────────────────────────────────
    private WidgetGroup buildButtons(int cW, int cH) {
        WidgetGroup buttons = new WidgetGroup(PAD, BUTTONS_Y, cW - PAD * 2, 28);

        // Confirm — green
        int confirmW = 150;
        ButtonWidget confirmBtn = new ButtonWidget(0, 0, confirmW, 24,
                new GuiTextureGroup(
                        new ColorRectTexture(0xFF2E7D32),
                        new ColorBorderTexture(1, COLOR_SUCCESS)),
                cd -> performUpgrade());
        confirmBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.component_upgrade_dialog.actions.confirm_change").getString())
                .setWidth(confirmW).setType(TextTexture.TextType.NORMAL));
        confirmBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(0xFF43A047),
                new ColorBorderTexture(1, COLOR_TEXT_WHITE)));
        buttons.addWidget(confirmBtn);

        // Auto-craft — blue, only when ME linked
        boolean showAutoCraft = WirelessTerminalHandler.isLinked(findWirelessTerminal());
        int autoCraftW = 110;
        if (showAutoCraft) {
            ButtonWidget autoCraftBtn = new ButtonWidget(confirmW + 8, 0, autoCraftW, 24,
                    new GuiTextureGroup(
                            new ColorRectTexture(0xFF1A3A6B),
                            new ColorBorderTexture(1, 0xFF2E75B6)),
                    cd -> performAutoCraft());
            autoCraftBtn.setButtonTexture(new TextTexture(
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.actions.auto_craft").getString())
                    .setWidth(autoCraftW).setType(TextTexture.TextType.NORMAL));
            autoCraftBtn.setHoverTexture(new GuiTextureGroup(
                    new ColorRectTexture(0xFF243A6B),
                    new ColorBorderTexture(1, COLOR_TEXT_WHITE)));
            buttons.addWidget(autoCraftBtn);
        }

        // Cancel — right side
        int cancelW = 120;
        ButtonWidget cancelBtn = new ButtonWidget(cW - PAD * 2 - cancelW, 0, cancelW, 24,
                new GuiTextureGroup(
                        new ColorRectTexture(COLOR_BG_LIGHT),
                        new ColorBorderTexture(1, COLOR_BORDER_LIGHT)),
                cd -> close());
        cancelBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.component_upgrade_dialog.actions.cancel").getString())
                .setWidth(cancelW).setType(TextTexture.TextType.NORMAL));
        cancelBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(COLOR_BG_LIGHT),
                new ColorBorderTexture(1, COLOR_TEXT_WHITE)));
        buttons.addWidget(cancelBtn);

        return buttons;
    }

    // ── Grid builders ───────────────────────────
    private boolean buildUniversalCandidatesGrid(ComponentInfo rep,
                                                 DraggableScrollableWidgetGroup scroll,
                                                 Integer filterTier) {
        if (universalCatalog == null) return false;
        var opt = universalCatalog.get(rep.getPosition());
        if (opt == null) return false;
        var candidates = opt.candidates();
        if (candidates == null || candidates.isEmpty()) return false;

        String currentId = rep.getState().getBlock().builtInRegistryHolder().key().location().toString();

        List<UniversalUpgradeCatalog.CandidatePart> list = new ArrayList<>();
        for (var c : candidates) {
            if (c == null || c.blockId() == null) continue;
            if (currentId != null && currentId.equalsIgnoreCase(c.blockId())) continue;
            if (filterTier != null && c.tier() != filterTier) continue;
            list.add(c);
        }
        if (list.isEmpty()) return false;

        list.sort(Comparator.comparingInt(UniversalUpgradeCatalog.CandidatePart::tier)
                .thenComparing(UniversalUpgradeCatalog.CandidatePart::blockId, String.CASE_INSENSITIVE_ORDER));

        int btnW = 120, btnH = 26, spacing = 6, perRow = 3, xPos = 0, yPos = 0, added = 0;
        for (var c : list) {
            if (added > 0 && added % perRow == 0) { xPos = 0; yPos += btnH + spacing; }
            String display = resolveBlockName(c.blockId(), 16);
            boolean isSelected = c.blockId().equalsIgnoreCase(selectedUpgradeId);
            scroll.addWidget(createOptionButton(
                    "§f" + display + "\n§7(" + safeTierName(c.tier()) + ")",
                    isSelected, xPos, yPos, btnW, btnH,
                    () -> selectUpgradeOption(c.blockId(), c.tier())));
            xPos += btnW + spacing;
            added++;
        }
        return added > 0;
    }

    private void buildTierGrid(ComponentInfo rep, DraggableScrollableWidgetGroup scroll, List<Integer> tiers) {
        int btnW = 48, btnH = 24, spacing = 3, perRow = 7, xPos = 0, yPos = 0, added = 0;

        scroll.addWidget(createOptionButton(
                Component.translatable("gui.gtceuterminal.component_upgrade_dialog.option_all_any").getString(),
                tierFilter == null, xPos, yPos, btnW, btnH, this::onShowAllClicked));
        xPos += btnW + spacing;
        added++;

        for (int tier : new java.util.LinkedHashSet<>(tiers)) {
            if (tier == rep.getTier()) continue;
            if (added % perRow == 0) { xPos = 0; yPos += btnH + spacing; }
            scroll.addWidget(createTierButton(tier, xPos, yPos, btnW, btnH));
            xPos += btnW + spacing;
            added++;
        }
    }

    private void buildMaintenanceGrid(ComponentInfo rep, DraggableScrollableWidgetGroup scroll) {
        String currentId = blockId(rep);
        List<MaintenanceHatchConfig.MaintenanceHatchEntry> entries =
                new ArrayList<>(MaintenanceHatchConfig.getAllHatches());
        entries.removeIf(Objects::isNull);
        entries.sort(Comparator.<MaintenanceHatchConfig.MaintenanceHatchEntry>comparingInt(e -> e.tier)
                .thenComparing(e -> String.valueOf(e.displayName), String.CASE_INSENSITIVE_ORDER));

        int btnW = 118, btnH = 26, spacing = 4, perRow = 3, xPos = 0, yPos = 0, added = 0;
        for (var e : entries) {
            if (e == null || e.blockId == null) continue;
            if (currentId != null && currentId.equalsIgnoreCase(e.blockId)) continue;
            if (added > 0 && added % perRow == 0) { xPos = 0; yPos += btnH + spacing; }
            String name = trimSuffixes(resolveBlockName(e.blockId, 20));
            if (name.isBlank()) name = e.displayName != null ? e.displayName : e.blockId;
            String tierTag = (e.tierName != null && !e.tierName.isBlank()) ? e.tierName : safeTierName(e.tier);
            boolean sel = e.blockId.equals(selectedUpgradeId);
            scroll.addWidget(createOptionButton(
                    "§f" + name + "\n§7(" + tierTag + ")", sel, xPos, yPos, btnW, btnH,
                    () -> selectUpgradeOption(e.blockId, e.tier)));
            xPos += btnW + spacing;
            added++;
        }
    }

    private void buildCoilGrid(ComponentInfo rep, DraggableScrollableWidgetGroup scroll) {
        record CoilEntry(String blockId, String display, int tier) {}
        List<CoilEntry> list = new ArrayList<>();
        for (var entry : GTCEuAPI.HEATING_COILS.entrySet()) {
            ICoilType coilType = entry.getKey();
            if (coilType == null) continue;
            Block block = null;
            try { block = entry.getValue() != null ? entry.getValue().get() : null; }
            catch (RuntimeException e) {
                GTCEUTerminalMod.LOGGER.warn("ComponentUpgradeDialog: could not resolve coil block for '{}': {}",
                        coilType.getName(), e.getMessage());
            }
            if (block == null) continue;
            String id   = BuiltInRegistries.BLOCK.getKey(block).toString();
            String name = trimSuffixes(Component.translatable(block.getDescriptionId()).getString());
            if (name.isBlank()) name = coilType.getName();
            list.add(new CoilEntry(id, name, coilType.getTier()));
        }
        list.sort(Comparator.comparingInt(CoilEntry::tier)
                .thenComparing(CoilEntry::display, String.CASE_INSENSITIVE_ORDER));

        String currentId = blockId(rep);
        int btnW = 118, btnH = 26, spacing = 4, perRow = 3, xPos = 0, yPos = 0, added = 0;
        for (var e : list) {
            if (e == null || e.blockId() == null) continue;
            if (currentId != null && currentId.equalsIgnoreCase(e.blockId())) continue;
            if (added > 0 && added % perRow == 0) { xPos = 0; yPos += btnH + spacing; }
            boolean sel = e.blockId().equals(selectedUpgradeId);
            scroll.addWidget(createOptionButton(
                    "§f" + e.display() + "\n§7(" + safeTierName(e.tier()) + ")",
                    sel, xPos, yPos, btnW, btnH,
                    () -> selectUpgradeOption(e.blockId(), e.tier())));
            xPos += btnW + spacing;
            added++;
        }
    }

    // ── Button factories ──────────────────────────────────────────────────────
    private ButtonWidget createTierButton(int tier, int x, int y, int w, int h) {
        boolean sel = (tier == selectedTier);
        int bg = sel ? 0x6600FF00 : COLOR_BG_LIGHT;
        int border = sel ? 0xFF00FF00 : COLOR_BORDER_LIGHT;
        TextTexture text = new TextTexture("§f" + safeTierName(tier)).setWidth(w).setType(TextTexture.TextType.NORMAL);
        ButtonWidget btn = new ButtonWidget(x, y, w, h,
                new GuiTextureGroup(new ColorRectTexture(bg), new ColorBorderTexture(1, border), text),
                cd -> onTierButtonClicked(tier));
        btn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(bg), new ColorBorderTexture(1, COLOR_TEXT_WHITE), text));
        return btn;
    }

    private ButtonWidget createOptionButton(String textLines, boolean isSelected,
                                            int x, int y, int w, int h, Runnable onClick) {
        int bg = isSelected ? 0x6600FF00 : COLOR_BG_LIGHT;
        int border = isSelected ? 0xFF00FF00 : COLOR_BORDER_LIGHT;
        TextTexture text = new TextTexture(textLines).setWidth(w).setType(TextTexture.TextType.NORMAL);
        ButtonWidget btn = new ButtonWidget(x, y, w, h,
                new GuiTextureGroup(new ColorRectTexture(bg), new ColorBorderTexture(1, border), text),
                cd -> onClick.run());
        btn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(bg), new ColorBorderTexture(1, COLOR_TEXT_WHITE), text));
        return btn;
    }

    // ── Event handlers ────────────────────────────────────────────────────────
    private void onTierButtonClicked(int tier) {
        this.selectedTier      = tier;
        this.tierFilter        = tier;
        this.selectedUpgradeId = null;
        // Repopulate tier buttons to update selection highlight
        if (tierButtonsRow != null && currentRep != null) {
            populateTierButtons(currentRep, tierButtonsRow);
        }
        // Repopulate variant scroll
        if (currentRep != null) populateOptionsScroll(currentRep);
        calculateMaterials();
        refreshMaterialsPanel();
    }

    private void onShowAllClicked() {
        this.tierFilter        = null;
        this.selectedUpgradeId = null;
        if (tierButtonsRow != null && currentRep != null) {
            populateTierButtons(currentRep, tierButtonsRow);
        }
        if (optionsScroll != null) {
            optionsScroll.clearAllWidgets();
            optionsScroll.addWidget(new LabelWidget(4, 4, "§7Select a tier above"));
        }
        calculateMaterials();
        refreshMaterialsPanel();
    }

    private void refreshOptionsPanel() {
    }

    private void selectUpgradeOption(String upgradeId, int tier) {
        this.selectedUpgradeId = upgradeId;
        this.selectedTier      = tier;
        GTCEUTerminalMod.LOGGER.info("Selected upgrade: {} (tier {})", upgradeId, tier);
        calculateMaterials();
        refreshMaterialsPanel();
    }

    // ── Business logic ────────────────────────────────────────────────────────
    private void calculateMaterials() {
        if (selectedTier == -1) return;
        ComponentInfo rep = group.getRepresentative();
        if (rep == null) return;

        Map<Item, Integer> singleRequired = (selectedUpgradeId != null && !selectedUpgradeId.isBlank())
                ? ComponentUpgradeHelper.getUpgradeItemsForBlockId(selectedUpgradeId)
                : ComponentUpgradeHelper.getUpgradeItems(rep, selectedTier);

        Map<Item, Integer> totalRequired = new HashMap<>();
        for (var e : singleRequired.entrySet()) {
            totalRequired.put(e.getKey(), e.getValue() * group.getCount());
        }

        materials = MaterialCalculator.checkMaterialsAvailability(totalRequired, player, player.level());
    }

    private void performUpgrade() {
        if (selectedTier == -1) {
            player.displayClientMessage(
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.chat.select_upgrade_option_first"),
                    true);
            return;
        }
        List<net.minecraft.core.BlockPos> positions = new ArrayList<>();
        for (ComponentInfo c : group.getComponents()) positions.add(c.getPosition());

        TerminalNetwork.CHANNEL.sendToServer(
                new CPacketComponentUpgrade(positions, selectedTier, selectedUpgradeId, multiblock.getControllerPos()));

        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.component_upgrade_dialog.chat.changing_components",
                        group.getCount()), true);
        close();
        if (parentDialog != null) parentDialog.close();
    }

    private void performAutoCraft() {
        if (selectedTier == -1 && (selectedUpgradeId == null || selectedUpgradeId.isBlank())) {
            player.displayClientMessage(
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.chat.select_upgrade_option_first"),
                    true);
            return;
        }
        TerminalNetwork.CHANNEL.sendToServer(
                new com.gtceuterminal.common.network.CPacketRequestUpgradeAnalysis(
                        new ArrayList<>(group.getComponents()),
                        selectedTier,
                        selectedUpgradeId != null ? selectedUpgradeId : "",
                        multiblock.getControllerPos()));
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.component_upgrade_dialog.chat.analyzing_me_network"), true);
        close();
        if (parentDialog != null) parentDialog.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private ItemStack findWirelessTerminal() {
        if (player == null) return ItemStack.EMPTY;
        for (ItemStack s : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (WirelessTerminalHandler.isWirelessTerminal(s)) return s;
        }
        for (ItemStack s : player.getInventory().items) {
            if (WirelessTerminalHandler.isWirelessTerminal(s)) return s;
        }
        return ItemStack.EMPTY;
    }

    private static String blockId(ComponentInfo rep) {
        try { return rep.getState().getBlock().builtInRegistryHolder().key().location().toString(); }
        catch (IllegalStateException e) {
            GTCEUTerminalMod.LOGGER.debug("ComponentUpgradeDialog: could not get block id for component: {}", e.getMessage());
            return null;
        }
    }

    private static String resolveBlockName(String blockId, int maxLen) {
        String display = blockId;
        try {
            Block b = BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.tryParse(blockId));
            if (b != null) {
                String loc = Component.translatable(b.getDescriptionId()).getString();
                if (loc != null && !loc.isBlank()) display = loc;
            }
        } catch (IllegalArgumentException e) {
            GTCEUTerminalMod.LOGGER.debug("ComponentUpgradeDialog: could not resolve block name for '{}': {}", blockId, e.getMessage());
        }
        if (display.length() > maxLen) display = display.substring(0, maxLen - 1) + "…";
        return display;
    }

    private static String trimSuffixes(String s) {
        if (s == null) return "";
        return s.replace(" Coil Block", "")
                .replace(" Heating Coil", "")
                .replace(" Maintenance Hatch", " Maintenance")
                .trim();
    }

    private static String safeTierName(int tier) {
        String[] vn = com.gregtechceu.gtceu.api.GTValues.VN;
        if (tier >= 0 && tier < vn.length) {
            String s = vn[tier];
            if (s != null && !s.isBlank()) return s;
        }
        return "T" + tier;
    }
}