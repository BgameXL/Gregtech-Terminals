package com.gtceuterminal.common.material;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.ae2.CuriosCompat;
import com.gtceuterminal.common.ae2.MENetworkItemExtractor;
import com.gtceuterminal.common.ae2.WirelessTerminalHandler;
import com.gtceuterminal.common.item.MultiStructureManagerItem;
import com.gtceuterminal.common.item.SchematicInterfaceItem;
import com.gtceuterminal.common.multiblock.ComponentGroup;
import com.gtceuterminal.common.multiblock.ComponentInfo;
import com.gtceuterminal.common.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Calculates material requirements and checks availability
public class MaterialCalculator {

    private static final int CHEST_SCAN_RADIUS = 3;

    public static Map<Item, Integer> calculateUpgradeCost(ComponentInfo component, int targetTier) {
        Map<Item, Integer> materials = new HashMap<>();

        ComponentGroup type = component.getGroup();
        int currentTier = component.getTier();

        if (targetTier <= currentTier) {
            GTCEUTerminalMod.LOGGER.warn("Cannot upgrade {} from tier {} to tier {} (must be higher)",
                    type.displayName, currentTier, targetTier);
            return materials;
        }

        String targetHatchId = findHatchBlockId(type, targetTier);

        if (targetHatchId == null) {
            GTCEUTerminalMod.LOGGER.warn("Could not find hatch block for {} tier {}",
                    type.displayName, targetTier);
            return materials;
        }

        Item targetHatch = getItemFromId(targetHatchId);

        if (targetHatch != null) {
            materials.put(targetHatch, 1);
            GTCEUTerminalMod.LOGGER.info("Upgrade {} (T{} → T{}) requires: {}",
                    type.displayName, currentTier, targetTier, targetHatchId);
        } else {
            GTCEUTerminalMod.LOGGER.error("Target hatch item not found: {}", targetHatchId);
        }

        return materials;
    }

    private static String findHatchBlockId(ComponentGroup type, int tier) {
        String tierName = com.gregtechceu.gtceu.api.GTValues.VN[tier].toLowerCase();

        String blockId = switch (type.id) {
            case "energy_hatch"          -> findEnergyHatch(tierName, true);
            case "dynamo_hatch"          -> findEnergyHatch(tierName, false);
            case "input_hatch"           -> findFluidHatch(tierName, true);
            case "output_hatch"          -> findFluidHatch(tierName, false);
            case "input_bus"             -> findItemBus(tierName, true);
            case "output_bus"            -> findItemBus(tierName, false);
            case "dual_hatch"            -> findDualHatch(tierName);
            case "input_laser"           -> findLaserHatch(tierName, true);
            case "output_laser"          -> findLaserHatch(tierName, false);
            case "maintenance"           -> "gtceu:maintenance_hatch";
            case "muffler"              -> "gtceu:" + tierName + "_muffler_hatch";
            case "wireless_energy_input"  -> findWirelessEnergyHatch(tierName, true);
            case "wireless_energy_output" -> findWirelessEnergyHatch(tierName, false);
            default -> null;
        };

        return blockId;
    }

    private static String findEnergyHatch(String tier, boolean isInput) {
        String suffix = isInput ? "_energy_input_hatch" : "_energy_output_hatch";

        String gtceu = "gtceu:" + tier + suffix;
        if (itemExists(gtceu)) return gtceu;

        String gtmthings = "gtmthings:" + tier + suffix;
        if (itemExists(gtmthings)) return gtmthings;

        return gtceu;
    }

    private static String findFluidHatch(String tier, boolean isInput) {
        String suffix = isInput ? "_input_hatch" : "_output_hatch";

        String gtceu = "gtceu:" + tier + suffix;
        if (itemExists(gtceu)) return gtceu;

        String withFluid = "gtceu:" + tier + "_fluid" + suffix;
        if (itemExists(withFluid)) return withFluid;

        return gtceu;
    }

    private static String findItemBus(String tier, boolean isInput) {
        String suffix = isInput ? "_input_bus" : "_output_bus";

        String gtceu = "gtceu:" + tier + suffix;
        if (itemExists(gtceu)) return gtceu;

        String withItem = "gtceu:" + tier + "_item" + suffix;
        if (itemExists(withItem)) return withItem;

        return gtceu;
    }

    private static String findDualHatch(String tier) {
        String gtceuDual = "gtceu:" + tier + "_dual_input_hatch";
        if (itemExists(gtceuDual)) return gtceuDual;

        String gtmthingsHuge = "gtmthings:" + tier + "_huge_dual_hatch";
        if (itemExists(gtmthingsHuge)) return gtmthingsHuge;

        String gtceuSimple = "gtceu:" + tier + "_dual_hatch";
        if (itemExists(gtceuSimple)) return gtceuSimple;

        return gtceuDual;
    }

