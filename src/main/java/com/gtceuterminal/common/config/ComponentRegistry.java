package com.gtceuterminal.common.config;

import com.gregtechceu.gtceu.api.GTValues;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class ComponentRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentRegistry.class);

    private static final Map<String, List<ComponentEntry>> DATA = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static final String FLUID_HATCHES       = "fluid_hatches";
    public static final String BUSES               = "buses";
    public static final String COILS               = "coils";
    public static final String ENERGY_HATCHES      = "energy_hatches";
    public static final String LASER_HATCHES       = "laser_hatches";
    public static final String WIRELESS_HATCHES    = "wireless_hatches";
    public static final String SUBSTATION_HATCHES  = "substation_hatches";
    public static final String PARALLEL_HATCHES    = "parallel_hatches";
    public static final String MUFFLER_HATCHES     = "muffler_hatches";
    public static final String MAINTENANCE_HATCHES = "maintenance_hatches";
    public static final String DUAL_HATCHES        = "dual_hatches";

    private ComponentRegistry() {}

    public static synchronized void init() {
        if (initialized) return;
        LOGGER.info("Initializing ComponentRegistry...");

        register(FLUID_HATCHES,       "fluid_hatches.json",       "GTCEu Terminal - Fluid Hatch Configuration",       ComponentRegistry::defaultFluidHatches);
        register(BUSES,               "buses.json",               "GTCEu Terminal - Bus Configuration (Item I/O)",    ComponentRegistry::defaultBuses);
        register(COILS,               "coils.json",               "GTCEu Terminal - Heating Coil Configuration",      ComponentRegistry::defaultCoils);
        register(ENERGY_HATCHES,      "energy_hatches.json",      "GTCEu Terminal - Energy Hatch Configuration",      ComponentRegistry::defaultEnergyHatches);
        register(LASER_HATCHES,       "laser_hatches.json",       "GTCEu Terminal - Laser Hatch Configuration",       ComponentRegistry::defaultLaserHatches);
        register(WIRELESS_HATCHES,    "wireless_hatches.json",    "GTCEu Terminal - Wireless Hatch Configuration",    ComponentRegistry::defaultWirelessHatches);
        register(SUBSTATION_HATCHES,  "substation_hatches.json",  "GTCEu Terminal - Substation Hatch Configuration",  ComponentRegistry::defaultSubstationHatches);
        register(PARALLEL_HATCHES,    "parallel_hatches.json",    "GTCEu Terminal - Parallel Hatch Configuration",    ComponentRegistry::defaultParallelHatches);
        register(MUFFLER_HATCHES,     "muffler_hatches.json",     "GTCEu Terminal - Muffler Hatch Configuration",     ComponentRegistry::defaultMufflerHatches);
        register(MAINTENANCE_HATCHES, "maintenance_hatches.json", "GTCEu Terminal - Maintenance Hatch Configuration", ComponentRegistry::defaultMaintenanceHatches);
        register(DUAL_HATCHES,        "dual_hatches.json",        "GTCEu Terminal - Dual Hatch Configuration",        ComponentRegistry::defaultDualHatches);

        initialized = true;
        DATA.forEach((k, v) -> LOGGER.info("  {} → {} entries", k, v.size()));
    }

    public static synchronized void reload() {
        initialized = false;
        DATA.clear();
        init();
    }

    private static void register(String category, String file, String desc,
                                 java.util.function.Supplier<List<ComponentEntry>> defaults) {
        DATA.put(category, ComponentConfigLoader.load(file, desc, defaults));
    }

    public static List<ComponentEntry> get(String category) {
        ensureInit();
        return Collections.unmodifiableList(DATA.getOrDefault(category, List.of()));
    }

    public static List<ComponentEntry> get(String category, Predicate<ComponentEntry> filter) {
        List<ComponentEntry> result = new ArrayList<>();
        for (ComponentEntry e : get(category)) if (filter.test(e)) result.add(e);
        return result;
    }

    public static ComponentEntry first(String category, Predicate<ComponentEntry> filter) {
        for (ComponentEntry e : get(category)) if (filter.test(e)) return e;
        return null;
    }

    private static void ensureInit() {
        if (!initialized) init();
    }

    public static List<ComponentEntry> getFluidHatches(String hatchType) {
        return get(FLUID_HATCHES, e -> e.attrIs("hatchType", hatchType));
    }

    public static List<ComponentEntry> getBuses(String busType) {
        return get(BUSES, e -> e.attrIs("hatchType", busType) || e.attrIs("busType", busType));
    }

    public static List<ComponentEntry> getCoils() {
        return get(COILS);
    }

    public static ComponentEntry coilByTier(int tier) {
        return first(COILS, e -> e.tier == tier);
    }

    public static ComponentEntry coilByBlockId(String blockId) {
        return first(COILS, e -> e.blockId.equalsIgnoreCase(blockId));
    }

    public static int coilTier(net.minecraft.world.level.block.state.BlockState state) {
        if (state == null) return -1;
        String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(state.getBlock()).toString();
        ComponentEntry e = coilByBlockId(blockId);
        return e != null ? e.tier : -1;
    }

    public static List<ComponentEntry> getEnergyHatches() {
        return get(ENERGY_HATCHES);
    }

    public static ComponentEntry energyHatchForTier(String hatchType, int tier, String amperage) {
        ensureInit();
        if (amperage != null && !amperage.isEmpty()) {
            ComponentEntry exact = first(ENERGY_HATCHES,
                    e -> e.tier == tier && e.attrIs("hatchType", hatchType) && amperage.equals(e.attr("amperage")));
            if (exact != null) return exact;
        }
        return first(ENERGY_HATCHES, e -> e.tier == tier && e.attrIs("hatchType", hatchType));
    }

    public static List<ComponentEntry> getLaserHatches() {
        return get(LASER_HATCHES);
    }

    public static ComponentEntry laserHatchForTier(String hatchType, int tier, String amperage) {
        ensureInit();
        if (amperage != null && !amperage.isEmpty()) {
            ComponentEntry exact = first(LASER_HATCHES,
                    e -> e.tier == tier && e.attrIs("hatchType", hatchType) && amperage.equals(e.attr("amperage")));
            if (exact != null) return exact;
        }
        return first(LASER_HATCHES, e -> e.tier == tier && e.attrIs("hatchType", hatchType));
    }

    public static List<ComponentEntry> getWirelessHatches() {
        return get(WIRELESS_HATCHES);
    }

    public static ComponentEntry substationHatchForTier(String hatchType, int tier) {
        return first(SUBSTATION_HATCHES, e -> e.tier == tier && e.attrIs("hatchType", hatchType));
    }

    public static List<ComponentEntry> getParallelHatches() {
        return get(PARALLEL_HATCHES);
    }

    public static ComponentEntry mufflerHatchForTier(int tier) {
        return first(MUFFLER_HATCHES, e -> e.tier == tier);
    }

    public static List<ComponentEntry> getMaintenanceHatches() {
        return get(MAINTENANCE_HATCHES);
    }

    public static List<ComponentEntry> getDualHatches(String hatchType) {
        return get(DUAL_HATCHES, e -> e.attrIs("hatchType", hatchType));
    }

    private static List<ComponentEntry> defaultFluidHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        String[][] patterns = {
                {"gtceu:*_input_hatch",     "INPUT_HATCH",         "INPUT",  "1x"},
                {"gtceu:*_input_hatch_1x",  "INPUT_HATCH_1X",      "INPUT",  "1x"},
                {"gtceu:*_input_hatch_4x",  "QUAD_INPUT_HATCH",    "INPUT",  "4x"},
                {"gtceu:*_input_hatch_9x",  "NONUPLE_INPUT_HATCH", "INPUT",  "9x"},
                {"gtceu:*_output_hatch",    "OUTPUT_HATCH",        "OUTPUT", "1x"},
                {"gtceu:*_output_hatch_1x", "OUTPUT_HATCH_1X",     "OUTPUT", "1x"},
                {"gtceu:*_output_hatch_4x", "QUAD_OUTPUT_HATCH",   "OUTPUT", "4x"},
                {"gtceu:*_output_hatch_9x", "NONUPLE_OUTPUT_HATCH","OUTPUT", "9x"},
                {"gtceu:*_pump_fluid_hatch","PUMP_FLUID_HATCH",    "INPUT",  "1x"},
                {"gtceu:*_tank_valve",      "TANK_VALVE",          "INPUT",  "1x"},
        };
        for (String[] p : patterns) entries.addAll(expandTiered(p[0], p[1], Map.of("hatchType", p[2], "capacity", p[3])));
        return entries;
    }

    private static List<ComponentEntry> defaultBuses() {
        List<ComponentEntry> entries = new ArrayList<>();
        String[][] patterns = {
                {"gtceu:*_input_bus",  "INPUT_BUS",  "INPUT"},
                {"gtceu:*_output_bus", "OUTPUT_BUS", "OUTPUT"},
        };
        for (String[] p : patterns) entries.addAll(expandTiered(p[0], p[1], Map.of("hatchType", p[2])));
        return entries;
    }

    private static List<ComponentEntry> defaultCoils() {
        record Coil(String blockId, String material, String heat) {}
        List<Coil> coils = List.of(
                new Coil("gtceu:cupronickel_coil_block", "Cupronickel", "1800K"),
                new Coil("gtceu:kanthal_coil_block",     "Kanthal",     "2700K"),
                new Coil("gtceu:nichrome_coil_block",    "Nichrome",    "3600K"),
                new Coil("gtceu:rtm_alloy_coil_block",   "RTM Alloy",   "4500K"),
                new Coil("gtceu:hssg_coil_block",        "HSS-G",       "5400K"),
                new Coil("gtceu:naquadah_coil_block",    "Naquadah",    "7200K"),
                new Coil("gtceu:trinium_coil_block",     "Trinium",     "9000K"),
                new Coil("gtceu:tritanium_coil_block",   "Tritanium",   "10800K")
        );
        List<ComponentEntry> entries = new ArrayList<>();
        for (int i = 0; i < coils.size(); i++) {
            Coil c = coils.get(i);
            entries.add(ComponentEntry.builder(c.blockId(), c.material() + " Coil", "T" + i, i)
                    .attr("coilType", c.material())
                    .attr("heatCapacity", c.heat())
                    .build());
        }
        return entries;
    }

    private static List<ComponentEntry> defaultEnergyHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        String[][] patterns = {
                {"gtceu:*_energy_input_hatch",       "ENERGY_HATCH",    "INPUT",  "2A"},
                {"gtceu:*_energy_input_hatch_4a",    "ENERGY_HATCH",    "INPUT",  "4A"},
                {"gtceu:*_energy_input_hatch_16a",   "ENERGY_HATCH",    "INPUT",  "16A"},
                {"gtceu:*_energy_input_hatch_64a",   "ENERGY_HATCH",    "INPUT",  "64A"},
                {"gtceu:*_energy_input_hatch_256a",  "ENERGY_HATCH",    "INPUT",  "256A"},
                {"gtceu:*_energy_output_hatch",      "DYNAMO_HATCH",    "OUTPUT", "2A"},
                {"gtceu:*_energy_output_hatch_4a",   "DYNAMO_HATCH",    "OUTPUT", "4A"},
                {"gtceu:*_energy_output_hatch_16a",  "DYNAMO_HATCH",    "OUTPUT", "16A"},
                {"gtceu:*_energy_output_hatch_64a",  "DYNAMO_HATCH",    "OUTPUT", "64A"},
        };
        for (String[] p : patterns) entries.addAll(expandTiered(p[0], p[1], Map.of("hatchType", p[2], "amperage", p[3])));
        return entries;
    }

    private static List<ComponentEntry> defaultLaserHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        String[][] patterns = {
                {"gtceu:*_laser_target_hatch",         "LASER_INPUT",  "INPUT",  null,     "5"},
                {"gtceu:*_laser_source_hatch",         "LASER_OUTPUT", "OUTPUT", null,     "5"},
                {"gtceu:*_256a_laser_target_hatch",    "LASER_INPUT",  "INPUT",  "256A",   "5"},
                {"gtceu:*_256a_laser_source_hatch",    "LASER_OUTPUT", "OUTPUT", "256A",   "5"},
                {"gtceu:*_1024a_laser_target_hatch",   "LASER_INPUT",  "INPUT",  "1024A",  "5"},
                {"gtceu:*_1024a_laser_source_hatch",   "LASER_OUTPUT", "OUTPUT", "1024A",  "5"},
                {"gtceu:*_4096a_laser_target_hatch",   "LASER_INPUT",  "INPUT",  "4096A",  "5"},
                {"gtceu:*_4096a_laser_source_hatch",   "LASER_OUTPUT", "OUTPUT", "4096A",  "5"},
                {"gtceu:*_16384a_laser_target_hatch",  "LASER_INPUT",  "INPUT",  "16384A", "5"},
                {"gtceu:*_16384a_laser_source_hatch",  "LASER_OUTPUT", "OUTPUT", "16384A", "5"},
                {"gtmthings:*_wireless_laser_target_hatch", "LASER_INPUT",  "INPUT",  null, "0"},
                {"gtmthings:*_wireless_laser_source_hatch", "LASER_OUTPUT", "OUTPUT", null, "0"},
        };
        for (String[] p : patterns) {
            Map<String, String> attrs = new HashMap<>();
            attrs.put("hatchType", p[2]);
            if (p[3] != null) attrs.put("amperage", p[3]);
            entries.addAll(expandTiered(p[0], p[1], attrs, Integer.parseInt(p[4]), GTValues.VN.length - 1));
        }
        return entries;
    }

    private static List<ComponentEntry> defaultWirelessHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        entries.addAll(expandTiered("gtmthings:*_2a_wireless_energy_input_hatch",   "WIRELESS_ENERGY_INPUT",  Map.of("hatchType", "ENERGY_INPUT",  "amperage", "2A")));
        entries.addAll(expandTiered("gtmthings:*_2a_wireless_energy_output_hatch",  "WIRELESS_ENERGY_OUTPUT", Map.of("hatchType", "ENERGY_OUTPUT", "amperage", "2A")));
        entries.addAll(expandTiered("gtmthings:*_256a_wireless_laser_target_hatch", "WIRELESS_LASER_INPUT",   Map.of("hatchType", "LASER_INPUT",   "amperage", "256A"), 5, GTValues.VN.length - 1));
        entries.addAll(expandTiered("gtmthings:*_256a_wireless_laser_source_hatch", "WIRELESS_LASER_OUTPUT",  Map.of("hatchType", "LASER_OUTPUT",  "amperage", "256A"), 5, GTValues.VN.length - 1));
        return entries;
    }

    private static List<ComponentEntry> defaultSubstationHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        entries.addAll(expandTiered("gtceu:*_substation_input_hatch_64a",  "SUBSTATION_INPUT",  Map.of("hatchType", "INPUT",  "amperage", "64A")));
        entries.addAll(expandTiered("gtceu:*_substation_input_hatch_256a", "SUBSTATION_INPUT",  Map.of("hatchType", "INPUT",  "amperage", "256A")));
        entries.addAll(expandTiered("gtceu:*_substation_output_hatch_64a", "SUBSTATION_OUTPUT", Map.of("hatchType", "OUTPUT", "amperage", "64A")));
        entries.addAll(expandTiered("gtceu:*_substation_output_hatch_256a","SUBSTATION_OUTPUT", Map.of("hatchType", "OUTPUT", "amperage", "256A")));
        return entries;
    }

    private static List<ComponentEntry> defaultParallelHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        entries.addAll(expandTiered("gtceu:*_parallel_hatch",     "PARALLEL_HATCH",    Map.of("variant", "STANDARD")));
        entries.addAll(expandTiered("gtceu:*_parallel_hatch_mk2", "PARALLEL_HATCH_MK", Map.of("variant", "MK")));
        return entries;
    }

    private static List<ComponentEntry> defaultMufflerHatches() {
        return expandTiered("gtceu:*_muffler_hatch", "MUFFLER", Map.of());
    }

    private static List<ComponentEntry> defaultMaintenanceHatches() {
        return List.of(
                ComponentEntry.builder("gtceu:maintenance_hatch",            "Maintenance Hatch",            "LV", 1).attr("maintenanceType", "STANDARD").build(),
                ComponentEntry.builder("gtceu:auto_maintenance_hatch",       "Auto-Maintenance Hatch",       "HV", 3).attr("maintenanceType", "AUTO").build(),
                ComponentEntry.builder("gtceu:configurable_maintenance_hatch","Configurable Maintenance Hatch","HV",3).attr("maintenanceType","CONFIGURABLE").build(),
                ComponentEntry.builder("gtceu:cleaning_maintenance_hatch",   "Cleaning Maintenance Hatch",   "HV", 3).attr("maintenanceType", "CLEANING").build()
        );
    }

    private static List<ComponentEntry> defaultDualHatches() {
        List<ComponentEntry> entries = new ArrayList<>();
        String[] tierNames   = {"lv","mv","hv","ev","iv","luv","zpm","uv"};
        String[] tierDisplay = {"LV","MV","HV","EV","IV","LuV","ZPM","UV"};
        for (int i = 0; i < tierNames.length; i++) {
            int tier = i + 1;
            entries.add(ComponentEntry.builder("gtceu:" + tierNames[i] + "_dual_input_hatch",
                            tierDisplay[i] + " Dual Input Hatch", tierDisplay[i], tier)
                    .attr("hatchType", "INPUT").attr("source", "gtceu").build());
            entries.add(ComponentEntry.builder("gtmthings:" + tierNames[i] + "_huge_dual_hatch",
                            tierDisplay[i] + " Huge Dual Hatch", tierDisplay[i], tier)
                    .attr("hatchType", "INPUT").attr("source", "gtmthings").build());
        }
        String[] hiNames   = {"uhv","uev","uiv","uxv","opv","max"};
        String[] hiDisplay = {"UHV","UEV","UIV","UXV","OpV","MAX"};
        for (int i = 0; i < hiNames.length; i++) {
            int tier = i + 9;
            entries.add(ComponentEntry.builder("gtmthings:" + hiNames[i] + "_huge_dual_hatch",
                            hiDisplay[i] + " Huge Dual Hatch", hiDisplay[i], tier)
                    .attr("hatchType", "INPUT").attr("source", "gtmthings").build());
        }
        return entries;
    }

    private static List<ComponentEntry> expandTiered(String pattern, String componentType, Map<String, String> attrs) {
        return expandTiered(pattern, componentType, attrs, 0, GTValues.VN.length - 1);
    }

    private static List<ComponentEntry> expandTiered(String pattern, String componentType,
                                                     Map<String, String> attrs, int minTier, int maxTier) {
        List<ComponentEntry> entries = new ArrayList<>();
        for (int tier = minTier; tier <= maxTier && tier < GTValues.VN.length; tier++) {
            String tierName  = GTValues.VN[tier];
            String blockId   = pattern.replace("*", tierName.toLowerCase());
            String displayName = ComponentConfigLoader.generateDisplayName(tierName, componentType, attrs);
            entries.add(new ComponentEntry(blockId, displayName, tierName, tier, new HashMap<>(attrs)));
        }
        return entries;
    }

    public enum ComponentCategory {
        ENERGY_HATCH("Energy Hatch"),
        FLUID_HATCH("Fluid Hatch"),
        ITEM_HATCH("Item Hatch/Bus"),
        MAINTENANCE_HATCH("Maintenance Hatch"),
        MUFFLER_HATCH("Muffler Hatch"),
        LASER_HATCH("Laser Hatch"),
        ROTOR_HOLDER("Rotor Holder"),
        COIL("Heating Coil"),
        CASING("Casing"),
        OTHER("Other");

        private final String displayName;
        ComponentCategory(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public static List<net.minecraft.world.item.ItemStack> getMatchingItemStacks(ComponentCategory category) {
        String registryCategory = switch (category) {
            case ENERGY_HATCH      -> ENERGY_HATCHES;
            case FLUID_HATCH       -> FLUID_HATCHES;
            case ITEM_HATCH        -> BUSES;
            case MAINTENANCE_HATCH -> MAINTENANCE_HATCHES;
            case MUFFLER_HATCH     -> MUFFLER_HATCHES;
            case LASER_HATCH       -> LASER_HATCHES;
            case COIL              -> COILS;
            default                -> null;
        };
        if (registryCategory == null) return List.of();

        List<net.minecraft.world.item.ItemStack> stacks = new ArrayList<>();
        for (ComponentEntry e : get(registryCategory)) {
            net.minecraft.resources.ResourceLocation rl =
                    net.minecraft.resources.ResourceLocation.tryParse(e.blockId);
            if (rl == null) continue;
            net.minecraft.world.level.block.Block block =
                    net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(rl);
            if (block != null) stacks.add(new net.minecraft.world.item.ItemStack(block));
        }
        return stacks;
    }
}