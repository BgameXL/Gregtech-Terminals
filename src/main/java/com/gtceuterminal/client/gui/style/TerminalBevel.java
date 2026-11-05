package com.gtceuterminal.client.gui.style;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TerminalBevel implements IGuiTexture {

    private final int fill;
    private final int highlight;
    private final int shadow;
    private final boolean inset;

    public TerminalBevel(int fill) {
        this(fill, false);
    }

    public TerminalBevel(int fill, boolean inset) {
        this(fill, TerminalPalette.shade(fill, 1.45f), TerminalPalette.shade(fill, 0.62f), inset);
    }

    public TerminalBevel(int fill, int highlight, int shadow, boolean inset) {
        this.fill = fill;
        this.highlight = highlight;
        this.shadow = shadow;
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

        // Interior fill.
        g.fill(x0, y0, x1, y1, fill);

        // Uniform 1px border, same tone on all sides; lighter raised, darker recessed.
        int edge = inset ? shadow : highlight;
        g.fill(x0, y0, x1, y0 + 1, edge);
        g.fill(x0, y1 - 1, x1, y1, edge);
        g.fill(x0, y0, x0 + 1, y1, edge);
        g.fill(x1 - 1, y0, x1, y1, edge);
    }
}
