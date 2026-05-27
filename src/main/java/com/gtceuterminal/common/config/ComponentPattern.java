package com.gtceuterminal.common.config;

import com.gtceuterminal.common.multiblock.ComponentGroup;

public class ComponentPattern {

    private String pattern;
    private ComponentGroup group;
    private int priority = 50;
    private String displayPrefix;
    private String description;

    public ComponentPattern() {}

    public ComponentPattern(String pattern, ComponentGroup group) {
        this.pattern = pattern;
        this.group   = group;
    }

    public ComponentPattern(String pattern, ComponentGroup group, int priority) {
        this.pattern  = pattern;
        this.group    = group;
        this.priority = priority;
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

    public String  getPattern()       { return pattern; }
    public void    setPattern(String pattern) { this.pattern = pattern; }

    public ComponentGroup getGroup()  { return group; }
    public void    setGroup(ComponentGroup group) { this.group = group; }

    public int     getPriority()      { return priority; }
    public void    setPriority(int priority) { this.priority = priority; }

    public String  getDisplayPrefix() { return displayPrefix; }
    public void    setDisplayPrefix(String displayPrefix) { this.displayPrefix = displayPrefix; }

    public String  getDescription()   { return description; }
    public void    setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "ComponentPattern{pattern='" + pattern + "', group=" + (group != null ? group.id : "null")
                + ", priority=" + priority + ", displayPrefix='" + displayPrefix + "'}";
    }
}
