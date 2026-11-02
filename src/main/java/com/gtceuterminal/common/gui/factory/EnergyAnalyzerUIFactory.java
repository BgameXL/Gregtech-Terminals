package com.gtceuterminal.common.gui.factory;

import com.gtceuterminal.common.energy.EnergySnapshot;
import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.theme.ItemTheme;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class EnergyAnalyzerUIFactory {

    public static class EnergyAnalyzerHolder implements IUIHolder {
        public final List<EnergySnapshot> snapshots;
        public final List<LinkedMachineData> machines;
        public final int initialIndex;
        public final ItemStack itemStack;
        public final ItemTheme theme;
        private final boolean remote;
        private Player player;

        public EnergyAnalyzerHolder(boolean remote, ItemStack itemStack, List<EnergySnapshot> snapshots,
                                    List<LinkedMachineData> machines, int initialIndex, ItemTheme theme) {
            this.remote = remote;
            this.itemStack = itemStack != null ? itemStack : ItemStack.EMPTY;
            this.snapshots = snapshots;
            this.machines = machines;
            this.initialIndex = initialIndex;
            this.theme = theme != null ? theme : new ItemTheme();
        }

        public void attach(Player p) {
            if (this.player == null) this.player = p;
        }

        public Player getPlayer() {
            return player;
        }

        @Override
        public ModularUI createUI(Player p) {
            attach(p);
            if (Platform.isClient()) {
                return com.gtceuterminal.client.gui.energy.EnergyAnalyzerUI.create(this, p);
            }
            return new ModularUI(600, 480, this, p);
        }

        @Override
        public boolean isInvalid() {
            return player != null && player.isRemoved();
        }

        @Override
        public boolean isRemote() {
            return remote;
        }

        @Override
        public void markAsDirty() {
        }
    }
}
