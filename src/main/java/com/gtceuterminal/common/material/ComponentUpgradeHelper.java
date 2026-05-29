package com.gtceuterminal.common.material;

import com.gregtechceu.gtceu.api.GTValues;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.config.*;
import com.gtceuterminal.common.multiblock.ComponentGroup;
import com.gtceuterminal.common.multiblock.ComponentGroupRegistry;
import com.gtceuterminal.common.multiblock.ComponentInfo;
import com.gtceuterminal.common.config.ComponentEntry;
import com.gtceuterminal.common.config.ComponentRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentUpgradeHelper {

    public static Map<Item, Integer> getUpgradeItemsForBlockId(String targetBlockId) {
        Map<Item, Integer> items = new HashMap<>();
        if (targetBlockId == null || targetBlockId.isBlank()) return items;

        try {
            ResourceLocation id = ResourceLocation.parse(targetBlockId);
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (block != net.minecraft.world.level.block.Blocks.AIR && block.asItem() != null) {
                items.put(block.asItem(), 1);
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Invalid block ID: {}", targetBlockId, e);
        }

        return items;
    }

    public static boolean isUpgradeIdAllowed(ComponentGroup group, String targetBlockId) {
        if (group == null) return false;
        if (targetBlockId == null || targetBlockId.isBlank()) return false;
        if (group == ComponentGroupRegistry.MAINTENANCE)
            return ComponentRegistry.getMaintenanceHatches().stream().anyMatch(e -> targetBlockId.equals(e.blockId));
        return true;
    }

    public static Map<Item, Integer> getUpgradeItems(ComponentInfo component, int targetTier) {
        Map<Item, Integer> items = new HashMap<>();

        Block targetBlock = getComponentBlock(component.getGroup(), targetTier, component);
        if (targetBlock != null && targetBlock.asItem() != null) {
            items.put(targetBlock.asItem(), 1);
        }

        return items;
    }

    private static Block getComponentBlock(ComponentGroup type, int tier) {
        return getComponentBlock(type, tier, null);
    }

    private static Block getComponentBlock(ComponentGroup type, int tier, ComponentInfo component) {
        String blockId = getBlockIdFromConfig(type, tier, component);
        if (blockId == null) return null;

        try {
            ResourceLocation id = ResourceLocation.parse(blockId);
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (block != net.minecraft.world.level.block.Blocks.AIR) {
                return block;
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Invalid block ID: {}", blockId, e);
        }

        return null;
    }

    private static String getBlockIdFromConfig(ComponentGroup type, int tier, ComponentInfo component) {
        ComponentGroupRegistry cgr = null;
        if (type == ComponentGroupRegistry.INPUT_HATCH || type == ComponentGroupRegistry.INPUT_HATCH_1X
                || type == ComponentGroupRegistry.QUAD_INPUT_HATCH || type == ComponentGroupRegistry.NONUPLE_INPUT_HATCH
                || type == ComponentGroupRegistry.OUTPUT_HATCH || type == ComponentGroupRegistry.OUTPUT_HATCH_1X
                || type == ComponentGroupRegistry.QUAD_OUTPUT_HATCH || type == ComponentGroupRegistry.NONUPLE_OUTPUT_HATCH
                || type == ComponentGroupRegistry.INPUT_BUS || type == ComponentGroupRegistry.OUTPUT_BUS
                || type == ComponentGroupRegistry.ENERGY_HATCH || type == ComponentGroupRegistry.DYNAMO_HATCH
                || type == ComponentGroupRegistry.SUBSTATION_INPUT || type == ComponentGroupRegistry.SUBSTATION_OUTPUT
                || type == ComponentGroupRegistry.INPUT_LASER || type == ComponentGroupRegistry.OUTPUT_LASER
                || type == ComponentGroupRegistry.PARALLEL_HATCH || type == ComponentGroupRegistry.MUFFLER
                || type == ComponentGroupRegistry.COIL || type == ComponentGroupRegistry.MAINTENANCE
                || type == ComponentGroupRegistry.DUAL_HATCH
                || type == ComponentGroupRegistry.WIRELESS_ENERGY_INPUT || type == ComponentGroupRegistry.WIRELESS_ENERGY_OUTPUT
                || type == ComponentGroupRegistry.WIRELESS_LASER_INPUT || type == ComponentGroupRegistry.WIRELESS_LASER_OUTPUT) {
            return getBlockIdFromConfigImpl(type, tier, component);
        }
        if (type.registryCategory != null) {
            for (var e : ComponentRegistry.get(type.registryCategory)) {
                if (e.tier == tier) return e.blockId;
            }
        }
        return null;
    }

    private static String getBlockIdFromConfigImpl(ComponentGroup type, int tier, ComponentInfo component) {
        return switch (type.id) {
            case "input_hatch", "input_hatch_1x", "quad_input_hatch", "nonuple_input_hatch" -> {
                String cap = switch (type.id) {
                    case "quad_input_hatch"    -> "4x";
                    case "nonuple_input_hatch" -> "9x";
                    default                    -> "1x";
                };
                for (ComponentEntry hatch : ComponentRegistry.getFluidHatches("INPUT")) {
                    if (hatch.tier == tier && hatch.attrIs("capacity", cap)) yield hatch.blockId;
                }
                yield null;
            }

            case "output_hatch", "output_hatch_1x", "quad_output_hatch", "nonuple_output_hatch" -> {
                String cap = switch (type.id) {
                    case "quad_output_hatch"    -> "4x";
                    case "nonuple_output_hatch" -> "9x";
                    default                     -> "1x";
                };
                for (ComponentEntry hatch : ComponentRegistry.getFluidHatches("OUTPUT")) {
                    if (hatch.tier == tier && hatch.attrIs("capacity", cap)) yield hatch.blockId;
                }
                yield null;
            }

            case "input_bus" -> {
                for (ComponentEntry bus : ComponentRegistry.getBuses("INPUT")) {
                    if (bus.tier == tier) yield bus.blockId;
                }
                yield null;
            }

            case "output_bus" -> {
                for (ComponentEntry bus : ComponentRegistry.getBuses("OUTPUT")) {
                    if (bus.tier == tier) yield bus.blockId;
                }
                yield null;
            }

            case "energy_hatch" -> {
                String amperage = component != null ? component.getAmperage() : null;
                ComponentEntry energy = ComponentRegistry.energyHatchForTier("INPUT", tier, amperage);
                yield energy != null ? energy.blockId : null;
            }

            case "dynamo_hatch" -> {
                String amperage = component != null ? component.getAmperage() : null;
                ComponentEntry dynamo = ComponentRegistry.energyHatchForTier("OUTPUT", tier, amperage);
                yield dynamo != null ? dynamo.blockId : null;
            }

            case "substation_input" -> {
                ComponentEntry substation = ComponentRegistry.substationHatchForTier("INPUT", tier);
                yield substation != null ? substation.blockId : null;
            }

            case "substation_output" -> {
                ComponentEntry substation = ComponentRegistry.substationHatchForTier("OUTPUT", tier);
                yield substation != null ? substation.blockId : null;
            }

            case "input_laser" -> {
                String amperage = component != null ? component.getAmperage() : null;
                ComponentEntry laser = ComponentRegistry.laserHatchForTier("INPUT", tier, amperage);
                yield laser != null ? laser.blockId : null;
            }

            case "output_laser" -> {
                String amperage = component != null ? component.getAmperage() : null;
                ComponentEntry laser = ComponentRegistry.laserHatchForTier("OUTPUT", tier, amperage);
                yield laser != null ? laser.blockId : null;
            }

            case "parallel_hatch" -> {
                for (ComponentEntry parallel : ComponentRegistry.getParallelHatches()) {
                    if (parallel.tier == tier && parallel.attrIs("variant", "STANDARD")) {
                        yield parallel.blockId;
                    }
                }
                yield null;
            }

            case "muffler" -> {
                ComponentEntry muffler = ComponentRegistry.mufflerHatchForTier(tier);
                yield muffler != null ? muffler.blockId : null;
            }

            case "coil" -> {
                ComponentEntry coil = ComponentRegistry.coilByTier(tier);
                yield coil != null ? coil.blockId : null;
            }

            case "maintenance" -> {
                for (ComponentEntry maintenance : ComponentRegistry.getMaintenanceHatches()) {
                    if (maintenance.tier == tier) {
                        yield maintenance.blockId;
                    }
                }
                yield null;
            }

            case "dual_hatch" -> {
                for (ComponentEntry hatch : ComponentRegistry.getDualHatches("INPUT")) {
                    if (hatch.tier == tier) {
                        yield hatch.blockId;
                    }
                }

                for (ComponentEntry hatch : ComponentRegistry.getDualHatches("OUTPUT")) {
                    if (hatch.tier == tier) {
                        yield hatch.blockId;
                    }
                }

                yield null;
            }

            default -> {
                yield getWirelessBlockId(type, tier);
            }
        };
    }

    private static String getWirelessBlockId(ComponentGroup type, int tier) {
        String tierName = getTierNameForWireless(tier);
        if (tierName == null) return null;
        if (type.registryCategory != null) {
            for (var e : ComponentRegistry.get(type.registryCategory)) {
                if (e.tier == tier && e.attrIs("hatchType", type.id.contains("input") ? "INPUT" : "OUTPUT"))
                    return e.blockId;
            }
        }
        return switch (type.id) {
            case "wireless_energy_input"  -> "gtmthings:" + tierName + "_2a_wireless_energy_input_hatch";
            case "wireless_energy_output" -> "gtmthings:" + tierName + "_2a_wireless_energy_output_hatch";
            case "wireless_laser_input"   -> "gtmthings:" + tierName + "_256a_wireless_laser_target_hatch";
            case "wireless_laser_output"  -> "gtmthings:" + tierName + "_256a_wireless_laser_source_hatch";
            default -> null;
        };
    }

    public static boolean canUpgrade(ComponentInfo component, int targetTier) {
        if (!component.getGroup().isUpgradeable) {
            return false;
        }

        if (targetTier == component.getTier()) {
            return false;
        }

        String blockId = getBlockIdFromConfig(component.getGroup(), targetTier, component);
        if (blockId == null) {
            return false;
        }

        return getComponentBlock(component.getGroup(), targetTier, component) != null;
    }

    public static String getUpgradeName(ComponentInfo component, int targetTier) {
        ComponentGroup type = component.getGroup();
        String tierDisplayName = getTierDisplayName(targetTier);

        return switch (type.id) {
            case "input_hatch", "input_hatch_1x", "quad_input_hatch", "nonuple_input_hatch" -> {
                String cap = switch (type.id) {
                    case "quad_input_hatch"    -> "4x";
                    case "nonuple_input_hatch" -> "9x";
                    default                    -> "1x";
                };
                for (ComponentEntry hatch : ComponentRegistry.getFluidHatches("INPUT")) {
                    if (hatch.tier == targetTier && hatch.attrIs("capacity", cap)) yield hatch.displayName + " →";
                }
                yield null;
            }

            case "output_hatch", "output_hatch_1x", "quad_output_hatch", "nonuple_output_hatch" -> {
                String cap = switch (type.id) {
                    case "quad_output_hatch"    -> "4x";
                    case "nonuple_output_hatch" -> "9x";
                    default                     -> "1x";
                };
                for (ComponentEntry hatch : ComponentRegistry.getFluidHatches("OUTPUT")) {
                    if (hatch.tier == targetTier && hatch.attrIs("capacity", cap)) yield hatch.displayName + " →";
                }
                yield null;
            }

            case "input_bus" -> {
                for (ComponentEntry bus : ComponentRegistry.getBuses("INPUT")) {
                    if (bus.tier == targetTier) yield bus.displayName + " →";
                }
                yield null;
            }

            case "output_bus" -> {
                for (ComponentEntry bus : ComponentRegistry.getBuses("OUTPUT")) {
                    if (bus.tier == targetTier) yield bus.displayName + " →";
                }
                yield null;
            }

            case "energy_hatch" -> {
                for (ComponentEntry energy : ComponentRegistry.getEnergyHatches()) {
                    if (energy.tier == targetTier && energy.attrIs("hatchType", "INPUT")) {
                        yield energy.displayName + " →";
                    }
                }
                yield null;
            }

            case "dynamo_hatch" -> {
                for (ComponentEntry energy : ComponentRegistry.getEnergyHatches()) {
                    if (energy.tier == targetTier && energy.attrIs("hatchType", "OUTPUT")) {
                        yield energy.displayName + " →";
                    }
                }
                yield null;
            }

            case "substation_input" -> {
                ComponentEntry substation = ComponentRegistry.substationHatchForTier("INPUT", targetTier);
                yield substation != null ? substation.displayName + " →" : null;
            }

            case "substation_output" -> {
                ComponentEntry substation = ComponentRegistry.substationHatchForTier("OUTPUT", targetTier);
                yield substation != null ? substation.displayName + " →" : null;
            }

            case "input_laser" -> {
                ComponentEntry laser = ComponentRegistry.laserHatchForTier("INPUT", targetTier, null);
                yield laser != null ? laser.displayName + " →" : null;
            }

            case "output_laser" -> {
                ComponentEntry laser = ComponentRegistry.laserHatchForTier("OUTPUT", targetTier, null);
                yield laser != null ? laser.displayName + " →" : null;
            }

            case "parallel_hatch" -> {
                for (ComponentEntry parallel : ComponentRegistry.getParallelHatches()) {
                    if (parallel.tier == targetTier && parallel.attrIs("variant", "STANDARD")) {
                        yield parallel.displayName + " →";
                    }
                }
                yield null;
            }

            case "muffler" ->
                    tierDisplayName + " Muffler Hatch →";

            case "coil" -> {
                ComponentEntry coil = ComponentRegistry.coilByTier(targetTier);
                yield coil != null ? coil.displayName : null;
            }

            case "maintenance" -> {
                for (ComponentEntry maintenance : ComponentRegistry.getMaintenanceHatches()) {
                    if (maintenance.tier == targetTier) {
                        yield maintenance.displayName + " →";
                    }
                }
                yield null;
            }

            default -> {
                yield tierDisplayName + " " + type.displayName + " →";
            }
        };
    }

    public static List<Integer> getAvailableTiers(ComponentGroup type) {
        return switch (type.id) {
            case "input_hatch", "input_hatch_1x", "quad_input_hatch", "nonuple_input_hatch" ->
                    ComponentRegistry.getFluidHatches("INPUT").stream().map(e -> e.tier).distinct().sorted().toList();

            case "output_hatch", "output_hatch_1x", "quad_output_hatch", "nonuple_output_hatch" ->
                    ComponentRegistry.getFluidHatches("OUTPUT").stream().map(e -> e.tier).distinct().sorted().toList();

            case "input_bus" -> ComponentRegistry.getBuses("INPUT").stream().map(e -> e.tier).sorted().toList();
            case "output_bus" -> ComponentRegistry.getBuses("OUTPUT").stream().map(e -> e.tier).sorted().toList();

            case "energy_hatch", "dynamo_hatch" ->
                    ComponentRegistry.getEnergyHatches().stream()
                            .filter(h -> !h.blockId.contains("substation"))
                            .map(h -> h.tier)
                            .distinct()
                            .sorted()
                            .toList();

            case "substation_input", "substation_output" ->
                    ComponentRegistry.get(ComponentRegistry.SUBSTATION_HATCHES).stream()
                            .map(h -> h.tier)
                            .distinct()
                            .sorted()
                            .toList();

            case "input_laser", "output_laser" ->
                    ComponentRegistry.getLaserHatches().stream()
                            .map(l -> l.tier)
                            .filter(t -> t >= 5)
                            .distinct()
                            .sorted()
                            .toList();

            case "parallel_hatch" ->
                    ComponentRegistry.getParallelHatches().stream()
                            .filter(p -> p.attrIs("variant", "STANDARD"))
                            .map(p -> p.tier)
                            .sorted()
                            .toList();

            case "muffler" ->
                    ComponentRegistry.get(ComponentRegistry.MUFFLER_HATCHES).stream().map(e -> e.tier).sorted().toList();

            case "coil" ->
                    ComponentRegistry.getCoils().stream().map(e -> e.tier).sorted().toList();

            case "maintenance" ->
                    ComponentRegistry.getMaintenanceHatches().stream()
                            .map(m -> m.tier)
                            .distinct()
                            .sorted()
                            .toList();

            case "dual_hatch" ->
                    ComponentRegistry.getDualHatches("INPUT").stream()
                            .map(h -> h.tier)
                            .distinct()
                            .sorted()
                            .toList();

            default -> {
                if (type.id.startsWith("wireless_energy_")) {
                    yield ComponentRegistry.getWirelessHatches().stream().map(e -> e.tier).distinct().sorted().toList();
                } else if (type.id.startsWith("wireless_laser_")) {
                    yield ComponentRegistry.getWirelessHatches().stream().map(e -> e.tier).filter(t -> t >= 5).distinct().sorted().toList();
                }
                if (type.registryCategory != null) {
                    yield ComponentRegistry.get(type.registryCategory).stream().map(e -> e.tier).distinct().sorted().toList();
                }
                yield List.of();
            }
        };
    }

    private static String getTierNameForWireless(int tier) {
        if (tier < 0 || tier >= GTValues.VN.length) {
            return null;
        }
        return GTValues.VN[tier].toLowerCase();
    }

    private static String getTierDisplayName(int tier) {
        if (tier < 0 || tier >= GTValues.VN.length) {
            return "Unknown";
        }
        return GTValues.VN[tier];
    }
}
