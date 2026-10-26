package com.gtceuterminal.client.gui.style;

import com.gtceuterminal.common.theme.ItemTheme;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class Palette {

    public static final int BLACK = 0xFF000000;
    public static final int N0 = 0xFF0A0A0A;
    public static final int N1 = 0xFF111111;
    public static final int N2 = 0xFF1A1A1A;
    public static final int N3 = 0xFF222222;
    public static final int N4 = 0xFF2A2A2A;
    public static final int N5 = 0xFF333333;
    public static final int N6 = 0xFF3A3A3A;
    public static final int N7 = 0xFF444444;
    public static final int N8 = 0xFF555555;
    public static final int N9 = 0xFF666666;
    public static final int N10 = 0xFF888888;
    public static final int N11 = 0xFFAAAAAA;
    public static final int N12 = 0xFFDDDDDD;
    public static final int N13 = 0xFFFFFFFF;
    public static final int CLEAR = 0x00000000;
    public static final int HOVER = 0x40FFFFFF;
    public static final int SCRIM = 0xA0000000;
    public static final int OK = ItemTheme.DEFAULT_SUCCESS;
    public static final int WARN = ItemTheme.DEFAULT_WARNING;
    public static final int ERROR = ItemTheme.DEFAULT_ERROR;
    public static final int RED_CHANNEL = 0xCCFF4444;
    public static final int GREEN_CHANNEL = 0xCC44FF44;
    public static final int BLUE_CHANNEL = 0xCC4444FF;
    public static final int ALPHA_CHANNEL = 0xCCCCCCCC;
    private Palette() {
    }

    public static int shade(int argb, float f) {
        int a = argb >>> 24;
        int r = clamp(Math.round(((argb >> 16) & 0xFF) * f));
        int g = clamp(Math.round(((argb >> 8) & 0xFF) * f));
        int b = clamp(Math.round((argb & 0xFF) * f));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static int blend(int first, int second, float amount) {
        int r = Math.round(((first >> 16) & 0xFF)
                + (((second >> 16) & 0xFF) - ((first >> 16) & 0xFF)) * amount);
        int g = Math.round(((first >> 8) & 0xFF)
                + (((second >> 8) & 0xFF) - ((first >> 8) & 0xFF)) * amount);
        int b = Math.round((first & 0xFF) + ((second & 0xFF) - (first & 0xFF)) * amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }
}
