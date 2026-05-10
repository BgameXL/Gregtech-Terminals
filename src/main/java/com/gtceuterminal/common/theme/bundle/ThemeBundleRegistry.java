package com.gtceuterminal.common.theme.bundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.theme.bundle.modpack.TerraFirmaGregBundle;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ThemeBundleRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String THEMES_DIR = "config/gtceuterminal/themes";

    private static final LinkedHashMap<String, ThemeBundle> REGISTRY = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        registerBuiltins();
        loadExternalOverrides();
        GTCEUTerminalMod.LOGGER.info("ThemeBundleRegistry: {} bundle(s) loaded: {}",
                REGISTRY.size(), String.join(", ", REGISTRY.keySet()));
    }

    public static void register(ThemeBundle bundle) {
        REGISTRY.put(bundle.id(), bundle);
    }

    public static List<ThemeBundle> all() {
        return Collections.unmodifiableList(new ArrayList<>(REGISTRY.values()));
    }

    @Nullable
    public static ThemeBundle get(String id) {
        if (id == null || id.isBlank()) return null;
        return REGISTRY.get(id);
    }

    public static boolean has(String id) {
        return id != null && REGISTRY.containsKey(id);
    }

    private static void registerBuiltins() {
        register(new TerraFirmaGregBundle());
        // register(new AstroGregBundle());
        // register(new PTFBundle());
    }

    private static void loadExternalOverrides() {
        Path themesRoot = Paths.get(THEMES_DIR);
        if (!Files.exists(themesRoot)) {
            try { Files.createDirectories(themesRoot); } catch (IOException ignored) {}
            return;
        }

        try {
            Files.list(themesRoot)
                 .filter(Files::isDirectory)
                 .forEach(ThemeBundleRegistry::loadExternalBundle);
        } catch (IOException e) {
            GTCEUTerminalMod.LOGGER.warn("ThemeBundleRegistry: failed to scan themes dir: {}", e.getMessage());
        }
    }

    private static void loadExternalBundle(Path dir) {
        String id = dir.getFileName().toString().toLowerCase(Locale.ROOT);
        Path jsonFile = dir.resolve("bundle.json");

        if (!Files.exists(jsonFile)) {
            return;
        }

        try {
            String raw = Files.readString(jsonFile);
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();

            ThemeBundle existing = REGISTRY.get(id);
            if (existing != null) {
                REGISTRY.put(id, new JsonOverrideBundle(existing, json));
                GTCEUTerminalMod.LOGGER.info(
                        "ThemeBundleRegistry: applied external color overrides to bundle '{}'", id);
            } else {
                REGISTRY.put(id, new JsonOnlyBundle(id, json));
                GTCEUTerminalMod.LOGGER.info(
                        "ThemeBundleRegistry: loaded external-only bundle '{}'", id);
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn(
                    "ThemeBundleRegistry: failed to parse bundle.json in '{}': {}", id, e.getMessage());
        }
    }

    private static final class JsonOverrideBundle extends ThemeBundle {
        private final ThemeBundle base;
        private final int accent, bg, panel, text;
        private final String overrideName;

        JsonOverrideBundle(ThemeBundle base, JsonObject json) {
            this.base         = base;
            this.accent       = readColor(json, "accentColor", base.accentColor());
            this.bg           = readColor(json, "bgColor",     base.bgColor());
            this.panel        = readColor(json, "panelColor",  base.panelColor());
            this.text         = readColor(json, "textColor",   base.textColor());
            this.overrideName = json.has("displayName")
                    ? json.get("displayName").getAsString()
                    : base.displayName();
        }

        @Override public String id()          { return base.id(); }
        @Override public String displayName() { return overrideName; }
        @Override public String description() { return base.description() + " §7(custom colors)"; }
        @Override public int accentColor()    { return accent; }
        @Override public int bgColor()        { return bg; }
        @Override public int panelColor()     { return panel; }
        @Override public int textColor()      { return text; }

        @Override public net.minecraft.resources.ResourceLocation builtinWallpaper() { return base.builtinWallpaper(); }
        @Override public com.lowdragmc.lowdraglib.gui.texture.IGuiTexture backgroundTexture() { return base.backgroundTexture(); }
        @Override public com.lowdragmc.lowdraglib.gui.texture.IGuiTexture panelTexture()      { return base.panelTexture(); }
        @Override public com.lowdragmc.lowdraglib.gui.texture.IGuiTexture headerTexture()     { return base.headerTexture(); }
        @Override public com.lowdragmc.lowdraglib.gui.texture.IGuiTexture slotTexture()       { return base.slotTexture(); }
        @Override public com.lowdragmc.lowdraglib.gui.texture.IGuiTexture buttonTexture()     { return base.buttonTexture(); }
        @Override public com.lowdragmc.lowdraglib.gui.texture.IGuiTexture borderTexture()     { return base.borderTexture(); }
    }

    private static final class JsonOnlyBundle extends ThemeBundle {
        private final String id, name, desc;
        private final int accent, bg, panel, text;

        JsonOnlyBundle(String id, JsonObject json) {
            this.id     = id;
            this.name   = json.has("displayName") ? json.get("displayName").getAsString() : id;
            this.desc   = json.has("description") ? json.get("description").getAsString() : "External theme";
            this.accent = readColor(json, "accentColor", 0xFF2E75B6);
            this.bg     = readColor(json, "bgColor",     0xFF1A1A1A);
            this.panel  = readColor(json, "panelColor",  0xFF252525);
            this.text   = readColor(json, "textColor",   0xFFFFFFFF);
        }

        @Override public String id()          { return id; }
        @Override public String displayName() { return name; }
        @Override public String description() { return desc; }
        @Override public int accentColor()    { return accent; }
        @Override public int bgColor()        { return bg; }
        @Override public int panelColor()     { return panel; }
        @Override public int textColor()      { return text; }
    }

    static int readColor(JsonObject json, String key, int fallback) {
        if (!json.has(key)) return fallback;
        try {
            String hex = json.get(key).getAsString();
            String s   = hex.startsWith("#") ? hex.substring(1) : hex;
            if (s.length() == 6) return 0xFF000000 | Integer.parseUnsignedInt(s, 16);
            return (int) Long.parseLong(s, 16);
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("ThemeBundleRegistry: invalid color for '{}': {}", key, e.getMessage());
            return fallback;
        }
    }
}