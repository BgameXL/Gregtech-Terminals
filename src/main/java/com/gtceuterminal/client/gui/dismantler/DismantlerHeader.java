package com.gtceuterminal.client.gui.dismantler;

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

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class DismantlerHeader {

    private DismantlerHeader() {}

    static WidgetGroup build(
            int guiW,
            int headerH,
            BlockPos controllerPos,
            ItemTheme theme,
            WidgetGroup mainGroup,
            Player player,
            Runnable onClose
    ) {
        WidgetGroup header = new WidgetGroup(2, 2, guiW - 4, headerH);
        header.setBackground(theme.headerTexture());

        int titleX = 10;
        if (theme.isBundleStyle()) {
            ThemeBundle bundle = ThemeBundleRegistry.get(theme.bundleId);
            if (bundle != null) {
                IGuiTexture icon = bundle.iconTexture();
                if (icon != null) {
                    int iconSize = 20, iconY = (headerH - iconSize) / 2;
                    header.addWidget(new ImageWidget(titleX, iconY, iconSize, iconSize, icon));
                    titleX += iconSize + 4;
                }
            }
        }

        LabelWidget title = new LabelWidget(titleX, 10,
                Component.translatable("gui.gtceuterminal.dismantler.title").getString());
        title.setTextColor(theme.isBundleStyle() ? theme.labelColor() : 0xFFFFFFFF);
        header.addWidget(title);

        String coords = String.format("(%d, %d, %d)",
                controllerPos.getX(), controllerPos.getY(), controllerPos.getZ());
        LabelWidget coordsLabel = new LabelWidget((guiW - 4) / 2 - 40, 10, "§7" + coords);
        coordsLabel.setTextColor(0xFFAAAAAA);
        header.addWidget(coordsLabel);

        ButtonWidget gearBtn = new ButtonWidget(guiW - 50, 6, 18, 18,
                new ColorRectTexture(0x00000000),
                cd -> ThemeEditorDialog.open(mainGroup, ItemTheme.load(player.getMainHandItem())));
        gearBtn.setButtonTexture(new TextTexture("§7⚙").setWidth(18).setType(TextTexture.TextType.NORMAL));
        gearBtn.setHoverTexture(new ColorRectTexture(0x40FFFFFF));
        gearBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.theme_settings").getString());
        header.addWidget(gearBtn);

        int accentColor = theme.accentColor;
        ButtonWidget closeBtn = new ButtonWidget(guiW - 28, 6, 20, 18,
                new GuiTextureGroup(
                        new ColorRectTexture(theme.panelColor),
                        new ColorBorderTexture(1, (accentColor & 0x00FFFFFF) | 0x80000000)),
                cd -> onClose.run());
        closeBtn.setButtonTexture(new TextTexture("§c✕").setWidth(20).setType(TextTexture.TextType.NORMAL));
        closeBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(0xFFAA0000),
                new ColorBorderTexture(1, 0xFFFFFFFF)));
        header.addWidget(closeBtn);

        return header;
    }
}