package com.gtceuterminal.common.energy;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.compat.RecipeLogicReflection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.Ingredient;

import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Tracks recipe history for machines with RecipeLogic, detecting starts, completions, and interruptions.
@Mod.EventBusSubscriber(modid = com.gtceuterminal.GTCEUTerminalMod.MOD_ID)
public class RecipeHistoryTracker {

    public static final int MAX_HISTORY = 20;

    private static final Map<BlockPos, Deque<RecipeHistoryEntry>> historyMap = new HashMap<>();

    private static final Map<BlockPos, TrackState> stateMap = new HashMap<>();

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        historyMap.clear();
        stateMap.clear();
        GTCEUTerminalMod.LOGGER.debug("RecipeHistoryTracker: cleared all state on level unload");
    }

    // Internal state per machine
    private static class TrackState {
        String  lastRecipeId     = "";
        int     lastProgress     = 0;
        long    recipeStartTime  = 0;
        String  currentOutput    = "";
        int     currentDuration  = 0;
        boolean wasWorking       = false;
    }

    // Public API
    public static void poll(BlockPos pos, IRecipeLogicMachine rlm) {
        try {
            RecipeLogic logic = rlm.getRecipeLogic();
            if (logic == null) return;

            TrackState state = stateMap.computeIfAbsent(pos, k -> new TrackState());

            boolean isWorking = logic.isWorking();
            int progress = getProgress(logic);
            int duration = logic.getMaxProgress();

            GTRecipe currentRecipe = getCurrentRecipe(logic).orElse(null);
            String currentId = currentRecipe != null && currentRecipe.id != null
                    ? currentRecipe.id.toString() : "";

            if (isWorking && !state.lastRecipeId.equals(currentId)) {
                if (!state.lastRecipeId.isEmpty() && state.wasWorking) {
                    addEntry(pos, new RecipeHistoryEntry(
                            state.currentOutput, state.currentDuration,
                            state.recipeStartTime, false));
                }

                state.lastRecipeId    = currentId;
                state.recipeStartTime = System.currentTimeMillis();
                state.currentOutput   = getOutputName(currentRecipe);
                state.currentDuration = duration;
                state.lastProgress    = progress;
                state.wasWorking      = true;

            } else if (state.wasWorking && !isWorking && !state.lastRecipeId.isEmpty()) {
                boolean completed = state.lastProgress >= state.currentDuration - 2
                        || progress == 0 && state.lastProgress > 0;
                addEntry(pos, new RecipeHistoryEntry(
                        state.currentOutput, state.currentDuration,
                        state.recipeStartTime, completed));

                state.lastRecipeId = "";
                state.wasWorking   = false;

            } else if (isWorking && progress < state.lastProgress - 5) {
                addEntry(pos, new RecipeHistoryEntry(
                        state.currentOutput, state.currentDuration,
                        state.recipeStartTime, true));

                state.recipeStartTime = System.currentTimeMillis();
                state.currentDuration = duration;
                state.currentOutput   = getOutputName(currentRecipe);
            }

            state.lastProgress = progress;
            state.wasWorking   = isWorking;

        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.debug("RecipeHistoryTracker.poll error at {}: {}", pos, e.getMessage());
        }
    }

    public static List<RecipeHistoryEntry> getHistory(BlockPos pos) {
        Deque<RecipeHistoryEntry> deque = historyMap.get(pos);
        if (deque == null) return List.of();
        return List.copyOf(deque);
    }

    public static void clearHistory(BlockPos pos) {
        historyMap.remove(pos);
        stateMap.remove(pos);
    }

    // Internal helpers
    private static void addEntry(BlockPos pos, RecipeHistoryEntry entry) {
        Deque<RecipeHistoryEntry> deque =
                historyMap.computeIfAbsent(pos, k -> new ArrayDeque<>());
        deque.addLast(entry);
        while (deque.size() > MAX_HISTORY) deque.pollFirst();
    }

    private static int getProgress(RecipeLogic logic) {
        int p = RecipeLogicReflection.getProgress(logic);
        return p > 0 ? p : (int)(logic.getProgressPercent() * logic.getMaxProgress());
    }

    private static java.util.Optional<GTRecipe> getCurrentRecipe(RecipeLogic logic) {
        return RecipeLogicReflection.getLastRecipe(logic);
    }

    private static String getOutputName(GTRecipe recipe) {
        if (recipe == null) return "Unknown";
        try {
            var outputs = recipe.outputs.get(ItemRecipeCapability.CAP);
            if (outputs != null && !outputs.isEmpty()) {
                Object raw = outputs.get(0).content;
                if (raw instanceof Ingredient ing) {
                    var items = ing.getItems();
                    if (items.length > 0 && !items[0].isEmpty()) {
                        return items[0].getHoverName().getString();
                    }
                }
            }
        } catch (RuntimeException e) {
            GTCEUTerminalMod.LOGGER.debug("RecipeHistoryTracker: error reading recipe output name", e);
        }
        if (recipe.id != null) {
            String path = recipe.id.getPath();
            int slash = path.lastIndexOf('/');
            return EnergyDataCollector.toReadableName(slash >= 0 ? path.substring(slash + 1) : path);
        }
        return "Unknown";
    }
}