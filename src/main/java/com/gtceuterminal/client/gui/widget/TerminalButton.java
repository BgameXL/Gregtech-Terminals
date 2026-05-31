package com.gtceuterminal.client.gui.widget;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
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

    private static final String ICON_ROOT = "gtceuterminal:textures/gui/buttons/";

    private TerminalButton() {}

    public static IGuiTexture iconTex(String iconName) {
        return new ResourceTexture(ICON_ROOT + iconName + ".png");
    }

    public static ButtonWidget icon(int x, int y, int size,
                                    String iconName,
                                    int bgColor, int borderColor,
                                    Consumer<ClickData> action) {
        IGuiTexture normal = new GuiTextureGroup(
                new ColorRectTexture(bgColor),
                new ColorBorderTexture(1, borderColor),
                iconTex(iconName));

        IGuiTexture hover = new GuiTextureGroup(
                new ColorRectTexture(bgColor),
                new ColorBorderTexture(1, C_WHITE),
                new ColorRectTexture(HOVER_OVERLAY),
                iconTex(iconName));

        ButtonWidget btn = new ButtonWidget(x, y, size, size, normal, action);
        btn.setHoverTexture(hover);
        return btn;
    }

    public static ButtonWidget icon(int x, int y,
                                    String iconName,
                                    int bgColor, int borderColor,
                                    Consumer<ClickData> action) {
        return icon(x, y, 20, iconName, bgColor, borderColor, action);
    }

    public static ButtonWidget ghostIcon(int x, int y, int size,
                                         String iconName,
                                         Consumer<ClickData> action) {
        ButtonWidget btn = new ButtonWidget(x, y, size, size,
                new ColorRectTexture(0x00000000), action);
        btn.setButtonTexture(iconTex(iconName));
        btn.setHoverTexture(new GuiTextureGroup(new ColorRectTexture(HOVER_ICON), iconTex(iconName)));
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
                iconTex("close"));

        IGuiTexture hover = new GuiTextureGroup(
                new ColorRectTexture(0xFF8B0000),
                new ColorBorderTexture(1, 0xFFFF4444),
                iconTex("close"));

        ButtonWidget btn = new ButtonWidget(x, y, size, size, normal, action);
        btn.setHoverTexture(hover);
        return btn;
    }

    private static String stripFormatting(String s) {
        return s.replaceAll("§.", "");
    }
}
