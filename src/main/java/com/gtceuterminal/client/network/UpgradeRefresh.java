package com.gtceuterminal.client.network;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.multiblock.MultiStructureManagerUI;

import net.minecraft.core.BlockPos;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = GTCEUTerminalMod.MOD_ID, value = Dist.CLIENT)
public final class UpgradeRefresh {

    private static final int DELAY_TICKS = 3;

    private static final List<Pending> PENDING = new ArrayList<>();

    private UpgradeRefresh() {
    }

    public static void schedule(BlockPos controllerPos) {
        PENDING.add(new Pending(controllerPos, DELAY_TICKS));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;

        var it = PENDING.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (--p.ticksLeft > 0) continue;
            it.remove();
            MultiStructureManagerUI.onUpgradeApplied(p.controllerPos);
        }
    }

    private static final class Pending {
        final BlockPos controllerPos;
        int ticksLeft;

        Pending(BlockPos controllerPos, int ticksLeft) {
            this.controllerPos = controllerPos;
            this.ticksLeft = ticksLeft;
        }
    }
}