    private static String findLaserHatch(String tier, boolean isInput) {
        String suffix = isInput ? "_laser_input_hatch" : "_laser_output_hatch";

        String gtceu = "gtceu:" + tier + suffix;
        if (itemExists(gtceu)) return gtceu;

        String alt = isInput ?
                "gtceu:" + tier + "_laser_target_hatch" :
                "gtceu:" + tier + "_laser_source_hatch";
        if (itemExists(alt)) return alt;

        return gtceu;
    }

    private static String findWirelessEnergyHatch(String tier, boolean isInput) {
        String suffix = isInput ? "_wireless_energy_input_hatch" : "_wireless_energy_output_hatch";

        String gtmthings = "gtmthings:" + tier + suffix;
        if (itemExists(gtmthings)) return gtmthings;

        String[] ampVariants = {"", "_1a", "_4a", "_16a"};
        for (String amp : ampVariants) {
            String variant = "gtmthings:" + tier + amp + suffix;
            if (itemExists(variant)) return variant;
        }

        return gtmthings;
    }

    private static boolean itemExists(String itemId) {
        try {
            net.minecraft.resources.ResourceLocation loc = net.minecraft.resources.ResourceLocation.parse(itemId);
            return net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(loc);
        } catch (Exception e) {
            return false;
        }
    }

    private static Item getItemFromId(String itemId) {
        try {
            net.minecraft.resources.ResourceLocation loc = net.minecraft.resources.ResourceLocation.parse(itemId);
            return net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(loc);
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("Error getting item: {}", itemId, e);
            return null;
        }
    }


