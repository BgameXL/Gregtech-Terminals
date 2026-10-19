package com.gtceuterminal.client.gui.widget;

import com.gtceuterminal.client.gui.style.Palette;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ViewportClippedButton extends ButtonWidget {

    private final DraggableScrollableWidgetGroup viewport;

    public ViewportClippedButton(int x, int y, int w, int h, int hoverColor, DraggableScrollableWidgetGroup viewport, Consumer<ClickData> action) {
        super(x, y, w, h, new ColorRectTexture(Palette.CLEAR), action);
        this.viewport = viewport;
        setHoverTexture(new ColorRectTexture(hoverColor));
    }

    @Override
    public boolean isMouseOverElement(double mouseX, double mouseY) {
        return viewport != null
                && Widget.isMouseOver(
                viewport.getPositionX(), viewport.getPositionY(),
                viewport.getSizeWidth(), viewport.getSizeHeight(),
                mouseX, mouseY)
                && super.isMouseOverElement(mouseX, mouseY);
    }
}