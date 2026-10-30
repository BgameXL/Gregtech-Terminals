package com.gtceuterminal.common.theme;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

// Represents the theme configuration stored in the terminal item. This is what the user edits in the Theme Editor.
public class ItemTheme {

    // Defaults
    public static final int DEFAULT_ACCENT = 0xFF2E75B6;
    public static final int DEFAULT_BG = 0xFF1A1A1A;
    public static final int DEFAULT_PANEL = 0xFF252525;
    public static final int DEFAULT_TEXT = 0xFFFFFFFF;
    public static final int DEFAULT_TEXT_MUTED = 0xFFAAAAAA;
    public static final int DEFAULT_BORDER = DEFAULT_ACCENT;
    public static final int DEFAULT_SUCCESS = 0xFF57C07A;
    public static final int DEFAULT_WARNING = 0xFFD8A43C;
    public static final int DEFAULT_ERROR = 0xFFD05656;
    // NBT keys
    private static final String TAG_ROOT = "Theme";
    private static final String TAG_ACCENT = "accentColor";
    private static final String TAG_BG = "bgColor";
    private static final String TAG_PANEL = "panelColor";
    private static final String TAG_TEXT = "textColor";
    private static final String TAG_TEXT_MUTED = "textMutedColor";
    private static final String TAG_BORDER = "borderColor";
    private static final String TAG_SUCCESS = "successColor";
    private static final String TAG_WARNING = "warningColor";
    private static final String TAG_ERROR = "errorColor";
    private static final String TAG_COMPACT = "compactMode";
    private static final String TAG_TOOLTIPS = "showTooltips";
    private static final String TAG_BORDERS = "showBorders";
    private static final String TAG_WALLPAPER = "wallpaper";
    private static final String TAG_STYLE = "uiStyle";
    private static final String TAG_BUNDLE = "bundleId";
    private static final String TAG_SLIDESHOW = "slideshowMode";
    private static final String TAG_SLIDESHOW_SRC = "slideshowSource";
    public int accentColor;
    public int bgColor;
    public int panelColor;
    public int textColor;
    public int textMutedColor;
    public int borderColor;
    public int successColor;
    public int warningColor;
    public int errorColor;
    public boolean compactMode;
    public boolean showTooltips;
    public boolean showBorders;
    public String wallpaper;
    public UiStyle uiStyle;
    public String bundleId;
    public boolean slideshowMode;
    public SlideshowSource slideshowSource;
    public ItemTheme() {
        accentColor = DEFAULT_ACCENT;
        bgColor = DEFAULT_BG;
        panelColor = DEFAULT_PANEL;
        textColor = DEFAULT_TEXT;
        textMutedColor = DEFAULT_TEXT_MUTED;
        borderColor = DEFAULT_BORDER;
        successColor = DEFAULT_SUCCESS;
        warningColor = DEFAULT_WARNING;
        errorColor = DEFAULT_ERROR;
        compactMode = false;
        showTooltips = true;
        showBorders = true;
        wallpaper = "";
        uiStyle = UiStyle.DARK;
        bundleId = "";
        slideshowMode = false;
        slideshowSource = SlideshowSource.BUILTIN;
    }
    public ItemTheme(ItemTheme other) {
        accentColor = other.accentColor;
        bgColor = other.bgColor;
        panelColor = other.panelColor;
        textColor = other.textColor;
        textMutedColor = other.textMutedColor;
        borderColor = other.borderColor;
        successColor = other.successColor;
        warningColor = other.warningColor;
        errorColor = other.errorColor;
        compactMode = other.compactMode;
        showTooltips = other.showTooltips;
        showBorders = other.showBorders;
        wallpaper = other.wallpaper;
        uiStyle = other.uiStyle;
        bundleId = other.bundleId != null ? other.bundleId : "";
        slideshowMode = other.slideshowMode;
        slideshowSource = other.slideshowSource != null ? other.slideshowSource : SlideshowSource.BUILTIN;
    }

