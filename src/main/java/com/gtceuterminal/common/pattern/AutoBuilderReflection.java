package com.gtceuterminal.common.pattern;

import com.gtceuterminal.GTCEUTerminalMod;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;

import java.lang.reflect.Field;

final class AutoBuilderReflection {

    static Field F_BLOCK_MATCHES;
    static Field F_AISLE_REP;
    static Field F_STRUCTURE_DIR;
    static Field F_CENTER_OFFSET;
    static boolean READY = false;

    private AutoBuilderReflection() {}

    static void ensure() {
        if (READY) return;
        try {
            F_BLOCK_MATCHES = BlockPattern.class.getDeclaredField("blockMatches");
            F_BLOCK_MATCHES.setAccessible(true);
            F_AISLE_REP = BlockPattern.class.getDeclaredField("aisleRepetitions");
            F_AISLE_REP.setAccessible(true);
            F_STRUCTURE_DIR = BlockPattern.class.getDeclaredField("structureDir");
            F_STRUCTURE_DIR.setAccessible(true);
            F_CENTER_OFFSET = BlockPattern.class.getDeclaredField("centerOffset");
            F_CENTER_OFFSET.setAccessible(true);
            READY = true;
        } catch (Throwable t) {
            READY = false;
            GTCEUTerminalMod.LOGGER.error("AutoBuilderReflection: failed to init BlockPattern fields", t);
        }
    }
}