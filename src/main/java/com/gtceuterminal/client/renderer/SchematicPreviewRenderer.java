package com.gtceuterminal.client.renderer;

import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.util.SchematicUtils;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class SchematicPreviewRenderer {

    public static final float GHOST_ALPHA = 0.7f;
    public static final float GHOST_SCALE = 1.0f;

    public static void renderGhostBlocks(GhostRenderCache cache, PoseStack poseStack,
                                         SchematicData schematic, Minecraft minecraft) {
        if (schematic == null || schematic.blocks().isEmpty()
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

        cache.draw(poseStack, schematic, rotationSteps,
                adjustedTarget.getX() - cameraPos.x,
                adjustedTarget.getY() - cameraPos.y,
                adjustedTarget.getZ() - cameraPos.z);
    }

    public static void renderGhostBlocksAt(GhostRenderCache cache, PoseStack poseStack,
                                           SchematicData schematic, Minecraft minecraft,
                                           BlockPos origin, int rotSteps) {
        if (schematic == null || schematic.blocks().isEmpty() || minecraft.level == null) return;

        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        cache.draw(poseStack, schematic, rotSteps,
                origin.getX() - cameraPos.x,
                origin.getY() - cameraPos.y,
                origin.getZ() - cameraPos.z);
    }
}