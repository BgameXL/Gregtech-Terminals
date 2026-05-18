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
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class MachineOptionsDialog {

    private static final int C_GOLD  = 0xFFFFAA00;
    private static final int C_GRAY  = 0xFFAAAAAA;
    private static final int C_RED   = 0xFFFF5555;

    private MachineOptionsDialog() {}

    static void open(
            WidgetGroup rootGroup,
            int parentW,
            int parentH,
            int machineIdx,
            LinkedMachineData machine,
            Player player
    ) {
        final int DW = 220, DH = 110;
        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(0xA0000000));
        dialog.setClickClose(true);

        WidgetGroup panel = new WidgetGroup((parentW - DW) / 2, (parentH - DH) / 2, DW, DH);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF1E1E1E),
                ColorPattern.GRAY.borderTexture(-1)));
        dialog.addWidget(panel);

        WidgetGroup titleBar = new WidgetGroup(0, 0, DW, 18);
        titleBar.setBackground(new ColorRectTexture(0xFF2A2A2A));
        panel.addWidget(titleBar);

        LabelWidget titleLbl = new LabelWidget(8, 4,
                Component.translatable("gui.gtceuterminal.energy_analyzer.machine_options.title").getString());
        titleLbl.setTextColor(C_GOLD);
        titleBar.addWidget(titleLbl);

        ButtonWidget closeBtn = new ButtonWidget(DW - 18, 1, 16, 16,
                new ColorRectTexture(0x80FF0000), cd -> dialog.close());
        closeBtn.setButtonTexture(new TextTexture("§fX").setWidth(16).setType(TextTexture.TextType.NORMAL));
        closeBtn.setHoverTexture(new ColorRectTexture(0xFFFF3333));
        titleBar.addWidget(closeBtn);

        LabelWidget nameLbl = new LabelWidget(8, 22,
                "§7" + EnergyDisplayHelper.truncate(machine.getDisplayNameComponent().getString(), 28));
        nameLbl.setTextColor(C_GRAY);
        panel.addWidget(nameLbl);

        ButtonWidget renameBtn = new ButtonWidget(8, 38, DW - 16, 18,
                new ColorRectTexture(0xFF333333),
                cd -> { dialog.close(); MachineRenameDialog.open(rootGroup, parentW, parentH, machineIdx, machine); });
        renameBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.energy_analyzer.machine_options.rename").getString())
                .setWidth(DW - 16).setType(TextTexture.TextType.LEFT));
        renameBtn.setHoverTexture(new ColorRectTexture(0xFF3A3A1A));
        panel.addWidget(renameBtn);

        ButtonWidget unlinkBtn = new ButtonWidget(8, 60, DW - 16, 18,
                new ColorRectTexture(0xFF333333),
                cd -> { dialog.close(); MachineUnlinkDialog.open(rootGroup, parentW, parentH, machineIdx, machine, player); });
        unlinkBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.energy_analyzer.machine_options.unlink_machine").getString())
                .setWidth(DW - 16).setType(TextTexture.TextType.LEFT));
        unlinkBtn.setHoverTexture(new ColorRectTexture(0xFF3A1515));
        panel.addWidget(unlinkBtn);

        ButtonWidget cancelBtn = new ButtonWidget(8, 84, DW - 16, 18,
                new ColorRectTexture(0xFF2A2A2A), cd -> dialog.close());
        cancelBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.energy_analyzer.machine_options.cancel").getString())
                .setWidth(DW - 16).setType(TextTexture.TextType.NORMAL));
        cancelBtn.setHoverTexture(new ColorRectTexture(0xFF333333));
        panel.addWidget(cancelBtn);
    }
}