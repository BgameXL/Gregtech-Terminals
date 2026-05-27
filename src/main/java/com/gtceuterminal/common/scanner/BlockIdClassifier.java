package com.gtceuterminal.common.scanner;

import com.gtceuterminal.common.compat.integrations.StarTCoreIntegration;
import com.gtceuterminal.common.config.ComponentEntry;
import com.gtceuterminal.common.config.ComponentRegistry;
import com.gtceuterminal.common.multiblock.ComponentGroup;
import com.gtceuterminal.common.multiblock.ComponentGroupRegistry;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

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
            String amp = detectAmperage(id);
            String prefix = amp != null ? amp + " " : "";
            if (id.contains("output") || id.contains("dynamo")) return prefix + "Dynamo Hatch";
            if (id.contains("input"))                           return prefix + "Energy Hatch";
        }
        if (id.contains("dynamo")) {
            String amp = detectAmperage(id);
            return (amp != null ? amp + " " : "") + "Dynamo Hatch";
        }
        if (id.contains("casing")) return "Casing";
        return null;
    }

    @Nullable
    static UniversalMultiblockScanner.ComponentData identifyStructureBlock(
            BlockState blockState, BlockPos pos, Level level) {

        String blockId      = blockState.getBlock().builtInRegistryHolder().key().location().toString();
        String blockIdLower = blockId.toLowerCase();

        if (blockIdLower.contains("coil")) {
            boolean isHeatingCoil = GTCEuAPI.HEATING_COILS.values().stream()
                    .anyMatch(s -> s.get() == blockState.getBlock());
            if (isHeatingCoil) {
                return new UniversalMultiblockScanner.ComponentData(
                        "COIL", blockState.getBlock().getName().getString(), detectCoilTier(blockId), pos);
            }
        }

        if (blockIdLower.contains("casing")) {
            return new UniversalMultiblockScanner.ComponentData(
                    "CASING", blockState.getBlock().getName().getString(), 0, pos);
        }

        if (ModList.get().isLoaded(StarTCoreIntegration.MOD_ID)) {
            try {
                Class<?> reflectorClass = Class.forName("com.startechnology.start_core.block.FusionReflectorBlock");
                if (reflectorClass.isInstance(blockState.getBlock())) {
                    Object reflectorType = reflectorClass.getField("reflectorType").get(blockState.getBlock());
                    int tier = (int) reflectorType.getClass().getMethod("getTier").invoke(reflectorType);
                    return new UniversalMultiblockScanner.ComponentData(
                            StarTCoreIntegration.FUSION_REFLECTORS,
                            blockState.getBlock().getName().getString(), tier, pos);
                }
            } catch (Exception ignored) {}
        }

        ComponentGroup group = ComponentGroupRegistry.detectFromBlock(blockState.getBlock());
        if (group != ComponentGroupRegistry.UNKNOWN) {
            String category = group.registryCategory != null ? group.registryCategory : group.id;
            ComponentEntry entry = ComponentRegistry.first(category, e -> e.blockId.equalsIgnoreCase(blockId));
            int tier    = entry != null ? entry.tier : 0;
            String name = entry != null ? entry.displayName : blockState.getBlock().getName().getString();
            return new UniversalMultiblockScanner.ComponentData(group.id, name, tier, pos);
        }

        if (blockState.getBlock() instanceof MetaMachineBlock) return null;

        return new UniversalMultiblockScanner.ComponentData(
                "casing", blockState.getBlock().getName().getString(), 0, pos);
    }

    static int detectCoilTier(String blockId) {
        String lower = blockId.toLowerCase();
        if (lower.contains("cupronickel"))                              return 0;
        if (lower.contains("kanthal"))                                  return 1;
        if (lower.contains("nichrome"))                                 return 2;
        if (lower.contains("rtm_alloy") || lower.contains("rtmalloy")) return 3;
        if (lower.contains("hss_g")     || lower.contains("hssg"))     return 4;
        if (lower.contains("naquadah")  && !lower.contains("enriched")) return 5;
        if (lower.contains("trinium"))                                  return 6;
        if (lower.contains("tritanium"))                                return 7;
        return 0;
    }
}
