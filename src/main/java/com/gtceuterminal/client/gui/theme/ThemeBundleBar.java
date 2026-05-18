package com.gtceuterminal.client.gui.theme;

import com.gtceuterminal.common.theme.bundle.ThemeBundle;
import com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
final class ThemeBundleBar {

    static final int BAR_H      = 110;
    static final int CARD_W     = 100;
    static final int CARD_H     = 98;
    static final int CARD_PAD   = 4;

    private static final int C_BG         = 0xFF1A1A1A;
    private static final int C_BORDER     = 0xFF3A3A3A;
    private static final int C_HOVER      = 0x33FFFFFF;
    private static final int C_BUNDLE_SEL  = 0xFF2A4A2A;
    private static final int C_BUNDLE_CARD = 0xFF222222;

    private ThemeBundleBar() {}

    static WidgetGroup build(
            int x, int y, int w,
            String activeBundleId,
            Consumer<ThemeBundle> onSelect,
            ImageWidget[] cardBgRefs
    ) {
        WidgetGroup bar = new WidgetGroup(x, y, w, BAR_H);
        bar.setBackground(new ColorRectTexture(C_BG));
        bar.addWidget(new LabelWidget(CARD_PAD, 3, "§7Modpack Themes"));

        List<ThemeBundle> bundles = ThemeBundleRegistry.all().stream()
                .filter(ThemeBundle::isAvailable)
                .collect(Collectors.toList());

        if (cardBgRefs.length < bundles.size()) {
            throw new IllegalArgumentException("cardBgRefs must be sized to at least " + bundles.size());
        }

        int scrollH    = BAR_H - 14;
        int totalCardsW = bundles.size() * (CARD_W + CARD_PAD) + CARD_PAD;

        DraggableScrollableWidgetGroup scroll =
                new DraggableScrollableWidgetGroup(0, 14, w, scrollH);
        scroll.setXScrollBarHeight(4);
        scroll.setXBarStyle(new ColorRectTexture(0xFF111111), new ColorRectTexture(C_BORDER));

        WidgetGroup cards = new WidgetGroup(0, 0, Math.max(totalCardsW, w), scrollH);

        for (int i = 0; i < bundles.size(); i++) {
            final ThemeBundle bundle = bundles.get(i);
            int cx = CARD_PAD + i * (CARD_W + CARD_PAD);
            int cy = (scrollH - CARD_H) / 2;
            boolean sel = bundle.id().equals(activeBundleId);

            ImageWidget cardBg = new ImageWidget(cx, cy, CARD_W, CARD_H,
                    new GuiTextureGroup(
                            new ColorRectTexture(sel ? C_BUNDLE_SEL : C_BUNDLE_CARD),
                            new ColorBorderTexture(1, sel ? bundle.accentColor() : C_BORDER)));
            cardBgRefs[i] = cardBg;
            cards.addWidget(cardBg);

            int previewH = CARD_W - 2;
            cards.addWidget(new ImageWidget(cx + 1, cy + 1, CARD_W - 2, previewH, bundle.previewTexture()));
            cards.addWidget(new ImageWidget(cx + 1, cy + previewH - 3, CARD_W - 2, 3,
                    new ColorRectTexture(bundle.accentColor() | 0xFF000000)));

            cards.addWidget(new LabelWidget(cx + 3, cy + previewH + 3, "§f" + bundle.displayName()));

            ButtonWidget btn = new ButtonWidget(cx, cy, CARD_W, CARD_H,
                    new ColorRectTexture(0x00000000), cd -> onSelect.accept(bundle));
            btn.setHoverTexture(new ColorRectTexture(C_HOVER));
            btn.setHoverTooltips(
                    Component.literal("§e" + bundle.displayName()),
                    Component.literal(bundle.description()),
                    Component.literal("§7Click to apply"));
            cards.addWidget(btn);
        }

        if (bundles.isEmpty()) {
            cards.addWidget(new LabelWidget(CARD_PAD, (scrollH - 8) / 2, "§8No modpack themes installed"));
        }

        scroll.addWidget(cards);
        bar.addWidget(scroll);
        return bar;
    }

    static void refreshCardBgs(
            ImageWidget[] cardBgRefs,
            String activeBundleId
    ) {
        if (cardBgRefs == null) return;
        List<ThemeBundle> bundles = ThemeBundleRegistry.all().stream()
                .filter(ThemeBundle::isAvailable)
                .collect(Collectors.toList());
        for (int i = 0; i < cardBgRefs.length && i < bundles.size(); i++) {
            if (cardBgRefs[i] == null) continue;
            ThemeBundle b = bundles.get(i);
            boolean sel = b.id().equals(activeBundleId);
            cardBgRefs[i].setImage(new GuiTextureGroup(
                    new ColorRectTexture(sel ? C_BUNDLE_SEL : C_BUNDLE_CARD),
                    new ColorBorderTexture(1, sel ? b.accentColor() : C_BORDER)));
        }
    }
}