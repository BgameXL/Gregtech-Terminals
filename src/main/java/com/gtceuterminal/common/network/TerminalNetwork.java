package com.gtceuterminal.common.network;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class TerminalNetwork {

    private static final String PROTOCOL = "7";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(GTCEUTerminalMod.MOD_ID, "network"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;

    public static void registerPackets() {

        CHANNEL.messageBuilder(CPacketBlockReplacement.class,        id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketBlockReplacement::encode)
                .decoder(CPacketBlockReplacement::new)
                .consumerMainThread(CPacketBlockReplacement::handle)
                .add();

        CHANNEL.messageBuilder(CPacketSchematicAction.class,         id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketSchematicAction::encode)
                .decoder(CPacketSchematicAction::new)
                .consumerMainThread(CPacketSchematicAction::handle)
                .add();

        CHANNEL.messageBuilder(CPacketComponentUpgrade.class,        id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketComponentUpgrade::encode)
                .decoder(CPacketComponentUpgrade::decode)
                .consumerMainThread(CPacketComponentUpgrade::handle)
                .add();

        CHANNEL.messageBuilder(CPacketSetCustomMultiblockName.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketSetCustomMultiblockName::encode)
                .decoder(CPacketSetCustomMultiblockName::new)
                .consumerMainThread(CPacketSetCustomMultiblockName::handle)
                .add();

        CHANNEL.messageBuilder(CPacketDismantle.class,               id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketDismantle::encode)
                .decoder(CPacketDismantle::decode)
                .consumerMainThread(CPacketDismantle::handle)
                .add();

        CHANNEL.messageBuilder(CPacketOpenEnergyAnalyzerUI.class,    id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketOpenEnergyAnalyzerUI::encode)
                .decoder(CPacketOpenEnergyAnalyzerUI::decode)
                .consumerMainThread(CPacketOpenEnergyAnalyzerUI::handle)
                .add();

        CHANNEL.messageBuilder(CPacketEnergyAnalyzerAction.class,    id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketEnergyAnalyzerAction::encode)
                .decoder(CPacketEnergyAnalyzerAction::decode)
                .consumerMainThread(CPacketEnergyAnalyzerAction::handle)
                .add();

        CHANNEL.messageBuilder(CPacketSaveTheme.class,               id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketSaveTheme::encode)
                .decoder(CPacketSaveTheme::new)
                .consumerMainThread(CPacketSaveTheme::handle)
                .add();

        CHANNEL.messageBuilder(CPacketPlacePlannerGhosts.class,      id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketPlacePlannerGhosts::encode)
                .decoder(CPacketPlacePlannerGhosts::new)
                .consumerMainThread(CPacketPlacePlannerGhosts::handle)
                .add();

        CHANNEL.messageBuilder(CPacketConfirmAutobuild.class,         id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketConfirmAutobuild::encode)
                .decoder(CPacketConfirmAutobuild::new)
                .consumerMainThread(CPacketConfirmAutobuild::handle)
                .add();

        CHANNEL.messageBuilder(CPacketRequestUpgradeAnalysis.class,   id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketRequestUpgradeAnalysis::encode)
                .decoder(CPacketRequestUpgradeAnalysis::new)
                .consumerMainThread(CPacketRequestUpgradeAnalysis::handle)
                .add();

        CHANNEL.messageBuilder(CPacketRequestUpgradeAvailability.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketRequestUpgradeAvailability::encode)
                .decoder(CPacketRequestUpgradeAvailability::new)
                .consumerMainThread(CPacketRequestUpgradeAvailability::handle)
                .add();

        CHANNEL.messageBuilder(SPacketOpenMultiStructureManagerUI.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SPacketOpenMultiStructureManagerUI::encode)
                .decoder(SPacketOpenMultiStructureManagerUI::new)
                .consumerMainThread(SPacketOpenMultiStructureManagerUI::handle)
                .add();

        CHANNEL.messageBuilder(SPacketOpenSchematicInterfaceUI.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SPacketOpenSchematicInterfaceUI::encode)
                .decoder(SPacketOpenSchematicInterfaceUI::new)
                .consumerMainThread(SPacketOpenSchematicInterfaceUI::handle)
                .add();

        CHANNEL.messageBuilder(SPacketOpenDismantlerUI.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SPacketOpenDismantlerUI::encode)
                .decoder(SPacketOpenDismantlerUI::new)
                .consumerMainThread(SPacketOpenDismantlerUI::handle)
                .add();

        CHANNEL.messageBuilder(SPacketOpenEnergyAnalyzerUI.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SPacketOpenEnergyAnalyzerUI::encode)
                .decoder(SPacketOpenEnergyAnalyzerUI::new)
                .consumerMainThread(SPacketOpenEnergyAnalyzerUI::handle)
                .add();

        CHANNEL.messageBuilder(SPacketDefaultTheme.class,            id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SPacketDefaultTheme::encode)
                .decoder(SPacketDefaultTheme::new)
                .consumerMainThread(SPacketDefaultTheme::handle)
                .add();

        CHANNEL.messageBuilder(SPacketAnalysisResult.class,          id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SPacketAnalysisResult::encode)
                .decoder(SPacketAnalysisResult::new)
                .consumerMainThread(SPacketAnalysisResult::handle)
                .add();

        CHANNEL.messageBuilder(SPacketUpgradeAvailability.class,     id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SPacketUpgradeAvailability::encode)
                .decoder(SPacketUpgradeAvailability::new)
                .consumerMainThread(SPacketUpgradeAvailability::handle)
                .add();

        CHANNEL.messageBuilder(CPacketSaveManagerSettings.class,     id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketSaveManagerSettings::encode)
                .decoder(CPacketSaveManagerSettings::new)
                .consumerMainThread(CPacketSaveManagerSettings::handle)
                .add();

        GTCEUTerminalMod.LOGGER.info("TerminalNetwork: registered {} packets", id);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
