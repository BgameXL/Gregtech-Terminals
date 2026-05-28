package com.gtceuterminal.common.network;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.network.ClientPacketHandlers;
import com.gtceuterminal.common.autocraft.AnalysisResult;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SPacketAnalysisResult {

    private final AnalysisResult result;

    public SPacketAnalysisResult(AnalysisResult result) {
        this.result = result;
    }

    public void encode(FriendlyByteBuf buf) {
        result.encode(buf);
    }

    public SPacketAnalysisResult(FriendlyByteBuf buf) {
        this.result = AnalysisResult.decode(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    try {
                        ClientPacketHandlers.handleAnalysisResult(result);
                    } catch (Exception e) {
                        GTCEUTerminalMod.LOGGER.error("SPacketAnalysisResult: failed to open dialog", e);
                    }
                }));
        ctx.get().setPacketHandled(true);
    }
}