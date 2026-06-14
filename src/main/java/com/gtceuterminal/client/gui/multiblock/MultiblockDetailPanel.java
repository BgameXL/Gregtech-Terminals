package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.common.multiblock.ComponentInfo;
import com.gtceuterminal.common.multiblock.ComponentInfoGroup;
import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.multiblock.MultiblockStatus;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.gtceuterminal.client.gui.widget.TerminalButton;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.BiConsumer;

@OnlyIn(Dist.CLIENT)
final class MultiblockDetailPanel {

    private static final int DETAIL_HEADER_H = 38;
    private static final int FOOTER_H        = 22;
    private static final int ENTRY_H         = 36;
    private static final int ENTRY_GAP       = 4;

    private MultiblockDetailPanel() {}

    static WidgetGroup buildEmpty(
            int x, int y, int w, int h,
            int colorBgDark, int colorBgMedium, int colorBgLight,
            int colorBorderDark, int colorBorderLight,
            int colorTextGray
    ) {
        WidgetGroup panel = new WidgetGroup(x, y, w, h);
        panel.setBackground(new ColorRectTexture(colorBgDark));

        LabelWidget hint = new LabelWidget(w / 2 - 60, h / 2 - 6,
                "§8Select a multiblock");
        hint.setTextColor(colorTextGray);
        panel.addWidget(hint);
        return panel;
    }

    static WidgetGroup build(
            int x, int y, int w, int h,
            MultiblockInfo mb,
            Player player,
            ItemTheme theme,
            int colorBgDark, int colorBgMedium, int colorBgLight,
            int colorBorderDark, int colorBorderLight,
            int colorTextGray, int colorHover,
            BiConsumer<ComponentInfoGroup, MultiblockInfo> onUpgrade
    ) {
        WidgetGroup panel = new WidgetGroup(x, y, w, h);
        panel.setBackground(new ColorRectTexture(colorBgDark));

        panel.addWidget(buildDetailHeader(w, mb, colorBgMedium, colorTextGray));

        int listY = DETAIL_HEADER_H + 1;
        int listH = h - listY - FOOTER_H - 1;
        BiConsumer<ComponentInfoGroup, MultiblockInfo> upgradeHandler =
                mb.getStatus() == MultiblockStatus.UNFORMED ? null : onUpgrade;
        panel.addWidget(buildGroupList(0, listY, w, listH, mb,
                colorBgDark, colorBgMedium, colorBgLight, colorBorderDark, colorBorderLight,
                colorTextGray, colorHover, upgradeHandler));

        panel.addWidget(buildDetailFooter(w, h, colorBorderDark, colorTextGray));

        return panel;
    }

    private static WidgetGroup buildDetailHeader(int w, MultiblockInfo mb,
                                                 int colorBgMedium, int colorTextGray) {
        WidgetGroup header = new WidgetGroup(0, 0, w, DETAIL_HEADER_H);
        header.setBackground(new ColorRectTexture(colorBgMedium));

        String displayName = resolveDisplayName(mb);
        header.addWidget(new LabelWidget(10, 8, "§f" + truncate(displayName, 32)));

        MultiblockStatus status = mb.getStatus();
        String statusStr = "§" + statusColorCode(status) + status.getDisplayName().toLowerCase();
        String meta = mb.getTierName()
                + " · " + statusStr
                + " §7· " + mb.getComponents().size() + " components"
                + " · " + mb.getDistanceString() + " away";
        LabelWidget metaLabel = new LabelWidget(10, 22, meta);
        metaLabel.setTextColor(colorTextGray);
        header.addWidget(metaLabel);

        return header;
    }

    private static WidgetGroup buildGroupList(
            int x, int y, int w, int h,
            MultiblockInfo mb,
            int colorBgDark, int colorBgMedium, int colorBgLight,
            int colorBorderDark, int colorBorderLight,
            int colorTextGray, int colorHover,
            BiConsumer<ComponentInfoGroup, MultiblockInfo> onUpgrade
    ) {
        WidgetGroup listPanel = new WidgetGroup(x, y, w, h);
        listPanel.setBackground(new ColorRectTexture(colorBgDark));

        int scrollW = w - 10;
        DraggableScrollableWidgetGroup scroll =
                new DraggableScrollableWidgetGroup(0, 0, scrollW, h);
        scroll.setYScrollBarWidth(8);
        scroll.setYBarStyle(
                new ColorRectTexture(colorBorderDark),
                new ColorRectTexture(colorBorderLight));

        List<ComponentInfoGroup> groups = mb.getGroupedComponents();
        int yPos = 2;
        for (ComponentInfoGroup group : groups) {
            scroll.addWidget(buildGroupEntry(group, mb, yPos, scrollW - 4,
                    colorBgMedium, colorBgLight, colorBorderDark, colorBorderLight,
                    colorTextGray, colorHover, onUpgrade));
            yPos += ENTRY_H + ENTRY_GAP;
        }

        if (groups.isEmpty()) {
            LabelWidget empty = new LabelWidget(10, 10,
                    Component.translatable("gui.gtceuterminal.component_detail.no_upgradeable").getString());
            empty.setTextColor(colorTextGray);
            scroll.addWidget(empty);
        }

        listPanel.addWidget(scroll);
        return listPanel;
    }

