package com.gtceuterminal.common.multiblock;

import com.gregtechceu.gtceu.api.GTValues;
import com.gtceuterminal.common.config.ComponentRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ComponentInfo {

    private final ComponentGroup group;
    private final int tier;
    private final BlockPos position;
    private final BlockState state;
    private final String blockName;
    private final String amperage;

    public ComponentInfo(ComponentGroup group, int tier, BlockPos position, BlockState state) {
        this.group    = group;
        this.tier     = tier;
        this.position = position;
        this.state    = state;
        this.blockName = state.getBlock().builtInRegistryHolder().key().location().getPath();
        this.amperage  = detectAmperageFromBlockName(this.blockName);
    }

    public ComponentGroup getGroup() { return group; }

    /** Kept for compatibility with code that hasn't been migrated yet. */
    @Deprecated
    public ComponentType getType() {
        return ComponentType.fromGroup(group);
    }

    public int getTier()           { return tier; }
    public BlockPos getPosition()  { return position; }
    public BlockState getState()   { return state; }
    public String getBlockName()   { return blockName; }
    public String getAmperage()    { return amperage; }

    public String getTierName() {
        if (group == ComponentGroupRegistry.COIL) {
            var coilEntry = ComponentRegistry.coilByTier(tier);
            String name = coilEntry != null ? coilEntry.displayName : "Unknown Coil";
            return name.replace(" Coil", "").trim();
        }
        if (tier < 0 || tier >= GTValues.VN.length) return "Unknown";
        return GTValues.VN[tier].toUpperCase(Locale.ROOT);
    }

    public List<Integer> getPossibleUpgradeTiers() {
        List<Integer> tiers = new ArrayList<>();
        int max = group == ComponentGroupRegistry.COIL ? 7
                : group == ComponentGroupRegistry.MAINTENANCE ? 4
                  : GTValues.VN.length - 1;
        for (int i = 0; i <= max; i++) {
            if (i != tier) tiers.add(i);
        }
        return tiers;
    }

    public boolean canUpgradeTo(int targetTier) {
        int max = group == ComponentGroupRegistry.COIL ? 7
                : group == ComponentGroupRegistry.MAINTENANCE ? 4
                  : GTValues.VN.length - 1;
        return targetTier != tier && targetTier >= 0 && targetTier <= max;
    }

    public String getDisplayName() {
        if (blockName.contains("rtm_alloy"))
            return blockName.replace("rtm_alloy", "RTM Alloy").replace("_coil_block", " Coil Block").replace("_", " ");
        if (blockName.contains("hss_g"))
            return blockName.replace("hss_g", "HSS-G").replace("_coil_block", " Coil Block").replace("_", " ");

        String[] parts = blockName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                if (isTierName(part)) sb.append(part.toUpperCase(Locale.ROOT));
                else sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private boolean isTierName(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "ulv","lv","mv","hv","ev","iv","luv","zpm","uv","uhv","uev","uiv","uxv","opv","max" -> true;
            default -> false;
        };
    }

    private static String detectAmperageFromBlockName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("_65536a")) return "65536A";
        if (lower.contains("_16384a")) return "16384A";
        if (lower.contains("_4096a"))  return "4096A";
        if (lower.contains("_1024a"))  return "1024A";
        if (lower.contains("_256a"))   return "256A";
        if (lower.contains("_64a"))    return "64A";
        if (lower.contains("_16a"))    return "16A";
        if (lower.contains("_8a"))     return "8A";
        if (lower.contains("_4a"))     return "4A";
        if (lower.contains("_2a"))     return "2A";
        if (lower.contains("substation")) return "64A";
        return null;
    }

    public Object getPos() { return position; }

    @Override
    public String toString() {
        return "ComponentInfo{group=" + group.id + ", tier=" + getTierName() + ", pos=" + position + '}';
    }
}