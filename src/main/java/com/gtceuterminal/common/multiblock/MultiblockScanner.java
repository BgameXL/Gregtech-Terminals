package com.gtceuterminal.common.multiblock;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.scanner.UniversalMultiblockScanner;
import com.gtceuterminal.common.scanner.UniversalMultiblockScanner.DetectedMultiblock;
import com.gtceuterminal.common.scanner.UniversalMultiblockScanner.ComponentData;

import com.gtceuterminal.common.item.MultiStructureManagerItem;
import com.gtceuterminal.common.energy.LinkedMachineData;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Multiblock Scanner
 * Detects multiblocks from ANY mod/modpack that uses the GTCEu API, without needing hardcoded support.
 */
public class MultiblockScanner {

    public static List<MultiblockInfo> scanNearbyMultiblocks(Player player, Level level, int radius) {
        List<MultiblockInfo> multiblocks = new ArrayList<>();
        BlockPos playerPos = player.blockPosition();
        Vec3 playerVec = player.position();


        List<DetectedMultiblock> detected = UniversalMultiblockScanner.scanForAllMultiblocks(
                level,
                playerPos,
                radius
        );

        // GTCEUTerminalMod.LOGGER.info("Universal scanner found {} multiblocks", detected.size());
        for (DetectedMultiblock mb : detected) {
            try {
                MultiblockInfo info = convertToMultiblockInfo(mb, playerVec, level);
                multiblocks.add(info);
            } catch (Exception e) {
                GTCEUTerminalMod.LOGGER.error("Error converting multiblock {}: {}",
                        mb.getName(), e.getMessage());
            }
        }

        applyCustomNames(player, multiblocks);

        multiblocks.sort((a, b) -> Double.compare(a.getDistanceFromPlayer(), b.getDistanceFromPlayer()));

        int maxDetected = com.gtceuterminal.common.config.ItemsConfig.getMgrMaxDetectedMultiblocks();
        if (multiblocks.size() > maxDetected) {
            multiblocks = new ArrayList<>(multiblocks.subList(0, maxDetected));
        }

        long uniqueMods = detected.stream()
                .map(DetectedMultiblock::getModId)
                .distinct()
                .count();
        return multiblocks;
    }

    private static void applyCustomNames(Player player, List<MultiblockInfo> multiblocks) {
        if (multiblocks.isEmpty()) return;

        ItemStack msmStack = findMSMItem(player);
        if (msmStack == null || msmStack.isEmpty()) return;

        CompoundTag itemTag = msmStack.getTag();
        if (itemTag == null || !itemTag.contains("CustomMultiblockNames")) return;

        CompoundTag names = itemTag.getCompound("CustomMultiblockNames");
        if (names.isEmpty()) return;

        String dimId = LinkedMachineData.dimId(player.level());
        for (MultiblockInfo mb : multiblocks) {
            String key = mb.posKey(dimId);
            if (names.contains(key)) {
                String custom = names.getString(key);
                if (!custom.isBlank()) {
                    mb.setCustomDisplayName(custom);
                }
            }
        }
    }

    private static ItemStack findMSMItem(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (!s.isEmpty() && s.getItem() instanceof MultiStructureManagerItem) return s;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof MultiStructureManagerItem) return s;
        }
        return null;
    }

    private static MultiblockInfo convertToMultiblockInfo(
            DetectedMultiblock detected,
            Vec3 playerPos,
            Level level
    ) {
        BlockPos controllerPos = detected.getPosition();
        double distance = playerPos.distanceTo(Vec3.atCenterOf(controllerPos));

        /**
         * DEBUG
         GTCEUTerminalMod.LOGGER.info("Multiblock: {}", detected.getName());
         */

        MultiblockInfo info = new MultiblockInfo(
                detected.getController(),
                detected.getName(),
                controllerPos,
                detected.getTier(),
                distance,
                true
        );

        info.setSourceMod(detected.getModId());

        try {
            Set<BlockPos> allPos = com.gtceuterminal.common.scanner.UniversalMultiblockScanner
                    .getMultiblockBlocks(detected.getController(), level);
            if (allPos != null && !allPos.isEmpty()) {
                info.setAllBlockPositions(allPos);
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("Failed to pre-compute block positions for '{}': {}",
                    detected.getName(), e.getMessage());
        }

        for (var entry : detected.getComponents().entrySet()) {
            String category = entry.getKey();
            List<ComponentData> components = entry.getValue();

            for (ComponentData comp : components) {
                ComponentInfo componentInfo = createComponentInfo(category, comp, level);
                if (componentInfo != null) {
                    info.addComponent(componentInfo);
                }
            }
        }

        return info;
    }


    private static ComponentInfo createComponentInfo(String category, ComponentData comp, Level level) {
        try {
            ComponentGroup group = ComponentGroupRegistry.fromCategory(category);
            BlockPos pos = comp.getPosition();
            BlockState state = level.getBlockState(pos);
            return new ComponentInfo(group, comp.getTier(), pos, state);
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("Could not create component info for {}: {}", category, e.getMessage());
            return null;
        }
    }

    public static Set<BlockPos> getMultiblockBlocks(IMultiController controller) {
        Set<BlockPos> blocks = new java.util.HashSet<>();
        blocks.add(controller.self().getPos());
        try {
            for (var part : controller.getParts()) {
                blocks.add(part.self().getPos());
            }
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("getMultiblockBlocks: failed to get parts for {}", controller.self().getPos());
        }
        return blocks;
    }
}