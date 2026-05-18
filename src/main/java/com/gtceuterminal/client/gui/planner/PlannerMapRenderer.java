package com.gtceuterminal.client.gui.planner;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.util.SchematicUtils;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
final class PlannerMapRenderer {

    private static final int C_GRID        = 0x18FFFFFF;
    private static final int C_GRID_CHUNK  = 0x30FFFFFF;
    private static final int C_PLAYER      = 0xFFFFCC00;
    private static final int C_GHOST_PLACE = 0x8000E676;
    private static final int C_GHOST_SAVED = 0x804FC3FF;
    private static final int C_GHOST_BDR   = 0xFF00E676;

    private static final int MAX_QUERIES_PER_FRAME = 64;

    private final HashMap<Long, Integer> tileCache     = new HashMap<>(4096);
    private final HashMap<String, List<int[]>> footprintCache = new HashMap<>();
    private int queriesThisFrame = 0;

    void beginFrame(double camX, double camZ, float zoom, float tilt, int screenW, int screenH, int sidebarW, int hudH) {
        queriesThisFrame = 0;
        evictTileCache(camX, camZ, zoom, tilt, screenW, screenH);
    }

    void renderWorldTiles(GuiGraphics g, double camX, double camZ, float zoom, float tilt,
                          int screenW, int screenH, int sidebarW) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        float tiltRad = (float) Math.toRadians(tilt);
        float sinT    = (float) Math.sin(tiltRad);
        int   cellW   = Math.max(1, (int) zoom);
        int   cellH   = Math.max(1, (int)(zoom * sinT));

        int rawBlocksX = (screenW - sidebarW) / cellW + 4;
        int rawBlocksZ = screenH / cellH + 4;
        int blocksX = Math.min(rawBlocksX, 256);
        int blocksZ = Math.min(rawBlocksZ, 256);
        int startX  = (int)(camX - blocksX / 2.0);
        int startZ  = (int)(camZ - blocksZ / 2.0);

