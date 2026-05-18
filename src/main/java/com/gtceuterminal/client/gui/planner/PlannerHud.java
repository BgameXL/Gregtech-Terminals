package com.gtceuterminal.client.gui.planner;

import com.gtceuterminal.common.data.SchematicData;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
final class PlannerHud {

    private static final int C_DIM = 0xFF888888;
    private static final int C_HUD = 0xCC111111;

    static final int BTN_W      = 90;
    static final int BTN_W_MODE = 44;
    static final int BTN_H      = 16;

    // Button x positions updated each render, read back by PlannerScreen for hit-testing
    int lastBuildBx, lastClearBx, lastModeBx, lastHudY;

    void render(GuiGraphics g, Font font, int screenW, int screenH, int sidebarW, int hudH) {
        int y = screenH - hudH;
        g.fill(0, y, screenW, screenH, C_HUD);
        g.fill(0, y, screenW, y + 1, 0xFF333333);

        g.drawString(font,
                Component.translatable("gui.gtceuterminal.planner.sidebar.hints").getString(),
                sidebarW + 6, y + 7, C_DIM, false);

        boolean hasGhosts = !PlannerState.ghosts.isEmpty();

        int modeBx  = screenW - 8 - BTN_W_MODE;
        int clearBx = modeBx  - 6 - BTN_W;
        int buildBx = clearBx - 6 - BTN_W;
        lastModeBx  = modeBx;
        lastClearBx = clearBx;
        lastBuildBx = buildBx;
        lastHudY    = y;

        drawBtn(g, modeBx, y + 3, BTN_W_MODE, BTN_H, 0xFF1A3A6B, 0xFF2E75B6);
        g.drawString(font,
                Component.translatable("gui.gtceuterminal.planner.hud.mode_3d").getString(),
                modeBx + 14, y + 7, 0xFFFFFFFF, false);

        int buildFill = hasGhosts ? 0xFF1E6B1E : 0xFF2A2A2A;
        int buildBdr  = hasGhosts ? 0xFF2E8B2E : 0xFF444444;
        drawBtn(g, buildBx, y + 3, BTN_W, BTN_H, buildFill, buildBdr);
        String buildLbl = hasGhosts
                ? Component.translatable("gui.gtceuterminal.planner.hud.build_all.enabled",
                        PlannerState.ghosts.size()).getString()
                : Component.translatable("gui.gtceuterminal.planner.hud.build_all.disabled").getString();
        g.drawString(font, buildLbl, buildBx + 6, y + 7, 0xFFFFFFFF, false);

        int clearFill = hasGhosts ? 0xFF6B1E1E : 0xFF2A2A2A;
        int clearBdr  = hasGhosts ? 0xFF8B2E2E : 0xFF444444;
        drawBtn(g, clearBx, y + 3, BTN_W, BTN_H, clearFill, clearBdr);
        String clearLbl = hasGhosts
                ? Component.translatable("gui.gtceuterminal.planner.hud.clear_all.enabled").getString()
                : Component.translatable("gui.gtceuterminal.planner.hud.clear_all.disabled").getString();
        g.drawString(font, clearLbl, clearBx + 6, y + 7, 0xFFFFFFFF, false);
    }

    static void buildAll(List<SchematicData> schematics) {
        if (PlannerState.ghosts.isEmpty()) return;
        List<com.gtceuterminal.common.network.CPacketPlacePlannerGhosts.GhostEntry> entries = new ArrayList<>();
        for (PlannerState.PlacedGhost g : PlannerState.ghosts) {
            entries.add(new com.gtceuterminal.common.network.CPacketPlacePlannerGhosts.GhostEntry(
                    g.schematic.toNBT(), g.origin, g.rotSteps));
        }
        com.gtceuterminal.common.network.TerminalNetwork.sendToServer(
                new com.gtceuterminal.common.network.CPacketPlacePlannerGhosts(entries));
        PlannerState.ghosts.clear();
    }

    private static void drawBtn(GuiGraphics g, int x, int y, int w, int h, int fill, int bdr) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, bdr);
        g.fill(x, y + h - 1, x + w, y + h, bdr);
        g.fill(x, y, x + 1, y + h, bdr);
        g.fill(x + w - 1, y, x + w, y + h, bdr);
    }
}