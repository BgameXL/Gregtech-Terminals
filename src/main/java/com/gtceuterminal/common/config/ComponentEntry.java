package com.gtceuterminal.common.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ComponentEntry {

    public final String blockId;
    public final String displayName;
    public final String tierName;
    public final int    tier;
    public final Map<String, String> attrs;

    public ComponentEntry(String blockId, String displayName, String tierName, int tier,
                          Map<String, String> attrs) {
        this.blockId     = blockId;
        this.displayName = displayName;
        this.tierName    = tierName;
        this.tier        = tier;
        this.attrs       = attrs != null ? Collections.unmodifiableMap(new HashMap<>(attrs)) : Collections.emptyMap();
    }

    public ComponentEntry(String blockId, String displayName, String tierName, int tier) {
        this(blockId, displayName, tierName, tier, null);
    }
    public String attr(String key) {
        return attrs.get(key);
    }

    public String attr(String key, String defaultValue) {
        return attrs.getOrDefault(key, defaultValue);
    }

    public boolean attrIs(String key, String value) {
        String v = attrs.get(key);
        return v != null && v.equalsIgnoreCase(value);
    }

    @Override
    public String toString() {
        return "ComponentEntry{" + blockId + ", tier=" + tier + ", attrs=" + attrs + "}";
    }

    public static Builder builder(String blockId, String displayName, String tierName, int tier) {
        return new Builder(blockId, displayName, tierName, tier);
    }

    public static final class Builder {
        private final String blockId, displayName, tierName;
        private final int tier;
        private final Map<String, String> attrs = new HashMap<>();

        private Builder(String blockId, String displayName, String tierName, int tier) {
            this.blockId = blockId; this.displayName = displayName;
            this.tierName = tierName; this.tier = tier;
        }

        public Builder attr(String key, String value) { attrs.put(key, value); return this; }

        public ComponentEntry build() { return new ComponentEntry(blockId, displayName, tierName, tier, attrs); }
    }
}