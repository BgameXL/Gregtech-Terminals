package com.gtceuterminal.client.gui.widget;

import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gtceuterminal.GTCEUTerminalMod;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class MultiblockParadeWidget {

    private static final float ORBIT_RX    = 300f;
    private static final float ORBIT_RY    = 110f;
    private static final float ORBIT_SPEED = (float)(2 * Math.PI / 10_000.0);

    private static final float SCALE_FAR   = 0.25f;
    private static final float SCALE_NEAR  = 1.0f;
    private static final float TARGET_PX   = 110f;

    private static final float ROT_X       = 28f;
    private static final float ROT_Y_BASE  = 0f;

    private static final float DWELL_SECS  = 10f;
    private static final long  FADE_MS     = 700;

    static final class BlockEntry {
        final BlockPos pos;
        final BlockState state;
        final BlockInfo info;
        BlockEntry(BlockPos pos, BlockState state, BlockInfo info) {
            this.pos   = pos;
            this.state = state;
            this.info  = info;
        }
    }

    static final class ParadeEntry {
        final String name;
        final List<BlockEntry> blocks;
        final BlockPos size;
        final BlockPos minPos;
        TrackedDummyWorld dummyWorld;

        ParadeEntry(String name, List<BlockEntry> blocks, BlockPos size, BlockPos minPos) {
            this.name   = name;
            this.blocks = blocks;
            this.size   = size;
            this.minPos = minPos;
        }
    }

    private final List<ParadeEntry> entries = new ArrayList<>();
    private int   currentIdx = 0;
    private long  startMs;
    private long  lastTickMs;
    private float angle = 0f;

    private float guiCenterX = 0;
    private float guiCenterY = 0;

    public MultiblockParadeWidget() {
        startMs = lastTickMs = System.currentTimeMillis();
    }

    public void setGuiCenter(float cx, float cy) {
        guiCenterX = cx;
        guiCenterY = cy;
    }

    public boolean isEmpty() { return entries.isEmpty(); }

    public List<ParadeEntry> getEntries() { return entries; }

    public void addFromShapeInfo(String name, MultiblockShapeInfo shapeInfo) {
        try {
            BlockInfo[][][] b3 = shapeInfo.getBlocks();
            if (b3 == null || b3.length == 0) return;

            List<BlockEntry> blocks = new ArrayList<>();
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

            for (int z = 0; z < b3.length; z++) {
                if (b3[z] == null) continue;
                for (int y = 0; y < b3[z].length; y++) {
                    if (b3[z][y] == null) continue;
                    for (int x = 0; x < b3[z][y].length; x++) {
                        BlockInfo info = b3[z][y][x];
                        if (info == null) continue;
                        BlockState state = info.getBlockState();
                        if (state == null || state.isAir() || state.is(Blocks.AIR)) continue;
                        BlockPos p = new BlockPos(x, y, z);
                        blocks.add(new BlockEntry(p, state, info));
                        minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
                        maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
                    }
                }
            }

            if (blocks.isEmpty()) return;
            entries.add(new ParadeEntry(name, blocks,
                    new BlockPos(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1),
                    new BlockPos(minX, minY, minZ)));
            com.gtceuterminal.GTCEUTerminalMod.LOGGER.debug(
                    "MultiblockParade: loaded '{}' ({} blocks)", name, blocks.size());
        } catch (Exception e) {
            com.gtceuterminal.GTCEUTerminalMod.LOGGER.warn(
                    "MultiblockParade: failed to load '{}': {}", name, e.getMessage());
        }
    }

    public void addFromBlockMap(String name, Map<BlockPos, BlockState> blockMap) {
        if (blockMap == null || blockMap.isEmpty()) return;
        List<BlockEntry> blocks = new ArrayList<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Map.Entry<BlockPos, BlockState> e : blockMap.entrySet()) {
            BlockState s = e.getValue();
            if (s == null || s.isAir()) continue;
            BlockPos p = e.getKey();
            blocks.add(new BlockEntry(p, s, null));
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }
        if (blocks.isEmpty()) return;
        entries.add(new ParadeEntry(name, blocks,
                new BlockPos(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1),
                new BlockPos(minX, minY, minZ)));
    }

    static TrackedDummyWorld getDummyWorld(ParadeEntry entry) {
        if (entry.dummyWorld != null) return entry.dummyWorld;
        entry.dummyWorld = new TrackedDummyWorld();
        java.util.Map<BlockPos, BlockInfo> infoMap = new java.util.HashMap<>();
        for (BlockEntry b : entry.blocks) {
            infoMap.put(b.pos, b.info != null ? b.info : BlockInfo.fromBlockState(b.state));
        }
        entry.dummyWorld.addBlocks(infoMap);
        return entry.dummyWorld;
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (entries.isEmpty() || guiCenterX == 0) return;

        long nowMs   = System.currentTimeMillis();
        long elapsed = nowMs - lastTickMs;
        lastTickMs   = nowMs;

        long totalMs = nowMs - startMs;
        long dwellMs = (long)(DWELL_SECS * 1000);
        int newIdx   = (int)((totalMs / dwellMs) % entries.size());
        if (newIdx != currentIdx) {
            currentIdx = newIdx;
        }

        long posInDwell = totalMs % dwellMs;
        angle = (float)(2 * Math.PI * posInDwell / dwellMs) + (float)Math.toRadians(75);

        float ox    = (float)Math.cos(angle) * ORBIT_RX;
        float oy    = (float)Math.sin(angle) * ORBIT_RY;
        float depth = (float)Math.sin(angle);
        float scale = SCALE_FAR + (SCALE_NEAR - SCALE_FAR) * ((depth + 1f) / 2f);
        float z = 50f + ((depth + 1f) / 2f) * 49f;

        long  pos   = totalMs % dwellMs;
        float alpha = 1f;
        if (pos < FADE_MS)                alpha = pos / (float) FADE_MS;
        else if (pos > dwellMs - FADE_MS) alpha = (dwellMs - pos) / (float) FADE_MS;
        alpha = Math.max(0f, Math.min(1f, alpha));
        if (alpha < 0.01f) return;

        renderMultiblock(graphics, entries.get(currentIdx),
                guiCenterX + ox, guiCenterY + oy, z,
                scale, alpha,
                ROT_Y_BASE + (float)Math.toDegrees(angle) * 0.12f);
    }

    private void renderMultiblock(GuiGraphics graphics, ParadeEntry entry,
                                  float sx, float sy, float sz,
                                  float scale, float alpha, float rotY) {
        Minecraft mc           = Minecraft.getInstance();
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

            pose.translate(sx, sy, sz);

            float maxDim     = Math.max(entry.size.getX(), Math.max(entry.size.getY(), entry.size.getZ()));
            float blockScale = (TARGET_PX * scale) / Math.max(1f, maxDim);

            pose.scale(blockScale, blockScale, blockScale);
            pose.mulPose(Axis.XP.rotationDegrees(ROT_X));
            pose.mulPose(Axis.YP.rotationDegrees(rotY));

            pose.translate(
                    -(entry.minPos.getX() + entry.size.getX() / 2.0),
                    -(entry.minPos.getY() + entry.size.getY() / 2.0),
                    -(entry.minPos.getZ() + entry.size.getZ() / 2.0));

            TrackedDummyWorld dw = getDummyWorld(entry);
            net.minecraft.util.RandomSource rng = net.minecraft.util.RandomSource.create();
            for (BlockEntry block : entry.blocks) {
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
                    GTCEUTerminalMod.LOGGER.debug("MultiblockParadeWidget: tesselate failed for {}, trying renderSingleBlock: {}",
                            block.state.getBlock().getDescriptionId(), e.getMessage());
                    try { r.renderSingleBlock(block.state, pose, buf, 15728880, OverlayTexture.NO_OVERLAY); }
                    catch (RuntimeException e2) {
                        GTCEUTerminalMod.LOGGER.debug("MultiblockParadeWidget: renderSingleBlock also failed for {}: {}",
                                block.state.getBlock().getDescriptionId(), e2.getMessage());
                    }
                }
                pose.popPose();
            }

            buf.endBatch();

        } catch (Exception e) {
            com.gtceuterminal.GTCEUTerminalMod.LOGGER.error(
                    "MultiblockParade: render error: {}", e.getMessage());
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            pose.popPose();
        }
    }
}