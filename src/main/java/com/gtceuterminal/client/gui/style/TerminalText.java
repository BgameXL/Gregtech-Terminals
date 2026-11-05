package com.gtceuterminal.client.gui.style;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TerminalText {

    private TerminalText() {
    }

    public static String strip(String s) {
        return s.replaceAll("§.", "");
    }

    public static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
