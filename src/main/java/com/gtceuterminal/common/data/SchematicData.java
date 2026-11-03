package com.gtceuterminal.common.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record SchematicData(String name, String multiblockType, Map<BlockPos, BlockState> blocks,
                            Map<BlockPos, CompoundTag> blockEntities, BlockPos size, String originalFacing) {

    public SchematicData(String name, String multiblockType, Map<BlockPos, BlockState> blocks,
                         Map<BlockPos, CompoundTag> blockEntities, String originalFacing) {
        this(name, multiblockType, new HashMap<>(blocks), new HashMap<>(blockEntities), calculateSize(blocks), originalFacing);
    }

    public static SchematicData fromNBT(CompoundTag tag, HolderLookup.Provider provider) {
        String name = tag.getString("Name");
        String type = tag.getString("Type");
        String originalFacing = tag.contains("OriginalFacing") ? tag.getString("OriginalFacing") : "south";

        Map<BlockPos, BlockState> blocks = new HashMap<>();
        ListTag blocksList = tag.getList("Blocks", 10);

        var blockLookup = provider.lookupOrThrow(Registries.BLOCK);

        for (int i = 0; i < blocksList.size(); i++) {
            CompoundTag blockTag = blocksList.getCompound(i);
            BlockPos pos = NbtUtils.readBlockPos(blockTag.getCompound("Pos"));
            BlockState state = NbtUtils.readBlockState(blockLookup, blockTag.getCompound("State"));
            blocks.put(pos, state);
        }

        Map<BlockPos, CompoundTag> blockEntities = new HashMap<>();
        if (tag.contains("BlockEntities")) {
            ListTag beList = tag.getList("BlockEntities", 10);
            for (int i = 0; i < beList.size(); i++) {
                CompoundTag beTag = beList.getCompound(i);
                BlockPos pos = NbtUtils.readBlockPos(beTag.getCompound("Pos"));
                CompoundTag nbt = beTag.getCompound("NBT");
                if (!nbt.isEmpty()) {
                    blockEntities.put(pos, nbt.copy());
                }
            }
        }

        return new SchematicData(name, type, blocks, blockEntities, originalFacing);
    }

    private static BlockPos calculateSize(Map<BlockPos, BlockState> blocks) {
        if (blocks.isEmpty()) return BlockPos.ZERO;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : blocks.keySet()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new BlockPos(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
    }

    @Override
    public Map<BlockPos, BlockState> blocks() {
        return Collections.unmodifiableMap(blocks);
    }

    @Override
    public Map<BlockPos, CompoundTag> blockEntities() {
        return Collections.unmodifiableMap(blockEntities);
    }

    public int getBlockCount() {
        return blocks.size();
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Type", multiblockType);
        tag.putString("OriginalFacing", originalFacing);

        // Able to copy and paste blocks from different mods, include liquids like water and lava.
        ListTag blocksList = new ListTag();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.put("Pos", NbtUtils.writeBlockPos(entry.getKey()));
            blockTag.put("State", NbtUtils.writeBlockState(entry.getValue()));
            blocksList.add(blockTag);
        }
        tag.put("Blocks", blocksList);

        // Optional BlockEntities
        if (!blockEntities.isEmpty()) {
            ListTag beList = new ListTag();
            for (Map.Entry<BlockPos, CompoundTag> entry : blockEntities.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty()) continue;
                CompoundTag beTag = new CompoundTag();
                beTag.put("Pos", NbtUtils.writeBlockPos(entry.getKey()));
                beTag.put("NBT", entry.getValue().copy());
                beList.add(beTag);
            }
            tag.put("BlockEntities", beList);
        }

        return tag;
    }
}