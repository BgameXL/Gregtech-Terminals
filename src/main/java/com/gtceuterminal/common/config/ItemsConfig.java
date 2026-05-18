package com.gtceuterminal.common.config;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemsConfig {

    private static final YamlConfigLoader LOADER = new YamlConfigLoader("", "gtceuterminals.yaml");

    private static int     ea_maxLinkedMachines    = 16;
    private static int     ea_refreshIntervalTicks = 20;
    private static boolean ea_showPerHatchDetails  = true;
    private static int     ea_historySeconds       = 30;

    private static int     mgr_maxDetectedMultiblocks = 32;
    private static boolean mgr_allowMENetworkUpgrade  = true;
    private static int     mgr_highlightDurationSec   = 10;
    private static int     mgr_highlightColor         = 0x00AAFF;

    private static boolean sch_allowAE2ConfigCopy    = true;
    private static boolean sch_plannerBuildAllEnabled = false;

    private static Set<Block> dis_blockBlacklist = new HashSet<>();

    public static void load() {
        LOADER.writeDefaults(List.of(

                // Energy Analyzer
                YamlConfigLoader.ConfigEntry.section("Energy Analyzer"),
                YamlConfigLoader.ConfigEntry.of("energy_analyzer.max_linked_machines", 16,
                        " Maximum machines linkable per item (1-64)"),
                YamlConfigLoader.ConfigEntry.of("energy_analyzer.refresh_interval_ticks", 20,
                        " How often energy data is refreshed in ticks (20 = 1s, min 5)"),
                YamlConfigLoader.ConfigEntry.of("energy_analyzer.show_per_hatch_details", true,
                        " Show per-hatch energy breakdown in the UI"),
                YamlConfigLoader.ConfigEntry.of("energy_analyzer.history_seconds", 30,
                        " Seconds of energy history to graph (5-120)"),

                // Multi Structure Manager
                YamlConfigLoader.ConfigEntry.section("Multi Structure Manager"),
                YamlConfigLoader.ConfigEntry.of("manager.max_detected_multiblocks", 32,
                        " Maximum number of multiblocks detected and shown in the UI (1-128)"),
                YamlConfigLoader.ConfigEntry.of("manager.allow_me_network_upgrade", true,
                        " Allow upgrading components using items from a linked ME network (requires AE2)"),
                YamlConfigLoader.ConfigEntry.of("manager.highlight_duration_seconds", 10,
                        " How long the multiblock highlight lasts in the world after clicking (seconds, 1-60)"),
                YamlConfigLoader.ConfigEntry.of("manager.highlight_color", "00AAFF",
                        " Highlight color as hex RGB, e.g. 00AAFF = blue, FFAA00 = orange, 00FF88 = green"),

                // Schematic Interface
                YamlConfigLoader.ConfigEntry.section("Schematic Interface"),
                YamlConfigLoader.ConfigEntry.of("schematic.allow_ae2_config_copy", true,
                        " Preserve AE2/ME bus slot configurations when copying and pasting multiblocks (requires AE2)"),
                YamlConfigLoader.ConfigEntry.of("schematic.planner_build_all_enabled", false,
                        " Allow the Placement Planner 'Build All' button to place real blocks in the world. Disabled by default — the planner is a planning tool, not a builder."),

                YamlConfigLoader.ConfigEntry.section("Dismantler"),
                YamlConfigLoader.ConfigEntry.of("dismantler.block_blacklist", List.of(),
                        " Registry IDs of blocks the Dismantler will NEVER refund or remove.\n" +
                                " Use this to protect blocks that cannot normally be obtained (e.g. bedrock, barriers, etc.)\n" +
                                "- minecraft:bedrock, - minecraft; barriers, -kubejs:custom_block. \n")
        ));

        ea_maxLinkedMachines    = clamp(LOADER.getInt("energy_analyzer.max_linked_machines", 16), 1, 64);
        ea_refreshIntervalTicks = clamp(LOADER.getInt("energy_analyzer.refresh_interval_ticks", 20), 5, 200);
        ea_showPerHatchDetails  = LOADER.getBoolean("energy_analyzer.show_per_hatch_details", true);
        ea_historySeconds       = clamp(LOADER.getInt("energy_analyzer.history_seconds", 30), 5, 120);

        mgr_maxDetectedMultiblocks = clamp(LOADER.getInt("manager.max_detected_multiblocks", 32), 1, 128);
        mgr_allowMENetworkUpgrade  = LOADER.getBoolean("manager.allow_me_network_upgrade", true);
        mgr_highlightDurationSec   = clamp(LOADER.getInt("manager.highlight_duration_seconds", 10), 1, 60);
        mgr_highlightColor         = parseHexColor(LOADER.getString("manager.highlight_color", "00AAFF"), 0x00AAFF);

        sch_allowAE2ConfigCopy     = LOADER.getBoolean("schematic.allow_ae2_config_copy", true);
        sch_plannerBuildAllEnabled = LOADER.getBoolean("schematic.planner_build_all_enabled", false);

        dis_blockBlacklist = new HashSet<>();
        List<String> rawBlacklist = LOADER.getStringList("dismantler.block_blacklist");
        for (String entry : rawBlacklist) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            ResourceLocation rl = ResourceLocation.tryParse(trimmed);
            if (rl == null) {
                GTCEUTerminalMod.LOGGER.warn("[Dismantler] Invalid block ID in blacklist: '{}' — skipping", trimmed);
                continue;
            }
            Block block = ForgeRegistries.BLOCKS.getValue(rl);
            if (block == null) {
                GTCEUTerminalMod.LOGGER.warn("[Dismantler] Unknown block '{}' in blacklist — skipping (not registered?)", trimmed);
                continue;
            }
            dis_blockBlacklist.add(block);
        }
        if (!dis_blockBlacklist.isEmpty()) {
            GTCEUTerminalMod.LOGGER.info("[Dismantler] Block blacklist loaded: {} entries", dis_blockBlacklist.size());
        }

        GTCEUTerminalMod.LOGGER.info(
                "ItemsConfig loaded — EnergyAnalyzer: maxMachines={}, refresh={}t, history={}s | Manager: maxMultiblocks={}, allowME={} | Schematic: ae2Copy={}",
                ea_maxLinkedMachines, ea_refreshIntervalTicks, ea_historySeconds,
                mgr_maxDetectedMultiblocks, mgr_allowMENetworkUpgrade,
                sch_allowAE2ConfigCopy
        );
    }

    // Energy Analyzer Getters
    public static int     getEAMaxLinkedMachines()    { return ea_maxLinkedMachines; }
    public static int     getEARefreshIntervalTicks() { return ea_refreshIntervalTicks; }
    public static boolean getEAShowPerHatchDetails()  { return ea_showPerHatchDetails; }
    public static int     getEAHistorySeconds()       { return ea_historySeconds; }
    public static boolean isEADimensionAllowed(String dimensionId) { return true; }

    // Multi Structure Manager Getters
    public static int     getMgrMaxDetectedMultiblocks()  { return mgr_maxDetectedMultiblocks; }
    public static boolean isMgrAllowMENetworkUpgrade()    { return mgr_allowMENetworkUpgrade; }
    public static int     getMgrHighlightDurationMs()     { return mgr_highlightDurationSec * 1000; }
    public static int     getMgrHighlightColor()          { return mgr_highlightColor; }

    // Schematic Interface Getters
    public static boolean isSchAllowAE2ConfigCopy()     { return sch_allowAE2ConfigCopy; }
    public static boolean isSchPlannerBuildAllEnabled() { return sch_plannerBuildAllEnabled; }

    // Dismantler Getters
    public static boolean isDismantlerBlacklisted(Block block) {
        return dis_blockBlacklist.contains(block);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int parseHexColor(String hex, int fallback) {
        try {
            String clean = hex.trim().replace("#", "").replace("0x", "").replace("0X", "");
            return (int) Long.parseLong(clean, 16) & 0xFFFFFF;
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("Invalid highlight_color '{}', using default", hex);
            return fallback;
        }
    }
}