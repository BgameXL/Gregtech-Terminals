package com.gtceuterminal.client.gui.dialog;

import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.common.multiblock.ComponentGroup;
import com.gtceuterminal.common.multiblock.ComponentInfo;
import com.gtceuterminal.common.multiblock.ComponentType;
import com.gtceuterminal.common.multiblock.MultiblockInfo;

import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class ComponentDetailDialog extends DialogWidget {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int DIALOG_W  = 460;
    private static final int DIALOG_H  = 390;
    private static final int HEADER_H  = 28;
    private static final int INFO_H    = 32;
    private static final int ENTRY_H   = 40;
    private static final int ENTRY_GAP = 4;
    private static final int PAD       = 10;

    // ── Colors ────────────────────────────────────────────────────────────────
    private int COLOR_BG_DARK;
    private int COLOR_BG_MEDIUM;
    private int COLOR_BG_LIGHT;
    private int COLOR_BORDER_LIGHT;
    private static final int COLOR_BORDER_DARK = 0xFF0A0A0A;
    private static final int COLOR_TEXT_WHITE  = 0xFFFFFFFF;
    private static final int COLOR_TEXT_GRAY   = 0xFFAAAAAA;
    private static final int COLOR_HOVER       = 0x22FFFFFF;

    // ── State ─────────────────────────────────────────────────────────────────
    private ItemTheme theme;
    private final Player player;
    private final MultiblockInfo multiblock;
    private final Runnable onClose;
    private int W = DIALOG_W;
    private int H = DIALOG_H;

    // ── Constructors ──────────────────────────────────────────────────────────
    public ComponentDetailDialog(WidgetGroup parent, Player player, MultiblockInfo multiblock) {
        this(parent, player, multiblock, null, null);
    }

    public ComponentDetailDialog(WidgetGroup parent, Player player, MultiblockInfo multiblock, Runnable onClose) {
        this(parent, player, multiblock, onClose, null);
    }

    public ComponentDetailDialog(WidgetGroup parent, Player player, MultiblockInfo multiblock,
                                 Runnable onClose, ItemTheme passedTheme) {
        super(parent, true);
        this.player    = player;
        this.multiblock = multiblock;
        this.onClose   = onClose;
        this.theme     = passedTheme != null ? passedTheme : ItemTheme.loadFromPlayer(player);
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
        for (Widget widget : new ArrayList<>(parent.widgets)) {
            if (widget instanceof ComponentUpgradeDialog) {
                ((ComponentUpgradeDialog) widget).close();
            }
        }
        super.close();
        if (onClose != null) onClose.run();
    }

    // ── Layout init ───────────────────────────────────────────────────────────
    private void initDialog() {
        var mc = Minecraft.getInstance();
        int sw = mc.screen != null ? mc.screen.width  : mc.getWindow().getGuiScaledWidth();
        int sh = mc.screen != null ? mc.screen.height : mc.getWindow().getGuiScaledHeight();

        int margin   = 10;
        int maxW     = sw - margin * 2;
        int maxH     = sh - margin * 2;
        int contentW = DIALOG_W;
        int contentH = DIALOG_H;
        int viewW    = Math.min(contentW, maxW);
        int viewH    = Math.min(contentH, maxH);

        int screenX  = Mth.clamp((sw - viewW) / 2, margin, sw - viewW - margin);
        int screenY  = Mth.clamp((sh - viewH) / 2, margin, sh - viewH - margin);
        Position abs = parent.getPosition();
        int x = screenX - abs.x;
        int y = screenY - abs.y;

        this.W = viewW;
        this.H = viewH;
        setSize(new Size(viewW, viewH));
        setSelfPosition(new Position(x, y));
        setBackground(theme.backgroundTexture());

        // Content — may be larger than viewport if screen is tiny
        WidgetGroup content = buildContent(contentW, contentH);

        if (contentW <= maxW && contentH <= maxH) {
            // Fits — add directly
            for (Widget w : new ArrayList<>(content.widgets)) {
                addWidget(w);
            }
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
        root.addWidget(buildInfoBar(cW));
        root.addWidget(buildGroupList(cW, cH));

        return root;
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private WidgetGroup buildHeader(int cW) {
        WidgetGroup header = new WidgetGroup(2, 2, cW - 4, HEADER_H);
        header.setBackground(theme.headerTexture());

        // Multiblock name (resolved from translation key)
        String name = resolveDisplayName();
        LabelWidget title = new LabelWidget(10, 9, "§f" + name);
        title.setTextColor(COLOR_TEXT_WHITE);
        header.addWidget(title);

        
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

    // ── Info bar ──────────────────────────────────────────────────────────────
    private WidgetGroup buildInfoBar(int cW) {
        int y = 2 + HEADER_H + 3;
        WidgetGroup bar = new WidgetGroup(PAD, y, cW - PAD * 2, INFO_H);
        bar.setBackground(theme.panelTexture());

        List<ComponentGroup> groups = multiblock.getGroupedComponents();
        int totalComponents = multiblock.getComponents().size();

        String text = Component.translatable(
                "gui.gtceuterminal.component_detail_dialog.info.summary",
                multiblock.getTierName(),
                multiblock.getDistanceString(),
                totalComponents,
                groups.size()
        ).getString();

        LabelWidget lbl = new LabelWidget(10, 10, text);
        lbl.setTextColor(COLOR_TEXT_GRAY);
        bar.addWidget(lbl);

        return bar;
    }

    // ── Component groups list ─────────────────────────────────────────────────
    private WidgetGroup buildGroupList(int cW, int cH) {
        int listY = 2 + HEADER_H + 3 + INFO_H + 4;
        int listH = cH - listY - 8;

        WidgetGroup panel = new WidgetGroup(PAD, listY, cW - PAD * 2, listH);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(COLOR_BG_DARK),
                new ColorBorderTexture(1, COLOR_BORDER_DARK)));

        // "Components" section label
        LabelWidget sectionLabel = new LabelWidget(10, 5,
                Component.translatable("gui.gtceuterminal.component_detail_dialog.list.header").getString());
        sectionLabel.setTextColor(COLOR_TEXT_WHITE);
        panel.addWidget(sectionLabel);

        int scrollW = (cW - PAD * 2) - 14;
        int scrollH = listH - 22;
        DraggableScrollableWidgetGroup scroll =
                new DraggableScrollableWidgetGroup(4, 20, scrollW, scrollH);
        scroll.setYScrollBarWidth(8);
        scroll.setYBarStyle(
                new ColorRectTexture(COLOR_BORDER_DARK),
                new ColorRectTexture(COLOR_BORDER_LIGHT));

        List<ComponentGroup> groups = multiblock.getGroupedComponents();
        int yPos = 0;
        for (ComponentGroup group : groups) {
            scroll.addWidget(buildEntry(group, yPos, scrollW - 10));
            yPos += ENTRY_H + ENTRY_GAP;
        }

        panel.addWidget(scroll);
        return panel;
    }

    // ── Single component group entry ──────────────────────────────────────────
    private WidgetGroup buildEntry(ComponentGroup group, int yPos, int entryW) {
        WidgetGroup entry = new WidgetGroup(0, yPos, entryW, ENTRY_H);
        entry.setBackground(theme.panelTexture());

        ComponentInfo rep = group.getRepresentative();
        if (rep == null) return entry;

        boolean upgradeable = !rep.getPossibleUpgradeTiers().isEmpty();

        if (upgradeable) {
            ButtonWidget clickArea = new ButtonWidget(0, 0, entryW, ENTRY_H,
                    new ColorRectTexture(0x00000000),
                    cd -> openUpgradeDialog(group));
            clickArea.setHoverTexture(new ColorRectTexture(COLOR_HOVER));
            entry.addWidget(clickArea);
        }

        int dotColor = 0xFF000000 | group.getType().getColor();
        entry.addWidget(new ImageWidget(8, 16, 8, 8, new ColorRectTexture(dotColor)));

        String typeName = group.getType().getDisplayNameComponent().getString();
        LabelWidget typeLabel = new LabelWidget(22, 6, "§f" + typeName);
        typeLabel.setTextColor(COLOR_TEXT_WHITE);
        entry.addWidget(typeLabel);

        String blockSub = formatBlockSubtype(group.getBlockName());
        LabelWidget subLabel = new LabelWidget(22, 20, "§7" + blockSub);
        subLabel.setTextColor(COLOR_TEXT_GRAY);
        entry.addWidget(subLabel);

        String countText = "×" + group.getCount();
        int badgeW = 72;
        int countX = entryW - 80;
        LabelWidget countLabel = new LabelWidget(countX, 6, "§e" + countText);
        countLabel.setTextColor(COLOR_TEXT_WHITE);
        entry.addWidget(countLabel);

        String tierText = Component.translatable(
                "gui.gtceuterminal.component_detail_dialog.entry.tier",
                tierNameForList(group, rep)).getString();
        LabelWidget tierLabel = new LabelWidget(countX, 20, tierText);
        tierLabel.setTextColor(COLOR_TEXT_GRAY);
        entry.addWidget(tierLabel);

        return entry;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String resolveDisplayName() {
        String raw = multiblock.getName();
        if (raw == null || raw.isEmpty())
            return Component.translatable("gui.gtceuterminal.component_detail_dialog.unknown_multiblock").getString();
        String resolved = Component.translatable(raw).getString();
        if (resolved.equals(raw) && raw.contains(".")) {
            String last = raw.substring(raw.lastIndexOf('.') + 1);
            resolved = Character.toUpperCase(last.charAt(0)) + last.substring(1).replace('_', ' ');
        }
        return resolved;
    }

    private static String formatBlockSubtype(String blockName) {
        if (blockName == null || blockName.isBlank()) return "";
        String s = blockName;

        // Strip common prefixes to get at the tier/amperage suffix
        for (String prefix : new String[]{"energy_hatch_", "input_hatch_", "output_hatch_",
                "input_bus_", "output_bus_", "dynamo_hatch_"}) {
            if (s.startsWith(prefix)) { s = s.substring(prefix.length()); break; }
        }

        // Uppercase tier names and amperage
        String[] parts = s.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            if (p.matches("(?i)ulv|lv|mv|hv|ev|iv|luv|zpm|uv|uhv|uev|uiv|uxv|opv|max")) {
                sb.append(p.toUpperCase(java.util.Locale.ROOT));
            } else if (p.matches("\\d+a")) {
                sb.append(p.toUpperCase(java.util.Locale.ROOT));
            } else {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        String result = sb.toString();
        return result.length() > 24 ? result.substring(0, 23) + "…" : result;
    }

    private static String tierNameForList(ComponentGroup group, ComponentInfo rep) {
        try {
            if (group.getType() == ComponentType.COIL) {
                return ComponentType.getCoilTierName(rep.getTier());
            }
        } catch (Throwable ignored) {}
        String s = rep.getTierName();
        return s != null ? s : "";
    }

    private void openUpgradeDialog(ComponentGroup group) {
        if (this.gui == null || this.gui.mainGroup == null) return;
        new ComponentUpgradeDialog(
                this.gui.mainGroup,
                null,
                this,
                group,
                multiblock,
                player,
                this.theme);
    }
}