package com.gtceuterminal.client.gui.schematic;

import com.gtceuterminal.client.gui.style.TerminalText;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.gtceuterminal.client.gui.widget.ViewportClippedButton;
import com.gtceuterminal.client.gui.widget.TerminalScrollGroup;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.IntConsumer;

@OnlyIn(Dist.CLIENT)
final class SchematicListPanel {

    private static final int ENTRY_H = 36;
    private static final int ENTRY_STEP = 38;
    private static final int HEADER_H = 22;

    private SchematicListPanel() {
    }

    static WidgetGroup build(
            int panelX, int panelY, int leftW, int listH,
            int selectedIndex,
            List<SchematicData> schematics,
            IntConsumer onSelect,
            DraggableScrollableWidgetGroup[] scrollRef
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup panel = new WidgetGroup(panelX, panelY, leftW, listH);
        panel.setBackground(new ColorRectTexture(tokens.surface()));

        LabelWidget title = new LabelWidget(8, 7, TerminalText.strip(Component.translatable(
                "gui.gtceuterminal.schematic_interface.saved_schematics_label").getString()));
        title.setColor(tokens.textMuted());
        panel.addWidget(title);

        DraggableScrollableWidgetGroup scroll =
                new TerminalScrollGroup(0, HEADER_H, leftW, listH - HEADER_H);
        scroll.setYScrollBarWidth(0);
        scrollRef[0] = scroll;
        panel.addWidget(scroll);

        repopulate(scroll, schematics, selectedIndex, leftW - 2, onSelect);

        return panel;
    }

    static void repopulate(
            DraggableScrollableWidgetGroup scroll,
            List<SchematicData> schematics,
            int selectedIndex,
            int entryW,
            IntConsumer onSelect
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        int savedY = (scroll instanceof TerminalScrollGroup t) ? t.getScrollYOffset() : 0;
        scroll.clearAllWidgets();

        if (schematics.isEmpty()) {
            LabelWidget empty = new LabelWidget(8, 12,
                    TerminalText.strip(Component.translatable(
                            "gui.gtceuterminal.schematic_interface.no_schematics_saved").getString()));
            empty.setColor(tokens.textMuted());
            scroll.addWidget(empty);
            return;
        }

        int y = 2;
        for (int i = 0; i < schematics.size(); i++) {
            scroll.addWidget(buildEntry(schematics.get(i), i, y, entryW, selectedIndex, tokens, onSelect, scroll));
            y += ENTRY_STEP;
        }

        int maxScroll = Math.max(0, y - scroll.getSizeHeight());
        scroll.setScrollYOffset(Math.min(savedY, maxScroll));
    }

    private static WidgetGroup buildEntry(
            SchematicData schematic, int index, int yPos, int entryW,
            int selectedIndex, TerminalTheme tokens, IntConsumer onSelect,
            DraggableScrollableWidgetGroup scroll
    ) {
        boolean sel = index == selectedIndex;
        WidgetGroup entry = new WidgetGroup(1, yPos, entryW, ENTRY_H);
        entry.setBackground(new GuiTextureGroup(
                new ColorRectTexture(sel ? tokens.accentSoft() : tokens.panel()),
                new ColorBorderTexture(1, sel ? tokens.border() : Palette.CLEAR)));

        ViewportClippedButton click = new ViewportClippedButton(0, 0, entryW, ENTRY_H,
                tokens.hover(), scroll, cd -> onSelect.accept(index));
        entry.addWidget(click);

        String name = schematic.getName();
        LabelWidget nameLbl = new LabelWidget(8, 7, fitText(name, entryW - 16));
        nameLbl.setColor(tokens.textStrong());
        entry.addWidget(nameLbl);

        var size = schematic.getSize();
        String sub = schematic.getBlocks().size() + " blocks · "
                + size.getX() + "×" + size.getY() + "×" + size.getZ();
        LabelWidget subLbl = new LabelWidget(8, 21, fitText(sub, entryW - 16));
        subLbl.setColor(tokens.textFaint());
        entry.addWidget(subLbl);

        return entry;
    }

    private static String fitText(String text, int maxWidth) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        return font.substrByWidth(Component.literal(text),
                Math.max(0, maxWidth - font.width(ellipsis))).getString() + ellipsis;
    }

}
