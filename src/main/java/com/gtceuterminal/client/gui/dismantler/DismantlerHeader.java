package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.client.gui.style.Bevel3DTexture;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
import com.gtceuterminal.client.gui.widget.HeaderItemIcon;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class DismantlerHeader {

    private DismantlerHeader() {
    }

    static WidgetGroup build(
            int guiW,
            int headerH,
            BlockPos controllerPos,
            ItemStack itemStack,
            ItemTheme theme,
            WidgetGroup mainGroup,
            Runnable onClose
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup header = new WidgetGroup(2, 2, guiW - 4, headerH);
        header.setBackground(new Bevel3DTexture(tokens.panel()));

        int titleX = 8;
        int iconSize = 20, iconY = (headerH - iconSize) / 2;
        if (theme.isBundleStyle()) {
            com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                    com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
            if (bundle != null) {
                com.lowdragmc.lowdraglib.gui.texture.IGuiTexture bundleIcon = bundle.iconTexture();
                if (bundleIcon != null) {
                    header.addWidget(new ImageWidget(titleX, iconY, iconSize, iconSize, bundleIcon));
                    titleX += iconSize + 4;
                }
            }
        } else {
            header.addWidget(HeaderItemIcon.build(titleX, iconY, iconSize, itemStack,
                    tokens.panelDisabled(), tokens.border()));
            titleX += iconSize + 6;
        }

        LabelWidget title = new LabelWidget(titleX, 11,
                fitText(itemStack.getHoverName().getString(), 260));
        title.setTextColor(theme.isBundleStyle() ? theme.labelColor() : tokens.textStrong());
        header.addWidget(title);

        int btnSize = 20;
        int btnY = (headerH - btnSize) / 2;
        int closeX = header.getSizeWidth() - btnSize - 6;
        int themeX = closeX - btnSize - 4;

        String coords = String.format("(%d, %d, %d)",
                controllerPos.getX(), controllerPos.getY(), controllerPos.getZ());
        int coordsX = themeX - Minecraft.getInstance().font.width(coords) - 12;
        if (coordsX > titleX + 270) {
            LabelWidget position = new LabelWidget(coordsX, 11, coords);
            position.setTextColor(tokens.textMuted());
            header.addWidget(position);
        }

        ButtonWidget gearBtn = TerminalButton.icon3d(themeX, btnY, btnSize, "✦",
                cd -> ThemeEditorDialog.open(mainGroup, ItemTheme.load(itemStack)));
        gearBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.theme_settings").getString());
        header.addWidget(gearBtn);

        ButtonWidget closeBtn = TerminalButton.icon3d(closeX, btnY, btnSize, "×",
                cd -> onClose.run());
        closeBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        header.addWidget(closeBtn);

        return header;
    }

    private static String fitText(String text, int maxWidth) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        return font.substrByWidth(Component.literal(text),
                Math.max(0, maxWidth - font.width(ellipsis))).getString() + ellipsis;
    }
}
