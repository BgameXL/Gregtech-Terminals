package com.gtceuterminal.common.compat;

import com.gtceuterminal.GTCEUTerminalMod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public final class MultiblockMachineReflection {

    private MultiblockMachineReflection() {}

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

    public static Optional<BlockPos> getLinkedControllerPos(Object partMachine) {
        try {
            Object p = partMachine.getClass().getMethod("getControllerPos").invoke(partMachine);
            if (p instanceof BlockPos bp) return Optional.of(bp);
        } catch (Throwable ignored) {}

        try {
            Object ctrl = partMachine.getClass().getMethod("getController").invoke(partMachine);
            if (ctrl != null) {
                Object p = ctrl.getClass().getMethod("getPos").invoke(ctrl);
                if (p instanceof BlockPos bp) return Optional.of(bp);
            }
        } catch (Throwable ignored) {}

        return Optional.empty();
    }
}