package com.gtceuterminal.common.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class ManagerSettings {

    public static class Settings {
        public final int noHatchMode;
        public final int tierMode;
        public final int repeatCount;
        public final int isUseAE;
        public final int mirrorMode;
        public final int expandRadius;
        public final int expandHeight;

        public Settings(ItemStack itemStack) {
            CompoundTag tag = itemStack.getTag();
            if (tag == null) tag = new CompoundTag();
            this.noHatchMode = tag.getInt("NoHatchMode");
            this.tierMode = tag.getInt("TierMode");
            this.repeatCount = tag.getInt("RepeatCount");
            this.isUseAE = tag.getInt("IsUseAE");
            this.mirrorMode = tag.getInt("MirrorMode");
            this.expandRadius = tag.getInt("ExpandRadius");
            this.expandHeight = tag.getInt("ExpandHeight");
        }

        public AutoBuildSettings toAutoBuildSettings() {
            AutoBuildSettings s = new AutoBuildSettings();
            s.noHatchMode = this.noHatchMode;
            s.tierMode = this.tierMode;
            s.repeatCount = this.repeatCount;
            s.isUseAE = this.isUseAE;
            s.mirrorMode = this.mirrorMode;
            s.expandRadius = this.expandRadius;
            s.expandHeight = this.expandHeight;
            return s;
        }
    }

    public static class AutoBuildSettings {
        public int repeatCount = 0;
        public int noHatchMode = 0;
        public int tierMode = 0;
        public int isUseAE = 0;
        public int mirrorMode = 0;
        public int expandRadius = 0;
        public int expandHeight = 0;
    }
}
