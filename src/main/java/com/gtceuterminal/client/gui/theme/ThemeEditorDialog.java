package com.gtceuterminal.client.gui.theme;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.widget.RGBSliderWidget;
import com.gtceuterminal.common.network.CPacketSaveTheme;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.common.theme.ThemePreset;
import com.gtceuterminal.common.theme.bundle.ThemeBundle;
import com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ThemeEditorDialog {

    private static final int HDR_H = 24;
    private static final int FTR_H = 30;
    private static final int PAD   = 8;

    private static final int COL_L  = ThemeEditorLeftCol.COL_W;
    private static final int COL_RX = COL_L + PAD * 2;

    private static final int C_BG     = 0xFF1C1C1C;
    private static final int C_PANEL  = 0xFF272727;
    private static final int C_BORDER = 0xFF3A3A3A;
    private static final int C_SAVE   = 0xFF1E6B1E;
    private static final int C_RESET  = 0xFF6B1E1E;

    private final WidgetGroup rootGroup;
    private final ItemTheme   working;

    private String activeBundleId;
    private List<String> wallpapers;
    private int wallpaperIdx;

    private final int[] editTargetHolder = {0};
    private final int[] channels = new int[4];

    private RGBSliderWidget[] sliders   = new RGBSliderWidget[4];
    private ImageWidget[]     swatches  = new ImageWidget[3];
    private ImageWidget[]     tabBgs    = new ImageWidget[3];
    private LabelWidget[]     hexLabel  = new LabelWidget[1];
    private ImageWidget[]     previewRefs = new ImageWidget[5];
    private ImageWidget[]     wallpaperThumbRef = new ImageWidget[1];
    private LabelWidget[]     wallpaperLabelRef = new LabelWidget[1];
    private ImageWidget[]     bundleCardBgs;

    private int dw, dh, colR;

    public static void open(WidgetGroup rootGroup, ItemTheme current) {
        new ThemeEditorDialog(rootGroup, current).buildAndShow();
    }

    private ThemeEditorDialog(WidgetGroup rootGroup, ItemTheme current) {
        this.rootGroup      = rootGroup;
        this.working        = new ItemTheme(current);
        this.activeBundleId = current.isBundleStyle() ? current.bundleId : "";
        this.wallpapers     = WallpaperManager.listWallpapers();
        this.wallpaperIdx   = wallpapers.indexOf(current.wallpaper);
        syncChannels(working.accentColor);
    }

    private void buildAndShow() {
        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(0xB0000000));
        dialog.setClickClose(false);

        this.dw   = rootGroup.getSize().width;
        this.dh   = rootGroup.getSize().height;
        this.colR = dw - COL_RX - PAD;

        WidgetGroup panel = new WidgetGroup(0, 0, dw, dh);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(C_BG), new ColorBorderTexture(1, C_BORDER)));

        panel.addWidget(buildHeader(dialog));

        List<ThemeBundle> bundles = ThemeBundleRegistry.all().stream()
                .filter(ThemeBundle::isAvailable)
                .collect(java.util.stream.Collectors.toList());
        bundleCardBgs = new ImageWidget[bundles.size()];
        panel.addWidget(ThemeBundleBar.build(0, HDR_H, dw, activeBundleId, this::applyBundle, bundleCardBgs));
        panel.addWidget(new ImageWidget(0, HDR_H + ThemeBundleBar.BAR_H - 2, dw, 1, new ColorRectTexture(C_BORDER)));

        int contentY = HDR_H + ThemeBundleBar.BAR_H + 2;
        int scrollH  = dh - contentY - FTR_H;

        DraggableScrollableWidgetGroup scroll =
                new DraggableScrollableWidgetGroup(0, contentY, dw, scrollH);
        scroll.setYScrollBarWidth(6);
        scroll.setYBarStyle(new ColorRectTexture(0xFF1A1A1A), new ColorRectTexture(C_BORDER));

        int colH = scrollH;
        WidgetGroup content = new WidgetGroup(0, 0, dw, colH);

        content.addWidget(ThemeEditorLeftCol.build(
                PAD, PAD, colH - PAD * 2,
                working, wallpapers,
                this::applyPreset,
                () -> navigateWallpaper(-1),
                () -> navigateWallpaper(+1),
                this::clearWallpaper,
                wallpaperThumbRef,
                wallpaperLabelRef,
                previewRefs));

        content.addWidget(ThemeEditorRightCol.build(
                COL_RX, PAD, colR, colH - PAD * 2,
                working, editTargetHolder, channels,
                this::onChannelOrTabChanged,
                sliders, swatches, tabBgs, hexLabel));

        scroll.addWidget(content);
        panel.addWidget(scroll);
        panel.addWidget(buildFooter(dw, dh, dialog));

        dialog.addWidget(panel);
    }

    private WidgetGroup buildHeader(DialogWidget dialog) {
        WidgetGroup g = new WidgetGroup(0, 0, dw, HDR_H);
        g.setBackground(new ColorRectTexture(C_PANEL));
        g.addWidget(new LabelWidget(PAD, (HDR_H - 8) / 2,
                Component.translatable("gui.gtceuterminal.theme_editor.title").getString()));
        ButtonWidget close = new ButtonWidget(dw - 20, (HDR_H - 14) / 2, 14, 14,
                new ColorRectTexture(0x00000000), cd -> dialog.setVisible(false));
        close.setButtonTexture(new TextTexture("§c✖").setWidth(14).setType(TextTexture.TextType.NORMAL));
        close.setHoverTexture(new ColorRectTexture(0x33FF0000));
        g.addWidget(close);
        return g;
    }

    private WidgetGroup buildFooter(int w, int h, DialogWidget dialog) {
        WidgetGroup ftr = new WidgetGroup(0, h - FTR_H, w, FTR_H);
        ftr.setBackground(new ColorRectTexture(C_PANEL));

        int btnW = 80, btnH = 18, btnY = (FTR_H - btnH) / 2;

        ButtonWidget save = new ButtonWidget(w / 2 - btnW - 4, btnY, btnW, btnH,
                new GuiTextureGroup(new ColorRectTexture(C_SAVE), new ColorBorderTexture(1, 0xFF2E8B2E)),
                cd -> saveAndClose(dialog));
        save.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.theme_editor.button.save").getString())
                .setWidth(btnW).setType(TextTexture.TextType.NORMAL));
        save.setHoverTexture(new ColorRectTexture(0x2200FF00));
        ftr.addWidget(save);

        ButtonWidget reset = new ButtonWidget(w / 2 + 4, btnY, btnW, btnH,
                new GuiTextureGroup(new ColorRectTexture(C_RESET), new ColorBorderTexture(1, 0xFF8B2E2E)),
                cd -> resetDefaults());
        reset.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.theme_editor.button.reset").getString())
                .setWidth(btnW).setType(TextTexture.TextType.NORMAL));
        reset.setHoverTexture(new ColorRectTexture(0x22FF0000));
        ftr.addWidget(reset);

        return ftr;
    }

    private void applyBundle(ThemeBundle bundle) {
        bundle.applyTo(working);
        activeBundleId = bundle.id();
        syncChannels(working.accentColor);
        refreshAll();
        GTCEUTerminalMod.LOGGER.debug("ThemeEditorDialog: applied bundle '{}'", bundle.id());
    }

    private void applyPreset(ThemePreset p) {
        p.applyTo(working);
        syncChannels(currentColor());
        refreshAll();
    }

    private void navigateWallpaper(int delta) {
        if (wallpapers == null || wallpapers.isEmpty()) return;
        wallpaperIdx = Math.floorMod(wallpaperIdx + delta, wallpapers.size());
        working.wallpaper = wallpapers.get(wallpaperIdx);
        refreshWallpaperThumb();
    }

    private void clearWallpaper() {
        working.wallpaper = "";
        wallpaperIdx = -1;
        refreshWallpaperThumb();
    }

    private void onChannelOrTabChanged(int signalTarget) {
        if (signalTarget >= 0) {
            syncChannels(currentColor());
        } else {
            applyChannels();
        }
        refreshAll();
    }

    private void saveAndClose(DialogWidget dialog) {
        GTCEUTerminalMod.LOGGER.info("ThemeEditorDialog: saving theme accent=#{} bg=#{} panel=#{}",
                String.format("%06X", working.accentColor & 0xFFFFFF),
                String.format("%06X", working.bgColor     & 0xFFFFFF),
                String.format("%06X", working.panelColor  & 0xFFFFFF));

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            net.minecraft.world.item.ItemStack found = net.minecraft.world.item.ItemStack.EMPTY;
            for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
                net.minecraft.world.item.ItemStack s = mc.player.getItemInHand(hand);
                net.minecraft.resources.ResourceLocation rl =
                        net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem());
                if (rl != null && "gtceuterminal".equals(rl.getNamespace())) { found = s; break; }
            }
            if (found.isEmpty()) {
                for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
                    net.minecraft.world.item.ItemStack s = mc.player.getInventory().getItem(i);
                    net.minecraft.resources.ResourceLocation rl =
                            net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem());
                    if (rl != null && "gtceuterminal".equals(rl.getNamespace())) { found = s; break; }
                }
            }
            if (!found.isEmpty()) {
                ItemTheme.save(found, working);
            } else {
                GTCEUTerminalMod.LOGGER.warn("ThemeEditorDialog: no terminal item found in client inventory!");
            }
        }

        try { TerminalNetwork.CHANNEL.sendToServer(new CPacketSaveTheme(working)); }
        catch (Exception e) { GTCEUTerminalMod.LOGGER.warn("ThemeEditorDialog: failed to send save packet", e); }

        dialog.setVisible(false);
        mc.setScreen(null);
    }

    private void resetDefaults() {
        ItemTheme def = new ItemTheme();
        working.accentColor     = def.accentColor;
        working.bgColor         = def.bgColor;
        working.panelColor      = def.panelColor;
        working.textColor       = def.textColor;
        working.compactMode     = def.compactMode;
        working.showTooltips    = def.showTooltips;
        working.showBorders     = def.showBorders;
        working.wallpaper       = "";
        working.uiStyle         = ItemTheme.UiStyle.DARK;
        working.bundleId        = "";
        working.paradeMode      = ItemTheme.ParadeMode.ORBITAL;
        working.slideshowMode   = false;
        working.slideshowSource = ItemTheme.SlideshowSource.BUILTIN;
        activeBundleId          = "";
        wallpaperIdx            = -1;
        syncChannels(currentColor());
        refreshAll();
    }

    private void applyChannels() {
        int color = (channels[3] << 24) | (channels[0] << 16) | (channels[1] << 8) | channels[2];
        switch (editTargetHolder[0]) {
            case 1  -> working.bgColor    = color;
            case 2  -> working.panelColor = color;
            default -> working.accentColor = color;
        }
        if (working.isBundleStyle()) {
            working.uiStyle  = ItemTheme.UiStyle.DARK;
            working.bundleId = "";
            activeBundleId   = "";
            ThemeBundleBar.refreshCardBgs(bundleCardBgs, activeBundleId);
        }
    }

    private void syncChannels(int color) {
        channels[0] = (color >> 16) & 0xFF;
        channels[1] = (color >>  8) & 0xFF;
        channels[2] =  color        & 0xFF;
        channels[3] = (color >> 24) & 0xFF;
    }

    private int currentColor() {
        return switch (editTargetHolder[0]) {
            case 1  -> working.bgColor;
            case 2  -> working.panelColor;
            default -> working.accentColor;
        };
    }

    private void refreshAll() {
        refreshSwatches();
        refreshPreview();
        refreshTabHighlights();
        ThemeBundleBar.refreshCardBgs(bundleCardBgs, activeBundleId);
    }

    private void refreshSwatches() {
        if (swatches[0] != null) swatches[0].setImage(new ColorRectTexture(working.accentColor));
        if (swatches[1] != null) swatches[1].setImage(new ColorRectTexture(working.bgColor));
        if (swatches[2] != null) swatches[2].setImage(new ColorRectTexture(working.panelColor));
    }

    private void refreshPreview() {
        if (previewRefs[0] != null) previewRefs[0].setImage(new ColorRectTexture(working.bgColor));
        if (previewRefs[1] != null) previewRefs[1].setImage(new ColorRectTexture(working.accentColor | 0xFF000000));
        if (previewRefs[2] != null) previewRefs[2].setImage(new ColorRectTexture(working.panelColor));
        if (previewRefs[3] != null) previewRefs[3].setImage(new ColorRectTexture(working.panelColor));
        if (previewRefs[4] != null) previewRefs[4].setImage(new ColorRectTexture(working.accentColor | 0xFF000000));
    }

    private void refreshTabHighlights() {
        for (int i = 0; i < 3; i++) {
            if (tabBgs[i] != null)
                tabBgs[i].setImage(new ColorRectTexture(i == editTargetHolder[0] ? C_PANEL + 0x202030 : C_PANEL));
        }
    }

    private void refreshWallpaperThumb() {
        ImageWidget thumb = wallpaperThumbRef[0];
        if (thumb == null) return;
        if (working.hasWallpaper()) {
            WallpaperManager.getTexture(working.wallpaper).ifPresentOrElse(
                    rl -> thumb.setImage(new com.lowdragmc.lowdraglib.gui.texture.ResourceTexture(rl.toString())),
                    () -> thumb.setImage(new ColorRectTexture(0xFF0A0A0A)));
        } else {
            thumb.setImage(new ColorRectTexture(0xFF0A0A0A));
        }
    }
}