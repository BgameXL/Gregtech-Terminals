package com.gtceuterminal.client.gui.style;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BevelTexture implements IGuiTexture {

    private final int fill;
    private final int highlight;
    private final int shadow;
    private final int edge;
    private final boolean inset;

    public BevelTexture(int fill) {
        this(fill, false);
    }

    public BevelTexture(int fill, boolean inset) {
        this(fill, Palette.shade(fill, 1.45f), Palette.shade(fill, 0.62f), Palette.shade(fill, 0.30f), inset);
    }

    public BevelTexture(int fill, int highlight, int shadow, int edge, boolean inset) {
        this.fill = fill;
        this.highlight = highlight;
        this.shadow = shadow;
        this.edge = edge;
        this.inset = inset;
    }

    @Override
    public void draw(GuiGraphics g, int mouseX, int mouseY, float x, float y, int width, int height) {
        int x0 = (int) x;
        int y0 = (int) y;
        int x1 = x0 + width;
        int y1 = y0 + height;
        if (width < 2 || height < 2) {
            g.fill(x0, y0, x1, y1, fill);
            return;
        }

        // Outer edge
        g.fill(x0, y0, x1, y0 + 1, edge);
        g.fill(x0, y1 - 1, x1, y1, edge);
        g.fill(x0, y0, x0 + 1, y1, edge);
        g.fill(x1 - 1, y0, x1, y1, edge);

        // Interior fill
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);

        // Raised lights top + left and shadows bottom + right; inset swaps them for a carved look
        int topLeft = inset ? shadow : highlight;
        int bottomRight = inset ? highlight : shadow;
        g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, topLeft);
        g.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, topLeft);
        g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, bottomRight);
        g.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, bottomRight);
    }
}
