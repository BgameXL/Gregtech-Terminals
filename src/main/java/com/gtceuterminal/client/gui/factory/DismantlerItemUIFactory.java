package com.gtceuterminal.client.gui.factory;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.multiblock.DismantleScanner;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DismantlerItemUIFactory extends UIFactory<DismantlerItemUIFactory.Holder> {

    public static final ResourceLocation UI_ID = ResourceLocation.fromNamespaceAndPath(
            GTCEUTerminalMod.MOD_ID, "dismantler_ui"
    );

    public static final DismantlerItemUIFactory INSTANCE = new DismantlerItemUIFactory();

    private DismantlerItemUIFactory() {
        super(UI_ID);
    }

    // ── Server entry point ────────────────────────────────────────────────────
    public void openUI(ServerPlayer player, ItemStack item, BlockPos controllerPos) {
        Set<BlockPos> scannedPositions = scanPositions(player, controllerPos);
        super.openUI(new Holder(false, item, controllerPos, scannedPositions), player);
    }

    private static Set<BlockPos> scanPositions(ServerPlayer player, BlockPos controllerPos) {
        try {
            var be = player.level().getBlockEntity(controllerPos);
            if (be instanceof com.gregtechceu.gtceu.api.machine.IMachineBlockEntity mbe) {
                var meta = mbe.getMetaMachine();
                if (meta instanceof com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine ctrl) {
                    var result = DismantleScanner.scanMultiblock(player.level(), ctrl);
                    return result.getAllBlocks();
                }
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("DismantlerItemUIFactory: failed to pre-scan at {}", controllerPos, e);
        }
        return java.util.Collections.emptySet();
    }

    // ── UIFactory impl ────────────────────────────────────────────────────────
    @Override
    protected ModularUI createUITemplate(Holder holder, Player entityPlayer) {
        holder.attach(entityPlayer);
        try {
            Class<?> uiClass = Class.forName(
                    "com.gtceuterminal.client.gui.multiblock.DismantlerUI");
            var m = uiClass.getMethod("create", Holder.class, Player.class);
            return (ModularUI) m.invoke(null, holder, entityPlayer);
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.error("Failed to create Dismantler UI", t);
            throw new RuntimeException("Failed to create Dismantler UI", t);
        }
    }

    @Override
    protected Holder readHolderFromSyncData(FriendlyByteBuf buf) {
        ItemStack item = buf.readItem();
        BlockPos controllerPos = buf.readBlockPos();
        int count = buf.readVarInt();
        Set<BlockPos> positions = new java.util.HashSet<>(count);
        for (int i = 0; i < count; i++) positions.add(buf.readBlockPos());
        return new Holder(true, item, controllerPos, positions);
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf buf, Holder holder) {
        buf.writeItem(holder.item);
        buf.writeBlockPos(holder.controllerPos);
        buf.writeVarInt(holder.scannedPositions.size());
        for (BlockPos pos : holder.scannedPositions) buf.writeBlockPos(pos);
    }

    // ── Holder ────────────────────────────────────────────────────────────────
    public static class Holder implements IUIHolder {
        public final boolean remote;
        public final ItemStack item;
        public final BlockPos controllerPos;
        public final Set<BlockPos> scannedPositions;
        private Player player;

        public Holder(boolean remote, ItemStack item, BlockPos controllerPos, Set<BlockPos> scannedPositions) {
            this.remote = remote;
            this.item = item;
            this.controllerPos = controllerPos;
            this.scannedPositions = scannedPositions;
        }

        public void attach(Player p) { if (this.player == null) this.player = p; }
        public Player getPlayer() { return player; }

        @Override public ModularUI createUI(Player p) { attach(p); return INSTANCE.createUITemplate(this, p); }
        @Override public boolean isInvalid() { return player != null && player.isRemoved(); }
        @Override public boolean isRemote() { return remote; }
        @Override public void markAsDirty() { }
    }
}