    public static ItemTheme fromNBT(CompoundTag tag) {
        ItemTheme t = new ItemTheme();
        if (tag == null) return t;
        if (tag.contains(TAG_ACCENT)) t.accentColor = tag.getInt(TAG_ACCENT);
        if (tag.contains(TAG_BG)) t.bgColor = tag.getInt(TAG_BG);
        if (tag.contains(TAG_PANEL)) t.panelColor = tag.getInt(TAG_PANEL);
        if (tag.contains(TAG_TEXT)) t.textColor = tag.getInt(TAG_TEXT);
        if (tag.contains(TAG_TEXT_MUTED)) t.textMutedColor = tag.getInt(TAG_TEXT_MUTED);
        if (tag.contains(TAG_BORDER)) t.borderColor = tag.getInt(TAG_BORDER);
        if (tag.contains(TAG_SUCCESS)) t.successColor = tag.getInt(TAG_SUCCESS);
        if (tag.contains(TAG_WARNING)) t.warningColor = tag.getInt(TAG_WARNING);
        if (tag.contains(TAG_ERROR)) t.errorColor = tag.getInt(TAG_ERROR);
        if (tag.contains(TAG_COMPACT)) t.compactMode = tag.getBoolean(TAG_COMPACT);
        if (tag.contains(TAG_TOOLTIPS)) t.showTooltips = tag.getBoolean(TAG_TOOLTIPS);
        if (tag.contains(TAG_BORDERS)) t.showBorders = tag.getBoolean(TAG_BORDERS);
        if (tag.contains(TAG_WALLPAPER)) t.wallpaper = tag.getString(TAG_WALLPAPER);
        if (tag.contains(TAG_BUNDLE)) t.bundleId = tag.getString(TAG_BUNDLE);
        if (tag.contains(TAG_SLIDESHOW)) t.slideshowMode = tag.getBoolean(TAG_SLIDESHOW);
        if (tag.contains(TAG_SLIDESHOW_SRC)) {
            try {
                t.slideshowSource = SlideshowSource.valueOf(tag.getString(TAG_SLIDESHOW_SRC));
            } catch (IllegalArgumentException e) {
                t.slideshowSource = SlideshowSource.BUILTIN;
            }
        }
        if (tag.contains(TAG_STYLE)) {
            try {
                t.uiStyle = UiStyle.valueOf(tag.getString(TAG_STYLE));
            } catch (IllegalArgumentException e) {
                t.uiStyle = UiStyle.DARK;
            }
        }
        return t;
    }

