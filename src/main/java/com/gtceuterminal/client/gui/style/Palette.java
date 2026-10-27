package com.gtceuterminal.client.gui.style;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class Palette {

    public static final int BLACK = 0xFF000000;
    public static final int CLEAR = 0x00000000;
    public static final int HOVER = 0x40FFFFFF;
    public static final int SCRIM = 0xA0000000;
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

    public static boolean isLight(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 > 140;
    }

    public static int contrast(int bg, int light, int dark) {
        return isLight(bg) ? dark : light;
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
