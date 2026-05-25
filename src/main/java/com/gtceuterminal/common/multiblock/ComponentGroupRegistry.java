package com.gtceuterminal.common.multiblock;

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.config.ComponentRegistry;

import net.minecraft.world.level.block.Block;

import java.util.*;

public final class ComponentGroupRegistry {

    private static final Map<String, ComponentGroup> BY_ID          = new LinkedHashMap<>();
    private static final List<ComponentGroup>        ORDERED        = new ArrayList<>();
    private static final Map<String, ComponentGroup> BY_ABILITY     = new HashMap<>();
    private static final Map<String, ComponentGroup> BY_BLOCK_HINT  = new LinkedHashMap<>();

    // Builtin groups
    public static final ComponentGroup ENERGY_HATCH   = reg(ComponentGroup.builder("energy_hatch",   "Energy Hatch")
            .color(0xFF4500).energyHandler().registryCategory(ComponentRegistry.ENERGY_HATCHES)
            .detector(b -> PartAbility.INPUT_ENERGY.isApplicable(b))
            .build(), "input_energy");

    public static final ComponentGroup DYNAMO_HATCH   = reg(ComponentGroup.builder("dynamo_hatch",   "Dynamo Hatch")
            .color(0xFF4500).energyHandler().registryCategory(ComponentRegistry.ENERGY_HATCHES)
            .detector(b -> PartAbility.OUTPUT_ENERGY.isApplicable(b))
            .build(), "output_energy");

    public static final ComponentGroup SUBSTATION_INPUT  = reg(ComponentGroup.builder("substation_input",  "Substation Input Energy")
            .color(0xFF4500).energyHandler().registryCategory(ComponentRegistry.SUBSTATION_HATCHES)
            .detector(b -> PartAbility.SUBSTATION_INPUT_ENERGY.isApplicable(b))
            .build(), "substation_input_energy");

    public static final ComponentGroup SUBSTATION_OUTPUT = reg(ComponentGroup.builder("substation_output", "Substation Output Energy")
            .color(0xFF4500).energyHandler().registryCategory(ComponentRegistry.SUBSTATION_HATCHES)
            .detector(b -> PartAbility.SUBSTATION_OUTPUT_ENERGY.isApplicable(b))
            .build(), "substation_output_energy");

    public static final ComponentGroup WIRELESS_ENERGY_INPUT  = reg(ComponentGroup.builder("wireless_energy_input",  "Wireless Energy Input Hatch")
            .color(0xFF4500).energyHandler().registryCategory(ComponentRegistry.WIRELESS_HATCHES)
            .build(), "wireless_energy_input");

    public static final ComponentGroup WIRELESS_ENERGY_OUTPUT = reg(ComponentGroup.builder("wireless_energy_output", "Wireless Energy Output Hatch")
            .color(0xFF4500).energyHandler().registryCategory(ComponentRegistry.WIRELESS_HATCHES)
            .build(), "wireless_energy_output");

    public static final ComponentGroup WIRELESS_LASER_INPUT  = reg(ComponentGroup.builder("wireless_laser_input",  "Wireless Laser Target Hatch")
            .color(0xFF4500).energyHandler().registryCategory(ComponentRegistry.WIRELESS_HATCHES)
            .build(), "wireless_laser_input");

    public static final ComponentGroup WIRELESS_LASER_OUTPUT = reg(ComponentGroup.builder("wireless_laser_output", "Wireless Laser Source Hatch")
            .color(0xFF4500).energyHandler().registryCategory(ComponentRegistry.WIRELESS_HATCHES)
            .build(), "wireless_laser_output");

    public static final ComponentGroup INPUT_BUS   = reg(ComponentGroup.builder("input_bus",   "Input Bus")
            .color(0xFFD700).itemHandler().registryCategory(ComponentRegistry.BUSES)
            .detector(b -> PartAbility.IMPORT_ITEMS.isApplicable(b))
            .build(), "import_items");

    public static final ComponentGroup OUTPUT_BUS  = reg(ComponentGroup.builder("output_bus",  "Output Bus")
            .color(0xFFD700).itemHandler().registryCategory(ComponentRegistry.BUSES)
            .detector(b -> PartAbility.EXPORT_ITEMS.isApplicable(b))
            .build(), "export_items");

    public static final ComponentGroup STEAM_INPUT_BUS  = reg(ComponentGroup.builder("steam_input_bus",  "Steam Input Bus")
            .color(0xFFD700).itemHandler().registryCategory(ComponentRegistry.BUSES)
            .detector(b -> PartAbility.STEAM_IMPORT_ITEMS.isApplicable(b))
            .build(), "steam_import_items");

