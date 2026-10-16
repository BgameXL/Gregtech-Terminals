package com.gtceuterminal.client.gui.widget;

import com.gtceuterminal.client.ClientEvents;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.common.theme.bundle.ThemeBundle;
import com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TerminalParade {

    private TerminalParade() {
    }

    public static void setup(ItemTheme theme, int width, int height) {
        ClientEvents.clearActiveParade();
        if (!theme.isBundleStyle()) return;
        ThemeBundle bundle = ThemeBundleRegistry.get(theme.bundleId);
        if (bundle == null) return;
        MultiblockParadeWidget parade = bundle.createParadeWidget(0, 0, width, height);
        if (parade == null || parade.isEmpty()) return;
        parade.setGuiCenter(width / 2f, height / 2f);
        ClientEvents.setActiveParade(parade, theme.paradeMode);
    }
}
