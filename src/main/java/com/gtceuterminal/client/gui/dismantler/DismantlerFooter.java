package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class DismantlerFooter {

    private DismantlerFooter() {
    }

    static WidgetGroup build(
            int x, int y, int w, int h,
            Runnable onDismantle,
            Runnable onCancel
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup footer = new WidgetGroup(x, y, w, h);
        footer.setBackground(new ColorRectTexture(tokens.panel()));

        int btnH = 22, btnY = (h - btnH) / 2;
        int allW = 220;
        int cancelW = 110;

        footer.addWidget(TerminalButton.text3d(0, btnY, allW, btnH,
                Component.translatable("gui.gtceuterminal.dismantler.action.dismantle").getString(),
                tokens.errorSurface(), cd -> onDismantle.run()));

        footer.addWidget(TerminalButton.text3d(w - cancelW, btnY, cancelW, btnH,
                Component.translatable("gui.gtceuterminal.dismantler.action.cancel").getString(),
                tokens.panelDisabled(), cd -> onCancel.run()));

        return footer;
    }
}
