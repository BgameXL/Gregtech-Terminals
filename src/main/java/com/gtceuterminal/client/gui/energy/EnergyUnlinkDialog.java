package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.client.gui.style.*;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.network.CPacketEnergyAnalyzerAction;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class EnergyUnlinkDialog {

    private EnergyUnlinkDialog() {
    }

    static void open(
            WidgetGroup rootGroup,
            int parentW,
            int parentH,
            int machineIdx,
            LinkedMachineData machine,
            Player player
    ) {
        final int DW = 220, DH = 90;
        TerminalTheme tokens = TerminalUI.tokens();

        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(TerminalUI.scrim());
        dialog.setClickClose(true);

        WidgetGroup panel = new WidgetGroup((parentW - DW) / 2, (parentH - DH) / 2, DW, DH);
        panel.setBackground(TerminalUI.cardBG());
        dialog.addWidget(panel);

        WidgetGroup titleBar = new WidgetGroup(0, 0, DW, 18);
        titleBar.setBackground(TerminalUI.bevel(tokens.errorSurface()));
        panel.addWidget(titleBar);

        LabelWidget titleLbl = new LabelWidget(8, 4,
                TerminalText.strip(Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.unlink_dialog.title").getString()));
        titleLbl.setColor(tokens.error());
        titleBar.addWidget(titleLbl);

        String targetName = EnergyDisplayHelper.truncate(machine.getDisplayNameComponent().getString(), 22);
        LabelWidget confirmLbl = new LabelWidget(8, 22,
                TerminalText.strip(Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.unlink_dialog.remove_prompt", targetName).getString()));
        confirmLbl.setColor(tokens.textMuted());
        panel.addWidget(confirmLbl);

        int buttonW = (DW - 20) / 2;
        ButtonWidget unlinkBtn = TerminalButton.text3D(8, 40, buttonW, 18,
                Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.unlink_dialog.unlink").getString(),
                tokens.errorSurface(),
                cd -> {
                    TerminalNetwork.CHANNEL.sendToServer(
                            new CPacketEnergyAnalyzerAction(CPacketEnergyAnalyzerAction.Action.UNLINK, machineIdx, ""));
                    if (player instanceof net.minecraft.client.player.LocalPlayer lp) {
                        lp.closeContainer();
                    }
                });
        panel.addWidget(unlinkBtn);

        ButtonWidget cancelBtn = TerminalButton.text3D(DW / 2 + 2, 40, buttonW, 18,
                Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.unlink_dialog.cancel").getString(),
                tokens.panelDisabled(), cd -> dialog.close());
        panel.addWidget(cancelBtn);

        LabelWidget hint = new LabelWidget(8, DH - 12,
                TerminalText.strip(Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.unlink_dialog.hint_click_outside_cancel").getString()));
        hint.setColor(tokens.textFaint());
        panel.addWidget(hint);
    }

}
