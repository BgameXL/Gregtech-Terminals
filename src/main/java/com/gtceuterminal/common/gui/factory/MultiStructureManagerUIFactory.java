package com.gtceuterminal.common.gui.factory;

import com.gtceuterminal.GTCEUTerminalMod;
import com.lowdragmc.lowdraglib.Platform;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MultiStructureManagerUIFactory
        extends UIFactory<MultiStructureManagerUIFactory.Holder> {

    public static final String MODE_MULTI    = "multi";
    public static final String MODE_SETTINGS = "settings";

    public static final ResourceLocation UI_ID = ResourceLocation.fromNamespaceAndPath(
            GTCEUTerminalMod.MOD_ID, "multi_structure_manager_ui"
    );

    public static final MultiStructureManagerUIFactory INSTANCE = new MultiStructureManagerUIFactory();

    private MultiStructureManagerUIFactory() {
        super(UI_ID);
    }

    // Server entry points
    public void openMultiStructure(ServerPlayer player, ItemStack item) {
        super.openUI(new Holder(false, MODE_MULTI, item), player);
    }

    public void openManagerSettings(ServerPlayer player, ItemStack item) {
        super.openUI(new Holder(false, MODE_SETTINGS, item), player);
    }

    // UIFactory impl
    @Override
    protected ModularUI createUITemplate(Holder holder, Player entityPlayer) {
        holder.attach(entityPlayer);
        GTCEUTerminalMod.LOGGER.info("=== MultiStructureManager createUITemplate isClient={} mode={} player={}", Platform.isClient(), holder.mode, entityPlayer);
        if (!Platform.isClient()) {
            return new ModularUI(600, 480, holder, entityPlayer);
        }
        try {
            ModularUI ui;
            if (MODE_MULTI.equals(holder.mode)) {
                ui = com.gtceuterminal.client.gui.multiblock.MultiStructureManagerUI.create(holder, entityPlayer);
            } else {
                ui = new com.gtceuterminal.client.gui.multiblock.ManagerSettingsUI(holder, entityPlayer).createUI();
            }
            GTCEUTerminalMod.LOGGER.info("=== MultiStructureManager UI created: {}", ui);
            return ui;
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.error("=== MultiStructureManager UI creation FAILED", t);
            return null;
        }
    }

    @Override
    protected Holder readHolderFromSyncData(FriendlyByteBuf buf) {
        String mode = buf.readUtf();
        ItemStack item = buf.readItem();
        return new Holder(true, mode, item);
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf buf, Holder holder) {
        buf.writeUtf(holder.mode);
        buf.writeItem(holder.item);
    }

    // Holder
    public static class Holder implements IUIHolder {
        public final boolean remote;
        public final String mode;
        public final ItemStack item;
        private Player player;

        public Holder(boolean remote, String mode, ItemStack item) {
            this.remote = remote;
            this.mode   = mode;
            this.item   = item;
        }

        public void attach(Player p) {
            if (this.player == null) this.player = p;
        }

        public Player getPlayer() { return player; }

        public ItemStack getTerminalItem() { return item; }

        @Override
        public ModularUI createUI(Player entityPlayer) {
            attach(entityPlayer);
            return INSTANCE.createUITemplate(this, entityPlayer);
        }

        @Override public boolean isInvalid() { return player != null && player.isRemoved(); }
        @Override public boolean isRemote()  { return remote; }
        @Override public void markAsDirty()  { }
    }
}