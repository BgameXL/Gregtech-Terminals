package com.gtceuterminal.client.gui.theme;

import com.gtceuterminal.client.gui.widget.RGBSliderWidget;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.IntConsumer;

@OnlyIn(Dist.CLIENT)
final class ThemeEditorRightCol {

    private static final int C_PANEL  = 0xFF272727;
    private static final int C_BORDER = 0xFF3A3A3A;
    private static final int C_HOVER  = 0x33FFFFFF;
    private static final int C_SEL    = 0xFF2A1A4A;
    private static final int C_SEL_BR = 0xFF7A3FBF;

    private static final int[] CHAN_FILL = {0xCCFF4444, 0xCC44FF44, 0xCC4444FF, 0xCCCCCCCC};

    private ThemeEditorRightCol() {}

    static WidgetGroup build(
            int x, int y, int colW, int colH,
            ItemTheme working,
            int[] editTargetHolder,
            int[] channels,
            IntConsumer onChannelChanged,
            RGBSliderWidget[] sliderRefs,
            ImageWidget[] swatchRefs,
            ImageWidget[] tabBgRefs,
            LabelWidget[] hexLabelRef,
            Runnable onWallPrev,
            Runnable onWallNext,
            LabelWidget[] wallLabelRef,
            ImageWidget[] wallThumbRef
    ) {
        WidgetGroup col = new WidgetGroup(x, y, colW, colH);

        String[] tNames = {
                Component.translatable("gui.gtceuterminal.theme_editor.tab.accent").getString(),
                Component.translatable("gui.gtceuterminal.theme_editor.tab.bg").getString(),
                Component.translatable("gui.gtceuterminal.theme_editor.tab.panel").getString()
        };

        int cy = 0;

        int tBtnH = 18;
        int tBtnW = (colW - 8) / 3;
        for (int i = 0; i < 3; i++) {
            final int ti = i;
            int bx  = i * (tBtnW + 4);
            boolean sel = ti == editTargetHolder[0];

            tabBgRefs[i] = new ImageWidget(bx, cy, tBtnW, tBtnH,
                    new GuiTextureGroup(
                            new ColorRectTexture(sel ? C_SEL : C_PANEL),
                            new ColorBorderTexture(1, sel ? C_SEL_BR : C_BORDER)));
            col.addWidget(tabBgRefs[i]);
            col.addWidget(new LabelWidget(bx + 4, cy + (tBtnH - 8) / 2, tNames[i]));

            ButtonWidget btn = new ButtonWidget(bx, cy, tBtnW, tBtnH,
                    new ColorRectTexture(0x00000000), cd -> {
                        editTargetHolder[0] = ti;
                        onChannelChanged.accept(ti);
                    });
            btn.setHoverTexture(new ColorRectTexture(C_HOVER));
            col.addWidget(btn);
        }
        cy += tBtnH + 6;

        int barW = colW - 38;
        String[] chanLabels = {"§cR", "§aG", "§9B", "§7A"};
        for (int c = 0; c < 4; c++) {
            final int ci = c;
            int sy = cy + c * 18;
            col.addWidget(new LabelWidget(0, sy + 3, chanLabels[c]));

            LabelWidget valLbl = new LabelWidget(10 + barW + 2, sy + 3,
                    () -> String.valueOf(channels[ci]));
            valLbl.setClientSideWidget();
            col.addWidget(valLbl);

            RGBSliderWidget slider = new RGBSliderWidget(10, sy, barW, 12,
                    0xFF0A0A0A, CHAN_FILL[ci], 0xFFFFFFFF,
                    () -> channels[ci],
                    val -> { channels[ci] = val; onChannelChanged.accept(-1); });
            sliderRefs[c] = slider;
            col.addWidget(slider);
        }
        cy += 4 * 18 + 4;

        col.addWidget(new LabelWidget(0, cy + 3, "§8hex"));
        int hexX = 22, hexW = 82;
        col.addWidget(new ImageWidget(hexX, cy, hexW, 14,
                new GuiTextureGroup(new ColorRectTexture(0xFF141414), new ColorBorderTexture(1, 0xFF333333))));
        LabelWidget hexLabel = new LabelWidget(hexX + 3, cy + 3, () ->
                "§f#" + String.format("%06X", currentColor(working, editTargetHolder[0]) & 0xFFFFFF));
        hexLabel.setClientSideWidget();
        hexLabelRef[0] = hexLabel;
        col.addWidget(hexLabel);
        cy += 20;

        col.addWidget(new LabelWidget(0, cy, "§8Color swatches"));
        cy += 12;

        int swSz = 22;
        int[] swColors = { working.bgColor, working.panelColor, working.accentColor };
        String[] swNames = { "bg", "panel", "accent" };
        for (int i = 0; i < 3; i++) {
            int sx = i * (swSz + 6);
            ImageWidget sw = new ImageWidget(sx, cy, swSz, swSz,
                    new GuiTextureGroup(new ColorRectTexture(swColors[i]),
                                        new ColorBorderTexture(1, 0xFF555555)));
            swatchRefs[i] = sw;
            col.addWidget(sw);
            col.addWidget(new LabelWidget(sx, cy + swSz + 2, "§8" + swNames[i]));
        }
        cy += swSz + 14;

        col.addWidget(new LabelWidget(0, cy + 2, "§8Wallpaper"));

        col.addWidget(makeSmallBtn(54, cy, 12, 12, "§7◀",
                cd -> onWallPrev.run()));

        LabelWidget wallLabel = new LabelWidget(70, cy + 2, () ->
                working.hasWallpaper()
                        ? "§f" + truncate(working.wallpaper, 12)
                        : Component.translatable("gui.gtceuterminal.theme_editor.none").getString());
        wallLabel.setClientSideWidget();
        wallLabelRef[0] = wallLabel;
        col.addWidget(wallLabel);

        col.addWidget(makeSmallBtn(colW - 14, cy, 12, 12, "§7▶",
                cd -> onWallNext.run()));
        cy += 16;

        int previewH = colH - cy - 2;
        if (previewH > 8) {
            col.addWidget(new ImageWidget(0, cy, colW, 1, new ColorRectTexture(0xFF222222)));
            ImageWidget thumb = new ImageWidget(0, cy + 1, colW, previewH - 1,
                    new ColorRectTexture(0xFF0A0A0A));
            wallThumbRef[0] = thumb;
            col.addWidget(thumb);
        }

        return col;
    }

    private static int currentColor(ItemTheme t, int editTarget) {
        return switch (editTarget) {
            case 1  -> t.bgColor;
            case 2  -> t.panelColor;
            default -> t.accentColor;
        };
    }

    private static ButtonWidget makeSmallBtn(int x, int y, int w, int h, String label,
            java.util.function.Consumer<com.lowdragmc.lowdraglib.gui.util.ClickData> action) {
        ButtonWidget btn = new ButtonWidget(x, y, w, h,
                new GuiTextureGroup(new ColorRectTexture(C_PANEL), new ColorBorderTexture(1, C_BORDER)),
                action);
        btn.setButtonTexture(new TextTexture(label).setWidth(w).setType(TextTexture.TextType.NORMAL));
        btn.setHoverTexture(new ColorRectTexture(0x33FFFFFF));
        return btn;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}