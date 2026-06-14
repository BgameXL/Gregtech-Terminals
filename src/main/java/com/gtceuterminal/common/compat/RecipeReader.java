package com.gtceuterminal.common.compat;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import net.minecraftforge.fluids.FluidStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecipeReader {

    private RecipeReader() {}

    public record FluidLine(String name, long amount) {}

    public static final class RecipeView {
        public final List<ItemStack> items;
        public final List<FluidLine> fluids;

        public RecipeView(List<ItemStack> items, List<FluidLine> fluids) {
            this.items  = items;
            this.fluids = fluids;
        }

        public boolean isEmpty() { return items.isEmpty() && fluids.isEmpty(); }

        public static final RecipeView EMPTY = new RecipeView(List.of(), List.of());
    }

    private static boolean  gtChecked = false;
    private static boolean  gtReady   = false;
    private static Class<?> cGTRecipe;
    private static Field    fInputs;
    private static Field    fOutputs;
    private static Field    fContent;
    private static Object   itemCap;
    private static Object   fluidCap;
    private static Class<?> cSized;
    private static Method   mGetAmount;
    private static Class<?> cFluidIng;
    private static Method   mFluidStacks;
    private static Method   mFluidAmount;

    private static synchronized void ensureGT() {
        if (gtChecked) return;
        gtChecked = true;
        try {
            cGTRecipe = Class.forName("com.gregtechceu.gtceu.api.recipe.GTRecipe");
            fInputs   = cGTRecipe.getField("inputs");
            fOutputs  = cGTRecipe.getField("outputs");

            Class<?> cContent = Class.forName("com.gregtechceu.gtceu.api.recipe.content.Content");
            fContent  = cContent.getField("content");

            Class<?> cItemCap = Class.forName("com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability");
            itemCap   = cItemCap.getField("CAP").get(null);

            try {
                cSized     = Class.forName("com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient");
                mGetAmount = cSized.getMethod("getAmount");
            } catch (Throwable ignored) {
                cSized = null; mGetAmount = null;
            }

            try {
                Class<?> cFluidCap = Class.forName("com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability");
                fluidCap     = cFluidCap.getField("CAP").get(null);
                cFluidIng    = Class.forName("com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient");
                mFluidStacks = cFluidIng.getMethod("getStacks");
                mFluidAmount = cFluidIng.getMethod("getAmount");
            } catch (Throwable ignored) {
                fluidCap = null; cFluidIng = null; mFluidStacks = null; mFluidAmount = null;
            }

            gtReady = true;
        } catch (Throwable t) {
            gtReady = false;
            GTCEUTerminalMod.LOGGER.debug("RecipeReader: GT recipe support unavailable", t);
        }
    }

    public static RecipeView findIngredients(RecipeManager rm, RegistryAccess reg, ItemStack output) {
        if (rm == null || output == null || output.isEmpty()) return RecipeView.EMPTY;

        List<ItemStack> vanilla = findVanilla(rm, reg, output);
        if (!vanilla.isEmpty()) return new RecipeView(vanilla, List.of());

        return findGT(rm, output);
    }

    private static List<ItemStack> findVanilla(RecipeManager rm, RegistryAccess reg, ItemStack output) {
        try {
            for (CraftingRecipe recipe : rm.getAllRecipesFor(RecipeType.CRAFTING)) {
                ItemStack result = recipe.getResultItem(reg);
                if (result == null || result.isEmpty()) continue;
                if (!ItemStack.isSameItem(result, output)) continue;
                return collectVanilla(recipe);
            }
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("RecipeReader: vanilla recipe lookup failed", t);
        }
        return List.of();
    }

    private static List<ItemStack> collectVanilla(CraftingRecipe recipe) {
        Map<String, ItemStack> merged = new LinkedHashMap<>();
        for (Ingredient ing : recipe.getIngredients()) {
            if (ing == null || ing.isEmpty()) continue;
            ItemStack[] items = ing.getItems();
            if (items.length == 0) continue;
            ItemStack rep = items[0];
            if (rep.isEmpty()) continue;
            String key = itemKey(rep);
            ItemStack acc = merged.get(key);
            if (acc == null) merged.put(key, withCount(rep, 1));
            else acc.grow(1);
        }
        return new ArrayList<>(merged.values());
    }

    @SuppressWarnings("unchecked")
    private static RecipeView findGT(RecipeManager rm, ItemStack output) {
        ensureGT();
        if (!gtReady) return RecipeView.EMPTY;

        try {
            for (Recipe<?> recipe : rm.getRecipes()) {
                if (!cGTRecipe.isInstance(recipe)) continue;

                Map<Object, List<?>> outputs = (Map<Object, List<?>>) fOutputs.get(recipe);
                if (outputs == null) continue;
                List<?> outContents = outputs.get(itemCap);
                if (outContents == null || outContents.isEmpty()) continue;
                if (!contentsProduce(outContents, output)) continue;

                Map<Object, List<?>> inputs = (Map<Object, List<?>>) fInputs.get(recipe);
                if (inputs == null) continue;

                List<ItemStack> items  = collectGTItems((List<?>) inputs.get(itemCap));
                List<FluidLine> fluids = collectGTFluids(fluidCap == null ? null : (List<?>) inputs.get(fluidCap));
                RecipeView view = new RecipeView(items, fluids);
                if (!view.isEmpty()) return view;
            }
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("RecipeReader: GT recipe lookup failed", t);
        }
        return RecipeView.EMPTY;
    }

    private static boolean contentsProduce(List<?> contents, ItemStack output) throws ReflectiveOperationException {
        for (Object content : contents) {
            Object obj = fContent.get(content);
            if (obj instanceof Ingredient ing) {
                for (ItemStack s : ing.getItems()) {
                    if (ItemStack.isSameItem(s, output)) return true;
                }
            }
        }
        return false;
    }

    private static List<ItemStack> collectGTItems(List<?> contents) throws ReflectiveOperationException {
        if (contents == null) return List.of();
        Map<String, ItemStack> merged = new LinkedHashMap<>();
        for (Object content : contents) {
            Object obj = fContent.get(content);
            if (!(obj instanceof Ingredient ing)) continue;
            ItemStack[] items = ing.getItems();
            if (items.length == 0) continue;
            ItemStack rep = items[0];
            if (rep.isEmpty()) continue;

            int amount = 1;
            if (cSized != null && cSized.isInstance(ing)) {
                try { amount = (int) mGetAmount.invoke(ing); } catch (Throwable ignored) {}
            } else if (rep.getCount() > 0) {
                amount = rep.getCount();
            }
            if (amount <= 0) amount = 1;

            String key = itemKey(rep);
            ItemStack acc = merged.get(key);
            if (acc == null) merged.put(key, withCount(rep, amount));
            else acc.grow(amount);
        }
        return new ArrayList<>(merged.values());
    }

    private static List<FluidLine> collectGTFluids(List<?> contents) {
        if (contents == null || cFluidIng == null) return List.of();
        Map<String, FluidLine> merged = new LinkedHashMap<>();
        try {
            for (Object content : contents) {
                Object obj = fContent.get(content);
                if (!cFluidIng.isInstance(obj)) continue;

                Object stacksObj = mFluidStacks.invoke(obj);
                if (!(stacksObj instanceof FluidStack[] stacks) || stacks.length == 0) continue;
                FluidStack fs = stacks[0];
                if (fs == null || fs.isEmpty()) continue;

                long amount;
                try { amount = ((Number) mFluidAmount.invoke(obj)).longValue(); }
                catch (Throwable ignored) { amount = fs.getAmount(); }
                if (amount <= 0) amount = fs.getAmount();

                String name = fs.getDisplayName().getString();
                FluidLine existing = merged.get(name);
                merged.put(name, existing == null
                        ? new FluidLine(name, amount)
                        : new FluidLine(name, existing.amount() + amount));
            }
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("RecipeReader: GT fluid lookup failed", t);
        }
        return new ArrayList<>(merged.values());
    }

    private static String itemKey(ItemStack s) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }

    private static ItemStack withCount(ItemStack s, int count) {
        ItemStack copy = s.copy();
        copy.setCount(Math.max(1, count));
        return copy;
    }
}
