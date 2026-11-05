package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.client.gui.widget.TerminalRenameDialog;
import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.network.CPacketSetCustomMultiblockName;
import com.gtceuterminal.common.network.TerminalNetwork;

import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class MultiblockRenameDialog {

    private MultiblockRenameDialog() {
    }

    static void open(
            WidgetGroup rootGroup,
            int parentW,
            int parentH,
            MultiblockInfo mb,
            Player player,
            DraggableScrollableWidgetGroup listScroll,
            Runnable onConfirm
    ) {
        TerminalRenameDialog.open(rootGroup, parentW, parentH,
                "gui.gtceuterminal.multiblock_manager.rename_dialog.title",
                mb.getMachineTypeName(), mb.getCustomDisplayName(), listScroll,
                name -> {
                    String dimId = LinkedMachineData.dimId(player.level());
                    TerminalNetwork.CHANNEL.sendToServer(
                            new CPacketSetCustomMultiblockName(null, mb.posKey(dimId), name, name.isEmpty()));
                    mb.setCustomDisplayName(name.isEmpty() ? null : name);
                    onConfirm.run();
                });
    }

}
