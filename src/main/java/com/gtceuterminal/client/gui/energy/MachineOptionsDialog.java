package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.client.gui.style.Bevel3DTexture;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.energy.LinkedMachineData;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class MachineOptionsDialog {

    private MachineOptionsDialog() {
    }

    static void open(
            WidgetGroup rootGroup,
            int parentW,
            int parentH,
            int machineIdx,
            LinkedMachineData machine,
            Player player
    ) {
        final int DW = 220, DH = 110;
        TerminalTheme tokens = TerminalUI.tokens();
        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(Palette.SCRIM));
        dialog.setClickClose(true);

        WidgetGroup panel = new WidgetGroup((parentW - DW) / 2, (parentH - DH) / 2, DW, DH);
        panel.setBackground(TerminalUI.cardBg());
        dialog.addWidget(panel);

        WidgetGroup titleBar = new WidgetGroup(0, 0, DW, 18);
        titleBar.setBackground(new Bevel3DTexture(tokens.panel()));
        panel.addWidget(titleBar);

        LabelWidget titleLbl = new LabelWidget(8, 4,
                stripFormatting(Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.machine_options.title").getString()));
        titleLbl.setTextColor(tokens.textStrong());
        titleBar.addWidget(titleLbl);

        ButtonWidget closeBtn = TerminalButton.icon3d(DW - 18, 1, 16, "×", cd -> dialog.close());
        closeBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        titleBar.addWidget(closeBtn);

        LabelWidget nameLbl = new LabelWidget(8, 22,
                EnergyDisplayHelper.truncate(machine.getDisplayNameComponent().getString(), 28));
        nameLbl.setTextColor(tokens.textMuted());
        panel.addWidget(nameLbl);

        ButtonWidget renameBtn = TerminalButton.text3d(8, 38, DW - 16, 18,
                Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.machine_options.rename").getString(),
                tokens.warningSurface(),
                cd -> {
                    dialog.close();
                    MachineRenameDialog.open(rootGroup, parentW, parentH, machineIdx, machine);
                });
        panel.addWidget(renameBtn);

        ButtonWidget unlinkBtn = TerminalButton.text3d(8, 60, DW - 16, 18,
                Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.machine_options.unlink_machine").getString(),
                tokens.errorSurface(),
                cd -> {
                    dialog.close();
                    MachineUnlinkDialog.open(rootGroup, parentW, parentH, machineIdx, machine, player);
                });
        panel.addWidget(unlinkBtn);

        ButtonWidget cancelBtn = TerminalButton.text3d(8, 84, DW - 16, 18,
                Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.machine_options.cancel").getString(),
                tokens.panelDisabled(), cd -> dialog.close());
        panel.addWidget(cancelBtn);
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("§.", "");
    }
}
