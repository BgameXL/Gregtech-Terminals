package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.config.ComponentEntry;
import com.gtceuterminal.common.config.ComponentRegistry;
import com.gtceuterminal.common.multiblock.ComponentInfo;
import com.gtceuterminal.common.upgrade.UniversalUpgradeCatalog;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class UpgradeCandidates {

    private UpgradeCandidates() {
    }

    public record UpgradeOption(String blockId, String displayName, int tier) {
    }

    public static List<UpgradeOption> universalCandidates(
            ComponentInfo rep,
            Integer filterTier,
            UniversalUpgradeCatalog catalog
    ) {
        if (catalog == null) return List.of();
        var opt = catalog.get(rep.getPosition());
        if (opt == null || opt.candidates() == null || opt.candidates().isEmpty()) return List.of();

        String currentId = blockId(rep);
        List<UpgradeOption> list = new ArrayList<>();
        for (var c : opt.candidates()) {
            if (c == null || c.blockId() == null) continue;
            if (currentId != null && currentId.equalsIgnoreCase(c.blockId())) continue;
            if (filterTier != null && c.tier() != filterTier) continue;
            list.add(new UpgradeOption(c.blockId(), resolveBlockName(c.blockId(), 48), c.tier()));
        }

        list.sort(Comparator.comparingInt(UpgradeOption::tier)
                .thenComparing(UpgradeOption::blockId, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public static List<UpgradeOption> maintenanceCandidates(ComponentInfo rep) {
        String currentId = blockId(rep);
        List<UpgradeOption> list = new ArrayList<>();
        for (ComponentEntry e : ComponentRegistry.getMaintenanceHatches()) {
            if (e == null || e.blockId == null) continue;
            if (currentId != null && currentId.equalsIgnoreCase(e.blockId)) continue;
            String name = trimSuffixes(resolveBlockName(e.blockId, 48));
            if (name.isBlank()) name = e.displayName != null ? e.displayName : e.blockId;
            list.add(new UpgradeOption(e.blockId, name, e.tier));
        }
        list.sort(Comparator.<UpgradeOption>comparingInt(UpgradeOption::tier)
                .thenComparing(UpgradeOption::displayName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public static List<UpgradeOption> coilCandidates(ComponentInfo rep) {
        String currentId = blockId(rep);
        List<UpgradeOption> list = new ArrayList<>();
        for (var entry : ComponentRegistry.getCoils()) {
            if (entry == null || entry.blockId == null) continue;
            if (currentId != null && currentId.equalsIgnoreCase(entry.blockId)) continue;

            String name = entry.displayName;
            if (name != null && name.startsWith("block.")) {
                name = trimSuffixes(Component.translatable(name).getString());
            }
            if (name == null || name.isBlank()) name = entry.blockId;
            list.add(new UpgradeOption(entry.blockId, name, entry.tier));
        }
        list.sort(Comparator.<UpgradeOption>comparingInt(UpgradeOption::tier)
                .thenComparing(UpgradeOption::displayName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public static String blockId(ComponentInfo rep) {
        try {
            return rep.getState().getBlock().builtInRegistryHolder().key().location().toString();
        } catch (IllegalStateException e) {
            GTCEUTerminalMod.LOGGER.debug("UpgradeCandidates: could not get block id: {}", e.getMessage());
            return null;
        }
    }

    static String resolveBlockName(String blockId, int maxLen) {
        String display = blockId;
        try {
            Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(blockId));
            if (b != null) {
                String loc = Component.translatable(b.getDescriptionId()).getString();
                if (loc != null && !loc.isBlank()) display = loc;
            }
        } catch (IllegalArgumentException e) {
            GTCEUTerminalMod.LOGGER.debug("UpgradeCandidates: could not resolve '{}': {}", blockId, e.getMessage());
        }
        return display.length() > maxLen ? display.substring(0, maxLen - 1) + "…" : display;
    }

    public static String tierName(int tier) {
        String[] vn = GTValues.VN;
        if (tier >= 0 && tier < vn.length) {
            String s = vn[tier];
            if (s != null && !s.isBlank()) return s;
        }
        return "T" + tier;
    }

    static String trimSuffixes(String s) {
        if (s == null) return "";
        return s.replace(" Coil Block", "").replace(" Heating Coil", "")
                .replace(" Maintenance Hatch", " Maintenance").trim();
    }
}