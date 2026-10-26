package com.gtceuterminal.client;

import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.common.multiblock.DismantleScanner;

import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.world.level.block.Block;

import java.util.Map;

public class BlockListWidget extends DraggableScrollableWidgetGroup {

    private static final int ROW_HEIGHT = 20;
    private final DismantleScanner.ScanResult scanResult;
    private final TerminalTheme tokens;

    public BlockListWidget(int x, int y, int width, int height,
                           DismantleScanner.ScanResult scanResult) {
        super(x, y, width, height);
        this.scanResult = scanResult;
        this.tokens = TerminalUI.tokens();

        setBackground(TerminalUI.surface());
        setYScrollBarWidth(6);
        setYBarStyle(
                TerminalUI.color(tokens.borderDark()),
                TerminalUI.color(tokens.border())
        );

        buildContent();
    }

    private void buildContent() {
        int yPos = 2;

        Map<Block, Integer> blocks = scanResult.getBlockCounts();

        for (Map.Entry<Block, Integer> entry : blocks.entrySet()) {
            addWidget(createBlockRow(entry.getKey(), entry.getValue(), yPos));
            yPos += ROW_HEIGHT;
        }
    }

    private WidgetGroup createBlockRow(Block block, int count, int yPos) {
        WidgetGroup row = new WidgetGroup(0, yPos, getSize().width - 12, ROW_HEIGHT);

        // Background hover
        row.setBackground(TerminalUI.panel());

        // Block name
        String blockName = block.getName().getString();
        LabelWidget nameLabel = new LabelWidget(5, ROW_HEIGHT / 2 - 4, blockName);
        nameLabel.setColor(tokens.textStrong());
        row.addWidget(nameLabel);

        // Count
        String countText = "§7x" + count;
        LabelWidget countLabel = new LabelWidget(getSize().width - 50, ROW_HEIGHT / 2 - 4, countText);
        countLabel.setColor(tokens.textMuted());
        row.addWidget(countLabel);

        // Separator line
        row.addWidget(new ImageWidget(0, ROW_HEIGHT - 1, getSize().width - 12, 1,
                TerminalUI.divider()));

        return row;
    }
}
