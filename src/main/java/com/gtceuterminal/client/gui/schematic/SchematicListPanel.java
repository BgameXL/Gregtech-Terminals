package com.gtceuterminal.client.gui.schematic;

import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

@OnlyIn(Dist.CLIENT)
final class SchematicListPanel {

    private static final int C_INFO = 0xFF42A5F5;

    private SchematicListPanel() {}

    static WidgetGroup build(
            int panelX,
            int panelY,
            int leftW,
            int listH,
            int pad,
            int colorBorderDark,
            int colorBorderLight,
            int colorTextGray,
            ItemTheme theme,
            int selectedIndex,
            List<SchematicData> schematics,
            IntConsumer onSelect,
            TextFieldWidget[] nameInputRef,             // [0] filled
            DraggableScrollableWidgetGroup[] scrollRef  // [0] filled
    ) {
        WidgetGroup panel = new WidgetGroup(panelX, panelY, leftW, listH);
        panel.setBackground(theme.panelTexture());

        LabelWidget nameLabel = new LabelWidget(8, 6,
                Component.translatable("gui.gtceuterminal.schematic_interface.name_label").getString());
        nameLabel.setTextColor(colorTextGray);
        panel.addWidget(nameLabel);

        TextFieldWidget nameInput = new TextFieldWidget(8, 18, leftW - 16, 16, null, s -> {});
        nameInput.setMaxStringLength(32);
        nameInput.setTextColor(0xFFFFFFFF);
        nameInput.setBackground(theme.backgroundTexture());
        nameInput.setBordered(true);
        panel.addWidget(nameInput);
        nameInputRef[0] = nameInput;

        LabelWidget listLabel = new LabelWidget(8, 40,
                Component.translatable("gui.gtceuterminal.schematic_interface.saved_schematics_label").getString());
        listLabel.setTextColor(colorTextGray);
        panel.addWidget(listLabel);

        int scrollH = listH - 56;
        DraggableScrollableWidgetGroup scroll = new DraggableScrollableWidgetGroup(8, 54, leftW - 16, scrollH);
        scroll.setBackground(theme.backgroundTexture());
        scroll.setYScrollBarWidth(6);
        scroll.setYBarStyle(new ColorRectTexture(colorBorderDark), new ColorRectTexture(colorBorderLight));
        panel.addWidget(scroll);
        scrollRef[0] = scroll;

        repopulate(scroll, schematics, selectedIndex, leftW - 16 - 8, colorTextGray, theme, onSelect);

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
            LabelWidget empty = new LabelWidget(6, 10,
                    Component.translatable("gui.gtceuterminal.schematic_interface.no_schematics_saved").getString());
            empty.setTextColor(colorTextGray);
            scroll.addWidget(empty);
            LabelWidget h1 = new LabelWidget(6, 26,
                    Component.translatable("gui.gtceuterminal.schematic_interface.hint_formed_1").getString());
            h1.setTextColor(0xFF666666);
            scroll.addWidget(h1);
            LabelWidget h2 = new LabelWidget(6, 38,
                    Component.translatable("gui.gtceuterminal.schematic_interface.hint_formed_2").getString());
            h2.setTextColor(0xFF666666);
            scroll.addWidget(h2);
            return;
        }

        int y = 2;
        for (int i = 0; i < schematics.size(); i++) {
            scroll.addWidget(buildEntry(schematics.get(i), i, y, entryW, selectedIndex, theme, onSelect));
            y += 46;
        }
    }

    private static WidgetGroup buildEntry(
            SchematicData schematic,
            int index,
            int yPos,
            int entryW,
            int selectedIndex,
            ItemTheme theme,
            IntConsumer onSelect
    ) {
        boolean sel = index == selectedIndex;
        int colorBgLight  = theme.isNativeStyle() ? 0xFF3A3A3A : theme.accent(0xAA);
        int colorBgMedium = theme.panelColor;

        WidgetGroup entry = new WidgetGroup(0, yPos, entryW, 44);
        entry.setBackground(new ColorRectTexture(sel ? colorBgLight : colorBgMedium));

        if (sel) {
            entry.addWidget(new ImageWidget(0, 0, 3, 44, new ColorRectTexture(C_INFO)));
        }

        ButtonWidget click = new ButtonWidget(0, 0, entryW, 44,
                new ColorRectTexture(0x00000000), cd -> onSelect.accept(index));
        click.setHoverTexture(new ColorRectTexture(0x40FFFFFF));
        entry.addWidget(click);

        String name = schematic.getName();
        if (name.length() > 20) name = name.substring(0, 18) + "…";
        LabelWidget nameLbl = new LabelWidget(8, 6, "§f" + name);
        nameLbl.setTextColor(0xFFFFFFFF);
        entry.addWidget(nameLbl);

        String info = Component.translatable(
                "gui.gtceuterminal.schematic_interface.entry.blocks",
                schematic.getBlocks().size()).getString();
        LabelWidget infoLbl = new LabelWidget(8, 20, info);
        infoLbl.setTextColor(0xFFAAAAAA);
        entry.addWidget(infoLbl);

        BlockPos sz = schematic.getSize();
        entry.addWidget(new LabelWidget(8, 30, "§8" + sz.getX() + "×" + sz.getY() + "×" + sz.getZ()));

        return entry;
    }
}