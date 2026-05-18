package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.network.CPacketSetCustomMultiblockName;
import com.gtceuterminal.common.network.TerminalNetwork;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class MultiblockRenameDialog {

    private static final int C_GOLD = 0xFFFFAA00;
    private static final int C_GRAY = 0xFFAAAAAA;

    private MultiblockRenameDialog() {}

    static void open(
            WidgetGroup rootGroup,
            int parentW,
            int parentH,
            MultiblockInfo mb,
            Player player,
            DraggableScrollableWidgetGroup listScroll,
            Runnable onConfirm
    ) {
        final int DW = 260, DH = 92;

        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(0xA0000000));
        dialog.setClickClose(false);

        if (listScroll != null) listScroll.setActive(false);

        WidgetGroup panel = new WidgetGroup((parentW - DW) / 2, (parentH - DH) / 2, DW, DH);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF1E1E1E),
                ColorPattern.GRAY.borderTexture(-1)));
        dialog.addWidget(panel);

        WidgetGroup titleBar = new WidgetGroup(0, 0, DW, 20);
        titleBar.setBackground(new ColorRectTexture(0xFF2A2A2A));
        panel.addWidget(titleBar);

        LabelWidget titleLbl = new LabelWidget(8, 5,
                Component.translatable("gui.gtceuterminal.multiblock_manager.rename_dialog.title").getString());
        titleLbl.setTextColor(C_GOLD);
        titleBar.addWidget(titleLbl);

        LabelWidget sub = new LabelWidget(8, 24, "§7" + truncate(mb.getMachineTypeName(), 35));
        sub.setTextColor(C_GRAY);
        panel.addWidget(sub);

        TextFieldWidget textField = new TextFieldWidget(8, 38, DW - 16, 16, null, s -> {});
        textField.setMaxStringLength(32);
        textField.setBordered(true);
        textField.setCurrentString(mb.getCustomDisplayName());
        panel.addWidget(textField);

        int btnW = (DW - 20) / 2;

        ButtonWidget confirmBtn = new ButtonWidget(8, 60, btnW, 18,
                new ColorRectTexture(0xFF1A4A1A),
                cd -> {
                    String dimId = LinkedMachineData.dimId(player.level());
                    String name  = textField.getCurrentString().trim();
                    TerminalNetwork.CHANNEL.sendToServer(
                            new CPacketSetCustomMultiblockName(null, mb.posKey(dimId), name, false));
                    mb.setCustomDisplayName(name);
                    dialog.close();
                    if (listScroll != null) listScroll.setActive(true);
                    onConfirm.run();
                });
        confirmBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.multiblock_manager.rename_dialog.confirm").getString())
                .setWidth(btnW).setType(TextTexture.TextType.NORMAL));
        confirmBtn.setHoverTexture(new ColorRectTexture(0xFF1E6A1E));
        panel.addWidget(confirmBtn);

        ButtonWidget clearBtn = new ButtonWidget(DW / 2 + 2, 60, btnW, 18,
                new ColorRectTexture(0xFF3A2A2A),
                cd -> {
                    String dimId = LinkedMachineData.dimId(player.level());
                    TerminalNetwork.CHANNEL.sendToServer(
                            new CPacketSetCustomMultiblockName(null, mb.posKey(dimId), "", true));
                    mb.setCustomDisplayName(null);
                    dialog.close();
                    if (listScroll != null) listScroll.setActive(true);
                    onConfirm.run();
                });
        clearBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.multiblock_manager.rename_dialog.clear_name").getString())
                .setWidth(btnW).setType(TextTexture.TextType.NORMAL));
        clearBtn.setHoverTexture(new ColorRectTexture(0xFF5A3A3A));
        panel.addWidget(clearBtn);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}