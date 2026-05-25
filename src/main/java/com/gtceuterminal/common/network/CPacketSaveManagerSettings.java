package com.gtceuterminal.common.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class CPacketSaveManagerSettings {

    private final CompoundTag tag;

    public CPacketSaveManagerSettings(CompoundTag tag) {
        this.tag = tag;
    }

    public CPacketSaveManagerSettings(FriendlyByteBuf buf) {
        this.tag = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || tag == null) return;
            ItemStack stack = findTerminalItem(player);
            if (stack == null || stack.isEmpty()) return;
            CompoundTag existing = stack.getOrCreateTag();
            for (String key : tag.getAllKeys())
                existing.put(key, tag.get(key));
            player.getInventory().setChanged();
        });
        ctx.get().setPacketHandled(true);
    }

    private static ItemStack findTerminalItem(ServerPlayer player) {
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (isTerminalItem(s)) return s;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (isTerminalItem(s)) return s;
        }
        return null;
    }

    private static boolean isTerminalItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return rl != null && "gtceuterminal".equals(rl.getNamespace());
    }
}