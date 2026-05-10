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

    // ── NBT keys ─────────────────────────────────────────────────────────────
    private static final String TAG_ROOT     = "Theme";
    private static final String TAG_ACCENT   = "accentColor";
    private static final String TAG_BG       = "bgColor";
    private static final String TAG_PANEL    = "panelColor";
    private static final String TAG_TEXT     = "textColor";
    private static final String TAG_COMPACT  = "compactMode";
    private static final String TAG_TOOLTIPS = "showTooltips";
    private static final String TAG_BORDERS  = "showBorders";
    private static final String TAG_WALLPAPER= "wallpaper";
    private static final String TAG_STYLE    = "uiStyle";
    private static final String TAG_BUNDLE        = "bundleId";
    private static final String TAG_PARADE_MODE   = "paradeMode";
    private static final String TAG_SLIDESHOW     = "slideshowMode";
    private static final String TAG_SLIDESHOW_SRC = "slideshowSource";

    // ── Defaults ──────────────────────────────────────────────────────────────
    public static final int DEFAULT_ACCENT = 0xFF2E75B6;
    public static final int DEFAULT_BG     = 0xFF1A1A1A;
    public static final int DEFAULT_PANEL  = 0xFF252525;
    public static final int DEFAULT_TEXT   = 0xFFFFFFFF;

    // ── UI Style enum ─────────────────────────────────────────────────────────
    public enum UiStyle {
        DARK("Dark"),
        GTCEU_NATIVE("GTCEu Native"),
        BUNDLE("Bundle");

        public final String label;
        UiStyle(String label) { this.label = label; }
    }

    // ── Parade Mode enum ─────────────────────────────────────────────────────
    public enum ParadeMode {
        ORBITAL("Orbital"),
        BOUNCING("Bouncing"),
        NONE("Off");

        public final String label;
        ParadeMode(String label) { this.label = label; }
    }

    // ── Slideshow Source enum ─────────────────────────────────────────────────
    public enum SlideshowSource {
        BUILTIN("Built-in"),
        CUSTOM("Custom");

        public final String label;
        SlideshowSource(String label) { this.label = label; }
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    public int        accentColor;
    public int        bgColor;
    public int        panelColor;
    public int        textColor;
    public boolean    compactMode;
    public boolean    showTooltips;
    public boolean    showBorders;
    public String     wallpaper;
    public UiStyle    uiStyle;
    public String     bundleId;
    public ParadeMode paradeMode;
    public boolean slideshowMode;
    public SlideshowSource slideshowSource;

    // ── Constructors ──────────────────────────────────────────────────────────
    public ItemTheme() {
        accentColor  = DEFAULT_ACCENT;
        bgColor      = DEFAULT_BG;
        panelColor   = DEFAULT_PANEL;
        textColor    = DEFAULT_TEXT;
        compactMode  = false;
        showTooltips = true;
        showBorders  = true;
        wallpaper    = "";
        uiStyle      = UiStyle.DARK;
        bundleId     = "";
        paradeMode      = ParadeMode.ORBITAL;
        slideshowMode   = false;
        slideshowSource = SlideshowSource.BUILTIN;
    }

    public ItemTheme(ItemTheme other) {
        accentColor  = other.accentColor;
        bgColor      = other.bgColor;
        panelColor   = other.panelColor;
        textColor    = other.textColor;
        compactMode  = other.compactMode;
        showTooltips = other.showTooltips;
        showBorders  = other.showBorders;
        wallpaper    = other.wallpaper;
        uiStyle      = other.uiStyle;
        bundleId     = other.bundleId != null ? other.bundleId : "";
        paradeMode      = other.paradeMode != null ? other.paradeMode : ParadeMode.ORBITAL;
        slideshowMode   = other.slideshowMode;
        slideshowSource = other.slideshowSource != null ? other.slideshowSource : SlideshowSource.BUILTIN;
    }

    // ── NBT ───────────────────────────────────────────────────────────────────
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt    (TAG_ACCENT,    accentColor);
        tag.putInt    (TAG_BG,        bgColor);
        tag.putInt    (TAG_PANEL,     panelColor);
        tag.putInt    (TAG_TEXT,      textColor);
        tag.putBoolean(TAG_COMPACT,   compactMode);
        tag.putBoolean(TAG_TOOLTIPS,  showTooltips);
        tag.putBoolean(TAG_BORDERS,   showBorders);
        tag.putString (TAG_WALLPAPER, wallpaper != null ? wallpaper : "");
        tag.putString (TAG_STYLE,       uiStyle != null ? uiStyle.name() : UiStyle.DARK.name());
        tag.putString (TAG_BUNDLE,      bundleId != null ? bundleId : "");
        tag.putString (TAG_PARADE_MODE,   paradeMode != null ? paradeMode.name() : ParadeMode.ORBITAL.name());
        tag.putBoolean(TAG_SLIDESHOW,     slideshowMode);
        tag.putString (TAG_SLIDESHOW_SRC, slideshowSource != null ? slideshowSource.name() : SlideshowSource.BUILTIN.name());
        return tag;
    }

    public static ItemTheme fromNBT(CompoundTag tag) {
        ItemTheme t = new ItemTheme();
        if (tag == null) return t;
        if (tag.contains(TAG_ACCENT))    t.accentColor  = tag.getInt    (TAG_ACCENT);
        if (tag.contains(TAG_BG))        t.bgColor      = tag.getInt    (TAG_BG);
        if (tag.contains(TAG_PANEL))     t.panelColor   = tag.getInt    (TAG_PANEL);
        if (tag.contains(TAG_TEXT))      t.textColor    = tag.getInt    (TAG_TEXT);
        if (tag.contains(TAG_COMPACT))   t.compactMode  = tag.getBoolean(TAG_COMPACT);
        if (tag.contains(TAG_TOOLTIPS))  t.showTooltips = tag.getBoolean(TAG_TOOLTIPS);
        if (tag.contains(TAG_BORDERS))   t.showBorders  = tag.getBoolean(TAG_BORDERS);
        if (tag.contains(TAG_WALLPAPER)) t.wallpaper    = tag.getString (TAG_WALLPAPER);
        if (tag.contains(TAG_BUNDLE))    t.bundleId     = tag.getString (TAG_BUNDLE);
        if (tag.contains(TAG_PARADE_MODE)) {
            try   { t.paradeMode = ParadeMode.valueOf(tag.getString(TAG_PARADE_MODE)); }
            catch (IllegalArgumentException e) { t.paradeMode = ParadeMode.ORBITAL; }
        }
        if (tag.contains(TAG_SLIDESHOW))     t.slideshowMode = tag.getBoolean(TAG_SLIDESHOW);
        if (tag.contains(TAG_SLIDESHOW_SRC)) {
            try   { t.slideshowSource = SlideshowSource.valueOf(tag.getString(TAG_SLIDESHOW_SRC)); }
            catch (IllegalArgumentException e) { t.slideshowSource = SlideshowSource.BUILTIN; }
        }
        if (tag.contains(TAG_STYLE)) {
            try   { t.uiStyle = UiStyle.valueOf(tag.getString(TAG_STYLE)); }
            catch (IllegalArgumentException e) { t.uiStyle = UiStyle.DARK; }
        }
        return t;
    }

    // ── ItemStack helpers ─────────────────────────────────────────────────────
    public static ItemTheme load(ItemStack stack) {
        if (stack.isEmpty()) return DefaultThemeConfig.get();
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(TAG_ROOT)) return DefaultThemeConfig.get();
        return fromNBT(root.getCompound(TAG_ROOT));
    }

    public static ItemTheme loadFromPlayer(net.minecraft.world.entity.player.Player player) {
        if (player == null) return DefaultThemeConfig.get();

        // Check hands first (most common case)
        for (ItemStack stack : new ItemStack[]{
                player.getMainHandItem(), player.getOffhandItem()}) {
            ItemTheme t = load(stack);
            if (t.uiStyle != UiStyle.DARK || t.bgColor != DEFAULT_BG) return t;
        }

        // Search full inventory
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            CompoundTag root = stack.getTag();
            if (root == null || !root.contains(TAG_ROOT)) continue;
            ItemTheme t = fromNBT(root.getCompound(TAG_ROOT));
            if (t.uiStyle != UiStyle.DARK || t.bgColor != DEFAULT_BG) return t;
        }

        return DefaultThemeConfig.get();
    }

    public static void save(ItemStack stack, ItemTheme theme) {
        if (stack.isEmpty()) return;
        stack.getOrCreateTag().put(TAG_ROOT, theme.toNBT());
    }

    // ── Style query ───────────────────────────────────────────────────────────
    public boolean isNativeStyle() { return uiStyle == UiStyle.GTCEU_NATIVE; }
    public boolean isBundleStyle() { return uiStyle == UiStyle.BUNDLE && bundleId != null && !bundleId.isBlank(); }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @javax.annotation.Nullable
    private com.gtceuterminal.common.theme.bundle.ThemeBundle activeBundle() {
        if (!isBundleStyle()) return null;
        return com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(bundleId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Texture helpers — USE THESE instead of reading color fields directly.
    // ─────────────────────────────────────────────────────────────────────────
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
                new ColorBorderTexture(1, accentColor));
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
        return new ColorBorderTexture(1, accentColor);
    }

    public IGuiTexture panelWithBorderTexture() {
        if (isNativeStyle()) return GuiTextures.DISPLAY;
        com.gtceuterminal.common.theme.bundle.ThemeBundle b = activeBundle();
        if (b != null) return b.resolvedPanelWithBorder();
        return new GuiTextureGroup(
                new ColorRectTexture(panelColor),
                new ColorBorderTexture(1, accentColor));
    }

    public int labelColor() {
        if (isNativeStyle()) return 0xFFDDDDDD;
        com.gtceuterminal.common.theme.bundle.ThemeBundle b = activeBundle();
        if (b != null) return b.textColor();
        return textColor;
    }

    public int dimColor() { return 0xFFAAAAAA; }

    public int borderColor() {
        if (isNativeStyle()) return 0x00000000;
        return accentColor;
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

    // ── Legacy color helpers ──────────────────────────────────────────────────
    public int accent(int alpha) {
        return (accentColor & 0x00FFFFFF) | (alpha << 24);
    }

    public int panelDark() {
        int r = Math.max(0, ((bgColor >> 16) & 0xFF) - 10);
        int g = Math.max(0, ((bgColor >>  8) & 0xFF) - 10);
        int b = Math.max(0, ( bgColor        & 0xFF) - 10);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public int hoverOverlay() { return accent(0x40); }

    public boolean hasWallpaper() {
        return wallpaper != null && !wallpaper.isBlank();
    }
}