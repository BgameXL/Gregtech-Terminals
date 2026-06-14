package com.gtceuterminal.common.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public final class MENetworkCrafting {

    private static final int MAX_AGE_TICKS = 600;

    private static final List<Pending> PENDING = new ArrayList<>();

    private MENetworkCrafting() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(MENetworkCrafting::onServerTick);
    }

    public static int requestCrafts(ServerPlayer player, Map<Item, Integer> deficits) {
        if (!MENetworkScanner.isAE2Available() || deficits.isEmpty()) return 0;

        IGrid grid = MEQueryResult.resolve(player).getGrid();
        if (grid == null) {
            GTCEUTerminalMod.LOGGER.warn("MENetworkCrafting.requestCrafts: no linked ME grid");
            return 0;
        }

        ICraftingService cs    = grid.getCraftingService();
        IActionSource     src   = IActionSource.ofPlayer(player);
        Level             level = player.serverLevel();
        ICraftingSimulationRequester requester = new SimRequester(src, grid.getPivot());

        int handled = 0;
        for (Map.Entry<Item, Integer> e : deficits.entrySet()) {
            int amount = e.getValue();
            if (amount <= 0) continue;
            AEItemKey key = AEItemKey.of(e.getKey());
            if (key == null) continue;

            if (!cs.isCraftable(key)) {
                GTCEUTerminalMod.LOGGER.info("MENetworkCrafting: {} is not craftable in the ME network", e.getKey());
                continue;
            }
            if (cs.isRequesting(key) || hasPending(player, e.getKey())) { handled++; continue; }

            try {
                Future<ICraftingPlan> future = cs.beginCraftingCalculation(
                        level, requester, key, amount, CalculationStrategy.CRAFT_LESS);
                PENDING.add(new Pending(future, grid, src, player, e.getKey(), amount));
                handled++;
                GTCEUTerminalMod.LOGGER.info("MENetworkCrafting: started crafting calculation for {} x{}", e.getKey(), amount);
            } catch (Exception ex) {
                GTCEUTerminalMod.LOGGER.warn("MENetworkCrafting: failed to begin crafting for {}", e.getKey(), ex);
            }
        }
        return handled;
    }

    private static boolean hasPending(ServerPlayer player, Item item) {
        for (Pending p : PENDING) {
            if (p.item == item && p.player == player) return true;
        }
        return false;
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;

        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (!p.future.isDone()) {
                if (++p.age > MAX_AGE_TICKS) {
                    p.future.cancel(true);
                    it.remove();
                    GTCEUTerminalMod.LOGGER.warn("MENetworkCrafting: crafting calculation timed out for {}", p.item);
                }
                continue;
            }
            it.remove();
            try {
                ICraftingPlan plan = p.future.get();
                if (plan == null) {
                    notify(p, "§c[ME] Crafting calculation failed: %s");
                    continue;
                }
                if (plan.simulation()) {
                    String missing = describeMissing(plan);
                    notify(p, "§c[ME] Cannot craft %s — missing: " + missing);
                    GTCEUTerminalMod.LOGGER.info("MENetworkCrafting: plan is simulation for {} — missing=[{}]", p.item, missing);
                    continue;
                }
                ICraftingSubmitResult result = p.grid.getCraftingService()
                        .submitJob(plan, null, null, false, p.src);
                if (result != null && result.successful()) {
                    GTCEUTerminalMod.LOGGER.info("MENetworkCrafting: submitted crafting job for {} x{}", p.item, p.amount);
                } else {
                    String code = result != null ? String.valueOf(result.errorCode()) : "unknown";
                    notify(p, "§c[ME] Could not start crafting %s (" + code + ")");
                    GTCEUTerminalMod.LOGGER.warn("MENetworkCrafting: submitJob unsuccessful for {}: {}", p.item, code);
                }
            } catch (Exception ex) {
                notify(p, "§c[ME] Crafting error for %s");
                GTCEUTerminalMod.LOGGER.warn("MENetworkCrafting: error finalizing crafting job for {}", p.item, ex);
            }
        }
    }

    private static String describeMissing(ICraftingPlan plan) {
        StringBuilder sb = new StringBuilder();
        try {
            for (appeng.api.stacks.AEKey k : plan.missingItems().keySet()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(k.getDisplayName().getString());
                if (sb.length() > 200) { sb.append("…"); break; }
            }
        } catch (Exception ignored) {}
        return sb.length() > 0 ? sb.toString() : "?";
    }

    private static void notify(Pending p, String format) {
        if (p.player == null || p.player.hasDisconnected()) return;
        String name = new ItemStack(p.item).getHoverName().getString();
        p.player.displayClientMessage(Component.literal(String.format(format, name)), false);
    }

    private record SimRequester(IActionSource source, IGridNode node)
            implements ICraftingSimulationRequester {
        @Override public IActionSource getActionSource() { return source; }
        @Override public IGridNode getGridNode() { return node; }
    }

    private static final class Pending {
        final Future<ICraftingPlan> future;
        final IGrid                 grid;
        final IActionSource         src;
        final ServerPlayer          player;
        final Item                  item;
        final long                  amount;
        int age;

        Pending(Future<ICraftingPlan> future, IGrid grid, IActionSource src,
                ServerPlayer player, Item item, long amount) {
            this.future = future;
            this.grid   = grid;
            this.src    = src;
            this.player = player;
            this.item   = item;
            this.amount = amount;
        }
    }
}
