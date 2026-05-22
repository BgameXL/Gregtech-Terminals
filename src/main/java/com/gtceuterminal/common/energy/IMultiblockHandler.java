package com.gtceuterminal.common.energy;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface IMultiblockHandler {
    boolean canHandle(MetaMachine machine);
    EnergySnapshot collect(MetaMachine machine, ServerLevel level, BlockPos pos);
}