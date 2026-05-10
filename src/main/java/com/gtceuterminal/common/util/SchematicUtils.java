package com.gtceuterminal.common.util;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.SchematicData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared utility methods for schematic operations.
 * Centralizes logic that was previously duplicated across
 * SchematicInterfaceBehavior, PlannerScreen, and SchematicPreviewRenderer.
 */
public final class SchematicUtils {

    private SchematicUtils() {}

    /**
     * Rotates a relative block position by 90° clockwise, repeated {@code steps} times.
     * Each step applies: (x, y, z) → (-z, y, x)
     */
    public static BlockPos rotatePositionSteps(BlockPos pos, int steps) {
        BlockPos result = pos;
        for (int i = 0; i < steps; i++) {
            result = new BlockPos(-result.getZ(), result.getY(), result.getX());
        }
        return result;
    }

    /**
     * Calculates how many 90° clockwise steps are needed to rotate
     * {@code from} to face the same direction as {@code to}.
     */
    public static int getRotationSteps(Direction from, Direction to) {
        return (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
    }

    /**
     * Rotates a {@link BlockState} by 90° clockwise, repeated {@code steps} times.
     * Handles HORIZONTAL_FACING, FACING (horizontal only), and AXIS properties.
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
        BlockPos size = schematic.getSize();
        int maxDimension = Math.max(size.getX(), Math.max(size.getY(), size.getZ()));
        double distance = 4.0 + (maxDimension / 2.0);
        return Math.min(15.0, Math.max(4.0, distance));
    }

    /**
     * Returns the world position where a schematic should be placed, mirroring
     * the logic used by SchematicPreviewRenderer so the paste lands exactly on
     * the ghost preview.
     */
    public static BlockPos getTargetPlacementPos(Player player, double distance) {
        double raycastDistance = Math.max(10.0, distance + 5.0);
        HitResult hitResult = player.pick(raycastDistance, 0.0f, false);

        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            return blockHit.getBlockPos().relative(blockHit.getDirection());
        }

        Vec3 eyePos  = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 target  = eyePos.add(lookVec.scale(distance));

        return new BlockPos(
                (int) Math.floor(target.x),
                (int) Math.floor(target.y),
                (int) Math.floor(target.z)
        );
    }
}