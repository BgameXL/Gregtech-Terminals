package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.client.gui.style.TerminalText;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.common.config.ItemsConfig;
import com.gtceuterminal.common.multiblock.DismantleScanner;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class DismantlerInfoBar {

    private DismantlerInfoBar() {
    }

    static WidgetGroup build(
            int x, int y, int w, int h,
            DismantleScanner.ScanResult scanResult,
            Player player
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup bar = new WidgetGroup(x, y, w, h);
        bar.setBackground(new ColorRectTexture(tokens.panel()));
        int total = 0, skipped = 0, requiredSlots = 0;
        if (scanResult != null) {
            for (Map.Entry<Block, Integer> e : scanResult.getBlockCounts().entrySet()) {
                total += e.getValue();
                if (ItemsConfig.isDismantlerBlacklisted(e.getKey())) {
                    skipped += e.getValue();
                } else {
                    requiredSlots += (e.getValue() + 63) / 64;
                }
            }
        }

        int emptySlots = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).isEmpty()) emptySlots++;
        }

        int xPos = 12;
        LabelWidget totalLabel = new LabelWidget(xPos, h / 2 - 4,
                TerminalText.strip(Component.translatable(
                        "gui.gtceuterminal.dismantler.bar.total", total).getString()));
        totalLabel.setColor(tokens.textMuted());
        bar.addWidget(totalLabel);
        xPos += 96;

        LabelWidget dot1 = new LabelWidget(xPos, h / 2 - 4, "·");
        dot1.setColor(tokens.textFaint());
        bar.addWidget(dot1);
        xPos += 14;

        boolean enough = emptySlots >= requiredSlots;
        boolean partial = emptySlots > 0 && !enough;
        LabelWidget invLabel = new LabelWidget(xPos, h / 2 - 4,
                TerminalText.strip(Component.translatable(
                        "gui.gtceuterminal.dismantler.bar.inventory_space",
                        "", emptySlots, requiredSlots).getString()));
        invLabel.setColor(enough ? tokens.success() : partial ? tokens.warning() : tokens.error());
        bar.addWidget(invLabel);

        if (skipped > 0) {
            LabelWidget skipLabel = new LabelWidget(w - 82, h / 2 - 4,
                    TerminalText.strip(Component.translatable(
                            "gui.gtceuterminal.dismantler.bar.skipped", skipped).getString()));
            skipLabel.setColor(tokens.warning());
            bar.addWidget(skipLabel);
        }

        return bar;
    }

}
