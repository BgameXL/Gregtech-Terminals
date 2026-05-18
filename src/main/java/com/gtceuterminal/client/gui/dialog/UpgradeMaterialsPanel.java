package com.gtceuterminal.client.gui.dialog;

import com.gtceuterminal.client.gui.widget.LDLMaterialListWidget;
import com.gtceuterminal.common.material.MaterialAvailability;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
final class UpgradeMaterialsPanel {

    private static final int C_SUCCESS = 0xFF00FF00;
    private static final int C_GRAY    = 0xFFAAAAAA;
    private static final int C_WHITE   = 0xFFFFFFFF;

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

        LabelWidget sectionLabel = new LabelWidget(10, 5,
                Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.required_label").getString());
        sectionLabel.setTextColor(C_WHITE);
        panel.addWidget(sectionLabel);

        if (!tierSelected) {
            LabelWidget placeholder = new LabelWidget(10, 22,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.select_option_to_see").getString());
            placeholder.setTextColor(C_GRAY);
            panel.addWidget(placeholder);
            return panel;
        }

        if (creativeMode) {
            LabelWidget creative = new LabelWidget(10, 22,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.creative_mode").getString());
            creative.setTextColor(C_SUCCESS);
            panel.addWidget(creative);
            if (materials != null && !materials.isEmpty()) {
                panel.addWidget(new LDLMaterialListWidget(0, 38, w, h - 42, materials));
            }
            return panel;
        }

        if (materials != null && !materials.isEmpty()) {
            panel.addWidget(new LDLMaterialListWidget(0, 20, w, h - 24, materials));
        } else {
            LabelWidget none = new LabelWidget(10, 22,
                    Component.translatable("gui.gtceuterminal.component_upgrade_dialog.materials.select_option_to_see").getString());
            none.setTextColor(C_GRAY);
            panel.addWidget(none);
        }

        return panel;
    }
}