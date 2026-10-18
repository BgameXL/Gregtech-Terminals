package com.gtceuterminal.common.pattern;

import com.gtceuterminal.common.config.ManagerSettings.AutoBuildSettings;

import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CandidateSelector {

    private CandidateSelector() {
    }

    public static List<ItemStack> excludeOptionalAllowances(List<ItemStack> candidates, TraceabilityPredicate predicate) {
        if (candidates.isEmpty() || predicate == null) return candidates;

        Set<Item> allowances = new HashSet<>();
        for (CandidateClassifier.CandidateGroup group : CandidateClassifier.classify(predicate)) {
            if (group.role() == CandidateClassifier.Role.REQUIRED) continue;
            if (group.predicate().maxLayerCount > 0) continue;
            if (group.candidates().isEmpty()) continue;
            Set<Item> distinct = new HashSet<>();
            for (ItemStack stack : group.candidates()) distinct.add(stack.getItem());

            boolean anyPart = false;
            for (ItemStack stack : group.candidates()) {
                if (CandidateFilter.isMultiblockPart(stack)) {
                    anyPart = true;
                    break;
                }
            }

            boolean door = group.role() == CandidateClassifier.Role.OPTIONAL && distinct.size() == 1;
            if (door || anyPart) allowances.addAll(distinct);
        }
        if (allowances.isEmpty()) return candidates;

        List<ItemStack> filtered = new ArrayList<>();
        for (ItemStack stack : candidates) {
            if (!allowances.contains(stack.getItem())) filtered.add(stack);
        }
        return filtered.isEmpty() ? candidates : filtered;
    }

    public static List<ItemStack> applyTierPreference(List<ItemStack> candidates, AutoBuildSettings settings) {
        if (candidates.size() <= 1) return candidates;

        int targetTier = settings.tierMode;

        record Tiered(ItemStack stack, int tier, int index) {
        }
        List<Tiered> tiered = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Integer tier = TierResolver.tierOf(candidates.get(i));
            if (tier != null) tiered.add(new Tiered(candidates.get(i), tier, i));
        }
        if (tiered.size() <= 1) return candidates;

        List<Integer> slots = new ArrayList<>(tiered.size());
        for (Tiered t : tiered) slots.add(t.index());

        tiered.sort(Comparator
                .comparingInt((Tiered t) -> t.tier() < 0 ? 1 : 0)
                .thenComparingInt((Tiered t) -> t.tier() < 0 ? 0 : Math.abs(t.tier() - targetTier))
                .thenComparingInt(Tiered::tier));

        List<ItemStack> result = new ArrayList<>(candidates);
        for (int k = 0; k < slots.size(); k++) result.set(slots.get(k), tiered.get(k).stack());
        return result;
    }
}
