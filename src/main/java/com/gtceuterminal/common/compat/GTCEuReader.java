package com.gtceuterminal.common.compat;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.machines.GCYMMachines;
import com.gtceuterminal.common.config.ComponentEntry;

import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GTCEuReader {

    private GTCEuReader() {}

    public static List<ComponentEntry> readCoils() {
        List<ComponentEntry> entries = new ArrayList<>();
        List<ICoilType> sorted = GTCEuAPI.HEATING_COILS.keySet().stream()
                .sorted(java.util.Comparator.comparingInt(ICoilType::getTier))
                .toList();
        for (ICoilType coil : sorted) {
            var supplier = GTCEuAPI.HEATING_COILS.get(coil);
            if (supplier == null) continue;
            var block = supplier.get();
            if (block == null) continue;
            String blockId      = BuiltInRegistries.BLOCK.getKey(block).toString();
            String displayName  = net.minecraft.network.chat.Component.translatable(block.getDescriptionId()).getString();
            String tierName     = "T" + coil.getTier();
            entries.add(ComponentEntry.builder(blockId, displayName, tierName, coil.getTier())
                    .attr("coilType", coil.getName())
                    .attr("heatCapacity", coil.getCoilTemperature() + "K")
                    .build());
        }

        return entries;
    }

    public static List<ComponentEntry> readFluidHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        addTieredHatches(entries, GTMachines.FLUID_IMPORT_HATCH,    "INPUT",  "1x");
        addTieredHatches(entries, GTMachines.FLUID_IMPORT_HATCH_4X, "INPUT",  "4x");
        addTieredHatches(entries, GTMachines.FLUID_IMPORT_HATCH_9X, "INPUT",  "9x");
        addTieredHatches(entries, GTMachines.FLUID_EXPORT_HATCH,    "OUTPUT", "1x");
        addTieredHatches(entries, GTMachines.FLUID_EXPORT_HATCH_4X, "OUTPUT", "4x");
        addTieredHatches(entries, GTMachines.FLUID_EXPORT_HATCH_9X, "OUTPUT", "9x");
        return entries;
    }

    public static List<ComponentEntry> readBuses() {
        List<ComponentEntry> entries = new ArrayList<>();
        addTieredHatches(entries, GTMachines.ITEM_IMPORT_BUS, "INPUT",  null);
        addTieredHatches(entries, GTMachines.ITEM_EXPORT_BUS, "OUTPUT", null);
        return entries;
    }

    public static List<ComponentEntry> readEnergyHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        addTieredEnergy(entries, GTMachines.ENERGY_INPUT_HATCH,     "INPUT",  "2A");
        addTieredEnergy(entries, GTMachines.ENERGY_INPUT_HATCH_4A,  "INPUT",  "4A");
        addTieredEnergy(entries, GTMachines.ENERGY_INPUT_HATCH_16A, "INPUT",  "16A");
        addTieredEnergy(entries, GTMachines.ENERGY_OUTPUT_HATCH,    "OUTPUT", "2A");
        addTieredEnergy(entries, GTMachines.ENERGY_OUTPUT_HATCH_4A, "OUTPUT", "4A");
        addTieredEnergy(entries, GTMachines.ENERGY_OUTPUT_HATCH_16A,"OUTPUT", "16A");
        return entries;
    }

    public static List<ComponentEntry> readSubstationHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        addTieredEnergy(entries, GTMachines.SUBSTATION_ENERGY_INPUT_HATCH,  "INPUT",  "64A");
        addTieredEnergy(entries, GTMachines.SUBSTATION_ENERGY_OUTPUT_HATCH, "OUTPUT", "64A");
        return entries;
    }

    public static List<ComponentEntry> readLaserHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        addTieredEnergy(entries, GTMachines.LASER_INPUT_HATCH_256,   "INPUT",  "256A");
        addTieredEnergy(entries, GTMachines.LASER_INPUT_HATCH_1024,  "INPUT",  "1024A");
        addTieredEnergy(entries, GTMachines.LASER_INPUT_HATCH_4096,  "INPUT",  "4096A");
        addTieredEnergy(entries, GTMachines.LASER_OUTPUT_HATCH_256,  "OUTPUT", "256A");
        addTieredEnergy(entries, GTMachines.LASER_OUTPUT_HATCH_1024, "OUTPUT", "1024A");
        addTieredEnergy(entries, GTMachines.LASER_OUTPUT_HATCH_4096, "OUTPUT", "4096A");
        return entries;
    }

    public static List<ComponentEntry> readMufflerHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        addTieredHatches(entries, GTMachines.MUFFLER_HATCH, null, null);
        return entries;
    }

    public static List<ComponentEntry> readMaintenanceHatches() {
        return List.of(
                fromSingle(GTMachines.MAINTENANCE_HATCH,              Map.of("maintenanceType", "STANDARD")),
                fromSingle(GTMachines.AUTO_MAINTENANCE_HATCH,         Map.of("maintenanceType", "AUTO")),
                fromSingle(GTMachines.CONFIGURABLE_MAINTENANCE_HATCH, Map.of("maintenanceType", "CONFIGURABLE")),
                fromSingle(GTMachines.CLEANING_MAINTENANCE_HATCH,     Map.of("maintenanceType", "CLEANING"))
        );
    }

    public static List<ComponentEntry> readDualHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        addTieredHatches(entries, GTMachines.DUAL_IMPORT_HATCH, "INPUT",  null);
        addTieredHatches(entries, GTMachines.DUAL_EXPORT_HATCH, "OUTPUT", null);
        return entries;
    }

    public static List<ComponentEntry> readParallelHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        for (MachineDefinition def : GCYMMachines.PARALLEL_HATCH) {
            if (def == null) continue;
            String blockId = def.getId().toString();
            int tier = def.getTier();
            String tierName = GTValues.VN[tier];
            entries.add(ComponentEntry.builder(blockId, def.getLangValue(), tierName, tier)
                    .attr("variant", "STANDARD")
                    .build());
        }
        return entries;
    }

    // helpers
    private static void addTieredHatches(List<ComponentEntry> out, MachineDefinition[] array,
                                         String hatchType, String capacity) {
        if (array == null) return;
        for (MachineDefinition def : array) {
            if (def == null) continue;
            String blockId  = def.getId().toString();
            int tier        = def.getTier();
            String tierName = GTValues.VN[tier];
            var builder = ComponentEntry.builder(blockId, def.getLangValue(), tierName, tier);
            if (hatchType != null) builder.attr("hatchType", hatchType);
            if (capacity  != null) builder.attr("capacity",  capacity);
            out.add(builder.build());
        }
    }

    private static void addTieredEnergy(List<ComponentEntry> out, MachineDefinition[] array,
                                        String hatchType, String amperage) {
        if (array == null) return;
        for (MachineDefinition def : array) {
            if (def == null) continue;
            String blockId  = def.getId().toString();
            int tier        = def.getTier();
            String tierName = GTValues.VN[tier];
            out.add(ComponentEntry.builder(blockId, def.getLangValue(), tierName, tier)
                    .attr("hatchType", hatchType)
                    .attr("amperage",  amperage)
                    .build());
        }
    }

    private static ComponentEntry fromSingle(MachineDefinition def, Map<String, String> attrs) {
        String blockId  = def.getId().toString();
        int tier        = def.getTier();
        String tierName = GTValues.VN[tier];
        var builder = ComponentEntry.builder(blockId, def.getLangValue(), tierName, tier);
        attrs.forEach(builder::attr);
        return builder.build();
    }
}