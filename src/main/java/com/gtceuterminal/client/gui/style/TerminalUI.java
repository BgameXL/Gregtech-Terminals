package com.gtceuterminal.client.gui.style;

import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TerminalUI {

    private static TerminalTheme tokens = TerminalTheme.DEFAULT;

    private TerminalUI() {
    }

    public static void use(ItemTheme theme) {
        tokens = TerminalTheme.derive(theme);
    }

    public static TerminalTheme tokens() {
        return tokens;
    }

    public static IGuiTexture cardBg() {
        return new BevelTexture(tokens.surface());
    }

    public static IGuiTexture cardBg(int fill) {
        return new BevelTexture(fill);
    }
}