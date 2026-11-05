package com.gtceuterminal.common.compat;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gtceuterminal.GTCEUTerminalMod;

import java.lang.reflect.Field;

public final class BlockPatternReflection {

    public static Field F_BLOCK_MATCHES;
    public static Field F_CENTER_OFFSET;
    public static boolean READY = false;

    private BlockPatternReflection() {
    }

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
}
