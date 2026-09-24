package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.multiblock.MultiblockStatus;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
final class MultiblockListPanel {

    private static final int ENTRY_H = 46;
    private static final int ENTRY_STEP = 48;
    private static final int FOOTER_H = 28;

    private MultiblockListPanel() {
    }

    static WidgetGroup build(
            int panelX, int panelY, int panelW, int panelH,
            int selectedIndex,
            List<MultiblockInfo> multiblocks,
            Consumer<Integer> onEntryClick,
            Consumer<MultiblockInfo> onHighlight,
            Consumer<MultiblockInfo> onRename,
            Supplier<MultiblockInfo> getSelected,
            WidgetGroup[] footerRef,
            DraggableScrollableWidgetGroup[] scrollRef
    ) {
        TerminalTheme t = TerminalUI.tokens();
        int listH = panelH - FOOTER_H - 1;
        int scrollW = panelW;

        WidgetGroup panel = new WidgetGroup(panelX, panelY, panelW, listH);
        panel.setBackground(new ColorRectTexture(t.surface()));

        DraggableScrollableWidgetGroup scroll =
                new DraggableScrollableWidgetGroup(0, 0, scrollW, listH);
        scroll.setYScrollBarWidth(0);
        scrollRef[0] = scroll;

        populateScroll(scroll, multiblocks, selectedIndex, scrollW, t, onEntryClick);

        panel.addWidget(scroll);

        footerRef[0] = buildFooter(panelX, panelY + listH, panelW, t,
                getSelected, onHighlight, onRename);

        return panel;
    }

    static void repopulate(
            DraggableScrollableWidgetGroup scroll,
            List<MultiblockInfo> multiblocks,
            int selectedIndex,
            int scrollW,
            Consumer<Integer> onEntryClick
    ) {
        TerminalTheme t = TerminalUI.tokens();
        scroll.clearAllWidgets();
        populateScroll(scroll, multiblocks, selectedIndex, scrollW, t, onEntryClick);
    }

    private static void populateScroll(
            DraggableScrollableWidgetGroup scroll,
            List<MultiblockInfo> multiblocks,
            int selectedIndex,
            int entryW,
            TerminalTheme t,
            Consumer<Integer> onEntryClick
    ) {
        if (multiblocks.isEmpty()) {
            LabelWidget empty = new LabelWidget(10, 12,
                    Component.translatable("gui.gtceuterminal.multiblock_manager.none_found").getString());
            empty.setTextColor(t.textMuted());
            scroll.addWidget(empty);
            return;
        }
        int y = 2;
        for (int i = 0; i < multiblocks.size(); i++) {
            final int idx = i;
            scroll.addWidget(buildEntry(multiblocks.get(i), idx, y, entryW,
                    idx == selectedIndex, t, scroll, onEntryClick));
            y += ENTRY_STEP;
        }
    }

    private static WidgetGroup buildEntry(
            MultiblockInfo mb, int index, int yPos, int entryW,
            boolean selected,
            TerminalTheme t,
            DraggableScrollableWidgetGroup viewport,
            Consumer<Integer> onEntryClick
    ) {
        int rowW = entryW - 2;
        WidgetGroup entry = new WidgetGroup(1, yPos, rowW, ENTRY_H);

        int bg = selected ? t.accentSoft() : t.panel();
        int borderColor = selected ? t.border() : Palette.CLEAR;
        entry.setBackground(new GuiTextureGroup(
                new ColorRectTexture(bg),
                new ColorBorderTexture(1, borderColor)));

        // Clickable area (full entry minus buttons)
        ButtonWidget clickBtn = new ViewportClippedButtonWidget(
                0, 0, rowW, ENTRY_H, t.hover(), viewport, cd -> onEntryClick.accept(index));
        entry.addWidget(clickBtn);

        // Name line
        String displayName = resolveDisplayName(mb);
        LabelWidget nameLabel = new LabelWidget(8, 6, truncate(displayName, 24));
        nameLabel.setTextColor(mb.isVanillaGTCEu() ? t.textStrong() : opaque(mb.getModColor()));
        entry.addWidget(nameLabel);

        // Sub line: status · tier · distance, matching the compact mockup row.
        MultiblockStatus status = mb.getStatus();
        String statusText = status.getDisplayName().toLowerCase();
        LabelWidget statusLabel = new LabelWidget(8, 21, statusText);
        statusLabel.setTextColor(statusColor(status, t));
        entry.addWidget(statusLabel);

        int metaX = 8 + Minecraft.getInstance().font.width(statusText) + 4;
        LabelWidget metaLabel = new LabelWidget(metaX, 21,
                mb.getTierName() + " · " + mb.getDistanceString());
        metaLabel.setTextColor(t.textFaint());
        entry.addWidget(metaLabel);

        return entry;
    }

    private static WidgetGroup buildFooter(
            int x, int y, int panelW,
            TerminalTheme t,
            Supplier<MultiblockInfo> getSelected,
            Consumer<MultiblockInfo> onHighlight,
            Consumer<MultiblockInfo> onRename
    ) {
        WidgetGroup footer = new WidgetGroup(x, y, panelW, FOOTER_H);
        footer.addWidget(new ImageWidget(0, 0, panelW, 1, new ColorRectTexture(t.borderDark())));

        ButtonWidget highlightBtn = TerminalButton.text3d(6, 5, panelW - 40, 18, "Highlight",
                cd -> {
                    MultiblockInfo sel = getSelected.get();
                    if (sel != null) onHighlight.accept(sel);
                });
        highlightBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.multiblock_manager.highlight_tooltip").getString());
        footer.addWidget(highlightBtn);

        ButtonWidget renameBtn = TerminalButton.icon3d(panelW - 28, 4, 20, "✎",
                cd -> {
                    MultiblockInfo sel = getSelected.get();
                    if (sel != null) onRename.accept(sel);
                });
        renameBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.multiblock_manager.rename_tooltip").getString());
        footer.addWidget(renameBtn);

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

    private static int opaque(int color) {
        return 0xFF000000 | (color & 0x00FFFFFF);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
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

    private static final class ViewportClippedButtonWidget extends ButtonWidget {
        private final DraggableScrollableWidgetGroup viewport;

        private ViewportClippedButtonWidget(
                int x, int y, int w, int h,
                int hoverColor,
                DraggableScrollableWidgetGroup viewport,
                Consumer<com.lowdragmc.lowdraglib.gui.util.ClickData> action
        ) {
            super(x, y, w, h, new ColorRectTexture(Palette.CLEAR), action);
            this.viewport = viewport;
            setHoverTexture(new ColorRectTexture(hoverColor));
        }

        @Override
        public boolean isMouseOverElement(double mouseX, double mouseY) {
            return viewport != null
                    && Widget.isMouseOver(
                    viewport.getPositionX(), viewport.getPositionY(),
                    viewport.getSizeWidth(), viewport.getSizeHeight(),
                    mouseX, mouseY)
                    && super.isMouseOverElement(mouseX, mouseY);
        }
    }
}
