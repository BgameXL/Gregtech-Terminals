package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.widget.SchematicPreviewWidget;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.multiblock.DismantleScanner;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class DismantlerPreviewPanel {

    private DismantlerPreviewPanel() {}

    static WidgetGroup build(
            int x, int y, int w, int h,
            int colorBgLight,
            int colorBorderDark,
            DismantleScanner.ScanResult scanResult,
            BlockPos controllerPos,
            Player player
    ) {
        WidgetGroup panel = new WidgetGroup(x, y, w, h);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(colorBgLight),
                new ColorBorderTexture(1, colorBorderDark)));

        LabelWidget label = new LabelWidget(6, 5,
                Component.translatable("gui.gtceuterminal.dismantler.preview_label").getString());
        label.setTextColor(0xFFAAAAAA);
        panel.addWidget(label);

        if (scanResult != null && scanResult.getTotalBlocks() > 0) {
            SchematicData previewData = buildSchematic(scanResult, controllerPos, player);
            if (previewData != null && !previewData.getBlocks().isEmpty()) {
                panel.addWidget(new SchematicPreviewWidget(6, 20, w - 12, h - 26, previewData));
                return panel;
            }
        }

        LabelWidget noData = new LabelWidget(w / 2 - 30, h / 2,
                Component.translatable("gui.gtceuterminal.dismantler.no_data").getString());
        noData.setTextColor(0xFFFF4444);
        panel.addWidget(noData);

        return panel;
    }

    private static SchematicData buildSchematic(
            DismantleScanner.ScanResult scanResult,
            BlockPos controllerPos,
            Player player
    ) {
        try {
            var level = player.level();
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            Map<BlockPos, CompoundTag> blockEntities = new HashMap<>();

            for (BlockPos worldPos : scanResult.getAllBlocks()) {
                BlockState state = level.getBlockState(worldPos);
                if (state.isAir()) continue;
                BlockPos rel = worldPos.subtract(controllerPos);
                blocks.put(rel, state);
                BlockEntity be = level.getBlockEntity(worldPos);
                if (be != null) {
                    try { blockEntities.put(rel, be.saveWithFullMetadata()); }
                    catch (RuntimeException e) {
                        GTCEUTerminalMod.LOGGER.debug(
                                "DismantlerPreviewPanel: could not serialize BE at {}: {}", worldPos, e.getMessage());
                    }
                }
            }
            return new SchematicData("preview", "", blocks, blockEntities, "south");
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("DismantlerPreviewPanel: failed to build preview schematic", e);
            return null;
        }
    }
}