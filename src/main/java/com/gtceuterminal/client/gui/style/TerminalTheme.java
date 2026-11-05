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

    public static final TerminalTheme DEFAULT = derive(new ItemTheme());

    public static TerminalTheme derive(ItemTheme it) {
        return new TerminalTheme(
                it.bgColor,
                it.panelColor,
                TerminalPalette.shade(it.panelColor, 0.72f),
                TerminalPalette.withAlpha(it.accentColor, 0xAA),
                TerminalPalette.shade(it.accentColor, 0.55f),
                it.borderColor,
                TerminalPalette.withAlpha(it.borderColor, 0x55),
                TerminalPalette.shade(it.borderColor, 0.25f),
                TerminalPalette.shade(it.textMutedColor, 0.80f),
                it.textMutedColor,
                it.textColor,
                TerminalPalette.HOVER,
                it.successColor,
                TerminalPalette.shade(it.successColor, 0.32f),
                it.warningColor,
                TerminalPalette.shade(it.warningColor, 0.28f),
                it.errorColor,
                TerminalPalette.shade(it.errorColor, 0.28f));
    }
}
