package com.gtceuterminal.common.multiblock;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class ComponentInfo {
    private final ComponentType type;
    private final int tier;
    private final BlockPos position;
    private final BlockState state;
    private final String blockName;
    private final String amperage;

    public ComponentInfo(ComponentType type, int tier, BlockPos position, BlockState state) {
        this.type = type;
        this.tier = tier;
        this.position = position;
        this.state = state;
        this.blockName = state.getBlock().builtInRegistryHolder().key().location().getPath();
        this.amperage = detectAmperageFromBlockName(this.blockName);
    }

    public ComponentType getType() {
        return type;
    }

    public int getTier() {
        return tier;
    }

    public BlockPos getPosition() {
        return position;
    }

    public BlockState getState() {
        return state;
    }

    public String getBlockName() {
        return blockName;
    }

    private int getMaxTierForType() {
        if (type == ComponentType.COIL) {
            return 7;
        }

        if (type == ComponentType.MAINTENANCE) {
            return 4;
        }

        return GTValues.VN.length - 1;
    }

    public String getTierName() {
        if (type == ComponentType.COIL) {
            com.gtceuterminal.common.config.ComponentEntry coilEntry = com.gtceuterminal.common.config.ComponentRegistry.coilByTier(tier);
            String coilName = coilEntry != null ? coilEntry.displayName : "Unknown Coil";
            if (coilName != null && !coilName.isEmpty()) {
                return coilName.replace(" Coil", "").trim();
            }
            return "Unknown Coil";
        }

        if (tier < 0 || tier >= GTValues.VN.length) {
            return "Unknown";
        }
        return GTValues.VN[tier].toUpperCase(java.util.Locale.ROOT);
    }

    public List<Integer> getPossibleUpgradeTiers() {
        List<Integer> tiers = new ArrayList<>();
        int maxTier = getMaxTierForType();
        for (int i = 0; i <= maxTier; i++) {
            if (i != tier) {
                tiers.add(i);
            }
        }

        return tiers;
    }

    public boolean canUpgradeTo(int targetTier) {
        int maxTier = getMaxTierForType();
        return targetTier != tier && targetTier >= 0 && targetTier <= maxTier;
    }

    public String getDisplayName() {
        if (blockName.contains("rtm_alloy")) {
            return blockName.replace("rtm_alloy", "RTM Alloy")
                    .replace("_coil_block", " Coil Block")
                    .replace("_", " ");
        }
        if (blockName.contains("hss_g")) {
            return blockName.replace("hss_g", "HSS-G")
                    .replace("_coil_block", " Coil Block")
                    .replace("_", " ");
        }

        String[] parts = blockName.split("_");
        StringBuilder display = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (display.length() > 0) {
                display.append(" ");
            }

            if (!part.isEmpty()) {
                if (isTierName(part)) {
                    display.append(part.toUpperCase(java.util.Locale.ROOT));
                } else {
                    display.append(Character.toUpperCase(part.charAt(0)));
                    if (part.length() > 1) {
                        display.append(part.substring(1));
                    }
                }
            }
        }

        return display.toString();
    }

    private boolean isTierName(String s) {
        String lower = s.toLowerCase(java.util.Locale.ROOT);
        return lower.equals("ulv") || lower.equals("lv") || lower.equals("mv") ||
                lower.equals("hv") || lower.equals("ev") || lower.equals("iv") ||
                lower.equals("luv") || lower.equals("zpm") || lower.equals("uv") ||
                lower.equals("uhv") || lower.equals("uev") || lower.equals("uiv") ||
                lower.equals("uxv") || lower.equals("opv") || lower.equals("max");
    }

    @Override
    public String toString() {
        return "ComponentInfo{" +
                "type=" + type +
                ", tier=" + getTierName() +
                ", position=" + position +
                '}';
    }

    public Object getPos() {
        return position;
    }

    private static String detectAmperageFromBlockName(String blockName) {
        if (blockName == null) return null;

        String lower = blockName.toLowerCase();

        if (lower.contains("_16a") || lower.endsWith("_16a")) return "16A";
        if (lower.contains("_8a") || lower.endsWith("_8a")) return "8A";
        if (lower.contains("_4a") || lower.endsWith("_4a")) return "4A";
        if (lower.contains("_2a") || lower.endsWith("_2a")) return "2A";

        if (lower.contains("_65536a_") || lower.contains("_65536a")) return "65536A";
        if (lower.contains("_16384a_") || lower.contains("_16384a")) return "16384A";
        if (lower.contains("_4096a_") || lower.contains("_4096a")) return "4096A";
        if (lower.contains("_1024a_") || lower.contains("_1024a")) return "1024A";
        if (lower.contains("_256a_") || lower.contains("_256a")) return "256A";
        if (lower.contains("_64a_") || lower.contains("_64a")) return "64A";

        if (lower.contains("substation")) return "64A";

        if (lower.contains("wireless")) {
            if (lower.contains("_16a") || lower.endsWith("_16a")) return "16A";
            if (lower.contains("_8a") || lower.endsWith("_8a")) return "8A";
            if (lower.contains("_4a") || lower.endsWith("_4a")) return "4A";
            if (lower.contains("_2a") || lower.endsWith("_2a")) return "2A";
        }

        return null;
    }
    public String getAmperage() {
        return amperage;
    }
}