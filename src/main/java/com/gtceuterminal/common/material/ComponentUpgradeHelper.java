package com.gtceuterminal.common.material;

import com.gregtechceu.gtceu.api.GTValues;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.config.*;
import com.gtceuterminal.common.multiblock.ComponentInfo;
import com.gtceuterminal.common.multiblock.ComponentType;
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

    public static boolean isUpgradeIdAllowed(ComponentType type, String targetBlockId) {
        if (type == null) return false;
        if (targetBlockId == null || targetBlockId.isBlank()) return false;

        if (type == ComponentType.MAINTENANCE) {
            return ComponentRegistry.getMaintenanceHatches().stream()
                    .anyMatch(e -> targetBlockId.equals(e.blockId));
        }

        return true;
    }

    public static Map<Item, Integer> getUpgradeItems(ComponentInfo component, int targetTier) {
        Map<Item, Integer> items = new HashMap<>();

        Block targetBlock = getComponentBlock(component.getType(), targetTier);
        if (targetBlock != null && targetBlock.asItem() != null) {
            items.put(targetBlock.asItem(), 1);
        }

        return items;
    }

    private static Block getComponentBlock(ComponentType type, int tier) {
        String blockId = getBlockIdFromConfig(type, tier, null);
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

    private static String getBlockIdFromConfig(ComponentType type, int tier, ComponentInfo component) {
        return switch (type) {
            case INPUT_HATCH, INPUT_HATCH_1X, QUAD_INPUT_HATCH, NONUPLE_INPUT_HATCH -> {
                for (ComponentEntry hatch : ComponentRegistry.getFluidHatches("INPUT")) {
                    if (hatch.tier == tier) {
                        boolean matches = switch (type) {
                            case INPUT_HATCH -> hatch.attrIs("capacity", "1x") && hatch.blockId.matches(".*:.*_input_hatch$");
                            case INPUT_HATCH_1X -> hatch.attrIs("capacity", "1x") && hatch.blockId.contains("_1x");
                            case QUAD_INPUT_HATCH -> hatch.attrIs("capacity", "4x");
                            case NONUPLE_INPUT_HATCH -> hatch.attrIs("capacity", "9x");
                            default -> false;
                        };
                        if (matches) yield hatch.blockId;
                    }
                }
                yield null;
            }

            case OUTPUT_HATCH, OUTPUT_HATCH_1X, QUAD_OUTPUT_HATCH, NONUPLE_OUTPUT_HATCH -> {
                for (ComponentEntry hatch : ComponentRegistry.getFluidHatches("OUTPUT")) {
                    if (hatch.tier == tier) {
                        boolean matches = switch (type) {
                            case OUTPUT_HATCH -> hatch.attrIs("capacity", "1x") && hatch.blockId.matches(".*:.*_output_hatch$");
                            case OUTPUT_HATCH_1X -> hatch.attrIs("capacity", "1x") && hatch.blockId.contains("_1x");
                            case QUAD_OUTPUT_HATCH -> hatch.attrIs("capacity", "4x");
                            case NONUPLE_OUTPUT_HATCH -> hatch.attrIs("capacity", "9x");
                            default -> false;
                        };
                        if (matches) yield hatch.blockId;
                    }
                }
                yield null;
            }

            case INPUT_BUS -> {
                for (ComponentEntry bus : ComponentRegistry.getBuses("INPUT")) {
                    if (bus.tier == tier) yield bus.blockId;
                }
                yield null;
            }

            case OUTPUT_BUS -> {
                for (ComponentEntry bus : ComponentRegistry.getBuses("OUTPUT")) {
                    if (bus.tier == tier) yield bus.blockId;
                }
                yield null;
            }

            case ENERGY_HATCH -> {
                String amperage = component != null ? component.getAmperage() : null;
                ComponentEntry energy = ComponentRegistry.energyHatchForTier("INPUT", tier, amperage);
                yield energy != null ? energy.blockId : null;
            }

            case DYNAMO_HATCH -> {
                String amperage = component != null ? component.getAmperage() : null;
                ComponentEntry dynamo = ComponentRegistry.energyHatchForTier("OUTPUT", tier, amperage);
                yield dynamo != null ? dynamo.blockId : null;
            }

            case SUBSTATION_INPUT_ENERGY -> {
                ComponentEntry substation = ComponentRegistry.substationHatchForTier("INPUT", tier);
                yield substation != null ? substation.blockId : null;
            }

            case SUBSTATION_OUTPUT_ENERGY -> {
                ComponentEntry substation = ComponentRegistry.substationHatchForTier("OUTPUT", tier);
                yield substation != null ? substation.blockId : null;
            }

            case INPUT_LASER -> {
                String amperage = component != null ? component.getAmperage() : null;
                ComponentEntry laser = ComponentRegistry.laserHatchForTier("INPUT", tier, amperage);
                yield laser != null ? laser.blockId : null;
            }

            case OUTPUT_LASER -> {
                String amperage = component != null ? component.getAmperage() : null;
                ComponentEntry laser = ComponentRegistry.laserHatchForTier("OUTPUT", tier, amperage);
                yield laser != null ? laser.blockId : null;
            }

            case PARALLEL_HATCH -> {
                for (ComponentEntry parallel : ComponentRegistry.getParallelHatches()) {
                    if (parallel.tier == tier && parallel.attrIs("variant", "STANDARD")) {
                        yield parallel.blockId;
                    }
                }
                yield null;
            }

            case MUFFLER -> {
                ComponentEntry muffler = ComponentRegistry.mufflerHatchForTier(tier);
                yield muffler != null ? muffler.blockId : null;
            }

            case COIL -> {
                ComponentEntry coil = ComponentRegistry.coilByTier(tier);
                yield coil != null ? coil.blockId : null;
            }

            case MAINTENANCE -> {
                for (ComponentEntry maintenance : ComponentRegistry.getMaintenanceHatches()) {
                    if (maintenance.tier == tier) {
                        yield maintenance.blockId;
                    }
                }
                yield null;
            }

            case DUAL_HATCH -> {
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
                String typeName = type.name();
                if (typeName.startsWith("WIRELESS_")) {
                    yield getWirelessBlockId(type, tier);
                }
                yield null;
            }
        };
    }

    private static String getWirelessBlockId(ComponentType type, int tier) {
        String tierName = getTierNameForWireless(tier);
        if (tierName == null) return null;

        return switch (type.name()) {
            case "WIRELESS_ENERGY_INPUT" -> "gtmthings:" + tierName + "_2a_wireless_energy_input_hatch";
            case "WIRELESS_ENERGY_OUTPUT" -> "gtmthings:" + tierName + "_2a_wireless_energy_output_hatch";
            case "WIRELESS_LASER_INPUT" -> "gtmthings:" + tierName + "_256a_wireless_laser_target_hatch";
            case "WIRELESS_LASER_OUTPUT" -> "gtmthings:" + tierName + "_256a_wireless_laser_source_hatch";
            default -> null;
        };
    }

    public static boolean canUpgrade(ComponentInfo component, int targetTier) {
        if (!component.getType().isUpgradeable()) {
            return false;
        }

        if (targetTier == component.getTier()) {
            return false;
        }

        String blockId = getBlockIdFromConfig(component.getType(), targetTier, component);
        if (blockId == null) {
            return false;
        }

        return getComponentBlock(component.getType(), targetTier) != null;
    }

    public static String getUpgradeName(ComponentInfo component, int targetTier) {
        ComponentType type = component.getType();
        String tierDisplayName = getTierDisplayName(targetTier);

        return switch (type) {
            case INPUT_HATCH, INPUT_HATCH_1X, QUAD_INPUT_HATCH, NONUPLE_INPUT_HATCH -> {
                for (ComponentEntry hatch : ComponentRegistry.getFluidHatches("INPUT")) {
                    if (hatch.tier == targetTier) {
                        boolean matches = switch (type) {
                            case INPUT_HATCH -> hatch.attrIs("capacity", "1x") && hatch.blockId.matches(".*:.*_input_hatch$");
                            case INPUT_HATCH_1X -> hatch.attrIs("capacity", "1x") && hatch.blockId.contains("_1x");
                            case QUAD_INPUT_HATCH -> hatch.attrIs("capacity", "4x");
                            case NONUPLE_INPUT_HATCH -> hatch.attrIs("capacity", "9x");
                            default -> false;
                        };
                        if (matches) yield hatch.displayName + " →";
                    }
                }
                yield null;
            }

            case OUTPUT_HATCH, OUTPUT_HATCH_1X, QUAD_OUTPUT_HATCH, NONUPLE_OUTPUT_HATCH -> {
                for (ComponentEntry hatch : ComponentRegistry.getFluidHatches("OUTPUT")) {
                    if (hatch.tier == targetTier) {
                        boolean matches = switch (type) {
                            case OUTPUT_HATCH -> hatch.attrIs("capacity", "1x") && hatch.blockId.matches(".*:.*_output_hatch$");
                            case OUTPUT_HATCH_1X -> hatch.attrIs("capacity", "1x") && hatch.blockId.contains("_1x");
                            case QUAD_OUTPUT_HATCH -> hatch.attrIs("capacity", "4x");
                            case NONUPLE_OUTPUT_HATCH -> hatch.attrIs("capacity", "9x");
                            default -> false;
                        };
                        if (matches) yield hatch.displayName + " →";
                    }
                }
                yield null;
            }

            case INPUT_BUS -> {
                for (ComponentEntry bus : ComponentRegistry.getBuses("INPUT")) {
                    if (bus.tier == targetTier) yield bus.displayName + " →";
                }
                yield null;
            }

            case OUTPUT_BUS -> {
                for (ComponentEntry bus : ComponentRegistry.getBuses("OUTPUT")) {
                    if (bus.tier == targetTier) yield bus.displayName + " →";
                }
                yield null;
            }

            case ENERGY_HATCH -> {
                for (ComponentEntry energy : ComponentRegistry.getEnergyHatches()) {
                    if (energy.tier == targetTier &&
                            energy.attrIs("hatchType", "INPUT") &&
                            energy.blockId.endsWith("_energy_input_hatch")) {
                        yield energy.displayName + " →";
                    }
                }
                yield null;
            }

            case DYNAMO_HATCH -> {
                for (ComponentEntry energy : ComponentRegistry.getEnergyHatches()) {
                    if (energy.tier == targetTier &&
                            energy.attrIs("hatchType", "OUTPUT") &&
                            energy.blockId.endsWith("_energy_output_hatch")) {
                        yield energy.displayName + " →";
                    }
                }
                yield null;
            }

            case SUBSTATION_INPUT_ENERGY -> {
                ComponentEntry substation = ComponentRegistry.substationHatchForTier("INPUT", targetTier);
                yield substation != null ? substation.displayName + " →" : null;
            }

            case SUBSTATION_OUTPUT_ENERGY -> {
                ComponentEntry substation = ComponentRegistry.substationHatchForTier("OUTPUT", targetTier);
                yield substation != null ? substation.displayName + " →" : null;
            }

            case INPUT_LASER -> {
                ComponentEntry laser = ComponentRegistry.laserHatchForTier("INPUT", targetTier, null);
                yield laser != null ? laser.displayName + " →" : null;
            }

            case OUTPUT_LASER -> {
                ComponentEntry laser = ComponentRegistry.laserHatchForTier("OUTPUT", targetTier, null);
                yield laser != null ? laser.displayName + " →" : null;
            }

            case PARALLEL_HATCH -> {
                for (ComponentEntry parallel : ComponentRegistry.getParallelHatches()) {
                    if (parallel.tier == targetTier && parallel.attrIs("variant", "STANDARD")) {
                        yield parallel.displayName + " →";
                    }
                }
                yield null;
            }

            case MUFFLER ->
                    tierDisplayName + " Muffler Hatch →";

            case COIL -> {
                ComponentEntry coil = ComponentRegistry.coilByTier(targetTier);
                yield coil != null ? coil.displayName : null;
            }

            case MAINTENANCE -> {
                for (ComponentEntry maintenance : ComponentRegistry.getMaintenanceHatches()) {
                    if (maintenance.tier == targetTier) {
                        yield maintenance.displayName + " →";
                    }
                }
                yield null;
            }

            default -> {
                String typeName = type.name();
                if (typeName.contains("WIRELESS_ENERGY_INPUT")) {
                    yield tierDisplayName + " Wireless Energy Input Hatch →";
                } else if (typeName.contains("WIRELESS_ENERGY_OUTPUT")) {
                    yield tierDisplayName + " Wireless Energy Output Hatch →";
                } else if (typeName.contains("WIRELESS_LASER_INPUT")) {
                    yield tierDisplayName + " Wireless Laser Target Hatch →";
                } else if (typeName.contains("WIRELESS_LASER_OUTPUT")) {
                    yield tierDisplayName + " Wireless Laser Source Hatch →";
                }
                yield null;
            }
        };
    }

    public static List<Integer> getAvailableTiers(ComponentType type) {
        return switch (type) {
            case INPUT_HATCH, INPUT_HATCH_1X, QUAD_INPUT_HATCH, NONUPLE_INPUT_HATCH ->
                    ComponentRegistry.getFluidHatches("INPUT").stream().map(e -> e.tier).distinct().sorted().toList();

            case OUTPUT_HATCH, OUTPUT_HATCH_1X, QUAD_OUTPUT_HATCH, NONUPLE_OUTPUT_HATCH ->
                    ComponentRegistry.getFluidHatches("OUTPUT").stream().map(e -> e.tier).distinct().sorted().toList();

            case INPUT_BUS -> ComponentRegistry.getBuses("INPUT").stream().map(e -> e.tier).sorted().toList();
            case OUTPUT_BUS -> ComponentRegistry.getBuses("OUTPUT").stream().map(e -> e.tier).sorted().toList();

            case ENERGY_HATCH, DYNAMO_HATCH ->
                    ComponentRegistry.getEnergyHatches().stream()
                            .filter(h -> !h.blockId.contains("substation"))
                            .map(h -> h.tier)
                            .distinct()
                            .sorted()
                            .toList();

            case SUBSTATION_INPUT_ENERGY, SUBSTATION_OUTPUT_ENERGY ->
                    ComponentRegistry.get(ComponentRegistry.SUBSTATION_HATCHES).stream()
                            .map(h -> h.tier)
                            .distinct()
                            .sorted()
                            .toList();

            case INPUT_LASER, OUTPUT_LASER ->
                    ComponentRegistry.getLaserHatches().stream()
                            .map(l -> l.tier)
                            .filter(t -> t >= 5)
                            .distinct()
                            .sorted()
                            .toList();

            case PARALLEL_HATCH ->
                    ComponentRegistry.getParallelHatches().stream()
                            .filter(p -> p.attrIs("variant", "STANDARD"))
                            .map(p -> p.tier)
                            .sorted()
                            .toList();

            case MUFFLER ->
                    ComponentRegistry.get(ComponentRegistry.MUFFLER_HATCHES).stream().map(e -> e.tier).sorted().toList();

            case COIL ->
                    ComponentRegistry.getCoils().stream().map(e -> e.tier).sorted().toList();

            case MAINTENANCE ->
                    ComponentRegistry.getMaintenanceHatches().stream()
                            .map(m -> m.tier)
                            .distinct()
                            .sorted()
                            .toList();

            case DUAL_HATCH ->
                    ComponentRegistry.getDualHatches("INPUT").stream()
                            .map(h -> h.tier)
                            .distinct()
                            .sorted()
                            .toList();

            default -> {
                String typeName = type.name();
                if (typeName.startsWith("WIRELESS_ENERGY_")) {
                    yield ComponentRegistry.getWirelessHatches().stream()
                            .map(e -> e.tier)
                            .distinct()
                            .sorted()
                            .toList();
                } else if (typeName.startsWith("WIRELESS_LASER_")) {
                    yield ComponentRegistry.getWirelessHatches().stream()
                            .map(e -> e.tier)
                            .filter(t -> t >= 5)
                            .distinct()
                            .sorted()
                            .toList();
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