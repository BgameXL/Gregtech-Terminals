package com.gtceuterminal.common.compat;

import com.gtceuterminal.GTCEUTerminalMod;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import java.lang.reflect.Field;
import java.util.Optional;

public final class RecipeLogicReflection {

    private static Field F_PROGRESS;
    private static Field F_LAST_RECIPE;
    private static boolean READY = false;

    private RecipeLogicReflection() {}

    public static synchronized void ensure() {
        if (READY) return;
        try {
            F_PROGRESS    = field("progress");
            F_LAST_RECIPE = field("lastRecipe");
            READY = true;
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.warn("RecipeLogicReflection: could not cache RecipeLogic fields — recipe tracking will be limited", t);
        }
    }

    private static Field field(String name) throws ReflectiveOperationException {
        Field f = RecipeLogic.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    public static int getProgress(RecipeLogic logic) {
        ensure();
        if (F_PROGRESS == null || logic == null) return 0;
        try { return (int) F_PROGRESS.get(logic); }
        catch (IllegalAccessException e) {
            GTCEUTerminalMod.LOGGER.debug("RecipeLogicReflection: getProgress failed", e);
            return 0;
        }
    }

    public static Optional<GTRecipe> getLastRecipe(RecipeLogic logic) {
        ensure();
        if (F_LAST_RECIPE == null || logic == null) return Optional.empty();
        try {
            Object val = F_LAST_RECIPE.get(logic);
            return val instanceof GTRecipe r ? Optional.of(r) : Optional.empty();
        } catch (IllegalAccessException e) {
            GTCEUTerminalMod.LOGGER.debug("RecipeLogicReflection: getLastRecipe failed", e);
            return Optional.empty();
        }
    }
}