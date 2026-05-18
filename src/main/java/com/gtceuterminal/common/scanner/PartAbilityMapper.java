package com.gtceuterminal.common.scanner;

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gtceuterminal.common.multiblock.ComponentType;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps GTCEu PartAbility to ComponentType
 * This is more reliable than string parsing and works automatically with addons
 */
public class PartAbilityMapper {

    private static final Map<String, ComponentType> ABILITY_TO_TYPE = new HashMap<>();

    static {

        ABILITY_TO_TYPE.put("input_energy", ComponentType.ENERGY_HATCH);
        ABILITY_TO_TYPE.put("output_energy", ComponentType.DYNAMO_HATCH);
        ABILITY_TO_TYPE.put("substation_input_energy", ComponentType.SUBSTATION_INPUT_ENERGY);
        ABILITY_TO_TYPE.put("substation_output_energy", ComponentType.SUBSTATION_OUTPUT_ENERGY);

        ABILITY_TO_TYPE.put("import_items", ComponentType.INPUT_BUS);
        ABILITY_TO_TYPE.put("export_items", ComponentType.OUTPUT_BUS);
        ABILITY_TO_TYPE.put("steam_import_items", ComponentType.STEAM_INPUT_BUS);
        ABILITY_TO_TYPE.put("steam_export_items", ComponentType.STEAM_OUTPUT_BUS);

        ABILITY_TO_TYPE.put("import_fluids", ComponentType.INPUT_HATCH);
        ABILITY_TO_TYPE.put("export_fluids", ComponentType.OUTPUT_HATCH);
        ABILITY_TO_TYPE.put("import_fluids_1x", ComponentType.INPUT_HATCH_1X);
        ABILITY_TO_TYPE.put("export_fluids_1x", ComponentType.OUTPUT_HATCH_1X);
        ABILITY_TO_TYPE.put("import_fluids_4x", ComponentType.QUAD_INPUT_HATCH);
        ABILITY_TO_TYPE.put("export_fluids_4x", ComponentType.QUAD_OUTPUT_HATCH);
        ABILITY_TO_TYPE.put("import_fluids_9x", ComponentType.NONUPLE_INPUT_HATCH);
        ABILITY_TO_TYPE.put("export_fluids_9x", ComponentType.NONUPLE_OUTPUT_HATCH);

        ABILITY_TO_TYPE.put("input_laser", ComponentType.INPUT_LASER);
        ABILITY_TO_TYPE.put("output_laser", ComponentType.OUTPUT_LASER);

        ABILITY_TO_TYPE.put("wireless_energy_input", ComponentType.WIRELESS_ENERGY_INPUT);
        ABILITY_TO_TYPE.put("wireless_energy_output", ComponentType.WIRELESS_ENERGY_OUTPUT);
        ABILITY_TO_TYPE.put("wireless_laser_input", ComponentType.WIRELESS_LASER_INPUT);
        ABILITY_TO_TYPE.put("wireless_laser_output", ComponentType.WIRELESS_LASER_OUTPUT);

        ABILITY_TO_TYPE.put("computation_data_reception", ComponentType.COMPUTATION_DATA_RECEPTION);
        ABILITY_TO_TYPE.put("computation_data_transmission", ComponentType.COMPUTATION_DATA_TRANSMISSION);
        ABILITY_TO_TYPE.put("optical_data_reception", ComponentType.OPTICAL_DATA_RECEPTION);
        ABILITY_TO_TYPE.put("optical_data_transmission", ComponentType.OPTICAL_DATA_TRANSMISSION);
        ABILITY_TO_TYPE.put("data_access", ComponentType.DATA_ACCESS);

        ABILITY_TO_TYPE.put("hpca_component", ComponentType.HPCA_COMPONENT);
        ABILITY_TO_TYPE.put("object_holder", ComponentType.OBJECT_HOLDER);

        ABILITY_TO_TYPE.put("maintenance", ComponentType.MAINTENANCE);
        ABILITY_TO_TYPE.put("muffler", ComponentType.MUFFLER);
        ABILITY_TO_TYPE.put("rotor_holder", ComponentType.ROTOR_HOLDER);
        ABILITY_TO_TYPE.put("pump_fluid_hatch", ComponentType.PUMP_FLUID_HATCH);
        ABILITY_TO_TYPE.put("steam", ComponentType.STEAM);
        ABILITY_TO_TYPE.put("tank_valve", ComponentType.TANK_VALVE);
        ABILITY_TO_TYPE.put("passthrough_hatch", ComponentType.PASSTHROUGH_HATCH);
        ABILITY_TO_TYPE.put("parallel_hatch", ComponentType.PARALLEL_HATCH);
    }

    public static java.util.Optional<ComponentType> fromAbility(PartAbility ability) {
        if (ability == null) return java.util.Optional.empty();
        return java.util.Optional.ofNullable(ABILITY_TO_TYPE.get(ability.getName()));
    }

    public static java.util.Optional<ComponentType> fromAbilityName(String abilityName) {
        if (abilityName == null) return java.util.Optional.empty();
        return java.util.Optional.ofNullable(ABILITY_TO_TYPE.get(abilityName));
    }

