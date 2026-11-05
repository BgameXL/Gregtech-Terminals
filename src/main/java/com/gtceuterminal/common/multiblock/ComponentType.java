package com.gtceuterminal.common.multiblock;

public enum ComponentType {

    ENERGY_HATCH("input_energy"),
    DYNAMO_HATCH("output_energy"),

    WIRELESS_ENERGY_INPUT("wireless_energy_input"),
    WIRELESS_ENERGY_OUTPUT("wireless_energy_output"),
    WIRELESS_LASER_INPUT("wireless_laser_input"),
    WIRELESS_LASER_OUTPUT("wireless_laser_output"),

    SUBSTATION_INPUT_ENERGY("substation_input_energy"),
    SUBSTATION_OUTPUT_ENERGY("substation_output_energy"),

    INPUT_BUS("import_items"),
    OUTPUT_BUS("export_items"),
    STEAM_INPUT_BUS("steam_import_items"),
    STEAM_OUTPUT_BUS("steam_export_items"),

    INPUT_HATCH("import_fluids"),
    OUTPUT_HATCH("export_fluids"),

    INPUT_HATCH_1X("import_fluids_1x"),
    OUTPUT_HATCH_1X("export_fluids_1x"),

    QUAD_INPUT_HATCH("import_fluids_4x"),
    QUAD_OUTPUT_HATCH("export_fluids_4x"),

    NONUPLE_INPUT_HATCH("import_fluids_9x"),
    NONUPLE_OUTPUT_HATCH("export_fluids_9x"),

    MUFFLER("muffler"),
    MAINTENANCE("maintenance"),
    ROTOR_HOLDER("rotor_holder"),
    PUMP_FLUID_HATCH("pump_fluid_hatch"),
    STEAM("steam"),
    TANK_VALVE("tank_valve"),
    PASSTHROUGH_HATCH("passthrough_hatch"),
    PARALLEL_HATCH("parallel_hatch"),

    INPUT_LASER("input_laser"),
    OUTPUT_LASER("output_laser"),

    COMPUTATION_DATA_RECEPTION("computation_data_reception"),
    COMPUTATION_DATA_TRANSMISSION("computation_data_transmission"),
    OPTICAL_DATA_RECEPTION("optical_data_reception"),
    OPTICAL_DATA_TRANSMISSION("optical_data_transmission"),
    DATA_ACCESS("data_access"),

    HPCA_COMPONENT("hpca_component"),
    OBJECT_HOLDER("object_holder"),

    COIL("coil"),
    CASING("casing"),

    MACHINE_HATCH("machine_hatch"),

    DUAL_HATCH("dual_hatch"),

    FILTER("filter"),

    UNKNOWN("unknown");

    private final String abilityId;

    ComponentType(String abilityId) {
        this.abilityId = abilityId;
    }

    public String getAbilityId() {
        return abilityId;
    }
}
