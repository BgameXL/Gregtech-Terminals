package com.gtceuterminal.common.network;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.multiblock.DismantleScanner;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class SPacketOpenDismantlerUI {

    public final ItemStack item;
    public final BlockPos controllerPos;
    public final Set<BlockPos> scannedPositions;

    public SPacketOpenDismantlerUI(ItemStack item, BlockPos controllerPos, Set<BlockPos> scannedPositions) {
        this.item = item.copy();
        this.controllerPos = controllerPos;
        this.scannedPositions = scannedPositions != null ? new HashSet<>(scannedPositions) : Collections.emptySet();
    }

    public SPacketOpenDismantlerUI(FriendlyByteBuf buf) {
        this.item = buf.readItem();
        this.controllerPos = buf.readBlockPos();
        int count = buf.readVarInt();
        Set<BlockPos> positions = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            positions.add(buf.readBlockPos());
        }
        this.scannedPositions = positions;
    }

    public static void encode(SPacketOpenDismantlerUI msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.item);
        buf.writeBlockPos(msg.controllerPos);
        buf.writeVarInt(msg.scannedPositions.size());
        for (BlockPos pos : msg.scannedPositions) {
            buf.writeBlockPos(pos);
        }
    }

    public static void openFor(ServerPlayer player, ItemStack item, BlockPos controllerPos) {
        TerminalNetwork.sendToPlayer(new SPacketOpenDismantlerUI(item, controllerPos, scanPositions(player, controllerPos)), player);
    }

    private static Set<BlockPos> scanPositions(ServerPlayer player, BlockPos controllerPos) {
        try {
            var be = player.level().getBlockEntity(controllerPos);
            if (be instanceof IMachineBlockEntity mbe) {
                var meta = mbe.getMetaMachine();
                if (meta instanceof MultiblockControllerMachine ctrl) {
                    var result = DismantleScanner.scanMultiblock(player.level(), ctrl);
                    return result.getAllBlocks();
                }
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("SPacketOpenDismantlerUI: failed to pre-scan at {}", controllerPos, e);
        }
        return Collections.emptySet();
    }

    public static void handle(SPacketOpenDismantlerUI msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.gtceuterminal.client.network.ClientPacketHandlers.handleOpenDismantler(
                        msg.item, msg.controllerPos, msg.scannedPositions)));
        context.setPacketHandled(true);
    }
}
