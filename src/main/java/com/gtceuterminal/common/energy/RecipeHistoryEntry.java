package com.gtceuterminal.common.energy;

import net.minecraft.network.FriendlyByteBuf;

public record RecipeHistoryEntry(String outputName, int durationTicks, long timestamp, boolean completed) {

    public static RecipeHistoryEntry decode(FriendlyByteBuf buf) {
        return new RecipeHistoryEntry(
                buf.readUtf(),
                buf.readInt(),
                buf.readLong(),
                buf.readBoolean()
        );
    }

    // Network
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(outputName);
        buf.writeInt(durationTicks);
        buf.writeLong(timestamp);
        buf.writeBoolean(completed);
    }

    // Display helpers
    public int secondsAgo() {
        return (int) ((System.currentTimeMillis() - timestamp) / 1000);
    }

    public String durationStr() {
        int secs = durationTicks / 20;
        if (secs < 60) return secs + "s";
        return (secs / 60) + "m " + (secs % 60) + "s";
    }
}