    public static final ComponentGroup STEAM_OUTPUT_BUS = reg(ComponentGroup.builder("steam_output_bus", "Steam Output Bus")
            .color(0xFFD700).itemHandler().registryCategory(ComponentRegistry.BUSES)
            .detector(b -> PartAbility.STEAM_EXPORT_ITEMS.isApplicable(b))
            .build(), "steam_export_items");

    public static final ComponentGroup INPUT_HATCH  = reg(ComponentGroup.builder("input_hatch",  "Input Hatch")
            .color(0x4169E1).fluidHandler().registryCategory(ComponentRegistry.FLUID_HATCHES)
            .detector(b -> PartAbility.IMPORT_FLUIDS.isApplicable(b))
            .build(), "import_fluids");

    public static final ComponentGroup OUTPUT_HATCH = reg(ComponentGroup.builder("output_hatch", "Output Hatch")
            .color(0x4169E1).fluidHandler().registryCategory(ComponentRegistry.FLUID_HATCHES)
            .detector(b -> PartAbility.EXPORT_FLUIDS.isApplicable(b))
            .build(), "export_fluids");

    public static final ComponentGroup INPUT_HATCH_1X  = reg(ComponentGroup.builder("input_hatch_1x",  "Input Hatch (1x)")
            .color(0x4169E1).fluidHandler().capacityMultiplier(4).registryCategory(ComponentRegistry.FLUID_HATCHES)
            .detector(b -> PartAbility.IMPORT_FLUIDS_1X.isApplicable(b))
            .build(), "import_fluids_1x");

    public static final ComponentGroup OUTPUT_HATCH_1X = reg(ComponentGroup.builder("output_hatch_1x", "Output Hatch (1x)")
            .color(0x4169E1).fluidHandler().capacityMultiplier(4).registryCategory(ComponentRegistry.FLUID_HATCHES)
            .detector(b -> PartAbility.EXPORT_FLUIDS_1X.isApplicable(b))
            .build(), "export_fluids_1x");

    public static final ComponentGroup QUAD_INPUT_HATCH  = reg(ComponentGroup.builder("quad_input_hatch",  "Quad Input Hatch (4x)")
            .color(0x4169E1).fluidHandler().capacityMultiplier(4).registryCategory(ComponentRegistry.FLUID_HATCHES)
            .detector(b -> PartAbility.IMPORT_FLUIDS_4X.isApplicable(b))
            .build(), "import_fluids_4x");

    public static final ComponentGroup QUAD_OUTPUT_HATCH = reg(ComponentGroup.builder("quad_output_hatch", "Quad Output Hatch (4x)")
            .color(0x4169E1).fluidHandler().capacityMultiplier(4).registryCategory(ComponentRegistry.FLUID_HATCHES)
            .detector(b -> PartAbility.EXPORT_FLUIDS_4X.isApplicable(b))
            .build(), "export_fluids_4x");

    public static final ComponentGroup NONUPLE_INPUT_HATCH  = reg(ComponentGroup.builder("nonuple_input_hatch",  "Nonuple Input Hatch (9x)")
            .color(0x4169E1).fluidHandler().capacityMultiplier(9).registryCategory(ComponentRegistry.FLUID_HATCHES)
            .detector(b -> PartAbility.IMPORT_FLUIDS_9X.isApplicable(b))
            .build(), "import_fluids_9x");

    public static final ComponentGroup NONUPLE_OUTPUT_HATCH = reg(ComponentGroup.builder("nonuple_output_hatch", "Nonuple Output Hatch (9x)")
            .color(0x4169E1).fluidHandler().capacityMultiplier(9).registryCategory(ComponentRegistry.FLUID_HATCHES)
            .detector(b -> PartAbility.EXPORT_FLUIDS_9X.isApplicable(b))
            .build(), "export_fluids_9x");

    public static final ComponentGroup INPUT_LASER  = reg(ComponentGroup.builder("input_laser",  "Input Laser Hatch")
            .color(0xFF4500).energyHandler().registryCategory(ComponentRegistry.LASER_HATCHES)
            .detector(b -> PartAbility.INPUT_LASER.isApplicable(b))
            .build(), "input_laser");

    public static final ComponentGroup OUTPUT_LASER = reg(ComponentGroup.builder("output_laser", "Output Laser Hatch")
            .color(0xFF4500).energyHandler().registryCategory(ComponentRegistry.LASER_HATCHES)
            .detector(b -> PartAbility.OUTPUT_LASER.isApplicable(b))
            .build(), "output_laser");

