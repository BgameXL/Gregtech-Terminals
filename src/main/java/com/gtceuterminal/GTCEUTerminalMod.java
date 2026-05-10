package com.gtceuterminal;

import com.gtceuterminal.client.gui.factory.DismantlerItemUIFactory;
import com.gtceuterminal.client.gui.factory.EnergyAnalyzerUIFactory;
import com.gtceuterminal.client.gui.factory.MultiStructureManagerUIFactory;
import com.gtceuterminal.client.gui.factory.SchematicItemUIFactory;
import com.gtceuterminal.common.ae2.AE2Integration;
import com.gtceuterminal.common.command.GTCETerminalCommands;
import com.gtceuterminal.common.config.*;
import com.gtceuterminal.common.theme.DefaultThemeConfig;
import com.gtceuterminal.common.data.GTCEUTerminalItems;
import com.gtceuterminal.common.data.GTCEUTerminalTabs;
import com.gtceuterminal.common.network.TerminalNetwork;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.minecraft.client.gui.screens.MenuScreens;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;

import com.mojang.logging.LogUtils;

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

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

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

        LOGGER.info("GTCEu Terminal Addon initialized");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Common setup started");
        TerminalNetwork.registerPackets();
        LOGGER.info("Terminal Network packets registered");

        event.enqueueWork(() -> {
            LOGGER.info("Initializing component configurations...");
                    com.gtceuterminal.common.config.ItemsConfig.load();

            CoilConfig.initialize();
            HatchConfig.initialize();
            BusConfig.initialize();
            EnergyHatchConfig.initialize();
            MufflerHatchConfig.initialize();
            ParallelHatchConfig.initialize();
            MaintenanceHatchConfig.initialize();
            ComponentRegistry.init();
            LaserHatchConfig.initialize();
            WirelessHatchConfig.initialize();
            SubstationHatchConfig.initialize();
            DualHatchConfig.initialize();

            com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.init();

            LOGGER.info("All component configurations initialized successfully");
                    LOGGER.info("Initializing AE2 integration...");
            if (net.minecraftforge.fml.ModList.get().isLoaded("ae2")) {
                AE2Integration.init();
            } else {
                LOGGER.info("AE2 not present — AE2 integration skipped");
            }
        });

        LOGGER.info("Common setup complete");
    }

    private void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Client setup started");
        event.enqueueWork(() -> {
            UIFactory.register(EnergyAnalyzerUIFactory.INSTANCE);
            UIFactory.register(MultiStructureManagerUIFactory.INSTANCE);
            UIFactory.register(SchematicItemUIFactory.INSTANCE);
            UIFactory.register(DismantlerItemUIFactory.INSTANCE);
            LOGGER.info("UI Factories registered (EnergyAnalyzer + MultiStructureManager + Schematic + Dismantler)");
        });
        LOGGER.info("Client setup complete");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GTCETerminalCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        com.gtceuterminal.common.config.ItemsConfig.load();
        DefaultThemeConfig.reload();
    }
}