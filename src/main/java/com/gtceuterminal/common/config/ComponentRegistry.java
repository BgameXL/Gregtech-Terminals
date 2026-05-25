package com.gtceuterminal.common.config;

import com.gtceuterminal.common.compat.GTCEuReader;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class ComponentRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentRegistry.class);

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

    private static final Map<String, List<ComponentEntry>> DATA = new LinkedHashMap<>();
    private static final Map<String, List<ComponentEntry>> EXTERNAL = new LinkedHashMap<>();
    private static boolean initialized = false;

    private ComponentRegistry() {}

    public static synchronized void init() {
        if (initialized) return;
        LOGGER.info("Initializing ComponentRegistry...");

        put(COILS,               GTCEuReader.readCoils());
        put(FLUID_HATCHES,       GTCEuReader.readFluidHatches());
        put(BUSES,               GTCEuReader.readBuses());
        put(ENERGY_HATCHES,      GTCEuReader.readEnergyHatches());
        put(LASER_HATCHES,       GTCEuReader.readLaserHatches());
        put(WIRELESS_HATCHES,    new ArrayList<>());
        put(SUBSTATION_HATCHES,  GTCEuReader.readSubstationHatches());
        put(PARALLEL_HATCHES,    GTCEuReader.readParallelHatches());
        put(MUFFLER_HATCHES,     GTCEuReader.readMufflerHatches());
        put(MAINTENANCE_HATCHES, GTCEuReader.readMaintenanceHatches());
        put(DUAL_HATCHES,        GTCEuReader.readDualHatches());

        initialized = true;
        EXTERNAL.forEach((category, entries) -> DATA.computeIfAbsent(category, k -> new ArrayList<>()).addAll(entries));
        DATA.forEach((k, v) -> LOGGER.info("  {} → {} entries", k, v.size()));
    }

    private static void put(String category, List<ComponentEntry> entries) {
        DATA.put(category, new ArrayList<>(entries));
    }

    public static synchronized void addExternal(String category, ComponentEntry entry) {
        LOGGER.info("addExternal called: category={} blockId={} initialized={}", category, entry.blockId, initialized);
        EXTERNAL.computeIfAbsent(category, k -> new ArrayList<>()).add(entry);
        if (initialized) DATA.computeIfAbsent(category, k -> new ArrayList<>()).add(entry);
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

    public static List<ComponentEntry> getCoils() { return get(COILS); }

    public static ComponentEntry coilByTier(int tier) {
        return first(COILS, e -> e.tier == tier);
    }

    public static ComponentEntry coilByBlockId(String blockId) {
        return first(COILS, e -> e.blockId.equalsIgnoreCase(blockId));
    }

    public static int coilTier(BlockState state) {
        if (state == null) return -1;
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        ComponentEntry e = coilByBlockId(blockId);
        return e != null ? e.tier : -1;
    }

    public static List<ComponentEntry> getFluidHatches(String hatchType) {
        return get(FLUID_HATCHES, e -> e.attrIs("hatchType", hatchType));
    }

    public static List<ComponentEntry> getBuses(String busType) {
        return get(BUSES, e -> e.attrIs("hatchType", busType) || e.attrIs("busType", busType));
    }

    public static List<ComponentEntry> getEnergyHatches() { return get(ENERGY_HATCHES); }

    public static ComponentEntry energyHatchForTier(String hatchType, int tier, String amperage) {
        ensureInit();
        if (amperage != null && !amperage.isEmpty()) {
            ComponentEntry exact = first(ENERGY_HATCHES,
                    e -> e.tier == tier && e.attrIs("hatchType", hatchType) && amperage.equals(e.attr("amperage")));
            if (exact != null) return exact;
        }
        return first(ENERGY_HATCHES, e -> e.tier == tier && e.attrIs("hatchType", hatchType));
    }

    public static List<ComponentEntry> getLaserHatches() { return get(LASER_HATCHES); }

    public static ComponentEntry laserHatchForTier(String hatchType, int tier, String amperage) {
        ensureInit();
        if (amperage != null && !amperage.isEmpty()) {
            ComponentEntry exact = first(LASER_HATCHES,
                    e -> e.tier == tier && e.attrIs("hatchType", hatchType) && amperage.equals(e.attr("amperage")));
            if (exact != null) return exact;
        }
        return first(LASER_HATCHES, e -> e.tier == tier && e.attrIs("hatchType", hatchType));
    }

    public static List<ComponentEntry> getWirelessHatches() { return get(WIRELESS_HATCHES); }

    public static ComponentEntry substationHatchForTier(String hatchType, int tier) {
        return first(SUBSTATION_HATCHES, e -> e.tier == tier && e.attrIs("hatchType", hatchType));
    }

    public static List<ComponentEntry> getParallelHatches() { return get(PARALLEL_HATCHES); }

    public static ComponentEntry mufflerHatchForTier(int tier) {
        return first(MUFFLER_HATCHES, e -> e.tier == tier);
    }

    public static List<ComponentEntry> getMaintenanceHatches() { return get(MAINTENANCE_HATCHES); }

    public static List<ComponentEntry> getDualHatches(String hatchType) {
        return get(DUAL_HATCHES, e -> e.attrIs("hatchType", hatchType));
    }

    // UI helpers
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

    public static List<ItemStack> getMatchingItemStacks(ComponentCategory category) {
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

        List<ItemStack> stacks = new ArrayList<>();
        for (ComponentEntry e : get(registryCategory)) {
            ResourceLocation rl = ResourceLocation.tryParse(e.blockId);
            if (rl == null) continue;
            Block block = ForgeRegistries.BLOCKS.getValue(rl);
            if (block != null) stacks.add(new ItemStack(block));
        }
        return stacks;
    }

    private static void ensureInit() {
        if (!initialized) init();
    }
}