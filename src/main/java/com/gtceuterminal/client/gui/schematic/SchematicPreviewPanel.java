package com.gtceuterminal.client.gui.schematic;

import com.gtceuterminal.client.gui.widget.SchematicPreviewWidget;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class SchematicPreviewPanel {

    private static final int C_WHITE   = 0xFFFFFFFF;
    private static final int C_GRAY    = 0xFFAAAAAA;
    private static final int C_SUCCESS = 0xFF4CAF50;

    private SchematicPreviewPanel() {}

    static WidgetGroup build(
            int panelX,
            int panelY,
            int panelW,
            int panelH,
            int selectedIndex,
            List<SchematicData> schematics,
            boolean hasClipboard,
            ItemTheme theme,
            Player player
    ) {
        WidgetGroup panel = new WidgetGroup(panelX, panelY, panelW, panelH);
        panel.setBackground(theme.panelTexture());
        populate(panel, panelW, panelH, selectedIndex, schematics, hasClipboard, theme, player);
        return panel;
    }

    static void repopulate(
            WidgetGroup panel,
            int panelW,
            int panelH,
            int selectedIndex,
            List<SchematicData> schematics,
            boolean hasClipboard,
            ItemTheme theme,
            Player player
    ) {
        disposePreviewWidgets(panel);
        panel.clearAllWidgets();
        populate(panel, panelW, panelH, selectedIndex, schematics, hasClipboard, theme, player);
    }

    private static void disposePreviewWidgets(WidgetGroup panel) {
        for (Widget w : panel.widgets) {
            if (w instanceof WidgetGroup group) {
                for (Widget inner : group.widgets) {
                    if (inner instanceof SchematicPreviewWidget preview) {
                        preview.dispose();
                    }
                }
            }
            if (w instanceof SchematicPreviewWidget preview) {
                preview.dispose();
            }
        }
    }

    private static void populate(
            WidgetGroup panel,
            int panelW,
            int panelH,
            int selectedIndex,
            List<SchematicData> schematics,
            boolean hasClipboard,
            ItemTheme theme,
            Player player
    ) {
        int previewW = panelW - 16;
        int infoH    = 72;
        int previewH = panelH - 16 - infoH;

        WidgetGroup previewArea = new WidgetGroup(8, 8, previewW, panelH - 16);
        previewArea.setBackground(theme.backgroundTexture());

        if (selectedIndex >= 0 && selectedIndex < schematics.size()) {
            SchematicData sel = schematics.get(selectedIndex);

            if (player.level().isClientSide) {
                SchematicPreviewWidget preview = new SchematicPreviewWidget(0, 0, previewW, previewH, sel);
                preview.setBackground(new ColorRectTexture(0xFF080808));
                previewArea.addWidget(preview);
            }

            int ty = previewH + 6;
            String displayName = getMultiblockName(sel);
            if (displayName.length() > 28) displayName = displayName.substring(0, 26) + "…";
            LabelWidget nameLbl = new LabelWidget(4, ty, "§f§l" + displayName);
            nameLbl.setTextColor(C_WHITE);
            previewArea.addWidget(nameLbl); ty += 14;

            BlockPos size = sel.getSize();
            String info = Component.translatable(
                    "gui.gtceuterminal.schematic_interface.preview.info",
                    sel.getBlocks().size(), size.getX(), size.getY(), size.getZ()).getString();
            LabelWidget infoLbl = new LabelWidget(4, ty, info);
            infoLbl.setTextColor(C_GRAY);
            previewArea.addWidget(infoLbl); ty += 12;

            LabelWidget zoom = new LabelWidget(4, ty,
                    Component.translatable("gui.gtceuterminal.schematic_interface.preview.zoom_hint").getString());
            zoom.setTextColor(0xFF555555);
            previewArea.addWidget(zoom); ty += 12;

            LabelWidget rot = new LabelWidget(4, ty,
                    Component.translatable("gui.gtceuterminal.schematic_interface.preview.rotation_hint").getString());
            rot.setTextColor(0xFF555555);
            previewArea.addWidget(rot);

        } else if (hasClipboard) {
            LabelWidget clipInfo = new LabelWidget(10, previewH / 2 - 10,
                    Component.translatable("gui.gtceuterminal.schematic_interface.preview.clipboard_content").getString());
            clipInfo.setTextColor(C_SUCCESS);
            previewArea.addWidget(clipInfo);

            LabelWidget hint = new LabelWidget(10, previewH / 2 + 4,
                    Component.translatable("gui.gtceuterminal.schematic_interface.preview.save_to_see").getString());
            hint.setTextColor(C_GRAY);
            previewArea.addWidget(hint);
        } else {
            LabelWidget none = new LabelWidget(previewW / 2 - 40, panelH / 2 - 10,
                    Component.translatable("gui.gtceuterminal.schematic_interface.preview.no_preview").getString());
            none.setTextColor(C_GRAY);
            previewArea.addWidget(none);
        }

        panel.addWidget(previewArea);
    }

    private static String getMultiblockName(SchematicData schematic) {
        if (schematic == null || schematic.getBlocks().isEmpty()) {
            return Component.translatable(
                    "gui.gtceuterminal.schematic_interface.fallback.multiblock_structure").getString();
        }
        for (Map.Entry<BlockPos, BlockState> e : schematic.getBlocks().entrySet()) {
            String id = e.getValue().getBlock().getDescriptionId().toLowerCase();
            if (id.contains("gtceu") && !id.contains("casing") && !id.contains("hatch")
                    && !id.contains("pipe") && !id.contains("coil") && !id.contains("glass")) {
                String n = e.getValue().getBlock().getName().getString();
                return n.length() > 35 ? n.substring(0, 32) + "…" : n;
            }
        }
        String type = schematic.getMultiblockType();
        if (type != null && !type.isEmpty()) {
            type = type.replace("WorkableElectricMultiblockMachine", "Electric Machine")
                    .replace("CoilWorkableElectricMultiblockMachine", "Coil Machine")
                    .replace("ElectricMultiblockMachine", "Electric Machine");
            return type.length() > 35 ? type.substring(0, 32) + "…" : type;
        }
        return Component.translatable(
                "gui.gtceuterminal.schematic_interface.fallback.multiblock_structure").getString();
    }
}