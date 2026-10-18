package com.gtceuterminal.client.gui.widget;

import com.gtceuterminal.client.gui.style.TerminalText;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.texture.TransformTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.gtceuterminal.client.gui.style.BevelTexture;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalUI;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class TerminalButton {

    private TerminalButton() {
    }

    private static final String BTN_TEX = "gtceuterminal:textures/gui/buttons/";
    public static final ResourceTexture ICON_CLOSE = new ResourceTexture(BTN_TEX + "close.png");
    public static final ResourceTexture ICON_THEME = new ResourceTexture(BTN_TEX + "theme.png");
    public static final ResourceTexture ICON_CONFIG = new ResourceTexture(BTN_TEX + "config.png");
    public static final ResourceTexture ICON_ARROW_LEFT = new ResourceTexture(BTN_TEX + "arrow_left.png");
    public static final ResourceTexture ICON_ARROW_RIGHT = new ResourceTexture(BTN_TEX + "arrow_right.png");
    public static final ResourceTexture ICON_RENAME = new ResourceTexture(BTN_TEX + "rename.png");

    private static final int ICON_MARGIN = 1;

    public static ButtonWidget icon3d(int x, int y, int size, IGuiTexture icon,
                                      Consumer<ClickData> action) {
        int fill = TerminalUI.tokens().panel();
        IGuiTexture centered = new IconTexture(icon, Math.max(1, size - ICON_MARGIN * 2));
        ButtonWidget btn = new ButtonWidget(x, y, size, size,
                new GuiTextureGroup(new BevelTexture(fill), centered), action);
        btn.setHoverTexture(new GuiTextureGroup(new BevelTexture(Palette.shade(fill, 1.30f)), centered));
        return btn;
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

    private static final class IconTexture extends TransformTexture {
        private final IGuiTexture icon;
        private final int iconSize;

        private IconTexture(IGuiTexture icon, int iconSize) {
            this.icon = icon;
            this.iconSize = iconSize;
        }

        @Override
        protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY,
                                    float x, float y, int width, int height) {
            float ix = x + (width - iconSize) / 2f;
            float iy = y + (height - iconSize) / 2f;
            icon.draw(graphics, mouseX, mouseY, ix, iy, iconSize, iconSize);
        }
    }

}
