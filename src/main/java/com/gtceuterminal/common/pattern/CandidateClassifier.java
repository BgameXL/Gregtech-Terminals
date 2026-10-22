package com.gtceuterminal.common.pattern;

import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

final class CandidateClassifier {

    private CandidateClassifier() {
    }

    static List<CandidateGroup> classify(TraceabilityPredicate predicate) {
        List<CandidateGroup> groups = new ArrayList<>();
        if (predicate == null) return groups;

        if (predicate.limited != null) {
            for (SimplePredicate sp : predicate.limited) {
                if (sp == null) continue;
                Role role = (sp.minCount > 0 || sp.minLayerCount > 0) ? Role.REQUIRED : Role.OPTIONAL;
                groups.add(new CandidateGroup(role, sp, candidatesOf(sp)));
            }
        }

        if (predicate.common != null) {
            for (SimplePredicate sp : predicate.common) {
                if (sp == null) continue;
                groups.add(new CandidateGroup(Role.STRUCTURAL, sp, candidatesOf(sp)));
            }
        }

        return groups;
    }

    private static List<ItemStack> candidatesOf(SimplePredicate sp) {
        List<ItemStack> out = new ArrayList<>();
        if (sp.candidates == null) return out;
        BlockInfo[] infos = sp.candidates.get();
        if (infos == null) return out;
        for (BlockInfo info : infos) {
            if (info == null) continue;
            BlockState state = info.getBlockState();
            if (state == null || state.isAir()) continue;
            ItemStack stack = info.getItemStackForm();
            if (stack != null && !stack.isEmpty()) out.add(stack);
        }
        return out;
    }

    enum Role {
        STRUCTURAL,
        REQUIRED,
        OPTIONAL
    }

    record CandidateGroup(Role role, SimplePredicate predicate, List<ItemStack> candidates) {
    }
}
