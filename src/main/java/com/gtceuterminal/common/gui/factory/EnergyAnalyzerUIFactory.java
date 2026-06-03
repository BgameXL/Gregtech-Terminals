package com.gtceuterminal.common.gui.factory;

import com.gtceuterminal.GTCEUTerminalMod;
import com.lowdragmc.lowdraglib.Platform;
import com.gtceuterminal.common.energy.EnergyDataCollector;
import com.gtceuterminal.common.energy.EnergySnapshot;
import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.item.EnergyAnalyzerItem;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.gtceuterminal.client.gui.energy.EnergyUpdateWidget;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.gtceuterminal.common.theme.ItemTheme;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class EnergyAnalyzerUIFactory extends UIFactory<EnergyAnalyzerUIFactory.EnergyAnalyzerHolder> {

    public static final ResourceLocation UI_ID = ResourceLocation.fromNamespaceAndPath(
            GTCEUTerminalMod.MOD_ID, "energy_analyzer"
    );

    public static final EnergyAnalyzerUIFactory INSTANCE = new EnergyAnalyzerUIFactory();

    private EnergyAnalyzerUIFactory() {
        super(UI_ID);
    }

    public void openUI(ServerPlayer player, int initialIndex) {
        ItemStack stack = findAnalyzerItem(player);
        ItemTheme theme = ItemTheme.load(stack);
        List<EnergySnapshot> snapshots = new ArrayList<>();
        List<LinkedMachineData> machines = EnergyAnalyzerItem.loadMachines(stack);

        for (LinkedMachineData m : machines) {
            String dimId = m.getDimensionId();
            ServerLevel targetLevel = null;
            for (ServerLevel sl : player.getServer().getAllLevels()) {
                if (sl.dimension().location().toString().equals(dimId)) {
                    targetLevel = sl;
                    break;
                }
            }
            if (targetLevel != null) {
                snapshots.add(EnergyDataCollector.collect(
                        targetLevel, m.getPos(), m.getCustomName(), m.getControllerBlockKey()));
            } else {
                EnergySnapshot offline = new EnergySnapshot();
                m.applyToSnapshotIdentity(offline);
                offline.mode = EnergySnapshot.MachineMode.UNKNOWN;
                offline.isFormed = false;
                snapshots.add(offline);
            }
        }

        EnergyAnalyzerHolder holder = new EnergyAnalyzerHolder(false, stack, snapshots, machines, initialIndex, theme);
        super.openUI(holder, player);
    }

    // UIFactory impl
    @Override
    protected ModularUI createUITemplate(EnergyAnalyzerHolder holder, Player entityPlayer) {
        holder.attach(entityPlayer);
        GTCEUTerminalMod.LOGGER.info("=== EnergyAnalyzer createUITemplate isClient={} player={}", Platform.isClient(), entityPlayer);
        if (Platform.isClient()) {
            try {
                ModularUI ui = com.gtceuterminal.client.gui.energy.EnergyAnalyzerUI.create(holder, entityPlayer);
                GTCEUTerminalMod.LOGGER.info("=== EnergyAnalyzer UI created: {}", ui);
                return ui;
            } catch (Throwable t) {
                GTCEUTerminalMod.LOGGER.error("=== EnergyAnalyzer UI creation FAILED", t);
                return null;
            }
        }
        WidgetGroup serverRoot = new WidgetGroup(0, 0, 600, 480);
        serverRoot.addWidget(new EnergyUpdateWidget(holder));
        return new ModularUI(600, 480, holder, entityPlayer).widget(serverRoot);

    }

    @Override
    protected EnergyAnalyzerHolder readHolderFromSyncData(FriendlyByteBuf buf) {
        GTCEUTerminalMod.LOGGER.info("=== readHolderFromSyncData called");
        int index = buf.readInt();
        int count = buf.readInt();
        List<EnergySnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < count; i++) snapshots.add(EnergySnapshot.decode(buf));

        int mcount = buf.readInt();
        List<LinkedMachineData> machines = new ArrayList<>();
        for (int i = 0; i < mcount; i++) machines.add(LinkedMachineData.fromNBT(buf.readNbt()));

        ItemStack itemStack = buf.readItem();
        net.minecraft.nbt.CompoundTag themeTag = buf.readNbt();
        ItemTheme theme = ItemTheme.fromNBT(themeTag);
        return new EnergyAnalyzerHolder(true, itemStack, snapshots, machines, index, theme);
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf buf, EnergyAnalyzerHolder holder) {
        buf.writeInt(holder.initialIndex);
        buf.writeInt(holder.snapshots.size());
        for (EnergySnapshot s : holder.snapshots) s.encode(buf);
        buf.writeInt(holder.machines.size());
        for (LinkedMachineData m : holder.machines) buf.writeNbt(m.toNBT());
        buf.writeItem(holder.itemStack);
        buf.writeNbt(holder.theme.toNBT());
    }

    private static ItemStack findAnalyzerItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() instanceof EnergyAnalyzerItem) return s;
        }
        return ItemStack.EMPTY;
    }

    public static class EnergyAnalyzerHolder implements IUIHolder {
        private final boolean remote;
        private Player player;

        public final List<EnergySnapshot> snapshots;
        public final List<LinkedMachineData> machines;
        public final int initialIndex;
        public final ItemStack itemStack;
        public final ItemTheme theme;

        public EnergyAnalyzerHolder(boolean remote, ItemStack itemStack, List<EnergySnapshot> snapshots,
                                    List<LinkedMachineData> machines, int initialIndex, ItemTheme theme) {
            this.remote = remote;
            this.itemStack = itemStack != null ? itemStack : ItemStack.EMPTY;
            this.snapshots = snapshots;
            this.machines = machines;
            this.initialIndex = initialIndex;
            this.theme = theme != null ? theme : new ItemTheme();
        }

        public void attach(Player p) { if (this.player == null) this.player = p; }
        public Player getPlayer() { return player; }

        @Override public ModularUI createUI(Player p) { attach(p); return INSTANCE.createUITemplate(this, p); }
        @Override public boolean isInvalid() { return player != null && player.isRemoved(); }
        @Override public boolean isRemote() { return remote; }
        @Override public void markAsDirty() {}
    }
}