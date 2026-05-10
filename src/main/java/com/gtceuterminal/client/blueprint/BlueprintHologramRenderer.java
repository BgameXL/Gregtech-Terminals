package com.gtceuterminal.client.blueprint;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.util.SchematicUtils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Renders a {@link SchematicData} as a translucent hologram at a given world position.
 *
 * Two render modes:
 *
 *   {@code PREVIEW} — neutral blue tint, used in {@link BlueprintViewScreen}
 *       where the camera orbits the schematic.
 *   {@code PLACEMENT} — green tint when placement is valid, red tint when
 *       blocked, used in the in-world WASD placement mode.
 *
 * Rendering is done block-by-block with {@link BlockRenderDispatcher#renderSingleBlock},
 * matching the same approach as the existing {@code SchematicPreviewRenderer} so that
 * block model variations, rotations, and fluid states all render correctly.
 */
public final class BlueprintHologramRenderer {

    public enum Mode { PREVIEW, PLACEMENT_OK, PLACEMENT_BLOCKED }

    // ARGB tint values multiplied into the light color.
    // Full-bright light (0xF000F0) with alpha controlled by blending.
    private static final int LIGHT_FULLBRIGHT = 0xF000F0;

    private BlueprintHologramRenderer() {}

    /**
     * Renders the schematic hologram.
     *
     * @param poseStack    current pose stack (caller manages push/pop)
     * @param bufferSource buffer source for batched rendering
     * @param schematic    schematic to render
     * @param origin       world-space block position of the schematic's (0,0,0) corner
     * @param rotSteps     0–3 clockwise 90° rotation steps
     * @param cameraPos    current camera world position (for translation)
     * @param mode         rendering mode (tint color)
     */
    public static void render(PoseStack poseStack,
                              MultiBufferSource bufferSource,
                              SchematicData schematic,
                              BlockPos origin,
                              int rotSteps,
                              Vec3 cameraPos,
                              Mode mode) {

        if (schematic == null || schematic.getBlocks().isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

        // Enable blending for the translucent tint
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float alpha = (mode == Mode.PREVIEW) ? 0.55f : 0.65f;
        // Tint: blue for preview, green/red for placement
        int overlayColor = switch (mode) {
            case PREVIEW          -> OverlayTexture.NO_OVERLAY;
            case PLACEMENT_OK     -> OverlayTexture.pack(0, false);   // green tint
            case PLACEMENT_BLOCKED -> OverlayTexture.pack(10, true);  // red tint
        };

        for (var entry : schematic.getBlocks().entrySet()) {
            BlockPos   relPos      = entry.getKey();
            BlockState state       = entry.getValue();
            if (state.isAir()) continue;

            BlockPos   rotatedRel  = SchematicUtils.rotatePositionSteps(relPos, rotSteps);
            BlockState rotatedState= SchematicUtils.rotateBlockStateSteps(state, rotSteps);
            BlockPos   worldPos    = origin.offset(rotatedRel);

            poseStack.pushPose();
            poseStack.translate(
                    worldPos.getX() - cameraPos.x,
                    worldPos.getY() - cameraPos.y,
                    worldPos.getZ() - cameraPos.z);

            try {
                dispatcher.renderSingleBlock(
                        rotatedState,
                        poseStack,
                        bufferSource,
                        LIGHT_FULLBRIGHT,
                        overlayColor);
            } catch (RuntimeException e) {
                GTCEUTerminalMod.LOGGER.debug("BlueprintHologramRenderer: skipped block render for {}: {}",
                        rotatedState.getBlock().getDescriptionId(), e.getMessage());
            }

            poseStack.popPose();
        }

        RenderSystem.disableBlend();
    }

    public static Vec3 computeCenter(SchematicData schematic, BlockPos origin) {
        BlockPos sz = schematic.getSize();
        return new Vec3(
                origin.getX() + sz.getX() / 2.0,
                origin.getY() + sz.getY() / 2.0,
                origin.getZ() + sz.getZ() / 2.0);
    }
}