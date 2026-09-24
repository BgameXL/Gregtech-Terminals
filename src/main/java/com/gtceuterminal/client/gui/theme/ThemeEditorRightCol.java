package com.gtceuterminal.client.gui.theme;

import com.gtceuterminal.client.gui.style.Bevel3DTexture;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.widget.RGBSliderWidget;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

@OnlyIn(Dist.CLIENT)
final class ThemeEditorRightCol {

    private static final int PAD = 8;
    private static final int COLOR_TARGETS = 9;
    private static final int[] CHAN_FILL = {0xCCFF4444, 0xCC44FF44, 0xCC4444FF, 0xCCCCCCCC};

    private ThemeEditorRightCol() {
    }

    static WidgetGroup buildColors(
            int x, int y, int colW, int colH,
            ItemTheme working,
            int[] editTargetHolder,
            int[] channels,
            IntConsumer onChannelChanged,
            RGBSliderWidget[] sliderRefs,
            ImageWidget[] swatchRefs,
            ImageWidget[] tabBgRefs,
            Consumer<String> onHexEntered,
            TextFieldWidget[] hexFieldRef
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup col = new WidgetGroup(x, y, colW, colH);
        col.setBackground(new Bevel3DTexture(tokens.surface()));

        LabelWidget title = new LabelWidget(PAD, 8,
                Component.translatable("gui.gtceuterminal.theme_editor.color_customization").getString());
        title.setColor(tokens.textMuted());
        col.addWidget(title);

        String[] targetNames = {
                Component.translatable("gui.gtceuterminal.theme_editor.tab.accent").getString(),
                Component.translatable("gui.gtceuterminal.theme_editor.tab.bg").getString(),
                Component.translatable("gui.gtceuterminal.theme_editor.tab.panel").getString(),
                Component.translatable("gui.gtceuterminal.theme_editor.tab.text").getString(),
                Component.translatable("gui.gtceuterminal.theme_editor.tab.muted").getString(),
                Component.translatable("gui.gtceuterminal.theme_editor.tab.border").getString(),
                Component.translatable("gui.gtceuterminal.theme_editor.tab.success").getString(),
                Component.translatable("gui.gtceuterminal.theme_editor.tab.warning").getString(),
                Component.translatable("gui.gtceuterminal.theme_editor.tab.error").getString()
        };

        int cy = 24;
        int targetH = 20;
        int targetW = (colW - PAD * 2 - 8) / 3;
        for (int i = 0; i < COLOR_TARGETS; i++) {
            int target = i;
            int bx = PAD + (i % 3) * (targetW + 4);
            int by = cy + (i / 3) * (targetH + 4);

            tabBgRefs[i] = new ImageWidget(bx, by, targetW, targetH,
                    targetTexture(i == editTargetHolder[0]));
            col.addWidget(tabBgRefs[i]);

            LabelWidget label = new LabelWidget(bx + 6, by + 6, targetNames[i]);
            label.setColor(tokens.textStrong());
            col.addWidget(label);

            ButtonWidget button = new ButtonWidget(bx, by, targetW, targetH,
                    new ColorRectTexture(Palette.CLEAR), cd -> {
                editTargetHolder[0] = target;
                onChannelChanged.accept(target);
            });
            button.setHoverTexture(new ColorRectTexture(tokens.hover()));
            col.addWidget(button);
        }
        cy += 3 * (targetH + 4) + 6;

        int barW = colW - PAD * 2 - 40;
        String[] channelLabels = {"§cR", "§aG", "§9B", "§7A"};
        for (int channel = 0; channel < 4; channel++) {
            int channelIndex = channel;
            int sy = cy + channel * 20;
            col.addWidget(new LabelWidget(PAD, sy + 3, channelLabels[channel]));

            LabelWidget value = new LabelWidget(PAD + 12 + barW + 4, sy + 3,
                    () -> String.valueOf(channels[channelIndex]));
            value.setClientSideWidget();
            value.setColor(tokens.textMuted());
            col.addWidget(value);

            RGBSliderWidget slider = new RGBSliderWidget(PAD + 12, sy, barW, 12,
                    tokens.borderDark(), CHAN_FILL[channel], tokens.textStrong(),
                    () -> channels[channelIndex],
                    newValue -> {
                        channels[channelIndex] = newValue;
                        onChannelChanged.accept(-1);
                    });
            sliderRefs[channel] = slider;
            col.addWidget(slider);
        }
        cy += 84;

        LabelWidget hexLabel = new LabelWidget(PAD, cy + 4,
                Component.translatable("gui.gtceuterminal.theme_editor.hex").getString());
        hexLabel.setColor(tokens.textMuted());
        col.addWidget(hexLabel);

        TextFieldWidget hexField = new TextFieldWidget(PAD + 28, cy, 96, 16, null, onHexEntered);
        hexField.setClientSideWidget();
        hexField.setBordered(false);
        hexField.setMaxStringLength(6);
        hexField.setValidator(ThemeEditorRightCol::sanitizeHex);
        hexField.setTextColor(tokens.textStrong());
        hexField.setBackground(new Bevel3DTexture(tokens.panelDisabled()));
        hexField.setHoverTexture(new Bevel3DTexture(tokens.panel()));
        hexField.setCurrentString(String.format("%06X",
                currentColor(working, editTargetHolder[0]) & 0xFFFFFF));
        hexFieldRef[0] = hexField;
        col.addWidget(hexField);
        cy += 26;

        LabelWidget swatchTitle = new LabelWidget(PAD, cy,
                Component.translatable("gui.gtceuterminal.theme_editor.color_swatches").getString());
        swatchTitle.setColor(tokens.textMuted());
        col.addWidget(swatchTitle);
        cy += 14;

        int swatchSize = 20;
        int cellW = (colW - PAD * 2) / 3;
        int[] colors = {
                working.accentColor, working.bgColor, working.panelColor,
                working.textColor, working.textMutedColor, working.borderColor,
                working.successColor, working.warningColor, working.errorColor
        };
        String[] names = {"accent", "bg", "panel", "text", "muted", "border", "success", "warning", "error"};
        for (int i = 0; i < COLOR_TARGETS; i++) {
            int sx = PAD + (i % 3) * cellW;
            int sy = cy + (i / 3) * 26;
            ImageWidget swatch = new ImageWidget(sx, sy, swatchSize, swatchSize,
                    new GuiTextureGroup(new ColorRectTexture(colors[i]),
                            new ColorBorderTexture(1, tokens.border())));
            swatchRefs[i] = swatch;
            col.addWidget(swatch);

            LabelWidget name = new LabelWidget(sx + swatchSize + 4, sy + 6,
                    Component.translatable("gui.gtceuterminal.theme_editor.swatch." + names[i]).getString());
            name.setColor(tokens.textMuted());
            col.addWidget(name);
        }
        return col;
    }

