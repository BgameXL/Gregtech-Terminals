package com.gtceuterminal.client;

import com.gtceuterminal.common.network.CPacketOpenEnergyAnalyzerUI;
import com.gtceuterminal.common.network.TerminalNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientProxy {

    public static void openEnergyAnalyzerGUI(ServerPlayer player, int machineIndex) {
        if (player == null) return;
        TerminalNetwork.CHANNEL.sendToServer(new CPacketOpenEnergyAnalyzerUI(machineIndex));
    }
}
