package com.gtceuterminal.common.config;

import com.gtceuterminal.common.multiblock.ComponentType;

public class ComponentPattern {
    
    private String pattern;
    private ComponentType componentType;
    private int priority = 50;
    private String displayPrefix;
    private String description;
    
    public ComponentPattern() {}
    
    public ComponentPattern(String pattern, ComponentType componentType) {
        this.pattern = pattern;
        this.componentType = componentType;
    }
    
    public ComponentPattern(String pattern, ComponentType componentType, int priority) {
        this.pattern = pattern;
        this.componentType = componentType;
        this.priority = priority;
    }

    public boolean matches(String blockId) {
        if (blockId == null || pattern == null) return false;
        return wildcardMatch(blockId.toLowerCase(), pattern.toLowerCase());
    }

    private boolean wildcardMatch(String text, String pattern) {
        // Convert wildcard pattern to regex
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        
        return text.matches(regex);
    }

    public String getDisplayName() {
        String baseName = componentType.getDisplayName();
        if (displayPrefix != null && !displayPrefix.isEmpty()) {
            return displayPrefix + " " + baseName;
        }
        return baseName;
    }

    public String getPattern() {
        return pattern;
    }
    
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    
    public ComponentType getComponentType() {
        return componentType;
    }
    
    public void setComponentType(ComponentType componentType) {
        this.componentType = componentType;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public String getDisplayPrefix() {
        return displayPrefix;
    }
    
    public void setDisplayPrefix(String displayPrefix) {
        this.displayPrefix = displayPrefix;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "ComponentPattern{" +
                "pattern='" + pattern + '\'' +
                ", componentType=" + componentType +
                ", priority=" + priority +
                ", displayPrefix='" + displayPrefix + '\'' +
                '}';
    }
}