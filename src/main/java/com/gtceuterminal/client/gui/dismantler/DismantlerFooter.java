package com.gtceuterminal.client.gui.dismantler;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class DismantlerFooter {

    private static final int C_DISMANTLE = 0xFF7A1A1A;
    private static final int C_ERROR     = 0xFFFF4444;
    private static final int C_WHITE     = 0xFFFFFFFF;

    private DismantlerFooter() {}

    static WidgetGroup build(
            int x, int y, int w, int h,
            int colorBgMedium,
            int colorBorderLight,
            Runnable onDismantle,
            Runnable onCancel
    ) {
        WidgetGroup footer = new WidgetGroup(x, y, w, h);

        int btnH = 24, btnW = (w - 10) / 2, btnY = (h - btnH) / 2;

        ButtonWidget dismantleBtn = new ButtonWidget(0, btnY, btnW, btnH,
                new GuiTextureGroup(
                        new ColorRectTexture(C_DISMANTLE),
                        new ColorBorderTexture(1, C_ERROR)),
                cd -> onDismantle.run());
        dismantleBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.dismantler.action.dismantle").getString())
                .setWidth(btnW).setType(TextTexture.TextType.NORMAL));
        dismantleBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(0xFF9A2A2A),
                new ColorBorderTexture(2, C_WHITE)));
        footer.addWidget(dismantleBtn);

        ButtonWidget cancelBtn = new ButtonWidget(btnW + 10, btnY, btnW, btnH,
                new GuiTextureGroup(
                        new ColorRectTexture(colorBgMedium),
                        new ColorBorderTexture(1, colorBorderLight)),
                cd -> onCancel.run());
        cancelBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.dismantler.action.cancel").getString())
                .setWidth(btnW).setType(TextTexture.TextType.NORMAL));
        cancelBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(colorBgMedium),
                new ColorBorderTexture(2, C_WHITE)));
        footer.addWidget(cancelBtn);

        return footer;
    }
}