package com.gtceuterminal.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig {
    public static final String FILE_NAME = "gtceuterminal-server.toml";
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_AE2_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("GTCEu Terminal Addon Server Configuration")
                .push("general");

        ENABLE_AE2_INTEGRATION = builder
                .comment("Enable Applied Energistics 2 integration (default: true)")
                .define("enableAE2Integration", true);

        ENABLE_DEBUG_LOGGING = builder
                .comment("Enable debug logging for troubleshooting (default: false)")
                .define("enableDebugLogging", false);

        builder.pop();

        SPEC = builder.build();
    }

    public static boolean isAE2IntegrationEnabled() {
        try {
            return ENABLE_AE2_INTEGRATION.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    public static boolean isDebugLoggingEnabled() {
        try {
            return ENABLE_DEBUG_LOGGING.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }
}