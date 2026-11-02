package com.gtceuterminal.common.gui.factory;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SchematicItemUIFactory {

    public static class Holder implements IUIHolder {
        public final boolean remote;
        public final ItemStack item;
        private Player player;

        public Holder(boolean remote, ItemStack item) {
            this.remote = remote;
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
        public ModularUI createUI(Player p) {
            attach(p);
            if (Platform.isClient()) {
                return com.gtceuterminal.client.gui.schematic.SchematicInterfaceUI.create(this, p);
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
