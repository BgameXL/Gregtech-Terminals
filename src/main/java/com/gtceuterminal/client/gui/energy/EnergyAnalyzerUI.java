package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.gui.factory.EnergyAnalyzerUIFactory;
import com.gtceuterminal.client.gui.widget.WallpaperWidget;
import com.gtceuterminal.common.energy.EnergySnapshot;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnergyAnalyzerUI {

    private static final int W         = 600;
    private static final int H         = 480;
    private static final int SIDEBAR_W = 148;
    private static final int DETAIL_X  = SIDEBAR_W + 6;
    private static final int DETAIL_W  = W - DETAIL_X - 6;
    private static final int HEADER_H  = 26;
    private static final int GRAPH_W   = DETAIL_W - 68;
    private static final int GRAPH_H   = 80;
    private static final int PAD       = 6;
    private static final int ITEM_H    = 36;

    private static final int C_RED    = 0xFFFF5555;
    private static final int C_ORANGE = 0xFFFF8800;
    private static final int C_GREEN  = 0xFF55FF55;
    private static final int C_GRAY   = 0xFFAAAAAA;

    private final EnergyAnalyzerUIFactory.EnergyAnalyzerHolder holder;
    private final Player player;
    private final ItemTheme theme;

    private int selectedIndex;
    private WidgetGroup rootGroup;
    private WidgetGroup[] sidebarSelects;
    private EnergyAnalyzerDetail detail;

    private EnergyAnalyzerUI(EnergyAnalyzerUIFactory.EnergyAnalyzerHolder holder, Player player) {
        this.holder        = holder;
        this.player        = player;
        this.selectedIndex = holder.initialIndex;
        this.theme         = holder.theme;
    }

    public static ModularUI create(EnergyAnalyzerUIFactory.EnergyAnalyzerHolder holder, Player player) {
        return new EnergyAnalyzerUI(holder, player).buildUI();
    }

    private ModularUI buildUI() {
        rootGroup = new WidgetGroup(0, 0, W, H);
        rootGroup.setBackground(new ColorRectTexture(0x00000000));
        EnergyUpdateWidget updater = new EnergyUpdateWidget(holder);
        updater.setRebuildCallback(this::onDataRefresh);
        rootGroup.addWidget(updater);

        if (!theme.isNativeStyle()) {
            rootGroup.addWidget(new WallpaperWidget(0, 0, W, H, () -> this.theme));
        }

        rootGroup.addWidget(EnergyAnalyzerHeader.build(
                W, HEADER_H, PAD, holder.machines.size(), holder.itemStack, theme, rootGroup, player));

        WidgetGroup div = new WidgetGroup(SIDEBAR_W + 2, HEADER_H, 2, H - HEADER_H);
        div.setBackground(new ColorRectTexture(theme.isNativeStyle() ? 0x40000000 : theme.accent(0x80)));
        rootGroup.addWidget(div);

        sidebarSelects = new WidgetGroup[holder.machines.size()];
        rootGroup.addWidget(EnergyAnalyzerSidebar.build(
                SIDEBAR_W, HEADER_H, H, PAD, selectedIndex,
                holder.machines, ITEM_H,
                theme.panelColor, theme.accent(0x55), C_GRAY, C_RED, C_ORANGE, C_GREEN,
                this::snapAt,
                this::selectMachine,
                idx -> MachineOptionsDialog.open(rootGroup, W, H, idx, holder.machines.get(idx), player),
                sidebarSelects));

        detail = new EnergyAnalyzerDetail(DETAIL_W, GRAPH_W, GRAPH_H);
        rootGroup.addWidget(detail.build(
                DETAIL_X, HEADER_H, H, PAD,
                this::selSnap,
                () -> { EnergySnapshot s = selSnap(); if (s != null) RecipeHistoryDialog.open(rootGroup, s, W, H); }));

        setupParade();
        return wrapUI(rootGroup);
    }

    private void setupParade() {
        com.gtceuterminal.client.ClientEvents.clearActiveParade();
        if (!theme.isBundleStyle()) return;
        com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
        if (bundle == null) return;
        com.gtceuterminal.client.gui.widget.MultiblockParadeWidget parade = bundle.createParadeWidget(0, 0, W, H);
        if (parade == null || parade.isEmpty()) return;
        parade.setGuiCenter(W / 2f, H / 2f);
        com.gtceuterminal.client.ClientEvents.setActiveParade(parade, theme.paradeMode);
    }

    private void onDataRefresh() {
        detail.updateDynamic(selSnap());
    }

    private void selectMachine(int idx) {
        if (idx == selectedIndex) return;
        selectedIndex = idx;
        if (sidebarSelects != null) {
            for (int i = 0; i < sidebarSelects.length; i++) {
                sidebarSelects[i].setVisible(i == selectedIndex);
            }
        }
        detail.updateDynamic(selSnap());
    }

    private EnergySnapshot selSnap() {
        List<EnergySnapshot> snaps = holder.snapshots;
        if (snaps.isEmpty() || selectedIndex >= snaps.size()) return null;
        return snaps.get(selectedIndex);
    }

    private EnergySnapshot snapAt(int idx) {
        List<EnergySnapshot> snaps = holder.snapshots;
        return idx < snaps.size() ? snaps.get(idx) : null;
    }

    private ModularUI wrapUI(WidgetGroup content) {
        int w = W, h = H;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                w = Math.min(W, mc.getWindow().getGuiScaledWidth()  - 16);
                h = Math.min(H, mc.getWindow().getGuiScaledHeight() - 16);
            }
        } catch (RuntimeException e) {
            GTCEUTerminalMod.LOGGER.warn("EnergyAnalyzerUI: could not read window size: {}", e.getMessage());
        }
        ModularUI ui = new ModularUI(new Size(w, h), holder, player);
        ui.widget(content);
        ui.background(theme.modularUIBackground());
        return ui;
    }
}