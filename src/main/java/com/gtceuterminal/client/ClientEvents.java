package com.gtceuterminal.client;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.blueprint.BlueprintHologramRenderer;
import com.gtceuterminal.client.gui.planner.PlannerScreen;
import com.gtceuterminal.client.gui.planner.PlannerState;
import com.gtceuterminal.client.renderer.SchematicPreviewRenderer;
import com.gtceuterminal.common.data.SchematicData;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    private static SchematicData cachedClipboard = null;
    private static int cachedClipboardHash = 0;

    public static void enterFreeCamScreen(Minecraft mc,
                                          List<SchematicData> schematics,
                                          int selectedIdx, int rotSteps) {
        mc.setScreen(null);
        PlannerState.enterFreeCam(schematics, selectedIdx, rotSteps);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!PlannerState.freeCamActive) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        switch (event.getKey()) {
            case GLFW.GLFW_KEY_TAB -> {
                PlannerState.exitFreeCam();
                mc.execute(() -> mc.setScreen(
                        new PlannerScreen(
                                PlannerState.freeCamSchematics,
                                PlannerState.freeCamSelectedIdx,
                                PlannerState.freeCamRotSteps
                        )
                ));
            }
            case GLFW.GLFW_KEY_ESCAPE -> PlannerState.exitFreeCam();
            case GLFW.GLFW_KEY_R -> PlannerState.freeCamRotSteps = (PlannerState.freeCamRotSteps + 1) & 3;
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!PlannerState.freeCamActive) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (PlannerState.freeCamSnapPos != null && !PlannerState.freeCamSchematics.isEmpty()) {
                SchematicData sel = PlannerState.freeCamSchematics.get(PlannerState.freeCamSelectedIdx);
                PlannerState.ghosts.add(new PlannerState.PlacedGhost(
                        sel, PlannerState.freeCamSnapPos, PlannerState.freeCamRotSteps));
            }
            event.setCanceled(true);
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (PlannerState.freeCamSnapPos != null) {
                PlannerState.tryRemoveAt(
                        PlannerState.freeCamSnapPos.getX(),
                        PlannerState.freeCamSnapPos.getZ());
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!PlannerState.freeCamActive) return;
        int size = PlannerState.freeCamSchematics.size();
        if (size == 0) return;
        int dir = event.getScrollDelta() > 0 ? -1 : 1;
        PlannerState.freeCamSelectedIdx = (PlannerState.freeCamSelectedIdx + dir + size) % size;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!PlannerState.freeCamActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || PlannerState.cameraEntity == null) return;

        PlannerState.tickFreezePlayer();

        PlannerState.cameraEntity.xo = PlannerState.cameraEntity.getX();
        PlannerState.cameraEntity.yo = PlannerState.cameraEntity.getY();
        PlannerState.cameraEntity.zo = PlannerState.cameraEntity.getZ();
        PlannerState.cameraEntity.xOld = PlannerState.cameraEntity.getX();
        PlannerState.cameraEntity.yOld = PlannerState.cameraEntity.getY();
        PlannerState.cameraEntity.zOld = PlannerState.cameraEntity.getZ();

        float prevYaw = PlannerState.cameraEntity.getYRot();
        float prevPitch = PlannerState.cameraEntity.getXRot();
        float prevHeadYaw = PlannerState.cameraEntity.getYHeadRot();

        PlannerState.cameraEntity.yRotO = prevYaw;
        PlannerState.cameraEntity.xRotO = prevPitch;
        PlannerState.cameraEntity.yHeadRotO = prevHeadYaw;

        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();

        PlannerState.cameraEntity.setYRot(yaw);
        PlannerState.cameraEntity.setXRot(pitch);
        PlannerState.cameraEntity.setYHeadRot(mc.player.getYHeadRot());

        float speed = mc.options.keySprint.isDown() ? 1.5f : 0.5f;
        double yawRad = Math.toRadians(yaw);
        double pitRad = Math.toRadians(pitch);

        double fwdX = -Math.sin(yawRad) * Math.cos(pitRad);
        double fwdY = -Math.sin(pitRad);
        double fwdZ = Math.cos(yawRad) * Math.cos(pitRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        var opts = mc.options;
        double nx = PlannerState.cameraEntity.getX();
        double ny = PlannerState.cameraEntity.getY();
        double nz = PlannerState.cameraEntity.getZ();

        if (opts.keyUp.isDown()) {
            nx += fwdX * speed;
            ny += fwdY * speed;
            nz += fwdZ * speed;
        }
        if (opts.keyDown.isDown()) {
            nx -= fwdX * speed;
            ny -= fwdY * speed;
            nz -= fwdZ * speed;
        }
        if (opts.keyRight.isDown()) {
            nx += rightX * speed;
            nz += rightZ * speed;
        }
        if (opts.keyLeft.isDown()) {
            nx -= rightX * speed;
            nz -= rightZ * speed;
        }
        if (opts.keyJump.isDown()) {
            ny += speed;
        }
        if (opts.keyShift.isDown()) {
            ny -= speed;
        }

        PlannerState.cameraEntity.setPos(nx, ny, nz);

        updateSnapPosition(mc);
    }

    private static void updateSnapPosition(Minecraft mc) {
        if (mc.level == null || PlannerState.cameraEntity == null) {
            PlannerState.freeCamSnapPos = null;
            return;
        }

        Vec3 origin = PlannerState.cameraEntity.getEyePosition();
        Vec3 look = Vec3.directionFromRotation(
                PlannerState.cameraEntity.getXRot(),
                PlannerState.cameraEntity.getYRot()
        );
        Vec3 end = origin.add(look.scale(128.0));

        var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(
                origin,
                end,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                PlannerState.cameraEntity
        ));

        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            PlannerState.freeCamSnapPos = null;
            return;
        }

        BlockPos hitPos = hit.getBlockPos();
        BlockPos placePos = hitPos.relative(hit.getDirection());

        if (!PlannerState.freeCamSchematics.isEmpty()) {
            SchematicData sel = PlannerState.freeCamSchematics.get(PlannerState.freeCamSelectedIdx);
            final int rot = PlannerState.freeCamRotSteps;

            int minRelY = sel.getBlocks().keySet().stream()
                    .mapToInt(p -> {
                        BlockPos rp = rotatePosY(p, rot);
                        return rp.getY();
                    })
                    .min()
                    .orElse(0);

            PlannerState.freeCamSnapPos = new BlockPos(
                    placePos.getX(),
                    placePos.getY() - minRelY,
                    placePos.getZ()
            );
        } else {
            PlannerState.freeCamSnapPos = placePos;
        }
    }

    private static BlockPos rotatePosY(BlockPos pos, int rotSteps) {
        return switch (rotSteps & 3) {
            case 1 -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case 2 -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case 3 -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            default -> pos;
        };
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        if (!PlannerState.ghosts.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Vec3 cam = minecraft.gameRenderer.getMainCamera().getPosition();
            for (PlannerState.PlacedGhost ghost : PlannerState.ghosts) {
                try {
                    poseStack.pushPose();
                    BlockPos origin = ghost.origin;
                    poseStack.translate(
                            origin.getX() - cam.x,
                            origin.getY() - cam.y,
                            origin.getZ() - cam.z);
                    SchematicPreviewRenderer.renderGhostBlocksAtOrigin(
                            poseStack, bufferSource, ghost.schematic, minecraft, ghost.rotSteps);
                    bufferSource.endBatch();
                    poseStack.popPose();
                } catch (RuntimeException e) {
                    GTCEUTerminalMod.LOGGER.warn("ClientEvents: error rendering planner ghost block: {}", e.getMessage());
                }
            }
            RenderSystem.disableBlend();
        }

        if (PlannerState.freeCamActive) return;

        if (minecraft.screen instanceof com.gtceuterminal.client.gui.blueprint.BlueprintViewScreen bvs) {
            Vec3 cam = minecraft.gameRenderer.getMainCamera().getPosition();
            BlueprintHologramRenderer.Mode mode = bvs.isPlaceMode()
                    ? BlueprintHologramRenderer.Mode.PLACEMENT_OK
                    : BlueprintHologramRenderer.Mode.PREVIEW;
            BlockPos origin = bvs.isPlaceMode() ? bvs.getPlacementOrigin() : bvs.getOrbitOrigin();
            poseStack.pushPose();
            com.gtceuterminal.client.blueprint.BlueprintHologramRenderer.render(
                    poseStack, bufferSource, bvs.getSchematic(),
                    origin, bvs.getRotSteps(), cam, mode);
            bufferSource.endBatch();
            poseStack.popPose();
            return;
        }

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