    public static ComponentType detectFromBlock(Block block) {
        // Get block ID for specific checks
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

        if (blockId.contains("dual_hatch") || blockId.contains("dual_input_hatch")) {
            return ComponentType.DUAL_HATCH;
        }

        if (blockId.contains("wireless")) {
            if (blockId.contains("energy")) {
                if (blockId.contains("input")) return ComponentType.WIRELESS_ENERGY_INPUT;
                if (blockId.contains("output")) return ComponentType.WIRELESS_ENERGY_OUTPUT;
            }
            if (blockId.contains("laser")) {
                if (blockId.contains("target")) return ComponentType.WIRELESS_LASER_INPUT;
                if (blockId.contains("source")) return ComponentType.WIRELESS_LASER_OUTPUT;
            }
        }

        if (PartAbility.INPUT_ENERGY.isApplicable(block)) return ComponentType.ENERGY_HATCH;
        if (PartAbility.OUTPUT_ENERGY.isApplicable(block)) return ComponentType.DYNAMO_HATCH;
        if (PartAbility.SUBSTATION_INPUT_ENERGY.isApplicable(block)) return ComponentType.SUBSTATION_INPUT_ENERGY;
        if (PartAbility.SUBSTATION_OUTPUT_ENERGY.isApplicable(block)) return ComponentType.SUBSTATION_OUTPUT_ENERGY;
        if (PartAbility.IMPORT_ITEMS.isApplicable(block)) return ComponentType.INPUT_BUS;
        if (PartAbility.EXPORT_ITEMS.isApplicable(block)) return ComponentType.OUTPUT_BUS;
        if (PartAbility.STEAM_IMPORT_ITEMS.isApplicable(block)) return ComponentType.STEAM_INPUT_BUS;
        if (PartAbility.STEAM_EXPORT_ITEMS.isApplicable(block)) return ComponentType.STEAM_OUTPUT_BUS;

        if (PartAbility.IMPORT_FLUIDS.isApplicable(block)) return ComponentType.INPUT_HATCH;
        if (PartAbility.EXPORT_FLUIDS.isApplicable(block)) return ComponentType.OUTPUT_HATCH;
        if (PartAbility.IMPORT_FLUIDS_1X.isApplicable(block)) return ComponentType.INPUT_HATCH_1X;
        if (PartAbility.EXPORT_FLUIDS_1X.isApplicable(block)) return ComponentType.OUTPUT_HATCH_1X;
        if (PartAbility.IMPORT_FLUIDS_4X.isApplicable(block)) return ComponentType.QUAD_INPUT_HATCH;
        if (PartAbility.EXPORT_FLUIDS_4X.isApplicable(block)) return ComponentType.QUAD_OUTPUT_HATCH;
        if (PartAbility.IMPORT_FLUIDS_9X.isApplicable(block)) return ComponentType.NONUPLE_INPUT_HATCH;
        if (PartAbility.EXPORT_FLUIDS_9X.isApplicable(block)) return ComponentType.NONUPLE_OUTPUT_HATCH;

        if (PartAbility.INPUT_LASER.isApplicable(block)) return ComponentType.INPUT_LASER;
        if (PartAbility.OUTPUT_LASER.isApplicable(block)) return ComponentType.OUTPUT_LASER;

        if (PartAbility.COMPUTATION_DATA_RECEPTION.isApplicable(block)) return ComponentType.COMPUTATION_DATA_RECEPTION;
        if (PartAbility.COMPUTATION_DATA_TRANSMISSION.isApplicable(block)) return ComponentType.COMPUTATION_DATA_TRANSMISSION;
        if (PartAbility.OPTICAL_DATA_RECEPTION.isApplicable(block)) return ComponentType.OPTICAL_DATA_RECEPTION;
        if (PartAbility.OPTICAL_DATA_TRANSMISSION.isApplicable(block)) return ComponentType.OPTICAL_DATA_TRANSMISSION;
        if (PartAbility.DATA_ACCESS.isApplicable(block)) return ComponentType.DATA_ACCESS;

        if (PartAbility.HPCA_COMPONENT.isApplicable(block)) return ComponentType.HPCA_COMPONENT;
        if (PartAbility.OBJECT_HOLDER.isApplicable(block)) return ComponentType.OBJECT_HOLDER;

        if (PartAbility.MAINTENANCE.isApplicable(block)) return ComponentType.MAINTENANCE;
        if (PartAbility.MUFFLER.isApplicable(block)) return ComponentType.MUFFLER;
        if (PartAbility.ROTOR_HOLDER.isApplicable(block)) return ComponentType.ROTOR_HOLDER;
        if (PartAbility.PUMP_FLUID_HATCH.isApplicable(block)) return ComponentType.PUMP_FLUID_HATCH;
        if (PartAbility.STEAM.isApplicable(block)) return ComponentType.STEAM;
        if (PartAbility.TANK_VALVE.isApplicable(block)) return ComponentType.TANK_VALVE;
        if (PartAbility.PASSTHROUGH_HATCH.isApplicable(block)) return ComponentType.PASSTHROUGH_HATCH;
        if (PartAbility.PARALLEL_HATCH.isApplicable(block)) return ComponentType.PARALLEL_HATCH;

        return null;
    }

    public static boolean isMapped(String abilityName) {
        return abilityName != null && ABILITY_TO_TYPE.containsKey(abilityName);
    }
}