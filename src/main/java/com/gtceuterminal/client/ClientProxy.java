package com.gtceuterminal.client;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.network.*;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

// Client-only proxy for handling client-side operations.
@OnlyIn(Dist.CLIENT)
public class ClientProxy {

    public static void openEnergyAnalyzerGUI(ServerPlayer player, int machineIndex) {
        if (player == null) return;
        GTCEUTerminalMod.LOGGER.debug("ClientProxy: sending CPacketOpenEnergyAnalyzerUI index={}", machineIndex);
        TerminalNetwork.CHANNEL.sendToServer(new CPacketOpenEnergyAnalyzerUI(machineIndex));
    }

    /**
     *
     // Request server to open Power Monitor UI
     public static void openPowerMonitorGUI(net.minecraft.core.BlockPos monitorPos, Player player) {
     if (player == null) {
     GTCEUTerminalMod.LOGGER.error("ClientProxy.openPowerMonitorGUI: player is null!");
     return;
     }
     GTCEUTerminalMod.LOGGER.debug("ClientProxy: Sending CPacketOpenPowerMonitorUI to server for pos {}", monitorPos);

     try {
     TerminalNetwork.CHANNEL.sendToServer(new CPacketOpenPowerMonitorUI(monitorPos));
     GTCEUTerminalMod.LOGGER.debug("ClientProxy: Packet sent successfully");
     } catch (Exception e) {
     GTCEUTerminalMod.LOGGER.error("ClientProxy: Error sending packet", e);
     }
     } **/
}// Useless