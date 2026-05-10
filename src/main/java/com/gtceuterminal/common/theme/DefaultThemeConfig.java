package com.gtceuterminal.common.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.network.SPacketDefaultTheme;
import com.gtceuterminal.common.network.TerminalNetwork;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads the modpack's default theme from:
 *   config/gtceuterminal/default_theme.json
 *
 * Works on both sides:
 *  - Server: reads the JSON file, then broadcasts to all clients via SPacketDefaultTheme
 *  - Client: receives the theme via packet (setClientOverride), OR loads the local JSON
 *            as fallback (for singleplayer / integrated server)
 */
public class DefaultThemeConfig {

    private static final Gson   GSON        = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR  = "config/gtceuterminal";
    private static final String CONFIG_FILE = "default_theme.json";

    private static ItemTheme cached = null;
    private static boolean   loaded = false;

    // ─── Public API ──────────────────────────────────────────────────────────
    public static ItemTheme get() {
        if (!loaded) load();
        return cached;
    }

    public static void reload() {
        loaded = false;
        cached = null;
        load();
        broadcastToAll();
    }

    public static void setClientOverride(ItemTheme theme) {
        cached = theme;
        loaded = true;
        GTCEUTerminalMod.LOGGER.info("DefaultThemeConfig: received from server, accent=#{} bg=#{} panel=#{}",
                Integer.toHexString(theme.accentColor & 0xFFFFFF).toUpperCase(),
                Integer.toHexString(theme.bgColor     & 0xFFFFFF).toUpperCase(),
                Integer.toHexString(theme.panelColor  & 0xFFFFFF).toUpperCase());
    }

    // sends the current default theme to a single newly-joined player.
    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player) {
        if (!loaded) load();
        TerminalNetwork.sendToPlayer(new SPacketDefaultTheme(cached), player);
    }

    // ─── Load / Save ─────────────────────────────────────────────────────────
    private static void load() {
        loaded = true;
        Path file = configPath();

        if (!Files.exists(file)) {
            cached = new ItemTheme();
            save(cached);
            GTCEUTerminalMod.LOGGER.info("DefaultThemeConfig: created default at {}", file);
            return;
        }

        try {
            String json = Files.readString(file);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            cached = fromJson(obj);
            GTCEUTerminalMod.LOGGER.info("DefaultThemeConfig: loaded from {}", file);
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn(
                    "DefaultThemeConfig: failed to load '{}', using defaults: {}", file, e.getMessage());
            cached = new ItemTheme();
        }
    }

    private static void broadcastToAll() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return; // client-only context
        SPacketDefaultTheme pkt = new SPacketDefaultTheme(cached);
        for (var player : server.getPlayerList().getPlayers()) {
            TerminalNetwork.sendToPlayer(pkt, player);
        }
        GTCEUTerminalMod.LOGGER.info("DefaultThemeConfig: broadcast to {} players",
                server.getPlayerList().getPlayerCount());
    }

    public static void save(ItemTheme theme) {
        try {
            Path dir = Paths.get(CONFIG_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Files.writeString(configPath(), GSON.toJson(toJson(theme)));
        } catch (IOException e) {
            GTCEUTerminalMod.LOGGER.warn("DefaultThemeConfig: failed to save: {}", e.getMessage());
        }
    }

    // ─── JSON helpers ─────────────────────────────────────────────────────────
    private static JsonObject toJson(ItemTheme t) {
        JsonObject o = new JsonObject();
        o.addProperty("accentColor",  colorToHex(t.accentColor));
        o.addProperty("bgColor",      colorToHex(t.bgColor));
        o.addProperty("panelColor",   colorToHex(t.panelColor));
        o.addProperty("textColor",    colorToHex(t.textColor));
        o.addProperty("compactMode",  t.compactMode);
        o.addProperty("showTooltips", t.showTooltips);
        o.addProperty("showBorders",  t.showBorders);
        o.addProperty("wallpaper",    t.wallpaper != null ? t.wallpaper : "");
        o.addProperty("uiStyle",      t.uiStyle != null ? t.uiStyle.name() : ItemTheme.UiStyle.DARK.name());
        o.addProperty("bundleId",     t.bundleId != null ? t.bundleId : "");
        o.addProperty("paradeMode",   t.paradeMode != null ? t.paradeMode.name() : ItemTheme.ParadeMode.ORBITAL.name());
        o.addProperty("slideshowMode", t.slideshowMode);
        o.addProperty("slideshowSource", t.slideshowSource != null ? t.slideshowSource.name() : ItemTheme.SlideshowSource.BUILTIN.name());
        return o;
    }

    private static ItemTheme fromJson(JsonObject o) {
        ItemTheme t = new ItemTheme();
        if (o.has("accentColor"))  t.accentColor  = hexToColor(o.get("accentColor").getAsString());
        if (o.has("bgColor"))      t.bgColor       = hexToColor(o.get("bgColor").getAsString());
        if (o.has("panelColor"))   t.panelColor    = hexToColor(o.get("panelColor").getAsString());
        if (o.has("textColor"))    t.textColor     = hexToColor(o.get("textColor").getAsString());
        if (o.has("compactMode"))  t.compactMode   = o.get("compactMode").getAsBoolean();
        if (o.has("showTooltips")) t.showTooltips  = o.get("showTooltips").getAsBoolean();
        if (o.has("showBorders"))  t.showBorders   = o.get("showBorders").getAsBoolean();
        if (o.has("wallpaper"))    t.wallpaper     = o.get("wallpaper").getAsString();
        if (o.has("bundleId"))     t.bundleId      = o.get("bundleId").getAsString();
        if (o.has("uiStyle")) {
            try { t.uiStyle = ItemTheme.UiStyle.valueOf(o.get("uiStyle").getAsString()); }
            catch (IllegalArgumentException ignored) { t.uiStyle = ItemTheme.UiStyle.DARK; }
        }
        if (o.has("paradeMode")) {
            try { t.paradeMode = ItemTheme.ParadeMode.valueOf(o.get("paradeMode").getAsString()); }
            catch (IllegalArgumentException ignored) { t.paradeMode = ItemTheme.ParadeMode.ORBITAL; }
        }
        if (o.has("slideshowMode")) t.slideshowMode = o.get("slideshowMode").getAsBoolean();
        if (o.has("slideshowSource")) {
            try { t.slideshowSource = ItemTheme.SlideshowSource.valueOf(o.get("slideshowSource").getAsString()); }
            catch (IllegalArgumentException ignored) { t.slideshowSource = ItemTheme.SlideshowSource.BUILTIN; }
        }
        return t;
    }

    private static String colorToHex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }

    private static int hexToColor(String hex) {
        try {
            String s = hex.startsWith("#") ? hex.substring(1) : hex;
            return 0xFF000000 | Integer.parseUnsignedInt(s, 16);
        } catch (NumberFormatException e) {
            GTCEUTerminalMod.LOGGER.warn("DefaultThemeConfig: invalid color '{}', using black", hex);
            return 0xFF000000;
        }
    }

    private static Path configPath() {
        return Paths.get(CONFIG_DIR, CONFIG_FILE);
    }
}