package com.gtceuterminal.common.config;

import com.gtceuterminal.GTCEUTerminalMod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ComponentPatternRegistry {

    private static final List<ComponentPattern> patterns = new ArrayList<>();
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;
        GTCEUTerminalMod.LOGGER.info("Initializing Component Pattern Registry");
        loadPatterns();
        initialized = true;
    }

    public static void loadPatterns() {
        patterns.clear();
        try {
            PatternConfigLoader.loadDefaultPatterns(patterns);
            PatternConfigLoader.loadCustomPatterns(patterns);
            patterns.sort(Comparator.comparingInt(ComponentPattern::getPriority).reversed());
            GTCEUTerminalMod.LOGGER.info("Loaded {} component patterns", patterns.size());
            if (GTCEUTerminalMod.LOGGER.isDebugEnabled()) {
                for (ComponentPattern pattern : patterns) {
                    GTCEUTerminalMod.LOGGER.debug("Pattern: {} -> {} (priority: {})", 
                        pattern.getPattern(), 
                        pattern.getComponentType(), 
                        pattern.getPriority());
                }
            }
            
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Failed to load component patterns", e);
        }
    }

    public static void reload() {
        GTCEUTerminalMod.LOGGER.info("Reloading component patterns");
        loadPatterns();
    }

    public static java.util.Optional<ComponentPattern> findMatch(String blockId) {
        if (blockId == null) return java.util.Optional.empty();

        for (ComponentPattern pattern : patterns) {
            if (pattern.matches(blockId)) {
                return java.util.Optional.of(pattern);
            }
        }

        return java.util.Optional.empty();
    }

    public static List<ComponentPattern> findAllMatches(String blockId) {
        List<ComponentPattern> matches = new ArrayList<>();
        
        if (blockId == null) return matches;
        
        for (ComponentPattern pattern : patterns) {
            if (pattern.matches(blockId)) {
                matches.add(pattern);
            }
        }
        
        return matches;
    }

    public static void registerPattern(ComponentPattern pattern) {
        patterns.add(pattern);
        patterns.sort(Comparator.comparingInt(ComponentPattern::getPriority).reversed());
    }

    public static List<ComponentPattern> getAllPatterns() {
        return new ArrayList<>(patterns);
    }

    public static int getPatternCount() {
        return patterns.size();
    }

    public static void clear() {
        patterns.clear();
    }
}