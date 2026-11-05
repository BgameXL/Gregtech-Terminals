package com.gtceuterminal.client.renderer;

import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.util.SchematicUtils;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public final class GhostRenderCache {

    private static final double RESORT_DISTANCE_SQR = 1.0;

    private SchematicData requestedSchematic;
    private int requestedRotSteps = Integer.MIN_VALUE;
    private SchematicData builtSchematic;
    private int builtRotSteps = Integer.MIN_VALUE;
    private VertexBuffer buffer;
    private Thread thread;

    private BufferBuilder builder;
    private BufferBuilder.SortState sortState;
    private boolean dirtySort;
    private double lastSortX, lastSortY, lastSortZ;

    void draw(PoseStack poseStack, SchematicData schematic, int rotSteps, double dx, double dy, double dz) {
        ensure(schematic, rotSteps);
        if (buffer == null || schematic != builtSchematic || rotSteps != builtRotSteps) return;
        ShaderInstance shader = GameRenderer.getRendertypeTranslucentShader();
        if (shader == null) return;

        resortIfNeeded(-dx, -dy, -dz);

        RenderType layer = RenderType.translucent();
        layer.setupRenderState();
        RenderSystem.setShaderColor(1f, 1f, 1f, SchematicPreviewRenderer.GHOST_ALPHA);
        RenderSystem.polygonOffset(-1f, -1f);
        RenderSystem.enablePolygonOffset();

        Matrix4f modelView = new Matrix4f(poseStack.last().pose());
        modelView.translate((float) dx, (float) dy, (float) dz);

        buffer.bind();
        buffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), shader);
        VertexBuffer.unbind();

        RenderSystem.disablePolygonOffset();
        RenderSystem.polygonOffset(0f, 0f);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        layer.clearRenderState();
    }

    private void resortIfNeeded(double localCamX, double localCamY, double localCamZ) {
        if (builder == null || sortState == null) return;
        if (!dirtySort) {
            double dxx = localCamX - lastSortX, dyy = localCamY - lastSortY, dzz = localCamZ - lastSortZ;
            if (dxx * dxx + dyy * dyy + dzz * dzz < RESORT_DISTANCE_SQR) return;
        }
        try {
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            builder.restoreSortState(sortState);
            builder.setQuadSorting(VertexSorting.byDistance((float) localCamX, (float) localCamY, (float) localCamZ));
            sortState = builder.getSortState();
            BufferBuilder.RenderedBuffer resorted = builder.end();
            buffer.bind();
            buffer.upload(resorted);
            VertexBuffer.unbind();
            dirtySort = false;
            lastSortX = localCamX;
            lastSortY = localCamY;
            lastSortZ = localCamZ;
        } catch (RuntimeException ignored) {
        }
    }

    private void ensure(SchematicData schematic, int rotSteps) {
        if (schematic == requestedSchematic && rotSteps == requestedRotSteps) return;
        requestedSchematic = schematic;
        requestedRotSteps = rotSteps;

        Map<BlockPos, BlockState> rotated = new HashMap<>();
        Map<BlockPos, BlockInfo> infos = new HashMap<>();
        for (var entry : schematic.blocks().entrySet()) {
            BlockState state = entry.getValue();
            if (state.isAir()) continue;
            BlockPos pos = SchematicUtils.rotatePositionSteps(entry.getKey(), rotSteps);
            BlockState rotatedState = SchematicUtils.rotateBlockStateSteps(state, rotSteps);
            rotated.put(pos, rotatedState);
            infos.put(pos, BlockInfo.fromBlockState(rotatedState));
        }
        TrackedDummyWorld world = new TrackedDummyWorld();
        world.addBlocks(infos);

        startCompile(schematic, rotSteps, rotated, world);
    }

    private void startCompile(SchematicData schematic, int rotSteps,
                              Map<BlockPos, BlockState> blocks, TrackedDummyWorld world) {
        if (thread != null) thread.interrupt();
        thread = new Thread(() -> tesse(schematic, rotSteps, blocks, world), "gtceuterminal-ghost-compile");
        thread.setDaemon(true);
        thread.start();
    }

    private void tesse(SchematicData schematic, int rotSteps,
                       Map<BlockPos, BlockState> blocks, TrackedDummyWorld world) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = RandomSource.create();
        PoseStack poseStack = new PoseStack();
        BufferBuilder localBuilder = new BufferBuilder(2048);
        localBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

        ModelBlockRenderer.enableCaching();
        try {
            for (var entry : blocks.entrySet()) {
                if (Thread.interrupted()) return;
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();
                BakedModel model = dispatcher.getBlockModel(state);
                ModelData data = model.getModelData(world, pos, state, ModelData.EMPTY);

                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                if (SchematicPreviewRenderer.GHOST_SCALE != 1f) {
                    poseStack.translate(0.5, 0.5, 0.5);
                    poseStack.scale(SchematicPreviewRenderer.GHOST_SCALE, SchematicPreviewRenderer.GHOST_SCALE,
                            SchematicPreviewRenderer.GHOST_SCALE);
                    poseStack.translate(-0.5, -0.5, -0.5);
                }
                for (RenderType renderLayer : model.getRenderTypes(state, random, data)) {
                    dispatcher.renderBatched(state, pos, world, poseStack, localBuilder, true, random, data, renderLayer);
                }
                poseStack.popPose();
            }
        } catch (RuntimeException ignored) {
            return;
        } finally {
            ModelBlockRenderer.clearCache();
        }

        if (Thread.interrupted()) return;
        localBuilder.setQuadSorting(VertexSorting.byDistance(0f, 0f, 0f));
        BufferBuilder.SortState capturedSort = localBuilder.getSortState();
        final BufferBuilder.RenderedBuffer rendered;
        try {
            rendered = localBuilder.end();
        } catch (RuntimeException e) {
            return;
        }
        RenderSystem.recordRenderCall(() -> applyUpload(schematic, rotSteps, rendered, localBuilder, capturedSort));
    }

    private void applyUpload(SchematicData schematic, int rotSteps, BufferBuilder.RenderedBuffer rendered,
                             BufferBuilder newBuilder, BufferBuilder.SortState newSortState) {
        if (schematic != requestedSchematic || rotSteps != requestedRotSteps) {
            rendered.release();
            return;
        }
        if (buffer != null) buffer.close();
        VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vbo.bind();
        vbo.upload(rendered);
        VertexBuffer.unbind();
        buffer = vbo;
        builder = newBuilder;
        sortState = newSortState;
        dirtySort = true;
        builtSchematic = schematic;
        builtRotSteps = rotSteps;
    }
}