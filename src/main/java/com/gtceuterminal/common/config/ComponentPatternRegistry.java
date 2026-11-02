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
                            pattern.getGroup() != null ? pattern.getGroup().id : "null",
                            pattern.getPriority());
                }
            }

        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Failed to load component patterns", e);
        }
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
}