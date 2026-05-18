package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.network.CPacketEnergyAnalyzerAction;
import com.gtceuterminal.common.network.TerminalNetwork;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class MachineRenameDialog {

    private static final int C_GOLD = 0xFFFFAA00;

    private MachineRenameDialog() {}

    static void open(WidgetGroup rootGroup, int parentW, int parentH, int machineIdx, LinkedMachineData machine) {
        final int DW = 240, DH = 80;

        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(0xA0000000));
        dialog.setClickClose(false);

        WidgetGroup panel = new WidgetGroup((parentW - DW) / 2, (parentH - DH) / 2, DW, DH);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF1E1E1E),
                ColorPattern.GRAY.borderTexture(-1)));
        dialog.addWidget(panel);

        WidgetGroup titleBar = new WidgetGroup(0, 0, DW, 18);
        titleBar.setBackground(new ColorRectTexture(0xFF2A2A2A));
        panel.addWidget(titleBar);

        LabelWidget titleLbl = new LabelWidget(8, 4,
                Component.translatable("gui.gtceuterminal.energy_analyzer.rename_dialog.title").getString());
        titleLbl.setTextColor(C_GOLD);
        titleBar.addWidget(titleLbl);

        TextFieldWidget textField = new TextFieldWidget(8, 22, DW - 16, 16, null, s -> {});
        textField.setMaxStringLength(32);
        textField.setBordered(true);
        textField.setCurrentString(machine.getCustomName());
        panel.addWidget(textField);

        ButtonWidget confirmBtn = new ButtonWidget(8, 44, (DW - 20) / 2, 18,
                new ColorRectTexture(0xFF1A4A1A),
                cd -> {
                    String name = textField.getCurrentString().trim();
                    TerminalNetwork.CHANNEL.sendToServer(
                            new CPacketEnergyAnalyzerAction(CPacketEnergyAnalyzerAction.Action.RENAME, machineIdx, name));
                    dialog.close();
                });
        confirmBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.energy_analyzer.rename_dialog.confirm").getString())
                .setWidth((DW - 20) / 2).setType(TextTexture.TextType.NORMAL));
        confirmBtn.setHoverTexture(new ColorRectTexture(0xFF1E6A1E));
        panel.addWidget(confirmBtn);

        ButtonWidget cancelBtn = new ButtonWidget(DW / 2 + 2, 44, (DW - 20) / 2, 18,
                new ColorRectTexture(0xFF2A2A2A), cd -> dialog.close());
        cancelBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.energy_analyzer.rename_dialog.cancel").getString())
                .setWidth((DW - 20) / 2).setType(TextTexture.TextType.NORMAL));
        cancelBtn.setHoverTexture(new ColorRectTexture(0xFF333333));
        panel.addWidget(cancelBtn);
    }
}