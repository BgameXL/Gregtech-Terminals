package com.gtceuterminal.client.gui.style;

import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.common.theme.bundle.ThemeBundle;
import com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry;
import com.lowdragmc.lowdraglib.gui.texture.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TerminalUI {

    private static final int TEXT_VCENTER_NUDGE = 1;

    private static ItemTheme activeTheme = new ItemTheme();
    private static TerminalTheme tokens = TerminalTheme.DEFAULT;

    private TerminalUI() {
    }

    public static void use(ItemTheme theme) {
        activeTheme = theme;
        tokens = TerminalTheme.derive(theme);
    }

    public static TerminalTheme tokens() {
        return tokens;
    }

    public static IGuiTexture transparent() {
        return color(TerminalPalette.CLEAR);
    }

    public static IGuiTexture background() {
        return activeTheme.backgroundTexture();
    }

    public static IGuiTexture modularBackground() {
        return activeTheme.modularUIBackground();
    }

    public static IGuiTexture bundleIcon() {
        if (!activeTheme.isBundleStyle()) return null;
        ThemeBundle bundle = ThemeBundleRegistry.get(activeTheme.bundleId);
        return bundle != null ? bundle.iconTexture() : null;
    }

    public static int itemLabelColor() {
        if (activeTheme.isBundleStyle()) return activeTheme.labelColor();
        return TerminalPalette.contrast(tokens.surface(), tokens.textStrong(), TerminalPalette.BLACK);
    }

    public static IGuiTexture scrim() {
        return color(TerminalPalette.SCRIM);
    }

    public static IGuiTexture color(int color) {
        return new ColorRectTexture(color);
    }

    public static IGuiTexture border(int color) {
        return new ColorBorderTexture(1, color);
    }

    public static IGuiTexture layers(IGuiTexture... textures) {
        return new GuiTextureGroup(textures);
    }

    public static IGuiTexture outlined(int fill, int border) {
        return layers(color(fill), border(border));
    }

    public static IGuiTexture bevel(int fill) {
        return new TerminalBevel(fill);
    }

    public static IGuiTexture well(int fill) {
        return new TerminalBevel(fill, true);
    }

    public static IGuiTexture surface() {
        return color(tokens.surface());
    }

    public static IGuiTexture panel() {
        return color(tokens.panel());
    }

    public static IGuiTexture panelDisabled() {
        return color(tokens.panelDisabled());
    }

    public static IGuiTexture divider() {
        return color(tokens.borderDark());
    }

    public static IGuiTexture hover() {
        return color(tokens.hover());
    }

    public static IGuiTexture field(int border) {
        return outlined(tokens.panelDisabled(), border);
    }

    public static IGuiTexture text(String text, int color, int width) {
        return new VNudge(new TextTexture(text, color)
                .setWidth(width)
                .setType(TextTexture.TextType.NORMAL));
    }

    public static IGuiTexture button(String text, int width, int fill, int textColor) {
        return layers(bevel(fill), text(text, textColor, width));
    }

    public static IGuiTexture buttonHover(String text, int width, int fill, int textColor) {
        return button(text, width, TerminalPalette.shade(fill, 1.30f), textColor);
    }

    public static IGuiTexture buttonContent(int fill, IGuiTexture content) {
        return layers(bevel(fill), content);
    }

    public static IGuiTexture buttonContentHover(int fill, IGuiTexture content) {
        return buttonContent(TerminalPalette.shade(fill, 1.30f), content);
    }

    public static ResourceTexture resource(String path) {
        return new ResourceTexture(path);
    }

    public static IGuiTexture cardBG() {
        return bevel(tokens.surface());
    }

    private static final class VNudge extends TransformTexture {

        private final IGuiTexture inner;

        private VNudge(IGuiTexture inner) {
            this.inner = inner;
        }

        @Override
        protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY,
                                    float x, float y, int width, int height) {
            inner.draw(graphics, mouseX, mouseY, x, y + TEXT_VCENTER_NUDGE, width, height);
        }
    }
}