    static WidgetGroup buildBundleOptions(
            int x, int y, int colW, int colH,
            ItemTheme working,
            Runnable onCycleParade,
            ImageWidget[] animBgRef,
            Runnable onToggleSource,
            Runnable onWallPrev,
            Runnable onWallNext,
            LabelWidget[] wallLabelRef,
            ImageWidget[] wallThumbRef
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup col = new WidgetGroup(x, y, colW, colH);
        col.setBackground(new Bevel3DTexture(tokens.surface()));

        LabelWidget title = new LabelWidget(PAD, 8,
                Component.translatable("gui.gtceuterminal.theme_editor.bundle_options").getString());
        title.setColor(tokens.textMuted());
        col.addWidget(title);

        int cy = 26;
        int optionH = 20;
        int animW = 118;
        ImageWidget animBg = new ImageWidget(PAD, cy, animW, optionH,
                animToggleTexture(working.paradeMode != ItemTheme.ParadeMode.NONE));
        animBgRef[0] = animBg;
        col.addWidget(animBg);

        LabelWidget animLabel = new LabelWidget(PAD + 7, cy + 6, () -> paradeLabel(working.paradeMode));
        animLabel.setClientSideWidget();
        animLabel.setColor(tokens.textStrong());
        col.addWidget(animLabel);

        ButtonWidget animButton = new ButtonWidget(PAD, cy, animW, optionH,
                new ColorRectTexture(Palette.CLEAR), cd -> onCycleParade.run());
        animButton.setHoverTexture(new ColorRectTexture(tokens.hover()));
        animButton.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.theme_editor.animations"),
                Component.translatable("gui.gtceuterminal.theme_editor.animations_desc"));
        col.addWidget(animButton);

        int sourceX = PAD + animW + 6;
        int sourceW = 94;
        col.addWidget(new ImageWidget(sourceX, cy, sourceW, optionH,
                new Bevel3DTexture(tokens.panel())));
        LabelWidget sourceLabel = new LabelWidget(sourceX + 7, cy + 6, () ->
                working.slideshowSource == ItemTheme.SlideshowSource.CUSTOM ? "Custom" : "Built-in");
        sourceLabel.setClientSideWidget();
        sourceLabel.setColor(tokens.textStrong());
        col.addWidget(sourceLabel);

