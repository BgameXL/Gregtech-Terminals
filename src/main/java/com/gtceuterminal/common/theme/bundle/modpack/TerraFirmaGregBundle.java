package com.gtceuterminal.common.theme.bundle.modpack;

import com.gtceuterminal.common.theme.bundle.ThemeBundle;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

public class TerraFirmaGregBundle extends ThemeBundle {

    private static final int BG = 0xFF0D0610;
    private static final int PANEL = 0xFF1A0D1F;
    private static final int ACCENT = 0xFFEA097B;
    private static final int BRIGHT = 0xFFF43B7D;
    private static final int TEXT = 0xFFFFE8F0;

    @Override
    public String id() {
        return "terrafirmagreg";
    }

    @Override
    public String displayName() {
        return "TerraFirmaGreg";
    }

    @Override
    public String description() {
        return "§dOfficial TerraFirmaGreg Theme. §8TerraFirmaGreg meets GTCEu Terminals.";
    }

    @Override
    public int accentColor() {
        return ACCENT;
    }

    @Override
    public int bgColor() {
        return BG;
    }

    @Override
    public int panelColor() {
        return PANEL;
    }

    @Override
    public int textColor() {
        return TEXT;
    }

    @Override
    public int separatorColor() {
        return (BRIGHT & 0x00FFFFFF) | 0xAA000000;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public java.util.List<net.minecraft.resources.ResourceLocation> slideshowWallpapers() {
        String[] files = {
                "01.png",
                "02.png",
                "03.png",
                "04.png",
                "05.png",
                "06.png",
                "07.png",
        };
        java.util.List<net.minecraft.resources.ResourceLocation> list = new java.util.ArrayList<>();
        for (String f : files) {
            list.add(bundleTexture("slideshow/" + f));
        }
        return list;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public IGuiTexture previewTexture() {
        return new GuiTextureGroup(
                new ColorRectTexture(BG),
                bundleResourceTexture("icon.png"));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public IGuiTexture iconTexture() {
        return bundleResourceTexture("icon.png");
    }

    @Override
    @Nullable
    public ResourceLocation builtinWallpaper() {
        return null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IGuiTexture backgroundTexture() {
        // return bundleResourceTexture("background.png");
        return null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IGuiTexture panelTexture() {
        // return bundleResourceTexture("panel.png");
        return null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IGuiTexture headerTexture() {
        // return bundleResourceTexture("header.png");
        return null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IGuiTexture slotTexture() {
        // return bundleResourceTexture("slot.png");
        return null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IGuiTexture buttonTexture() {
        return new GuiTextureGroup(
                new ColorRectTexture(PANEL),
                new ColorBorderTexture(1, ACCENT));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IGuiTexture borderTexture() {
        return new ColorBorderTexture(1, ACCENT);
    }
}