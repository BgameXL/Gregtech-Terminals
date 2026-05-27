package com.gtceuterminal.common.upgrade;

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;

import net.minecraft.world.level.block.Block;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public final class PartAbilityIntrospector {

    private PartAbilityIntrospector() {}

    // GTCEu tiers go from ULV(0) to MAX(14)
    private static final int MAX_KNOWN_TIER = 14;

    private static volatile List<PartAbility> ALL_KNOWN_ABILITIES;
    private static volatile Map<PartAbility, Map<Integer, Set<Block>>> ABILITY_REGISTRY_CACHE;

    public static List<PartAbility> getAllKnownAbilities() {
        var local = ALL_KNOWN_ABILITIES;
        if (local != null) return local;

        List<PartAbility> out = new ArrayList<>();
        for (Field f : PartAbility.class.getDeclaredFields()) {
            if (f.getType() != PartAbility.class || !Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                PartAbility ability = (PartAbility) f.get(null);
                if (ability != null) out.add(ability);
            } catch (Throwable ignored) {}
        }

        out.sort(Comparator.comparing(PartAbility::getName));
        ALL_KNOWN_ABILITIES = Collections.unmodifiableList(out);
        return ALL_KNOWN_ABILITIES;
    }

    public static Map<PartAbility, Map<Integer, Set<Block>>> getAbilityTierRegistry() {
        var local = ABILITY_REGISTRY_CACHE;
        if (local != null) return local;

        Map<PartAbility, Map<Integer, Set<Block>>> out = new HashMap<>();

        for (PartAbility ability : getAllKnownAbilities()) {
            Map<Integer, Set<Block>> tierMap = new HashMap<>();
            for (int tier = 0; tier <= MAX_KNOWN_TIER; tier++) {
                Collection<Block> blocks = ability.getBlocks(tier);
                if (!blocks.isEmpty()) {
                    tierMap.put(tier, new HashSet<>(blocks));
                }
            }
            if (!tierMap.isEmpty()) out.put(ability, tierMap);
        }

        ABILITY_REGISTRY_CACHE = Collections.unmodifiableMap(out);
        return ABILITY_REGISTRY_CACHE;
    }

    public static Set<PartAbility> detectAbilitiesForBlock(Block block) {
        Set<PartAbility> abilities = new HashSet<>();
        for (PartAbility a : getAllKnownAbilities()) {
            try {
                if (a.isApplicable(block)) abilities.add(a);
            } catch (Throwable ignored) {}
        }
        return abilities;
    }
}
