package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.common.multiblock.DismantleScanner;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class DismantlerInfoBar {

    private static final int C_SUCCESS = 0xFF00CC00;
    private static final int C_WARNING = 0xFFFFAA00;
    private static final int C_WHITE   = 0xFFFFFFFF;

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

        int emptySlots = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).isEmpty()) emptySlots++;
        }

        LabelWidget slotsLabel = new LabelWidget(10, 8,
                Component.translatable("gui.gtceuterminal.dismantler.inventory_empty_slots", emptySlots).getString());
        slotsLabel.setTextColor(C_WHITE);
        bar.addWidget(slotsLabel);

        int slotsNeeded = (scanResult != null) ? scanResult.getItemsToRecover().size() : 0;
        boolean hasSpace = emptySlots >= slotsNeeded;

        LabelWidget spaceLabel = new LabelWidget(10, 22,
                Component.translatable(hasSpace
                        ? "gui.gtceuterminal.dismantler.enough_space"
                        : "gui.gtceuterminal.dismantler.warning_not_enough_space").getString());
        spaceLabel.setTextColor(hasSpace ? C_SUCCESS : C_WARNING);
        bar.addWidget(spaceLabel);

        return bar;
    }
}