        for (int dz = 0; dz <= blocksZ; dz++) {
            for (int dx = 0; dx <= blocksX; dx++) {
                int wx = startX + dx;
                int wz = startZ + dz;
                int[] sc = worldToScreen(wx, wz, camX, camZ, zoom, tilt, screenW, screenH, sidebarW);
                if (sc[0] + cellW < sidebarW || sc[0] > screenW) continue;
                if (sc[1] + cellH < 0 || sc[1] > screenH) continue;
                int color = getTileColor(level, wx, wz);
                g.fill(sc[0], sc[1], sc[0] + cellW, sc[1] + cellH, color);
            }
        }
    }

    private int getTileColor(Level level, int wx, int wz) {
        long key = ((long) wx << 32) | (wz & 0xFFFFFFFFL);
        Integer cached = tileCache.get(key);
        if (cached != null) return cached;
        if (queriesThisFrame >= MAX_QUERIES_PER_FRAME) return 0xFF1A1A1A;

        int color = 0xFF1A1A1A;
        try {
            queriesThisFrame++;
            int y = level.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
            if (y >= level.getMinBuildHeight()) {
                BlockState state = level.getBlockState(new BlockPos(wx, y, wz));
                MapColor mc = state.getMapColor(level, new BlockPos(wx, y, wz));
                if (mc != MapColor.NONE) {
                    int r = (mc.col >> 16) & 0xFF;
                    int gv = (mc.col >> 8)  & 0xFF;
                    int b  =  mc.col        & 0xFF;
                    color = 0xFF000000 | ((r * 3 / 4) << 16) | ((gv * 3 / 4) << 8) | (b * 3 / 4);
                }
            }
        } catch (RuntimeException e) {
            GTCEUTerminalMod.LOGGER.debug("PlannerMapRenderer: error reading map color at ({},{}): {}", wx, wz, e.getMessage());
        }
        tileCache.put(key, color);
        return color;
    }

    private void evictTileCache(double camX, double camZ, float zoom, float tilt, int screenW, int screenH) {
        if (tileCache.size() <= 40_000) return;
        float tiltRad = (float) Math.toRadians(tilt);
        float sinT    = Math.max(0.01f, (float) Math.sin(tiltRad));
        int   cellW   = Math.max(1, (int) PlannerScreen.ZOOM_MIN);
        int   cellH   = Math.max(1, (int)(PlannerScreen.ZOOM_MIN * sinT));
        int   margin  = 64;
        int halfBX = (screenW / cellW) / 2 + margin;
        int halfBZ = (screenH / cellH) / 2 + margin;
        int minX = (int) camX - halfBX, maxX = (int) camX + halfBX;
        int minZ = (int) camZ - halfBZ, maxZ = (int) camZ + halfBZ;
        tileCache.keySet().removeIf(k -> {
            int kx = (int)(k >> 32);
            int kz = (int)(k & 0xFFFFFFFFL);
            return kx < minX || kx > maxX || kz < minZ || kz > maxZ;
        });
    }

    void renderGrid(GuiGraphics g, double camX, double camZ, float zoom, float tilt,
                    int screenW, int screenH, int sidebarW) {
        if (zoom < 4f) return;
        float tiltRad = (float) Math.toRadians(tilt);
        float sinT    = (float) Math.sin(tiltRad);
        int   cellW   = (int) zoom;
        int   cellH   = Math.max(1, (int)(zoom * sinT));

        int blocksX = (screenW - sidebarW) / cellW + 4;
        int blocksZ = screenH / cellH + 4;
        int startX  = (int)(camX - blocksX / 2.0);
        int startZ  = (int)(camZ - blocksZ / 2.0);

        for (int dx = 0; dx <= blocksX; dx++) {
            int wx = startX + dx;
            int sx = worldToScreen(wx, (int) camZ, camX, camZ, zoom, tilt, screenW, screenH, sidebarW)[0];
            if (sx < sidebarW || sx > screenW) continue;
            g.fill(sx, 0, sx + 1, screenH, wx % 16 == 0 ? C_GRID_CHUNK : C_GRID);
        }
        for (int dz = 0; dz <= blocksZ; dz++) {
            int wz = startZ + dz;
            int sy = worldToScreen((int) camX, wz, camX, camZ, zoom, tilt, screenW, screenH, sidebarW)[1];
            if (sy < 0 || sy > screenH) continue;
            g.fill(sidebarW, sy, screenW, sy + 1, wz % 16 == 0 ? C_GRID_CHUNK : C_GRID);
        }
    }

    void renderPlacedGhosts(GuiGraphics g, double camX, double camZ, float zoom, float tilt,
                            int screenW, int screenH, int sidebarW) {
        for (PlannerState.PlacedGhost ghost : PlannerState.ghosts) {
            renderFootprint(g, ghost.schematic, ghost.origin.getX(), ghost.origin.getZ(),
                    ghost.rotSteps, C_GHOST_SAVED, 0xFF4FC3FF,
                    camX, camZ, zoom, tilt, screenW, screenH, sidebarW);
        }
    }

    void renderCursorFootprint(GuiGraphics g, int mouseX, int mouseY,
                               int selectedIdx, List<SchematicData> schematics,
                               double camX, double camZ, float zoom, float tilt,
                               int screenW, int screenH, int sidebarW, int rotSteps) {
        if (mouseX <= sidebarW) return;
        if (selectedIdx < 0 || selectedIdx >= schematics.size()) return;
        int[] world = screenToWorld(mouseX, mouseY, camX, camZ, zoom, tilt, screenW, screenH, sidebarW);
        renderFootprint(g, schematics.get(selectedIdx), world[0], world[1],
                rotSteps, C_GHOST_PLACE, C_GHOST_BDR,
                camX, camZ, zoom, tilt, screenW, screenH, sidebarW);
    }

    private void renderFootprint(GuiGraphics g, SchematicData schematic,
                                 int originX, int originZ, int rot,
                                 int fillColor, int borderColor,
                                 double camX, double camZ, float zoom, float tilt,
                                 int screenW, int screenH, int sidebarW) {
        List<int[]> cells = getFootprint(schematic, rot);
        int cellW = Math.max(1, (int) zoom);
        int cellH = Math.max(1, (int)(zoom * Math.sin(Math.toRadians(tilt))));

        for (int[] cell : cells) {
            int wx = originX + cell[0];
            int wz = originZ + cell[1];
            int[] sc = worldToScreen(wx, wz, camX, camZ, zoom, tilt, screenW, screenH, sidebarW);
            int sx = sc[0], sy = sc[1];
            if (sx + cellW < sidebarW || sx > screenW) continue;
            if (sy + cellH < 0 || sy > screenH) continue;

            g.fill(sx, sy, sx + cellW, sy + cellH, fillColor);
            if (zoom >= 5f) {
                g.fill(sx,             sy,             sx + cellW, sy + 1,     borderColor);
                g.fill(sx,             sy + cellH - 1, sx + cellW, sy + cellH, borderColor);
                g.fill(sx,             sy,             sx + 1,     sy + cellH, borderColor);
                g.fill(sx + cellW - 1, sy,             sx + cellW, sy + cellH, borderColor);
            }
        }
    }

    private List<int[]> getFootprint(SchematicData schematic, int rot) {
        String key = System.identityHashCode(schematic) + ":" + rot;
        return footprintCache.computeIfAbsent(key, k -> {
            Set<Long> seen = new HashSet<>();
            List<int[]> cells = new ArrayList<>();
            for (Map.Entry<BlockPos, BlockState> e : schematic.getBlocks().entrySet()) {
                if (e.getValue().isAir()) continue;
                BlockPos rotated = SchematicUtils.rotatePositionSteps(e.getKey(), rot);
                long packed = ((long) rotated.getX() << 32) | (rotated.getZ() & 0xFFFFFFFFL);
                if (seen.add(packed)) cells.add(new int[]{ rotated.getX(), rotated.getZ() });
            }
            return cells;
        });
    }


    void renderPlayerMarker(GuiGraphics g, double camX, double camZ, float zoom, float tilt,
                            int screenW, int screenH, int sidebarW) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int[] sc = worldToScreen((int) mc.player.getX(), (int) mc.player.getZ(),
                camX, camZ, zoom, tilt, screenW, screenH, sidebarW);
        int r = Math.max(3, (int)(zoom / 2.5f));
        g.fill(sc[0] - r, sc[1] - r, sc[0] + r, sc[1] + r, C_PLAYER);
        g.fill(sc[0] - r - 2, sc[1] - 1, sc[0] + r + 2, sc[1] + 1, C_PLAYER);
        g.fill(sc[0] - 1, sc[1] - r - 2, sc[0] + 1, sc[1] + r + 2, C_PLAYER);
    }

    static int[] worldToScreen(double worldX, double worldZ,
                               double camX, double camZ, float zoom, float tilt,
                               int screenW, int screenH, int sidebarW) {
        float tiltRad = (float) Math.toRadians(tilt);
        int sx = (int)(screenCenterX(screenW, sidebarW) + (worldX - camX) * zoom);
        int sy = (int)(screenCenterZ(screenH) + (worldZ - camZ) * zoom * Math.sin(tiltRad));
        return new int[]{ sx, sy };
    }

    static int[] screenToWorld(double screenX, double screenY,
                               double camX, double camZ, float zoom, float tilt,
                               int screenW, int screenH, int sidebarW) {
        float tiltRad = (float) Math.toRadians(tilt);
        int wx = (int) Math.floor(camX + (screenX - screenCenterX(screenW, sidebarW)) / zoom);
        int wz = (int) Math.floor(camZ + (screenY - screenCenterZ(screenH)) / (zoom * Math.sin(tiltRad)));
        return new int[]{ wx, wz };
    }

    static double screenCenterX(int screenW, int sidebarW) { return sidebarW + (screenW - sidebarW) / 2.0; }
    static double screenCenterZ(int screenH)               { return screenH / 2.0; }

    static void enableMapScissor(int screenW, int screenH, int sidebarW, int hudH) {
        Minecraft mc = Minecraft.getInstance();
        double scale     = mc.getWindow().getGuiScale();
        int hudPx        = (int) Math.round(hudH * scale);
        int mapHeightPx  = (int) Math.round(screenH * scale) - hudPx;
        RenderSystem.enableScissor(
                (int) Math.round(sidebarW * scale), hudPx,
                (int) Math.round((screenW - sidebarW) * scale), mapHeightPx);
    }

    static void enableSidebarScissor(int screenH, int sidebarW, int listTop, int listH) {
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        int physH    = mc.getWindow().getHeight();
        RenderSystem.enableScissor(
                0,
                (int) Math.round(physH - (listTop + listH) * scale),
                (int) Math.round(sidebarW * scale),
                (int) Math.round(listH * scale));
    }

    static void disableScissor() { RenderSystem.disableScissor(); }
}