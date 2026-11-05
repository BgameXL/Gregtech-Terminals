package com.gtceuterminal.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    public static final String FILE_NAME = "gtceuterminal-client.toml";
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue MULTIBLOCK_SCAN_RADIUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("GTCEu Terminal Addon Client Configuration")
                .push("general");

        MULTIBLOCK_SCAN_RADIUS = builder
                .comment("Radius in blocks for the client-side multiblock scan in the Multi-Structure Manager (default: 32)")
                .defineInRange("multiblockScanRadius", 32, 8, 128);

        builder.pop();

        SPEC = builder.build();
    }

    public static int getMultiblockScanRadius() {
        try {
            return MULTIBLOCK_SCAN_RADIUS.get();
        } catch (IllegalStateException e) {
            return 32;
        }
    }
}
