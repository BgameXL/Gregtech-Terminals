package com.gtceuterminal.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SPacketOpenSchematicInterfaceUI {

    public final ItemStack item;

    public SPacketOpenSchematicInterfaceUI(ItemStack item) {
        this.item = item.copy();
    }

    public SPacketOpenSchematicInterfaceUI(FriendlyByteBuf buf) {
        this.item = buf.readItem();
    }

    public static void encode(SPacketOpenSchematicInterfaceUI msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.item);
    }

    public static void handle(SPacketOpenSchematicInterfaceUI msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.gtceuterminal.client.network.ClientPacketHandlers.handleOpenSchematicInterface(msg.item)));
        context.setPacketHandled(true);
    }
}
