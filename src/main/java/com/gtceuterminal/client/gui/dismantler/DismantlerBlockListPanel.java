package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.client.gui.style.TerminalText;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.common.multiblock.ComponentGroup;
import com.gtceuterminal.common.multiblock.ComponentGroupRegistry;
import com.gtceuterminal.common.multiblock.DismantleScanner;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
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
        panel.setBackground(TerminalUI.surface());

        WidgetGroup colHeader = new WidgetGroup(0, 0, w, COL_H);
        colHeader.setBackground(TerminalUI.color(tokens.panel()));

        if (scanResult != null) {
            int coverTotal = 0;
            for (ItemStack cover : scanResult.getCoverStacks()) coverTotal += cover.getCount();
            int types = scanResult.getBlockCounts().size() + scanResult.getCoverStacks().size();
            int total = scanResult.getTotalBlocks() + coverTotal;
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

        if (scanResult == null
                || (scanResult.getBlockCounts().isEmpty() && scanResult.getCoverStacks().isEmpty())) {
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
            Block block = entry.getKey();
            ComponentGroup group = ComponentGroupRegistry.detectFromBlock(block);
            String category = group == ComponentGroupRegistry.UNKNOWN ? "" : group.displayName;
            scroll.addWidget(buildRow(new ItemStack(block), block.getName().getString(),
                    category, entry.getValue(), yPos, w, tokens));
            yPos += ENTRY_H + ENTRY_GAP;
        }
        String coverCategory = TerminalText.strip(
                Component.translatable("gui.gtceuterminal.dismantler.cover").getString());
        for (ItemStack cover : scanResult.getCoverStacks()) {
            ItemStack icon = cover.copy();
            icon.setCount(1);
            scroll.addWidget(buildRow(icon, cover.getHoverName().getString(),
                    coverCategory, cover.getCount(), yPos, w, tokens));
            yPos += ENTRY_H + ENTRY_GAP;
        }

        panel.addWidget(scroll);
        return panel;
    }

    private static WidgetGroup buildRow(
            ItemStack iconStack, String name, String category,
            int count, int yPos, int rowW, TerminalTheme tokens
    ) {
        WidgetGroup row = new WidgetGroup(0, yPos, rowW, ENTRY_H);
        row.setBackground(TerminalUI.outlined(tokens.panel(), tokens.accentSoft()));

        WidgetGroup slot = new WidgetGroup(6, 2, 20, 20);
        slot.setBackground(TerminalUI.well(tokens.accentSoft()));
        if (!iconStack.isEmpty()) {
            slot.addWidget(new ImageWidget(1, 1, 18, 18, new ItemStackTexture(iconStack)));
        }
        row.addWidget(slot);

        var font = Minecraft.getInstance().font;
        String countText = " × " + count;
        int countW = font.width(countText);
        LabelWidget countLabel = new LabelWidget(rowW - countW - 8, 7, countText);
        countLabel.setColor(tokens.textMuted());
        row.addWidget(countLabel);

        int nameX = 32;
        int rightLimit = rowW - countW - 14;
        if (!category.isEmpty()) {
            int catW = font.width(category);
            int catX = rightLimit - catW;
            LabelWidget catLabel = new LabelWidget(catX, 7, category);
            catLabel.setColor(tokens.textFaint());
            row.addWidget(catLabel);
            rightLimit = catX - 6;
        }

        LabelWidget nameLabel = new LabelWidget(nameX, 7, fitText(name, rightLimit - nameX));
        nameLabel.setColor(tokens.textStrong());
        row.addWidget(nameLabel);

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
