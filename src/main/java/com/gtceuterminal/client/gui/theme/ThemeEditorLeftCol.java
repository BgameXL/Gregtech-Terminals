package com.gtceuterminal.client.gui.theme;

import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.common.theme.ThemePreset;
import com.gtceuterminal.common.theme.bundle.ThemeBundle;
import com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
final class ThemeEditorLeftCol {

    static final int COL_W = 148;

    private static final int C_BORDER = 0xFF3A3A3A;
    private static final int C_PANEL  = 0xFF272727;
    private static final int C_HOVER  = 0x33FFFFFF;

    private ThemeEditorLeftCol() {}

    static WidgetGroup build(
            int x, int y, int colH,
            ItemTheme working,
            List<String> wallpapers,
            Consumer<ThemePreset> onPreset,
            Runnable onWallpaperPrev,
            Runnable onWallpaperNext,
            Runnable onWallpaperClear,
            ImageWidget[] wallpaperThumbRef,
            LabelWidget[] wallpaperLabelRef,
            ImageWidget[] previewRefs
    ) {
        WidgetGroup col = new WidgetGroup(x, y, COL_W, colH);

        col.addWidget(new LabelWidget(0, 0,
                Component.translatable("gui.gtceuterminal.theme_editor.presets").getString()));

        ThemePreset[] presets = ThemePreset.values();
        int swSz = 18, swGap = 4;
        for (int i = 0; i < presets.length; i++) {
            final ThemePreset p = presets[i];
            int sx = (i % 4) * (swSz + swGap);
            int sy = 12 + (i / 4) * (swSz + swGap);
            col.addWidget(new ImageWidget(sx, sy, swSz, swSz,
                    new GuiTextureGroup(new ColorRectTexture(p.accentColor), new ColorBorderTexture(1, 0xFF000000))));
            ButtonWidget btn = new ButtonWidget(sx, sy, swSz, swSz,
                    new ColorRectTexture(0x00000000), cd -> onPreset.accept(p));
            btn.setHoverTexture(new ColorRectTexture(0x55FFFFFF));
            btn.setHoverTooltips(Component.literal(presetLabel(p)));
            col.addWidget(btn);
        }

        int swRows = (presets.length + 3) / 4;
        int indicatorY = 12 + swRows * (swSz + swGap) + 2;

        LabelWidget styleLabel = new LabelWidget(0, indicatorY, () -> {
            if (working.isBundleStyle()) {
                ThemeBundle b = ThemeBundleRegistry.get(working.bundleId);
                String name = b != null ? b.displayName() : working.bundleId;
                return Component.translatable("gui.gtceuterminal.theme_editor.style_label", "§a" + name).getString();
            }
            return Component.translatable("gui.gtceuterminal.theme_editor.style_label",
                    Component.translatable("gui.gtceuterminal.theme_editor.style.dark").getString()).getString();
        });
        styleLabel.setClientSideWidget();
        col.addWidget(styleLabel);

        int sepY = indicatorY + 12;
        col.addWidget(new ImageWidget(0, sepY, COL_W, 1, new ColorRectTexture(C_BORDER)));

        int wy = sepY + 6;
        col.addWidget(new LabelWidget(0, wy,
                Component.translatable("gui.gtceuterminal.theme_editor.wallpaper").getString()));
        wy += 12;

        LabelWidget wallpaperLabel = new LabelWidget(0, wy,
                () -> working.hasWallpaper()
                        ? "§f" + truncate(working.wallpaper, 16)
                        : Component.translatable("gui.gtceuterminal.theme_editor.none").getString());
        wallpaperLabel.setClientSideWidget();
        wallpaperLabelRef[0] = wallpaperLabel;
        col.addWidget(wallpaperLabel);
        wy += 12;

        col.addWidget(makeTextBtn(0,  wy, 20, 14, "§7◀", cd -> onWallpaperPrev.run(), C_PANEL, C_HOVER));
        col.addWidget(makeTextBtn(24, wy, 20, 14, "§7▶", cd -> onWallpaperNext.run(), C_PANEL, C_HOVER));
        col.addWidget(makeTextBtn(48, wy, 34, 14,
                Component.translatable("gui.gtceuterminal.theme_editor.none").getString(),
                cd -> onWallpaperClear.run(), C_PANEL, C_HOVER));
        wy += 18;

        // Slideshow toggle (only for bundle style with slideshow support)
        if (working.isBundleStyle()) {
            ThemeBundle activeBundle = ThemeBundleRegistry.get(working.bundleId);
            if (activeBundle != null && activeBundle.hasSlideshow()) {
                col.addWidget(makeToggleNarrow(0, wy, 70, "Slideshow", working.slideshowMode,
                        v -> working.slideshowMode = v));
                wy += 16;

                LabelWidget sourceLabel = new LabelWidget(0, wy + 2,
                        () -> "§8Source: §f" + working.slideshowSource.label);
                sourceLabel.setClientSideWidget();
                col.addWidget(sourceLabel);

                ButtonWidget sourceBtn = new ButtonWidget(COL_W - 22, wy, 22, 12,
                        new GuiTextureGroup(new ColorRectTexture(C_PANEL), new ColorBorderTexture(1, C_BORDER)),
                        cd -> working.slideshowSource =
                                (working.slideshowSource == ItemTheme.SlideshowSource.BUILTIN)
                                        ? ItemTheme.SlideshowSource.CUSTOM
                                        : ItemTheme.SlideshowSource.BUILTIN);
                sourceBtn.setButtonTexture(new TextTexture("§7⇄").setWidth(22).setType(TextTexture.TextType.NORMAL));
                sourceBtn.setHoverTexture(new ColorRectTexture(C_HOVER));
                sourceBtn.setHoverTooltips(Component.literal("§7Toggle: Built-in / Custom wallpapers"));
                col.addWidget(sourceBtn);
                wy += 16;
            }
        }

        int previewH = 52;
        col.addWidget(new LabelWidget(0, wy,
                Component.translatable("gui.gtceuterminal.theme_editor.preview").getString()));
        wy += 10;

        ImageWidget prevBg = new ImageWidget(0, wy, COL_W, previewH, new ColorRectTexture(working.bgColor));
        col.addWidget(prevBg);
        ImageWidget prevHeader = new ImageWidget(0, wy, COL_W, 10, new ColorRectTexture(working.accentColor | 0xFF000000));
        col.addWidget(prevHeader);
        ImageWidget prevPanel1 = new ImageWidget(2, wy + 13, 60, 34, new ColorRectTexture(working.panelColor));
        col.addWidget(prevPanel1);
        ImageWidget prevPanel2 = new ImageWidget(66, wy + 13, COL_W - 68, 34, new ColorRectTexture(working.panelColor));
        col.addWidget(prevPanel2);
        ImageWidget prevAccentBar = new ImageWidget(0, wy + previewH - 3, COL_W, 3,
                new ColorRectTexture(working.accentColor | 0xFF000000));
        col.addWidget(prevAccentBar);

        previewRefs[0] = prevBg;
        previewRefs[1] = prevHeader;
        previewRefs[2] = prevPanel1;
        previewRefs[3] = prevPanel2;
        previewRefs[4] = prevAccentBar;

        wy += previewH + 4;
        int thumbH = Math.min(30, colH - wy - 4);
        if (thumbH > 10) {
            ImageWidget thumb = new ImageWidget(0, wy, COL_W, thumbH, new ColorRectTexture(0xFF0A0A0A));
            col.addWidget(thumb);
            wallpaperThumbRef[0] = thumb;
        }

        return col;
    }

