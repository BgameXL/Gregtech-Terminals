package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.client.gui.style.BevelTexture;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.network.CPacketEnergyAnalyzerAction;
import com.gtceuterminal.common.network.TerminalNetwork;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class MachineUnlinkDialog {

    private MachineUnlinkDialog() {
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
        dialog.setBackground(new ColorRectTexture(Palette.SCRIM));
        dialog.setClickClose(true);

        WidgetGroup panel = new WidgetGroup((parentW - DW) / 2, (parentH - DH) / 2, DW, DH);
        panel.setBackground(TerminalUI.cardBg());
        dialog.addWidget(panel);

        WidgetGroup titleBar = new WidgetGroup(0, 0, DW, 18);
        titleBar.setBackground(new BevelTexture(tokens.errorSurface()));
        panel.addWidget(titleBar);

        LabelWidget titleLbl = new LabelWidget(8, 4,
                stripFormatting(Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.unlink_dialog.title").getString()));
        titleLbl.setColor(tokens.error());
        titleBar.addWidget(titleLbl);

        String targetName = EnergyDisplayHelper.truncate(machine.getDisplayNameComponent().getString(), 22);
        LabelWidget confirmLbl = new LabelWidget(8, 22,
                stripFormatting(Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.unlink_dialog.remove_prompt", targetName).getString()));
        confirmLbl.setColor(tokens.textMuted());
        panel.addWidget(confirmLbl);

        int buttonW = (DW - 20) / 2;
        ButtonWidget unlinkBtn = TerminalButton.text3d(8, 40, buttonW, 18,
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

        ButtonWidget cancelBtn = TerminalButton.text3d(DW / 2 + 2, 40, buttonW, 18,
                Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.unlink_dialog.cancel").getString(),
                tokens.panelDisabled(), cd -> dialog.close());
        panel.addWidget(cancelBtn);

        LabelWidget hint = new LabelWidget(8, DH - 12,
                stripFormatting(Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.unlink_dialog.hint_click_outside_cancel").getString()));
        hint.setColor(tokens.textFaint());
        panel.addWidget(hint);
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("§.", "");
    }
}
