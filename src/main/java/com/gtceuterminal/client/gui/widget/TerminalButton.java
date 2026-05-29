package com.gtceuterminal.client.gui.widget;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class TerminalButton {

    private static final int HOVER_OVERLAY  = 0x30FFFFFF;
    private static final int HOVER_GHOST    = 0x22FFFFFF;
    private static final int HOVER_ICON     = 0x33FFFFFF;
    private static final int HOVER_DANGER   = 0x33FF3333;

    private static final int C_WHITE = 0xFFFFFFFF;

    private TerminalButton() {}
    public static ButtonWidget icon(int x, int y, int size,
                                    String glyph, String glyphColor,
                                    int bgColor, int borderColor,
                                    Consumer<ClickData> action) {
        IGuiTexture normal = new GuiTextureGroup(
                new ColorRectTexture(bgColor),
                new ColorBorderTexture(1, borderColor),
                iconGlyph(glyph, glyphColor, size));

        IGuiTexture hover = new GuiTextureGroup(
                new ColorRectTexture(bgColor),
                new ColorBorderTexture(1, C_WHITE),
                new ColorRectTexture(HOVER_OVERLAY),
                iconGlyph(glyph, glyphColor, size));

        ButtonWidget btn = new ButtonWidget(x, y, size, size, normal, action);
        btn.setHoverTexture(hover);
        return btn;
    }

    public static ButtonWidget icon(int x, int y,
                                    String glyph, String glyphColor,
                                    int bgColor, int borderColor,
                                    Consumer<ClickData> action) {
        return icon(x, y, 20, glyph, glyphColor, bgColor, borderColor, action);
    }

    public static ButtonWidget ghostIcon(int x, int y, int size,
                                         String glyph, String glyphColor,
                                         Consumer<ClickData> action) {
        IGuiTexture txt = iconGlyph(glyph, glyphColor, size);

        ButtonWidget btn = new ButtonWidget(x, y, size, size,
                new ColorRectTexture(0x00000000), action);
        btn.setButtonTexture(txt);
        btn.setHoverTexture(new GuiTextureGroup(new ColorRectTexture(HOVER_ICON), txt));
        return btn;
    }

    public static ButtonWidget action(int x, int y, int w, int h,
                                      String label,
                                      int bgColor, int borderColor,
                                      Consumer<ClickData> action) {
        TextTexture txt = new TextTexture(label).setWidth(w)
                .setType(TextTexture.TextType.NORMAL);

        IGuiTexture normal = new GuiTextureGroup(
                new ColorRectTexture(bgColor),
                new ColorBorderTexture(1, borderColor),
                txt);

        IGuiTexture hover = new GuiTextureGroup(
                new ColorRectTexture(bgColor),
                new ColorBorderTexture(1, C_WHITE),
                new ColorRectTexture(HOVER_OVERLAY),
                txt);

        ButtonWidget btn = new ButtonWidget(x, y, w, h, normal, action);
        btn.setHoverTexture(hover);
        return btn;
    }

    public static ButtonWidget danger(int x, int y, int w, int h,
                                      String label,
                                      int bgColor, int borderColor,
                                      Consumer<ClickData> action) {
        TextTexture txt = new TextTexture(label).setWidth(w)
                .setType(TextTexture.TextType.NORMAL);

        IGuiTexture normal = new GuiTextureGroup(
                new ColorRectTexture(bgColor),
                new ColorBorderTexture(1, borderColor),
                txt);

        IGuiTexture hover = new GuiTextureGroup(
                new ColorRectTexture(bgColor),
                new ColorBorderTexture(1, 0xFFFF4444),
                new ColorRectTexture(HOVER_DANGER),
                txt);

        ButtonWidget btn = new ButtonWidget(x, y, w, h, normal, action);
        btn.setHoverTexture(hover);
        return btn;
    }

    public static ButtonWidget disabled(int x, int y, int w, int h, String label) {
        TextTexture txt = new TextTexture("§8" + stripFormatting(label))
                .setWidth(w).setType(TextTexture.TextType.NORMAL);
        IGuiTexture normal = new GuiTextureGroup(
                new ColorRectTexture(0xFF2A2A2A),
                new ColorBorderTexture(1, 0xFF333333),
                txt);
        ButtonWidget btn = new ButtonWidget(x, y, w, h, normal, cd -> {});
        btn.setHoverTexture(normal);
        btn.setActive(false);
        return btn;
    }

    public static ButtonWidget ghost(int x, int y, int w, int h,
                                     String label,
                                     Consumer<ClickData> action) {
        TextTexture txt = new TextTexture(label).setWidth(w)
                .setType(TextTexture.TextType.NORMAL);

        ButtonWidget btn = new ButtonWidget(x, y, w, h,
                new ColorRectTexture(0x00000000), action);
        btn.setButtonTexture(txt);
        btn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(HOVER_GHOST), txt));
        return btn;
    }

    public static ButtonWidget entry(int x, int y, int w, int h,
                                     int hoverColor,
                                     Consumer<ClickData> action) {
        ButtonWidget btn = new ButtonWidget(x, y, w, h,
                new ColorRectTexture(0x00000000), action);
        btn.setHoverTexture(new ColorRectTexture(hoverColor));
        return btn;
    }

    public static ButtonWidget close(int x, int y, int size,
                                     int bgColor, int borderColor,
                                     Consumer<ClickData> action) {
        IGuiTexture normal = new GuiTextureGroup(
                new ColorRectTexture(bgColor),
                new ColorBorderTexture(1, borderColor),
                iconGlyph("✕", "§c", size));

        IGuiTexture hover = new GuiTextureGroup(
                new ColorRectTexture(0xFF8B0000),
                new ColorBorderTexture(1, 0xFFFF4444),
                iconGlyph("✕", "§c", size));

        ButtonWidget btn = new ButtonWidget(x, y, size, size, normal, action);
        btn.setHoverTexture(hover);
        return btn;
    }

    private static IGuiTexture iconGlyph(String glyph, String glyphColor, int size) {
        TextTexture texture = new TextTexture(glyphColor + glyph)
                .setWidth(size)
                .setType(TextTexture.TextType.NORMAL);
        texture.setDropShadow(false);
        texture.scale(iconScale(size));
        return texture;
    }

    private static float iconScale(int size) {
        return Math.max(1.25f, Math.min(1.6f, size / 12.0f));
    }

    private static String stripFormatting(String s) {
        return s.replaceAll("§.", "");
    }
}
