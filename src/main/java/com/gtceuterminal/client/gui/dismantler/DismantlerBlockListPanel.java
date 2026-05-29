package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.common.multiblock.DismantleScanner;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class DismantlerBlockListPanel {

    private static final int ENTRY_H   = 24;
    private static final int ENTRY_GAP = 1;
    private static final int COL_H     = 28;

    private DismantlerBlockListPanel() {}

    static WidgetGroup build(
            int x, int y, int w, int h,
            int colorBgLight,
            int colorBorderDark,
            DismantleScanner.ScanResult scanResult
    ) {
        WidgetGroup panel = new WidgetGroup(x, y, w, h);
        panel.setBackground(new ColorRectTexture(colorBgLight));

        // Column header
        WidgetGroup colHeader = new WidgetGroup(0, 0, w, COL_H);
        colHeader.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF1E1E1E),
                new ColorBorderTexture(1, 0xFF2A2A2A)));

        if (scanResult != null) {
            int types = scanResult.getBlockCounts().size();
            int total = scanResult.getTotalBlocks();
            LabelWidget blocksLabel = new LabelWidget(8, 9,
                    "§8blocks to collect  §7" + types + " types · " + total + " total");
            blocksLabel.setTextColor(0xFF888888);
            colHeader.addWidget(blocksLabel);
        } else {
            LabelWidget blocksLabel = new LabelWidget(8, 9, "§8blocks to collect");
            blocksLabel.setTextColor(0xFF888888);
            colHeader.addWidget(blocksLabel);
        }
        panel.addWidget(colHeader);

        if (scanResult == null || scanResult.getBlockCounts().isEmpty()) {
            LabelWidget none = new LabelWidget(10, COL_H + 12,
                    "§8No blocks scanned");
            none.setTextColor(0xFF666666);
            panel.addWidget(none);
            return panel;
        }

        int scrollH = h - COL_H;
        DraggableScrollableWidgetGroup scroll =
                new DraggableScrollableWidgetGroup(0, COL_H, w - 8, scrollH);
        scroll.setYScrollBarWidth(6);
        scroll.setYBarStyle(new ColorRectTexture(colorBorderDark), new ColorRectTexture(0xFF555555));

        int yPos = 2;
        for (Map.Entry<Block, Integer> entry : scanResult.getBlockCounts().entrySet()) {
            scroll.addWidget(buildBlockRow(entry.getKey(), entry.getValue(), yPos, w - 10));
            yPos += ENTRY_H + ENTRY_GAP;
        }

        panel.addWidget(scroll);
        return panel;
    }

    private static WidgetGroup buildBlockRow(Block block, int count, int yPos, int rowW) {
        WidgetGroup row = new WidgetGroup(0, yPos, rowW, ENTRY_H);
        row.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF1A1A1A),
                new ColorBorderTexture(1, 0xFF1E1E1E)));

        WidgetGroup slot = new WidgetGroup(6, 2, 20, 20);
        slot.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF111111),
                new ColorBorderTexture(1, 0xFF333333)));
        ItemStack blockStack = new ItemStack(block);
        if (!blockStack.isEmpty()) {
            slot.addWidget(new ImageWidget(1, 1, 18, 18, new ItemStackTexture(blockStack)));
        }
        row.addWidget(slot);

        String blockName = block.getName().getString();
        if (blockName.length() > 30) blockName = blockName.substring(0, 29) + "…";
        LabelWidget nameLabel = new LabelWidget(34, 7, blockName);
        nameLabel.setTextColor(0xFFCCCCCC);
        row.addWidget(nameLabel);

        LabelWidget countLabel = new LabelWidget(rowW - 40, 7, "×" + count);
        countLabel.setTextColor(0xFF888888);
        row.addWidget(countLabel);

        return row;
    }
}