        ButtonWidget sourceButton = new ButtonWidget(sourceX, cy, sourceW, optionH,
                new ColorRectTexture(Palette.CLEAR), cd -> onToggleSource.run());
        sourceButton.setHoverTexture(new ColorRectTexture(tokens.hover()));
        sourceButton.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.theme_editor.slideshow_source"),
                Component.translatable("gui.gtceuterminal.theme_editor.slideshow_source_desc1"),
                Component.translatable("gui.gtceuterminal.theme_editor.slideshow_source_desc2"));
        col.addWidget(sourceButton);
        cy += optionH + 12;

        LabelWidget wallpaperTitle = new LabelWidget(PAD, cy + 6,
                Component.translatable("gui.gtceuterminal.theme_editor.wallpaper_header").getString());
        wallpaperTitle.setColor(tokens.textMuted());
        col.addWidget(wallpaperTitle);

        ButtonWidget previous = TerminalButton.icon3d(PAD + 74, cy, 20, "<", cd -> onWallPrev.run());
        previous.setHoverTooltips(Component.translatable(
                "gui.gtceuterminal.theme_editor.wallpaper_prev").getString());
        col.addWidget(previous);

        ButtonWidget next = TerminalButton.icon3d(PAD + 98, cy, 20, ">", cd -> onWallNext.run());
        next.setHoverTooltips(Component.translatable(
                "gui.gtceuterminal.theme_editor.wallpaper_next").getString());
        col.addWidget(next);

        LabelWidget wallLabel = new LabelWidget(PAD + 128, cy + 6, () ->
                working.hasWallpaper()
                        ? truncate(working.wallpaper, Math.max(12, (colW - PAD - 128) / 6))
                        : Component.translatable("gui.gtceuterminal.theme_editor.none").getString());
        wallLabel.setClientSideWidget();
        wallLabel.setColor(tokens.textStrong());
        wallLabelRef[0] = wallLabel;
        col.addWidget(wallLabel);
        cy += 30;

        int previewH = colH - cy - PAD;
        if (previewH > 12) {
            col.addWidget(new ImageWidget(PAD, cy, colW - PAD * 2, previewH,
                    new Bevel3DTexture(tokens.panelDisabled())));
            ImageWidget thumb = new ImageWidget(PAD + 2, cy + 2,
                    colW - PAD * 2 - 4, previewH - 4,
                    new ColorRectTexture(tokens.borderDark()));
            wallThumbRef[0] = thumb;
            col.addWidget(thumb);
        }
        return col;
    }

    static IGuiTexture targetTexture(boolean selected) {
        TerminalTheme tokens = TerminalUI.tokens();
        return new Bevel3DTexture(selected ? tokens.action() : tokens.panel());
    }

    static IGuiTexture animToggleTexture(boolean on) {
        TerminalTheme tokens = TerminalUI.tokens();
        return new Bevel3DTexture(on ? tokens.action() : tokens.panel());
    }

    static void refreshAnimToggle(ImageWidget[] ref, boolean on) {
        if (ref == null || ref[0] == null) return;
        ref[0].setImage(animToggleTexture(on));
    }

    private static String paradeLabel(ItemTheme.ParadeMode mode) {
        return switch (mode) {
            case ORBITAL -> "Anim: Orbit";
            case BOUNCING -> "Anim: Bounce";
            default -> "Anim: Off";
        };
    }

    private static String sanitizeHex(String value) {
        if (value == null) return "";
        StringBuilder sanitized = new StringBuilder(6);
        for (int i = 0; i < value.length() && sanitized.length() < 6; i++) {
            char c = Character.toUpperCase(value.charAt(i));
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) sanitized.append(c);
        }
        return sanitized.toString();
    }

    private static int currentColor(ItemTheme theme, int editTarget) {
        return switch (editTarget) {
            case 1 -> theme.bgColor;
            case 2 -> theme.panelColor;
            case 3 -> theme.textColor;
            case 4 -> theme.textMutedColor;
            case 5 -> theme.borderColor;
            case 6 -> theme.successColor;
            case 7 -> theme.warningColor;
            case 8 -> theme.errorColor;
            default -> theme.accentColor;
        };
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
