package com.gtceuterminal.client.gui.dialog;

import com.gtceuterminal.client.gui.widget.LDLMaterialListWidget;
import com.gtceuterminal.common.ae2.MENetworkScanner;
import com.gtceuterminal.common.material.MaterialAvailability;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
final class UpgradeMaterialsPanel {

    private static final int C_WHITE   = 0xFFFFFFFF;
    private static final int C_GRAY    = 0xFF888888;
    private static final int C_SUCCESS = 0xFF00FF00;
    private static final int C_SECTION = 0xFF555555;

    private static final int BG_ME      = 0xFF1A3A1A;
    private static final int BD_ME      = 0xFF2D6B2D;
    private static final int BG_CRAFT   = 0xFF3A2A0A;
    private static final int BD_CRAFT   = 0xFF8A5A10;
    private static final int BG_MISS    = 0xFF3A1010;
    private static final int BD_MISS    = 0xFF8A2020;

    private static final int ENTRY_H = 22;
    private static final int ENTRY_GAP = 3;

    private UpgradeMaterialsPanel() {}

    static WidgetGroup build(
            int x, int y, int w, int h,
            int colorBgDark,
            int colorBorderDark,
            boolean tierSelected,
            boolean creativeMode,
            List<MaterialAvailability> materials
    ) {
        WidgetGroup panel = new WidgetGroup(x, y, w, h);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(colorBgDark),
                new ColorBorderTexture(1, colorBorderDark)));

        int countLabel = materials != null ? materials.size() : 0;
        String sectionText = tierSelected
                ? Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.required_label").getString()
                  + (countLabel > 0 ? " (×" + countLabel + ")" : "")
                : Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.required_label").getString();

        LabelWidget sectionLabel = new LabelWidget(10, 5, sectionText);
        sectionLabel.setTextColor(C_SECTION);
        panel.addWidget(sectionLabel);

        if (!tierSelected) {
            LabelWidget placeholder = new LabelWidget(10, 20,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.select_option_to_see").getString());
            placeholder.setTextColor(C_GRAY);
            panel.addWidget(placeholder);
            return panel;
        }

        if (creativeMode) {
            LabelWidget creative = new LabelWidget(10, 20,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.creative_mode").getString());
            creative.setTextColor(C_SUCCESS);
            panel.addWidget(creative);
            return panel;
        }

        if (materials == null || materials.isEmpty()) {
            LabelWidget none = new LabelWidget(10, 20,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.select_option_to_see").getString());
            none.setTextColor(C_GRAY);
            panel.addWidget(none);
            return panel;
        }

        boolean ae2Available = MENetworkScanner.isAE2Available();

        int scrollH = h - 18;
        DraggableScrollableWidgetGroup scroll = new DraggableScrollableWidgetGroup(0, 16, w, scrollH);
        scroll.setYScrollBarWidth(6);
        scroll.setYBarStyle(new ColorRectTexture(colorBorderDark), new ColorRectTexture(0xFF555555));

        int yPos = 0;
        for (MaterialAvailability mat : materials) {
            scroll.addWidget(buildMatRow(mat, 0, yPos, w - 10, ae2Available));
            yPos += ENTRY_H + ENTRY_GAP;
        }

        panel.addWidget(scroll);
        return panel;
    }

    private static WidgetGroup buildMatRow(MaterialAvailability mat, int x, int y, int rowW, boolean ae2Available) {
        WidgetGroup row = new WidgetGroup(x, y, rowW, ENTRY_H);

        row.addWidget(new ImageWidget(6, 3, 16, 16,
                new GuiTextureGroup(new ColorRectTexture(0xFF2A2A2A), new ColorBorderTexture(1, 0xFF3A3A3A))));

        String itemName = mat.getItemName();
        if (itemName.length() > 26) itemName = itemName.substring(0, 25) + "…";
        LabelWidget nameLabel = new LabelWidget(28, 6, itemName);
        nameLabel.setTextColor(0xFFCCCCCC);
        row.addWidget(nameLabel);

        long avail = mat.getTotalAvailable();
        int req = mat.getRequired();
        boolean enough = mat.hasEnough();
        String countText = avail + " / " + req;
        int countColor = enough ? 0xFF6FCF6F : 0xFFE05050;
        int countX = rowW - 130;
        LabelWidget countLabel = new LabelWidget(countX, 6, countText);
        countLabel.setTextColor(countColor);
        row.addWidget(countLabel);

        int badgeX = rowW - 72;
        WidgetGroup badge = buildBadge(mat, badgeX, 4, 62, 14, ae2Available);
        row.addWidget(badge);

        return row;
    }

    private static WidgetGroup buildBadge(MaterialAvailability mat, int x, int y, int w, int h, boolean ae2Available) {
        WidgetGroup badge = new WidgetGroup(x, y, w, h);

        boolean enough = mat.hasEnough();
        long avail = mat.getTotalAvailable();
        int req = mat.getRequired();

        int bg, border;
        String text;

        if (enough && ae2Available && mat.getInMENetwork() >= req) {
            bg = BG_ME; border = BD_ME;
            text = "§aME ✓";
        } else if (enough) {
            bg = BG_ME; border = BD_ME;
            text = "§aready";
        } else if (avail > 0) {
            bg = BG_CRAFT; border = BD_CRAFT;
            text = "§6craftable";
        } else {
            bg = BG_MISS; border = BD_MISS;
            text = "§cmissing";
        }

        badge.setBackground(new GuiTextureGroup(new ColorRectTexture(bg), new ColorBorderTexture(1, border)));
        badge.addWidget(new LabelWidget(4, 2, text));
        return badge;
    }
}