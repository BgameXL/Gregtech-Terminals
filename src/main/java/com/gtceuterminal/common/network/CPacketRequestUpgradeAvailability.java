package com.gtceuterminal.common.network;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.autocraft.AnalysisResult;
import com.gtceuterminal.common.autocraft.MultiblockAnalyzer;
import com.gtceuterminal.common.multiblock.ComponentGroup;
import com.gtceuterminal.common.multiblock.ComponentGroupRegistry;
import com.gtceuterminal.common.multiblock.ComponentInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CPacketRequestUpgradeAvailability {

    private static final class ComponentEntry {
        final BlockPos pos;
        final ComponentGroup group;
        final int tier;

        ComponentEntry(BlockPos pos, ComponentGroup group, int tier) {
            this.pos = pos;
            this.group = group;
            this.tier = tier;
        }
    }

    private final int requestId;
    private final List<ComponentEntry> components;
    private final int targetTier;
    private final String upgradeId;
    private final BlockPos controllerPos;

    public CPacketRequestUpgradeAvailability(int requestId,
                                             List<ComponentInfo> infos,
                                             int targetTier,
                                             String upgradeId,
                                             BlockPos controllerPos) {
        this.requestId = requestId;
        this.components = new ArrayList<>(infos.size());
        for (ComponentInfo ci : infos) {
            this.components.add(new ComponentEntry(ci.getPosition(), ci.getGroup(), ci.getTier()));
        }
        this.targetTier = targetTier;
        this.upgradeId = upgradeId != null ? upgradeId : "";
        this.controllerPos = controllerPos;
    }

    public CPacketRequestUpgradeAvailability(FriendlyByteBuf buf) {
        this.requestId = buf.readVarInt();
        this.controllerPos = buf.readBlockPos();
        this.targetTier = buf.readInt();
        this.upgradeId = buf.readUtf();
        int count = buf.readInt();
        this.components = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BlockPos pos = buf.readBlockPos();
            ComponentGroup group = ComponentGroupRegistry.byId(buf.readUtf());
            int tier = buf.readInt();
            components.add(new ComponentEntry(pos, group, tier));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(requestId);
        buf.writeBlockPos(controllerPos);
        buf.writeInt(targetTier);
        buf.writeUtf(upgradeId);
        buf.writeInt(components.size());
        for (ComponentEntry e : components) {
            buf.writeBlockPos(e.pos);
            buf.writeUtf(e.group.id);
            buf.writeInt(e.tier);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            try {
                List<ComponentInfo> infos = new ArrayList<>(components.size());
                for (ComponentEntry e : components) {
                    BlockState state = player.serverLevel().getBlockState(e.pos);
                    infos.add(new ComponentInfo(e.group, e.tier, e.pos, state));
                }

                if (infos.isEmpty()) return;

                String uid = upgradeId.isEmpty() ? null : upgradeId;
                AnalysisResult result = MultiblockAnalyzer.analyzeForUpgrade(
                        player, infos, targetTier, uid, controllerPos);
                if (result != null) {
                    TerminalNetwork.sendToPlayer(new SPacketUpgradeAvailability(requestId, result), player);
                }
            } catch (Exception e) {
                GTCEUTerminalMod.LOGGER.error("CPacketRequestUpgradeAvailability: error", e);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
