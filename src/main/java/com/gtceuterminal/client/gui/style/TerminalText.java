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
}
