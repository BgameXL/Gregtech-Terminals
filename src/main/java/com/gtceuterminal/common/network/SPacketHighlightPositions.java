package com.gtceuterminal.common.network;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SPacketHighlightPositions {

    private final BlockPos controllerPos;
    private final List<BlockPos> positions;

    public SPacketHighlightPositions(BlockPos controllerPos, List<BlockPos> positions) {
        this.controllerPos = controllerPos;
        this.positions = positions;
    }

    public SPacketHighlightPositions(FriendlyByteBuf buf) {
        this.controllerPos = buf.readBlockPos();
        int n = buf.readInt();
        this.positions = new ArrayList<>(n);
        for (int i = 0; i < n; i++) positions.add(buf.readBlockPos());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(controllerPos);
        buf.writeInt(positions.size());
        for (BlockPos p : positions) buf.writeBlockPos(p);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    try {
                        com.gtceuterminal.client.network.ClientPacketHandlers.handleHighlightPositions(controllerPos, positions);
                    } catch (Exception e) {
                        GTCEUTerminalMod.LOGGER.error("SPacketHighlightPositions: failed to apply highlight", e);
                    }
                }));
        ctx.get().setPacketHandled(true);
    }
}
