package com.gtceuterminal.client.gui.dialog;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.multiblock.ComponentDetailUI;
import com.gtceuterminal.common.ae2.WirelessTerminalHandler;
import com.gtceuterminal.common.ae2.CuriosCompat;
import com.gtceuterminal.common.material.ComponentUpgradeHelper;
import com.gtceuterminal.common.material.MaterialAvailability;
import com.gtceuterminal.common.material.MaterialCalculator;
import com.gtceuterminal.common.multiblock.ComponentInfoGroup;
import com.gtceuterminal.common.multiblock.ComponentInfo;
import com.gtceuterminal.common.multiblock.ComponentGroupRegistry;
import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.network.CPacketComponentUpgrade;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.common.upgrade.UniversalUpgradeCatalog;
import com.gtceuterminal.common.upgrade.UniversalUpgradeCatalogBuilder;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import com.gtceuterminal.common.util.MiscUtil;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentUpgradeDialog extends DialogWidget {

    private static final int DIALOG_W  = 600;
    private static final int DIALOG_H  = 480;
    private static final int HEADER_H  = 28;
    private static final int INFO_H    = 38;
    private static final int PAD       = 10;

    private static final int INFO_Y        = 2 + HEADER_H + 3;
    private static final int OPTIONS_Y     = INFO_Y + INFO_H + 4;
    private static final int OPTIONS_H_TWO = 130;
    private static final int BUTTONS_Y     = DIALOG_H - 36;

    private static final int C_BORDER_DARK = 0xFF0A0A0A;
    private static final int C_TEXT_WHITE  = 0xFFFFFFFF;
    private static final int C_TEXT_GRAY   = 0xFFAAAAAA;
    private static final int C_SUCCESS     = 0xFF00FF00;

    private final ItemTheme theme;
    private final int colorBgDark, colorBgMedium, colorBgLight, colorBorderLight;

    private final ComponentInfoGroup group;
    private final MultiblockInfo   multiblock;
    private final Player           player;
    private final DialogWidget     parentDialog;
    @SuppressWarnings("unused")
    private final ComponentDetailUI parentUI;

    private UniversalUpgradeCatalog universalCatalog;

    private Integer tierFilter      = null;
    private int     selectedTier    = -1;
    private String  selectedUpgradeId = null;
    private List<MaterialAvailability> materials;

    private WidgetGroup content;
    private WidgetGroup tierSelectionPanel;
    private WidgetGroup materialsPanel;
    private DraggableScrollableWidgetGroup optionsScroll;
    private WidgetGroup tierButtonsRow;
    private ComponentInfo currentRep;

    private static final int MAT_Y;
    private static final int MAT_H;
    static {
        int optionsH = OPTIONS_H_TWO;
        int matY = OPTIONS_Y + optionsH + 4;
        MAT_Y = matY;
        MAT_H = BUTTONS_Y - matY - 4;
    }

    public ComponentUpgradeDialog(WidgetGroup parent, ComponentDetailUI parentUI,
                                  DialogWidget parentDialog, ComponentInfoGroup group,
                                  MultiblockInfo multiblock, Player player) {
        this(parent, parentUI, parentDialog, group, multiblock, player, null);
    }

    public ComponentUpgradeDialog(WidgetGroup parent, ComponentDetailUI parentUI,
                                  DialogWidget parentDialog, ComponentInfoGroup group,
                                  MultiblockInfo multiblock, Player player, ItemTheme passedTheme) {
        super(parent, true);
        this.parentUI     = parentUI;
        this.parentDialog = parentDialog;
        this.group        = group;
        this.multiblock   = multiblock;
        this.player       = player;
        this.theme        = passedTheme != null ? passedTheme : ItemTheme.loadFromPlayer(player);

        colorBgDark     = theme.bgColor;
        colorBgMedium   = theme.panelColor;
        colorBgLight    = theme.isNativeStyle() ? 0xFF3A3A3A : theme.accent(0xAA);
        colorBorderLight = theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF);

        initDialog();
    }

    @Override
    public void close() {
        super.close();
        if (parentDialog != null) parentDialog.setActive(true);
    }

    private void initDialog() {
        var controller = multiblock.getController();
        if (controller instanceof MultiblockControllerMachine mmc) {
            universalCatalog = UniversalUpgradeCatalogBuilder.build(mmc, player.level());
        }

        var mc   = Minecraft.getInstance();
        int sw   = mc.screen != null ? mc.screen.width  : mc.getWindow().getGuiScaledWidth();
        int sh   = mc.screen != null ? mc.screen.height : mc.getWindow().getGuiScaledHeight();
        int margin = 10;
        int maxW = sw - margin * 2, maxH = sh - margin * 2;
        int viewW = Math.min(DIALOG_W, maxW), viewH = Math.min(DIALOG_H, maxH);

        Position abs = parent.getPosition();
        setSelfPosition(new Position(
                Mth.clamp((sw - viewW) / 2, margin, sw - viewW - margin) - abs.x,
                Mth.clamp((sh - viewH) / 2, margin, sh - viewH - margin) - abs.y));
        setSize(new Size(viewW, viewH));
        setBackground(theme.backgroundTexture());

        this.content = buildContent(DIALOG_W, DIALOG_H);

        if (DIALOG_W <= maxW && DIALOG_H <= maxH) {
            addWidget(content);
        } else {
            DraggableScrollableWidgetGroup viewport =
                    new DraggableScrollableWidgetGroup(0, 0, viewW, viewH);
            viewport.setYScrollBarWidth(8);
            viewport.setYBarStyle(
                    new ColorRectTexture(C_BORDER_DARK),
                    new ColorRectTexture(colorBorderLight));
            viewport.addWidget(content);
            addWidget(viewport);
        }
    }

    private WidgetGroup buildContent(int cW, int cH) {
        WidgetGroup root = new WidgetGroup(0, 0, cW, cH);
        root.setBackground(theme.backgroundTexture());

        if (!theme.isNativeStyle()) {
            root.addWidget(new ImageWidget(0,      0,      cW, 2,  new ColorRectTexture(colorBorderLight)));
            root.addWidget(new ImageWidget(0,      0,      2,  cH, new ColorRectTexture(colorBorderLight)));
            root.addWidget(new ImageWidget(cW - 2, 0,      2,  cH, new ColorRectTexture(C_BORDER_DARK)));
            root.addWidget(new ImageWidget(0,      cH - 2, cW, 2,  new ColorRectTexture(C_BORDER_DARK)));
        }

        root.addWidget(buildHeader(cW));
        root.addWidget(buildInfoPanel(cW));

        this.tierSelectionPanel = buildOptionsPanel(cW);
        root.addWidget(tierSelectionPanel);

        this.materialsPanel = UpgradeMaterialsPanel.build(
                PAD, MAT_Y, cW - PAD * 2, MAT_H,
                colorBgDark, C_BORDER_DARK,
                selectedTier != -1, player.isCreative(), materials);
        root.addWidget(materialsPanel);

        root.addWidget(buildButtons(cW));
        return root;
    }

    private WidgetGroup buildHeader(int cW) {
        WidgetGroup header = new WidgetGroup(2, 2, cW - 4, HEADER_H);
        header.setBackground(theme.headerTexture());

        String title = Component.translatable(
                "gui.gtceuterminal.component_upgrade_dialog.title",
                net.minecraft.network.chat.Component.literal(group.getGroup().displayName)).getString();
        LabelWidget titleLabel = new LabelWidget(10, 9, "§f" + title);
        titleLabel.setTextColor(C_TEXT_WHITE);
        header.addWidget(titleLabel);

        ButtonWidget closeBtn = new ButtonWidget(cW - 30, 4, 22, 20,
                new GuiTextureGroup(new ColorRectTexture(colorBgMedium), new ColorBorderTexture(1, colorBorderLight)),
                cd -> close());
        closeBtn.setButtonTexture(new TextTexture("§c✕").setWidth(22).setType(TextTexture.TextType.NORMAL));
        closeBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(0xFFAA0000), new ColorBorderTexture(1, C_TEXT_WHITE)));
        header.addWidget(closeBtn);
        return header;
    }

    private WidgetGroup buildInfoPanel(int cW) {
        WidgetGroup panel = new WidgetGroup(PAD, INFO_Y, cW - PAD * 2, INFO_H);
        panel.setBackground(theme.panelTexture());

        ComponentInfo rep = group.getRepresentative();
        if (rep == null) return panel;

        String tierName = rep.getTierName();

        LabelWidget l1 = new LabelWidget(10, 6, "§f" + Component.translatable(
                "gui.gtceuterminal.component_upgrade_dialog.info.count_components", group.getCount()).getString());
        LabelWidget l2 = new LabelWidget(10, 19, "§7" + Component.translatable(
                "gui.gtceuterminal.component_upgrade_dialog.info.current_tier", tierName).getString());
        l1.setTextColor(C_TEXT_WHITE);
        l2.setTextColor(C_TEXT_GRAY);
        panel.addWidget(l1);
        panel.addWidget(l2);
        return panel;
    }

    private WidgetGroup buildOptionsPanel(int cW) {
        ComponentInfo rep = group.getRepresentative();
        boolean special = rep != null && (rep.getGroup() == ComponentGroupRegistry.MAINTENANCE
                || rep.getGroup() == ComponentGroupRegistry.COIL);
        int panelH = special ? 80 : OPTIONS_H_TWO;

        WidgetGroup panel = new WidgetGroup(PAD, OPTIONS_Y, cW - PAD * 2, panelH);
        panel.setBackground(theme.panelTexture());
        if (rep == null) return panel;
        this.currentRep = rep;

        int scrollW = cW - PAD * 2 - 20;

        if (special) {
            LabelWidget label = new LabelWidget(10, 5,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.tier_selection.label_upgrade_option").getString());
            label.setTextColor(C_TEXT_WHITE);
            panel.addWidget(label);

            DraggableScrollableWidgetGroup scroll =
                    new DraggableScrollableWidgetGroup(10, 20, scrollW, panelH - 28);
            scroll.setYScrollBarWidth(6);
            scroll.setYBarStyle(new ColorRectTexture(C_BORDER_DARK), new ColorRectTexture(0xFF888888));
            panel.addWidget(scroll);
            this.optionsScroll = scroll;
            populateOptionsScroll(rep);
        } else {
            LabelWidget tierLabel = new LabelWidget(10, 5,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.tier_selection.label_target_tier").getString());
            tierLabel.setTextColor(C_TEXT_WHITE);
            panel.addWidget(tierLabel);

            WidgetGroup tierRow = new WidgetGroup(10, 18, scrollW, 22);
            panel.addWidget(tierRow);
            this.tierButtonsRow = tierRow;
            populateTierButtons(rep, tierRow);

            int divY = 18 + 22 + 3;
            panel.addWidget(new ImageWidget(10, divY, scrollW, 1, new ColorRectTexture(colorBorderLight)));

            int varY = divY + 4;
            int varH = panelH - varY - 4;
            DraggableScrollableWidgetGroup scroll =
                    new DraggableScrollableWidgetGroup(10, varY, scrollW, varH);
            scroll.setYScrollBarWidth(6);
            scroll.setYBarStyle(new ColorRectTexture(C_BORDER_DARK), new ColorRectTexture(0xFF888888));
            panel.addWidget(scroll);
            this.optionsScroll = scroll;

            if (tierFilter != null) populateOptionsScroll(rep);
            else scroll.addWidget(new LabelWidget(4, 4, "§7Select a tier above"));
        }

        return panel;
    }

    private void populateTierButtons(ComponentInfo rep, WidgetGroup tierRow) {
        tierRow.clearAllWidgets();
        List<Integer> tiers = getAvailableTiers(rep);
        int spacing = 2;
        int scrollW = DIALOG_W - PAD * 2 - 20;
        int btnW = tiers.isEmpty() ? 30 : Math.min(40, (scrollW - spacing * (tiers.size() - 1)) / tiers.size());
        int xPos = 0;
        for (int tier : tiers) {
            boolean sel = (tierFilter != null && tierFilter == tier);
            int bg = sel ? 0x6600FF00 : colorBgLight, border = sel ? 0xFF00FF00 : colorBorderLight;
            TextTexture txt = new TextTexture("§f" + UpgradeCandidateGrid.tierName(tier))
                    .setWidth(btnW).setType(TextTexture.TextType.NORMAL);
            final int t = tier;
            ButtonWidget btn = new ButtonWidget(xPos, 0, btnW, 22,
                    new GuiTextureGroup(new ColorRectTexture(bg), new ColorBorderTexture(1, border), txt),
                    cd -> onTierClicked(t));
            btn.setHoverTexture(new GuiTextureGroup(
                    new ColorRectTexture(bg), new ColorBorderTexture(1, C_TEXT_WHITE), txt));
            tierRow.addWidget(btn);
            xPos += btnW + spacing;
        }
    }

    private List<Integer> getAvailableTiers(ComponentInfo rep) {
        java.util.TreeSet<Integer> tiers = new java.util.TreeSet<>();
        if (universalCatalog != null) {
            var opt = universalCatalog.get(rep.getPosition());
            if (opt != null && opt.candidates() != null) {
                String currentId = UpgradeCandidateGrid.blockId(rep);
                for (var c : opt.candidates()) {
                    if (c == null || c.blockId() == null) continue;
                    if (currentId != null && currentId.equalsIgnoreCase(c.blockId())) continue;
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

    private void populateOptionsScroll(ComponentInfo rep) {
        if (optionsScroll == null) return;
        optionsScroll.clearAllWidgets();

        if (rep.getGroup() == ComponentGroupRegistry.MAINTENANCE) {
            UpgradeCandidateGrid.buildMaintenanceGrid(rep, optionsScroll, selectedUpgradeId,
                    colorBgLight, colorBorderLight, this::selectUpgrade);
            return;
        }
        if (rep.getGroup() == ComponentGroupRegistry.COIL) {
            UpgradeCandidateGrid.buildCoilGrid(rep, optionsScroll, selectedUpgradeId,
                    colorBgLight, colorBorderLight, this::selectUpgrade);
            return;
        }
        if (!UpgradeCandidateGrid.buildUniversalCandidates(rep, optionsScroll, tierFilter,
                selectedUpgradeId, universalCatalog, colorBgLight, colorBorderLight, this::selectUpgrade)) {
            buildFallbackTierGrid(rep);
        }
    }

    private void buildFallbackTierGrid(ComponentInfo rep) {
        List<Integer> tiers = ComponentUpgradeHelper.getAvailableTiers(rep.getGroup());
        int btnW = 48, btnH = 24, spacing = 3, perRow = 7, xPos = 0, yPos = 0, added = 0;

        optionsScroll.addWidget(UpgradeCandidateGrid.makeOptionBtn(
                Component.translatable("gui.gtceuterminal.component_upgrade_dialog.option_all_any").getString(),
                tierFilter == null, xPos, yPos, btnW, btnH, colorBgLight, colorBorderLight,
                this::onShowAll));
        xPos += btnW + spacing; added++;

        for (int tier : new java.util.LinkedHashSet<>(tiers)) {
            if (tier == rep.getTier()) continue;
            if (added % perRow == 0) { xPos = 0; yPos += btnH + spacing; }
            boolean sel = (tier == selectedTier);
            int bg = sel ? 0x6600FF00 : colorBgLight, border = sel ? 0xFF00FF00 : colorBorderLight;
            TextTexture txt = new TextTexture("§f" + UpgradeCandidateGrid.tierName(tier))
                    .setWidth(btnW).setType(TextTexture.TextType.NORMAL);
            final int t = tier;
            ButtonWidget btn = new ButtonWidget(xPos, yPos, btnW, btnH,
                    new GuiTextureGroup(new ColorRectTexture(bg), new ColorBorderTexture(1, border), txt),
                    cd -> onTierClicked(t));
            btn.setHoverTexture(new GuiTextureGroup(
                    new ColorRectTexture(bg), new ColorBorderTexture(1, C_TEXT_WHITE), txt));
            optionsScroll.addWidget(btn);
            xPos += btnW + spacing; added++;
        }
    }

    private void onTierClicked(int tier) {
        this.selectedTier      = tier;
        this.tierFilter        = tier;
        this.selectedUpgradeId = null;
        if (tierButtonsRow != null && currentRep != null) populateTierButtons(currentRep, tierButtonsRow);
        if (currentRep != null) populateOptionsScroll(currentRep);
        calculateMaterials();
        refreshMaterialsPanel();
    }

    private void onShowAll() {
        this.tierFilter        = null;
        this.selectedUpgradeId = null;
        if (tierButtonsRow != null && currentRep != null) populateTierButtons(currentRep, tierButtonsRow);
        if (optionsScroll != null) {
            optionsScroll.clearAllWidgets();
            optionsScroll.addWidget(new LabelWidget(4, 4, "§7Select a tier above"));
        }
        calculateMaterials();
        refreshMaterialsPanel();
    }

    private void selectUpgrade(String upgradeId, int tier) {
        this.selectedUpgradeId = upgradeId;
        this.selectedTier      = tier;
        GTCEUTerminalMod.LOGGER.info("Selected upgrade: {} (tier {})", upgradeId, tier);
        calculateMaterials();
        refreshMaterialsPanel();
    }

    private void calculateMaterials() {
        if (selectedTier == -1) return;
        ComponentInfo rep = group.getRepresentative();
        if (rep == null) return;

        Map<Item, Integer> singleRequired = (selectedUpgradeId != null && !selectedUpgradeId.isBlank())
                ? ComponentUpgradeHelper.getUpgradeItemsForBlockId(selectedUpgradeId)
                : ComponentUpgradeHelper.getUpgradeItems(rep, selectedTier);

        Map<Item, Integer> totalRequired = new HashMap<>();
        for (var e : singleRequired.entrySet()) totalRequired.put(e.getKey(), e.getValue() * group.getCount());

        materials = MaterialCalculator.checkMaterialsAvailability(totalRequired, player, player.level());
    }

    private void refreshMaterialsPanel() {
        if (materialsPanel == null || content == null) return;
        content.removeWidget(materialsPanel);

        this.materialsPanel = UpgradeMaterialsPanel.build(
                PAD, MAT_Y, DIALOG_W - PAD * 2, MAT_H,
                colorBgDark, C_BORDER_DARK,
                selectedTier != -1, player.isCreative(), materials);
        content.addWidget(materialsPanel);
    }

    private WidgetGroup buildButtons(int cW) {
        WidgetGroup buttons = new WidgetGroup(PAD, BUTTONS_Y, cW - PAD * 2, 28);

        int confirmW = 150;
        ButtonWidget confirmBtn = new ButtonWidget(0, 0, confirmW, 24,
                new GuiTextureGroup(new ColorRectTexture(0xFF2E7D32), new ColorBorderTexture(1, C_SUCCESS)),
                cd -> performUpgrade());
        confirmBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.component_upgrade_dialog.actions.confirm_change").getString())
                .setWidth(confirmW).setType(TextTexture.TextType.NORMAL));
        confirmBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(0xFF43A047), new ColorBorderTexture(1, C_TEXT_WHITE)));
        buttons.addWidget(confirmBtn);

        boolean showAutoCraft = WirelessTerminalHandler.isLinked(findWirelessTerminal());
        if (showAutoCraft) {
            int autoCraftW = 110;
            ButtonWidget autoCraftBtn = new ButtonWidget(confirmW + 8, 0, autoCraftW, 24,
                    new GuiTextureGroup(new ColorRectTexture(0xFF1A3A6B), new ColorBorderTexture(1, 0xFF2E75B6)),
                    cd -> performAutoCraft());
            autoCraftBtn.setButtonTexture(new TextTexture(
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.actions.auto_craft").getString())
                    .setWidth(autoCraftW).setType(TextTexture.TextType.NORMAL));
            autoCraftBtn.setHoverTexture(new GuiTextureGroup(
                    new ColorRectTexture(0xFF243A6B), new ColorBorderTexture(1, C_TEXT_WHITE)));
            buttons.addWidget(autoCraftBtn);
        }

        int cancelW = 120;
        ButtonWidget cancelBtn = new ButtonWidget(cW - PAD * 2 - cancelW, 0, cancelW, 24,
                new GuiTextureGroup(new ColorRectTexture(colorBgLight), new ColorBorderTexture(1, colorBorderLight)),
                cd -> close());
        cancelBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.component_upgrade_dialog.actions.cancel").getString())
                .setWidth(cancelW).setType(TextTexture.TextType.NORMAL));
        cancelBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(colorBgLight), new ColorBorderTexture(1, C_TEXT_WHITE)));
        buttons.addWidget(cancelBtn);

        return buttons;
    }

    private void performUpgrade() {
        if (selectedTier == -1) {
            player.displayClientMessage(
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.chat.select_upgrade_option_first"), true);
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
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.chat.select_upgrade_option_first"), true);
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

    private ItemStack findWirelessTerminal() {
        if (player == null) return ItemStack.EMPTY;
        for (ItemStack s : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (WirelessTerminalHandler.isWirelessTerminal(s)) return s;
        }
        for (ItemStack s : player.getInventory().items) {
            if (WirelessTerminalHandler.isWirelessTerminal(s)) return s;
        }
        if (MiscUtil.isCuriosLoaded) {
            for (ItemStack s : CuriosCompat.getEquippedItems(player)) {
                if (WirelessTerminalHandler.isWirelessTerminal(s)) return s;
            }
        }
        return ItemStack.EMPTY;
    }
}