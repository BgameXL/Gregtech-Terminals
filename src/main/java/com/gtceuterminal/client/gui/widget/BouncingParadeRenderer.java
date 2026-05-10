package com.gtceuterminal.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class BouncingParadeRenderer {

    private static final long  LIFE_MS   = 10_000;
    private static final long  FADE_MS   = 800;
    private static final float SIZE_PX   = 96f;
    private static final float ROT_X     = 28f;
    private static final float ROT_Y_BASE = 0f;

    private List<MultiblockParadeWidget.ParadeEntry> entries;
    private int   currentIdx  = 0;
    private long  spawnMs     = 0;
    private long  lastTickMs  = 0;

    private float x, y;
    private float vx, vy;
    private float rotY = ROT_Y_BASE;
    private float rotSpeed;

    private final Random rng = new Random();

    public BouncingParadeRenderer() {}

    public void setEntries(List<MultiblockParadeWidget.ParadeEntry> entries) {
        this.entries = entries;
    }
    public boolean isEmpty() {
        return entries == null || entries.isEmpty();
    }

    private void initPhysics(float minX, float minY, float maxX, float maxY) {
        float margin = SIZE_PX / 2f + 8;
        x = minX + margin + rng.nextFloat() * (maxX - minX - margin * 2);
        y = minY + margin + rng.nextFloat() * (maxY - minY - margin * 2);

        float speed = 0.04f + rng.nextFloat() * 0.05f;
        float angle = rng.nextFloat() * (float)(2 * Math.PI);
        vx = (float)Math.cos(angle) * speed;
        vy = (float)Math.sin(angle) * speed;

        rotSpeed = (15f + rng.nextFloat() * 25f) / 1000f;
        if (rng.nextBoolean()) rotSpeed = -rotSpeed;
        rotY = ROT_Y_BASE + rng.nextFloat() * 360f;

        spawnMs    = System.currentTimeMillis();
        lastTickMs = spawnMs;
    }

    private void nextEntry() {
        currentIdx = (currentIdx + 1) % entries.size();
        spawnMs    = System.currentTimeMillis();
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int offsetX = 0, offsetY = 0;
        net.minecraft.client.gui.screens.Screen screen = mc.screen;
        if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> cs) {
            try {
                java.lang.reflect.Field fLeft = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class
                        .getDeclaredField("leftPos");
                java.lang.reflect.Field fTop  = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class
                        .getDeclaredField("topPos");
                fLeft.setAccessible(true);
                fTop.setAccessible(true);
                offsetX = (int) fLeft.get(cs);
                offsetY = (int) fTop.get(cs);
            } catch (Exception e) {
                offsetX = (sw - 380) / 2;
                offsetY = (sh - 300) / 2;
            }
        }

        float minX = -offsetX;
        float minY = -offsetY;
        float maxX = sw - offsetX;
        float maxY = sh - offsetY;

        long nowMs   = System.currentTimeMillis();
        long elapsed = nowMs - lastTickMs;
        lastTickMs   = nowMs;

        long age = nowMs - spawnMs;

        if (spawnMs == 0) {
            initPhysics(minX, minY, maxX, maxY);
            age = 0;
        } else if (age >= LIFE_MS) {
            nextEntry();
            age = 0;
        }

        x += vx * elapsed;
        y += vy * elapsed;
        rotY += rotSpeed * elapsed;

        float margin = SIZE_PX / 2f;
        if (x - margin < minX) { x = minX + margin; vx =  Math.abs(vx); }
        if (x + margin > maxX) { x = maxX - margin; vx = -Math.abs(vx); }
        if (y - margin < minY) { y = minY + margin; vy =  Math.abs(vy); }
        if (y + margin > maxY) { y = maxY - margin; vy = -Math.abs(vy); }

        float alpha = 1f;
        if (age < FADE_MS)              alpha = age / (float) FADE_MS;
        else if (age > LIFE_MS - FADE_MS) alpha = (LIFE_MS - age) / (float) FADE_MS;
        alpha = Math.max(0f, Math.min(1f, alpha));
        if (alpha < 0.01f) return;

        renderBlock(graphics, entries.get(currentIdx), x, y, alpha, rotY);
    }

    private void renderBlock(GuiGraphics graphics, MultiblockParadeWidget.ParadeEntry entry,
                             float sx, float sy, float alpha, float rotY) {
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher r = mc.getBlockRenderer();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

            pose.translate(sx, sy, 99f);

            float maxDim    = Math.max(entry.size.getX(),
                    Math.max(entry.size.getY(), entry.size.getZ()));
            float blockScale = SIZE_PX / Math.max(1f, maxDim);

            pose.scale(blockScale, blockScale, blockScale);
            pose.mulPose(Axis.ZP.rotationDegrees(180f));
            pose.mulPose(Axis.XP.rotationDegrees(ROT_X));
            pose.mulPose(Axis.YP.rotationDegrees(rotY));
            pose.translate(
                    -(entry.minPos.getX() + entry.size.getX() / 2.0),
                    -(entry.minPos.getY() + entry.size.getY() / 2.0),
                    -(entry.minPos.getZ() + entry.size.getZ() / 2.0));

            TrackedDummyWorld dw = MultiblockParadeWidget.getDummyWorld(entry);
            net.minecraft.util.RandomSource rng = net.minecraft.util.RandomSource.create();
            for (MultiblockParadeWidget.BlockEntry block : entry.blocks) {
                pose.pushPose();
                pose.translate(block.pos.getX(), block.pos.getY(), block.pos.getZ());
                try {
                    var model = r.getBlockModel(block.state);
                    net.minecraftforge.client.model.data.ModelData base =
                            net.minecraftforge.client.model.data.ModelData.EMPTY;
                    var be = dw.getExistingBlockEntity(block.pos);
                    if (be != null) base = be.getModelData();
                    var modelData = model.getModelData(dw, block.pos, block.state, base);
                    for (net.minecraft.client.renderer.RenderType rt :
                            model.getRenderTypes(block.state, rng, modelData)) {
                        r.getModelRenderer().tesselateBlock(
                                dw, model, block.state, block.pos, pose,
                                buf.getBuffer(rt), true, rng,
                                block.state.getSeed(block.pos),
                                OverlayTexture.NO_OVERLAY, modelData, rt);
                    }
                } catch (RuntimeException e) {
                    com.gtceuterminal.GTCEUTerminalMod.LOGGER.debug("BouncingParadeRenderer: tesselate failed for {}, trying renderSingleBlock: {}",
                            block.state.getBlock().getDescriptionId(), e.getMessage());
                    try { r.renderSingleBlock(block.state, pose, buf, 15728880, OverlayTexture.NO_OVERLAY); }
                    catch (RuntimeException e2) {
                        com.gtceuterminal.GTCEUTerminalMod.LOGGER.debug("BouncingParadeRenderer: renderSingleBlock also failed for {}: {}",
                                block.state.getBlock().getDescriptionId(), e2.getMessage());
                    }
                }
                pose.popPose();
            }

            buf.endBatch();

        } catch (Exception e) {
            com.gtceuterminal.GTCEUTerminalMod.LOGGER.error(
                    "BouncingParade: render error: {}", e.getMessage());
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            pose.popPose();
        }
    }
}