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

    public BevelTexture(int fill) {
        this(fill, Palette.shade(fill, 1.45f), Palette.shade(fill, 0.62f), Palette.shade(fill, 0.30f));
    }

    public BevelTexture(int fill, int highlight, int shadow, int edge) {
        this.fill = fill;
        this.highlight = highlight;
        this.shadow = shadow;
        this.edge = edge;
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

        // Outer edge (1px, all four sides).
        g.fill(x0, y0, x1, y0 + 1, edge);
        g.fill(x0, y1 - 1, x1, y1, edge);
        g.fill(x0, y0, x0 + 1, y1, edge);
        g.fill(x1 - 1, y0, x1, y1, edge);

        // Interior fill.
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);

        // Inner highlight: top + left.
        g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, highlight);
        g.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, highlight);

        // Inner shadow: bottom + right.
        g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, shadow);
        g.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, shadow);
    }
}
