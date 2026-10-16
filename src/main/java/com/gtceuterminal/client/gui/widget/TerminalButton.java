package com.gtceuterminal.client.gui.widget;

import com.gtceuterminal.client.gui.style.TerminalText;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.gtceuterminal.client.gui.style.BevelTexture;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalUI;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class TerminalButton {

    private TerminalButton() {
    }

    private static IGuiTexture glyph(String s, int w) {
        return new TextTexture(TerminalText.strip(s))
                .setColor(TerminalUI.tokens().textStrong())
                .setWidth(w)
                .setType(TextTexture.TextType.NORMAL);
    }

    public static ButtonWidget icon3d(int x, int y, int size, String glyph,
                                      Consumer<ClickData> action) {
        int fill = TerminalUI.tokens().panel();
        IGuiTexture g = glyph(glyph, size);
        ButtonWidget btn = new ButtonWidget(x, y, size, size,
                new GuiTextureGroup(new BevelTexture(fill), g), action);
        btn.setHoverTexture(new GuiTextureGroup(new BevelTexture(Palette.shade(fill, 1.30f)), g));
        return btn;
    }

    public static ButtonWidget text3d(int x, int y, int w, int h, String label,
                                      Consumer<ClickData> action) {
        return text3d(x, y, w, h, label, TerminalUI.tokens().panel(), action);
    }

    public static ButtonWidget text3d(int x, int y, int w, int h, String label, int fill,
                                      Consumer<ClickData> action) {
        IGuiTexture txt = glyph(label, w);
        ButtonWidget btn = new ButtonWidget(x, y, w, h,
                new GuiTextureGroup(new BevelTexture(fill), txt), action);
        btn.setHoverTexture(new GuiTextureGroup(new BevelTexture(Palette.shade(fill, 1.30f)), txt));
        return btn;
    }

    public static ButtonWidget disabled(int x, int y, int w, int h, String label) {
        var tokens = TerminalUI.tokens();
        TextTexture txt = new TextTexture(TerminalText.strip(label), tokens.textFaint())
                .setWidth(w).setType(TextTexture.TextType.NORMAL);
        IGuiTexture normal = new GuiTextureGroup(
                new BevelTexture(tokens.panelDisabled()),
                txt);
        ButtonWidget btn = new ButtonWidget(x, y, w, h, normal, cd -> {
        });
        btn.setHoverTexture(normal);
        btn.setActive(false);
        return btn;
    }

    public static ButtonWidget entry(int x, int y, int w, int h,
                                     int hoverColor,
                                     Consumer<ClickData> action) {
        ButtonWidget btn = new ButtonWidget(x, y, w, h,
                new ColorRectTexture(Palette.CLEAR), action);
        btn.setHoverTexture(new ColorRectTexture(hoverColor));
        return btn;
    }

}
