package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
import com.gtceuterminal.client.gui.widget.HeaderItemIcon;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
            ItemStack itemStack,
            ItemTheme theme,
            WidgetGroup rootGroup,
            Player player
    ) {
        WidgetGroup g = new WidgetGroup(0, 0, guiW, headerH);
        g.setBackground(theme.isNativeStyle()
                ? com.gregtechceu.gtceu.api.gui.GuiTextures.TITLE_BAR_BACKGROUND
                : new ColorRectTexture(semi(theme.panelColor)));

        int titleX = pad;
        int iconSize = 18;
        int iconY = (headerH - iconSize) / 2;
        if (theme.isBundleStyle()) {
            com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                    com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
            if (bundle != null) {
                com.lowdragmc.lowdraglib.gui.texture.IGuiTexture bundleIcon = bundle.iconTexture();
                if (bundleIcon != null) {
                    g.addWidget(new ImageWidget(titleX, iconY, iconSize, iconSize, bundleIcon));
                    titleX += iconSize + 4;
                }
            }
        } else {
            g.addWidget(HeaderItemIcon.build(titleX, iconY, iconSize, itemStack,
                    theme.isNativeStyle() ? 0xFF2A2A2A : theme.accent(0x55),
                    theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF)));
            titleX += iconSize + 4;
        }

        LabelWidget title = new LabelWidget(titleX, 8,
                Component.translatable("gui.gtceuterminal.energy_analyzer.title").getString());
        title.setTextColor(0xFFFFAA00);
        g.addWidget(title);

        LabelWidget sub = new LabelWidget(guiW - 180, 8,
                Component.translatable("gui.gtceuterminal.energy_analyzer.header.linked", machineCount).getString());
        sub.setTextColor(0xFFAAAAAA);
        g.addWidget(sub);

        ButtonWidget gearBtn = TerminalButton.ghostIcon(guiW - 44, (headerH - 14) / 2, 14, "⚙", "§7",
                cd -> ThemeEditorDialog.open(rootGroup, theme));
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
