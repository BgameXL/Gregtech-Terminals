package com.gtceuterminal.common.ae2;

import appeng.api.features.GridLinkables;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.GTCEUTerminalItems;

import com.gtceuterminal.common.util.MiscUtil;

public class AE2Integration {

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;

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