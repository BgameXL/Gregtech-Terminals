package com.gtceuterminal.client.gui.widget;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.client.gui.GuiGraphics;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RGBSliderWidget extends Widget {

    // Visual
    private final int trackColor;
    private final int thumbColor;
    private final int fillColor;

    private final IntSupplier getter;
    private final IntConsumer setter;

    private boolean dragging = false;

    public RGBSliderWidget(int x, int y, int width, int height,
                           int trackColor, int fillColor, int thumbColor,
                           IntSupplier getter, IntConsumer setter) {
        super(x, y, width, height);
        this.trackColor = trackColor;
        this.fillColor  = fillColor;
        this.thumbColor = thumbColor;
        this.getter     = getter;
        this.setter     = setter;
    }

    @Override
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);

        Position pos  = getPosition();
        Size     size = getSize();

        int x = pos.x;
        int y = pos.y;
        int w = size.width;
        int h = size.height;

        int trackH  = 4;
        int trackY  = y + (h - trackH) / 2;
        int thumbW  = 6;
        int thumbH  = h;

        int val     = clamp(getter.getAsInt());
        int fillW   = (int) ((val / 255.0) * (w - thumbW));
        int thumbX  = x + fillW;

        // Track background
        graphics.fill(x, trackY, x + w, trackY + trackH, trackColor);

        if (fillW > 0)
            graphics.fill(x, trackY, x + fillW, trackY + trackH, fillColor);

        // Thumb
        graphics.fill(thumbX,     y,          thumbX + thumbW, y + thumbH,     0xFF000000);
        graphics.fill(thumbX + 1, y + 1,      thumbX + thumbW - 1, y + thumbH - 1, thumbColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOverElement(mouseX, mouseY)) {
            dragging = true;
            applyMouse(mouseX);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            applyMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double delta) {
        if (isMouseOverElement(mouseX, mouseY)) {
            boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
            int step = shift ? 10 : 1;
            int val  = clamp(getter.getAsInt() + (delta > 0 ? step : -step));
            setter.accept(val);
            return true;
        }
        return super.mouseWheelMove(mouseX, mouseY, delta);
    }

    private void applyMouse(double mouseX) {
        int x = getPosition().x;
        int w = getSize().width;
        int thumbW = 6;
        double t = (mouseX - x) / (double) (w - thumbW);
        int val = clamp((int) Math.round(t * 255));
        setter.accept(val);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}