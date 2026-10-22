package com.gtceuterminal.common.autocraft;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class AnalysisResult {

    public static final class Entry {
        public final ItemStack stack;
        public final long inME;
        public final long inInventory;
        public final boolean craftable;
        public final boolean informational;

        public Entry(ItemStack stack, long inME, long inInventory, boolean craftable) {
            this(stack, inME, inInventory, craftable, false);
        }

        public Entry(ItemStack stack, long inME, long inInventory, boolean craftable, boolean informational) {
            this.stack = stack;
            this.inME = inME;
            this.inInventory = inInventory;
            this.craftable = craftable;
            this.informational = informational;
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

        public static Entry decode(FriendlyByteBuf buf) {
            return new Entry(buf.readItem(), buf.readLong(), buf.readLong(), buf.readBoolean(), buf.readBoolean());
        }
    }

    public enum Kind {BUILD, UPGRADE}

    public final Kind kind;
    public final List<Entry> entries;
    public final net.minecraft.core.BlockPos controllerPos;
    public final String upgradeId;
    public final List<net.minecraft.core.BlockPos> componentPositions;
    public final int targetTier;
    public final ItemStack targetItemStack;
    public final int componentCount;
    public final List<net.minecraft.core.BlockPos> intruderPositions;

    // BUILD constructor
    public AnalysisResult(List<Entry> entries, net.minecraft.core.BlockPos controllerPos) {
        this(entries, controllerPos, List.of());
    }

    public AnalysisResult(List<Entry> entries, net.minecraft.core.BlockPos controllerPos,
                          List<net.minecraft.core.BlockPos> intruderPositions) {
        this.kind = Kind.BUILD;
        this.entries = entries;
        this.controllerPos = controllerPos;
        this.targetTier = 0;
        this.upgradeId = null;
        this.componentPositions = List.of();
        this.targetItemStack = ItemStack.EMPTY;
        this.componentCount = 0;
        this.intruderPositions = intruderPositions;
    }

    // UPGRADE constructor
    public AnalysisResult(List<Entry> entries,
                          net.minecraft.core.BlockPos controllerPos,
                          int targetTier,
                          String upgradeId,
                          List<net.minecraft.core.BlockPos> componentPositions,
                          ItemStack targetItemStack,
                          int componentCount) {
        this.kind = Kind.UPGRADE;
        this.entries = entries;
        this.controllerPos = controllerPos;
        this.targetTier = targetTier;
        this.upgradeId = upgradeId;
        this.componentPositions = componentPositions;
        this.targetItemStack = targetItemStack;
        this.componentCount = componentCount;
        this.intruderPositions = List.of();
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

    public static AnalysisResult decode(FriendlyByteBuf buf) {
        Kind kind = buf.readEnum(Kind.class);
        var controllerPos = buf.readBlockPos();
        int targetTier = buf.readInt();
        String upgradeId = buf.readUtf();
        if (upgradeId.isEmpty()) upgradeId = null;

        int compCount = buf.readInt();
        List<net.minecraft.core.BlockPos> comps = new ArrayList<>(compCount);
        for (int i = 0; i < compCount; i++) comps.add(buf.readBlockPos());

        ItemStack targetStack = buf.readItem();
        int componentCnt = buf.readInt();

        int entryCount = buf.readInt();
        List<Entry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) entries.add(Entry.decode(buf));

        int intruderCount = buf.readInt();
        List<net.minecraft.core.BlockPos> intruders = new ArrayList<>(intruderCount);
        for (int i = 0; i < intruderCount; i++) intruders.add(buf.readBlockPos());

        if (kind == Kind.BUILD) {
            return new AnalysisResult(entries, controllerPos, intruders);
        } else {
            return new AnalysisResult(entries, controllerPos, targetTier, upgradeId,
                    comps, targetStack, componentCnt);
        }
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
}