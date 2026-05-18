package com.gtceuterminal.common.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.theme.ItemTheme;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

// This was for testing.
public class ThemeCodec {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String PREFIX = "gtcet:";

    public static String encode(ItemTheme theme) {
        JsonObject o = new JsonObject();
        o.addProperty("a", colorToHex(theme.accentColor));
        o.addProperty("b", colorToHex(theme.bgColor));
        o.addProperty("p", colorToHex(theme.panelColor));
        o.addProperty("t", colorToHex(theme.textColor));
        o.addProperty("c", theme.compactMode);
        o.addProperty("s", theme.showTooltips);
        o.addProperty("r", theme.showBorders);
        String json    = GSON.toJson(o);
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return PREFIX + encoded;
    }

    public static ItemTheme decode(String code) {
        try {
            String raw = code.trim();
            if (raw.startsWith(PREFIX)) raw = raw.substring(PREFIX.length());
            String json = new String(Base64.getDecoder().decode(raw), StandardCharsets.UTF_8);
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();

            ItemTheme t = new ItemTheme();
            if (o.has("a")) t.accentColor  = hexToColor(o.get("a").getAsString());
            if (o.has("b")) t.bgColor      = hexToColor(o.get("b").getAsString());
            if (o.has("p")) t.panelColor   = hexToColor(o.get("p").getAsString());
            if (o.has("t")) t.textColor    = hexToColor(o.get("t").getAsString());
            if (o.has("c")) t.compactMode  = o.get("c").getAsBoolean();
            if (o.has("s")) t.showTooltips = o.get("s").getAsBoolean();
            if (o.has("r")) t.showBorders  = o.get("r").getAsBoolean();
            return t;
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("ThemeCodec: invalid code '{}': {}", code, e.getMessage());
            return null;
        }
    }

    private static String colorToHex(int argb) {
        return String.format("#%08X", argb);
    }

    private static int hexToColor(String hex) {
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        if (s.length() == 6) return 0xFF000000 | Integer.parseUnsignedInt(s, 16);
        return (int) Long.parseLong(s, 16);
    }
}