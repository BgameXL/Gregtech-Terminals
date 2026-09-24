package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.client.gui.style.Bevel3DTexture;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
import com.gtceuterminal.client.gui.widget.HeaderItemIcon;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class EnergyAnalyzerHeader {

    private EnergyAnalyzerHeader() {
    }

    static WidgetGroup build(
            int guiW,
            int headerH,
            int machineCount,
            ItemStack itemStack,
            ItemTheme theme,
            WidgetGroup rootGroup,
            Player player
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup g = new WidgetGroup(2, 2, guiW - 4, headerH);
        g.setBackground(new Bevel3DTexture(tokens.panel()));

        int titleX = 8;
        int iconSize = 20;
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
                    tokens.panelDisabled(), tokens.border()));
            titleX += iconSize + 6;
        }

        LabelWidget title = new LabelWidget(titleX, 11,
                fitText(itemStack.getHoverName().getString(), 260));
        title.setTextColor(theme.isBundleStyle() ? theme.labelColor() : tokens.textStrong());
        g.addWidget(title);

        int btnSize = 20;
        int btnY = (headerH - btnSize) / 2;
        int closeX = g.getSizeWidth() - btnSize - 6;
        int themeX = closeX - btnSize - 4;

        String linkedText = Component.translatable(
                "gui.gtceuterminal.energy_analyzer.header.linked", machineCount).getString();
        int linkedX = themeX - Minecraft.getInstance().font.width(linkedText) - 12;
        if (linkedX > titleX + 270) {
            LabelWidget linked = new LabelWidget(linkedX, 11, linkedText);
            linked.setTextColor(tokens.textMuted());
            g.addWidget(linked);
        }

        ButtonWidget gearBtn = TerminalButton.icon3d(themeX, btnY, btnSize, "✦",
                cd -> ThemeEditorDialog.open(rootGroup, theme));
        gearBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.theme_settings").getString());
        g.addWidget(gearBtn);

        ButtonWidget closeBtn = TerminalButton.icon3d(closeX, btnY, btnSize, "×",
                cd -> player.closeContainer());
        closeBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        g.addWidget(closeBtn);

        return g;
    }

    private static String fitText(String text, int maxWidth) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        return font.substrByWidth(Component.literal(text),
                Math.max(0, maxWidth - font.width(ellipsis))).getString() + ellipsis;
    }
}
