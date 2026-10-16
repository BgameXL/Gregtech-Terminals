package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.client.gui.style.TerminalText;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.common.multiblock.DismantleScanner;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class DismantlerBlockListPanel {

    private static final int ENTRY_H = 24;
    private static final int ENTRY_GAP = 1;
    private static final int COL_H = 28;

    private DismantlerBlockListPanel() {
    }

    static WidgetGroup build(
            int x, int y, int w, int h,
            DismantleScanner.ScanResult scanResult
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup panel = new WidgetGroup(x, y, w, h);
        panel.setBackground(new ColorRectTexture(tokens.surface()));

        WidgetGroup colHeader = new WidgetGroup(0, 0, w, COL_H);
        colHeader.setBackground(new GuiTextureGroup(
                new ColorRectTexture(tokens.panel()),
                new ColorBorderTexture(1, tokens.borderSubtle())));

        if (scanResult != null) {
            int types = scanResult.getBlockCounts().size();
            int total = scanResult.getTotalBlocks();
            LabelWidget blocksLabel = new LabelWidget(8, 9,
                    TerminalText.strip(Component.translatable(
                            "gui.gtceuterminal.dismantler.blocks_to_collect_count", types, total).getString()));
            blocksLabel.setColor(tokens.textMuted());
            colHeader.addWidget(blocksLabel);
        } else {
            LabelWidget blocksLabel = new LabelWidget(8, 9,
                    TerminalText.strip(Component.translatable(
                            "gui.gtceuterminal.dismantler.blocks_to_collect").getString()));
            blocksLabel.setColor(tokens.textMuted());
            colHeader.addWidget(blocksLabel);
        }
        panel.addWidget(colHeader);

        if (scanResult == null || scanResult.getBlockCounts().isEmpty()) {
            LabelWidget none = new LabelWidget(10, COL_H + 12,
                    TerminalText.strip(Component.translatable(
                            "gui.gtceuterminal.dismantler.no_data").getString()));
            none.setColor(tokens.textFaint());
            panel.addWidget(none);
            return panel;
        }

        int scrollH = h - COL_H;
        DraggableScrollableWidgetGroup scroll =
                new DraggableScrollableWidgetGroup(0, COL_H, w, scrollH);
        scroll.setYScrollBarWidth(0);

        int yPos = 2;
        for (Map.Entry<Block, Integer> entry : scanResult.getBlockCounts().entrySet()) {
            scroll.addWidget(buildBlockRow(entry.getKey(), entry.getValue(), yPos, w - 2, tokens));
            yPos += ENTRY_H + ENTRY_GAP;
        }

        panel.addWidget(scroll);
        return panel;
    }

    private static WidgetGroup buildBlockRow(
            Block block, int count, int yPos, int rowW, TerminalTheme tokens
    ) {
        WidgetGroup row = new WidgetGroup(1, yPos, rowW, ENTRY_H);
        row.setBackground(new GuiTextureGroup(
                new ColorRectTexture(tokens.panel()),
                new ColorBorderTexture(1, tokens.borderSubtle())));

        WidgetGroup slot = new WidgetGroup(6, 2, 20, 20);
        slot.setBackground(new GuiTextureGroup(
                new ColorRectTexture(tokens.panelDisabled()),
                new ColorBorderTexture(1, tokens.borderSubtle())));
        ItemStack blockStack = new ItemStack(block);
        if (!blockStack.isEmpty()) {
            slot.addWidget(new ImageWidget(1, 1, 18, 18, new ItemStackTexture(blockStack)));
        }
        row.addWidget(slot);

        String blockName = block.getName().getString();
        String countText = "×" + count;
        int countW = Minecraft.getInstance().font.width(countText);
        LabelWidget nameLabel = new LabelWidget(34, 7,
                fitText(blockName, rowW - 34 - countW - 18));
        nameLabel.setColor(tokens.textStrong());
        row.addWidget(nameLabel);

        LabelWidget countLabel = new LabelWidget(rowW - countW - 8, 7, countText);
        countLabel.setColor(tokens.textMuted());
        row.addWidget(countLabel);

        return row;
    }

    private static String fitText(String text, int maxWidth) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        return font.substrByWidth(Component.literal(text),
                Math.max(0, maxWidth - font.width(ellipsis))).getString() + ellipsis;
    }

}
