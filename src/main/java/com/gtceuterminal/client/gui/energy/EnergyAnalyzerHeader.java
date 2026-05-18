package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.common.theme.bundle.ThemeBundle;
import com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class EnergyAnalyzerHeader {

    private EnergyAnalyzerHeader() {}

    static WidgetGroup build(
            int guiW,
            int headerH,
            int pad,
            int machineCount,
            ItemTheme theme,
            WidgetGroup rootGroup,
            Player player
    ) {
        WidgetGroup g = new WidgetGroup(0, 0, guiW, headerH);
        g.setBackground(theme.isNativeStyle()
                ? com.gregtechceu.gtceu.api.gui.GuiTextures.TITLE_BAR_BACKGROUND
                : new ColorRectTexture(semi(theme.panelColor)));

        int titleX = pad;
        if (theme.isBundleStyle()) {
            ThemeBundle bundle = ThemeBundleRegistry.get(theme.bundleId);
            if (bundle != null) {
                IGuiTexture icon = bundle.iconTexture();
                if (icon != null) {
                    int iconSize = 18;
                    int iconY = (headerH - iconSize) / 2;
                    g.addWidget(new ImageWidget(titleX, iconY, iconSize, iconSize, icon));
                    titleX += iconSize + 4;
                }
            }
        }

        LabelWidget title = new LabelWidget(titleX, 8,
                Component.translatable("gui.gtceuterminal.energy_analyzer.title").getString());
        title.setTextColor(0xFFFFAA00);
        g.addWidget(title);

        LabelWidget sub = new LabelWidget(guiW - 180, 8,
                Component.translatable("gui.gtceuterminal.energy_analyzer.header.linked", machineCount).getString());
        sub.setTextColor(0xFFAAAAAA);
        g.addWidget(sub);

        ButtonWidget gearBtn = new ButtonWidget(guiW - 44, (headerH - 14) / 2, 14, 14,
                new ColorRectTexture(0x00000000),
                cd -> ThemeEditorDialog.open(rootGroup, theme));
        gearBtn.setButtonTexture(new TextTexture("§7⚙").setWidth(14).setType(TextTexture.TextType.NORMAL));
        gearBtn.setHoverTexture(new ColorRectTexture(0x33FFFFFF));
        gearBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.theme_settings").getString());
        g.addWidget(gearBtn);

        int accent80 = (theme.accentColor & 0x00FFFFFF) | 0x80000000;
        ButtonWidget closeBtn = new ButtonWidget(guiW - 26, (headerH - 14) / 2, 20, 14,
                new GuiTextureGroup(new ColorRectTexture(theme.panelColor), new ColorBorderTexture(1, accent80)),
                cd -> player.closeContainer());
        closeBtn.setButtonTexture(new TextTexture("§c✕").setWidth(20).setType(TextTexture.TextType.NORMAL));
        closeBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(0xFFAA0000),
                new ColorBorderTexture(1, 0xFFFFFFFF)));
        g.addWidget(closeBtn);

        return g;
    }

    private static int semi(int color) {
        return (color & 0x00FFFFFF) | 0xCC000000;
    }
}