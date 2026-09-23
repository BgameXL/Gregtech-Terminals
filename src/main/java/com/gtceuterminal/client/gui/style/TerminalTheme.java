package com.gtceuterminal.client.gui.style;

import com.gtceuterminal.common.theme.ItemTheme;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record TerminalTheme(
        int surface,
        int panel,
        int panelDisabled,
        int accentSoft,
        int action,
        int border,
        int borderSubtle,
        int borderDark,
        int textFaint,
        int textMuted,
        int textStrong,
        int hover,
        int success,
        int successSurface,
        int warning,
        int warningSurface,
        int error,
        int errorSurface
) {

    public static TerminalTheme derive(ItemTheme it) {
        return new TerminalTheme(
                it.bgColor,
                it.panelColor,
                Palette.shade(it.panelColor, 0.72f),
                Palette.withAlpha(it.accentColor, 0xAA),
                Palette.shade(it.accentColor, 0.55f),
                it.borderColor,
                Palette.withAlpha(it.borderColor, 0x55),
                Palette.shade(it.borderColor, 0.25f),
                Palette.shade(it.textMutedColor, 0.80f),
                it.textMutedColor,
                it.textColor,
                Palette.HOVER,
                it.successColor,
                Palette.shade(it.successColor, 0.32f),
                it.warningColor,
                Palette.shade(it.warningColor, 0.28f),
                it.errorColor,
                Palette.shade(it.errorColor, 0.28f));
    }

    public static final TerminalTheme DEFAULT = derive(new ItemTheme());
}
