package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.client.gui.style.Bevel3DTexture;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.network.CPacketEnergyAnalyzerAction;
import com.gtceuterminal.common.network.TerminalNetwork;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class MachineRenameDialog {

    private MachineRenameDialog() {
    }

    static void open(WidgetGroup rootGroup, int parentW, int parentH, int machineIdx, LinkedMachineData machine) {
        final int DW = 240, DH = 80;
        TerminalTheme tokens = TerminalUI.tokens();

        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(Palette.SCRIM));
        dialog.setClickClose(false);

        WidgetGroup panel = new WidgetGroup((parentW - DW) / 2, (parentH - DH) / 2, DW, DH);
        panel.setBackground(TerminalUI.cardBg());
        dialog.addWidget(panel);

        WidgetGroup titleBar = new WidgetGroup(0, 0, DW, 18);
        titleBar.setBackground(new Bevel3DTexture(tokens.panel()));
        panel.addWidget(titleBar);

        LabelWidget titleLbl = new LabelWidget(8, 4,
                stripFormatting(Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.rename_dialog.title").getString()));
        titleLbl.setTextColor(tokens.textStrong());
        titleBar.addWidget(titleLbl);

        TextFieldWidget textField = new TextFieldWidget(8, 22, DW - 16, 16, null, s -> {
        });
        textField.setMaxStringLength(32);
        textField.setBordered(false);
        textField.setTextColor(tokens.textStrong());
        textField.setBackground(new GuiTextureGroup(
                new ColorRectTexture(tokens.panelDisabled()),
                new ColorBorderTexture(1, tokens.border())));
        textField.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(tokens.panelDisabled()),
                new ColorBorderTexture(1, tokens.textStrong())));
        textField.setCurrentString(machine.getCustomName());
        panel.addWidget(textField);

        int buttonW = (DW - 20) / 2;
        ButtonWidget confirmBtn = TerminalButton.text3d(8, 44, buttonW, 18,
                Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.rename_dialog.confirm").getString(),
                tokens.successSurface(),
                cd -> {
                    String name = textField.getCurrentString().trim();
                    TerminalNetwork.CHANNEL.sendToServer(
                            new CPacketEnergyAnalyzerAction(CPacketEnergyAnalyzerAction.Action.RENAME, machineIdx, name));
                    dialog.close();
                });
        panel.addWidget(confirmBtn);

        ButtonWidget cancelBtn = TerminalButton.text3d(DW / 2 + 2, 44, buttonW, 18,
                Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.rename_dialog.cancel").getString(),
                tokens.panelDisabled(), cd -> dialog.close());
        panel.addWidget(cancelBtn);
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("§.", "");
    }
}
