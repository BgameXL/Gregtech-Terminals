package com.gtceuterminal.common.multiblock;

import net.minecraft.world.level.block.Block;

import java.util.function.Predicate;

public final class ComponentGroup {

    public final String id;
    public final String displayName;
    public final int color;
    public final boolean isUpgradeable;
    public final boolean isFluidHandler;
    public final boolean isItemHandler;
    public final boolean isEnergyHandler;
    public final boolean isDataHandler;
    public final boolean isSpecial;
    public final int capacityMultiplier;
    public final Predicate<Block> detector;
    public final String registryCategory;

    private ComponentGroup(Builder b) {
        this.id                = b.id;
        this.displayName       = b.displayName;
        this.color             = b.color;
        this.isUpgradeable     = b.isUpgradeable;
        this.isFluidHandler    = b.isFluidHandler;
        this.isItemHandler     = b.isItemHandler;
        this.isEnergyHandler   = b.isEnergyHandler;
        this.isDataHandler     = b.isDataHandler;
        this.isSpecial         = b.isSpecial;
        this.capacityMultiplier = b.capacityMultiplier;
        this.detector          = b.detector;
        this.registryCategory  = b.registryCategory;
    }

    public static Builder builder(String id, String displayName) {
        return new Builder(id, displayName);
    }

    public static final class Builder {
        private final String id;
        private final String displayName;
        private int color = 0xFFFFFF;
        private boolean isUpgradeable = true;
        private boolean isFluidHandler, isItemHandler, isEnergyHandler, isDataHandler, isSpecial;
        private int capacityMultiplier = 1;
        private Predicate<Block> detector;
        private String registryCategory;

        private Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder color(int color)                      { this.color = color; return this; }
        public Builder notUpgradeable()                      { this.isUpgradeable = false; return this; }
        public Builder fluidHandler()                        { this.isFluidHandler = true; return this; }
        public Builder itemHandler()                         { this.isItemHandler = true; return this; }
        public Builder energyHandler()                       { this.isEnergyHandler = true; return this; }
        public Builder dataHandler()                         { this.isDataHandler = true; return this; }
        public Builder special()                             { this.isSpecial = true; return this; }
        public Builder capacityMultiplier(int m)             { this.capacityMultiplier = m; return this; }
        public Builder detector(Predicate<Block> detector)   { this.detector = detector; return this; }
        public Builder registryCategory(String cat)          { this.registryCategory = cat; return this; }

        public ComponentGroup build() {
            return new ComponentGroup(this);
        }
    }

    @Override
    public String toString() { return "ComponentGroup{" + id + "}"; }
}