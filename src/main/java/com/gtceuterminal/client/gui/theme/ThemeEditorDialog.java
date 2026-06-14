package com.gtceuterminal.client.gui.theme;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.widget.RGBSliderWidget;
import com.gtceuterminal.client.gui.widget.TerminalButton;
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
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class ThemeEditorDialog {

    private static final int HDR_H = 24;
    private static final int FTR_H = 30;
    private static final int PAD   = 8;

    private static final int COL_L = ThemeEditorLeftCol.COL_W;

    private static final int C_BG     = 0xFF1C1C1C;
    private static final int C_PANEL  = 0xFF272727;
    private static final int C_BORDER = 0xFF3A3A3A;
    private static final int C_SEL    = 0xFF2A1A4A;
    private static final int C_SEL_BR = 0xFF7A3FBF;
    private static final int C_SAVE   = 0xFF1E6B1E;
    private static final int C_RESET  = 0xFF6B1E1E;

    private final WidgetGroup rootGroup;
    private final ItemTheme   working;

    private String       activeBundleId;
    private List<String> wallpapers;
    private int          wallpaperIdx;

    private final int[] editTargetHolder = {0};
    private final int[] channels         = new int[4];

    private ImageWidget[]     swatches    = new ImageWidget[3];
    private ImageWidget[]     tabBgs      = new ImageWidget[3];
    private TextFieldWidget[] hexFieldRef = new TextFieldWidget[1];
    private ImageWidget[] wallThumbRef  = new ImageWidget[1];
    private LabelWidget[] wallLabelRef  = new LabelWidget[1];
    private ImageWidget[] animToggleBg  = new ImageWidget[1];
    private ImageWidget[] bundleCardBgs;

    private int dw, dh;

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

        this.dw = rootGroup.getSize().width;
        this.dh = rootGroup.getSize().height;

        WidgetGroup panel = new WidgetGroup(0, 0, dw, dh);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(C_BG), new ColorBorderTexture(1, C_BORDER)));

        panel.addWidget(buildHeader(dialog));

        int contentY = HDR_H + PAD;
        int contentH = dh - contentY - FTR_H - PAD;
        int divX     = PAD + COL_L;
        int colRx    = divX + 1 + PAD;
        int colRw    = dw - colRx - PAD;

        List<ThemeBundle> bundles = ThemeBundleRegistry.all().stream()
                .filter(ThemeBundle::isAvailable)
                .collect(Collectors.toList());
        bundleCardBgs = new ImageWidget[bundles.size()];

        panel.addWidget(ThemeEditorLeftCol.build(
                PAD, contentY, contentH,
                activeBundleId,
                this::applyPreset,
                this::applyBundle,
                bundleCardBgs));

        panel.addWidget(new ImageWidget(divX, contentY, 1, contentH,
                new ColorRectTexture(C_BORDER)));

        RGBSliderWidget[] sliders = new RGBSliderWidget[4];
        panel.addWidget(ThemeEditorRightCol.build(
                colRx, contentY, colRw, contentH,
                working, editTargetHolder, channels,
                this::onChannelOrTabChanged,
                sliders, swatches, tabBgs,
                this::onHexEntered, hexFieldRef,
                this::cycleParade,
                animToggleBg,
                this::toggleSource,
                () -> navigateWallpaper(-1),
                () -> navigateWallpaper(+1),
                wallLabelRef,
                wallThumbRef));

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
        close.setButtonTexture(TerminalButton.iconTex("close"));
        close.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        close.setHoverTexture(new ColorRectTexture(0x33FF0000));
        g.addWidget(close);
        return g;
    }

    private WidgetGroup buildFooter(int w, int h, DialogWidget dialog) {
        WidgetGroup ftr = new WidgetGroup(0, h - FTR_H, w, FTR_H);
        ftr.setBackground(new ColorRectTexture(C_PANEL));

        int btnH   = 18, btnY = (FTR_H - btnH) / 2;
        int totalW = w - PAD * 2;
        int resetW = totalW / 3;
        int saveW  = totalW - resetW - PAD;

        ButtonWidget save = new ButtonWidget(PAD, btnY, saveW, btnH,
                new GuiTextureGroup(new ColorRectTexture(C_SAVE), new ColorBorderTexture(1, 0xFF2E8B2E)),
                cd -> saveAndClose(dialog));
        save.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.theme_editor.button.save").getString())
                .setWidth(saveW).setType(TextTexture.TextType.NORMAL));
        save.setHoverTexture(new ColorRectTexture(0x2200FF00));
        ftr.addWidget(save);

        ButtonWidget reset = new ButtonWidget(PAD + saveW + PAD, btnY, resetW, btnH,
                new GuiTextureGroup(new ColorRectTexture(C_RESET), new ColorBorderTexture(1, 0xFF8B2E2E)),
                cd -> resetDefaults());
        reset.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.theme_editor.button.reset").getString())
                .setWidth(resetW).setType(TextTexture.TextType.NORMAL));
        reset.setHoverTexture(new ColorRectTexture(0x22FF0000));
        ftr.addWidget(reset);

        return ftr;
    }

    private void applyBundle(ThemeBundle bundle) {
        bundle.applyTo(working);
        working.slideshowMode = true; // bundles drive the wallpaper slideshow automatically
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

    private void onHexEntered(String hex) {
        if (hex == null) return;
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        if (s.length() != 6) return;
        int rgb;
        try { rgb = Integer.parseInt(s, 16); }
        catch (NumberFormatException e) { return; }
        channels[0] = (rgb >> 16) & 0xFF;
        channels[1] = (rgb >>  8) & 0xFF;
        channels[2] =  rgb        & 0xFF;
        applyChannels();
        refreshAll();
    }

    private void cycleParade() {
        working.paradeMode = switch (working.paradeMode) {
            case NONE     -> ItemTheme.ParadeMode.ORBITAL;
            case ORBITAL  -> ItemTheme.ParadeMode.BOUNCING;
            case BOUNCING -> ItemTheme.ParadeMode.NONE;
        };
        ThemeEditorRightCol.refreshAnimToggle(animToggleBg,
                working.paradeMode != ItemTheme.ParadeMode.NONE);
    }

    private void toggleSource() {
        working.slideshowSource = (working.slideshowSource == ItemTheme.SlideshowSource.CUSTOM)
                ? ItemTheme.SlideshowSource.BUILTIN
                : ItemTheme.SlideshowSource.CUSTOM;
    }

    private void navigateWallpaper(int delta) {
        if (wallpapers == null) return;
        int n = wallpapers.size();
        wallpaperIdx = Math.floorMod((wallpaperIdx + 1) + delta, n + 1) - 1;
        working.wallpaper = (wallpaperIdx < 0 || n == 0) ? "" : wallpapers.get(wallpaperIdx);
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
        net.minecraft.client.Minecraft.getInstance().setScreen(null);
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
        working.paradeMode      = ItemTheme.ParadeMode.NONE;
        working.slideshowMode   = false;
        working.slideshowSource = ItemTheme.SlideshowSource.BUILTIN;
        activeBundleId          = "";
        wallpaperIdx            = -1;
        syncChannels(currentColor());
        refreshAll();
        refreshWallpaperThumb();
    }

    private void applyChannels() {
        int color = (channels[3] << 24) | (channels[0] << 16) | (channels[1] << 8) | channels[2];
        switch (editTargetHolder[0]) {
            case 1  -> working.bgColor    = color;
            case 2  -> working.panelColor = color;
            default -> working.accentColor = color;
        }
        if (working.isBundleStyle()) {
            working.uiStyle       = ItemTheme.UiStyle.DARK;
            working.bundleId      = "";
            working.slideshowMode = false;
            activeBundleId        = "";
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
        refreshTabHighlights();
        refreshHexField();
        ThemeEditorRightCol.refreshAnimToggle(animToggleBg, working.paradeMode != ItemTheme.ParadeMode.NONE);
        ThemeEditorLeftCol.refreshBundleCards(bundleCardBgs, activeBundleId);
    }

    private void refreshHexField() {
        if (hexFieldRef[0] != null)
            hexFieldRef[0].setCurrentString(String.format("%06X", currentColor() & 0xFFFFFF));
    }

    private void refreshSwatches() {
        if (swatches[0] != null) swatches[0].setImage(new GuiTextureGroup(
                new ColorRectTexture(working.bgColor), new ColorBorderTexture(1, 0xFF555555)));
        if (swatches[1] != null) swatches[1].setImage(new GuiTextureGroup(
                new ColorRectTexture(working.panelColor), new ColorBorderTexture(1, 0xFF555555)));
        if (swatches[2] != null) swatches[2].setImage(new GuiTextureGroup(
                new ColorRectTexture(working.accentColor), new ColorBorderTexture(1, 0xFF555555)));
    }

    private void refreshTabHighlights() {
        for (int i = 0; i < 3; i++) {
            if (tabBgs[i] == null) continue;
            boolean sel = i == editTargetHolder[0];
            tabBgs[i].setImage(new GuiTextureGroup(
                    new ColorRectTexture(sel ? C_SEL : C_PANEL),
                    new ColorBorderTexture(1, sel ? C_SEL_BR : C_BORDER)));
        }
    }

    private void refreshWallpaperThumb() {
        ImageWidget thumb = wallThumbRef[0];
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