package com.gtceuterminal.common.network;

import com.gtceuterminal.common.energy.EnergySnapshot;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SPacketEnergyUpdate {

    public final List<EnergySnapshot> snapshots;

    public SPacketEnergyUpdate(List<EnergySnapshot> snapshots) {
        this.snapshots = snapshots != null ? snapshots : List.of();
    }

    public SPacketEnergyUpdate(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<EnergySnapshot> decoded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) decoded.add(EnergySnapshot.decode(buf));
        this.snapshots = decoded;
    }

    public static void encode(SPacketEnergyUpdate msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.snapshots.size());
        for (EnergySnapshot s : msg.snapshots) s.encode(buf);
    }

    public static void handle(SPacketEnergyUpdate msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.gtceuterminal.client.network.ClientPacketHandlers.handleEnergyUpdate(msg.snapshots)));
        context.setPacketHandled(true);
    }
}
