package com.gtceuterminal.client.gui.schematic;

import com.gtceuterminal.client.gui.widget.SchematicPreviewWidget;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.common.data.SchematicData;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.client.Minecraft;
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

    private SchematicPreviewPanel() {
    }

    static void repopulate(
            WidgetGroup panel,
            int panelW,
            int panelH,
            int selectedIndex,
            List<SchematicData> schematics,
            boolean hasClipboard,
            Player player
    ) {
        disposePreviewWidgets(panel);
        panel.clearAllWidgets();
        populate(panel, panelW, panelH, selectedIndex, schematics, hasClipboard, player);
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
            Player player
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        int previewW = panelW - 16;
        int infoH = 72;
        int previewH = panelH - 16 - infoH;

        WidgetGroup previewArea = new WidgetGroup(8, 8, previewW, panelH - 16);
        previewArea.setBackground(new ColorRectTexture(tokens.panel()));

        if (selectedIndex >= 0 && selectedIndex < schematics.size()) {
            SchematicData sel = schematics.get(selectedIndex);

            if (player.level().isClientSide) {
                SchematicPreviewWidget preview = new SchematicPreviewWidget(0, 0, previewW, previewH, sel);
                preview.setBackground(new ColorRectTexture(tokens.panelDisabled()));
                previewArea.addWidget(preview);
            }

            int ty = previewH + 6;
            String displayName = getMultiblockName(sel);
            LabelWidget nameLbl = new LabelWidget(4, ty, fitText(displayName, previewW - 8));
            nameLbl.setTextColor(tokens.textStrong());
            previewArea.addWidget(nameLbl);
            ty += 14;

            BlockPos size = sel.getSize();
            String info = Component.translatable(
                    "gui.gtceuterminal.schematic_interface.preview.info",
                    sel.getBlocks().size(), size.getX(), size.getY(), size.getZ()).getString();
            LabelWidget infoLbl = new LabelWidget(4, ty, stripFormatting(info));
            infoLbl.setTextColor(tokens.textMuted());
            previewArea.addWidget(infoLbl);
            ty += 12;

            LabelWidget zoom = new LabelWidget(4, ty,
                    stripFormatting(Component.translatable(
                            "gui.gtceuterminal.schematic_interface.preview.zoom_hint").getString()));
            zoom.setTextColor(tokens.textFaint());
            previewArea.addWidget(zoom);
            ty += 12;

            LabelWidget rot = new LabelWidget(4, ty,
                    stripFormatting(Component.translatable(
                            "gui.gtceuterminal.schematic_interface.preview.rotation_hint").getString()));
            rot.setTextColor(tokens.textFaint());
            previewArea.addWidget(rot);

        } else if (hasClipboard) {
            LabelWidget clipInfo = new LabelWidget(10, previewH / 2 - 10,
                    stripFormatting(Component.translatable(
                            "gui.gtceuterminal.schematic_interface.preview.clipboard_content").getString()));
            clipInfo.setTextColor(tokens.success());
            previewArea.addWidget(clipInfo);

            LabelWidget hint = new LabelWidget(10, previewH / 2 + 4,
                    stripFormatting(Component.translatable(
                            "gui.gtceuterminal.schematic_interface.preview.save_to_see").getString()));
            hint.setTextColor(tokens.textMuted());
            previewArea.addWidget(hint);
        } else {
            String noPreview = stripFormatting(Component.translatable(
                    "gui.gtceuterminal.schematic_interface.preview.no_preview").getString());
            LabelWidget none = new LabelWidget(
                    (previewW - Minecraft.getInstance().font.width(noPreview)) / 2,
                    panelH / 2 - 10, noPreview);
            none.setTextColor(tokens.textMuted());
            previewArea.addWidget(none);
        }

        panel.addWidget(previewArea);
    }

    private static String fitText(String text, int maxWidth) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        return font.substrByWidth(Component.literal(text),
                Math.max(0, maxWidth - font.width(ellipsis))).getString() + ellipsis;
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("§.", "");
    }

    private static String getMultiblockName(SchematicData schematic) {
        if (schematic == null || schematic.getBlocks().isEmpty()) {
            return Component.translatable(
                    "gui.gtceuterminal.schematic_interface.fallback.multiblock_structure").getString();
        }

        for (Map.Entry<BlockPos, BlockState> e : schematic.getBlocks().entrySet()) {
            if (!(e.getValue().getBlock() instanceof com.gregtechceu.gtceu.api.block.MetaMachineBlock mmb)) continue;
            if (!(mmb.definition instanceof com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition)) continue;
            String n = e.getValue().getBlock().getName().getString();
            return n.length() > 35 ? n.substring(0, 32) + "…" : n;
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
