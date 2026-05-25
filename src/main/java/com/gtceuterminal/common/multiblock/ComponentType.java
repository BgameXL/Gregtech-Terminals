package com.gtceuterminal.common.multiblock;

import net.minecraft.network.chat.Component;

public enum ComponentType {

    ENERGY_HATCH("Energy Hatch", "input_energy"),
    DYNAMO_HATCH("Dynamo Hatch", "output_energy"),

    WIRELESS_ENERGY_INPUT("Wireless Energy Input Hatch", "wireless_energy_input"),
    WIRELESS_ENERGY_OUTPUT("Wireless Energy Output Hatch", "wireless_energy_output"),
    WIRELESS_LASER_INPUT("Wireless Laser Target Hatch", "wireless_laser_input"),
    WIRELESS_LASER_OUTPUT("Wireless Laser Source Hatch", "wireless_laser_output"),

    SUBSTATION_INPUT_ENERGY("Substation Input Energy", "substation_input_energy"),
    SUBSTATION_OUTPUT_ENERGY("Substation Output Energy", "substation_output_energy"),

    INPUT_BUS("Input Bus", "import_items"),
    OUTPUT_BUS("Output Bus", "export_items"),
    STEAM_INPUT_BUS("Steam Input Bus", "steam_import_items"),
    STEAM_OUTPUT_BUS("Steam Output Bus", "steam_export_items"),

    INPUT_HATCH("Input Hatch", "import_fluids"),
    OUTPUT_HATCH("Output Hatch", "export_fluids"),

    INPUT_HATCH_1X("Input Hatch (1x)", "import_fluids_1x"),
    OUTPUT_HATCH_1X("Output Hatch (1x)", "export_fluids_1x"),

    QUAD_INPUT_HATCH("Quad Input Hatch (4x)", "import_fluids_4x"),
    QUAD_OUTPUT_HATCH("Quad Output Hatch (4x)", "export_fluids_4x"),

    NONUPLE_INPUT_HATCH("Nonuple Input Hatch (9x)", "import_fluids_9x"),
    NONUPLE_OUTPUT_HATCH("Nonuple Output Hatch (9x)", "export_fluids_9x"),

    MUFFLER("Muffler Hatch", "muffler"),
    MAINTENANCE("Maintenance Hatch", "maintenance"),
    ROTOR_HOLDER("Rotor Holder", "rotor_holder"),
    PUMP_FLUID_HATCH("Pump Fluid Hatch", "pump_fluid_hatch"),
    STEAM("Steam Hatch", "steam"),
    TANK_VALVE("Tank Valve", "tank_valve"),
    PASSTHROUGH_HATCH("Passthrough Hatch", "passthrough_hatch"),
    PARALLEL_HATCH("Parallel Hatch", "parallel_hatch"),

    INPUT_LASER("Input Laser Hatch", "input_laser"),
    OUTPUT_LASER("Output Laser Hatch", "output_laser"),

    COMPUTATION_DATA_RECEPTION("Computation Data Reception Hatch", "computation_data_reception"),
    COMPUTATION_DATA_TRANSMISSION("Computation Data Transmission Hatch", "computation_data_transmission"),
    OPTICAL_DATA_RECEPTION("Optical Data Reception Hatch", "optical_data_reception"),
    OPTICAL_DATA_TRANSMISSION("Optical Data Transmission Hatch", "optical_data_transmission"),
    DATA_ACCESS("Data Access Hatch", "data_access"),

    HPCA_COMPONENT("HPCA Component", "hpca_component"),
    OBJECT_HOLDER("Object Holder", "object_holder"),

    COIL("Heating Coil", "coil"),
    CASING("Casing", "casing"),

    // This one is a VERY specific hatch
    MACHINE_HATCH("Machine Hatch", "machine_hatch"),

    DUAL_HATCH("Dual Hatch", "dual_hatch"),

    FILTER("Filter", "filter"),

    UNKNOWN("Unknown Component", "unknown");

    private final String displayName;
    private final String abilityId;

