package com.gtceuterminal.client.network;

import com.gtceuterminal.client.gui.autocraft.AutocraftConfirmScreen;
import com.gtceuterminal.common.autocraft.AnalysisResult;
import com.gtceuterminal.common.theme.DefaultThemeConfig;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandlers {

    private ClientPacketHandlers() {}

    public static void handleDefaultTheme(int accentColor, int bgColor, int panelColor,
                                          int textColor, boolean compactMode,
                                          boolean showTooltips, boolean showBorders,
                                          String wallpaper) {
        ItemTheme theme = new ItemTheme();
        theme.accentColor  = accentColor;
        theme.bgColor      = bgColor;
        theme.panelColor   = panelColor;
        theme.textColor    = textColor;
        theme.compactMode  = compactMode;
        theme.showTooltips = showTooltips;
        theme.showBorders  = showBorders;
        theme.wallpaper    = wallpaper;
        DefaultThemeConfig.setClientOverride(theme);
    }

    public static void handleAnalysisResult(AnalysisResult result) {
        try {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new AutocraftConfirmScreen(result, mc.screen));
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("ClientPacketHandlers: failed to open confirm screen", e);
        }
    }
}