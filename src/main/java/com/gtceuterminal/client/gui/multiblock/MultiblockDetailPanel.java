package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.client.gui.style.TerminalText;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.multiblock.ComponentInfo;
import com.gtceuterminal.common.multiblock.ComponentInfoGroup;
import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.multiblock.MultiblockStatus;
import com.gtceuterminal.common.theme.ItemTheme;
import com.lowdragmc.lowdraglib.gui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.BiConsumer;

@OnlyIn(Dist.CLIENT)
final class MultiblockDetailPanel {

    private static final int DETAIL_HEADER_H = 38;
    private static final int ENTRY_H = 36;
    private static final int ENTRY_GAP = 4;

    private MultiblockDetailPanel() {
    }

    static WidgetGroup buildEmpty(int x, int y, int w, int h) {
        TerminalTheme t = TerminalUI.tokens();
        WidgetGroup panel = new WidgetGroup(x, y, w, h);
        panel.setBackground(TerminalUI.surface());

        LabelWidget hint = new LabelWidget(w / 2 - 60, h / 2 - 6,
                "Select a multiblock");
        hint.setColor(t.textMuted());
        panel.addWidget(hint);
        return panel;
    }

    static WidgetGroup build(
            int x, int y, int w, int h,
            MultiblockInfo mb,
            Player player,
            ItemTheme theme,
            BiConsumer<ComponentInfoGroup, MultiblockInfo> onUpgrade
    ) {
        TerminalTheme t = TerminalUI.tokens();
        WidgetGroup panel = new WidgetGroup(x, y, w, h);
        panel.setBackground(TerminalUI.surface());

        panel.addWidget(buildDetailHeader(w, mb, t));

        int listY = DETAIL_HEADER_H + 1;
        int listH = h - listY;
        BiConsumer<ComponentInfoGroup, MultiblockInfo> upgradeHandler =
                mb.getStatus() == MultiblockStatus.UNFORMED ? null : onUpgrade;
        panel.addWidget(buildGroupList(0, listY, w, listH, mb, t, upgradeHandler));

        return panel;
    }

    private static WidgetGroup buildDetailHeader(int w, MultiblockInfo mb, TerminalTheme t) {
        WidgetGroup header = new WidgetGroup(0, 0, w, DETAIL_HEADER_H);
        header.setBackground(TerminalUI.panel());

        String displayName = resolveDisplayName(mb);
        LabelWidget name = new LabelWidget(10, 8, TerminalText.truncate(displayName, 32));
        name.setColor(t.textStrong());
        header.addWidget(name);

        MultiblockStatus status = mb.getStatus();
        String prefix = mb.getTierName() + " · ";
        LabelWidget tierLabel = new LabelWidget(10, 22, prefix);
        tierLabel.setColor(t.textMuted());
        header.addWidget(tierLabel);

        String statusText = status.getDisplayName().toLowerCase();
        int statusX = 10 + Minecraft.getInstance().font.width(prefix);
        LabelWidget statusLabel = new LabelWidget(statusX, 22, statusText);
        statusLabel.setColor(statusColor(status, t));
        header.addWidget(statusLabel);

        int metaX = statusX + Minecraft.getInstance().font.width(statusText);
        LabelWidget metaLabel = new LabelWidget(metaX, 22,
                " · " + mb.getComponents().size() + " components · " + mb.getDistanceString() + " away");
        metaLabel.setColor(t.textMuted());
        header.addWidget(metaLabel);

        return header;
    }

