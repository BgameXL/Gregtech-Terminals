package com.gtceuterminal.common.autocraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record AnalysisResult(Kind kind, List<Entry> entries, BlockPos controllerPos, String upgradeId,
                             List<BlockPos> componentPositions, int targetTier, ItemStack targetItemStack,
                             int componentCount, List<BlockPos> intruderPositions) {

    // BUILD constructor
    public AnalysisResult(List<Entry> entries, BlockPos controllerPos) {
        this(entries, controllerPos, List.of());
    }

    public AnalysisResult(List<Entry> entries, BlockPos controllerPos,
                          List<BlockPos> intruderPositions) {
        this(Kind.BUILD, entries, controllerPos, null, List.of(), 0, ItemStack.EMPTY, 0, intruderPositions);
    }

    // UPGRADE constructor
    public AnalysisResult(List<Entry> entries,
                          BlockPos controllerPos,
                          int targetTier,
                          String upgradeId,
                          List<BlockPos> componentPositions,
                          ItemStack targetItemStack,
                          int componentCount) {
        this(Kind.UPGRADE, entries, controllerPos, upgradeId, componentPositions, targetTier, targetItemStack, componentCount, List.of());
    }

    public static AnalysisResult decode(FriendlyByteBuf buf) {
        Kind kind = buf.readEnum(Kind.class);
        var controllerPos = buf.readBlockPos();
        int targetTier = buf.readInt();
        String upgradeId = buf.readUtf();
        if (upgradeId.isEmpty()) upgradeId = null;

        int compCount = buf.readInt();
        List<BlockPos> comps = new ArrayList<>(compCount);
        for (int i = 0; i < compCount; i++) comps.add(buf.readBlockPos());

        ItemStack targetStack = buf.readItem();
        int componentCnt = buf.readInt();

        int entryCount = buf.readInt();
        List<Entry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) entries.add(Entry.decode(buf));

        int intruderCount = buf.readInt();
        List<BlockPos> intruders = new ArrayList<>(intruderCount);
        for (int i = 0; i < intruderCount; i++) intruders.add(buf.readBlockPos());

        if (kind == Kind.BUILD) {
            return new AnalysisResult(entries, controllerPos, intruders);
        } else {
            return new AnalysisResult(entries, controllerPos, targetTier, upgradeId,
                    comps, targetStack, componentCnt);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(kind);
        buf.writeBlockPos(controllerPos);
        buf.writeInt(targetTier);
        buf.writeUtf(upgradeId != null ? upgradeId : "");

        buf.writeInt(componentPositions.size());
        for (var pos : componentPositions) buf.writeBlockPos(pos);

        buf.writeItem(targetItemStack);
        buf.writeInt(componentCount);

        buf.writeInt(entries.size());
        for (Entry e : entries) e.encode(buf);

        buf.writeInt(intruderPositions.size());
        for (var pos : intruderPositions) buf.writeBlockPos(pos);
    }

    public boolean allAvailable() {
        return entries.stream().filter(e -> !e.informational).allMatch(Entry::hasAll);
    }

    public boolean anyMissing() {
        return entries.stream().filter(e -> !e.informational).anyMatch(e -> !e.hasAll() && !e.craftable);
    }

    public int missingCount() {
        return (int) entries.stream().filter(e -> !e.informational && !e.hasAll()).count();
    }

    public enum Kind {BUILD, UPGRADE}

    public record Entry(ItemStack stack, long inME, long inInventory, boolean craftable, boolean informational) {
        public Entry(ItemStack stack, long inME, long inInventory, boolean craftable) {
            this(stack, inME, inInventory, craftable, false);
        }

        public static Entry decode(FriendlyByteBuf buf) {
            return new Entry(buf.readItem(), buf.readLong(), buf.readLong(), buf.readBoolean(), buf.readBoolean());
        }

        public int needed() {
            return stack.getCount();
        }

        public long totalAvailable() {
            return inME + inInventory;
        }

        public boolean hasAll() {
            return totalAvailable() >= needed();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeItem(stack);
            buf.writeLong(inME);
            buf.writeLong(inInventory);
            buf.writeBoolean(craftable);
            buf.writeBoolean(informational);
        }
    }
}