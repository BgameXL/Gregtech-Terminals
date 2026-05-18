package com.gtceuterminal.common.compat;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class AE2Reflection {

    private AE2Reflection() {}

    private static final String GRID_HOST       = "appeng.api.networking.IGridHost";
    private static final String GRID_NODE       = "appeng.api.networking.IGridNode";
    private static final String GRID            = "appeng.api.networking.IGrid";
    private static final String STORAGE_GRID    = "appeng.api.networking.storage.IStorageGrid";
    private static final String AE_ITEM_STACK   = "appeng.api.storage.data.IAEItemStack";
    private static final String AE_ITEM_FACTORY = "appeng.util.item.AEItemStack";
    private static final String ME_INVENTORY    = "appeng.api.storage.IMEInventory";
    private static final String ACTIONABLE      = "appeng.api.config.Actionable";
    private static final String ACTION_SOURCE   = "appeng.api.networking.security.IActionSource";
    private static final String MACHINE_SOURCE  = "appeng.me.helpers.MachineSource";

    // Grid resolution
    public static Object resolveInventory(BlockEntity be) {
        try {
            Class<?> gridHostClass = Class.forName(GRID_HOST);
            if (!gridHostClass.isInstance(be)) return null;

            Object gridNode     = gridHostClass.getMethod("getGridNode").invoke(gridHostClass.cast(be));
            Object grid         = Class.forName(GRID_NODE).getMethod("getGrid").invoke(gridNode);
            Class<?> storageGridClass = Class.forName(STORAGE_GRID);
            Object storageGrid  = Class.forName(GRID).getMethod("getService", Class.class).invoke(grid, storageGridClass);
            return storageGridClass.getMethod("getInventory").invoke(storageGrid);
        } catch (ClassNotFoundException e) {
            GTCEUTerminalMod.LOGGER.debug("AE2Reflection: AE2 not present");
            return null;
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("AE2Reflection: resolveInventory failed", t);
            return null;
        }
    }

    public static ItemStack extract(Object inventory, BlockEntity be, ItemStack stack, long amount, boolean simulate) {
        if (inventory == null || stack.isEmpty()) return null;
        try {
            Class<?> aeItemStackClass   = Class.forName(AE_ITEM_STACK);
            Class<?> aeItemFactoryClass = Class.forName(AE_ITEM_FACTORY);

            Object aeStack = aeItemFactoryClass.getMethod("fromItemStack", ItemStack.class).invoke(null, stack);
            aeItemStackClass.getMethod("setStackSize", long.class).invoke(aeStack, amount);

            Class<?> actionableClass = Class.forName(ACTIONABLE);
            String actionName = simulate ? "SIMULATE" : "MODULATE";
            Object action = actionableClass.getField(actionName).get(null);

            Class<?> machineSourceClass = Class.forName(MACHINE_SOURCE);
            Object actionSource = machineSourceClass.getConstructor(Object.class).newInstance(be);

            Class<?> inventoryClass = Class.forName(ME_INVENTORY);
            Object result = inventoryClass.getMethod("extractItems", aeItemStackClass, actionableClass, Class.forName(ACTION_SOURCE))
                    .invoke(inventory, aeStack, action, actionSource);

            if (result == null) return null;
            long extracted = (long) aeItemStackClass.getMethod("getStackSize").invoke(result);
            if (extracted <= 0) return null;

            ItemStack out = stack.copy();
            out.setCount((int) Math.min(extracted, Integer.MAX_VALUE));
            return out;
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("AE2Reflection: extract failed", t);
            return null;
        }
    }

    public static ItemStack extractFirstCandidate(Object inventory, BlockEntity be, List<ItemStack> candidates) {
        for (ItemStack candidate : candidates) {
            if (candidate.isEmpty()) continue;
            ItemStack result = extract(inventory, be, candidate, 1, true);
            if (result != null && !result.isEmpty()) {
                return extract(inventory, be, candidate, 1, false);
            }
        }
        return null;
    }

    public static boolean isGridHost(BlockEntity be) {
        try {
            return Class.forName(GRID_HOST).isInstance(be);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}