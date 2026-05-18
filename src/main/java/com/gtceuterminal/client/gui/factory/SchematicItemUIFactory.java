package com.gtceuterminal.client.gui.factory;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.compat.UIFactoryReflection;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SchematicItemUIFactory extends UIFactory<SchematicItemUIFactory.Holder> {

    public static final ResourceLocation UI_ID = ResourceLocation.fromNamespaceAndPath(
            GTCEUTerminalMod.MOD_ID, "schematic_interface_ui"
    );

    public static final SchematicItemUIFactory INSTANCE = new SchematicItemUIFactory();

    private SchematicItemUIFactory() {
        super(UI_ID);
    }

    public void openUI(ServerPlayer player, ItemStack item) {
        super.openUI(new Holder(false, item), player);
    }

    @Override
    protected ModularUI createUITemplate(Holder holder, Player entityPlayer) {
        holder.attach(entityPlayer);
        return UIFactoryReflection.invokeCreate(
                "com.gtceuterminal.client.gui.schematic.SchematicInterfaceUI",
                Holder.class, holder, entityPlayer);
    }

    @Override
    protected Holder readHolderFromSyncData(FriendlyByteBuf buf) {
        ItemStack item = buf.readItem();
        return new Holder(true, item);
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf buf, Holder holder) {
        buf.writeItem(holder.item);
    }

    public static class Holder implements IUIHolder {
        public final boolean remote;
        public final ItemStack item;
        private Player player;

        public Holder(boolean remote, ItemStack item) {
            this.remote = remote;
            this.item   = item;
        }

        public void attach(Player p) {
            if (this.player == null) this.player = p;
        }

        public Player getPlayer()       { return player; }
        public ItemStack getTerminalItem() { return item; }

        @Override public ModularUI createUI(Player p) { attach(p); return INSTANCE.createUITemplate(this, p); }
        @Override public boolean isInvalid()  { return player != null && player.isRemoved(); }
        @Override public boolean isRemote()   { return remote; }
        @Override public void markAsDirty()   { }
    }
}