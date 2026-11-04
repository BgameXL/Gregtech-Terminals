package com.gtceuterminal.client;

import com.gtceuterminal.client.renderer.SchematicPreviewRenderer;
import com.gtceuterminal.common.data.SchematicData;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class MultiblockPreview {

    private static final float ALPHA = 0.7f;
    private static final float BLOCK_SCALE = 0.875f;
    private static SchematicData active;
    private static BlockPos origin;
    private static long expiryGameTime;

    private MultiblockPreview() {
    }

    public static void show(Level level, BlockPos controllerPos, SchematicData ghost, int durationTicks) {
        if (level == null || ghost == null || ghost.blocks().isEmpty()) {
            clear();
            return;
        }
        active = ghost;
        origin = controllerPos;
        expiryGameTime = level.getGameTime() + durationTicks;
    }

    public static void clear() {
        active = null;
        origin = null;
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, Minecraft minecraft) {
        if (active == null || origin == null || minecraft.level == null) return;
        if (minecraft.level.getGameTime() > expiryGameTime) {
            clear();
            return;
        }

        poseStack.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, ALPHA);
        SchematicPreviewRenderer.renderGhostBlocksAt(poseStack, bufferSource, active, minecraft, origin, 0, BLOCK_SCALE, true);
        if (bufferSource instanceof MultiBufferSource.BufferSource batched) batched.endBatch();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }
}