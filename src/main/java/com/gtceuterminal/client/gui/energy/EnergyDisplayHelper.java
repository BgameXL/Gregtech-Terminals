package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.common.energy.EnergySnapshot;

import net.minecraft.network.chat.Component;

import java.math.BigInteger;

// Static display/formatting helpers for EnergyAnalyzerUI and related dialogs.
public final class EnergyDisplayHelper {

    private EnergyDisplayHelper() {}

    public static String storedStr(EnergySnapshot s) {
        if (s.usesBigInt)
            return formatBigEU(s.bigStored) + " / " + formatBigEU(s.bigCapacity);
        return formatEU(s.energyStored) + " / " + formatEU(s.energyCapacity);
    }

    public static String formatEU(long eu) {
        if (eu >= 1_000_000_000L) return String.format("%.2fGEU", eu / 1_000_000_000.0);
        if (eu >= 1_000_000L)     return String.format("%.2fMEU", eu / 1_000_000.0);
        if (eu >= 1_000L)         return String.format("%.2fkEU", eu / 1_000.0);
        return eu + " EU";
    }

    public static String formatBigEU(BigInteger eu) {
        if (eu == null) return "?";
        BigInteger ZETTA = BigInteger.TEN.pow(21);
        BigInteger EXA   = BigInteger.TEN.pow(18);
        BigInteger PETA  = BigInteger.TEN.pow(15);
        BigInteger TERA  = BigInteger.TEN.pow(12);
        if (eu.compareTo(ZETTA) >= 0) return String.format("%.2f ZEU", eu.doubleValue() / 1e21);
        if (eu.compareTo(EXA)   >= 0) return String.format("%.2f EEU", eu.doubleValue() / 1e18);
        if (eu.compareTo(PETA)  >= 0) return String.format("%.2f PEU", eu.doubleValue() / 1e15);
        if (eu.compareTo(TERA)  >= 0) return String.format("%.2f TEU", eu.doubleValue() / 1e12);
        return formatEU(eu.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue());
    }

    public static String formatTime(long secs) {
        if (secs >= 3600) return String.format("%dh %dm", secs / 3600, (secs % 3600) / 60);
        if (secs >= 60)   return String.format("%dm %ds", secs / 60, secs % 60);
        return secs + "s";
    }

    public static String getVoltageTier(long v) {
        long[]   t = {8,32,128,512,2048,8192,32768,131072,524288,2097152,8388608};
        String[] n = {"ULV","LV","MV","HV","EV","IV","LuV","ZPM","UV","UHV","UEV"};
        for (int i = t.length - 1; i >= 0; i--) if (v >= t[i]) return n[i];
        return "ULV";
    }

    public static String modeBadge(EnergySnapshot s) {
        if (!s.isFormed) return Component.translatable("gui.gtceuterminal.energy_analyzer.mode.not_formed").getString();
        return switch (s.mode) {
            case CONSUMER  -> Component.translatable("gui.gtceuterminal.energy_analyzer.mode.consumer").getString();
            case GENERATOR -> Component.translatable("gui.gtceuterminal.energy_analyzer.mode.generator").getString();
            case STORAGE   -> Component.translatable("gui.gtceuterminal.energy_analyzer.mode.storage").getString();
            default        -> Component.translatable("gui.gtceuterminal.energy_analyzer.mode.unknown").getString();
        };
    }

    public static int statusColor(EnergySnapshot s, int colorRed, int colorOrange, int colorGreen) {
        if (s == null || !s.isFormed) return colorRed;
        if (s.chargePercent() < 0.1f) return colorOrange;
        return colorGreen;
    }

    public static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
