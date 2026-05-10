package com.gtceuterminal.client.gui.theme;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class WallpaperManager {

    public static final String WALLPAPER_DIR_NAME = "config/gtceuterminal/wallpapers";
    private static final String RL_NAMESPACE      = "gtceuterminal";
    private static final String RL_PATH_PREFIX    = "dynamic/wallpaper/";

    private static final Map<String, ResourceLocation> cache = new HashMap<>();

    public static File getWallpaperDir() {
        File dir = new File(Minecraft.getInstance().gameDirectory, WALLPAPER_DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static List<String> listWallpapers() {
        List<String> result = new ArrayList<>();
        File dir = getWallpaperDir();
        if (!dir.exists() || !dir.isDirectory()) return result;

        File[] files = dir.listFiles(f ->
                f.isFile() && (
                        f.getName().toLowerCase().endsWith(".png") ||
                        f.getName().toLowerCase().endsWith(".jpg") ||
                        f.getName().toLowerCase().endsWith(".jpeg")
                )
        );
        if (files == null) return result;

        for (File f : files) result.add(f.getName());
        result.sort(String::compareToIgnoreCase);
        return result;
    }

    public static java.util.Optional<ResourceLocation> getTexture(String filename) {
        if (filename == null || filename.isBlank()) return java.util.Optional.empty();

        if (cache.containsKey(filename)) return java.util.Optional.of(cache.get(filename));

        File file = new File(getWallpaperDir(), filename);
        if (!file.exists() || !file.isFile()) {
            GTCEUTerminalMod.LOGGER.warn("WallpaperManager: file not found: {}", file.getAbsolutePath());
            return java.util.Optional.empty();
        }

        try {
            Path wallpaperDir = getWallpaperDir().toPath().toRealPath();
            Path filePath     = file.toPath().toRealPath();
            if (!filePath.startsWith(wallpaperDir)) {
                GTCEUTerminalMod.LOGGER.warn("WallpaperManager: rejected suspicious path: {}", filename);
                return java.util.Optional.empty();
            }
        } catch (IOException e) {
            GTCEUTerminalMod.LOGGER.warn("WallpaperManager: could not resolve path for: {}", filename);
            return java.util.Optional.empty();
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            NativeImage img = NativeImage.read(fis);
            DynamicTexture tex = new DynamicTexture(img);

            String rlPath = RL_PATH_PREFIX + sanitizeFilename(filename);
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(RL_NAMESPACE, rlPath);

            Minecraft.getInstance().getTextureManager().register(rl, tex);
            cache.put(filename, rl);

            GTCEUTerminalMod.LOGGER.debug("WallpaperManager: loaded wallpaper '{}' → {}", filename, rl);
            return java.util.Optional.of(rl);

        } catch (IOException e) {
            GTCEUTerminalMod.LOGGER.warn("WallpaperManager: failed to load '{}': {}", filename, e.getMessage());
            return java.util.Optional.empty();
        }
    }

    public static void evict(String filename) {
        cache.remove(filename);
    }

    public static void clearCache() {
        cache.clear();
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────
    private static String sanitizeFilename(String filename) {
        return filename.toLowerCase()
                .replaceAll("[^a-z0-9._-]", "_")
                .replaceAll("_+", "_");
    }
}