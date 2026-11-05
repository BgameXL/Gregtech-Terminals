package com.gtceuterminal.common.gui.factory;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class DismantlerItemUIFactory {

    public static class Holder implements IUIHolder {
        public final boolean remote;
        public final ItemStack item;
        public final BlockPos controllerPos;
        public final Set<BlockPos> scannedPositions;
        private Player player;

        public Holder(boolean remote, ItemStack item, BlockPos controllerPos, Set<BlockPos> scannedPositions) {
            this.remote = remote;
            this.item = item;
            this.controllerPos = controllerPos;
            this.scannedPositions = scannedPositions;
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
                return com.gtceuterminal.client.gui.dismantler.DismantlerUI.create(this, p);
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