package com.gtceuterminal.common.energy;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MultiblockHandlerRegistry {

    private static final List<IMultiblockHandler> HANDLERS = new ArrayList<>();

    public static void register(IMultiblockHandler handler) {
        HANDLERS.add(handler);
    }

    public static Optional<EnergySnapshot> tryHandle(MetaMachine machine, ServerLevel level, BlockPos pos) {
        for (var handler : HANDLERS) {
            if (handler.canHandle(machine)) {
                return Optional.of(handler.collect(machine, level, pos));
            }
        }
        return Optional.empty();
    }
}