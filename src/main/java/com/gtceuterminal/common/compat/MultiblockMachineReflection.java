package com.gtceuterminal.common.compat;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MultiblockMachineReflection {

    private static final String MWSD_CLASS = "com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData";
    private static final String STATE_CLASS = "com.gregtechceu.gtceu.api.pattern.MultiblockState";
    private static final String CONTROLLER_CLASS = "com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController";

    private MultiblockMachineReflection() {
    }

    public static void refreshController(ServerLevel level, BlockPos controllerPos) {
        if (level == null || controllerPos == null) return;

        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be != null && markStructureDirty(getMetaMachine(be), level)) {
            return;
        }

        var state = level.getBlockState(controllerPos);
        level.sendBlockUpdated(controllerPos, state, state, 3);
        level.updateNeighborsAt(controllerPos, state.getBlock());
        level.blockUpdated(controllerPos, state.getBlock());
    }

    private static Object getMetaMachine(BlockEntity be) {
        try {
            return be.getClass().getMethod("getMetaMachine").invoke(be);
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("MultiblockMachineReflection: getMetaMachine failed on {}", be.getClass().getSimpleName(), t);
            return null;
        }
    }

    private static boolean markStructureDirty(Object machine, ServerLevel level) {
        if (machine == null) return false;
        try {
            Object state = machine.getClass().getMethod("getMultiblockState").invoke(machine);
            machine.getClass().getMethod("onStructureInvalid").invoke(machine);

            Class<?> mwsdClass = Class.forName(MWSD_CLASS);
            Object mwsd = mwsdClass.getMethod("getOrCreate", ServerLevel.class).invoke(null, level);
            mwsdClass.getMethod("removeMapping", Class.forName(STATE_CLASS)).invoke(mwsd, state);
            mwsdClass.getMethod("addAsyncLogic", Class.forName(CONTROLLER_CLASS)).invoke(mwsd, machine);
            return true;
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("MultiblockMachineReflection: markStructureDirty failed", t);
            return false;
        }
    }
}
