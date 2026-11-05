package com.gtceuterminal.client.gui.widget;

import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class TerminalTabs {

    private static final int ACTION_BTN = 20;
    private static final int ACTION_PAD = 6;
    private static final int ACTION_GAP = 4;
    public static final int ACTION_CARD_W = ACTION_PAD + ACTION_BTN + ACTION_GAP + ACTION_BTN + ACTION_PAD;

    private TerminalTabs() {
    }

    public static WidgetGroup tabCard(int x, int y, int w, int h, boolean active, Consumer<ClickData> onClick) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup card = new WidgetGroup(x, y, w, h);
        card.addWidget(new ImageWidget(0, 0, w, h,
                TerminalUI.bevel(active ? tokens.panel() : tokens.panelDisabled())));
        ButtonWidget target = new ButtonWidget(0, 0, w, h, TerminalUI.transparent(), onClick);
        target.setHoverTexture(TerminalUI.hover());
        card.addWidget(target);
        return card;
    }

    public static WidgetGroup actionCard(int x, int y, int h,
                                         Consumer<ClickData> onTheme, Consumer<ClickData> onClose) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup card = new WidgetGroup(x, y, ACTION_CARD_W, h);
        card.addWidget(new ImageWidget(0, 0, ACTION_CARD_W, h, TerminalUI.bevel(tokens.surface())));

        int by = (h - ACTION_BTN) / 2;
        ButtonWidget theme = TerminalButton.icon3D(ACTION_PAD, by, ACTION_BTN, TerminalButton.ICON_THEME, onTheme);
        theme.setHoverTooltips(Component.translatable("gui.gtceuterminal.theme_settings").getString());
        card.addWidget(theme);

        ButtonWidget close = TerminalButton.icon3D(ACTION_PAD + ACTION_BTN + ACTION_GAP, by, ACTION_BTN,
                TerminalButton.ICON_CLOSE, onClose);
        close.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        card.addWidget(close);
        return card;
    }
}
