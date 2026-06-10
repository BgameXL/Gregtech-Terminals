package com.gtceuterminal.common.network;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.ae2.MENetworkCrafting;
import com.gtceuterminal.common.autocraft.AnalysisResult;
import com.gtceuterminal.common.autocraft.MultiblockAnalyzer;
import com.gtceuterminal.common.pattern.AdvancedAutoBuilder;
import com.gtceuterminal.common.upgrade.ComponentUpgrader;
import com.gtceuterminal.common.config.ManagerSettings;
import com.gtceuterminal.common.multiblock.ComponentInfo;

import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Map;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CPacketConfirmAutobuild {

    private final AnalysisResult.Kind kind;
    private final BlockPos            controllerPos;
    private final int                 targetTier;
    private final String              upgradeId;
    private final List<BlockPos>      componentPositions;

    public CPacketConfirmAutobuild(AnalysisResult result) {
        this.kind               = result.kind;
        this.controllerPos      = result.controllerPos;
        this.targetTier         = result.targetTier;
        this.upgradeId          = result.upgradeId != null ? result.upgradeId : "";
        this.componentPositions = new ArrayList<>(result.componentPositions);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(kind);
        buf.writeBlockPos(controllerPos);
        buf.writeInt(targetTier);
        buf.writeUtf(upgradeId);
        buf.writeInt(componentPositions.size());
        for (BlockPos pos : componentPositions) buf.writeBlockPos(pos);
    }

    public CPacketConfirmAutobuild(FriendlyByteBuf buf) {
        this.kind          = buf.readEnum(AnalysisResult.Kind.class);
        this.controllerPos = buf.readBlockPos();
        this.targetTier    = buf.readInt();
        this.upgradeId     = buf.readUtf();
        int count = buf.readInt();
        this.componentPositions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) componentPositions.add(buf.readBlockPos());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            try {
                if (kind == AnalysisResult.Kind.BUILD) {
                    handleBuild(player);
                } else {
                    handleUpgrade(player);
                }
            } catch (Exception e) {
                GTCEUTerminalMod.LOGGER.error("CPacketConfirmAutobuild: error", e);
                player.displayClientMessage(
                        Component.translatable(
                                "item.gtceuterminal.autocraft_confirm.message.autocraft_failed",
                                e.getMessage()
                        ),
                        false
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleBuild(ServerPlayer player) {
        BlockEntity be = player.serverLevel().getBlockEntity(controllerPos);
        if (!(be instanceof com.gregtechceu.gtceu.api.machine.IMachineBlockEntity mbe)) {
            msg(player,
                    Component.translatable("item.gtceuterminal.autocraft_confirm.message.controller_not_found"), false);
            return;
        }
        MetaMachine machine = mbe.getMetaMachine();
        if (!(machine instanceof IMultiController controller)) {
            msg(player,
                    Component.translatable("item.gtceuterminal.autocraft_confirm.message.target_not_multiblock_controller"), false);
            return;
        }
        if (controller.isFormed()) {
            msg(player,
                    Component.translatable("item.gtceuterminal.autocraft_confirm.message.multiblock_already_formed"), true);
            return;
        }

        ManagerSettings.AutoBuildSettings settings = getSettings(player);
        settings.isUseAE = 1;

        AnalysisResult analysis = MultiblockAnalyzer.analyzeForBuild(player, controller, settings);
        FulfillResult fr = analysis == null
                ? new FulfillResult(Fulfillment.READY, 0)
                : evaluate(player, analysis.entries);

        switch (fr.status) {
            case MISSING -> msg(player,
                    Component.translatable("item.gtceuterminal.autocraft_confirm.message.build_failed_check_materials"), false);
            case CRAFTING -> msg(player,
                    Component.translatable("item.gtceuterminal.autocraft_confirm.message.crafting_started", fr.craftCount), false);
            case READY -> {
                boolean ok = AdvancedAutoBuilder.autoBuild(player, controller, settings);
                if (ok) {
                    msg(player,
                            Component.translatable("item.gtceuterminal.autocraft_confirm.message.multiblock_built"), true);
                } else {
                    msg(player,
                            Component.translatable("item.gtceuterminal.autocraft_confirm.message.build_failed_check_materials"), false);
                }
            }
        }
    }

    private void handleUpgrade(ServerPlayer player) {
        if (componentPositions.isEmpty()) {
            msg(player,
                    Component.translatable("item.gtceuterminal.autocraft_confirm.message.no_components_to_upgrade"),
                    false);
            return;
        }

        List<ComponentInfo> components = new ArrayList<>();
        for (BlockPos pos : componentPositions) {
            net.minecraft.world.level.block.state.BlockState state =
                    player.serverLevel().getBlockState(pos);
            components.add(new ComponentInfo(
                    com.gtceuterminal.common.multiblock.ComponentGroupRegistry.CASING, 0, pos, state));
        }

        AnalysisResult analysis = MultiblockAnalyzer.analyzeForUpgrade(player, components, targetTier,
                upgradeId.isEmpty() ? null : upgradeId, controllerPos);
        FulfillResult fr = analysis == null
                ? new FulfillResult(Fulfillment.READY, 0)
                : evaluate(player, analysis.entries);
        if (fr.status == Fulfillment.MISSING) {
            msg(player,
                    Component.translatable("item.gtceuterminal.autocraft_confirm.message.upgrade_failed_check_materials"), false);
            return;
        }
        if (fr.status == Fulfillment.CRAFTING) {
            msg(player,
                    Component.translatable("item.gtceuterminal.autocraft_confirm.message.crafting_started", fr.craftCount), false);
            return;
        }

        ItemStack terminal = ItemStack.EMPTY;
        int succeeded = 0, failed = 0;

        for (ComponentInfo info : components) {
            ComponentUpgrader.UpgradeResult result = ComponentUpgrader.upgradeComponent(
                    info,
                    targetTier,
                    upgradeId.isEmpty() ? null : upgradeId,
                    player,
                    player.serverLevel(),
                    true,
                    terminal);

            if (result.success) succeeded++; else failed++;
        }

        if (succeeded > 0) {
            if (failed > 0) {
                msg(player,
                        Component.translatable("item.gtceuterminal.autocraft_confirm.message.upgrade_success_with_failed", succeeded, failed), false);
            } else {
                msg(player,
                        Component.translatable("item.gtceuterminal.autocraft_confirm.message.upgrade_success_no_failed", succeeded), false);
            }
        } else {
            msg(player,
                    Component.translatable("item.gtceuterminal.autocraft_confirm.message.upgrade_failed_check_materials"), false);
        }
    }

    private static ManagerSettings.AutoBuildSettings getSettings(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.is(com.gtceuterminal.common.data.GTCEUTerminalItems.MULTI_STRUCTURE_MANAGER.get())) {
            return new ManagerSettings.Settings(held).toAutoBuildSettings();
        }
        held = player.getOffhandItem();
        if (held.is(com.gtceuterminal.common.data.GTCEUTerminalItems.MULTI_STRUCTURE_MANAGER.get())) {
            return new ManagerSettings.Settings(held).toAutoBuildSettings();
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(com.gtceuterminal.common.data.GTCEUTerminalItems.MULTI_STRUCTURE_MANAGER.get())) {
                return new ManagerSettings.Settings(s).toAutoBuildSettings();
            }
        }
        return new ManagerSettings.AutoBuildSettings();
    }

    private enum Fulfillment { READY, CRAFTING, MISSING }

    private record FulfillResult(Fulfillment status, int craftCount) {}

    private static FulfillResult evaluate(ServerPlayer player, List<AnalysisResult.Entry> entries) {
        Map<Item, Integer> craftDeficits = new LinkedHashMap<>();
        boolean hardMissing = false;
        boolean anyDeficit  = false;

        for (AnalysisResult.Entry e : entries) {
            int  needed = e.needed();
            long avail  = e.inME + countInInventory(player, e.stack.getItem());
            if (avail >= needed) continue;

            anyDeficit = true;
            if (e.craftable) {
                craftDeficits.merge(e.stack.getItem(), (int) (needed - avail), Integer::sum);
            } else {
                hardMissing = true;
            }
        }

        if (!anyDeficit) return new FulfillResult(Fulfillment.READY, 0);
        if (hardMissing) return new FulfillResult(Fulfillment.MISSING, 0);

        int queued = MENetworkCrafting.requestCrafts(player, craftDeficits);
        return new FulfillResult(queued > 0 ? Fulfillment.CRAFTING : Fulfillment.MISSING, queued);
    }

    private static int countInInventory(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() == item) count += s.getCount();
        }
        return count;
    }

    private static void msg(ServerPlayer p, Component component, boolean actionBar) {
        p.displayClientMessage(component, actionBar);
    }
}