    public static Map<Item, Integer> scanPlayerInventory(Player player) {
        Map<Item, Integer> inventory = new HashMap<>();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                inventory.merge(item, stack.getCount(), Integer::sum);
            }
        }

        return inventory;
    }

    public static Map<Item, Integer> scanNearbyChests(Level level, BlockPos center) {
        Map<Item, Integer> items = new HashMap<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-CHEST_SCAN_RADIUS, -CHEST_SCAN_RADIUS, -CHEST_SCAN_RADIUS),
                center.offset(CHEST_SCAN_RADIUS, CHEST_SCAN_RADIUS, CHEST_SCAN_RADIUS)
        )) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof ChestBlockEntity chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    ItemStack stack = chest.getItem(i);
                    if (!stack.isEmpty()) {
                        Item item = stack.getItem();
                        items.merge(item, stack.getCount(), Integer::sum);
                    }
                }
            }
        }

        return items;
    }


    private static ItemStack findWirelessTerminal(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (isTerminal(mainHand)) return mainHand;

        ItemStack offHand = player.getOffhandItem();
        if (isTerminal(offHand)) return offHand;

        for (ItemStack stack : player.getInventory().items)
            if (isTerminal(stack)) return stack;

        if (MiscUtil.isCuriosLoaded) {
            for (ItemStack stack : CuriosCompat.getEquippedItems(player))
                if (isTerminal(stack)) return stack;
        }

        return ItemStack.EMPTY;
    }

    private static boolean isTerminal(ItemStack terminal) {
        return terminal.getItem() instanceof MultiStructureManagerItem ||
                terminal.getItem() instanceof SchematicInterfaceItem;
    }

    public static List<MaterialAvailability> checkMaterialsAvailability(
            Map<Item, Integer> required,
            Player player,
            Level level
    ) {
        GTCEUTerminalMod.LOGGER.info("=== Checking Materials Availability (Server: {}) ===", !level.isClientSide);

        List<MaterialAvailability> availability = new ArrayList<>();

        Map<Item, Integer> playerInv = scanPlayerInventory(player);
        Map<Item, Integer> chests = scanNearbyChests(level, player.blockPosition());

        ItemStack wirelessTerminal = findWirelessTerminal(player);
        boolean isLinked = !wirelessTerminal.isEmpty() && WirelessTerminalHandler.isLinked(wirelessTerminal);

        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            MaterialAvailability mat = new MaterialAvailability(entry.getKey(), entry.getValue());

            mat.setInInventory(playerInv.getOrDefault(entry.getKey(), 0));
            mat.setInNearbyChests(chests.getOrDefault(entry.getKey(), 0));

            long inME = 0;

            if (WirelessTerminalHandler.isLinked(wirelessTerminal)) {
                if (level.isClientSide) {
                    long needed = entry.getValue();
                    long haveLocally = mat.getInInventory() + mat.getInNearbyChests();
                    long stillNeeded = Math.max(0, needed - haveLocally);

                    inME = stillNeeded;
                    GTCEUTerminalMod.LOGGER.info("  {} [CLIENT]: Wireless linked, assuming ME can provide {} (will verify on server)",
                            entry.getKey().getDescription().getString(), stillNeeded);
                } else {
                    inME = MENetworkItemExtractor.checkItemAvailability(
                            wirelessTerminal, level, player, entry.getKey()
                    );
                    GTCEUTerminalMod.LOGGER.info("  {} [SERVER]: Found {} in ME",
                            entry.getKey().getDescription().getString(), inME);
                }
            }

            mat.setInMENetwork(inME);

            String itemName = entry.getKey().getDescription().getString();
            GTCEUTerminalMod.LOGGER.info("  {}: Required={}, InInv={}, InChests={}, InME={}, Total={}, Enough={}",
                    itemName, entry.getValue(), mat.getInInventory(), mat.getInNearbyChests(),
                    inME, mat.getTotalAvailable(), mat.hasEnough());

            availability.add(mat);
        }

        return availability;
    }

    public static boolean hasEnoughMaterials(List<MaterialAvailability> materials) {
        return materials.stream().allMatch(MaterialAvailability::hasEnough);
    }

    public static Map<Item, Integer> getMissingMaterials(List<MaterialAvailability> materials) {
        Map<Item, Integer> missing = new HashMap<>();

        for (MaterialAvailability mat : materials) {
            if (!mat.hasEnough()) {
                missing.put(mat.getItem(), mat.getMissing());
            }
        }

        return missing;
    }

    public static boolean extractFromInventory(Player player, Item item, int amount) {
        int remaining = amount;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                int toTake = Math.min(remaining, stack.getCount());
                stack.shrink(toTake);
                remaining -= toTake;

                if (remaining <= 0) {
                    return true;
                }
            }
        }

        return remaining <= 0;
    }

    public static boolean extractFromChests(Level level, BlockPos center, Item item, int amount) {
        int remaining = amount;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-CHEST_SCAN_RADIUS, -CHEST_SCAN_RADIUS, -CHEST_SCAN_RADIUS),
                center.offset(CHEST_SCAN_RADIUS, CHEST_SCAN_RADIUS, CHEST_SCAN_RADIUS)
        )) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof ChestBlockEntity chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    ItemStack stack = chest.getItem(i);
                    if (stack.getItem() == item) {
                        int toTake = Math.min(remaining, stack.getCount());
                        stack.shrink(toTake);
                        remaining -= toTake;

                        if (remaining <= 0) {
                            chest.setChanged();
                            return true;
                        }
                    }
                }
            }
        }

        return remaining <= 0;
    }

    public static boolean extractMaterials(
            List<MaterialAvailability> materials,
            Player player,
            Level level,
            boolean useInventory,
            boolean useChests
    ) {
        GTCEUTerminalMod.LOGGER.info("=== Extracting Materials (Server: {}) ===", !level.isClientSide);

        Map<Item, Integer> required = new HashMap<>();
        for (MaterialAvailability mat : materials) {
            required.put(mat.getItem(), mat.getRequired());
        }

        ItemStack wirelessTerminal = findWirelessTerminal(player);

        if (!wirelessTerminal.isEmpty()) {
            GTCEUTerminalMod.LOGGER.info("  Found wireless terminal, trying ME Network extraction...");

            MENetworkItemExtractor.ExtractResult result =
                    MENetworkItemExtractor.tryExtractFromMEOrInventory(
                            wirelessTerminal, level, player, required
                    );

            if (result.success) {
                if (result.source == MENetworkItemExtractor.ExtractionSource.ME_NETWORK) {
                    GTCEUTerminalMod.LOGGER.info("  ✓ Successfully extracted from ME Network");
                    return true;
                } else if (result.source == MENetworkItemExtractor.ExtractionSource.PLAYER_INVENTORY) {
                    GTCEUTerminalMod.LOGGER.info("  ✓ Successfully extracted from Player Inventory");
                    return true;
                }
            }

            GTCEUTerminalMod.LOGGER.warn("  ✗ ME Network extraction failed, trying traditional method...");
        }

        for (MaterialAvailability mat : materials) {
            int remaining = mat.getRequired();
            String itemName = mat.getItemName();

            GTCEUTerminalMod.LOGGER.info("  Item: {}, Required: {}", itemName, remaining);

            if (useInventory && remaining > 0) {
                int available = Math.min(remaining, mat.getInInventory());
                if (available > 0 && extractFromInventory(player, mat.getItem(), available)) {
                    GTCEUTerminalMod.LOGGER.info("    Extracted {} from inventory", available);
                    remaining -= available;
                }
            }

            if (useChests && remaining > 0) {
                int available = Math.min(remaining, mat.getInNearbyChests());
                if (available > 0 && extractFromChests(level, player.blockPosition(), mat.getItem(), available)) {
                    GTCEUTerminalMod.LOGGER.info("    Extracted {} from chests", available);
                    remaining -= available;
                }
            }

            if (remaining > 0) {
                GTCEUTerminalMod.LOGGER.warn("  FAILED: Still missing {} of {}", remaining, itemName);
                return false;
            }

            GTCEUTerminalMod.LOGGER.info("  SUCCESS: Got all required {}", itemName);
        }

        GTCEUTerminalMod.LOGGER.info("=== All Materials Extracted Successfully ===");
        return true;
    }
}