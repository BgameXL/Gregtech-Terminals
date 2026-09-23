package com.gtceuterminal.client.gui.theme;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.style.Bevel3DTexture;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
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
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class ThemeEditorDialog {

    private static final int HDR_H = 24;
    private static final int FTR_H = 30;
    private static final int PAD = 8;
    private static final int SECTION_H = 30;
    private static final int SECTION_W = 150;

    private static final int COL_L = ThemeEditorLeftCol.COL_W;
    private static final int COLOR_TARGETS = 9;

    private final WidgetGroup rootGroup;
    private final ItemTheme working;

    private String activeBundleId;
    private Section activeSection;
    private List<String> wallpapers;
    private int wallpaperIdx;

    private final int[] editTargetHolder = {0};
    private final int[] channels = new int[4];

    private final ImageWidget[] swatches = new ImageWidget[COLOR_TARGETS];
    private final ImageWidget[] tabBgs = new ImageWidget[COLOR_TARGETS];
    private TextFieldWidget[] hexFieldRef = new TextFieldWidget[1];
    private ImageWidget[] wallThumbRef = new ImageWidget[1];
    private LabelWidget[] wallLabelRef = new LabelWidget[1];
    private ImageWidget[] animToggleBg = new ImageWidget[1];
    private ImageWidget[] bundleCardBgs;
    private final ImageWidget[] sectionCardBgs = new ImageWidget[Section.values().length];

    private WidgetGroup contentHost;

    private int dw, dh;

    public static void open(WidgetGroup rootGroup, ItemTheme current) {
        new ThemeEditorDialog(rootGroup, current).buildAndShow();
    }

    private ThemeEditorDialog(WidgetGroup rootGroup, ItemTheme current) {
        this.rootGroup = rootGroup;
        this.working = new ItemTheme(current);
        this.activeBundleId = current.isBundleStyle() ? current.bundleId : "";
        this.activeSection = current.isBundleStyle() ? Section.BUNDLES : Section.COLORS;
        this.wallpapers = WallpaperManager.listWallpapers();
        this.wallpaperIdx = wallpapers.indexOf(current.wallpaper);
        syncChannels(working.accentColor);
    }

    private void buildAndShow() {
        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(0xB0000000));
        dialog.setClickClose(false);

        this.dw = rootGroup.getSize().width;
        this.dh = rootGroup.getSize().height;

        WidgetGroup panel = new WidgetGroup(0, 0, dw, dh);
        panel.setBackground(new Bevel3DTexture(TerminalUI.tokens().surface()));

        panel.addWidget(buildHeader(dialog));

        List<ThemeBundle> bundles = ThemeBundleRegistry.all().stream()
                .filter(ThemeBundle::isAvailable)
                .collect(Collectors.toList());
        bundleCardBgs = new ImageWidget[bundles.size()];

        int cardsY = HDR_H + 4;
        int bodyY = cardsY + SECTION_H - 1;
        int bodyH = dh - bodyY - FTR_H - 4;
        contentHost = new WidgetGroup(PAD, bodyY, dw - PAD * 2, bodyH);
        contentHost.setBackground(TerminalUI.cardBg());
        panel.addWidget(contentHost);

        panel.addWidget(buildSectionCard(Section.COLORS, PAD, cardsY));
        panel.addWidget(buildSectionCard(Section.BUNDLES, PAD + SECTION_W + 4, cardsY));
        rebuildSection();

        panel.addWidget(buildFooter(dw, dh, dialog));
        dialog.addWidget(panel);
    }

    private WidgetGroup buildHeader(DialogWidget dialog) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup g = new WidgetGroup(0, 0, dw, HDR_H);
        g.setBackground(new Bevel3DTexture(tokens.panel()));
        LabelWidget title = new LabelWidget(PAD, (HDR_H - 8) / 2,
                Component.translatable("gui.gtceuterminal.theme_editor.title").getString());
        title.setTextColor(tokens.textStrong());
        g.addWidget(title);
        ButtonWidget close = TerminalButton.icon3d(dw - 22, 2, 20, "×",
                cd -> dialog.setVisible(false));
        close.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        g.addWidget(close);
        return g;
    }

    private WidgetGroup buildSectionCard(Section section, int x, int y) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup card = new WidgetGroup(x, y, SECTION_W, SECTION_H);
        ImageWidget background = new ImageWidget(0, 0, SECTION_W, SECTION_H,
                sectionCardTexture(section));
        sectionCardBgs[section.ordinal()] = background;
        card.addWidget(background);

        LabelWidget label = new LabelWidget(10, (SECTION_H - 8) / 2,
                section.glyph + " " + Component.translatable(section.translationKey).getString());
        label.setTextColor(tokens.textStrong());
        card.addWidget(label);

        ButtonWidget button = new ButtonWidget(0, 0, SECTION_W, SECTION_H,
                new ColorRectTexture(Palette.CLEAR), cd -> switchSection(section));
        button.setHoverTexture(new ColorRectTexture(tokens.hover()));
        card.addWidget(button);
        return card;
    }

    private void switchSection(Section section) {
        if (section == activeSection) return;
        activeSection = section;
        rebuildSection();
        refreshAll();
    }

    private void rebuildSection() {
        if (contentHost == null) return;
        contentHost.clearAllWidgets();
        clearSectionRefs();

        TerminalTheme tokens = TerminalUI.tokens();
        int contentH = contentHost.getSize().height - PAD * 2;
        int dividerX = PAD + COL_L + PAD / 2;
        int rightX = dividerX + 1 + PAD / 2;
        int rightW = contentHost.getSize().width - rightX - PAD;

        if (activeSection == Section.COLORS) {
            contentHost.addWidget(ThemeEditorLeftCol.buildColorPresets(
                    PAD, PAD, contentH, this::applyPreset));
            contentHost.addWidget(ThemeEditorRightCol.buildColors(
                    rightX, PAD, rightW, contentH,
                    working, editTargetHolder, channels,
                    this::onChannelOrTabChanged,
                    new RGBSliderWidget[4], swatches, tabBgs,
                    this::onHexEntered, hexFieldRef));
        } else {
            contentHost.addWidget(ThemeEditorLeftCol.buildBundles(
                    PAD, PAD, contentH, activeBundleId, this::applyBundle, bundleCardBgs));
            contentHost.addWidget(ThemeEditorRightCol.buildBundleOptions(
                    rightX, PAD, rightW, contentH,
                    working, this::cycleParade, animToggleBg,
                    this::toggleSource,
                    () -> navigateWallpaper(-1),
                    () -> navigateWallpaper(1),
                    wallLabelRef, wallThumbRef));
        }

        contentHost.addWidget(new ImageWidget(dividerX, PAD, 1, contentH,
                new ColorRectTexture(tokens.borderDark())));
        refreshSectionCards();
        refreshWallpaperThumb();
    }

    private void clearSectionRefs() {
        Arrays.fill(swatches, null);
        Arrays.fill(tabBgs, null);
        Arrays.fill(bundleCardBgs, null);
        hexFieldRef[0] = null;
        wallThumbRef[0] = null;
        wallLabelRef[0] = null;
        animToggleBg[0] = null;
    }

    private void refreshSectionCards() {
        for (Section section : Section.values()) {
            ImageWidget background = sectionCardBgs[section.ordinal()];
            if (background != null) background.setImage(sectionCardTexture(section));
        }
    }

    private Bevel3DTexture sectionCardTexture(Section section) {
        TerminalTheme tokens = TerminalUI.tokens();
        return new Bevel3DTexture(section == activeSection ? tokens.panel() : tokens.panelDisabled());
    }

    private WidgetGroup buildFooter(int w, int h, DialogWidget dialog) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup ftr = new WidgetGroup(0, h - FTR_H, w, FTR_H);
        ftr.setBackground(new Bevel3DTexture(tokens.panel()));

        int btnH = 18, btnY = (FTR_H - btnH) / 2;
        int totalW = w - PAD * 2;
        int resetW = totalW / 3;
        int saveW = totalW - resetW - PAD;

        ButtonWidget save = TerminalButton.text3d(PAD, btnY, saveW, btnH,
                Component.translatable("gui.gtceuterminal.theme_editor.button.save").getString(),
                tokens.successSurface(), cd -> saveAndClose(dialog));
        ftr.addWidget(save);

        ButtonWidget reset = TerminalButton.text3d(PAD + saveW + PAD, btnY, resetW, btnH,
                Component.translatable("gui.gtceuterminal.theme_editor.button.reset").getString(),
                tokens.errorSurface(), cd -> resetDefaults());
        ftr.addWidget(reset);

        return ftr;
    }

    private enum Section {
        COLORS("◆", "gui.gtceuterminal.theme_editor.section.colors"),
        BUNDLES("✦", "gui.gtceuterminal.theme_editor.section.bundles");

        private final String glyph;
        private final String translationKey;

        Section(String glyph, String translationKey) {
            this.glyph = glyph;
            this.translationKey = translationKey;
        }
    }

    private void applyBundle(ThemeBundle bundle) {
        bundle.applyTo(working);
        working.slideshowMode = true; // bundles drive the wallpaper slideshow automatically
        activeBundleId = bundle.id();
        syncChannels(currentColor());
        refreshAll();
        GTCEUTerminalMod.LOGGER.debug("ThemeEditorDialog: applied bundle '{}'", bundle.id());
    }

    private void applyPreset(ThemePreset p) {
        p.applyTo(working);
        working.slideshowMode = false;
        activeBundleId = "";
        syncChannels(currentColor());
        refreshAll();
    }

    private void onHexEntered(String hex) {
        if (hex == null) return;
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        if (s.length() != 6) return;
        int rgb;
        try {
            rgb = Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return;
        }
        channels[0] = (rgb >> 16) & 0xFF;
        channels[1] = (rgb >> 8) & 0xFF;
        channels[2] = rgb & 0xFF;
        applyChannels();
        refreshAll();
    }

    private void cycleParade() {
        working.paradeMode = switch (working.paradeMode) {
            case NONE -> ItemTheme.ParadeMode.ORBITAL;
            case ORBITAL -> ItemTheme.ParadeMode.BOUNCING;
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
                String.format("%06X", working.bgColor & 0xFFFFFF),
                String.format("%06X", working.panelColor & 0xFFFFFF));

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            net.minecraft.world.item.ItemStack found = net.minecraft.world.item.ItemStack.EMPTY;
            for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
                net.minecraft.world.item.ItemStack s = mc.player.getItemInHand(hand);
                net.minecraft.resources.ResourceLocation rl =
                        net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem());
                if (rl != null && "gtceuterminal".equals(rl.getNamespace())) {
                    found = s;
                    break;
                }
            }
            if (found.isEmpty()) {
                for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
                    net.minecraft.world.item.ItemStack s = mc.player.getInventory().getItem(i);
                    net.minecraft.resources.ResourceLocation rl =
                            net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem());
                    if (rl != null && "gtceuterminal".equals(rl.getNamespace())) {
                        found = s;
                        break;
                    }
                }
            }
            if (!found.isEmpty()) {
                ItemTheme.save(found, working);
            } else {
                GTCEUTerminalMod.LOGGER.warn("ThemeEditorDialog: no terminal item found in client inventory!");
            }
        }

        try {
            TerminalNetwork.CHANNEL.sendToServer(new CPacketSaveTheme(working));
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("ThemeEditorDialog: failed to send save packet", e);
        }

        dialog.setVisible(false);
        net.minecraft.client.Minecraft.getInstance().setScreen(null);
    }

    private void resetDefaults() {
        ItemTheme def = new ItemTheme();
        working.accentColor = def.accentColor;
        working.bgColor = def.bgColor;
        working.panelColor = def.panelColor;
        working.textColor = def.textColor;
        working.textMutedColor = def.textMutedColor;
        working.borderColor = def.borderColor;
        working.successColor = def.successColor;
        working.warningColor = def.warningColor;
        working.errorColor = def.errorColor;
        working.compactMode = def.compactMode;
        working.showTooltips = def.showTooltips;
        working.showBorders = def.showBorders;
        working.wallpaper = "";
        working.uiStyle = ItemTheme.UiStyle.DARK;
        working.bundleId = "";
        working.paradeMode = ItemTheme.ParadeMode.NONE;
        working.slideshowMode = false;
        working.slideshowSource = ItemTheme.SlideshowSource.BUILTIN;
        activeBundleId = "";
        wallpaperIdx = -1;
        syncChannels(currentColor());
        refreshAll();
        refreshWallpaperThumb();
    }

    private void applyChannels() {
        int color = (channels[3] << 24) | (channels[0] << 16) | (channels[1] << 8) | channels[2];
        switch (editTargetHolder[0]) {
            case 1 -> working.bgColor = color;
            case 2 -> working.panelColor = color;
            case 3 -> working.textColor = color;
            case 4 -> working.textMutedColor = color;
            case 5 -> working.borderColor = color;
            case 6 -> working.successColor = color;
            case 7 -> working.warningColor = color;
            case 8 -> working.errorColor = color;
            default -> working.accentColor = color;
        }
        if (working.isBundleStyle()) {
            working.uiStyle = ItemTheme.UiStyle.DARK;
            working.bundleId = "";
            working.slideshowMode = false;
            activeBundleId = "";
        }
    }

    private void syncChannels(int color) {
        channels[0] = (color >> 16) & 0xFF;
        channels[1] = (color >> 8) & 0xFF;
        channels[2] = color & 0xFF;
        channels[3] = (color >> 24) & 0xFF;
    }

    private int currentColor() {
        return switch (editTargetHolder[0]) {
            case 1 -> working.bgColor;
            case 2 -> working.panelColor;
            case 3 -> working.textColor;
            case 4 -> working.textMutedColor;
            case 5 -> working.borderColor;
            case 6 -> working.successColor;
            case 7 -> working.warningColor;
            case 8 -> working.errorColor;
            default -> working.accentColor;
        };
    }

    private void refreshAll() {
        refreshSectionCards();
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
        TerminalTheme tokens = TerminalUI.tokens();
        int[] colors = {
                working.accentColor, working.bgColor, working.panelColor,
                working.textColor, working.textMutedColor, working.borderColor,
                working.successColor, working.warningColor, working.errorColor
        };
        for (int i = 0; i < COLOR_TARGETS; i++) {
            if (swatches[i] != null) swatches[i].setImage(new GuiTextureGroup(
                    new ColorRectTexture(colors[i]), new ColorBorderTexture(1, tokens.border())));
        }
    }

    private void refreshTabHighlights() {
        for (int i = 0; i < COLOR_TARGETS; i++) {
            if (tabBgs[i] == null) continue;
            boolean sel = i == editTargetHolder[0];
            tabBgs[i].setImage(ThemeEditorRightCol.targetTexture(sel));
        }
    }

    private void refreshWallpaperThumb() {
        ImageWidget thumb = wallThumbRef[0];
        if (thumb == null) return;
        if (working.hasWallpaper()) {
            WallpaperManager.getTexture(working.wallpaper).ifPresentOrElse(
                    rl -> thumb.setImage(new com.lowdragmc.lowdraglib.gui.texture.ResourceTexture(rl.toString())),
                    () -> thumb.setImage(new ColorRectTexture(TerminalUI.tokens().borderDark())));
        } else {
            thumb.setImage(new ColorRectTexture(TerminalUI.tokens().borderDark()));
        }
    }
}
