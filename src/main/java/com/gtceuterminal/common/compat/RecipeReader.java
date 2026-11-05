package com.gtceuterminal.common.compat;

import com.gtceuterminal.GTCEUTerminalMod;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.fluids.FluidStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public final class RecipeReader {

    private static boolean gtChecked = false;
    private static boolean gtReady = false;
    private static Class<?> cGTRecipe;
    private static Field fInputs;
    private static Field fOutputs;
    private static Field fContent;
    private static Object itemCap;
    private static Object fluidCap;
    private static Class<?> cSized;
    private static Method mGetAmount;
    private static Class<?> cFluidIng;
    private static Method mFluidStacks;
    private static Method mFluidAmount;

    private static RecipeManager indexedManager;
    private static Map<Item, List<Recipe<?>>> outputIndex;

    private RecipeReader() {
    }

    private static synchronized void ensureGT() {
        if (gtChecked) return;
        gtChecked = true;
        try {
            cGTRecipe = Class.forName("com.gregtechceu.gtceu.api.recipe.GTRecipe");
            fInputs = cGTRecipe.getField("inputs");
            fOutputs = cGTRecipe.getField("outputs");

            Class<?> cContent = Class.forName("com.gregtechceu.gtceu.api.recipe.content.Content");
            fContent = cContent.getField("content");

            Class<?> cItemCap = Class.forName("com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability");
            itemCap = cItemCap.getField("CAP").get(null);

            try {
                cSized = Class.forName("com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient");
                mGetAmount = cSized.getMethod("getAmount");
            } catch (Throwable ignored) {
                cSized = null;
                mGetAmount = null;
            }

            try {
                Class<?> cFluidCap = Class.forName("com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability");
                fluidCap = cFluidCap.getField("CAP").get(null);
                cFluidIng = Class.forName("com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient");
                mFluidStacks = cFluidIng.getMethod("getStacks");
                mFluidAmount = cFluidIng.getMethod("getAmount");
            } catch (Throwable ignored) {
                fluidCap = null;
                cFluidIng = null;
                mFluidStacks = null;
                mFluidAmount = null;
            }

            gtReady = true;
        } catch (Throwable t) {
            gtReady = false;
            GTCEUTerminalMod.LOGGER.debug("RecipeReader: GT recipe support unavailable", t);
        }
    }

    public static RecipeView findIngredients(RecipeManager rm, RegistryAccess reg, ItemStack output) {
        if (rm == null || output == null || output.isEmpty()) return RecipeView.EMPTY;

        List<Recipe<?>> candidates = index(rm, reg).get(output.getItem());
        if (candidates == null || candidates.isEmpty()) return RecipeView.EMPTY;

        for (Recipe<?> recipe : candidates) {
            if (recipe instanceof CraftingRecipe crafting) {
                List<ItemStack> items = collectVanilla(crafting);
                if (!items.isEmpty()) return new RecipeView(items, List.of());
            }
        }
        for (Recipe<?> recipe : candidates) {
            if (cGTRecipe != null && cGTRecipe.isInstance(recipe)) {
                RecipeView view = collectGT(recipe);
                if (!view.isEmpty()) return view;
            }
        }
        return RecipeView.EMPTY;
    }

    private static Map<Item, List<Recipe<?>>> index(RecipeManager rm, RegistryAccess reg) {
        if (rm == indexedManager && outputIndex != null) return outputIndex;

        Map<Item, List<Recipe<?>>> index = new HashMap<>();
        try {
            for (CraftingRecipe recipe : rm.getAllRecipesFor(RecipeType.CRAFTING)) {
                ItemStack result = recipe.getResultItem(reg);
                if (result == null || result.isEmpty()) continue;
                index.computeIfAbsent(result.getItem(), k -> new ArrayList<>()).add(recipe);
            }
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("RecipeReader: vanilla recipe index failed", t);
        }

        ensureGT();
        if (gtReady) {
            try {
                for (Recipe<?> recipe : rm.getRecipes()) {
                    if (!cGTRecipe.isInstance(recipe)) continue;
                    for (Item item : gtOutputItems(recipe)) {
                        index.computeIfAbsent(item, k -> new ArrayList<>()).add(recipe);
                    }
                }
            } catch (Throwable t) {
                GTCEUTerminalMod.LOGGER.debug("RecipeReader: GT recipe index failed", t);
            }
        }

        outputIndex = index;
        indexedManager = rm;
        return index;
    }

    @SuppressWarnings("unchecked")
    private static List<Item> gtOutputItems(Recipe<?> recipe) {
        try {
            Map<Object, List<?>> outputs = (Map<Object, List<?>>) fOutputs.get(recipe);
            if (outputs == null) return List.of();
            List<?> contents = outputs.get(itemCap);
            if (contents == null || contents.isEmpty()) return List.of();

            List<Item> items = new ArrayList<>();
            for (Object content : contents) {
                Object obj = fContent.get(content);
                if (obj instanceof Ingredient ing) {
                    for (ItemStack s : ing.getItems()) {
                        if (!s.isEmpty()) items.add(s.getItem());
                    }
                }
            }
            return items;
        } catch (Throwable t) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private static RecipeView collectGT(Recipe<?> recipe) {
        try {
            Map<Object, List<?>> inputs = (Map<Object, List<?>>) fInputs.get(recipe);
            if (inputs == null) return RecipeView.EMPTY;
            List<ItemStack> items = collectGTItems(inputs.get(itemCap));
            List<FluidLine> fluids = collectGTFluids(fluidCap == null ? null : (List<?>) inputs.get(fluidCap));
            return new RecipeView(items, fluids);
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("RecipeReader: GT recipe read failed", t);
            return RecipeView.EMPTY;
        }
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
                try {
                    amount = (int) mGetAmount.invoke(ing);
                } catch (Throwable ignored) {
                }
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
                try {
                    amount = ((Number) mFluidAmount.invoke(obj)).longValue();
                } catch (Throwable ignored) {
                    amount = fs.getAmount();
                }
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

    public record FluidLine(String name, long amount) {
    }

    public record RecipeView(List<ItemStack> items, List<FluidLine> fluids) {
        public static final RecipeView EMPTY = new RecipeView(List.of(), List.of());

        public boolean isEmpty() {
            return items.isEmpty() && fluids.isEmpty();
        }
    }
}
