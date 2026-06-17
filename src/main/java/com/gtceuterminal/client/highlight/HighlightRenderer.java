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
    private static int cachedHighlightVersion = -1;
    float alphaMod = 1.0f;

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
        float alphaMod = 0.55f + Math.abs((pulse * 2) - 1) * 0.35f;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.lineWidth(2.5f);

        alphaMod = Math.min(alphaMod, 1.0f);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f mat = ps.last().pose();

        // Replay cached line verts with live alpha.
        // Layout: pairs of vertices, each (x, y, z, r, g, b) = 6 floats.
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

        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void rebuildCache(Map<BlockPos, MultiblockHighlighter.HighlightInfo> highlights) {
        int faceCount = 0;
        for (var hl : highlights.values()) {
            Set<BlockPos> blockSet = hl.blocks;
            for (BlockPos pos : blockSet) {
                for (Direction dir : Direction.values()) {
                    if (!blockSet.contains(pos.relative(dir))) faceCount++;
                }
            }
        }

        // Worst case: each exposed face contributes 4 edges = 4 line segments = 8 verts,
        // 6 floats per vert (x,y,z,r,g,b). Most edges are culled (only the silhouette/
        // contour edges survive), so the actual vertex count is tracked separately.
        cachedVerts = new float[faceCount * 8 * 6];
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
                    idx = writeFace(cachedVerts, idx, blockSet, pos, dir, r, g, b);
                }
            }
        }

        cachedVertCount = idx / 6;
    }

    private static int writeFace(float[] buf, int idx, Set<BlockPos> blockSet,
                                 BlockPos pos, Direction dir, float r, float g, float b) {
        float h = 0.5f - INSET;
        float cx = pos.getX() + 0.5f;
        float cy = pos.getY() + 0.5f;
        float cz = pos.getZ() + 0.5f;

        Direction.Axis nAxis = dir.getAxis();
        float fcx = cx + dir.getStepX() * h;
        float fcy = cy + dir.getStepY() * h;
        float fcz = cz + dir.getStepZ() * h;

        for (Direction s : Direction.values()) {
            if (s.getAxis() == nAxis) continue;

            BlockPos sideNeighbor = pos.relative(s);
            if (blockSet.contains(sideNeighbor) && !blockSet.contains(sideNeighbor.relative(dir))) {
                continue;
            }

            Direction.Axis perpAxis = perpendicularAxis(nAxis, s.getAxis());
            float px = perpAxis == Direction.Axis.X ? 1f : 0f;
            float py = perpAxis == Direction.Axis.Y ? 1f : 0f;
            float pz = perpAxis == Direction.Axis.Z ? 1f : 0f;

            float ex = fcx + s.getStepX() * h;
            float ey = fcy + s.getStepY() * h;
            float ez = fcz + s.getStepZ() * h;

            idx = writeVert(buf, idx, ex + px * h, ey + py * h, ez + pz * h, r, g, b);
            idx = writeVert(buf, idx, ex - px * h, ey - py * h, ez - pz * h, r, g, b);
        }
        return idx;
    }

    private static Direction.Axis perpendicularAxis(Direction.Axis a, Direction.Axis b) {
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (axis != a && axis != b) return axis;
        }
        return Direction.Axis.X;
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