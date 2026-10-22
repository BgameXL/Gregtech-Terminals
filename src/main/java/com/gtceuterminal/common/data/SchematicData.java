package com.gtceuterminal.common.data;

import com.gtceuterminal.GTCEUTerminalMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record SchematicData(String name, String multiblockType, Map<BlockPos, BlockState> blocks,
                            Map<BlockPos, CompoundTag> blockEntities, BlockPos size, String originalFacing) {

    public SchematicData(String name, String multiblockType, Map<BlockPos, BlockState> blocks) {
        this(name, multiblockType, blocks, Collections.emptyMap(), "south");
    }

    public SchematicData(String name, String multiblockType, Map<BlockPos, BlockState> blocks, String originalFacing) {
        this(name, multiblockType, blocks, Collections.emptyMap(), originalFacing);
    }

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

        GTCEUTerminalMod.LOGGER.info("Saving schematic '{}' - originalFacing: {}", name, originalFacing);

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

    public SchematicData rotate(Direction.Axis axis) {
        Map<BlockPos, BlockState> rotatedBlocks = new HashMap<>();
        Map<BlockPos, CompoundTag> rotatedBEs = new HashMap<>();

        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();

            BlockPos newPos = rotatePosition(pos, axis);
            BlockState newState = rotateBlockState(state, axis);

            rotatedBlocks.put(newPos, newState);
        }

        for (Map.Entry<BlockPos, CompoundTag> entry : blockEntities.entrySet()) {
            BlockPos newPos = rotatePosition(entry.getKey(), axis);
            rotatedBEs.put(newPos, entry.getValue().copy());
        }

        return new SchematicData(name, multiblockType, rotatedBlocks, rotatedBEs, originalFacing);
    }

    private BlockPos rotatePosition(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> new BlockPos(pos.getX(), -pos.getZ(), pos.getY());
            case Y -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case Z -> new BlockPos(pos.getY(), -pos.getX(), pos.getZ());
        };
    }

    private BlockState rotateBlockState(BlockState state, Direction.Axis axis) {
        try {
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                var rotated = switch (axis) {
                    case Y -> facing.getClockWise();
                    case X, Z -> facing;
                };
                return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotated);
            }

            if (state.hasProperty(BlockStateProperties.FACING)) {
                var facing = state.getValue(BlockStateProperties.FACING);
                var rotated = switch (axis) {
                    case Y -> facing.getClockWise();
                    case X -> rotateFacingX(facing);
                    case Z -> rotateFacingZ(facing);
                };
                return state.setValue(BlockStateProperties.FACING, rotated);
            }
        } catch (Exception e) {
        }

        return state;
    }

    private Direction rotateFacingX(Direction facing) {
        return switch (facing) {
            case UP -> Direction.NORTH;
            case NORTH -> Direction.DOWN;
            case DOWN -> Direction.SOUTH;
            case SOUTH -> Direction.UP;
            default -> facing;
        };
    }

    private Direction rotateFacingZ(Direction facing) {
        return switch (facing) {
            case UP -> Direction.EAST;
            case EAST -> Direction.DOWN;
            case DOWN -> Direction.WEST;
            case WEST -> Direction.UP;
            default -> facing;
        };
    }
}