package com.gtceuterminal.client.gui.planner;

import com.gtceuterminal.client.gui.planner.PlannerState.PlacedGhost;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.util.SchematicUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 2D top-down placement planner — Satisfactory/Factorio style.
 *
 * Controls:
 *   Left-click map      → place schematic ghost
 *   Right-click ghost   → remove it
 *   Middle-drag / R-drag → pan camera
 *   Scroll              → zoom in/out
 *   Shift + Scroll      → tilt camera up/down
 *   R key               → rotate selected schematic 90° CW
 *   Escape              → close (ghosts stay in world)
 */
@OnlyIn(Dist.CLIENT)
public class PlannerScreen extends Screen {

    private static final int SIDEBAR_W = 160;
    private static final int ENTRY_H   = 40;
    private static final int HUD_H     = 22;

    static final float ZOOM_DEFAULT = 8f;
    static final float ZOOM_MIN     = 3f;
    static final float ZOOM_MAX     = 28f;
    static final float TILT_DEFAULT = 72f;
    static final float TILT_MIN     = 25f;
    static final float TILT_MAX     = 90f;

    private static final int C_BG = 0xFF0D0D0D;

    private final List<SchematicData> schematics;
    private int   selectedIdx = 0;
    private int   rotSteps    = 0;

    private double camX, camZ;
    private float  zoom = ZOOM_DEFAULT;
    private float  tilt = TILT_DEFAULT;

    private boolean panning;
    private double  panStartX, panStartY, panCamX, panCamZ;

    private int sidebarScrollY = 0;

    private final PlannerMapRenderer mapRenderer = new PlannerMapRenderer();
    private final PlannerSidebar sidebar = new PlannerSidebar();
    private final PlannerHud hud = new PlannerHud();

    // Double-click detection
    private int  lastClickedIdx  = -1;
    private long lastClickTimeMs = 0;
    private static final long DOUBLE_CLICK_MS = 400;

