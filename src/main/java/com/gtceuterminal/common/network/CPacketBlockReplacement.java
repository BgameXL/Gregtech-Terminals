package com.gtceuterminal.common.network;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.ae2.CuriosCompat;
import com.gtceuterminal.common.data.BlockReplacementData;
import com.gtceuterminal.common.item.MultiStructureManagerItem;
import com.gtceuterminal.common.multiblock.BlockReplacer;
import com.gtceuterminal.common.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CPacketBlockReplacement {

    private final BlockPos controllerPos;
    private final Map<Integer, Integer> replacements;
    private final boolean mirrorMode;
    private final int fillCasingId;

    public CPacketBlockReplacement(BlockPos controllerPos,
                                   Map<BlockState, BlockState> replacements,
                                   boolean mirrorMode,
                                   BlockState fillCasing) {
        this.controllerPos = controllerPos;
        this.replacements = new HashMap<>();
        this.mirrorMode = mirrorMode;
        this.fillCasingId = fillCasing != null ? Block.getId(fillCasing) : -1;

        for (Map.Entry<BlockState, BlockState> entry : replacements.entrySet()) {
            int oldId = Block.getId(entry.getKey());
            int newId = Block.getId(entry.getValue());
            this.replacements.put(oldId, newId);
        }
    }

    public CPacketBlockReplacement(FriendlyByteBuf buf) {
        this.controllerPos = buf.readBlockPos();
        this.mirrorMode = buf.readBoolean();
        this.fillCasingId = buf.readVarInt();

        int size = buf.readVarInt();
        this.replacements = new HashMap<>(size);

        for (int i = 0; i < size; i++) {
            int oldId = buf.readVarInt();
            int newId = buf.readVarInt();
            this.replacements.put(oldId, newId);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(controllerPos);
        buf.writeBoolean(mirrorMode);
        buf.writeVarInt(fillCasingId);

        buf.writeVarInt(replacements.size());
        for (Map.Entry<Integer, Integer> entry : replacements.entrySet()) {
            buf.writeVarInt(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Level level = player.level();
            MetaMachine machine = MetaMachine.getMachine(level, controllerPos);

            if (!(machine instanceof IMultiController controller)) {
                GTCEUTerminalMod.LOGGER.warn("Invalid multiblock controller at {}", controllerPos);
                return;
            }

            ItemStack wirelessTerminal = findWirelessTerminal(player);

            BlockReplacementData data = new BlockReplacementData();
            data.setMirrorMode(mirrorMode);

            if (fillCasingId >= 0) {
                BlockState fillCasing = Block.stateById(fillCasingId);
                if (fillCasing != null && !fillCasing.isAir()) {
                    data.setFillCasing(fillCasing);
                } else {
                    GTCEUTerminalMod.LOGGER.warn("CPacketBlockReplacement: invalid fillCasingId {} from {}",
                            fillCasingId, player.getName().getString());
                    return;
                }
            }

            for (Map.Entry<Integer, Integer> entry : replacements.entrySet()) {
                BlockState oldState = Block.stateById(entry.getKey());
                BlockState newState = Block.stateById(entry.getValue());

                if (oldState == null || oldState.isAir()) {
                    GTCEUTerminalMod.LOGGER.warn("CPacketBlockReplacement: invalid oldState id={} from {}",
                            entry.getKey(), player.getName().getString());
                    continue;
                }
                if (newState == null || newState.isAir()) {
                    GTCEUTerminalMod.LOGGER.warn("CPacketBlockReplacement: invalid newState id={} from {}",
                            entry.getValue(), player.getName().getString());
                    continue;
                }

                data.setReplacement(oldState, newState);
                data.addBlock(oldState);
            }

            boolean success = BlockReplacer.replaceBlocks(controller, player, data, wirelessTerminal);

            if (success) {
                GTCEUTerminalMod.LOGGER.info("Block replacement successful for player: {}",
                        player.getName().getString());
            } else {
                GTCEUTerminalMod.LOGGER.warn("Block replacement failed for player: {}",
                        player.getName().getString());
            }
        });

        ctx.get().setPacketHandled(true);
    }

    private ItemStack findWirelessTerminal(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof MultiStructureManagerItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof MultiStructureManagerItem) return player.getOffhandItem();

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof MultiStructureManagerItem) return stack;
        }

        if (MiscUtil.isCuriosLoaded) {
            for (ItemStack stack : CuriosCompat.getEquippedItems(player)) {
                if (stack.getItem() instanceof MultiStructureManagerItem) return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}