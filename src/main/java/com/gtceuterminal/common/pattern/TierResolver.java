package com.gtceuterminal.common.pattern;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;
import com.gregtechceu.gtceu.common.block.BatteryBlock;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.config.ComponentEntry;
import com.gtceuterminal.common.config.ComponentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public final class TierResolver {

    private TierResolver() {
    }

    @Nullable
    public static Integer tierOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.getItem() instanceof BlockItem blockItem) return tierOf(blockItem.getBlock());
        return null;
    }

    @Nullable
    public static Integer tierOf(Block block) {
        if (block == null) return null;

        if (block instanceof CoilBlock coil && coil.coilType != null) {
            return coil.coilType.getTier();
        }

        if (block instanceof BatteryBlock battery && battery.getData() != null) {
            return battery.getData().getTier();
        }

        Integer machineTier = machineTierOf(block);
        if (machineTier != null) return machineTier;

        var key = ForgeRegistries.BLOCKS.getKey(block);
        if (key != null) {
            ComponentEntry entry = ComponentRegistry.entryByBlockId(key.toString());
            if (entry != null) return entry.tier();
        }

        return null;
    }

    @Nullable
    private static Integer machineTierOf(Block block) {
        if (!(block instanceof MetaMachineBlock machineBlock)) return null;
        try {
            if (machineBlock.newBlockEntity(BlockPos.ZERO, machineBlock.defaultBlockState())
                    instanceof IMachineBlockEntity mbe) {
                MetaMachine machine = mbe.getMetaMachine();
                if (machine instanceof ITieredMachine tiered) return tiered.getTier();
            }
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.debug("TierResolver: could not resolve machine tier for {}: {}",
                    block.getDescriptionId(), t.getMessage());
        }
        return null;
    }
}
