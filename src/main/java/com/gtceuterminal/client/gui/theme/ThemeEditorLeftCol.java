package com.gtceuterminal.client.gui.theme;

import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.theme.ThemePreset;
import com.gtceuterminal.common.theme.bundle.ThemeBundle;
import com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
final class ThemeEditorLeftCol {

    static final int COL_W = 190;

    private static final int PAD = 6;
    private static final int PRESET_H = 16;
    private static final int CARD_H = 36;
    private static final int CARD_GAP = 4;

    private ThemeEditorLeftCol() {
    }

    static WidgetGroup buildColorPresets(
            int x, int y, int colH,
            Consumer<ThemePreset> onPreset
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup col = new WidgetGroup(x, y, COL_W, colH);
        col.setBackground(TerminalUI.bevel(tokens.surface()));

        LabelWidget title = new LabelWidget(PAD, 8,
                Component.translatable("gui.gtceuterminal.theme_editor.color_presets").getString());
        title.setColor(tokens.textMuted());
        col.addWidget(title);

        int curX = PAD;
        int curY = 24;
        for (ThemePreset preset : ThemePreset.values()) {
            int btnW = Math.max(42, preset.label.length() * 5 + 12);
            if (curX + btnW > COL_W - PAD && curX > PAD) {
                curX = PAD;
                curY += PRESET_H + 4;
            }
            int fill = Palette.blend(preset.bgColor, tokens.panel(), 0.35f);
            col.addWidget(TerminalButton.text3d(curX, curY, btnW, PRESET_H,
                    presetLabel(preset), fill, cd -> onPreset.accept(preset)));
            curX += btnW + 4;
        }

        LabelWidget hint = new LabelWidget(PAD, curY + PRESET_H + 12,
                Component.translatable("gui.gtceuterminal.theme_editor.presets_hint").getString());
        hint.setColor(tokens.textFaint());
        col.addWidget(hint);
        return col;
    }

    static WidgetGroup buildBundles(
            int x, int y, int colH,
            String activeBundleId,
            Consumer<ThemeBundle> onBundleSelect,
            ImageWidget[] bundleCardBgs
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup col = new WidgetGroup(x, y, COL_W, colH);
        col.setBackground(TerminalUI.bevel(tokens.surface()));

        LabelWidget title = new LabelWidget(PAD, 8,
                Component.translatable("gui.gtceuterminal.theme_editor.bundle_themes").getString());
        title.setColor(tokens.textMuted());
        col.addWidget(title);

        List<ThemeBundle> bundles = availableBundles();
        if (bundleCardBgs.length < bundles.size()) {
            throw new IllegalArgumentException("bundleCardBgs must be at least " + bundles.size());
        }

        int scrollY = 24;
        DraggableScrollableWidgetGroup scroll =
                new DraggableScrollableWidgetGroup(PAD, scrollY, COL_W - PAD * 2, colH - scrollY - PAD);
        scroll.setYScrollBarWidth(0);

        for (int i = 0; i < bundles.size(); i++) {
            ThemeBundle bundle = bundles.get(i);
            int cardY = 1 + i * (CARD_H + CARD_GAP);
            int cardW = scroll.getSizeWidth() - 2;
            boolean selected = bundle.id().equals(activeBundleId);

            ImageWidget cardBg = new ImageWidget(1, cardY, cardW, CARD_H,
                    bundleCardTexture(bundle, selected));
            bundleCardBgs[i] = cardBg;
            scroll.addWidget(cardBg);

            IGuiTexture icon = bundle.iconTexture();
            IGuiTexture iconTexture = icon != null
                    ? TerminalUI.layers(TerminalUI.color(bundle.bgColor()), icon,
                    TerminalUI.border(Palette.withAlpha(tokens.borderDark(), 0xAA)))
                    : TerminalUI.outlined(bundle.accentColor(), tokens.borderDark());
            scroll.addWidget(new ImageWidget(7, cardY + 6, 24, 24, iconTexture));

            LabelWidget name = new LabelWidget(38, cardY + 14, bundle.displayName());
            name.setColor(tokens.textStrong());
            scroll.addWidget(name);

            ButtonWidget button = new ButtonWidget(1, cardY, cardW, CARD_H,
                    TerminalUI.transparent(), cd -> onBundleSelect.accept(bundle));
            button.setHoverTexture(TerminalUI.hover());
            button.setHoverTooltips(
                    Component.literal("§e" + bundle.displayName()),
                    Component.literal(bundle.description()),
                    Component.translatable("gui.gtceuterminal.theme_editor.click_to_apply"));
            scroll.addWidget(button);
        }

        if (bundles.isEmpty()) {
            LabelWidget empty = new LabelWidget(4, 4,
                    Component.translatable("gui.gtceuterminal.theme_editor.no_modpack_themes").getString());
            empty.setColor(tokens.textFaint());
            scroll.addWidget(empty);
        }

        col.addWidget(scroll);
        return col;
    }

    static void refreshBundleCards(ImageWidget[] cardBgRefs, String activeBundleId) {
        if (cardBgRefs == null) return;
        List<ThemeBundle> bundles = availableBundles();
        for (int i = 0; i < cardBgRefs.length && i < bundles.size(); i++) {
            if (cardBgRefs[i] == null) continue;
            ThemeBundle bundle = bundles.get(i);
            cardBgRefs[i].setImage(bundleCardTexture(bundle, bundle.id().equals(activeBundleId)));
        }
    }

    private static List<ThemeBundle> availableBundles() {
        return ThemeBundleRegistry.all().stream()
                .filter(ThemeBundle::isAvailable)
                .collect(Collectors.toList());
    }

    private static IGuiTexture bundleCardTexture(ThemeBundle bundle, boolean selected) {
        TerminalTheme tokens = TerminalUI.tokens();
        int fill = selected ? Palette.withAlpha(bundle.accentColor(), 0x66) : tokens.panel();
        return TerminalUI.bevel(fill);
    }

    private static String presetLabel(ThemePreset preset) {
        return switch (preset) {
            case DEFAULT -> "§fDefault";
            case GTCEU_RED -> "§cRed";
            case MATRIX -> "§aMatrix";
            case GOLD -> "§6Gold";
            case PURPLE -> "§5Purple";
            case CYAN -> "§bCyan";
            case ORANGE -> "§6Orange";
            case MONO -> "§7Mono";
            case PITCH_BLACK -> "§8Pitch Black";
        };
    }

}
