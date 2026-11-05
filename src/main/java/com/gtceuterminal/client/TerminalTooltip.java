package com.gtceuterminal.client;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.common.theme.ItemTheme;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = GTCEUTerminalMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TerminalTooltip {

    private static final int BG_ALPHA = 0xF0;
    private static final int BORDER_ALPHA = 0xFF;

    private TerminalTooltip() {
    }

    @SubscribeEvent
    public static void onColor(RenderTooltipEvent.Color event) {
        try {
            ItemStack stack = event.getItemStack();
            if (stack.isEmpty()) return;

            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null || !GTCEUTerminalMod.MOD_ID.equals(id.getNamespace())) return;

            ItemTheme theme = ItemTheme.load(stack);
            if (theme.isNativeStyle()) return;

            TerminalTheme tokens = TerminalTheme.derive(theme);
            event.setBackground(argb(tokens.surface(), BG_ALPHA));
            int border = argb(tokens.border(), BORDER_ALPHA);
            event.setBorderStart(border);
            event.setBorderEnd(border);
        } catch (Throwable ignored) {
        }
    }

    private static int argb(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }
}