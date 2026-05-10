package com.gtceuterminal.client.renderer;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.util.SchematicUtils;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SchematicPreviewRenderer {

    public static void renderGhostBlocks(PoseStack poseStack,
                                         MultiBufferSource bufferSource,
                                         SchematicData schematic,
                                         Minecraft minecraft,
                                         CompoundTag clipboardTag) {
        if (schematic == null || schematic.getBlocks().isEmpty()
                || minecraft.level == null || minecraft.player == null) return;

        Direction originalFacing = Direction.SOUTH;
        try {
            String facingStr = schematic.getOriginalFacing();
            if (facingStr != null && !facingStr.isEmpty()) {
                Direction byName = Direction.byName(facingStr);
                if (byName != null) originalFacing = byName;
            }
        } catch (IllegalArgumentException e) {
            GTCEUTerminalMod.LOGGER.debug("SchematicPreviewRenderer: invalid facing '{}', defaulting to SOUTH", schematic.getOriginalFacing());
        }

        double distance    = SchematicUtils.calculateOptimalDistance(schematic);
        BlockPos targetPos = SchematicUtils.getTargetPlacementPos(minecraft.player, distance);
        if (targetPos == null) return;

        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        if (cameraPos.distanceTo(Vec3.atCenterOf(targetPos)) > 20.0) return;

        Direction playerFacing = minecraft.player.getDirection();
        Direction targetFacing = playerFacing.getOpposite();
        int rotationSteps = SchematicUtils.getRotationSteps(originalFacing, targetFacing);


        // Apply the same Y-offset as SchematicPaster so the preview aligns with the paste.
        int minRelY = schematic.getBlocks().keySet().stream()
                .mapToInt(pos -> SchematicUtils.rotatePositionSteps(pos, rotationSteps).getY())
                .min().orElse(0);
        BlockPos adjustedTarget = targetPos.above(-minRelY);

        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();

        for (var entry : schematic.getBlocks().entrySet()) {
            BlockPos   relativePos  = entry.getKey();
            BlockState state        = entry.getValue();

            BlockPos   rotatedPos   = SchematicUtils.rotatePositionSteps(relativePos, rotationSteps);
            BlockState rotatedState = SchematicUtils.rotateBlockStateSteps(state, rotationSteps);
            BlockPos   worldPos     = adjustedTarget.offset(rotatedPos);
            poseStack.pushPose();
            poseStack.translate(
                    worldPos.getX() - cameraPos.x,
                    worldPos.getY() - cameraPos.y,
                    worldPos.getZ() - cameraPos.z);
            try {
                blockRenderer.renderSingleBlock(rotatedState, poseStack, bufferSource,
                        15728880, OverlayTexture.NO_OVERLAY);
            } catch (RuntimeException e) {
                GTCEUTerminalMod.LOGGER.debug("SchematicPreviewRenderer: skipped block render for {}: {}", rotatedState.getBlock().getDescriptionId(), e.getMessage());
            }
            poseStack.popPose();
        }
    }

    public static void renderGhostBlocksAtOrigin(PoseStack poseStack,
                                                 MultiBufferSource bufferSource,
                                                 SchematicData schematic,
                                                 Minecraft minecraft,
                                                 int rotSteps) {
        if (schematic == null || schematic.getBlocks().isEmpty()) return;

        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();

        for (var entry : schematic.getBlocks().entrySet()) {
            BlockPos   relPos       = entry.getKey();
            BlockState state        = entry.getValue();
            if (state.isAir()) continue;

            BlockPos   rotated      = SchematicUtils.rotatePositionSteps(relPos, rotSteps);
            BlockState rotatedState = SchematicUtils.rotateBlockStateSteps(state, rotSteps);

            poseStack.pushPose();
            poseStack.translate(rotated.getX(), rotated.getY(), rotated.getZ());
            try {
                blockRenderer.renderSingleBlock(rotatedState, poseStack, bufferSource,
                        15728880, OverlayTexture.NO_OVERLAY);
            } catch (RuntimeException e) {
                GTCEUTerminalMod.LOGGER.debug("SchematicPreviewRenderer: skipped ghost block render for {}: {}",
                        rotatedState.getBlock().getDescriptionId(), e.getMessage());
            }
            poseStack.popPose();
        }
    }

    public static BlockPos getTargetPlacementPos(Minecraft minecraft, double distance) {
        return SchematicUtils.getTargetPlacementPos(minecraft.player, distance);
    }
}