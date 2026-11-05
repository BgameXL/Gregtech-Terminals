package com.gtceuterminal.common.theme.bundle;

import com.gtceuterminal.common.theme.ItemTheme;
import com.lowdragmc.lowdraglib.gui.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

public abstract class ThemeBundle {

    public boolean isAvailable() {
        return true;
    }

    public abstract String id();

    public abstract String displayName();

    public abstract String description();

    public abstract int accentColor();

    public abstract int bgColor();

    public abstract int panelColor();

    public int textColor() {
        return ItemTheme.DEFAULT_TEXT;
    }

    public int textMutedColor() {
        return ItemTheme.DEFAULT_TEXT_MUTED;
    }

    public int borderColor() {
        return accentColor();
    }

    public int successColor() {
        return ItemTheme.DEFAULT_SUCCESS;
    }

    public int warningColor() {
        return ItemTheme.DEFAULT_WARNING;
    }

    public int errorColor() {
        return ItemTheme.DEFAULT_ERROR;
    }

    @OnlyIn(Dist.CLIENT)
    public java.util.List<ResourceLocation> slideshowWallpapers() {
        return java.util.Collections.emptyList();
    }

    @OnlyIn(Dist.CLIENT)
    public boolean hasSlideshow() {
        return !slideshowWallpapers().isEmpty();
    }

    @Nullable
    public ResourceLocation builtinWallpaper() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public IGuiTexture previewTexture() {
        ResourceLocation wp = builtinWallpaper();
        if (wp != null) return new ResourceTexture(wp.toString());
        return new ColorRectTexture(accentColor());
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IGuiTexture iconTexture() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IGuiTexture backgroundTexture() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IGuiTexture panelTexture() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IGuiTexture headerTexture() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IGuiTexture slotTexture() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IGuiTexture buttonTexture() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IGuiTexture borderTexture() {
        return null;
    }

    public int separatorColor() {
        return (borderColor() & 0x00FFFFFF) | 0x55000000;
    }

    public void applyTo(ItemTheme theme) {
        theme.accentColor = accentColor();
        theme.bgColor = bgColor();
        theme.panelColor = panelColor();
        theme.textColor = textColor();
        theme.textMutedColor = textMutedColor();
        theme.borderColor = borderColor();
        theme.successColor = successColor();
        theme.warningColor = warningColor();
        theme.errorColor = errorColor();
        theme.uiStyle = ItemTheme.UiStyle.BUNDLE;
        theme.bundleId = id();
        ResourceLocation wp = builtinWallpaper();
        theme.wallpaper = (wp != null) ? ("bundle:" + id()) : "";
    }

    protected ResourceLocation bundleTexture(String filename) {
        return ResourceLocation.fromNamespaceAndPath(
                "gtceuterminal",
                "textures/themes/" + id() + "/" + filename
        );
    }

    @OnlyIn(Dist.CLIENT)
    protected IGuiTexture bundleResourceTexture(String filename) {
        return new ResourceTexture(bundleTexture(filename).toString());
    }

    @OnlyIn(Dist.CLIENT)
    protected IGuiTexture accentButton(@Nullable IGuiTexture bg) {
        IGuiTexture base = (bg != null)
                ? bg
                : new ColorRectTexture(panelColor());
        return new GuiTextureGroup(
                base,
                new ColorBorderTexture(1, borderColor())
        );
    }

    @OnlyIn(Dist.CLIENT)
    public final IGuiTexture resolvedBackground() {
        IGuiTexture t = backgroundTexture();
        return t != null ? t : new ColorRectTexture(bgColor());
    }

    @OnlyIn(Dist.CLIENT)
    public final IGuiTexture resolvedPanel() {
        IGuiTexture t = panelTexture();
        return t != null ? t : new ColorRectTexture(panelColor());
    }

    @OnlyIn(Dist.CLIENT)
    public final IGuiTexture resolvedHeader() {
        IGuiTexture t = headerTexture();
        return t != null ? t : new ColorRectTexture(panelColor());
    }

    @OnlyIn(Dist.CLIENT)
    public final IGuiTexture resolvedSlot() {
        IGuiTexture t = slotTexture();
        if (t != null) return t;
        return new com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture(
                "ldlib:textures/gui/button_common.png",
                198,
                18,
                1,
                1
        );
    }

    @OnlyIn(Dist.CLIENT)
    public final IGuiTexture resolvedButton() {
        IGuiTexture t = buttonTexture();
        if (t != null) return t;
        return new GuiTextureGroup(
                new ColorRectTexture(panelColor()),
                new ColorBorderTexture(1, borderColor())
        );
    }

    @OnlyIn(Dist.CLIENT)
    public final IGuiTexture resolvedBorder() {
        IGuiTexture t = borderTexture();
        return t != null ? t : new ColorBorderTexture(1, borderColor());
    }

    @OnlyIn(Dist.CLIENT)
    public final IGuiTexture resolvedPanelWithBorder() {
        IGuiTexture panel = panelTexture();
        IGuiTexture border = borderTexture();
        IGuiTexture base = (panel != null)
                ? panel
                : new ColorRectTexture(panelColor());
        IGuiTexture rim = (border != null)
                ? border
                : new ColorBorderTexture(1, borderColor());
        return new GuiTextureGroup(base, rim);
    }
}