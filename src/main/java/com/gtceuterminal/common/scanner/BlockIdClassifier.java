package com.gtceuterminal.common.scanner;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;


final class BlockIdClassifier {

    private BlockIdClassifier() {}

    @Nullable
    static String detectAmperage(String id) {
        if (id.contains("_65536a")) return "65536A";
        if (id.contains("_16384a")) return "16384A";
        if (id.contains("_4096a"))  return "4096A";
        if (id.contains("_1024a"))  return "1024A";
        if (id.contains("_256a"))   return "256A";
        if (id.contains("_64a"))    return "64A";
        if (id.contains("_16a"))    return "16A";
        if (id.contains("_4a"))     return "4A";
        return null;
    }

    @Nullable
    static String detectByImprovedAnalysis(String id) {
        if (id.contains("wireless")) {
            if (id.contains("energy")) {
                if (id.contains("input"))  return "Wireless Energy Hatch (Input)";
                if (id.contains("output")) return "Wireless Energy Hatch (Output)";
                return "Wireless Energy Hatch";
            }
            if (id.contains("laser")) {
                if (id.contains("target")) return "Wireless Laser Target Hatch";
                if (id.contains("source")) return "Wireless Laser Source Hatch";
                return "Wireless Laser Hatch";
            }
        }

        if (id.contains("substation")) {
            if (id.contains("input"))  return "Substation Input Energy Hatch";
            if (id.contains("output")) return "Substation Output Energy Hatch";
        }

        if (id.contains("energy")) {
            String amperage = detectAmperage(id);
            String prefix = amperage != null ? amperage + " " : "";
            if (id.contains("output") || id.contains("dynamo")) return prefix + "Dynamo Hatch";
            if (id.contains("input"))                           return prefix + "Energy Hatch";
        }

        if (id.contains("dynamo")) {
            String amperage = detectAmperage(id);
            return (amperage != null ? amperage + " " : "") + "Dynamo Hatch";
        }

        if (id.contains("casing")) return "Casing";

        return null;
    }

    static String getBaseComponentType(String id) {
        if (id.contains("energy") && id.contains("input"))  return "Energy Hatch";
        if (id.contains("energy") && id.contains("output")) return "Dynamo Hatch";
        if (id.contains("dynamo"))                          return "Dynamo Hatch";
        return "Component";
    }

    @Nullable
    static UniversalMultiblockScanner.ComponentData identifyStructureBlock(
            BlockState blockState, BlockPos pos, Level level) {

        String blockId      = blockState.getBlock().builtInRegistryHolder().key().location().toString();
        String blockIdLower = blockId.toLowerCase();


        if (blockIdLower.contains("coil")) {
            boolean isHeatingCoil = com.gregtechceu.gtceu.api.GTCEuAPI.HEATING_COILS.values().stream()
                    .anyMatch(s -> s.get() == blockState.getBlock());
            if (isHeatingCoil) {
                int tier    = detectCoilTier(blockId);
                String name = blockState.getBlock().getName().getString();
                return new UniversalMultiblockScanner.ComponentData("COIL", name, tier, pos);
            }
        }

        if (blockIdLower.contains("casing")) {
            return new UniversalMultiblockScanner.ComponentData(
                    "CASING", blockState.getBlock().getName().getString(), 0, pos);
        }

        com.gtceuterminal.common.multiblock.ComponentGroup group =
                com.gtceuterminal.common.multiblock.ComponentGroupRegistry.detectFromBlock(blockState.getBlock());
        if (group != com.gtceuterminal.common.multiblock.ComponentGroupRegistry.UNKNOWN) {
            String category = group.registryCategory != null ? group.registryCategory : group.id;
            com.gtceuterminal.common.config.ComponentEntry entry =
                    com.gtceuterminal.common.config.ComponentRegistry.first(
                            category, e -> e.blockId.equalsIgnoreCase(blockId));
            int tier    = entry != null ? entry.tier : 0;
            String name = entry != null ? entry.displayName : blockState.getBlock().getName().getString();
            return new UniversalMultiblockScanner.ComponentData(group.id, name, tier, pos);
        }

        return null;
    }

    static int detectCoilTier(String blockId) {
        String lower = blockId.toLowerCase();
        if (lower.contains("cupronickel"))                              return 0;
        if (lower.contains("kanthal"))                                  return 1;
        if (lower.contains("nichrome"))                                 return 2;
        if (lower.contains("rtm_alloy") || lower.contains("rtmalloy")) return 3;
        if (lower.contains("hss_g") || lower.contains("hssg"))         return 4;
        if (lower.contains("naquadah") && !lower.contains("enriched")) return 5;
        if (lower.contains("trinium"))                                  return 6;
        if (lower.contains("tritanium"))                                return 7;
        return 0;
    }
}