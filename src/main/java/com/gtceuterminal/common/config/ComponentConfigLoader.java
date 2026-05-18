package com.gtceuterminal.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.gregtechceu.gtceu.api.GTValues;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ComponentConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentConfigLoader.class);
    private static final Gson   GSON   = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "config/gtceuterminal";

    private ComponentConfigLoader() {}

    public static List<ComponentEntry> load(
            String fileName,
            String description,
            Supplier<List<ComponentEntry>> defaultFactory
    ) {
        Path configPath = Paths.get(CONFIG_DIR, fileName);

        if (!Files.exists(configPath)) {
            LOGGER.info("{} not found, writing defaults", fileName);
            List<ComponentEntry> defaults = defaultFactory.get();
            writeDefaults(configPath, description, defaults);
            return new ArrayList<>(defaults);
        }

        try {
            String json = Files.readString(configPath);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            return parse(root);
        } catch (Exception e) {
            LOGGER.error("Failed to load {}, falling back to defaults", fileName, e);
            return new ArrayList<>(defaultFactory.get());
        }
    }

    private static List<ComponentEntry> parse(JsonObject root) {
        List<ComponentEntry> result = new ArrayList<>();

        if (root.has("patterns")) {
            for (JsonElement el : root.getAsJsonArray("patterns")) {
                result.addAll(expandPattern(el.getAsJsonObject()));
            }
        }

        for (String key : new String[]{"entries", "hatches", "buses", "coils"}) {
            if (root.has(key)) {
                for (JsonElement el : root.getAsJsonArray(key)) {
                    result.add(parseEntry(el.getAsJsonObject()));
                }
                break;
            }
        }

        return result;
    }

    private static List<ComponentEntry> expandPattern(JsonObject p) {
        List<ComponentEntry> entries = new ArrayList<>();

        String blockIdPattern = p.has("blockIdPattern") ? p.get("blockIdPattern").getAsString() : null;
        if (blockIdPattern == null) return entries;

        String componentType = p.has("componentType") ? p.get("componentType").getAsString() : "";
        int minTier = p.has("minTier") ? p.get("minTier").getAsInt() : 0;
        int maxTier = p.has("maxTier") ? p.get("maxTier").getAsInt() : GTValues.VN.length - 1;

        Map<String, String> baseAttrs = new HashMap<>();
        if (p.has("attrs")) {
            for (Map.Entry<String, JsonElement> e : p.getAsJsonObject("attrs").entrySet()) {
                baseAttrs.put(e.getKey(), e.getValue().getAsString());
            }
        }

        for (int tier = minTier; tier <= maxTier && tier < GTValues.VN.length; tier++) {
            String tierName  = GTValues.VN[tier];
            String tierLower = tierName.toLowerCase();
            String blockId   = blockIdPattern.replace("*", tierLower);
            String displayName = generateDisplayName(tierName, componentType, baseAttrs);

            entries.add(new ComponentEntry(blockId, displayName, tierName, tier, new HashMap<>(baseAttrs)));
        }

        return entries;
    }

    private static ComponentEntry parseEntry(JsonObject o) {
        String blockId     = o.has("blockId")     ? o.get("blockId").getAsString()     : "";
        String displayName = o.has("displayName") ? o.get("displayName").getAsString() : "";
        String tierName    = o.has("tierName")    ? o.get("tierName").getAsString()    : "";
        int tier           = o.has("tier")        ? o.get("tier").getAsInt()           : 0;

        Map<String, String> attrs = new HashMap<>();
        for (String key : new String[]{
                "hatchType", "busType", "amperage", "capacity", "variant",
                "maintenanceType", "heatCapacity", "coilType", "source", "wireless"}) {
            if (o.has(key)) attrs.put(key, o.get(key).getAsString());
        }
        if (o.has("attrs")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("attrs").entrySet()) {
                attrs.put(e.getKey(), e.getValue().getAsString());
            }
        }

        return new ComponentEntry(blockId, displayName, tierName, tier, attrs);
    }

    private static void writeDefaults(Path configPath, String description, List<ComponentEntry> entries) {
        try {
            Files.createDirectories(configPath.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", "3.0");
            root.addProperty("description", description);
            JsonArray arr = new JsonArray();
            for (ComponentEntry e : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("blockId",     e.blockId);
                obj.addProperty("displayName", e.displayName);
                obj.addProperty("tierName",    e.tierName);
                obj.addProperty("tier",        e.tier);
                if (!e.attrs.isEmpty()) {
                    JsonObject attrsObj = new JsonObject();
                    e.attrs.forEach(attrsObj::addProperty);
                    obj.add("attrs", attrsObj);
                }
                arr.add(obj);
            }
            root.add("entries", arr);
            Files.writeString(configPath, GSON.toJson(root));
            LOGGER.info("Wrote default config to {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to write defaults to {}", configPath, e);
        }
    }

    static String generateDisplayName(String tierName, String componentType, Map<String, String> attrs) {
        String amperage = attrs.getOrDefault("amperage", "");
        String capacity = attrs.getOrDefault("capacity", "");

        String base = capitalizeWords(componentType.replace("_", " ").toLowerCase());
        StringBuilder sb = new StringBuilder(tierName);
        if (!amperage.isEmpty() && !amperage.equals("2A")) sb.append(" ").append(amperage);
        sb.append(" ").append(base);
        if (!capacity.isEmpty() && !capacity.equals("1x")) sb.append(" (").append(capacity).append(")");
        return sb.toString().trim();
    }

    public static String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return text;
        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) result.append(word.substring(1).toLowerCase());
                result.append(" ");
            }
        }
        return result.toString().trim();
    }
}