    public PlannerScreen(List<SchematicData> schematics) {
        super(Component.translatable("gui.gtceuterminal.planner.title"));
        this.schematics = schematics;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.camX = mc.player.getX();
            this.camZ = mc.player.getZ();
        }
    }

    public PlannerScreen(List<SchematicData> schematics, int selectedIdx, int rotSteps) {
        this(schematics);
        this.selectedIdx = Math.max(0, Math.min(selectedIdx, schematics.size() - 1));
        this.rotSteps    = rotSteps;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        mapRenderer.beginFrame(camX, camZ, zoom, tilt, width, height, SIDEBAR_W, HUD_H);

        g.fill(0, 0, width, height, C_BG);

        PlannerMapRenderer.enableMapScissor(width, height, SIDEBAR_W, HUD_H);
        mapRenderer.renderWorldTiles(g, camX, camZ, zoom, tilt, width, height, SIDEBAR_W);
        mapRenderer.renderGrid(g, camX, camZ, zoom, tilt, width, height, SIDEBAR_W);
        mapRenderer.renderPlacedGhosts(g, camX, camZ, zoom, tilt, width, height, SIDEBAR_W);
        mapRenderer.renderCursorFootprint(g, mouseX, mouseY, selectedIdx, schematics,
                camX, camZ, zoom, tilt, width, height, SIDEBAR_W, rotSteps);
        mapRenderer.renderPlayerMarker(g, camX, camZ, zoom, tilt, width, height, SIDEBAR_W);
        PlannerMapRenderer.disableScissor();

        sidebar.render(g, font, SIDEBAR_W, height, HUD_H, ENTRY_H,
                selectedIdx, rotSteps, sidebarScrollY, mouseX, mouseY, schematics);
        hud.render(g, font, width, height, SIDEBAR_W, HUD_H);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX < SIDEBAR_W) {
            if (mouseY >= sidebar.lastViewBtnY && mouseY < sidebar.lastViewBtnY + 12
                    && selectedIdx >= 0 && selectedIdx < schematics.size()) {
                Minecraft.getInstance().setScreen(
                        new com.gtceuterminal.client.gui.blueprint.BlueprintViewScreen(
                                schematics.get(selectedIdx), schematics, selectedIdx));
                return true;
            }
            if (mouseY >= sidebar.lastBpBtnY && mouseY < sidebar.lastBpBtnY + 12) {
                SchematicData current = (selectedIdx >= 0 && selectedIdx < schematics.size())
                        ? schematics.get(selectedIdx) : null;
                Minecraft.getInstance().setScreen(
                        new com.gtceuterminal.client.gui.blueprint.BlueprintLibraryScreen(
                                this, current, loaded -> schematics.add(loaded)));
                return true;
            }
        }

        if (button == 0 && mouseY >= hud.lastHudY) {
            if (mouseX >= hud.lastModeBx && mouseX < hud.lastModeBx + PlannerHud.BTN_W_MODE) {
                com.gtceuterminal.client.ClientEvents.enterFreeCamScreen(
                        Minecraft.getInstance(), schematics, selectedIdx, rotSteps);
                return true;
            }
            if (mouseX >= hud.lastBuildBx && mouseX < hud.lastBuildBx + PlannerHud.BTN_W) {
                PlannerHud.buildAll(schematics);
                return true;
            }
            if (mouseX >= hud.lastClearBx && mouseX < hud.lastClearBx + PlannerHud.BTN_W) {
                PlannerState.ghosts.clear();
                return true;
            }
        }

        if (mouseX < SIDEBAR_W) {
            int listTop = 34;
            int i = (int)((mouseY - listTop + sidebarScrollY) / ENTRY_H);
            if (i >= 0 && i < schematics.size()) {
                long now = System.currentTimeMillis();
                if (i == lastClickedIdx && (now - lastClickTimeMs) < DOUBLE_CLICK_MS) {
                    Minecraft.getInstance().setScreen(
                            new com.gtceuterminal.client.gui.blueprint.BlueprintViewScreen(
                                    schematics.get(i), schematics, i));
                    return true;
                }
                lastClickedIdx  = i;
                lastClickTimeMs = now;
                selectedIdx = i;
            }
            return true;
        }

        if (mouseY >= hud.lastHudY) return true;

        if (button == 0) {
            if (selectedIdx >= 0 && selectedIdx < schematics.size()) {
                int[] world = PlannerMapRenderer.screenToWorld(mouseX, mouseY,
                        camX, camZ, zoom, tilt, width, height, SIDEBAR_W);
                Minecraft mc = Minecraft.getInstance();
                int surfaceY = mc.level != null
                        ? mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        world[0], world[1])
                        : (mc.player != null ? (int) mc.player.getY() : 64);

                SchematicData sel = schematics.get(selectedIdx);
                final int snap = rotSteps;
                int minRelY = sel.getBlocks().keySet().stream()
                        .mapToInt(pos -> SchematicUtils.rotatePositionSteps(pos, snap).getY())
                        .min().orElse(0);

                if (!PlannerState.isOccupied(world[0], world[1], rotSteps, sel)) {
                    PlannerState.ghosts.add(new PlacedGhost(
                            sel, new BlockPos(world[0], surfaceY - minRelY, world[1]), rotSteps));
                }
            }
            return true;
        }

        if (button == 1) {
            int[] world = PlannerMapRenderer.screenToWorld(mouseX, mouseY,
                    camX, camZ, zoom, tilt, width, height, SIDEBAR_W);
            if (!PlannerState.tryRemoveAt(world[0], world[1])) {
                panning   = true;
                panStartX = mouseX; panStartY = mouseY;
                panCamX   = camX;   panCamZ   = camZ;
            }
            return true;
        }

        if (button == 2) {
            panning   = true;
            panStartX = mouseX; panStartY = mouseY;
            panCamX   = camX;   panCamZ   = camZ;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1 || button == 2) panning = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (panning) {
            float tiltRad = (float) Math.toRadians(tilt);
            camX = panCamX - (mouseX - panStartX) / zoom;
            camZ = panCamZ - (mouseY - panStartY) / (zoom * Math.sin(tiltRad));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (hasShiftDown()) {
            tilt = Math.max(TILT_MIN, Math.min(TILT_MAX, tilt + (float)(delta * 3f)));
        } else if (mouseX < SIDEBAR_W) {
            int maxScroll = Math.max(0, schematics.size() * ENTRY_H - (height - 34 - HUD_H - 10));
            sidebarScrollY = Math.max(0, Math.min(maxScroll, sidebarScrollY - (int)(delta * ENTRY_H)));
        } else {
            double cx = PlannerMapRenderer.screenCenterX(width, SIDEBAR_W);
            double cz = PlannerMapRenderer.screenCenterZ(height);
            double tiltRad = Math.toRadians(tilt);
            double beforeX = (mouseX - cx) / zoom;
            double beforeZ = (mouseY - cz) / (zoom * Math.sin(tiltRad));
            zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom + (float)(delta * 1.5f)));
            double afterX = (mouseX - cx) / zoom;
            double afterZ = (mouseY - cz) / (zoom * Math.sin(tiltRad));
            camX -= afterX - beforeX;
            camZ -= afterZ - beforeZ;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_R) { rotSteps = (rotSteps + 1) & 3; return true; }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
            com.gtceuterminal.client.ClientEvents.enterFreeCamScreen(
                    Minecraft.getInstance(), schematics, selectedIdx, rotSteps);
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_C && hasControlDown()) {
            PlannerState.ghosts.clear();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}