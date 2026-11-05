package com.gtceuterminal.common.network;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.network.ClientPacketHandlers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SPacketComponentUpgradeDone {

    private final BlockPos controllerPos;

    public SPacketComponentUpgradeDone(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    public SPacketComponentUpgradeDone(FriendlyByteBuf buf) {
        this.controllerPos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(controllerPos);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    try {
                        ClientPacketHandlers.handleComponentUpgradeDone(controllerPos);
                    } catch (Exception e) {
                        GTCEUTerminalMod.LOGGER.error("SPacketComponentUpgradeDone: failed to refresh manager", e);
                    }
                }));
        ctx.get().setPacketHandled(true);
    }
}