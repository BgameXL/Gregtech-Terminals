package com.gtceuterminal.common.theme;

import java.util.Locale;

public enum ThemePreset {

    DEFAULT(0xFF2E75B6, 0xFF1A1A1A, 0xFF252525, ItemTheme.UiStyle.DARK),
    GTCEU_RED(0xFFC0392B, 0xFF1A0A0A, 0xFF2A1010, ItemTheme.UiStyle.DARK),
    MATRIX(0xFF00FF41, 0xFF0A0F0A, 0xFF0F1A0F, ItemTheme.UiStyle.DARK),
    GOLD(0xFFFFAA00, 0xFF1A1500, 0xFF252000, ItemTheme.UiStyle.DARK),
    PURPLE(0xFF8E44AD, 0xFF120A1A, 0xFF1C1025, ItemTheme.UiStyle.DARK),
    CYAN(0xFF00BCD4, 0xFF001A1E, 0xFF00252A, ItemTheme.UiStyle.DARK),
    ORANGE(0xFFE67E22, 0xFF1A0F00, 0xFF251800, ItemTheme.UiStyle.DARK),
    MONO(0xFFAAAAAA, 0xFF111111, 0xFF1E1E1E, ItemTheme.UiStyle.DARK),
    PITCH_BLACK(0xFF444444, 0xFF000000, 0xFF0A0A0A, ItemTheme.UiStyle.DARK);

    public final int accentColor;
    public final int bgColor;
    public final int panelColor;
    public final ItemTheme.UiStyle style;

    ThemePreset(int accent, int bg, int panel, ItemTheme.UiStyle style) {
        this.accentColor = accent;
        this.bgColor = bg;
        this.panelColor = panel;
        this.style = style;
    }

    public String langKey() {
        return "gui.gtceuterminal.theme_editor.preset." + name().toLowerCase(Locale.ROOT);
    }

    public void applyTo(ItemTheme theme) {
        theme.accentColor = accentColor;
        theme.bgColor = bgColor;
        theme.panelColor = panelColor;
        theme.textColor = ItemTheme.DEFAULT_TEXT;
        theme.textMutedColor = ItemTheme.DEFAULT_TEXT_MUTED;
        theme.borderColor = accentColor;
        theme.successColor = ItemTheme.DEFAULT_SUCCESS;
        theme.warningColor = ItemTheme.DEFAULT_WARNING;
        theme.errorColor = ItemTheme.DEFAULT_ERROR;
        theme.uiStyle = style;
        theme.bundleId = "";
    }
}
