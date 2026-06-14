package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.common.config.ItemsConfig;
import com.gtceuterminal.common.multiblock.DismantleScanner;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class DismantlerInfoBar {

    private DismantlerInfoBar() {}

    static WidgetGroup build(
            int x, int y, int w, int h,
            int colorBgMedium,
            int colorBorderDark,
            DismantleScanner.ScanResult scanResult,
            Player player
    ) {
        WidgetGroup bar = new WidgetGroup(x, y, w, h);
        bar.setBackground(new GuiTextureGroup(
                new ColorRectTexture(colorBgMedium),
                new ColorBorderTexture(1, colorBorderDark)));
        int total = 0, collectable = 0, skipped = 0, requiredSlots = 0;
        if (scanResult != null) {
            for (Map.Entry<Block, Integer> e : scanResult.getBlockCounts().entrySet()) {
                total += e.getValue();
                if (ItemsConfig.isDismantlerBlacklisted(e.getKey())) {
                    skipped += e.getValue();
                } else {
                    collectable += e.getValue();
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
                Component.translatable("gui.gtceuterminal.dismantler.bar.total", total).getString());
        totalLabel.setTextColor(0xFFAAAAAA);
        bar.addWidget(totalLabel);
        xPos += 96;

        LabelWidget dot1 = new LabelWidget(xPos, h / 2 - 4, "§8·");
        dot1.setTextColor(0xFF555555);
        bar.addWidget(dot1);
        xPos += 14;

        boolean enough = emptySlots >= requiredSlots;
        boolean partial = emptySlots > 0 && !enough;
        String slotColor = enough ? "§a" : (partial ? "§e" : "§c");
        LabelWidget invLabel = new LabelWidget(xPos, h / 2 - 4,
                Component.translatable("gui.gtceuterminal.dismantler.bar.inventory_space",
                        slotColor, emptySlots, requiredSlots).getString());
        invLabel.setTextColor(0xFFAAAAAA);
        bar.addWidget(invLabel);

        if (skipped > 0) {
            LabelWidget skipLabel = new LabelWidget(w - 82, h / 2 - 4,
                    Component.translatable("gui.gtceuterminal.dismantler.bar.skipped", skipped).getString());
            skipLabel.setTextColor(0xFFAAAAAA);
            bar.addWidget(skipLabel);
        }

        return bar;
    }

    private static int addStat(WidgetGroup parent, int x, int y, int w, int h,
                               String val, String lbl, int valColor) {
        LabelWidget value = new LabelWidget(x, y, val);
        value.setTextColor(valColor);
        parent.addWidget(value);

        LabelWidget label = new LabelWidget(x, y + 14, "§8" + lbl);
        label.setTextColor(0xFF555555);
        parent.addWidget(label);

        return x + w;
    }
}