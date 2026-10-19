package com.gtceuterminal.client.gui.widget;

import com.gtceuterminal.client.gui.style.BevelTexture;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class TerminalTabs {

    private TerminalTabs() {
    }

    public static WidgetGroup tabCard(int x, int y, int w, int h, boolean active, Consumer<ClickData> onClick) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup card = new WidgetGroup(x, y, w, h);
        card.addWidget(new ImageWidget(0, 0, w, h,
                new BevelTexture(active ? tokens.panel() : tokens.panelDisabled())));
        ButtonWidget target = new ButtonWidget(0, 0, w, h, new ColorRectTexture(Palette.CLEAR), onClick);
        target.setHoverTexture(new ColorRectTexture(tokens.hover()));
        card.addWidget(target);
        return card;
    }
}
