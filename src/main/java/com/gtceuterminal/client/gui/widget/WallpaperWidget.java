package com.gtceuterminal.client.gui.widget;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.theme.WallpaperManager;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.common.theme.bundle.ThemeBundle;
import com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


@OnlyIn(Dist.CLIENT)
public class WallpaperWidget extends Widget {

    private static final long SLIDE_MS = 8_000;
    private static final long FADE_MS  = 1_000;

    private final Supplier<ItemTheme> themeSupplier;

    private List<ResourceLocation> slideList = new ArrayList<>();
    private int  slideIdx   = 0;
    private long slideStart = 0;

    private String  lastBundleId     = null;
    private boolean lastSlideshowMode = false;
    private ItemTheme.SlideshowSource lastSource = null;

    public WallpaperWidget(int x, int y, int width, int height, Supplier<ItemTheme> themeSupplier) {
        super(x, y, width, height);
        this.themeSupplier = themeSupplier;
        setClientSideWidget();
    }

    @Override
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        ItemTheme theme = themeSupplier.get();
        Position pos  = getPosition();
        Size     size = getSize();

        int x = pos.x;
        int y = pos.y;
        int w = size.width;
        int h = size.height;

        graphics.fill(x, y, x + w, y + h, theme.bgColor);

        if (theme.slideshowMode && theme.isBundleStyle()) {
            renderSlideshow(graphics, theme, x, y, w, h);
        } else {
            renderStatic(graphics, theme, x, y, w, h);
        }
    }

    private void renderStatic(GuiGraphics graphics, ItemTheme theme,
                              int x, int y, int w, int h) {
        if (!theme.hasWallpaper()) return;
        ResourceLocation rl = resolveStaticWallpaper(theme);
        if (rl == null) return;
        drawImage(graphics, rl, x, y, w, h, 1f);
    }

    private ResourceLocation resolveStaticWallpaper(ItemTheme theme) {
        String wp = theme.wallpaper;
        if (wp == null || wp.isBlank()) return null;

        if (wp.startsWith("bundle:")) {
            String bundleId = wp.substring("bundle:".length());
            ThemeBundle bundle = ThemeBundleRegistry.get(bundleId);
            if (bundle == null) return null;
            return bundle.builtinWallpaper();
        }

        return WallpaperManager.getTexture(wp).orElse(null);
    }

    private void renderSlideshow(GuiGraphics graphics, ItemTheme theme,
                                 int x, int y, int w, int h) {
        if (slideshowConfigChanged(theme)) {
            rebuildSlideList(theme);
        }

        if (slideList.isEmpty()) return;

        long nowMs = System.currentTimeMillis();

        if (slideStart == 0) slideStart = nowMs;

        long age = nowMs - slideStart;
        long cycleDuration = SLIDE_MS + FADE_MS * 2;
        long posInCycle = age % cycleDuration;

        int expectedIdx = (int)((age / cycleDuration) % slideList.size());
        if (expectedIdx != slideIdx) {
            slideIdx = expectedIdx;
        }

        float alpha;
        if (posInCycle < FADE_MS) {
            alpha = posInCycle / (float) FADE_MS;
        } else if (posInCycle > SLIDE_MS + FADE_MS) {
            alpha = 1f - (posInCycle - SLIDE_MS - FADE_MS) / (float) FADE_MS;
        } else {
            alpha = 1f;
        }

        alpha = Math.max(0f, Math.min(1f, alpha));
        if (alpha < 0.01f) return;

        ResourceLocation rl = slideList.get(slideIdx);
        drawImage(graphics, rl, x, y, w, h, alpha);
    }

    private boolean slideshowConfigChanged(ItemTheme theme) {
        return !theme.bundleId.equals(lastBundleId)
                || theme.slideshowMode != lastSlideshowMode
                || theme.slideshowSource != lastSource;
    }

    private void rebuildSlideList(ItemTheme theme) {
        slideList.clear();
        slideIdx   = 0;
        slideStart = 0;

        lastBundleId      = theme.bundleId;
        lastSlideshowMode = theme.slideshowMode;
        lastSource        = theme.slideshowSource;

        if (theme.slideshowSource == ItemTheme.SlideshowSource.BUILTIN) {
            ThemeBundle bundle = ThemeBundleRegistry.get(theme.bundleId);
            if (bundle != null) {
                slideList.addAll(bundle.slideshowWallpapers());
            }
        } else {
            List<String> files = WallpaperManager.listWallpapers();
            for (String f : files) {
                WallpaperManager.getTexture(f).ifPresent(slideList::add);
            }
        }

        com.gtceuterminal.GTCEUTerminalMod.LOGGER.debug(
                "WallpaperWidget: built slideshow list ({} images, source={})",
                slideList.size(), theme.slideshowSource);
    }

    private void drawImage(GuiGraphics graphics, ResourceLocation rl,
                           int x, int y, int w, int h, float alpha) {
        int imgW = w, imgH = h;
        try {
            AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(rl);
            if (tex instanceof DynamicTexture dt && dt.getPixels() != null) {
                imgW = dt.getPixels().getWidth();
                imgH = dt.getPixels().getHeight();
            }
        } catch (RuntimeException e) {
            GTCEUTerminalMod.LOGGER.debug("WallpaperWidget: could not read texture dimensions for {}: {}", rl, e.getMessage());
        }

        float scale = Math.min((float) w / imgW, (float) h / imgH);
        float drawW = imgW * scale;
        float drawH = imgH * scale;
        float drawX = x + (w - drawW) / 2f;
        float drawY = y + (h - drawH) / 2f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, rl);

        Matrix4f mat = graphics.pose().last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        buf.vertex(mat, drawX,         drawY + drawH, 0f).uv(0f, 1f).endVertex();
        buf.vertex(mat, drawX + drawW, drawY + drawH, 0f).uv(1f, 1f).endVertex();
        buf.vertex(mat, drawX + drawW, drawY,         0f).uv(1f, 0f).endVertex();
        buf.vertex(mat, drawX,         drawY,         0f).uv(0f, 0f).endVertex();

        tess.end();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}