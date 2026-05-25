package com.gtceuterminal.common.network;

import com.gtceuterminal.common.data.GTCEUTerminalItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

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
            ItemStack stack = findManagerItem(player);
            if (stack == null || stack.isEmpty()) return;
            CompoundTag existing = stack.getOrCreateTag();
            existing.putInt("NoHatchMode",  tag.getInt("NoHatchMode")  != 0 ? 1 : 0);
            existing.putInt("TierMode",     Mth.clamp(tag.getInt("TierMode"),     1, 16));
            existing.putInt("RepeatCount",  Mth.clamp(tag.getInt("RepeatCount"),  0, 99));
            existing.putInt("IsUseAE",      tag.getInt("IsUseAE")      != 0 ? 1 : 0);
            player.getInventory().setChanged();
        });
        ctx.get().setPacketHandled(true);
    }

    private static ItemStack findManagerItem(ServerPlayer player) {
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (isManagerItem(s)) return s;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (isManagerItem(s)) return s;
        }
        return null;
    }

    private static boolean isManagerItem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(GTCEUTerminalItems.MULTI_STRUCTURE_MANAGER.get());
    }
}