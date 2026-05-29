package com.gtceuterminal.client.gui.schematic;

import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.IntConsumer;

@OnlyIn(Dist.CLIENT)
final class SchematicListPanel {

    private static final int C_SUCCESS  = 0xFF4CAF50;
    private static final int C_HOVER    = 0x40FFFFFF;
    private static final int ENTRY_H    = 36;
    private static final int ENTRY_STEP = 38;

    private SchematicListPanel() {}

    static WidgetGroup build(
            int panelX, int panelY, int leftW, int listH, int pad,
            int colorBorderDark, int colorBorderLight, int colorTextGray,
            ItemTheme theme, int selectedIndex,
            List<SchematicData> schematics,
            IntConsumer onSelect,
            TextFieldWidget[] nameInputRef,
            DraggableScrollableWidgetGroup[] scrollRef
    ) {
        WidgetGroup panel = new WidgetGroup(panelX, panelY, leftW, listH);
        panel.setBackground(theme.panelTexture());

        int scrollH = listH;
        DraggableScrollableWidgetGroup scroll =
                new DraggableScrollableWidgetGroup(0, 0, leftW - 8, scrollH);
        scroll.setYScrollBarWidth(6);
        scroll.setYBarStyle(new ColorRectTexture(colorBorderDark), new ColorRectTexture(colorBorderLight));
        scrollRef[0] = scroll;
        panel.addWidget(scroll);

        repopulate(scroll, schematics, selectedIndex, leftW - 8 - 4, colorTextGray, theme, onSelect);

        return panel;
    }

    static void repopulate(
            DraggableScrollableWidgetGroup scroll,
            List<SchematicData> schematics,
            int selectedIndex,
            int entryW,
            int colorTextGray,
            ItemTheme theme,
            IntConsumer onSelect
    ) {
        scroll.clearAllWidgets();

        if (schematics.isEmpty()) {
            LabelWidget empty = new LabelWidget(8, 12,
                    Component.translatable("gui.gtceuterminal.schematic_interface.no_schematics_saved").getString());
            empty.setTextColor(colorTextGray);
            scroll.addWidget(empty);
            return;
        }

        int y = 2;
        for (int i = 0; i < schematics.size(); i++) {
            scroll.addWidget(buildEntry(schematics.get(i), i, y, entryW, selectedIndex, theme, onSelect));
            y += ENTRY_STEP;
        }
    }

    private static WidgetGroup buildEntry(
            SchematicData schematic, int index, int yPos, int entryW,
            int selectedIndex, ItemTheme theme, IntConsumer onSelect
    ) {
        boolean sel = index == selectedIndex;
        int colorBgLight  = theme.isNativeStyle() ? 0xFF3A3A3A : theme.accent(0xAA);
        int colorBgMedium = theme.panelColor;

        WidgetGroup entry = new WidgetGroup(0, yPos, entryW, ENTRY_H);

        if (sel) {
            entry.setBackground(new GuiTextureGroup(
                    new ColorRectTexture(colorBgLight),
                    new ColorBorderTexture(2, C_SUCCESS)));
        } else {
            entry.setBackground(new ColorRectTexture(colorBgMedium));
        }

        ButtonWidget click = new ButtonWidget(0, 0, entryW, ENTRY_H,
                new ColorRectTexture(0x00000000), cd -> onSelect.accept(index));
        click.setHoverTexture(new ColorRectTexture(C_HOVER));
        entry.addWidget(click);

        String name = schematic.getName();
        if (name.length() > 24) name = name.substring(0, 23) + "…";
        LabelWidget nameLbl = new LabelWidget(8, 7, "§f" + name);
        nameLbl.setTextColor(0xFFDDDDDD);
        entry.addWidget(nameLbl);

        var size = schematic.getSize();
        String sub = schematic.getBlocks().size() + " blocks · "
                + size.getX() + "×" + size.getY() + "×" + size.getZ();
        LabelWidget subLbl = new LabelWidget(8, 21, "§8" + sub);
        subLbl.setTextColor(0xFF666666);
        entry.addWidget(subLbl);

        return entry;
    }
}