    private static WidgetGroup buildGroupList(
            int x, int y, int w, int h,
            MultiblockInfo mb,
            TerminalTheme t,
            BiConsumer<ComponentInfoGroup, MultiblockInfo> onUpgrade
    ) {
        WidgetGroup listPanel = new WidgetGroup(x, y, w, h);
        listPanel.setBackground(TerminalUI.surface());

        int scrollW = w;
        DraggableScrollableWidgetGroup scroll =
                new DraggableScrollableWidgetGroup(0, 0, scrollW, h);
        scroll.setYScrollBarWidth(0);

        List<ComponentInfoGroup> groups = mb.getGroupedComponents();
        int yPos = 2;
        for (ComponentInfoGroup group : groups) {
            scroll.addWidget(buildGroupEntry(group, mb, yPos, scrollW - 2, t, onUpgrade));
            yPos += ENTRY_H + ENTRY_GAP;
        }

        if (groups.isEmpty()) {
            LabelWidget empty = new LabelWidget(10, 10,
                    Component.translatable("gui.gtceuterminal.component_detail.no_upgradeable").getString());
            empty.setColor(t.textMuted());
            scroll.addWidget(empty);
        }

        listPanel.addWidget(scroll);
        return listPanel;
    }

    private static WidgetGroup buildGroupEntry(
            ComponentInfoGroup group,
            MultiblockInfo mb,
            int yPos, int entryW,
            TerminalTheme t,
            BiConsumer<ComponentInfoGroup, MultiblockInfo> onUpgrade
    ) {
        WidgetGroup entry = new WidgetGroup(1, yPos, entryW, ENTRY_H);

        ComponentInfo rep = group.getRepresentative();
        boolean upgradeable = rep != null
                && (rep.getGroup().isUpgradeable || !rep.getPossibleUpgradeTiers().isEmpty());

        entry.setBackground(TerminalUI.outlined(
                upgradeable ? t.panel() : t.panelDisabled(),
                upgradeable ? t.border() : t.borderSubtle()));

        if (upgradeable && onUpgrade != null) {
            ButtonWidget clickArea = TerminalButton.entry(0, 0, entryW, ENTRY_H,
                    t.hover(), cd -> onUpgrade.accept(group, mb));
            entry.addWidget(clickArea);
        }

        // Color dot
        int dotColor = 0xFF000000 | group.getGroup().color;
        int dotY = (ENTRY_H - 8) / 2;
        entry.addWidget(new ImageWidget(8, dotY, 8, 8, TerminalUI.color(dotColor)));

        // Group name
        String groupName = group.getGroup().displayName;
        LabelWidget nameLabel = new LabelWidget(22, 8, groupName);
        nameLabel.setColor(upgradeable ? t.textStrong() : t.textFaint());
        entry.addWidget(nameLabel);

        // Sub: tier · count
        if (rep != null) {
            String blockSub = stripLeadingTier(formatBlockSubtype(group.getBlockName()), rep.getTierName());
            String sub = (blockSub.isEmpty() ? rep.getTierName() : rep.getTierName() + " " + blockSub)
                    + " · ×" + group.getCount();
            LabelWidget subLabel = new LabelWidget(22, 21, sub);
            subLabel.setColor(t.textMuted());
            entry.addWidget(subLabel);
        }

        if (!upgradeable) {
            LabelWidget fixed = new LabelWidget(entryW - 58, (ENTRY_H - 10) / 2,
                    Component.translatable("gui.gtceuterminal.component_detail.fixed_badge").getString());
            fixed.setColor(t.textFaint());
            entry.addWidget(fixed);
        }

        return entry;
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
            if (s.startsWith(prefix)) {
                s = s.substring(prefix.length());
                break;
            }
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
        return TerminalText.truncate(result, 28);
    }

    private static int statusColor(MultiblockStatus status, TerminalTheme theme) {
        return switch (status) {
            case ACTIVE -> theme.success();
            case IDLE, NEEDS_MAINTENANCE -> theme.warning();
            case NO_POWER -> theme.error();
            case OUTPUT_FULL -> theme.border();
            case DISABLED, UNFORMED -> theme.textFaint();
        };
    }

    private static String stripLeadingTier(String blockSub, String tierName) {
        if (blockSub == null || tierName == null) return blockSub != null ? blockSub : "";
        String upper = blockSub.toUpperCase(java.util.Locale.ROOT);
        String tier = tierName.toUpperCase(java.util.Locale.ROOT);
        if (upper.startsWith(tier + " ")) return blockSub.substring(tier.length() + 1);
        if (upper.equals(tier)) return "";
        return blockSub;
    }
}
