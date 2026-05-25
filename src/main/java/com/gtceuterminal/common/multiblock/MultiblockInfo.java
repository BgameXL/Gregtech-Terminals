package com.gtceuterminal.common.multiblock;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import net.minecraft.core.BlockPos;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a detected multiblock structure in the world, containing information about its controller, components, status, and other metadata.
 * This is the main data structure used by the Multiblock Scanner and related UI components to display multiblock information to the player.
 */
public class MultiblockInfo {
    private final IMultiController controller;
    private final String name;
    private final BlockPos controllerPos;
    private final int tier;
    private final double distanceFromPlayer;
    private final boolean isFormed;
    private final List<ComponentInfo> components;
    private MultiblockStatus status;

    private String sourceMod = "gtceu";
    private String customDisplayName = null;
    private Set<BlockPos> allBlockPositions = null;

    public MultiblockInfo(
            IMultiController controller,
            String name,
            BlockPos controllerPos,
            int tier,
            double distanceFromPlayer,
            boolean isFormed
    ) {
        this.controller = controller;
        this.name = name;
        this.controllerPos = controllerPos;
        this.tier = tier;
        this.distanceFromPlayer = distanceFromPlayer;
        this.isFormed = isFormed;
        this.components = new ArrayList<>();
        this.status = MultiblockStatus.IDLE;
    }

    public IMultiController getController() {
        return controller;
    }

    public String getName() {
        return customDisplayName != null && !customDisplayName.isBlank()
                ? customDisplayName : name;
    }

    public String getMachineTypeName() {
        return name;
    }

    public void setCustomDisplayName(String name) {
        this.customDisplayName = name;
    }

    public String getCustomDisplayName() {
        return customDisplayName != null ? customDisplayName : "";
    }

    public Set<BlockPos> getAllBlockPositions() {
        return allBlockPositions != null ? allBlockPositions : Collections.emptySet();
    }

    public void setAllBlockPositions(Set<BlockPos> positions) {
        this.allBlockPositions = positions;
    }

    public String posKey(String dimensionId) {
        return dimensionId + ":" + controllerPos.getX() + "," + controllerPos.getY() + "," + controllerPos.getZ();
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public int getTier() {
        return tier;
    }

    public double getDistanceFromPlayer() {
        return distanceFromPlayer;
    }

    public boolean isFormed() {
        return isFormed;
    }

    public List<ComponentInfo> getComponents() {
        return components;
    }

    public List<ComponentInfoGroup> getGroupedComponents() {
        java.util.Map<String, ComponentInfoGroup> groups = new java.util.LinkedHashMap<>();
        for (ComponentInfo comp : components) {
            if (!comp.getGroup().isUpgradeable) continue;
            String key = ComponentInfoGroup.getGroupKey(comp.getGroup(), comp.getTier(), comp.getBlockName());
            groups.computeIfAbsent(key, k -> new ComponentInfoGroup(comp.getGroup(), comp.getTier(), comp.getBlockName()))
                    .addComponent(comp);
        }
        List<ComponentInfoGroup> result = new ArrayList<>(groups.values());
        result.sort((a, b) -> {
            int tc = a.getGroup().displayName.compareTo(b.getGroup().displayName);
            if (tc != 0) return tc;
            int nc = a.getBlockName().compareTo(b.getBlockName());
            if (nc != 0) return nc;
            return Integer.compare(a.getTier(), b.getTier());
        });
        return result;
    }

    public void addComponent(ComponentInfo component) {
        components.add(component);
    }

    public MultiblockStatus getStatus() {
        return status;
    }

    public void setStatus(MultiblockStatus status) {
        this.status = status;
    }

    public String getTierName() {
        if (tier < 0 || tier >= com.gregtechceu.gtceu.api.GTValues.VN.length) {
            return "Unknown";
        }
        return com.gregtechceu.gtceu.api.GTValues.VN[tier].toUpperCase(java.util.Locale.ROOT);
    }

    public String getDistanceString() {
        if (Double.isNaN(distanceFromPlayer) || Double.isInfinite(distanceFromPlayer)) {
            return "?m";
        }

        if (distanceFromPlayer < 1000.0) {
            return String.format(java.util.Locale.ROOT, "%.1fm", distanceFromPlayer);
        }
        return String.format(java.util.Locale.ROOT, "%.0fm", distanceFromPlayer);
    }

    public List<ComponentInfo> getComponentsByGroup(ComponentGroup group) {
        return components.stream().filter(c -> c.getGroup() == group).toList();
    }

    public List<ComponentInfo> getUpgradeableComponents() {
        return components.stream().filter(c -> c.getGroup().isUpgradeable).toList();
    }

    public int countComponentsOfGroup(ComponentGroup group) {
        return (int) components.stream().filter(c -> c.getGroup() == group).count();
    }

    // MOD SOURCE TRACKING
    public void setSourceMod(String modId) {
        this.sourceMod = modId != null ? modId : "unknown";
    }

    public String getSourceMod() {
        return sourceMod;
    }

    public boolean isFromMod(String modId) {
        return this.sourceMod.equals(modId);
    }

    public boolean isVanillaGTCEu() {
        return "gtceu".equals(sourceMod);
    }

    public String getDisplayNameWithMod() {
        if ("gtceu".equals(sourceMod)) {
            return getName();
        }
        return "[" + sourceMod.toUpperCase() + "] " + getName();
    }

    public int getModColor() {
        return switch (sourceMod) {
            case "gtceu" -> 0xFFFFFF;
            case "monifactory" -> 0x00FF00;
            case "terrafirmagreg" -> 0x00FFFF;
            case "phoenix's technologies" -> 0xFFAA00;
            case "astrogreg:exsilium" -> 0xFF00FF;
            default -> 0xAAAAAA;
        };
    }

    @Override
    public String toString() {
        return "MultiblockInfo{" +
                "name='" + name + '\'' +
                ", tier=" + getTierName() +
                ", distance=" + getDistanceString() +
                ", formed=" + isFormed +
                ", components=" + components.size() +
                ", status=" + status +
                ", sourceMod='" + sourceMod + '\'' +
                '}';
    }

    public String getType() {
        return "";
    }
}