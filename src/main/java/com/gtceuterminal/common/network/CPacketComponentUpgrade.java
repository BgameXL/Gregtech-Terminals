package com.gtceuterminal.common.network;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.ae2.CuriosCompat;
import com.gtceuterminal.common.compat.MultiblockMachineReflection;
import com.gtceuterminal.common.item.MultiStructureManagerItem;
import com.gtceuterminal.common.multiblock.ComponentGroup;
import com.gtceuterminal.common.multiblock.ComponentGroupRegistry;
import com.gtceuterminal.common.multiblock.ComponentInfo;
import com.gtceuterminal.common.upgrade.ComponentUpgrader;
import com.gtceuterminal.common.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class CPacketComponentUpgrade {

    private List<BlockPos> positions = new ArrayList<>();
    private final int targetTier;
    private final String targetUpgradeId;
    private final BlockPos controllerPos;
    private ServerLevel level;

    public CPacketComponentUpgrade(List<BlockPos> positions, int targetTier, String targetUpgradeId, BlockPos controllerPos) {
        this.positions = positions;
        this.targetTier = targetTier;
        this.targetUpgradeId = targetUpgradeId;
        this.controllerPos = controllerPos;
    }

    public CPacketComponentUpgrade(BlockPos position, int targetTier, String targetUpgradeId, BlockPos controllerPos) {
        this.positions = new ArrayList<>();
        this.positions.add(position);
        this.targetTier = targetTier;
        this.targetUpgradeId = targetUpgradeId;
        this.controllerPos = controllerPos;
    }

    public CPacketComponentUpgrade(BlockPos position, int targetTier, BlockPos controllerPos) {
        this(position, targetTier, null, controllerPos);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(positions.size());
        for (BlockPos pos : positions) {
            buf.writeBlockPos(pos);
        }
        buf.writeInt(targetTier);
        buf.writeBlockPos(controllerPos);

        boolean hasId = targetUpgradeId != null && !targetUpgradeId.isBlank();
        buf.writeBoolean(hasId);
        if (hasId) buf.writeUtf(targetUpgradeId);
    }

    public static CPacketComponentUpgrade decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<BlockPos> positions = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        int targetTier = buf.readInt();
        BlockPos controllerPos = buf.readBlockPos();

        String upgradeId = null;
        boolean hasId = buf.readBoolean();
        if (hasId) {
            upgradeId = buf.readUtf(32767);
        }

        return new CPacketComponentUpgrade(positions, targetTier, upgradeId, controllerPos);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack wirelessTerminal = findWirelessTerminal(player);

            int upgraded = 0;
            int failed = 0;

            java.util.Set<BlockPos> seen = new java.util.HashSet<>();
            for (BlockPos pos : positions) {
                if (!seen.add(pos)) continue;
                var state = player.level().getBlockState(pos);
                var block = state.getBlock();

                ComponentGroup type = ComponentGroupRegistry.detectFromBlock(block);
                if (type == ComponentGroupRegistry.UNKNOWN) type = null;
                int currentTier = detectTier(block);

                boolean hasId = targetUpgradeId != null && !targetUpgradeId.isBlank();
                if (type == null && !hasId) {
                    failed++;
                    continue;
                }

                if (type == ComponentGroupRegistry.COIL) {
                }

                ComponentInfo component = new ComponentInfo(type, currentTier, pos, state);
                ComponentUpgrader.UpgradeResult result = ComponentUpgrader.upgradeComponent(
                        component,
                        targetTier,
                        targetUpgradeId,
                        player,
                        player.level(),
                        true,
                        wirelessTerminal
                );

                if (result.success) {
                    upgraded++;
                } else {
                    failed++;
                }
            }

            if (upgraded > 0) {
                player.displayClientMessage(
                        Component.translatable(
                                "item.gtceuterminal.component_upgrade.message.upgraded",
                                upgraded
                        ),
                        true
                );
                player.playSound(SoundEvents.ANVIL_USE, 1.0F, 1.0F);
            }

            if (upgraded > 0) {
                MultiblockMachineReflection.refreshController((ServerLevel) player.level(), controllerPos);
            }

            if (upgraded > 0) {
                player.displayClientMessage(
                        Component.translatable(
                                "item.gtceuterminal.component_upgrade.message.warning_void_or_dropped"
                        ),
                        false
                );
            }

            if (failed > 0) {
                player.displayClientMessage(
                        Component.translatable(
                                "item.gtceuterminal.component_upgrade.message.failed",
                                failed
                        ),
                        true
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // Manager-only: a manager upgrade resolves ME strictly from a Multi-Structure Manager. A linked
    // Schematic Interface (or the player's AE2 wireless terminal) must not bridge it to the network.
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

    private int detectTier(net.minecraft.world.level.block.Block block) {
        String blockId = block.builtInRegistryHolder().key().location().getPath().toLowerCase(Locale.ROOT);

        String[] tierTokens = {"ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "uhv", "uev", "uiv", "uxv", "opv", "max"};
        for (int i = tierTokens.length - 1; i >= 0; i--) {
            String token = tierTokens[i];
            if (blockId.equals(token) || blockId.startsWith(token + "_")
                    || blockId.contains("_" + token + "_") || blockId.endsWith("_" + token)) {
                return i;
            }
        }

        if (blockId.contains("cupronickel")) return 0;
        if (blockId.contains("kanthal")) return 1;
        if (blockId.contains("nichrome")) return 2;
        if (blockId.contains("rtm_alloy")) return 3;
        if (blockId.contains("hssg") || blockId.contains("hss_g")) return 4;
        if (blockId.contains("naquadah")) return 5;
        if (blockId.contains("trinium")) return 6;
        if (blockId.contains("tritanium")) return 7;

        return 0;
    }
}
