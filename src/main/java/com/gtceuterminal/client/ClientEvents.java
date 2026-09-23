package com.gtceuterminal.client;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.renderer.SchematicPreviewRenderer;
import com.gtceuterminal.common.data.SchematicData;

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

        ItemStack heldItem = minecraft.player.getMainHandItem();
        if (!heldItem.getItem().toString().contains("schematic_interface")) {
            heldItem = minecraft.player.getOffhandItem();
            if (!heldItem.getItem().toString().contains("schematic_interface")) {
                cachedClipboard = null;
                return;
            }
        }

        CompoundTag tag = heldItem.getTag();
        if (tag == null || !tag.contains("Clipboard")) {
            cachedClipboard = null;
            return;
        }

        try {
            CompoundTag clipboardTag = tag.getCompound("Clipboard");
            int currentHash = clipboardTag.hashCode();
            if (cachedClipboard == null || currentHash != cachedClipboardHash) {
                cachedClipboard = SchematicData.fromNBT(clipboardTag, minecraft.level.registryAccess());
                cachedClipboardHash = currentHash;
            }
            if (cachedClipboard == null || cachedClipboard.getBlocks().isEmpty()) return;

            poseStack.pushPose();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            SchematicPreviewRenderer.renderGhostBlocks(
                    poseStack, bufferSource, cachedClipboard, minecraft, clipboardTag);
            bufferSource.endBatch();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
            poseStack.popPose();
        } catch (RuntimeException e) {
            GTCEUTerminalMod.LOGGER.warn("ClientEvents: error rendering clipboard hologram: {}", e.getMessage());
        }
    }

    private static com.gtceuterminal.client.gui.widget.MultiblockParadeWidget activeParade = null;
    private static com.gtceuterminal.client.gui.widget.BouncingParadeRenderer activeBouncingParade = null;

    public static void setActiveParade(
            com.gtceuterminal.client.gui.widget.MultiblockParadeWidget parade,
            com.gtceuterminal.common.theme.ItemTheme.ParadeMode mode) {
        activeParade = null;
        activeBouncingParade = null;
        if (parade == null || parade.isEmpty()) return;

        if (mode == com.gtceuterminal.common.theme.ItemTheme.ParadeMode.NONE) {
        } else if (mode == com.gtceuterminal.common.theme.ItemTheme.ParadeMode.BOUNCING) {
            com.gtceuterminal.client.gui.widget.BouncingParadeRenderer bouncer =
                    new com.gtceuterminal.client.gui.widget.BouncingParadeRenderer();
            bouncer.setEntries(parade.getEntries());
            activeBouncingParade = bouncer;
        } else {
            activeParade = parade;
        }
    }

    public static void clearActiveParade() {
        activeParade = null;
        activeBouncingParade = null;
    }

    @SubscribeEvent
    public static void onContainerRenderForeground(
            net.minecraftforge.client.event.ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof
                com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer)) return;
        if (activeParade != null && !activeParade.isEmpty())
            activeParade.render(event.getGuiGraphics(), 0f);
        if (activeBouncingParade != null && !activeBouncingParade.isEmpty())
            activeBouncingParade.render(event.getGuiGraphics(), 0f);
    }

    @SubscribeEvent
    public static void onScreenClose(net.minecraftforge.client.event.ScreenEvent.Closing event) {
        if (event.getScreen() instanceof
                com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer) {
            clearActiveParade();
        }
    }
}
