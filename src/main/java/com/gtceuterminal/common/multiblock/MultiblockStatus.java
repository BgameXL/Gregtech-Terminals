package com.gtceuterminal.common.multiblock;

public enum MultiblockStatus {
    ACTIVE,
    IDLE,
    NEEDS_MAINTENANCE,
    NO_POWER,
    DISABLED,
    UNFORMED,
    OUTPUT_FULL;

    public int getColor() {
        return switch (this) {
            case ACTIVE -> 0x00FF00;
            case IDLE -> 0xFFFF00;
            case NEEDS_MAINTENANCE -> 0xFF8800;
            case NO_POWER -> 0xFF0000;
            case DISABLED -> 0x808080;
            case UNFORMED -> 0xFF0000;
            case OUTPUT_FULL -> 0x0088FF;
        };
    }

    public String getDisplayName() {
        return switch (this) {
            case ACTIVE -> "Active";
            case IDLE -> "Idle";
            case NEEDS_MAINTENANCE -> "Needs Maintenance";
            case NO_POWER -> "No Power";
            case DISABLED -> "Disabled";
            case UNFORMED -> "Unformed";
            case OUTPUT_FULL -> "Output Full";
        };
    }
}