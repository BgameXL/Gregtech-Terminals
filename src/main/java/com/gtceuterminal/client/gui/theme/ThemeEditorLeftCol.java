package com.gtceuterminal.client.gui.theme;

import com.gtceuterminal.common.theme.ThemePreset;
import com.gtceuterminal.common.theme.bundle.ThemeBundle;
import com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
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

    private static final int C_BG     = 0xFF111111;
    private static final int C_BORDER = 0xFF3A3A3A;
    private static final int C_PANEL  = 0xFF272727;
    private static final int C_HOVER  = 0x33FFFFFF;
    private static final int C_SEL_BG = 0xFF1A0A2A;

    private ThemeEditorLeftCol() {}

    static WidgetGroup build(
            int x, int y, int colH,
            String activeBundleId,
            Consumer<ThemePreset> onPreset,
            Consumer<ThemeBundle> onBundleSelect,
            ImageWidget[] bundleCardBgs
    ) {
        WidgetGroup col = new WidgetGroup(x, y, COL_W, colH);
        col.setBackground(new ColorRectTexture(C_BG));

        int cy = 6;

        col.addWidget(new LabelWidget(6, cy,
                Component.translatable("gui.gtceuterminal.theme_editor.color_presets").getString()));
        cy += 12;

        ThemePreset[] presets = ThemePreset.values();
        int btnH = 14, hGap = 3, vGap = 3;
        int curX = 6, curRowY = cy;

        for (ThemePreset p : presets) {
            int btnW = Math.max(34, p.label.length() * 5 + 10);
            if (curX + btnW > COL_W - 6 && curX > 6) {
                curX    = 6;
                curRowY += btnH + vGap;
            }
            final ThemePreset preset = p;
            int[] pCols = presetColors(p);

            col.addWidget(new ImageWidget(curX, curRowY, btnW, btnH,
                    new GuiTextureGroup(new ColorRectTexture(pCols[0]),
                                        new ColorBorderTexture(1, pCols[1]))));

            ButtonWidget btn = new ButtonWidget(curX, curRowY, btnW, btnH,
                    new ColorRectTexture(0x00000000), cd -> onPreset.accept(preset));
            btn.setButtonTexture(new TextTexture(presetLabel(p))
                    .setWidth(btnW).setType(TextTexture.TextType.NORMAL));
            btn.setHoverTexture(new ColorRectTexture(C_HOVER));
            col.addWidget(btn);

            curX += btnW + hGap;
        }
        cy = curRowY + btnH + 8;

        col.addWidget(new ImageWidget(6, cy, COL_W - 12, 1, new ColorRectTexture(C_BORDER)));
        cy += 6;

        col.addWidget(new LabelWidget(6, cy,
                Component.translatable("gui.gtceuterminal.theme_editor.bundle_themes").getString()));
        cy += 12;

        List<ThemeBundle> bundles = ThemeBundleRegistry.all().stream()
                .filter(ThemeBundle::isAvailable)
                .collect(Collectors.toList());

        if (bundleCardBgs.length < bundles.size())
            throw new IllegalArgumentException("bundleCardBgs must be at least " + bundles.size());

        int cardH = 32, cardGap = 3;
        int totalCardsH = bundles.isEmpty() ? 20 : bundles.size() * (cardH + cardGap);
        int scrollH     = colH - cy - 4;

        DraggableScrollableWidgetGroup bundleScroll =
                new DraggableScrollableWidgetGroup(0, cy, COL_W, scrollH);
        bundleScroll.setYScrollBarWidth(4);
        bundleScroll.setYBarStyle(new ColorRectTexture(0xFF111111), new ColorRectTexture(C_BORDER));

        WidgetGroup bundleList = new WidgetGroup(0, 0, COL_W, Math.max(totalCardsH, scrollH));

        for (int i = 0; i < bundles.size(); i++) {
            final ThemeBundle bundle = bundles.get(i);
            boolean sel = bundle.id().equals(activeBundleId);
            int cardY   = i * (cardH + cardGap);

            ImageWidget cardBg = new ImageWidget(6, cardY, COL_W - 10, cardH,
                    new GuiTextureGroup(
                            new ColorRectTexture(sel ? C_SEL_BG : C_PANEL),
                            new ColorBorderTexture(1, sel ? bundle.accentColor() : C_BORDER)));
            bundleCardBgs[i] = cardBg;
            bundleList.addWidget(cardBg);

            IGuiTexture icon = bundle.iconTexture();
            GuiTextureGroup iconTex = (icon != null)
                    ? new GuiTextureGroup(
                            new ColorRectTexture(bundle.bgColor() | 0xFF000000),
                            icon,
                            new ColorBorderTexture(1, 0xAA000000))
                    : new GuiTextureGroup(
                            new ColorRectTexture(bundle.accentColor() | 0xFF000000),
                            new ColorBorderTexture(1, 0xAA000000));
            int iconSz = 24;
            bundleList.addWidget(new ImageWidget(8, cardY + (cardH - iconSz) / 2, iconSz, iconSz, iconTex));

            bundleList.addWidget(new LabelWidget(8 + iconSz + 6, cardY + (cardH - 8) / 2,
                    "§f" + bundle.displayName()));

            ButtonWidget btn = new ButtonWidget(6, cardY, COL_W - 10, cardH,
                    new ColorRectTexture(0x00000000), cd -> onBundleSelect.accept(bundle));
            btn.setHoverTexture(new ColorRectTexture(C_HOVER));
            btn.setHoverTooltips(
                    Component.literal("§e" + bundle.displayName()),
                    Component.literal(bundle.description()),
                    Component.translatable("gui.gtceuterminal.theme_editor.click_to_apply"));
            bundleList.addWidget(btn);
        }

        if (bundles.isEmpty())
            bundleList.addWidget(new LabelWidget(6, 0,
                    Component.translatable("gui.gtceuterminal.theme_editor.no_modpack_themes").getString()));

        bundleScroll.addWidget(bundleList);
        col.addWidget(bundleScroll);

        return col;
    }

    static void refreshBundleCards(ImageWidget[] cardBgRefs, String activeBundleId) {
        if (cardBgRefs == null) return;
        List<ThemeBundle> bundles = ThemeBundleRegistry.all().stream()
                .filter(ThemeBundle::isAvailable)
                .collect(Collectors.toList());
        for (int i = 0; i < cardBgRefs.length && i < bundles.size(); i++) {
            if (cardBgRefs[i] == null) continue;
            ThemeBundle b = bundles.get(i);
            boolean sel   = b.id().equals(activeBundleId);
            cardBgRefs[i].setImage(new GuiTextureGroup(
                    new ColorRectTexture(sel ? C_SEL_BG : C_PANEL),
                    new ColorBorderTexture(1, sel ? b.accentColor() : C_BORDER)));
        }
    }

    private static int[] presetColors(ThemePreset p) {
        int darkBg = blend(p.bgColor, 0xFF111111, 0.5f);
        int border = (p.accentColor & 0x00FFFFFF) | 0xAA000000;
        return new int[]{ darkBg, border };
    }

    private static String presetLabel(ThemePreset p) {
        return switch (p) {
            case DEFAULT     -> "§fDefault";
            case GTCEU_RED   -> "§cRed";
            case MATRIX      -> "§aMatrix";
            case GOLD        -> "§6Gold";
            case PURPLE      -> "§5Purple";
            case CYAN        -> "§bCyan";
            case ORANGE      -> "§6Orange";
            case MONO        -> "§7Mono";
            case PITCH_BLACK -> "§8Pitch Black";
        };
    }

    private static int blend(int c1, int c2, float t) {
        int r = Math.round(((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = Math.round(((c1 >>  8) & 0xFF) + (((c2 >>  8) & 0xFF) - ((c1 >>  8) & 0xFF)) * t);
        int b = Math.round(( c1        & 0xFF) + (( c2        & 0xFF) - ( c1        & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
