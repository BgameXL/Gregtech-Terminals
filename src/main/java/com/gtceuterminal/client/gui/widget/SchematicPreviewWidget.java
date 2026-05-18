package com.gtceuterminal.client.gui.widget;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.SchematicData;

import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.NotNull;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class SchematicPreviewWidget extends WidgetGroup {

    private record SceneCacheEntry(TrackedDummyWorld level, SceneWidget scene) {}

    // Instance-level: destroyed with the widget, no leaks between GUI sessions.
    private final LinkedHashMap<String, SceneCacheEntry> sceneCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SceneCacheEntry> eldest) {
            if (size() > 8) {
                eldest.getValue().scene().setVisible(false);
                removeWidget(eldest.getValue().scene());
                return true;
            }
            return false;
        }
    };

    private SchematicData schematic;
    private int rotSteps = 0;
    private SceneWidget sceneWidget;
    private boolean pendingBuild = true;

    private int blockCount = 0;
    private String displayName = "Multiblock Structure";

    public SchematicPreviewWidget(int x, int y, int width, int height, SchematicData schematic) {
        super(x, y, width, height);
        this.schematic = schematic;
        updateDisplayInfo();
    }

    public void setSchematic(SchematicData schematic) {
        this.schematic = schematic;
        updateDisplayInfo();
        if (sceneWidget != null) {
            sceneWidget.setVisible(false);
            sceneWidget = null;
        }
        pendingBuild = true;
    }

    public void dispose() {
        for (SceneCacheEntry entry : sceneCache.values()) {
            entry.scene().setVisible(false);
        }
        sceneCache.clear();
        sceneWidget = null;
    }

    private void buildScene() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (schematic == null || schematic.getBlocks().isEmpty()) return;

        String cacheKey = schematic.getName() + ":" + rotSteps;
        if (sceneCache.containsKey(cacheKey)) {
            SceneCacheEntry entry = sceneCache.get(cacheKey);
            sceneWidget = entry.scene();
            sceneWidget.setVisible(true);
            if (!widgets.contains(sceneWidget)) addWidget(sceneWidget);
            return;
        }

        Map<BlockPos, BlockState> rotated = buildRotatedBlocks();
        Map<BlockPos, CompoundTag> rotatedBEs = buildRotatedBEs();

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : rotated.keySet()) {
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }
        int sizeX = maxX - minX + 1, sizeY = maxY - minY + 1, sizeZ = maxZ - minZ + 1;
        float maxDim = Math.max(sizeX, Math.max(sizeY, sizeZ));

        TrackedDummyWorld level = new TrackedDummyWorld();

        Map<BlockPos, BlockInfo> infoMap = new HashMap<>();
        for (var e : rotated.entrySet()) {
            if (e.getValue().isAir()) continue;
            BlockInfo info = BlockInfo.fromBlockState(e.getValue());
            CompoundTag nbt = rotatedBEs.get(e.getKey());
            if (nbt != null && !nbt.isEmpty()) info.setTag(nbt);
            infoMap.put(e.getKey(), info);
        }
        level.addBlocks(infoMap);

        com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine ctrl = null;
        for (BlockPos pos : infoMap.keySet()) {
            try {
                var be = level.getBlockEntity(pos);
                if (be instanceof com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity mbe) {
                    mbe.setLevel(level);
                    var machine = mbe.getMetaMachine();
                    if (machine instanceof com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine c) {
                        level.setInnerBlockEntity(mbe);
                        ctrl = c;
                    }
                }
            } catch (RuntimeException e) {
                GTCEUTerminalMod.LOGGER.debug("SchematicPreviewWidget: could not set up controller: {}", e.getMessage());
            }
        }

        if (ctrl != null) {
            try {
                var pat = ctrl.getPattern();
                if (pat != null && pat.checkPatternAt(ctrl.getMultiblockState(), true)) {
                    ctrl.onStructureFormed();
                }
            } catch (RuntimeException e) {
                GTCEUTerminalMod.LOGGER.debug("SchematicPreviewWidget: structure formation failed: {}", e.getMessage());
            }
        }

        int w = getSize().width, h = getSize().height;
        sceneWidget = new SceneWidget(0, 0, w, h, level);
        sceneWidget.useOrtho(true)
                .setOrthoRange(0.5f)
                .setScalable(true)
                .setDraggable(true)
                .setRenderFacing(false)
                .setRenderSelect(false);

        sceneWidget.setCameraYawAndPitch(25, -135);
        sceneWidget.setZoom((float) (3.5 * Math.sqrt(Math.max(maxDim, 1))));
        sceneWidget.setRenderedCore(infoMap.keySet(), null);

        com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(sceneWidget::useCacheBuffer);

        sceneCache.put(cacheKey, new SceneCacheEntry(level, sceneWidget));
        addWidget(sceneWidget);
    }

    private void updateDisplayInfo() {
        if (schematic == null || schematic.getBlocks().isEmpty()) {
            blockCount = 0; displayName = "Multiblock Structure"; return;
        }
        blockCount = (int) schematic.getBlocks().values().stream().filter(s -> !s.isAir()).count();
        for (var e : schematic.getBlocks().entrySet()) {
            String id = e.getValue().getBlock().getDescriptionId().toLowerCase();
            if (id.contains("controller") || id.contains("machine") || id.contains("multiblock") || id.contains("casing")) {
                String n = e.getValue().getBlock().getName().getString();
                displayName = n.length() > 35 ? n.substring(0, 32) + "..." : n; return;
            }
        }
        for (var e : schematic.getBlocks().entrySet()) {
            if (!e.getValue().isAir()) {
                String n = e.getValue().getBlock().getName().getString();
                displayName = n.length() > 35 ? n.substring(0, 32) + "..." : n; return;
            }
        }
        displayName = "Multiblock Structure";
    }

    private Map<BlockPos, BlockState> buildRotatedBlocks() {
        Map<BlockPos, BlockState> r = new HashMap<>();
        if (schematic == null) return r;
        for (var e : schematic.getBlocks().entrySet())
            r.put(rotatePos(e.getKey(), rotSteps), rotateState(e.getValue(), rotSteps));
        return r;
    }

    private Map<BlockPos, CompoundTag> buildRotatedBEs() {
        Map<BlockPos, CompoundTag> r = new HashMap<>();
        if (schematic == null) return r;
        for (var e : schematic.getBlockEntities().entrySet())
            r.put(rotatePos(e.getKey(), rotSteps), e.getValue().copy());
        return r;
    }

    private BlockPos rotatePos(BlockPos p, int steps) {
        for (int i = 0; i < steps; i++) p = new BlockPos(-p.getZ(), p.getY(), p.getX());
        return p;
    }

    private BlockState rotateState(BlockState s, int steps) {
        for (int i = 0; i < steps; i++) {
            var HF = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
            try {
                if (s.hasProperty(HF)) s = s.setValue(HF, s.getValue(HF).getClockWise());
            } catch (IllegalArgumentException e) {
                GTCEUTerminalMod.LOGGER.debug("SchematicPreviewWidget: could not rotate HORIZONTAL_FACING: {}", e.getMessage());
            }
            var F = net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;
            try {
                if (s.hasProperty(F) && s.getValue(F).getAxis().isHorizontal())
                    s = s.setValue(F, s.getValue(F).getClockWise());
            } catch (IllegalArgumentException e) {
                GTCEUTerminalMod.LOGGER.debug("SchematicPreviewWidget: could not rotate FACING: {}", e.getMessage());
            }
            var A = net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
            try {
                if (s.hasProperty(A)) {
                    var ax = s.getValue(A);
                    if (ax == net.minecraft.core.Direction.Axis.X) s = s.setValue(A, net.minecraft.core.Direction.Axis.Z);
                    else if (ax == net.minecraft.core.Direction.Axis.Z) s = s.setValue(A, net.minecraft.core.Direction.Axis.X);
                }
            } catch (IllegalArgumentException e) {
                GTCEUTerminalMod.LOGGER.debug("SchematicPreviewWidget: could not rotate AXIS: {}", e.getMessage());
            }
        }
        return s;
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (pendingBuild) {
            pendingBuild = false;
            buildScene();
        }

        int x = getPosition().x, y = getPosition().y, w = getSize().width, h = getSize().height;
        graphics.fill(x, y, x + w, y + h, 0xFF080808);

        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);

        if (sceneWidget == null) {
            String msg = "No Schematic";
            int tw = Minecraft.getInstance().font.width(msg);
            graphics.drawString(Minecraft.getInstance().font, msg,
                    x + (w - tw) / 2, y + h / 2 - 4, 0xFF888888, false);
        }

        graphics.fill(x, y, x + w, y + 1, 0xFF555555);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        graphics.fill(x, y, x + 1, y + h, 0xFF555555);
        graphics.fill(x + w - 1, y, x + w, y + h, 0xFF555555);
    }

    @Override
    public boolean mouseWheelMove(double mx, double my, double delta) {
        if (!isMouseOverElement(mx, my)) return super.mouseWheelMove(mx, my, delta);

        if (!net.minecraft.client.gui.screens.Screen.hasControlDown()) {
            if (sceneWidget != null) {
                float currentPitch = sceneWidget.getRotationPitch();
                sceneWidget.setCameraYawAndPitch(sceneWidget.getRotationYaw(), currentPitch + (delta > 0 ? 90f : -90f));
            }
            return true;
        }

        return super.mouseWheelMove(mx, my, delta);
    }

    public int    getRotSteps()              { return rotSteps; }
    public int    getBlockCount()            { return blockCount; }
    public String getMultiblockDisplayName() { return displayName; }
}