    ComponentType(String displayName, String abilityId) {
        this.displayName = displayName;
        this.abilityId = abilityId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayNameKey() {
        return "component.gtceuterminal.component_type." + name().toLowerCase();
    }

    public Component getDisplayNameComponent() {
        return Component.translatable(getDisplayNameKey());
    }

    public static String getCoilTierName(int tier) {
        String suffix = switch (tier) {
            case 0 -> "cupronickel";
            case 1 -> "kanthal";
            case 2 -> "nichrome";
            case 3 -> "rtm_alloy";
            case 4 -> "hss_g";
            case 5 -> "naquadah";
            case 6 -> "trinium";
            case 7 -> "tritanium";
            default -> "unknown";
        };
        return Component.translatable("gui.gtceuterminal.coil_tier." + suffix).getString();
    }

    public String getAbilityId() {
        return abilityId;
    }

    public boolean isUpgradeable() {
        return this != UNKNOWN &&
                this != CASING &&
                this != STEAM &&
                this != TANK_VALVE;
    }


    public boolean isFluidHandler() {
        return this == INPUT_HATCH ||
                this == OUTPUT_HATCH ||
                this == INPUT_HATCH_1X ||
                this == OUTPUT_HATCH_1X ||
                this == QUAD_INPUT_HATCH ||
                this == QUAD_OUTPUT_HATCH ||
                this == NONUPLE_INPUT_HATCH ||
                this == NONUPLE_OUTPUT_HATCH ||
                this == PUMP_FLUID_HATCH ||
                this == DUAL_HATCH;
    }

    public boolean isItemHandler() {
        return this == INPUT_BUS ||
                this == OUTPUT_BUS ||
                this == STEAM_INPUT_BUS ||
                this == STEAM_OUTPUT_BUS;
    }

    public boolean isEnergyHandler() {
        return this == ENERGY_HATCH ||
                this == DYNAMO_HATCH ||
                this == WIRELESS_ENERGY_INPUT ||
                this == WIRELESS_ENERGY_OUTPUT ||
                this == SUBSTATION_INPUT_ENERGY ||
                this == SUBSTATION_OUTPUT_ENERGY ||
                this == INPUT_LASER ||
                this == OUTPUT_LASER ||
                this == WIRELESS_LASER_INPUT ||
                this == WIRELESS_LASER_OUTPUT;
    }

    public boolean isDataHandler() {
        return this == COMPUTATION_DATA_RECEPTION ||
                this == COMPUTATION_DATA_TRANSMISSION ||
                this == OPTICAL_DATA_RECEPTION ||
                this == OPTICAL_DATA_TRANSMISSION ||
                this == DATA_ACCESS ||
                this == HPCA_COMPONENT;
    }

    public boolean isSpecial() {
        return this == MAINTENANCE ||
                this == MUFFLER ||
                this == ROTOR_HOLDER ||
                this == PARALLEL_HATCH ||
                this == OBJECT_HOLDER ||
                this == FILTER;
    }

    public int getCapacityMultiplier() {
        return switch (this) {
            case QUAD_INPUT_HATCH, QUAD_OUTPUT_HATCH, INPUT_HATCH_1X, OUTPUT_HATCH_1X -> 4;
            case NONUPLE_INPUT_HATCH, NONUPLE_OUTPUT_HATCH -> 9;
            default -> 1;
        };
    }

    public int getColor() {
        if (isFluidHandler()) return 0x4169E1;  // Blue (fluids)
        if (isItemHandler()) return 0xFFD700;   // Gold (items)
        if (isEnergyHandler()) return 0xFF4500; // Orange (energy)
        if (isDataHandler()) return 0x00CED1;   // Cyan (data)
        if (this == MAINTENANCE) return 0x32CD32;  // Green (maintenance)
        if (this == MUFFLER) return 0x808080;      // Gray (muffler)
        if (this == COIL) return 0xFF6347;         // Red (coil)
        return 0xFFFFFF;  // White (default)
    }

    public static ComponentType fromGroup(ComponentGroup group) {
        if (group == null) return UNKNOWN;
        return switch (group.id) {
            case "energy_hatch" -> ENERGY_HATCH;
            case "dynamo_hatch" -> DYNAMO_HATCH;
            case "wireless_energy_input" -> WIRELESS_ENERGY_INPUT;
            case "wireless_energy_output" -> WIRELESS_ENERGY_OUTPUT;
            case "wireless_laser_input" -> WIRELESS_LASER_INPUT;
            case "wireless_laser_output" -> WIRELESS_LASER_OUTPUT;
            case "substation_input" -> SUBSTATION_INPUT_ENERGY;
            case "substation_output" -> SUBSTATION_OUTPUT_ENERGY;
            case "input_bus" -> INPUT_BUS;
            case "output_bus" -> OUTPUT_BUS;
            case "steam_input_bus" -> STEAM_INPUT_BUS;
            case "steam_output_bus" -> STEAM_OUTPUT_BUS;
            case "input_hatch" -> INPUT_HATCH;
            case "output_hatch" -> OUTPUT_HATCH;
            case "input_hatch_1x" -> INPUT_HATCH_1X;
            case "output_hatch_1x" -> OUTPUT_HATCH_1X;
            case "quad_input_hatch" -> QUAD_INPUT_HATCH;
            case "quad_output_hatch" -> QUAD_OUTPUT_HATCH;
            case "nonuple_input_hatch" -> NONUPLE_INPUT_HATCH;
            case "nonuple_output_hatch" -> NONUPLE_OUTPUT_HATCH;
            case "input_laser" -> INPUT_LASER;
            case "output_laser" -> OUTPUT_LASER;
            case "muffler" -> MUFFLER;
            case "maintenance" -> MAINTENANCE;
            case "parallel_hatch" -> PARALLEL_HATCH;
            case "rotor_holder" -> ROTOR_HOLDER;
            case "dual_hatch" -> DUAL_HATCH;
            case "coil" -> COIL;
            case "casing" -> CASING;
            case "pump_fluid_hatch" -> PUMP_FLUID_HATCH;
            case "steam" -> STEAM;
            case "tank_valve" -> TANK_VALVE;
            case "passthrough_hatch" -> PASSTHROUGH_HATCH;
            case "computation_data_reception" -> COMPUTATION_DATA_RECEPTION;
            case "computation_data_transmission" -> COMPUTATION_DATA_TRANSMISSION;
            case "optical_data_reception" -> OPTICAL_DATA_RECEPTION;
            case "optical_data_transmission" -> OPTICAL_DATA_TRANSMISSION;
            case "data_access" -> DATA_ACCESS;
            case "hpca_component" -> HPCA_COMPONENT;
            case "object_holder" -> OBJECT_HOLDER;
            case "machine_hatch" -> MACHINE_HATCH;
            case "filter" -> FILTER;
            default -> UNKNOWN;
        };
    }
}