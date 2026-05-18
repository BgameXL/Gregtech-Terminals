package com.gtceuterminal.common.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.registries.ForgeRegistries;

public class LinkedMachineData {

    private BlockPos pos;
    private String dimensionId;
    private String customName;
    private String controllerBlockKey;
    private String legacyTypeDisplay;

    public LinkedMachineData(BlockPos pos, String dimensionId, String customName, String controllerBlockKey) {
        this(pos, dimensionId, customName, controllerBlockKey, "");
    }

    public LinkedMachineData(BlockPos pos, String dimensionId, String customName,
                             String controllerBlockKey, String legacyTypeDisplay) {
        this.pos = pos;
        this.dimensionId = dimensionId;
        this.customName = customName != null ? customName : "";
        this.controllerBlockKey = controllerBlockKey != null ? controllerBlockKey : "";
        this.legacyTypeDisplay = legacyTypeDisplay != null ? legacyTypeDisplay : "";
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        tag.putString("Dim", dimensionId);
        tag.putString("Name", customName);
        tag.putString("BlockKey", controllerBlockKey);
        if (!legacyTypeDisplay.isEmpty()) {
            tag.putString("Type", legacyTypeDisplay);
        }
        return tag;
    }

    public static LinkedMachineData fromNBT(CompoundTag tag) {
        BlockPos pos = new BlockPos(
                tag.getInt("X"),
                tag.getInt("Y"),
                tag.getInt("Z")
        );
        String blockKey = tag.contains("BlockKey") ? tag.getString("BlockKey") : "";
        String legacyType = tag.getString("Type");
        if (blockKey.isEmpty() && !legacyType.isEmpty()) {
            return new LinkedMachineData(
                    pos,
                    tag.getString("Dim"),
                    tag.getString("Name"),
                    "",
                    legacyType
            );
        }
        return new LinkedMachineData(
                pos,
                tag.getString("Dim"),
                tag.getString("Name"),
                blockKey,
                ""
        );
    }

    // Helpers
    public boolean matches(BlockPos otherPos, String otherDim) {
        return pos.equals(otherPos) && dimensionId.equals(otherDim);
    }

    public Component getDisplayNameComponent() {
        if (customName != null && !customName.isBlank()) {
            return Component.literal(customName);
        }
        if (controllerBlockKey != null && !controllerBlockKey.isBlank()) {
            Block b = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(controllerBlockKey));
            if (b != null) {
                return b.getName();
            }
        }
        return Component.literal(legacyTypeDisplay != null ? legacyTypeDisplay : "");
    }

    public String getDisplayName() {
        return getDisplayNameComponent().getString();
    }

    public void applyToSnapshotIdentity(EnergySnapshot snap) {
        snap.machineCustomName = customName != null ? customName : "";
        snap.machineTypeKey = "";
        if (controllerBlockKey != null && !controllerBlockKey.isBlank()) {
            Block b = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(controllerBlockKey));
            if (b != null) {
                snap.machineTypeKey = b.getDescriptionId();
            }
        }
        if (snap.machineTypeKey.isEmpty()
                && (snap.machineCustomName == null || snap.machineCustomName.isBlank())
                && legacyTypeDisplay != null && !legacyTypeDisplay.isBlank()) {
            snap.machineCustomName = legacyTypeDisplay;
        }
    }

    public static String dimId(Level level) {
        return level.dimension().location().toString();
    }

    public BlockPos getPos()          { return pos; }
    public String getDimensionId()    { return dimensionId; }
    public String getCustomName()     { return customName; }
    public String getControllerBlockKey() { return controllerBlockKey; }
    public String getLegacyTypeDisplay() { return legacyTypeDisplay; }
    public void setCustomName(String n) { this.customName = n != null ? n : ""; }
}