    private static WidgetGroup buildGroupEntry(
            ComponentInfoGroup group,
            MultiblockInfo mb,
            int yPos, int entryW,
            int colorBgMedium, int colorBgLight,
            int colorBorderDark, int colorBorderLight,
            int colorTextGray, int colorHover,
            BiConsumer<ComponentInfoGroup, MultiblockInfo> onUpgrade
    ) {
        WidgetGroup entry = new WidgetGroup(0, yPos, entryW, ENTRY_H);

        ComponentInfo rep = group.getRepresentative();
        boolean upgradeable = rep != null
                && (rep.getGroup().isUpgradeable || !rep.getPossibleUpgradeTiers().isEmpty());

        entry.setBackground(new GuiTextureGroup(
                new ColorRectTexture(upgradeable ? colorBgMedium : 0xFF1C1C1C),
                new ColorBorderTexture(1, upgradeable ? colorBorderLight : 0xFF272727)));

        if (upgradeable && onUpgrade != null) {
            ButtonWidget clickArea = TerminalButton.entry(0, 0, entryW, ENTRY_H, colorHover, cd -> onUpgrade.accept(group, mb));
            entry.addWidget(clickArea);
        }

        // Color dot
        int dotColor = 0xFF000000 | group.getGroup().color;
        int dotY = (ENTRY_H - 8) / 2;
        entry.addWidget(new ImageWidget(8, dotY, 8, 8, new ColorRectTexture(dotColor)));

        // Group name
        String groupName = group.getGroup().displayName;
        LabelWidget nameLabel = new LabelWidget(22, 8,
                (upgradeable ? "§f" : "§8") + groupName);
        nameLabel.setTextColor(upgradeable ? 0xFFFFFFFF : 0xFF888888);
        entry.addWidget(nameLabel);

        // Sub: tier · count
        if (rep != null) {
            String blockSub = stripLeadingTier(formatBlockSubtype(group.getBlockName()), rep.getTierName());
            String sub = (blockSub.isEmpty() ? rep.getTierName() : rep.getTierName() + " " + blockSub)
                    + " · ×" + group.getCount();
            LabelWidget subLabel = new LabelWidget(22, 21, "§7" + sub);
            subLabel.setTextColor(colorTextGray);
            entry.addWidget(subLabel);
        }

        // Badge
        int badgeW = 62;
        int badgeX = entryW - badgeW - 6;
        int badgeY = (ENTRY_H - 14) / 2;
        if (upgradeable) {
            WidgetGroup badge = new WidgetGroup(badgeX, badgeY, badgeW, 14);
            badge.setBackground(new GuiTextureGroup(
                    new ColorRectTexture(0xFF1A3A1A),
                    new ColorBorderTexture(1, 0xFF2D6B2D)));
            badge.addWidget(new LabelWidget(4, 2,
                    Component.translatable("gui.gtceuterminal.component_detail.upgrade_badge").getString()));
            entry.addWidget(badge);
        } else {
            LabelWidget fixed = new LabelWidget(badgeX + 4, badgeY + 2,
                    Component.translatable("gui.gtceuterminal.component_detail.fixed_badge").getString());
            fixed.setTextColor(0xFF444444);
            entry.addWidget(fixed);
        }

        return entry;
    }

    private static WidgetGroup buildDetailFooter(int w, int panelH,
                                                 int colorBorderDark, int colorTextGray) {
        int footerY = panelH - FOOTER_H;
        WidgetGroup footer = new WidgetGroup(0, footerY, w, FOOTER_H);
        footer.addWidget(new ImageWidget(0, 0, w, 1, new ColorRectTexture(colorBorderDark)));
        LabelWidget hint = new LabelWidget(8, 6,
                Component.translatable("gui.gtceuterminal.component_detail.upgrade_hint").getString());
        hint.setTextColor(colorTextGray);
        footer.addWidget(hint);
        return footer;
    }

    private static String resolveDisplayName(MultiblockInfo mb) {
        String raw = mb.getName();
        if (raw == null || raw.isEmpty()) return "Unknown";
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
        for (String prefix : new String[]{
                "energy_hatch_", "input_hatch_", "output_hatch_",
                "input_bus_", "output_bus_", "dynamo_hatch_"}) {
            if (s.startsWith(prefix)) { s = s.substring(prefix.length()); break; }
        }
        String[] parts = s.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            if (p.matches("(?i)ulv|lv|mv|hv|ev|iv|luv|zpm|uv|uhv|uev|uiv|uxv|opv|max")
                    || p.matches("\\d+a")) {
                sb.append(p.toUpperCase(java.util.Locale.ROOT));
            } else {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        String result = sb.toString();
        return result.length() > 28 ? result.substring(0, 27) + "…" : result;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private static char statusColorCode(MultiblockStatus status) {
        return switch (status) {
            case ACTIVE            -> 'a';
            case IDLE              -> 'e';
            case NEEDS_MAINTENANCE -> '6';
            case NO_POWER          -> 'c';
            case OUTPUT_FULL       -> 'b';
            case DISABLED, UNFORMED -> '8';
        };
    }

    private static String stripLeadingTier(String blockSub, String tierName) {
        if (blockSub == null || tierName == null) return blockSub != null ? blockSub : "";
        String upper = blockSub.toUpperCase(java.util.Locale.ROOT);
        String tier  = tierName.toUpperCase(java.util.Locale.ROOT);
        if (upper.startsWith(tier + " ")) return blockSub.substring(tier.length() + 1);
        if (upper.equals(tier))           return "";
        return blockSub;
    }
}