    public static ItemTheme load(ItemStack stack) {
        if (stack.isEmpty()) return DefaultThemeConfig.get();
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(TAG_ROOT)) return DefaultThemeConfig.get();
        return fromNBT(root.getCompound(TAG_ROOT));
    }

    public static ItemTheme loadFromPlayer(net.minecraft.world.entity.player.Player player) {
        if (player == null) return DefaultThemeConfig.get();

        for (ItemStack stack : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            CompoundTag root = stack.getTag();
            if (root != null && root.contains(TAG_ROOT)) return fromNBT(root.getCompound(TAG_ROOT));
        }

        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            CompoundTag root = stack.getTag();
            if (root == null || !root.contains(TAG_ROOT)) continue;
            return fromNBT(root.getCompound(TAG_ROOT));
        }

        return DefaultThemeConfig.get();
    }

    public static void save(ItemStack stack, ItemTheme theme) {
        if (stack.isEmpty()) return;
        stack.getOrCreateTag().put(TAG_ROOT, theme.toNBT());
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_ACCENT, accentColor);
        tag.putInt(TAG_BG, bgColor);
        tag.putInt(TAG_PANEL, panelColor);
        tag.putInt(TAG_TEXT, textColor);
        tag.putInt(TAG_TEXT_MUTED, textMutedColor);
        tag.putInt(TAG_BORDER, borderColor);
        tag.putInt(TAG_SUCCESS, successColor);
        tag.putInt(TAG_WARNING, warningColor);
        tag.putInt(TAG_ERROR, errorColor);
        tag.putBoolean(TAG_COMPACT, compactMode);
        tag.putBoolean(TAG_TOOLTIPS, showTooltips);
        tag.putBoolean(TAG_BORDERS, showBorders);
        tag.putString(TAG_WALLPAPER, wallpaper != null ? wallpaper : "");
        tag.putString(TAG_STYLE, uiStyle != null ? uiStyle.name() : UiStyle.DARK.name());
        tag.putString(TAG_BUNDLE, bundleId != null ? bundleId : "");
        tag.putBoolean(TAG_SLIDESHOW, slideshowMode);
        tag.putString(TAG_SLIDESHOW_SRC, slideshowSource != null ? slideshowSource.name() : SlideshowSource.BUILTIN.name());
        return tag;
    }

    public boolean isNativeStyle() {
        return uiStyle == UiStyle.GTCEU_NATIVE;
    }

    public boolean isBundleStyle() {
        return uiStyle == UiStyle.BUNDLE && bundleId != null && !bundleId.isBlank();
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @javax.annotation.Nullable
    private com.gtceuterminal.common.theme.bundle.ThemeBundle activeBundle() {
        if (!isBundleStyle()) return null;
        return com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(bundleId);
    }

    public IGuiTexture backgroundTexture() {
        if (isNativeStyle()) return GuiTextures.BACKGROUND;
        com.gtceuterminal.common.theme.bundle.ThemeBundle b = activeBundle();
        if (b != null) return b.resolvedBackground();
        return new ColorRectTexture(bgColor);
    }

    public IGuiTexture panelTexture() {
        if (isNativeStyle()) return GuiTextures.DISPLAY;
        com.gtceuterminal.common.theme.bundle.ThemeBundle b = activeBundle();
        if (b != null) return b.resolvedPanel();
        return new ColorRectTexture(panelColor);
    }

    public IGuiTexture slotTexture() {
        if (isNativeStyle()) return GuiTextures.SLOT;
        com.gtceuterminal.common.theme.bundle.ThemeBundle b = activeBundle();
        if (b != null) return b.resolvedSlot();
        return new com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture(
                "ldlib:textures/gui/button_common.png", 198, 18, 1, 1);
    }

    public IGuiTexture buttonTexture() {
        if (isNativeStyle()) return GuiTextures.BUTTON;
        com.gtceuterminal.common.theme.bundle.ThemeBundle b = activeBundle();
        if (b != null) return b.resolvedButton();
        return new GuiTextureGroup(
                new ColorRectTexture(panelColor),
                new ColorBorderTexture(1, borderColor));
    }

    public IGuiTexture headerTexture() {
        if (isNativeStyle()) return GuiTextures.TITLE_BAR_BACKGROUND;
        com.gtceuterminal.common.theme.bundle.ThemeBundle b = activeBundle();
        if (b != null) return b.resolvedHeader();
        return new ColorRectTexture(panelColor);
    }

    public IGuiTexture borderTexture() {
        if (isNativeStyle()) return IGuiTexture.EMPTY;
        com.gtceuterminal.common.theme.bundle.ThemeBundle b = activeBundle();
        if (b != null) return b.resolvedBorder();
        return new ColorBorderTexture(1, borderColor);
    }

    public IGuiTexture panelWithBorderTexture() {
        if (isNativeStyle()) return GuiTextures.DISPLAY;
        com.gtceuterminal.common.theme.bundle.ThemeBundle b = activeBundle();
        if (b != null) return b.resolvedPanelWithBorder();
        return new GuiTextureGroup(
                new ColorRectTexture(panelColor),
                new ColorBorderTexture(1, borderColor));
    }

    public int labelColor() {
        if (isNativeStyle()) return 0xFFDDDDDD;
        com.gtceuterminal.common.theme.bundle.ThemeBundle b = activeBundle();
        if (b != null) return b.textColor();
        return textColor;
    }

    public int dimColor() {
        return textMutedColor;
    }

    public int borderColor() {
        if (isNativeStyle()) return 0x00000000;
        return borderColor;
    }

    public int bgFillColor() {
        if (isNativeStyle()) return 0x00000000;
        return bgColor;
    }

    public IGuiTexture modularUIBackground() {
        return isNativeStyle()
                ? GuiTextures.BACKGROUND
                : new ColorRectTexture(0x90000000);
    }

    public int accent(int alpha) {
        return (accentColor & 0x00FFFFFF) | (alpha << 24);
    }

    public int panelDark() {
        int r = Math.max(0, ((bgColor >> 16) & 0xFF) - 10);
        int g = Math.max(0, ((bgColor >> 8) & 0xFF) - 10);
        int b = Math.max(0, (bgColor & 0xFF) - 10);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public int hoverOverlay() {
        return accent(0x40);
    }

    public boolean hasWallpaper() {
        return wallpaper != null && !wallpaper.isBlank();
    }

    public enum UiStyle {
        DARK("Dark"),
        GTCEU_NATIVE("GTCEu"),
        BUNDLE("Bundle");

        public final String label;

        UiStyle(String label) {
            this.label = label;
        }
    }

    public enum SlideshowSource {
        BUILTIN("Built-in"),
        CUSTOM("Custom");

        public final String label;

        SlideshowSource(String label) {
            this.label = label;
        }
    }
}
