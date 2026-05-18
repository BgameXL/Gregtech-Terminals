package com.gtceuterminal.client.highlight;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Matrix4f;

import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "gtceuterminal", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HighlightRenderer {

    private static final float INSET = 0.002f;

    // Cached geometry: rebuilt only when the set of highlights changes.
    // Stores raw float data: x1,y1,z1, x2,y2,z2, x3,y3,z3, x4,y4,z4, r,g,b per quad.
    private static float[] cachedVerts = null;
    private static int cachedVertCount = 0;
    // Key: snapshot of highlight state used to detect changes.
    private static int cachedHighlightVersion = -1;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Map<BlockPos, MultiblockHighlighter.HighlightInfo> highlights = MultiblockHighlighter.getActiveHighlights();
        if (highlights.isEmpty()) {
            cachedVerts = null;
            cachedVertCount = 0;
            cachedHighlightVersion = -1;
            return;
        }

        int currentVersion = computeVersion(highlights);
        if (currentVersion != cachedHighlightVersion) {
            rebuildCache(highlights);
            cachedHighlightVersion = currentVersion;
        }

        if (cachedVertCount == 0) return;

        float pulse = (System.currentTimeMillis() % 2000) / 2000f;
        float alphaMod = 0.25f + Math.abs((pulse * 2) - 1) * 0.20f;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f mat = ps.last().pose();

        // Replay cached verts with live alpha
        // Layout per quad: 4 × (x, y, z, r, g, b) = 24 floats
        int stride = 6; // floats per vertex
        for (int i = 0; i < cachedVertCount; i++) {
            int base = i * stride;
            buf.vertex(mat,
                            cachedVerts[base],
                            cachedVerts[base + 1],
                            cachedVerts[base + 2])
                    .color(cachedVerts[base + 3], cachedVerts[base + 4], cachedVerts[base + 5], alphaMod)
                    .endVertex();
        }

        ps.popPose();
        tess.end();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void rebuildCache(Map<BlockPos, MultiblockHighlighter.HighlightInfo> highlights) {
        // Count exposed faces first to allocate exactly
        int faceCount = 0;
        for (var hl : highlights.values()) {
            Set<BlockPos> blockSet = hl.blocks;
            for (BlockPos pos : blockSet) {
                for (Direction dir : Direction.values()) {
                    if (!blockSet.contains(pos.relative(dir))) faceCount++;
                }
            }
        }

        // 4 verts per face, 6 floats per vert (x,y,z,r,g,b)
        cachedVerts = new float[faceCount * 4 * 6];
        cachedVertCount = faceCount * 4;
        int idx = 0;

        for (var hl : highlights.values()) {
            Set<BlockPos> blockSet = hl.blocks;
            int col = hl.color;
            float r = ((col >> 16) & 0xFF) / 255f;
            float g = ((col >>  8) & 0xFF) / 255f;
            float b = ( col        & 0xFF) / 255f;

            for (BlockPos pos : blockSet) {
                for (Direction dir : Direction.values()) {
                    if (blockSet.contains(pos.relative(dir))) continue;
                    idx = writeFace(cachedVerts, idx, pos, dir, r, g, b);
                }
            }
        }
    }

    private static int writeFace(float[] buf, int idx, BlockPos pos, Direction dir, float r, float g, float b) {
        float x = pos.getX();
        float y = pos.getY();
        float z = pos.getZ();
        float i = INSET;

        float ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz;

        switch (dir) {
            case UP -> {
                float fy = y + 1 - i;
                ax = x+i;   ay = fy; az = z+i;
                bx = x+i;   by = fy; bz = z+1-i;
                cx = x+1-i; cy = fy; cz = z+1-i;
                dx = x+1-i; dy = fy; dz = z+i;
            }
            case DOWN -> {
                float fy = y + i;
                ax = x+i;   ay = fy; az = z+i;
                bx = x+1-i; by = fy; bz = z+i;
                cx = x+1-i; cy = fy; cz = z+1-i;
                dx = x+i;   dy = fy; dz = z+1-i;
            }
            case NORTH -> {
                float fz = z + i;
                ax = x+i;   ay = y+i;   az = fz;
                bx = x+1-i; by = y+i;   bz = fz;
                cx = x+1-i; cy = y+1-i; cz = fz;
                dx = x+i;   dy = y+1-i; dz = fz;
            }
            case SOUTH -> {
                float fz = z + 1 - i;
                ax = x+i;   ay = y+i;   az = fz;
                bx = x+i;   by = y+1-i; bz = fz;
                cx = x+1-i; cy = y+1-i; cz = fz;
                dx = x+1-i; dy = y+i;   dz = fz;
            }
            case WEST -> {
                float fx = x + i;
                ax = fx; ay = y+i;   az = z+i;
                bx = fx; by = y+1-i; bz = z+i;
                cx = fx; cy = y+1-i; cz = z+1-i;
                dx = fx; dy = y+i;   dz = z+1-i;
            }
            default -> { // EAST
                float fx = x + 1 - i;
                ax = fx; ay = y+i;   az = z+i;
                bx = fx; by = y+i;   bz = z+1-i;
                cx = fx; cy = y+1-i; cz = z+1-i;
                dx = fx; dy = y+1-i; dz = z+i;
            }
        }

        idx = writeVert(buf, idx, ax, ay, az, r, g, b);
        idx = writeVert(buf, idx, bx, by, bz, r, g, b);
        idx = writeVert(buf, idx, cx, cy, cz, r, g, b);
        idx = writeVert(buf, idx, dx, dy, dz, r, g, b);
        return idx;
    }

    private static int writeVert(float[] buf, int idx, float x, float y, float z, float r, float g, float b) {
        buf[idx++] = x;
        buf[idx++] = y;
        buf[idx++] = z;
        buf[idx++] = r;
        buf[idx++] = g;
        buf[idx++] = b;
        return idx;
    }

    // Version based on highlight identity (positions + colors), not geometry details.
    // Good enough to detect add/remove/color changes without hashing every BlockPos.
    private static int computeVersion(Map<BlockPos, MultiblockHighlighter.HighlightInfo> highlights) {
        int h = 0;
        for (var entry : highlights.entrySet()) {
            h = 31 * h + entry.getKey().hashCode();
            h = 31 * h + entry.getValue().color;
            h = 31 * h + entry.getValue().blocks.size();
        }
        return h;
    }
}