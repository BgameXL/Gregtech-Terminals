package com.gtceuterminal.client;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.renderer.SchematicPreviewRenderer;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.data.SchematicStorage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    private static SchematicData cachedClipboard = null;
    private static int cachedClipboardHash = 0;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        MultiblockPreview.render(poseStack, bufferSource, minecraft);

        ItemStack heldItem = minecraft.player.getMainHandItem();
        if (!heldItem.getItem().toString().contains("schematic_interface")) {
            heldItem = minecraft.player.getOffhandItem();
            if (!heldItem.getItem().toString().contains("schematic_interface")) {
                cachedClipboard = null;
                return;
            }
        }

        CompoundTag tag = heldItem.getTag();
        if (!SchematicStorage.hasClipboard(tag)) {
            cachedClipboard = null;
            return;
        }

        try {
            int currentHash = SchematicStorage.clipboardHash(tag);
            if (cachedClipboard == null || currentHash != cachedClipboardHash) {
                cachedClipboard = SchematicData.fromNBT(
                        SchematicStorage.getClipboard(tag), minecraft.level.registryAccess());
                cachedClipboardHash = currentHash;
            }
            if (cachedClipboard == null || cachedClipboard.blocks().isEmpty()) return;

            poseStack.pushPose();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, SchematicPreviewRenderer.GHOST_ALPHA);
            SchematicPreviewRenderer.renderGhostBlocks(
                    poseStack, bufferSource, cachedClipboard, minecraft);
            bufferSource.endBatch();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
            poseStack.popPose();
        } catch (RuntimeException e) {
            GTCEUTerminalMod.LOGGER.warn("ClientEvents: error rendering clipboard hologram: {}", e.getMessage());
        }
    }

}
