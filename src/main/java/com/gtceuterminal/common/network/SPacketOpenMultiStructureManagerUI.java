package com.gtceuterminal.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SPacketOpenMultiStructureManagerUI {

    public final String mode;
    public final ItemStack item;

    public SPacketOpenMultiStructureManagerUI(String mode, ItemStack item) {
        this.mode = mode != null ? mode : "multi";
        this.item = item.copy();
    }

    public SPacketOpenMultiStructureManagerUI(FriendlyByteBuf buf) {
        this.mode = buf.readUtf(64);
        this.item = buf.readItem();
    }

    public static void encode(SPacketOpenMultiStructureManagerUI msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.mode, 64);
        buf.writeItem(msg.item);
    }

    public static void handle(SPacketOpenMultiStructureManagerUI msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.gtceuterminal.client.network.ClientPacketHandlers.handleOpenMultiStructureManager(
                        msg.mode, msg.item)));
        context.setPacketHandled(true);
    }
}
