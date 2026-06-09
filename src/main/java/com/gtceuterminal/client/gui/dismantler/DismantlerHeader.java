package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
import com.gtceuterminal.client.gui.widget.HeaderItemIcon;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class DismantlerHeader {

    private DismantlerHeader() {}

    static WidgetGroup build(
            int guiW,
            int headerH,
            BlockPos controllerPos,
            ItemStack itemStack,
            ItemTheme theme,
            WidgetGroup mainGroup,
            Player player,
            Runnable onClose
    ) {
        WidgetGroup header = new WidgetGroup(2, 2, guiW - 4, headerH);
        header.setBackground(theme.headerTexture());

        int titleX = 10;
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
                    theme.isNativeStyle() ? 0xFF2A2A2A : theme.accent(0x55),
                    theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF)));
            titleX += iconSize + 6;
        }

        String coords = String.format("(%d, %d, %d)",
                controllerPos.getX(), controllerPos.getY(), controllerPos.getZ());
        String baseTitle = stripFormatting(Component.translatable("gui.gtceuterminal.dismantler.title").getString());
        LabelWidget title = new LabelWidget(titleX, 10, "§f" + baseTitle + " §7— @ " + coords);
        title.setTextColor(theme.isBundleStyle() ? theme.labelColor() : 0xFFFFFFFF);
        header.addWidget(title);

        ButtonWidget gearBtn = TerminalButton.ghostIcon(guiW - 42, 6, 14, "config",
                cd -> ThemeEditorDialog.open(mainGroup, ItemTheme.load(itemStack)));
        gearBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.theme_settings").getString());
        header.addWidget(gearBtn);

        int accentColor = theme.accentColor;
        ButtonWidget closeBtn = new ButtonWidget(guiW - 24, 6, 14, 14,
                new GuiTextureGroup(
                        new ColorRectTexture(theme.panelColor),
                        new ColorBorderTexture(1, (accentColor & 0x00FFFFFF) | 0x80000000)),
                cd -> onClose.run());
        closeBtn.setButtonTexture(TerminalButton.iconTex("close"));
        closeBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        closeBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(0x66AA0000),
                new ColorBorderTexture(1, 0xFFFFFFFF),
                TerminalButton.iconTex("close")));
        header.addWidget(closeBtn);

        return header;
    }

    private static String stripFormatting(String s) {
        return s == null ? "" : s.replaceAll("§.", "");
    }
}