package com.gtceuterminal.common.util;

import net.minecraftforge.fml.ModList;

public class MiscUtil {
    public static final boolean isAE2Loaded = isModLoaded("ae2");
    public static final boolean isCuriosLoaded = isModLoaded("curios");
    public static final boolean isStarTCoreLoaded = isModLoaded("start_core");

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
