package com.gtceuterminal.client.gui.planner;

import com.gtceuterminal.common.data.SchematicData;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
final class PlannerSidebar {

    private static final int C_SIDEBAR     = 0xFF1A1A1A;
    private static final int C_SIDEBAR_BDR = 0xFF2E75B6;
    private static final int C_SEL         = 0x332E75B6;
    private static final int C_TEXT        = 0xFFDDDDDD;
    private static final int C_DIM         = 0xFF888888;

    // Button Y positions updated each render, read back by PlannerScreen for hit-testing
    int lastViewBtnY;
    int lastBpBtnY;
    int lastViewBtnW;

    void render(GuiGraphics g, Font font,
                int sidebarW, int screenH, int hudH, int entryH,
                int selectedIdx, int rotSteps, int sidebarScrollY,
                int mouseX, int mouseY,
                List<SchematicData> schematics) {

        g.fill(0, 0, sidebarW, screenH, C_SIDEBAR);
        g.fill(sidebarW - 1, 0, sidebarW, screenH, C_SIDEBAR_BDR);

        g.drawString(font,
                Component.translatable("gui.gtceuterminal.planner.title").getString(),
                8, 8, C_TEXT, false);
        g.drawString(font,
                Component.translatable("gui.gtceuterminal.planner.sidebar.schematics").getString(),
                8, 22, C_DIM, false);
        g.fill(0, 32, sidebarW - 1, 33, 0xFF333333);

        int listTop = 34;
        int listH   = screenH - listTop - hudH - 10;
        PlannerMapRenderer.enableSidebarScissor(screenH, sidebarW, listTop, listH);

        for (int i = 0; i < schematics.size(); i++) {
            SchematicData s = schematics.get(i);
            int ey = listTop + i * entryH - sidebarScrollY;
            if (ey + entryH < listTop || ey > listTop + listH) continue;

            boolean sel     = (i == selectedIdx);
            boolean hovered = mouseX < sidebarW && mouseY >= ey && mouseY < ey + entryH;

            if (sel) {
                g.fill(0, ey, sidebarW - 1, ey + entryH, C_SEL);
                g.fill(0, ey, 3, ey + entryH, C_SIDEBAR_BDR);
            } else if (hovered) {
                g.fill(0, ey, sidebarW - 1, ey + entryH, 0x18FFFFFF);
            }

            String name = s.getName();
            if (name.length() > 18) name = name.substring(0, 15) + "…";
            g.drawString(font, "§f" + name, 10, ey + 7, C_TEXT, false);

            BlockPos sz = s.getSize();
            g.drawString(font, String.format("§8%dx%dx%d", sz.getX(), sz.getY(), sz.getZ()),
                    10, ey + 21, C_DIM, false);
            g.fill(4, ey + entryH - 1, sidebarW - 5, ey + entryH, 0xFF222222);
        }

        PlannerMapRenderer.disableScissor();

        int badgeY = screenH - hudH - 42;
        g.drawString(font,
                Component.translatable("gui.gtceuterminal.planner.hud.rotation", rotSteps * 90).getString(),
                8, badgeY, C_TEXT, false);
        g.drawString(font,
                Component.translatable("gui.gtceuterminal.planner.hud.placed", PlannerState.ghosts.size()).getString(),
                8, badgeY + 10, C_TEXT, false);

        boolean hasSelected = selectedIdx >= 0 && selectedIdx < schematics.size();
        int viewBtnY = screenH - hudH - 28;
        int viewBtnW = sidebarW - 16;
        drawBtn(g, 8, viewBtnY, viewBtnW, 12,
                hasSelected ? 0xFF1A3A6B : 0xFF2A2A2A,
                hasSelected ? 0xFF2E75B6 : 0xFF444444);
        g.drawString(font,
                hasSelected
                        ? Component.translatable("gui.gtceuterminal.planner.sidebar.view3d.enabled").getString()
                        : Component.translatable("gui.gtceuterminal.planner.sidebar.view3d.disabled").getString(),
                12, viewBtnY + 2, 0xFFFFFFFF, false);
        lastViewBtnY = viewBtnY;
        lastViewBtnW = viewBtnW;

        int bpBtnY = screenH - hudH - 14;
        drawBtn(g, 8, bpBtnY, viewBtnW, 12, 0xFF1A3A6B, 0xFF2E75B6);
        g.drawString(font,
                Component.translatable("gui.gtceuterminal.planner.sidebar.blueprints").getString(),
                12, bpBtnY + 2, 0xFFFFFFFF, false);
        lastBpBtnY = bpBtnY;
    }

    private static void drawBtn(GuiGraphics g, int x, int y, int w, int h, int fill, int bdr) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, bdr);
        g.fill(x, y + h - 1, x + w, y + h, bdr);
        g.fill(x, y, x + 1, y + h, bdr);
        g.fill(x + w - 1, y, x + w, y + h, bdr);
    }
}