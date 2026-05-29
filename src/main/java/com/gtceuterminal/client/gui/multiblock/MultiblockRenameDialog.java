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
        final int DW = 280, DH = 104;

        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(0xA0000000));
        dialog.setClickClose(false);

        if (listScroll != null) listScroll.setActive(false);

        WidgetGroup panel = new WidgetGroup((parentW - DW) / 2, (parentH - DH) / 2, DW, DH);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF1E1E1E),
                ColorPattern.GRAY.borderTexture(-1)));
        dialog.addWidget(panel);

        WidgetGroup titleBar = new WidgetGroup(0, 0, DW, 26);
        titleBar.setBackground(new ColorRectTexture(0xFF2A2A2A));
        panel.addWidget(titleBar);

        LabelWidget titleLbl = new LabelWidget(8, 7,
                Component.translatable("gui.gtceuterminal.multiblock_manager.rename_dialog.title").getString());
        titleLbl.setTextColor(C_GOLD);
        titleBar.addWidget(titleLbl);

        ButtonWidget closeBtn = new ButtonWidget(DW - 20, 5, 14, 14,
                new ColorRectTexture(0x00000000), cd -> {
                    dialog.close();
                    if (listScroll != null) listScroll.setActive(true);
                });
        closeBtn.setButtonTexture(new TextTexture("§c✕").setWidth(14).setType(TextTexture.TextType.NORMAL));
        closeBtn.setHoverTexture(new ColorRectTexture(0x33FF0000));
        titleBar.addWidget(closeBtn);

        LabelWidget sub = new LabelWidget(8, 32, "§7" + truncate(mb.getMachineTypeName(), 38));
        sub.setTextColor(C_GRAY);
        panel.addWidget(sub);

        TextFieldWidget textField = new TextFieldWidget(8, 46, DW - 16, 16, null, s -> {});
        textField.setMaxStringLength(32);
        textField.setBordered(true);
        textField.setCurrentString(mb.getCustomDisplayName());
        panel.addWidget(textField);

        int btnW = (DW - 22) / 2;

        ButtonWidget cancelBtn = new ButtonWidget(8, 74, btnW, 18,
                new ColorRectTexture(0xFF2A2A2A),
                cd -> {
                    dialog.close();
                    if (listScroll != null) listScroll.setActive(true);
                });
        cancelBtn.setButtonTexture(new TextTexture("§7Cancel").setWidth(btnW).setType(TextTexture.TextType.NORMAL));
        cancelBtn.setHoverTexture(new ColorRectTexture(0xFF333333));
        panel.addWidget(cancelBtn);

        ButtonWidget confirmBtn = new ButtonWidget(14 + btnW, 74, btnW, 18,
                new ColorRectTexture(0xFF1A4A1A),
                cd -> {
                    String dimId = LinkedMachineData.dimId(player.level());
                    String name  = textField.getCurrentString().trim();
                    TerminalNetwork.CHANNEL.sendToServer(
                            new CPacketSetCustomMultiblockName(null, mb.posKey(dimId), name, name.isEmpty()));
                    mb.setCustomDisplayName(name.isEmpty() ? null : name);
                    dialog.close();
                    if (listScroll != null) listScroll.setActive(true);
                    onConfirm.run();
                });
        confirmBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.multiblock_manager.rename_dialog.confirm").getString())
                .setWidth(btnW).setType(TextTexture.TextType.NORMAL));
        confirmBtn.setHoverTexture(new ColorRectTexture(0xFF1E6A1E));
        panel.addWidget(confirmBtn);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}