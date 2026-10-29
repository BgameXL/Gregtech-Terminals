package com.gtceuterminal.client.gui.widget;

import com.gtceuterminal.client.gui.style.TerminalPalette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class EnergyGraphWidget extends Widget {

    private long[] inputHistory;
    private long[] outputHistory;

    public EnergyGraphWidget(int x, int y, int width, int height,
                             long[] inputHistory, long[] outputHistory) {
        super(x, y, width, height);
        this.inputHistory = inputHistory != null ? inputHistory : new long[0];
        this.outputHistory = outputHistory != null ? outputHistory : new long[0];
    }

    private static void putLine(BufferBuilder buf, Matrix4f matrix,
                                float x1, float y1, float x2, float y2, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        buf.vertex(matrix, x1, y1, 0).color(r, g, b, a).endVertex();
        buf.vertex(matrix, x2, y2, 0).color(r, g, b, a).endVertex();
    }

    private static void fillRect(Matrix4f matrix, int x, int y, int w, int h, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(matrix, x, y, 0).color(r, g, b, a).endVertex();
        buf.vertex(matrix, x, y + h, 0).color(r, g, b, a).endVertex();
        buf.vertex(matrix, x + w, y + h, 0).color(r, g, b, a).endVertex();
        buf.vertex(matrix, x + w, y, 0).color(r, g, b, a).endVertex();
        tess.end();
    }

    public void updateData(long[] inputHistory, long[] outputHistory) {
        this.inputHistory = inputHistory != null ? inputHistory : new long[0];
        this.outputHistory = outputHistory != null ? outputHistory : new long[0];
    }

    @Override
    public void drawInForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int w = getSize().width;
        int h = getSize().height;

        PoseStack ps = graphics.pose();
        ps.pushPose();
        ps.translate(getPosition().x, getPosition().y, 0);
        Matrix4f matrix = ps.last().pose();
        TerminalTheme tokens = TerminalUI.tokens();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        fillRect(matrix, 0, 0, w, h, tokens.panelDisabled());
        drawGrid(matrix, w, h,
                TerminalPalette.withAlpha(tokens.border(), 0x22),
                TerminalPalette.withAlpha(tokens.border(), 0x66));

        int len = Math.max(inputHistory.length, outputHistory.length);
        if (len >= 2) {
            long max = 1;
            for (long v : inputHistory) max = Math.max(max, v);
            for (long v : outputHistory) max = Math.max(max, v);

            drawFilledArea(matrix, inputHistory, len, w, h, max, tokens.success(), 0.12f);
            drawFilledArea(matrix, outputHistory, len, w, h, max, tokens.error(), 0.12f);

            drawLine(matrix, inputHistory, len, w, h, max, tokens.success());
            drawLine(matrix, outputHistory, len, w, h, max, tokens.error());
        }

        drawBorder(matrix, w, h, tokens.borderSubtle());

        RenderSystem.disableBlend();
        ps.popPose();
    }

    private void drawGrid(Matrix4f matrix, int w, int h, int gridColor, int majorColor) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= 4; i++) {
            float y = (h * i) / 4f;
            int c = (i == 0 || i == 4) ? majorColor : gridColor;
            putLine(buf, matrix, 0, y, w, y, c);
        }

        for (int i = 0; i <= 8; i++) {
            float x = (w * i) / 8f;
            int c = (i == 0 || i == 8) ? majorColor : gridColor;
            putLine(buf, matrix, x, 0, x, h, c);
        }

        tess.end();
    }

    private void drawFilledArea(Matrix4f matrix, long[] data, int len,
                                int w, int h, long max, int color, float alpha) {
        if (data.length < 2) return;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < data.length - 1 && i < len - 1; i++) {
            float x1 = (i * (float) w) / (len - 1);
            float x2 = ((i + 1) * (float) w) / (len - 1);
            float y1 = h - 1 - ((data[i] * (h - 2f)) / max);
            float y2 = h - 1 - ((data[i + 1] * (h - 2f)) / max);

            buf.vertex(matrix, x1, h - 1, 0).color(r, g, b, alpha).endVertex();
            buf.vertex(matrix, x2, h - 1, 0).color(r, g, b, alpha).endVertex();
            buf.vertex(matrix, x2, y2, 0).color(r, g, b, alpha).endVertex();
            buf.vertex(matrix, x1, y1, 0).color(r, g, b, alpha).endVertex();
        }

        tess.end();
    }

    private void drawLine(Matrix4f matrix, long[] data, int len,
                          int w, int h, long max, int color) {
        if (data.length < 2) return;

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        for (int i = 0; i < data.length - 1 && i < len - 1; i++) {
            float x1 = (i * (float) w) / (len - 1);
            float x2 = ((i + 1) * (float) w) / (len - 1);
            float y1 = h - 1 - ((data[i] * (h - 2f)) / max);
            float y2 = h - 1 - ((data[i + 1] * (h - 2f)) / max);

            buf.vertex(matrix, x1, y1, 0).color(r, g, b, 1.0f).endVertex();
            buf.vertex(matrix, x2, y2, 0).color(r, g, b, 1.0f).endVertex();
        }

        tess.end();
    }

    private void drawBorder(Matrix4f matrix, int w, int h, int color) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        putLine(buf, matrix, 0, 0, w, 0, color);
        putLine(buf, matrix, 0, h - 1, w, h - 1, color);
        putLine(buf, matrix, 0, 0, 0, h, color);
        putLine(buf, matrix, w - 1, 0, w - 1, h, color);
        tess.end();
    }
}
