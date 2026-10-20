package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.client.gui.widget.TerminalRenameDialog;
import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.network.CPacketEnergyAnalyzerAction;
import com.gtceuterminal.common.network.TerminalNetwork;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class EnergyRenameDialog {

    private EnergyRenameDialog() {
    }

    static void open(WidgetGroup rootGroup, int parentW, int parentH, int machineIdx, LinkedMachineData machine) {
        TerminalRenameDialog.open(rootGroup, parentW, parentH,
                "gui.gtceuterminal.energy_analyzer.rename_dialog.title",
                null, machine.getCustomName(), null,
                name -> {
                    machine.setCustomName(name);
                    TerminalNetwork.CHANNEL.sendToServer(
                            new CPacketEnergyAnalyzerAction(CPacketEnergyAnalyzerAction.Action.RENAME, machineIdx, name));
                });
    }

}