    public static final ComponentGroup MUFFLER     = reg(ComponentGroup.builder("muffler",     "Muffler Hatch")
            .color(0x808080).special().registryCategory(ComponentRegistry.MUFFLER_HATCHES)
            .detector(b -> PartAbility.MUFFLER.isApplicable(b))
            .build(), "muffler");

    public static final ComponentGroup MAINTENANCE  = reg(ComponentGroup.builder("maintenance",  "Maintenance Hatch")
            .color(0x32CD32).special().registryCategory(ComponentRegistry.MAINTENANCE_HATCHES)
            .detector(b -> PartAbility.MAINTENANCE.isApplicable(b))
            .build(), "maintenance");

    public static final ComponentGroup PARALLEL_HATCH = reg(ComponentGroup.builder("parallel_hatch", "Parallel Hatch")
            .color(0xFFFFFF).special().registryCategory(ComponentRegistry.PARALLEL_HATCHES)
            .detector(b -> PartAbility.PARALLEL_HATCH.isApplicable(b))
            .build(), "parallel_hatch");

    public static final ComponentGroup ROTOR_HOLDER = reg(ComponentGroup.builder("rotor_holder", "Rotor Holder")
            .color(0xFFFFFF).special().notUpgradeable()
            .detector(b -> PartAbility.ROTOR_HOLDER.isApplicable(b))
            .build(), "rotor_holder");

    public static final ComponentGroup DUAL_HATCH   = reg(ComponentGroup.builder("dual_hatch",   "Dual Hatch")
            .color(0x4169E1).fluidHandler().registryCategory(ComponentRegistry.DUAL_HATCHES)
            .build());

    public static final ComponentGroup COIL         = reg(ComponentGroup.builder("coil",         "Heating Coil")
            .color(0xFF6347).registryCategory(ComponentRegistry.COILS)
            .build());

    public static final ComponentGroup CASING        = reg(ComponentGroup.builder("casing",        "Casing")
            .color(0xFFFFFF).notUpgradeable()
            .build());

    public static final ComponentGroup PUMP_FLUID_HATCH = reg(ComponentGroup.builder("pump_fluid_hatch", "Pump Fluid Hatch")
            .color(0x4169E1).fluidHandler()
            .detector(b -> PartAbility.PUMP_FLUID_HATCH.isApplicable(b))
            .build(), "pump_fluid_hatch");

    public static final ComponentGroup STEAM         = reg(ComponentGroup.builder("steam",         "Steam Hatch")
            .color(0xFFFFFF).notUpgradeable()
            .detector(b -> PartAbility.STEAM.isApplicable(b))
            .build(), "steam");

    public static final ComponentGroup TANK_VALVE    = reg(ComponentGroup.builder("tank_valve",    "Tank Valve")
            .color(0xFFFFFF).notUpgradeable()
            .detector(b -> PartAbility.TANK_VALVE.isApplicable(b))
            .build(), "tank_valve");

    public static final ComponentGroup PASSTHROUGH_HATCH = reg(ComponentGroup.builder("passthrough_hatch", "Passthrough Hatch")
            .color(0xFFFFFF)
            .detector(b -> PartAbility.PASSTHROUGH_HATCH.isApplicable(b))
            .build(), "passthrough_hatch");

    public static final ComponentGroup COMPUTATION_DATA_RECEPTION    = reg(ComponentGroup.builder("computation_data_reception",    "Computation Data Reception Hatch")   .color(0x00CED1).dataHandler().detector(b -> PartAbility.COMPUTATION_DATA_RECEPTION.isApplicable(b)).build(),    "computation_data_reception");
    public static final ComponentGroup COMPUTATION_DATA_TRANSMISSION = reg(ComponentGroup.builder("computation_data_transmission", "Computation Data Transmission Hatch").color(0x00CED1).dataHandler().detector(b -> PartAbility.COMPUTATION_DATA_TRANSMISSION.isApplicable(b)).build(), "computation_data_transmission");
    public static final ComponentGroup OPTICAL_DATA_RECEPTION        = reg(ComponentGroup.builder("optical_data_reception",        "Optical Data Reception Hatch")        .color(0x00CED1).dataHandler().detector(b -> PartAbility.OPTICAL_DATA_RECEPTION.isApplicable(b)).build(),        "optical_data_reception");
    public static final ComponentGroup OPTICAL_DATA_TRANSMISSION     = reg(ComponentGroup.builder("optical_data_transmission",     "Optical Data Transmission Hatch")     .color(0x00CED1).dataHandler().detector(b -> PartAbility.OPTICAL_DATA_TRANSMISSION.isApplicable(b)).build(),     "optical_data_transmission");
    public static final ComponentGroup DATA_ACCESS                   = reg(ComponentGroup.builder("data_access",                   "Data Access Hatch")                    .color(0x00CED1).dataHandler().detector(b -> PartAbility.DATA_ACCESS.isApplicable(b)).build(),                   "data_access");
    public static final ComponentGroup HPCA_COMPONENT                = reg(ComponentGroup.builder("hpca_component",                "HPCA Component")                       .color(0x00CED1).dataHandler().detector(b -> PartAbility.HPCA_COMPONENT.isApplicable(b)).build(),                "hpca_component");
    public static final ComponentGroup OBJECT_HOLDER                 = reg(ComponentGroup.builder("object_holder",                 "Object Holder")                        .color(0xFFFFFF).special()     .detector(b -> PartAbility.OBJECT_HOLDER.isApplicable(b)).build(),                 "object_holder");
    public static final ComponentGroup MACHINE_HATCH                 = reg(ComponentGroup.builder("machine_hatch",                 "Machine Hatch")                        .color(0xFFFFFF).build());
    public static final ComponentGroup FILTER                        = reg(ComponentGroup.builder("filter",                        "Filter")                               .color(0xFFFFFF).special().build());
    public static final ComponentGroup UNKNOWN                       = reg(ComponentGroup.builder("unknown",                       "Unknown Component")                    .color(0xFFFFFF).notUpgradeable().build());

