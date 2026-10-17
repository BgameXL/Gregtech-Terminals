package com.gtceuterminal.common.pattern;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.config.ComponentRegistry;
import com.gtceuterminal.common.config.ComponentRegistry.ComponentCategory;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.block.CoilBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CandidateFilter {

    private CandidateFilter() {
    }

    public static List<ItemStack> filterOutMultiblockParts(List<ItemStack> candidates) {
        if (candidates.isEmpty()) return candidates;

        List<ItemStack> out = new ArrayList<>(candidates.size());
        for (ItemStack stack : candidates) {
            if (!(stack.getItem() instanceof BlockItem)) continue;
            if (isMultiblockPart(stack)) continue;
            out.add(stack);
        }
        return out;
    }

    public static boolean isMultiblockPart(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        if (!(bi.getBlock() instanceof MetaMachineBlock machineBlock)) return false;
        try {
            if (machineBlock.newBlockEntity(BlockPos.ZERO, machineBlock.defaultBlockState())
                    instanceof IMachineBlockEntity mbe) {
                return mbe.getMetaMachine() instanceof MultiblockPartMachine;
            }
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug(
                    "CandidateFilter: could not instantiate block entity for {}: {}",
                    bi.getBlock().getDescriptionId(), t.getMessage());
        }
        return false;
    }

    public static List<ItemStack> enrichWithCustomComponents(
            List<ItemStack> originalCandidates,
            TraceabilityPredicate predicate) {

        if (originalCandidates.isEmpty()) return originalCandidates;

        ComponentCategory category = detectComponentCategory(originalCandidates);
        if (category == null) return originalCandidates;

        List<ItemStack> customComponents = ComponentRegistry.getExternalMatchingItemStacks(category);
        if (customComponents.isEmpty()) return originalCandidates;

        Set<String> existing = new HashSet<>();
        List<ItemStack> enriched = new ArrayList<>(originalCandidates);

        for (ItemStack stack : originalCandidates) {
            existing.add(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
        }

        for (ItemStack stack : customComponents) {
            String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
            if (existing.add(id)) {
                enriched.add(stack);
            }
        }

        int added = enriched.size() - originalCandidates.size();
        if (added > 0) {
            GTCEUTerminalMod.LOGGER.debug("CandidateFilter: enriched {} with {} custom components",
                    category.getDisplayName(), added);
        }
        return enriched;
    }

    static ComponentCategory detectComponentCategory(List<ItemStack> candidates) {
        for (ItemStack stack : candidates) {
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof BlockItem blockItem)) continue;

            Block block = blockItem.getBlock();
            String blockId = ForgeRegistries.BLOCKS.getKey(block).toString().toLowerCase();

            if (blockId.contains("energy") && blockId.contains("hatch")) return ComponentCategory.ENERGY_HATCH;
            if (blockId.contains("fluid") && blockId.contains("hatch")) return ComponentCategory.FLUID_HATCH;
            if (blockId.contains("item") && (blockId.contains("bus") || blockId.contains("hatch")))
                return ComponentCategory.ITEM_HATCH;
            if (blockId.contains("maintenance") && blockId.contains("hatch"))
                return ComponentCategory.MAINTENANCE_HATCH;
            if (blockId.contains("muffler")) return ComponentCategory.MUFFLER_HATCH;
            if (blockId.contains("laser") && blockId.contains("hatch")) return ComponentCategory.LASER_HATCH;
            if (blockId.contains("rotor")) return ComponentCategory.ROTOR_HOLDER;
            if (block instanceof CoilBlock) return ComponentCategory.COIL;
            if (blockId.contains("casing")) return ComponentCategory.CASING;

            if (block instanceof MetaMachineBlock) return ComponentCategory.OTHER;
        }
        return null;
    }
}