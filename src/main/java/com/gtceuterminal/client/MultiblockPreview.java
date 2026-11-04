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
        RenderSystem.setShaderColor(1f, 1f, 1f, SchematicPreviewRenderer.GHOST_ALPHA);
        SchematicPreviewRenderer.renderGhostBlocksAt(poseStack, bufferSource, active, minecraft, origin, 0,
                SchematicPreviewRenderer.GHOST_SCALE, true);
        if (bufferSource instanceof MultiBufferSource.BufferSource batched) batched.endBatch();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }
}