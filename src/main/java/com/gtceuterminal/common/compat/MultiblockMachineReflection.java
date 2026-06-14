package com.gtceuterminal.common.compat;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MultiblockMachineReflection {

    private MultiblockMachineReflection() {}

    public static void refreshController(ServerLevel level, BlockPos controllerPos) {
        if (level == null || controllerPos == null) return;
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be != null) {
            try {
                Object mm = getMetaMachine(be);
                if (mm != null) {
                    Object lock = acquirePatternLock(mm);
                    try {
                        invalidateStructure(mm);
                        checkAndRecheckPattern(mm);
                        return;
                    } finally {
                        releasePatternLock(lock);
                    }
                }
            } catch (Exception e) {
                GTCEUTerminalMod.LOGGER.error("MultiblockMachineReflection: refreshController failed at {}", controllerPos, e);
            }
        }

        var state = level.getBlockState(controllerPos);
        level.sendBlockUpdated(controllerPos, state, state, 3);
        level.updateNeighborsAt(controllerPos, state.getBlock());
        level.blockUpdated(controllerPos, state.getBlock());
    }

    public static Object getMetaMachine(BlockEntity be) {
        try {
            return be.getClass().getMethod("getMetaMachine").invoke(be);
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("MultiblockMachineReflection: getMetaMachine failed on {}", be.getClass().getSimpleName(), t);
            return null;
        }
    }

    public static Object acquirePatternLock(Object machine) {
        try {
            var getLock = machine.getClass().getMethod("getPatternLock");
            Object lock = getLock.invoke(machine);
            if (lock != null) lock.getClass().getMethod("lock").invoke(lock);
            return lock;
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("MultiblockMachineReflection: acquirePatternLock failed", t);
            return null;
        }
    }

    public static void releasePatternLock(Object lock) {
        if (lock == null) return;
        try { lock.getClass().getMethod("unlock").invoke(lock); }
        catch (Throwable t) { GTCEUTerminalMod.LOGGER.debug("MultiblockMachineReflection: releasePatternLock failed", t); }
    }

    public static void invalidateStructure(Object machine) {
        try { machine.getClass().getMethod("invalidateStructure").invoke(machine); }
        catch (Throwable t) { GTCEUTerminalMod.LOGGER.debug("MultiblockMachineReflection: invalidateStructure failed", t); }
    }

    public static boolean checkAndRecheckPattern(Object machine) {
        try {
            Object r = machine.getClass().getMethod("checkStructurePattern").invoke(machine);
            if (Boolean.TRUE.equals(r)) { notifyFormed(machine); return true; }
        } catch (Throwable ignored) {}

        try {
            Object r = machine.getClass().getMethod("checkPattern").invoke(machine);
            if (Boolean.TRUE.equals(r)) { notifyFormed(machine); return true; }
        } catch (Throwable ignored) {}

        notifyInvalid(machine);
        return false;
    }

    private static void notifyFormed(Object machine) {
        for (String name : new String[]{"onStructureFormed"}) {
            try { machine.getClass().getMethod(name).invoke(machine); return; }
            catch (Throwable ignored) {}
        }
    }

    private static void notifyInvalid(Object machine) {
        for (String name : new String[]{"onStructureInvalid", "onStructureInvalidated"}) {
            try { machine.getClass().getMethod(name).invoke(machine); return; }
            catch (Throwable ignored) {}
        }
    }


}