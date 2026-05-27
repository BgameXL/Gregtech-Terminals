package com.gtceuterminal.common.compat;

import com.gtceuterminal.GTCEUTerminalMod;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import java.lang.reflect.Field;

public final class BlockPatternReflection {

    public static Field F_BLOCK_MATCHES;
    public static Field F_CENTER_OFFSET;
    public static boolean READY = false;

    private BlockPatternReflection() {}

    public static synchronized void ensure() {
        if (READY) return;
        try {
            F_BLOCK_MATCHES = field("blockMatches");
            F_CENTER_OFFSET = field("centerOffset");
            READY = true;
        } catch (Throwable t) {
            READY = false;
            GTCEUTerminalMod.LOGGER.error("BlockPatternReflection: failed to init BlockPattern fields", t);
        }
    }

    private static Field field(String name) throws ReflectiveOperationException {
        Field f = BlockPattern.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    @SuppressWarnings("unchecked")
    public static TraceabilityPredicate[][][] getBlockMatches(BlockPattern pattern) {
        ensure();
        if (!READY) return null;
        try { return (TraceabilityPredicate[][][]) F_BLOCK_MATCHES.get(pattern); }
        catch (IllegalAccessException e) { GTCEUTerminalMod.LOGGER.error("BlockPatternReflection: getBlockMatches", e); return null; }
    }

    public static int[] getCenterOffset(BlockPattern pattern) {
        ensure();
        if (!READY) return null;
        try { return (int[]) F_CENTER_OFFSET.get(pattern); }
        catch (IllegalAccessException e) { GTCEUTerminalMod.LOGGER.error("BlockPatternReflection: getCenterOffset", e); return null; }
    }
}
