package com.gtceuterminal;

import com.gtceuterminal.common.ae2.AE2Integration;
import com.gtceuterminal.common.command.GTCETerminalCommands;
import com.gtceuterminal.common.compat.integrations.StarTCoreIntegration;
import com.gtceuterminal.common.config.ItemsConfig;
import com.gtceuterminal.common.config.ServerConfig;
import com.gtceuterminal.common.data.GTCEUTerminalItems;
import com.gtceuterminal.common.data.GTCEUTerminalTabs;
import com.gtceuterminal.common.gui.factory.DismantlerItemUIFactory;
import com.gtceuterminal.common.gui.factory.EnergyAnalyzerUIFactory;
import com.gtceuterminal.common.gui.factory.MultiStructureManagerUIFactory;
import com.gtceuterminal.common.gui.factory.SchematicItemUIFactory;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.gtceuterminal.common.theme.DefaultThemeConfig;
import com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry;
import com.gtceuterminal.common.util.MiscUtil;
import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

@Mod(GTCEUTerminalMod.MOD_ID)
public class GTCEUTerminalMod {
    public static final String MOD_ID = "gtceuterminal";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public GTCEUTerminalMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        GTCEUTerminalItems.ITEMS.register(modEventBus);
        GTCEUTerminalTabs.CREATIVE_TABS.register(modEventBus);

        UIFactory.register(EnergyAnalyzerUIFactory.INSTANCE);
        UIFactory.register(MultiStructureManagerUIFactory.INSTANCE);
        UIFactory.register(SchematicItemUIFactory.INSTANCE);
        UIFactory.register(DismantlerItemUIFactory.INSTANCE);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        // Guard: if the config file exists but is empty/truncated, delete it so Forge regenerates it
        try {
            Path configPath = FMLPaths.CONFIGDIR.get().resolve(ServerConfig.FILE_NAME);
            if (Files.exists(configPath) && Files.size(configPath) == 0) {
                LOGGER.warn("[GTCEuTerminal] Config file {} is empty/corrupted, deleting for regeneration", ServerConfig.FILE_NAME);
                Files.delete(configPath);
            }
        } catch (Exception e) {
            LOGGER.warn("[GTCEuTerminal] Could not check config file integrity: {}", e.getMessage());
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,
                ServerConfig.SPEC,
                ServerConfig.FILE_NAME);

    }

    private void commonSetup(FMLCommonSetupEvent event) {
        TerminalNetwork.registerPackets();

        event.enqueueWork(() -> {
            ItemsConfig.load();

            ThemeBundleRegistry.init();

            if (MiscUtil.isAE2Loaded) {
                AE2Integration.init();
            }
            StarTCoreIntegration.init();
        });

    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GTCETerminalCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ItemsConfig.load();
        DefaultThemeConfig.reload();
    }
}