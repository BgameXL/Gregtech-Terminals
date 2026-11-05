package com.gtceuterminal.common.config;

import com.gtceuterminal.common.multiblock.ComponentGroup;

public class ComponentPattern {

    private final String pattern;
    private final ComponentGroup group;
    private int priority = 50;
    private String displayPrefix;

    public ComponentPattern(String pattern, ComponentGroup group) {
        this.pattern = pattern;
        this.group = group;
    }

    public boolean matches(String blockId) {
        if (blockId == null || pattern == null) return false;
        String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
        return blockId.toLowerCase().matches(regex);
    }

    public String getDisplayName() {
        String baseName = group != null ? group.displayName : "Unknown";
        if (displayPrefix != null && !displayPrefix.isEmpty()) return displayPrefix + " " + baseName;
        return baseName;
    }

    public String getPattern() {
        return pattern;
    }

    public ComponentGroup getGroup() {
        return group;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setDisplayPrefix(String displayPrefix) {
        this.displayPrefix = displayPrefix;
    }

    @Override
    public String toString() {
        return "ComponentPattern{pattern='" + pattern + "', group=" + (group != null ? group.id : "null")
                + ", priority=" + priority + ", displayPrefix='" + displayPrefix + "'}";
    }
}
