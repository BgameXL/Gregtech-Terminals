package com.gtceuterminal.client.gui.widget;

import com.gtceuterminal.client.gui.style.TerminalText;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class TerminalButton {

    private static final String BTN_TEX = "gtceuterminal:textures/gui/buttons/";
    public static final ResourceTexture ICON_CLOSE = TerminalUI.resource(BTN_TEX + "close.png");
    public static final ResourceTexture ICON_THEME = TerminalUI.resource(BTN_TEX + "theme.png");
    public static final ResourceTexture ICON_CONFIG = TerminalUI.resource(BTN_TEX + "config.png");
    public static final ResourceTexture ICON_ARROW_LEFT = TerminalUI.resource(BTN_TEX + "arrow_left.png");
    public static final ResourceTexture ICON_ARROW_RIGHT = TerminalUI.resource(BTN_TEX + "arrow_right.png");
    public static final ResourceTexture ICON_RENAME = TerminalUI.resource(BTN_TEX + "rename.png");
    private static final int ICON_MARGIN = 1;

    private TerminalButton() {
    }

    public static ButtonWidget icon3D(int x, int y, int size, IGuiTexture icon,
                                      Consumer<ClickData> action) {
        int fill = TerminalUI.tokens().panel();
        IGuiTexture centered = new IconTexture(icon, Math.max(1, size - ICON_MARGIN * 2));
        ButtonWidget btn = new ButtonWidget(x, y, size, size,
                TerminalUI.buttonContent(fill, centered), action);
        btn.setHoverTexture(TerminalUI.buttonContentHover(fill, centered));
        return btn;
    }

    private static IGuiTexture glyph(String s, int w) {
        return glyph(s, w, TerminalUI.tokens().textStrong());
    }

    private static IGuiTexture glyph(String s, int w, int color) {
        return TerminalUI.text(TerminalText.strip(s), color, w);
    }

    public static ButtonWidget icon3D(int x, int y, int size, String glyph,
                                      Consumer<ClickData> action) {
        int fill = TerminalUI.tokens().panel();
        IGuiTexture g = glyph(glyph, size);
        ButtonWidget btn = new ButtonWidget(x, y, size, size,
                TerminalUI.buttonContent(fill, g), action);
        btn.setHoverTexture(TerminalUI.buttonContentHover(fill, g));
        return btn;
    }

    public static ButtonWidget text3D(int x, int y, int w, int h, String label,
                                      Consumer<ClickData> action) {
        return text3D(x, y, w, h, label, TerminalUI.tokens().panel(), action);
    }

    public static ButtonWidget text3D(int x, int y, int w, int h, String label, int fill,
                                      Consumer<ClickData> action) {
        return text3D(x, y, w, h, label, fill, TerminalUI.tokens().textStrong(), action);
    }

    public static ButtonWidget text3D(int x, int y, int w, int h, String label, int fill,
                                      int textColor, Consumer<ClickData> action) {
        IGuiTexture txt = glyph(label, w, textColor);
        ButtonWidget btn = new ButtonWidget(x, y, w, h,
                TerminalUI.buttonContent(fill, txt), action);
        btn.setHoverTexture(TerminalUI.buttonContentHover(fill, txt));
        return btn;
    }

    public static ButtonWidget disabled(int x, int y, int w, int h, String label) {
        var tokens = TerminalUI.tokens();
        IGuiTexture normal = TerminalUI.button(
                TerminalText.strip(label), w, tokens.panelDisabled(), tokens.textFaint());
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
                TerminalUI.transparent(), action);
        btn.setHoverTexture(TerminalUI.color(hoverColor));
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
