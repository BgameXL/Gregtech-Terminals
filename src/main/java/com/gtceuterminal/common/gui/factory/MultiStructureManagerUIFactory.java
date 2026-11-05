package com.gtceuterminal.common.gui.factory;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MultiStructureManagerUIFactory {

    public static final String MODE_MULTI = "multi";
    public static final String MODE_SETTINGS = "settings";

    public static class Holder implements IUIHolder {
        public final boolean remote;
        public final String mode;
        public final ItemStack item;
        private Player player;

        public Holder(boolean remote, String mode, ItemStack item) {
            this.remote = remote;
            this.mode = mode;
            this.item = item;
        }

        public void attach(Player p) {
            if (this.player == null) this.player = p;
        }

        public Player getPlayer() {
            return player;
        }

        public ItemStack getTerminalItem() {
            return item;
        }

        @Override
        public ModularUI createUI(Player entityPlayer) {
            attach(entityPlayer);
            if (Platform.isClient()) {
                return com.gtceuterminal.client.gui.multiblock.MultiStructureManagerUI.create(this, entityPlayer);
            }
            return new ModularUI(600, 480, this, entityPlayer);
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
