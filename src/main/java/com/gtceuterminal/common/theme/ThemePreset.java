package com.gtceuterminal.common.theme;

public enum ThemePreset {

    DEFAULT      ("Default",      0xFF2E75B6, 0xFF1A1A1A, 0xFF252525, ItemTheme.UiStyle.DARK),
    GTCEU_RED    ("GTCEu Red",    0xFFC0392B, 0xFF1A0A0A, 0xFF2A1010, ItemTheme.UiStyle.DARK),
    MATRIX       ("Matrix",       0xFF00FF41, 0xFF0A0F0A, 0xFF0F1A0F, ItemTheme.UiStyle.DARK),
    GOLD         ("Gold",         0xFFFFAA00, 0xFF1A1500, 0xFF252000, ItemTheme.UiStyle.DARK),
    PURPLE       ("Purple",       0xFF8E44AD, 0xFF120A1A, 0xFF1C1025, ItemTheme.UiStyle.DARK),
    CYAN         ("Cyan",         0xFF00BCD4, 0xFF001A1E, 0xFF00252A, ItemTheme.UiStyle.DARK),
    ORANGE       ("Orange",       0xFFE67E22, 0xFF1A0F00, 0xFF251800, ItemTheme.UiStyle.DARK),
    MONO         ("Mono",         0xFFAAAAAA, 0xFF111111, 0xFF1E1E1E, ItemTheme.UiStyle.DARK),
    PITCH_BLACK  ("Pitch Black",  0xFF444444, 0xFF000000, 0xFF0A0A0A, ItemTheme.UiStyle.DARK);

    public final String            label;
    public final int               accentColor;
    public final int               bgColor;
    public final int               panelColor;
    public final ItemTheme.UiStyle style;

    ThemePreset(String label, int accent, int bg, int panel, ItemTheme.UiStyle style) {
        this.label       = label;
        this.accentColor = accent;
        this.bgColor     = bg;
        this.panelColor  = panel;
        this.style       = style;
    }

    public void applyTo(ItemTheme theme) {
        theme.accentColor = accentColor;
        theme.bgColor     = bgColor;
        theme.panelColor  = panelColor;
        theme.uiStyle     = style;
    }
}