    private static String presetLabel(ThemePreset p) {
        return switch (p) {
            case DEFAULT     -> Component.translatable("gui.gtceuterminal.theme_editor.preset.default").getString();
            case GTCEU_RED   -> Component.translatable("gui.gtceuterminal.theme_editor.preset.gtceu_red").getString();
            case MATRIX      -> Component.translatable("gui.gtceuterminal.theme_editor.preset.matrix").getString();
            case GOLD        -> Component.translatable("gui.gtceuterminal.theme_editor.preset.gold").getString();
            case PURPLE      -> Component.translatable("gui.gtceuterminal.theme_editor.preset.purple").getString();
            case CYAN        -> Component.translatable("gui.gtceuterminal.theme_editor.preset.cyan").getString();
            case ORANGE      -> Component.translatable("gui.gtceuterminal.theme_editor.preset.orange").getString();
            case MONO        -> Component.translatable("gui.gtceuterminal.theme_editor.preset.mono").getString();
            case PITCH_BLACK -> Component.translatable("gui.gtceuterminal.theme_editor.preset.pitch_black").getString();
        };
    }

    private static ButtonWidget makeTextBtn(int x, int y, int w, int h, String text,
            java.util.function.Consumer<com.lowdragmc.lowdraglib.gui.util.ClickData> action,
            int bgColor, int hoverColor) {
        ButtonWidget btn = new ButtonWidget(x, y, w, h, new ColorRectTexture(bgColor), action);
        btn.setButtonTexture(new TextTexture(text).setWidth(w).setType(TextTexture.TextType.NORMAL));
        btn.setHoverTexture(new ColorRectTexture(hoverColor));
        return btn;
    }

    private static WidgetGroup makeToggleNarrow(int x, int y, int width, String label, boolean initial,
            Consumer<Boolean> onChange) {
        WidgetGroup g = new WidgetGroup(x, y, width, 14);
        final boolean[] state = {initial};

        ImageWidget box = new ImageWidget(0, 1, 10, 10,
                new GuiTextureGroup(
                        new ColorRectTexture(state[0] ? 0xFF2E75B6 : 0xFF333333),
                        new ColorBorderTexture(1, C_BORDER)));
        g.addWidget(box);

        LabelWidget tick = new LabelWidget(1, 1, () -> state[0] ? "§f✔" : " ");
        tick.setClientSideWidget();
        g.addWidget(tick);

        g.addWidget(new LabelWidget(14, 2, "§7" + label));

        ButtonWidget btn = new ButtonWidget(0, 0, width, 14, new ColorRectTexture(0x00000000), cd -> {
            state[0] = !state[0];
            box.setImage(new GuiTextureGroup(
                    new ColorRectTexture(state[0] ? 0xFF2E75B6 : 0xFF333333),
                    new ColorBorderTexture(1, C_BORDER)));
            onChange.accept(state[0]);
        });
        btn.setHoverTexture(new ColorRectTexture(C_HOVER));
        g.addWidget(btn);
        return g;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}