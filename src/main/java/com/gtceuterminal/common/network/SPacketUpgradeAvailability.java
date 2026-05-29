package com.gtceuterminal.common.network;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.network.ClientPacketHandlers;
import com.gtceuterminal.common.autocraft.AnalysisResult;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SPacketUpgradeAvailability {

    private final int requestId;
    private final AnalysisResult result;

    public SPacketUpgradeAvailability(int requestId, AnalysisResult result) {
        this.requestId = requestId;
        this.result = result;
    }

    public SPacketUpgradeAvailability(FriendlyByteBuf buf) {
        this.requestId = buf.readVarInt();
        this.result = AnalysisResult.decode(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(requestId);
        result.encode(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    try {
                        ClientPacketHandlers.handleUpgradeAvailability(requestId, result);
                    } catch (Exception e) {
                        GTCEUTerminalMod.LOGGER.error("SPacketUpgradeAvailability: failed to update preview", e);
                    }
                }));
        ctx.get().setPacketHandled(true);
    }
}
