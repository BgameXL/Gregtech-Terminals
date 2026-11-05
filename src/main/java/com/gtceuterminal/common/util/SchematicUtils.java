package com.gtceuterminal.common.util;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.SchematicData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SchematicUtils {

    private SchematicUtils() {
    }

    public static BlockPos rotatePositionSteps(BlockPos pos, int steps) {
        BlockPos result = pos;
        for (int i = 0; i < steps; i++) {
            result = new BlockPos(-result.getZ(), result.getY(), result.getX());
        }
        return result;
    }

    public static int getRotationSteps(Direction from, Direction to) {
        return (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
    }

    public static void rotateCoverSides(CompoundTag beTag, int steps) {
        steps &= 3;
        if (steps == 0 || beTag == null || !beTag.contains("cover", Tag.TAG_COMPOUND)) return;

        CompoundTag cover = beTag.getCompound("cover");
        String[] fieldName = {"down", "up", "north", "south", "west", "east"};

        java.util.List<CompoundTag> entries = new java.util.ArrayList<>();
        java.util.List<String> originalKeys = new java.util.ArrayList<>();
        for (String key : fieldName) {
            if (cover.contains(key, Tag.TAG_COMPOUND)) {
                entries.add(cover.getCompound(key).copy());
                originalKeys.add(key);
            }
        }
        if (entries.isEmpty()) return;

        for (String key : fieldName) cover.remove(key);

        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.get(i);
            String targetKey = originalKeys.get(i);

            CompoundTag uid = entry.getCompound("uid");
            if (uid.contains("side", Tag.TAG_INT)) {
                int ordinal = uid.getInt("side");
                if (ordinal >= 0 && ordinal < Direction.values().length) {
                    Direction dir = Direction.values()[ordinal];
                    if (dir.getAxis().isHorizontal()) {
                        for (int s = 0; s < steps; s++) dir = dir.getClockWise();
                        uid.putInt("side", dir.ordinal());
                        entry.put("uid", uid);
                        targetKey = fieldName[dir.ordinal()];
                    }
                }
            }
            cover.put(targetKey, entry);
        }
        beTag.put("cover", cover);
    }

    /**
     * Rotates a {@link BlockState} by 90° clockwise, repeated {@code steps} times.
     */
    public static BlockState rotateBlockStateSteps(BlockState state, int steps) {
        BlockState result = state;
        for (int i = 0; i < steps; i++) {
            result = rotateBlockStateOnce(result);
        }
        return result;
    }

    private static BlockState rotateBlockStateOnce(BlockState state) {
        try {
            for (var prop : state.getProperties()) {
                if (!(prop instanceof net.minecraft.world.level.block.state.properties.DirectionProperty dirProp))
                    continue;

                Direction facing = state.getValue(dirProp);
                if (!facing.getAxis().isHorizontal()) continue;

                Direction rotated = facing.getClockWise();

                if (dirProp.getPossibleValues().contains(rotated)) {
                    return state.setValue(dirProp, rotated);
                }
            }

            if (state.hasProperty(BlockStateProperties.AXIS)) {
                var axis = state.getValue(BlockStateProperties.AXIS);
                if (axis == Direction.Axis.X)
                    return state.setValue(BlockStateProperties.AXIS, Direction.Axis.Z);
                if (axis == Direction.Axis.Z)
                    return state.setValue(BlockStateProperties.AXIS, Direction.Axis.X);
            }
        } catch (IllegalArgumentException e) {
            GTCEUTerminalMod.LOGGER.debug("SchematicUtils: could not rotate BlockState property: {}", e.getMessage());
        }
        return state;
    }

    public static double calculateOptimalDistance(SchematicData schematic) {
        BlockPos size = schematic.size();
        int maxDimension = Math.max(size.getX(), Math.max(size.getY(), size.getZ()));
        double distance = 4.0 + (maxDimension / 2.0);
        return Math.min(15.0, Math.max(4.0, distance));
    }

    public static BlockPos getTargetPlacementPos(Player player, double distance) {
        double raycastDistance = Math.max(10.0, distance + 5.0);
        HitResult hitResult = player.pick(raycastDistance, 0.0f, false);

        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            return blockHit.getBlockPos().relative(blockHit.getDirection());
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 target = eyePos.add(lookVec.scale(distance));

        return new BlockPos(
                (int) Math.floor(target.x),
                (int) Math.floor(target.y),
                (int) Math.floor(target.z)
        );
    }

    public static Direction resolveOriginalFacing(SchematicData schematic) {
        String facingStr = schematic.originalFacing();
        if (facingStr != null && !facingStr.isEmpty()) {
            Direction byName = Direction.byName(facingStr);
            if (byName != null) return byName;
        }
        return Direction.SOUTH;
    }

    public static int minRotatedY(SchematicData schematic, int rotationSteps) {
        return schematic.blocks().keySet().stream()
                .mapToInt(pos -> rotatePositionSteps(pos, rotationSteps).getY())
                .min()
                .orElse(0);
    }
}