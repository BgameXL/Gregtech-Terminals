package com.gtceuterminal.client.blueprint;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.SchematicData;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlueprintFileManager {

    private static final String FOLDER_NAME = "blueprints";
    private static final String EXTENSION    = ".nbt";

    private BlueprintFileManager() {}

    public static Path getBlueprintFolder() {
        Path folder = Minecraft.getInstance().gameDirectory.toPath().resolve(FOLDER_NAME);
        if (!Files.exists(folder)) {
            try {
                Files.createDirectories(folder);
            } catch (IOException e) {
                GTCEUTerminalMod.LOGGER.error("[Blueprints] Could not create blueprints folder", e);
            }
        }
        return folder;
    }

    public static List<String> listBlueprintNames() {
        Path folder = getBlueprintFolder();
        List<String> names = new ArrayList<>();
        try (var stream = Files.list(folder)) {
            stream.filter(p -> p.toString().endsWith(EXTENSION))
                  .map(p -> p.getFileName().toString())
                  .map(s -> s.substring(0, s.length() - EXTENSION.length()))
                  .forEach(names::add);
        } catch (IOException e) {
            GTCEUTerminalMod.LOGGER.error("[Blueprints] Could not list blueprints", e);
        }
        Collections.sort(names);
        return names;
    }

    public static boolean save(String name, SchematicData schematic) {
        if (name == null || name.isBlank()) return false;
        Path file = getBlueprintFolder().resolve(sanitize(name) + EXTENSION);
        try {
            CompoundTag tag = schematic.toNBT();
            NbtIo.writeCompressed(tag, file.toFile());
            GTCEUTerminalMod.LOGGER.debug("[Blueprints] Saved '{}'", file);
            return true;
        } catch (IOException e) {
            GTCEUTerminalMod.LOGGER.error("[Blueprints] Failed to save '{}'", file, e);
            return false;
        }
    }

    public static SchematicData load(String name) {
        Path file = getBlueprintFolder().resolve(sanitize(name) + EXTENSION);
        if (!Files.exists(file)) {
            GTCEUTerminalMod.LOGGER.warn("[Blueprints] File not found: '{}'", file);
            return null;
        }
        try {
            CompoundTag tag = NbtIo.readCompressed(file.toFile());
            var registryAccess = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.registryAccess()
                    : null;
            if (registryAccess == null) {
                GTCEUTerminalMod.LOGGER.error("[Blueprints] No level loaded — cannot deserialize blueprint");
                return null;
            }
            SchematicData data = SchematicData.fromNBT(tag, registryAccess);
            GTCEUTerminalMod.LOGGER.debug("[Blueprints] Loaded '{}'", file);
            return data;
        } catch (IOException e) {
            GTCEUTerminalMod.LOGGER.error("[Blueprints] Failed to load '{}'", file, e);
            return null;
        }
    }

    public static boolean delete(String name) {
        Path file = getBlueprintFolder().resolve(sanitize(name) + EXTENSION);
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            GTCEUTerminalMod.LOGGER.error("[Blueprints] Failed to delete '{}'", file, e);
            return false;
        }
    }

    public static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}