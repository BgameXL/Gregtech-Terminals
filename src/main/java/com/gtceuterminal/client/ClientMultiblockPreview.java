package com.gtceuterminal.client;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gtceuterminal.common.config.ManagerSettings;
import com.gtceuterminal.common.data.GTCEUTerminalItems;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.pattern.MultiblockGhost;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class ClientMultiblockPreview {

    private static int requestToken = 0;
    private static BlockPos cachedPos;
    private static int cachedKey;
    private static SchematicData cachedGhost;

    private ClientMultiblockPreview() {
    }

    public static void show(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        MetaMachine machine = MetaMachine.getMachine(minecraft.level, pos);
        if (!(machine instanceof MultiblockControllerMachine controller) || controller.isFormed()) return;

        ManagerSettings.AutoBuildSettings settings = resolveSettings(minecraft.player);
        int key = cacheKey(controller, settings);
        int duration = ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20;
        int token = ++requestToken;

        if (pos.equals(cachedPos) && key == cachedKey && cachedGhost != null) {
            MultiblockPreview.show(minecraft.level, pos, cachedGhost, duration);
            return;
        }

        MultiblockGhost.Snapshot snapshot =
                MultiblockGhost.captureSnapshot(minecraft.level, controller, settings);
        if (snapshot == null) return;

        BlockPos reqPos = pos.immutable();
        Thread thread = new Thread(() -> {
            Map<BlockPos, BlockState> blocks = MultiblockGhost.walk(snapshot);
            minecraft.execute(() -> {
                if (token != requestToken || minecraft.level == null || blocks.isEmpty()) return;
                SchematicData ghost = new SchematicData("preview", "", blocks, Map.of(), "north");
                cachedPos = reqPos;
                cachedKey = key;
                cachedGhost = ghost;
                MultiblockPreview.show(minecraft.level, reqPos, ghost, duration);
            });
        }, "gtceuterminal-ghost-build");
        thread.setDaemon(true);
        thread.start();
    }

    private static int cacheKey(MultiblockControllerMachine controller, ManagerSettings.AutoBuildSettings settings) {
        return java.util.Objects.hash(
                settings.expandRadius, settings.expandHeight, settings.tierMode,
                settings.noHatchMode, settings.mirrorMode, settings.repeatCount,
                controller.self().getFrontFacing(), controller.self().getUpwardsFacing(),
                controller.self().isFlipped());
    }

    private static ManagerSettings.AutoBuildSettings resolveSettings(Player player) {
        ItemStack held = player.getMainHandItem();
        if (!held.is(GTCEUTerminalItems.MULTI_STRUCTURE_MANAGER.get())) {
            held = player.getOffhandItem();
        }
        if (held.is(GTCEUTerminalItems.MULTI_STRUCTURE_MANAGER.get())) {
            return new ManagerSettings.Settings(held).toAutoBuildSettings();
        }
        return new ManagerSettings.AutoBuildSettings();
    }
}
