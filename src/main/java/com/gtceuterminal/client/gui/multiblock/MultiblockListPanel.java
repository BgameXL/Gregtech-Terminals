package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.multiblock.MultiblockStatus;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.gtceuterminal.client.gui.widget.TerminalButton;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
final class MultiblockListPanel {

    private static final int ENTRY_H    = 46;
    private static final int ENTRY_STEP = 48;
    private static final int FOOTER_H   = 28;

    private MultiblockListPanel() {}

    static WidgetGroup build(
            int panelX, int panelY, int panelW, int panelH,
            int selectedIndex,
            List<MultiblockInfo> multiblocks,
            int colorBgDark, int colorBgMedium, int colorBgLight,
            int colorBorderDark, int colorBorderLight,
            int colorTextGray, int colorHover,
            Consumer<Integer> onEntryClick,
            Consumer<MultiblockInfo> onHighlight,
            Consumer<MultiblockInfo> onRename,
            Supplier<MultiblockInfo> getSelected,
            WidgetGroup[] footerRef,
            DraggableScrollableWidgetGroup[] scrollRef
    ) {
        int listH = panelH - FOOTER_H - 1;
        int scrollW = panelW - 10;

        WidgetGroup panel = new WidgetGroup(panelX, panelY, panelW, listH);
        panel.setBackground(new ColorRectTexture(colorBgDark));

        DraggableScrollableWidgetGroup scroll =
                new DraggableScrollableWidgetGroup(0, 0, scrollW, listH);
        scroll.setYScrollBarWidth(8);
        scroll.setYBarStyle(
                new ColorRectTexture(colorBorderDark),
                new ColorRectTexture(colorBorderLight));
        scrollRef[0] = scroll;

        populateScroll(scroll, multiblocks, selectedIndex,
                scrollW - 4, colorBgMedium, colorBgLight, colorTextGray, colorHover,
                onEntryClick);

        panel.addWidget(scroll);

        footerRef[0] = buildFooter(panelX, panelY + listH, panelW,
                colorBorderDark, colorTextGray, colorHover,
                getSelected, onHighlight, onRename);

        return panel;
    }

    static void repopulate(
            DraggableScrollableWidgetGroup scroll,
            List<MultiblockInfo> multiblocks,
            int selectedIndex,
            int scrollW,
            int colorBgMedium, int colorBgLight,
            int colorTextGray, int colorHover,
            Consumer<Integer> onEntryClick
    ) {
        scroll.clearAllWidgets();
        populateScroll(scroll, multiblocks, selectedIndex,
                scrollW, colorBgMedium, colorBgLight, colorTextGray, colorHover,
                onEntryClick);
    }

    private static void populateScroll(
            DraggableScrollableWidgetGroup scroll,
            List<MultiblockInfo> multiblocks,
            int selectedIndex,
            int entryW,
            int colorBgMedium, int colorBgLight,
            int colorTextGray, int colorHover,
            Consumer<Integer> onEntryClick
    ) {
        if (multiblocks.isEmpty()) {
            LabelWidget empty = new LabelWidget(10, 12,
                    Component.translatable("gui.gtceuterminal.multiblock_manager.none_found").getString());
            empty.setTextColor(colorTextGray);
            scroll.addWidget(empty);
            return;
        }
        int y = 2;
        for (int i = 0; i < multiblocks.size(); i++) {
            final int idx = i;
            scroll.addWidget(buildEntry(multiblocks.get(i), idx, y, entryW,
                    idx == selectedIndex, colorBgMedium, colorBgLight, colorTextGray, colorHover,
                    scroll,
                    onEntryClick));
            y += ENTRY_STEP;
        }
    }

    private static WidgetGroup buildEntry(
            MultiblockInfo mb, int index, int yPos, int entryW,
            boolean selected,
            int colorBgMedium, int colorBgLight,
            int colorTextGray, int colorHover,
            DraggableScrollableWidgetGroup viewport,
            Consumer<Integer> onEntryClick
    ) {
        WidgetGroup entry = new WidgetGroup(0, yPos, entryW, ENTRY_H);

        int bg = selected ? colorBgLight : colorBgMedium;
        int leftBarColor = selected ? 0xFF4CAF50 : 0x00000000;
        entry.setBackground(new GuiTextureGroup(
                new ColorRectTexture(bg),
                new ColorBorderTexture(2, leftBarColor)));

        // Clickable area (full entry minus buttons)
        ButtonWidget clickBtn = new ViewportClippedButtonWidget(
                0, 0, entryW, ENTRY_H, colorHover, viewport, cd -> onEntryClick.accept(index));
        entry.addWidget(clickBtn);

        // Name line
        String displayName = resolveDisplayName(mb);
        String nameColor = mb.isVanillaGTCEu() ? "§f" : "§" + modColorCode(mb.getModColor());
        entry.addWidget(new LabelWidget(8, 6, nameColor + truncate(displayName, 24)));

        // Sub line: status · tier · distance, matching the compact mockup row.
        MultiblockStatus status = mb.getStatus();
        String sub = "§" + statusColorCode(status) + "● " + status.getDisplayName().toLowerCase()
                + " §8" + mb.getTierName() + " · " + mb.getDistanceString();
        LabelWidget subLabel = new LabelWidget(8, 21, sub);
        subLabel.setTextColor(colorTextGray);
        entry.addWidget(subLabel);

        return entry;
    }

    private static WidgetGroup buildFooter(
            int x, int y, int panelW,
            int colorBorderDark, int colorTextGray, int colorHover,
            Supplier<MultiblockInfo> getSelected,
            Consumer<MultiblockInfo> onHighlight,
            Consumer<MultiblockInfo> onRename
    ) {
        WidgetGroup footer = new WidgetGroup(x, y, panelW, FOOTER_H);
        footer.addWidget(new ImageWidget(0, 0, panelW, 1, new ColorRectTexture(colorBorderDark)));

        ButtonWidget highlightBtn = TerminalButton.action(6, 5, panelW - 40, 18, "§e⬡ Highlight",
                0xFF222222, 0xFF3A3A3A,
                cd -> { MultiblockInfo sel = getSelected.get(); if (sel != null) onHighlight.accept(sel); });
        highlightBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.multiblock_manager.highlight_tooltip").getString());
        footer.addWidget(highlightBtn);

        ButtonWidget renameBtn = TerminalButton.icon(panelW - 28, 4, 20, "✎", "§7",
                0xFF222222, 0xFF3A3A3A,
                cd -> { MultiblockInfo sel = getSelected.get(); if (sel != null) onRename.accept(sel); });
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

    private static char modColorCode(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        if (g > 200 && r < 100 && b < 100) return 'a';
        if (r > 200 && g > 200 && b < 100) return 'e';
        if (r < 100 && g > 200 && b > 200) return 'b';
        if (r > 200 && g < 100 && b > 200) return 'd';
        if (r > 200 && g > 100 && b < 50)  return '6';
        return '7';
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

    private static final class ViewportClippedButtonWidget extends ButtonWidget {
        private final DraggableScrollableWidgetGroup viewport;

        private ViewportClippedButtonWidget(
                int x, int y, int w, int h,
                int hoverColor,
                DraggableScrollableWidgetGroup viewport,
                Consumer<com.lowdragmc.lowdraglib.gui.util.ClickData> action
        ) {
            super(x, y, w, h, new ColorRectTexture(0x00000000), action);
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