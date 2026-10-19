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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SchematicPreviewRenderer {

    public static void renderGhostBlocks(PoseStack poseStack,
                                         MultiBufferSource bufferSource,
                                         SchematicData schematic,
                                         Minecraft minecraft) {
        if (schematic == null || schematic.getBlocks().isEmpty()
                || minecraft.level == null || minecraft.player == null) return;

        Direction originalFacing = SchematicUtils.resolveOriginalFacing(schematic);

        double distance = SchematicUtils.calculateOptimalDistance(schematic);
        BlockPos targetPos = SchematicUtils.getTargetPlacementPos(minecraft.player, distance);
        if (targetPos == null) return;

        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        if (cameraPos.distanceTo(Vec3.atCenterOf(targetPos)) > 20.0) return;

        Direction playerFacing = minecraft.player.getDirection();
        Direction targetFacing = playerFacing.getOpposite();
        int rotationSteps = SchematicUtils.getRotationSteps(originalFacing, targetFacing);

        // Apply the same Y-offset as SchematicPaster so the preview aligns with the paste.
        int minRelY = SchematicUtils.minRotatedY(schematic, rotationSteps);
        BlockPos adjustedTarget = targetPos.above(-minRelY);

        renderRotatedBlocks(poseStack, bufferSource, schematic, rotationSteps,
                minecraft.getBlockRenderer(),
                adjustedTarget.getX() - cameraPos.x,
                adjustedTarget.getY() - cameraPos.y,
                adjustedTarget.getZ() - cameraPos.z);
    }

    public static void renderGhostBlocksAtOrigin(PoseStack poseStack,
                                                 MultiBufferSource bufferSource,
                                                 SchematicData schematic,
                                                 Minecraft minecraft,
                                                 int rotSteps) {
        if (schematic == null || schematic.getBlocks().isEmpty()) return;

        renderRotatedBlocks(poseStack, bufferSource, schematic, rotSteps,
                minecraft.getBlockRenderer(), 0, 0, 0);
    }

    private static void renderRotatedBlocks(PoseStack poseStack,
                                            MultiBufferSource bufferSource,
                                            SchematicData schematic,
                                            int rotSteps,
                                            BlockRenderDispatcher blockRenderer,
                                            double baseX, double baseY, double baseZ) {
        for (var entry : schematic.getBlocks().entrySet()) {
            BlockState state = entry.getValue();
            if (state.isAir()) continue;

            BlockPos rotated = SchematicUtils.rotatePositionSteps(entry.getKey(), rotSteps);
            BlockState rotatedState = SchematicUtils.rotateBlockStateSteps(state, rotSteps);

            poseStack.pushPose();
            poseStack.translate(baseX + rotated.getX(), baseY + rotated.getY(), baseZ + rotated.getZ());
            try {
                blockRenderer.renderSingleBlock(rotatedState, poseStack, bufferSource,
                        15728880, OverlayTexture.NO_OVERLAY);
            } catch (RuntimeException e) {
                GTCEUTerminalMod.LOGGER.debug("SchematicPreviewRenderer: skipped block render for {}: {}",
                        rotatedState.getBlock().getDescriptionId(), e.getMessage());
            }
            poseStack.popPose();
        }
    }

    public static BlockPos getTargetPlacementPos(Minecraft minecraft, double distance) {
        return SchematicUtils.getTargetPlacementPos(minecraft.player, distance);
    }
}