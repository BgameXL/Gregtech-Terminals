package com.gtceuterminal.common.scanner;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.autocraft.MultiblockAnalyzer;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

final class MultiblockBounds {

    private MultiblockBounds() {}

    static Set<BlockPos> getMultiblockBlocks(MultiblockControllerMachine controller, Level level) {
        Set<BlockPos> positions = new HashSet<>();

        Collection<BlockPos> cache = readCache(controller);
        if (cache != null && !cache.isEmpty()) {
            positions.addAll(cache);
        } else {
            positions.addAll(MultiblockAnalyzer.collectStructurePositions(controller, level));
        }

        try {
            for (var part : controller.getParts()) {
                if (part != null && part.self() != null) {
                    positions.add(part.self().getPos().immutable());
                }
            }
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("MultiblockBounds: getParts() failed: {}", t.toString());
        }
        positions.add(controller.getPos().immutable());

        return positions;
    }

    private static Collection<BlockPos> readCache(MultiblockControllerMachine controller) {
        try {
            var state = controller.getMultiblockState();
            if (state != null) {
                Collection<BlockPos> cache = state.getCache();
                if (cache != null && !cache.isEmpty()) return cache;
            }
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("MultiblockBounds: cache unavailable: {}", t.toString());
        }
        return null;
    }
}
