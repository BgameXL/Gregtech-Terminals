package com.gtceuterminal.client.network;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.autocraft.AutocraftConfirmDialog;
import com.gtceuterminal.client.gui.dismantler.DismantlerUI;
import com.gtceuterminal.client.gui.energy.EnergyAnalyzerUI;
import com.gtceuterminal.client.gui.multiblock.InlineUpgradePanel;
import com.gtceuterminal.client.gui.multiblock.ManagerSettingsUI;
import com.gtceuterminal.client.gui.multiblock.MultiStructureManagerUI;
import com.gtceuterminal.client.gui.schematic.SchematicInterfaceUI;
import com.gtceuterminal.common.autocraft.AnalysisResult;
import com.gtceuterminal.common.energy.EnergySnapshot;
import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.gui.factory.DismantlerItemUIFactory;
import com.gtceuterminal.common.gui.factory.EnergyAnalyzerUIFactory;
import com.gtceuterminal.common.gui.factory.MultiStructureManagerUIFactory;
import com.gtceuterminal.common.gui.factory.SchematicItemUIFactory;
import com.gtceuterminal.common.theme.DefaultThemeConfig;
import com.gtceuterminal.common.theme.ItemTheme;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Set;

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
            Minecraft mc     = Minecraft.getInstance();
            Player    player = mc.player;
            if (player == null) return;

            ModularUI ui = new ModularUI(IUIHolder.EMPTY, player);
            ui.setFullScreen();
            ui.mainGroup.setClientSideWidget();

            new AutocraftConfirmDialog(ui.mainGroup, result, player);

            openClientOnly(ui);
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.error("ClientPacketHandlers: failed to open confirm dialog", e);
        }
    }

    public static void handleUpgradeAvailability(int requestId, AnalysisResult result) {
        InlineUpgradePanel.handleAvailabilityResult(requestId, result);
    }

    public static void handleOpenMultiStructureManager(String mode, ItemStack item) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            MultiStructureManagerUIFactory.Holder holder =
                    new MultiStructureManagerUIFactory.Holder(true, mode, item);
            holder.attach(mc.player);

            ModularUI ui;
            if (MultiStructureManagerUIFactory.MODE_SETTINGS.equals(mode)) {
                ui = new ManagerSettingsUI(holder, mc.player).createUI();
            } else {
                ui = MultiStructureManagerUI.create(holder, mc.player);
            }
            openClientOnly(ui);
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.error("ClientPacketHandlers: failed to open Multi Structure Manager UI", t);
        }
    }

    public static void handleOpenSchematicInterface(ItemStack item) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            SchematicItemUIFactory.Holder holder = new SchematicItemUIFactory.Holder(true, item);
            holder.attach(mc.player);
            openClientOnly(SchematicInterfaceUI.create(holder, mc.player));
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.error("ClientPacketHandlers: failed to open Schematic Interface UI", t);
        }
    }

    public static void handleOpenDismantler(ItemStack item, BlockPos controllerPos, Set<BlockPos> scannedPositions) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            DismantlerItemUIFactory.Holder holder =
                    new DismantlerItemUIFactory.Holder(true, item, controllerPos, scannedPositions);
            holder.attach(mc.player);
            openClientOnly(DismantlerUI.create(holder, mc.player));
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.error("ClientPacketHandlers: failed to open Dismantler UI", t);
        }
    }

    public static void handleOpenEnergyAnalyzer(int initialIndex, List<EnergySnapshot> snapshots,
                                                List<LinkedMachineData> machines, ItemStack itemStack, ItemTheme theme) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            EnergyAnalyzerUIFactory.EnergyAnalyzerHolder holder =
                    new EnergyAnalyzerUIFactory.EnergyAnalyzerHolder(true, itemStack, snapshots, machines, initialIndex, theme);
            holder.attach(mc.player);
            openClientOnly(EnergyAnalyzerUI.create(holder, mc.player));
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.error("ClientPacketHandlers: failed to open Energy Analyzer UI", t);
        }
    }

    private static void openClientOnly(ModularUI ui) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ui == null) return;

        ui.initWidgets();
        int windowId = mc.player.containerMenu != null ? mc.player.containerMenu.containerId : 0;
        ModularUIGuiContainer screen = new ModularUIGuiContainer(ui, windowId);
        mc.setScreen(screen);
        mc.player.containerMenu = screen.getMenu();
    }
}
