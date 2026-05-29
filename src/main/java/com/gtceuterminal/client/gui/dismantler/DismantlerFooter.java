package com.gtceuterminal.client.gui.dismantler;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.gtceuterminal.client.gui.widget.TerminalButton;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class DismantlerFooter {

    private static final int C_DISMANTLE_ALL  = 0xFF7A1A1A;
    private static final int C_BORDER_ALL     = 0xFF8A2020;

    private DismantlerFooter() {}

    static WidgetGroup build(
            int x, int y, int w, int h,
            int colorBgMedium,
            int colorBorderLight,
            Runnable onDismantle,
            Runnable onCancel
    ) {
        WidgetGroup footer = new WidgetGroup(x, y, w, h);
        footer.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF1A1A1A),
                new ColorBorderTexture(1, 0xFF2A2A2A)));

        int btnH = 22, btnY = (h - btnH) / 2, spacing = 8;
        int allW = 220;
        int cancelW = 110;
        int startX = w - allW - cancelW - spacing - 8;

        footer.addWidget(TerminalButton.action(startX, btnY, cancelW, btnH,
                Component.translatable("gui.gtceuterminal.dismantler.action.cancel").getString(),
                colorBgMedium, colorBorderLight, cd -> onCancel.run()));

        footer.addWidget(TerminalButton.danger(startX + cancelW + spacing, btnY, allW, btnH,
                "§c⚠ Dismantle Multiblock",
                C_DISMANTLE_ALL, C_BORDER_ALL, cd -> onDismantle.run()));

        return footer;
    }
}