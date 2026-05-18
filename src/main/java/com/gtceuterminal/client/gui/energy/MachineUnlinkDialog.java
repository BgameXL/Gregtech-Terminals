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
final class MachineUnlinkDialog {

    private static final int C_GRAY = 0xFFAAAAAA;
    private static final int C_RED  = 0xFFFF5555;

    private MachineUnlinkDialog() {}

    static void open(
            WidgetGroup rootGroup,
            int parentW,
            int parentH,
            int machineIdx,
            LinkedMachineData machine,
            Player player
    ) {
        final int DW = 220, DH = 90;

        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(0xA0000000));
        dialog.setClickClose(true);

        WidgetGroup panel = new WidgetGroup((parentW - DW) / 2, (parentH - DH) / 2, DW, DH);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF1E1E1E),
                ColorPattern.GRAY.borderTexture(-1)));
        dialog.addWidget(panel);

        WidgetGroup titleBar = new WidgetGroup(0, 0, DW, 18);
        titleBar.setBackground(new ColorRectTexture(0xFF2A0000));
        panel.addWidget(titleBar);

        LabelWidget titleLbl = new LabelWidget(8, 4,
                Component.translatable("gui.gtceuterminal.energy_analyzer.unlink_dialog.title").getString());
        titleLbl.setTextColor(C_RED);
        titleBar.addWidget(titleLbl);

        String targetName = EnergyDisplayHelper.truncate(machine.getDisplayNameComponent().getString(), 22);
        LabelWidget confirmLbl = new LabelWidget(8, 22,
                Component.translatable("gui.gtceuterminal.energy_analyzer.unlink_dialog.remove_prompt", targetName).getString());
        confirmLbl.setTextColor(C_GRAY);
        panel.addWidget(confirmLbl);

        ButtonWidget unlinkBtn = new ButtonWidget(8, 40, (DW - 20) / 2, 18,
                new ColorRectTexture(0xFF4A1A1A),
                cd -> {
                    TerminalNetwork.CHANNEL.sendToServer(
                            new CPacketEnergyAnalyzerAction(CPacketEnergyAnalyzerAction.Action.UNLINK, machineIdx, ""));
                    if (player instanceof net.minecraft.client.player.LocalPlayer lp) {
                        lp.closeContainer();
                    }
                });
        unlinkBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.energy_analyzer.unlink_dialog.unlink").getString())
                .setWidth((DW - 20) / 2).setType(TextTexture.TextType.NORMAL));
        unlinkBtn.setHoverTexture(new ColorRectTexture(0xFF6A2A2A));
        panel.addWidget(unlinkBtn);

        ButtonWidget cancelBtn = new ButtonWidget(DW / 2 + 2, 40, (DW - 20) / 2, 18,
                new ColorRectTexture(0xFF2A2A2A), cd -> dialog.close());
        cancelBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.energy_analyzer.unlink_dialog.cancel").getString())
                .setWidth((DW - 20) / 2).setType(TextTexture.TextType.NORMAL));
        cancelBtn.setHoverTexture(new ColorRectTexture(0xFF333333));
        panel.addWidget(cancelBtn);

        LabelWidget hint = new LabelWidget(DW / 2 - 50, DH - 12,
                Component.translatable("gui.gtceuterminal.energy_analyzer.unlink_dialog.hint_click_outside_cancel").getString());
        hint.setTextColor(0xFF444444);
        panel.addWidget(hint);
    }
}