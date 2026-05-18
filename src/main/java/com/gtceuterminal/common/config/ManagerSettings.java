package com.gtceuterminal.common.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class ManagerSettings {

    public static class Settings {
        public final int noHatchMode;
        public final int tierMode;
        public final int repeatCount;
        public final int isUseAE;

        public Settings(ItemStack itemStack) {
            CompoundTag tag = itemStack.getTag();
            if (tag != null) {
                this.noHatchMode  = tag.contains("NoHatchMode")  ? tag.getInt("NoHatchMode")  : 0;
                this.tierMode     = tag.contains("TierMode")     ? tag.getInt("TierMode")     : 1;
                this.repeatCount  = tag.contains("RepeatCount")  ? tag.getInt("RepeatCount")  : 0;
                this.isUseAE      = tag.contains("IsUseAE")      ? tag.getInt("IsUseAE")      : 0;
            } else {
                this.noHatchMode  = 0;
                this.tierMode     = 1;
                this.repeatCount  = 0;
                this.isUseAE      = 0;
            }
        }

        public AutoBuildSettings toAutoBuildSettings() {
            AutoBuildSettings s = new AutoBuildSettings();
            s.noHatchMode = this.noHatchMode;
            s.tierMode    = this.tierMode;
            s.repeatCount = this.repeatCount;
            s.isUseAE     = this.isUseAE;
            return s;
        }
    }

    public static class AutoBuildSettings {
        public int repeatCount = 0;
        public int noHatchMode = 0;
        public int tierMode    = 1;
        public int isUseAE     = 0;
    }
}