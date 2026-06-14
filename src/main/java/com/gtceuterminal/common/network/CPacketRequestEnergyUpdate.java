package com.gtceuterminal.common.network;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.energy.EnergySnapshot;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class CPacketRequestEnergyUpdate {

    public CPacketRequestEnergyUpdate() {}

    public CPacketRequestEnergyUpdate(FriendlyByteBuf buf) {}

    public static void encode(CPacketRequestEnergyUpdate msg, FriendlyByteBuf buf) {}

    public static void handle(CPacketRequestEnergyUpdate msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            try {
                List<EnergySnapshot> snapshots = SPacketOpenEnergyAnalyzerUI.collectSnapshots(player);
                TerminalNetwork.sendToPlayer(new SPacketEnergyUpdate(snapshots), player);
            } catch (Throwable t) {
                GTCEUTerminalMod.LOGGER.error("Failed to collect energy update", t);
            }
        });
        context.setPacketHandled(true);
    }
}
