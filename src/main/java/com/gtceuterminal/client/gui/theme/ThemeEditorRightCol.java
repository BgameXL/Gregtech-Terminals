package com.gtceuterminal.client.gui.theme;

import com.gtceuterminal.client.gui.widget.RGBSliderWidget;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
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
    private static final int C_SEL    = 0xFF3A5A8A;

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
            LabelWidget[] hexLabelRef
    ) {
        WidgetGroup col = new WidgetGroup(x, y, colW, colH);

        col.addWidget(new LabelWidget(0, 0,
                Component.translatable("gui.gtceuterminal.theme_editor.color").getString()));

        String[] tNames = {
                Component.translatable("gui.gtceuterminal.theme_editor.tab.accent").getString(),
                Component.translatable("gui.gtceuterminal.theme_editor.tab.bg").getString(),
                Component.translatable("gui.gtceuterminal.theme_editor.tab.panel").getString()
        };
        int tBtnW = (colW - 4) / 3;

        ImageWidget accentSwatch = new ImageWidget(0, 0, tBtnW - 4, 6, new ColorRectTexture(working.accentColor));
        ImageWidget bgSwatch     = new ImageWidget(0, 0, tBtnW - 4, 6, new ColorRectTexture(working.bgColor));
        ImageWidget panelSwatch  = new ImageWidget(0, 0, tBtnW - 4, 6, new ColorRectTexture(working.panelColor));
        swatchRefs[0] = accentSwatch;
        swatchRefs[1] = bgSwatch;
        swatchRefs[2] = panelSwatch;
        ImageWidget[] swatches = {accentSwatch, bgSwatch, panelSwatch};

        for (int i = 0; i < 3; i++) {
            final int ti = i;
            int bx = i * (tBtnW + 2);

            tabBgRefs[i] = new ImageWidget(bx, 11, tBtnW, 22,
                    new ColorRectTexture(ti == editTargetHolder[0] ? C_SEL : C_PANEL));
            col.addWidget(tabBgRefs[i]);
            col.addWidget(new LabelWidget(bx + 2, 13, "§8" + tNames[i]));

            swatches[i].setSelfPosition(new com.lowdragmc.lowdraglib.utils.Position(bx + 2, 24));
            col.addWidget(swatches[i]);

            ButtonWidget sel = new ButtonWidget(bx, 11, tBtnW, 22,
                    new ColorRectTexture(0x00000000), cd -> {
                editTargetHolder[0] = ti;
                onChannelChanged.accept(ti);
            });
            sel.setHoverTexture(new ColorRectTexture(C_HOVER));
            col.addWidget(sel);
        }

        LabelWidget hexLabel = new LabelWidget(0, 35, () ->
                "§7" + tNames[editTargetHolder[0]] + ": §f#" +
                        String.format("%06X", currentColor(working, editTargetHolder[0]) & 0xFFFFFF));
        hexLabel.setClientSideWidget();
        hexLabelRef[0] = hexLabel;
        col.addWidget(hexLabel);

        int sliderY = 47;
        int barW    = colW - 36;
        String[] chanLabels = {"§cR", "§aG", "§9B", "§7A"};

        for (int c = 0; c < 4; c++) {
            final int ci = c;
            int sy = sliderY + c * 22;
            col.addWidget(new LabelWidget(0, sy + 4, chanLabels[c]));

            LabelWidget valLbl = new LabelWidget(10 + barW + 2, sy + 4, () -> String.valueOf(channels[ci]));
            valLbl.setClientSideWidget();
            col.addWidget(valLbl);

            RGBSliderWidget slider = new RGBSliderWidget(10, sy, barW, 14,
                    0xFF0A0A0A, CHAN_FILL[ci], 0xFFFFFFFF,
                    () -> channels[ci],
                    val -> { channels[ci] = val; onChannelChanged.accept(-1); });
            sliderRefs[c] = slider;
            col.addWidget(slider);
        }

        if (working.isBundleStyle()) {
            int optY = sliderY + 4 * 22 + 6;
            col.addWidget(new ImageWidget(0, optY - 3, colW, 1, new ColorRectTexture(C_BORDER)));
            col.addWidget(new LabelWidget(0, optY, "§7Options"));
            optY += 12;

            col.addWidget(new LabelWidget(0, optY + 2, "§7Animation:"));

            LabelWidget animLabel = new LabelWidget(60, optY + 2, () -> {
                if (working.paradeMode == ItemTheme.ParadeMode.ORBITAL)  return "§f⟳ Orbital";
                if (working.paradeMode == ItemTheme.ParadeMode.BOUNCING) return "§f⬇ Bouncing";
                return "§8✕ Off";
            });
            animLabel.setClientSideWidget();
            col.addWidget(animLabel);

            ButtonWidget animBtn = new ButtonWidget(58, optY, 90, 12,
                    new ColorRectTexture(0x00000000), cd -> {
                if (working.paradeMode == ItemTheme.ParadeMode.ORBITAL)
                    working.paradeMode = ItemTheme.ParadeMode.BOUNCING;
                else if (working.paradeMode == ItemTheme.ParadeMode.BOUNCING)
                    working.paradeMode = ItemTheme.ParadeMode.NONE;
                else
                    working.paradeMode = ItemTheme.ParadeMode.ORBITAL;
            });
            animBtn.setHoverTexture(new ColorRectTexture(C_HOVER));
            animBtn.setHoverTooltips(Component.literal("§7Click to cycle: Orbital → Bouncing → Off"));
            col.addWidget(animBtn);
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
}