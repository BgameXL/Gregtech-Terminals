package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.client.BlockListWidget;
import com.gtceuterminal.common.multiblock.DismantleScanner;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class DismantlerBlockListPanel {

    private DismantlerBlockListPanel() {}

    static WidgetGroup build(
            int x, int y, int w, int h,
            int colorBgLight,
            int colorBorderDark,
            DismantleScanner.ScanResult scanResult
    ) {
        WidgetGroup panel = new WidgetGroup(x, y, w, h);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(colorBgLight),
                new ColorBorderTexture(1, colorBorderDark)));

        LabelWidget recoverLabel = new LabelWidget(6, 5,
                Component.translatable("gui.gtceuterminal.dismantler.blocks_to_recover").getString());
        recoverLabel.setTextColor(0xFFAAAAAA);
        panel.addWidget(recoverLabel);

        if (scanResult == null) return panel;

        LabelWidget totalLabel = new LabelWidget(6, 18,
                Component.translatable("gui.gtceuterminal.dismantler.total_blocks",
                        scanResult.getTotalBlocks()).getString());
        totalLabel.setTextColor(0xFFFFFFFF);
        panel.addWidget(totalLabel);

        panel.addWidget(new BlockListWidget(6, 34, w - 12, h - 40, scanResult));

        return panel;
    }
}