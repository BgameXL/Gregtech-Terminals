package com.gtceuterminal.client;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.GTCEUTerminalItems;
import com.gtceuterminal.common.theme.ItemTheme;

import net.minecraft.world.item.Item;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GTCEUTerminalMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public class ItemThemeColorHandler {

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        Item[] terminalItems = {
                GTCEUTerminalItems.ENERGY_ANALYZER.get(),
                GTCEUTerminalItems.MULTI_STRUCTURE_MANAGER.get(),
                GTCEUTerminalItems.DISMANTLER.get(),
                GTCEUTerminalItems.SCHEMATIC_INTERFACE.get()
        };

        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 1) {
                        ItemTheme theme = ItemTheme.load(stack);
                        return theme.accentColor | 0xFF000000;
                    }
                    return 0xFFFFFFFF;
                },
                terminalItems
        );

        GTCEUTerminalMod.LOGGER.debug(
                "ItemThemeColorHandler: registered accent tinting for {} terminal items",
                terminalItems.length);
    }
}