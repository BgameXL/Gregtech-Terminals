package com.gtceuterminal.common.material;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.ae2.AccessPointLink;
import com.gtceuterminal.common.ae2.CuriosCompat;
import com.gtceuterminal.common.ae2.MENetworkItemExtractor;
import com.gtceuterminal.common.config.ItemsConfig;
import com.gtceuterminal.common.item.MultiStructureManagerItem;
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


    private static ItemStack findLinkedManager(Player player) {
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
        return terminal.getItem() instanceof MultiStructureManagerItem;
    }

    public static List<MaterialAvailability> checkMaterialsAvailability(
            Map<Item, Integer> required,
            Player player,
            Level level
    ) {
        List<MaterialAvailability> availability = new ArrayList<>();

        Map<Item, Integer> playerInv = scanPlayerInventory(player);
        Map<Item, Integer> chests = scanNearbyChests(level, player.blockPosition());

        ItemStack linkedManager = findLinkedManager(player);

        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            MaterialAvailability mat = new MaterialAvailability(entry.getKey(), entry.getValue());

            mat.setInInventory(playerInv.getOrDefault(entry.getKey(), 0));
            mat.setInNearbyChests(chests.getOrDefault(entry.getKey(), 0));

            long inME = 0;

            if (ItemsConfig.isMgrAllowMENetworkUpgrade() && AccessPointLink.isLinked(linkedManager)) {
                if (level.isClientSide) {
                    long needed = entry.getValue();
                    long haveLocally = mat.getInInventory() + mat.getInNearbyChests();
                    long stillNeeded = Math.max(0, needed - haveLocally);

                    inME = stillNeeded;
                } else {
                    inME = MENetworkItemExtractor.checkItemAvailability(
                            linkedManager, level, player, entry.getKey()
                    );
                }
            }

            mat.setInMENetwork(inME);

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
        int available = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() == item) {
                available += player.getInventory().getItem(i).getCount();
            }
        }
        if (available < amount) return false;

        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (remaining <= 0) break;
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                int toTake = Math.min(remaining, stack.getCount());
                stack.shrink(toTake);
                remaining -= toTake;
            }
        }
        return true;
    }

    public static boolean extractFromChests(Level level, BlockPos center, Item item, int amount) {
        List<ChestBlockEntity> chests = new ArrayList<>();
        int available = 0;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-CHEST_SCAN_RADIUS, -CHEST_SCAN_RADIUS, -CHEST_SCAN_RADIUS),
                center.offset(CHEST_SCAN_RADIUS, CHEST_SCAN_RADIUS, CHEST_SCAN_RADIUS)
        )) {
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                chests.add(chest);
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    ItemStack stack = chest.getItem(i);
                    if (stack.getItem() == item) available += stack.getCount();
                }
            }
        }
        if (available < amount) return false;

        int remaining = amount;
        for (ChestBlockEntity chest : chests) {
            if (remaining <= 0) break;
            boolean changed = false;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                if (remaining <= 0) break;
                ItemStack stack = chest.getItem(i);
                if (stack.getItem() == item) {
                    int toTake = Math.min(remaining, stack.getCount());
                    stack.shrink(toTake);
                    remaining -= toTake;
                    changed = true;
                }
            }
            if (changed) chest.setChanged();
        }
        return true;
    }

    public static boolean extractMaterials(
            List<MaterialAvailability> materials,
            Player player,
            Level level,
            boolean useInventory,
            boolean useChests
    ) {
        ItemStack linkedManager = findLinkedManager(player);
        boolean meUsable = MiscUtil.isAE2Loaded
                && ItemsConfig.isMgrAllowMENetworkUpgrade()
                && !linkedManager.isEmpty()
                && AccessPointLink.isLinked(linkedManager);

        if (meUsable) {
            Map<Item, Integer> required = new HashMap<>();
            for (MaterialAvailability mat : materials) {
                required.put(mat.getItem(), mat.getRequired());
            }
            MENetworkItemExtractor.ExtractResult result =
                    MENetworkItemExtractor.tryExtractFromMEOrInventory(
                            linkedManager, level, player, required
                    );
            if (result.success()) {
                return true;
            }
        }

        Map<Item, Integer> fromME = new HashMap<>();
        Map<Item, Integer> fromInventory = new HashMap<>();
        Map<Item, Integer> fromChests = new HashMap<>();

        for (MaterialAvailability mat : materials) {
            int remaining = mat.getRequired();

            if (meUsable && remaining > 0) {
                int take = (int) Math.min(remaining, mat.getInMENetwork());
                if (take > 0) {
                    fromME.put(mat.getItem(), take);
                    remaining -= take;
                }
            }
            if (useInventory && remaining > 0) {
                int take = Math.min(remaining, mat.getInInventory());
                if (take > 0) {
                    fromInventory.put(mat.getItem(), take);
                    remaining -= take;
                }
            }
            if (useChests && remaining > 0) {
                int take = Math.min(remaining, mat.getInNearbyChests());
                if (take > 0) {
                    fromChests.put(mat.getItem(), take);
                    remaining -= take;
                }
            }

            if (remaining > 0) {
                GTCEUTerminalMod.LOGGER.warn("  Cannot fully source {} ({} short), aborting before any extraction",
                        mat.getItemName(), remaining);
                return false;
            }
        }

        Map<Item, Integer> taken = new HashMap<>();

        if (!fromME.isEmpty()) {
            if (!MENetworkItemExtractor.extractFromME(linkedManager, level, player, fromME)) {
                return false;
            }
            fromME.forEach((item, count) -> taken.merge(item, count, Integer::sum));
        }

        for (Map.Entry<Item, Integer> e : fromInventory.entrySet()) {
            if (!extractFromInventory(player, e.getKey(), e.getValue())) {
                compensate(player, taken);
                return false;
            }
            taken.merge(e.getKey(), e.getValue(), Integer::sum);
        }

        for (Map.Entry<Item, Integer> e : fromChests.entrySet()) {
            if (!extractFromChests(level, player.blockPosition(), e.getKey(), e.getValue())) {
                compensate(player, taken);
                return false;
            }
            taken.merge(e.getKey(), e.getValue(), Integer::sum);
        }

        return true;
    }

    private static void compensate(Player player, Map<Item, Integer> taken) {
        for (Map.Entry<Item, Integer> e : taken.entrySet()) {
            if (e.getValue() <= 0) continue;
            ItemStack back = new ItemStack(e.getKey(), e.getValue());
            if (!player.getInventory().add(back)) {
                player.drop(back, false);
            }
        }
    }
}