    // Registration helpers
    private static ComponentGroup reg(ComponentGroup group, String... abilityNames) {
        BY_ID.put(group.id, group);
        ORDERED.add(group);
        for (String a : abilityNames) BY_ABILITY.put(a, group);
        return group;
    }

    public static synchronized void register(ComponentGroup group, String... abilityNames) {
        if (BY_ID.containsKey(group.id)) {
            GTCEUTerminalMod.LOGGER.warn("ComponentGroupRegistry: duplicate id '{}', skipping", group.id);
            return;
        }
        reg(group, abilityNames);
        GTCEUTerminalMod.LOGGER.info("ComponentGroupRegistry: registered custom group '{}'", group.id);
    }

    public static ComponentGroup byId(String id) {
        return BY_ID.getOrDefault(id, UNKNOWN);
    }

    public static Optional<ComponentGroup> byAbility(String abilityName) {
        return Optional.ofNullable(BY_ABILITY.get(abilityName));
    }

    public static ComponentGroup detectFromBlock(Block block) {
        String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();

        if (blockId.contains("dual_hatch") || blockId.contains("dual_input_hatch"))
            return DUAL_HATCH;

        if (blockId.contains("wireless")) {
            if (blockId.contains("energy")) return blockId.contains("output") ? WIRELESS_ENERGY_OUTPUT : WIRELESS_ENERGY_INPUT;
            if (blockId.contains("laser"))  return (blockId.contains("source") || blockId.contains("output")) ? WIRELESS_LASER_OUTPUT : WIRELESS_LASER_INPUT;
        }

        for (ComponentGroup g : ORDERED) {
            if (g.detector != null && g.detector.test(block)) return g;
        }

        if (blockId.contains("coil"))   return COIL;
        if (blockId.contains("casing")) return CASING;

        return UNKNOWN;
    }

    public static ComponentGroup fromCategory(String category) {
        String lower = category.toLowerCase(java.util.Locale.ROOT);

        ComponentGroup byId = BY_ID.get(lower);
        if (byId != null) return byId;

        ComponentGroup byAbility = BY_ABILITY.get(lower);
        if (byAbility != null) return byAbility;

        for (ComponentGroup g : ORDERED) {
            if (g.displayName.equalsIgnoreCase(category)) return g;
            String stripped = category.replaceFirst("^\\d+A\\s+", "");
            if (g.displayName.equalsIgnoreCase(stripped))  return g;
        }

        if (lower.contains("wireless")) {
            if (lower.contains("energy")) return lower.contains("output") ? WIRELESS_ENERGY_OUTPUT : WIRELESS_ENERGY_INPUT;
            if (lower.contains("laser"))  return (lower.contains("source") || lower.contains("output")) ? WIRELESS_LASER_OUTPUT : WIRELESS_LASER_INPUT;
        }
        if (lower.contains("substation")) return lower.contains("output") ? SUBSTATION_OUTPUT : SUBSTATION_INPUT;
        if (lower.contains("laser"))      return lower.contains("output") ? OUTPUT_LASER : INPUT_LASER;
        if (lower.contains("coil"))       return COIL;
        if (lower.contains("casing"))     return CASING;

        GTCEUTerminalMod.LOGGER.warn("ComponentGroupRegistry: unknown category '{}'", category);
        return UNKNOWN;
    }

    public static Collection<ComponentGroup> all() {
        return Collections.unmodifiableCollection(ORDERED);
    }
}