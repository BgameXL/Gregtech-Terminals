package com.gtceuterminal.common.ae2;

import appeng.api.features.GridLinkables;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.GTCEUTerminalItems;

import com.gtceuterminal.common.util.MiscUtil;

/**
 * Registers GTCEu Terminal items with AE2's GridLinkables system.
 * This class is only loaded when AE2 is present — callers must guard
 * with ModList.get().isLoaded("ae2") before referencing it.
 */
public class AE2Integration {

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;

        // Guard: this class uses appeng.* directly, so it must only be called
        // when AE2 is on the classpath.
        if (!MiscUtil.isAE2Loaded) {
            GTCEUTerminalMod.LOGGER.info("AE2 not present — skipping AE2 integration");
            return;
        }

        try {
            TerminalGridLinkableHandler handler = new TerminalGridLinkableHandler();

            GridLinkables.register(GTCEUTerminalItems.MULTI_STRUCTURE_MANAGER.get(), handler);
            GTCEUTerminalMod.LOGGER.info("Registered Multi-Structure Manager with AE2 GridLinkables");

            GridLinkables.register(GTCEUTerminalItems.SCHEMATIC_INTERFACE.get(), handler);
            GTCEUTerminalMod.LOGGER.info("Registered Schematic Interface with AE2 GridLinkables");

            initialized = true;
            GTCEUTerminalMod.LOGGER.info("AE2 Integration initialized successfully");

        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Failed to initialize AE2 Integration", e);
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}