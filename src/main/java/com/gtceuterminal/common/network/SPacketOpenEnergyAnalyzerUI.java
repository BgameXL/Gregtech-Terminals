package com.gtceuterminal.common.network;

import com.gtceuterminal.common.energy.EnergyDataCollector;
import com.gtceuterminal.common.energy.EnergySnapshot;
import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.item.EnergyAnalyzerItem;
import com.gtceuterminal.common.theme.ItemTheme;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SPacketOpenEnergyAnalyzerUI {

    public final int initialIndex;
    public final List<EnergySnapshot> snapshots;
    public final List<LinkedMachineData> machines;
    public final ItemStack itemStack;
    public final ItemTheme theme;

    public SPacketOpenEnergyAnalyzerUI(int initialIndex, List<EnergySnapshot> snapshots,
                                       List<LinkedMachineData> machines, ItemStack itemStack, ItemTheme theme) {
        this.initialIndex = initialIndex;
        this.snapshots = snapshots != null ? snapshots : List.of();
        this.machines = machines != null ? machines : List.of();
        this.itemStack = itemStack != null ? itemStack : ItemStack.EMPTY;
        this.theme = theme != null ? theme : new ItemTheme();
    }

    public SPacketOpenEnergyAnalyzerUI(FriendlyByteBuf buf) {
        this.initialIndex = buf.readInt();

        int count = buf.readInt();
        List<EnergySnapshot> decodedSnapshots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            decodedSnapshots.add(EnergySnapshot.decode(buf));
        }
        this.snapshots = decodedSnapshots;

        int machineCount = buf.readInt();
        List<LinkedMachineData> decodedMachines = new ArrayList<>(machineCount);
        for (int i = 0; i < machineCount; i++) {
            decodedMachines.add(LinkedMachineData.fromNBT(buf.readNbt()));
        }
        this.machines = decodedMachines;

        this.itemStack = buf.readItem();
        this.theme = ItemTheme.fromNBT(buf.readNbt());
    }

    public static void encode(SPacketOpenEnergyAnalyzerUI msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.initialIndex);
        buf.writeInt(msg.snapshots.size());
        for (EnergySnapshot snapshot : msg.snapshots) {
            snapshot.encode(buf);
        }
        buf.writeInt(msg.machines.size());
        for (LinkedMachineData machine : msg.machines) {
            buf.writeNbt(machine.toNBT());
        }
        buf.writeItem(msg.itemStack);
        buf.writeNbt(msg.theme.toNBT());
    }

    public static void openFor(ServerPlayer player, int initialIndex) {
        ItemStack stack = findAnalyzerItem(player);
        ItemTheme theme = ItemTheme.load(stack);
        List<EnergySnapshot> snapshots = new ArrayList<>();
        List<LinkedMachineData> machines = EnergyAnalyzerItem.loadMachines(stack);

        for (LinkedMachineData machine : machines) {
            String dimId = machine.getDimensionId();
            ServerLevel targetLevel = null;
            for (ServerLevel serverLevel : player.getServer().getAllLevels()) {
                if (serverLevel.dimension().location().toString().equals(dimId)) {
                    targetLevel = serverLevel;
                    break;
                }
            }

            if (targetLevel != null) {
                snapshots.add(EnergyDataCollector.collect(
                        targetLevel, machine.getPos(), machine.getCustomName(), machine.getControllerBlockKey()));
            } else {
                EnergySnapshot offline = new EnergySnapshot();
                machine.applyToSnapshotIdentity(offline);
                offline.mode = EnergySnapshot.MachineMode.UNKNOWN;
                offline.isFormed = false;
                snapshots.add(offline);
            }
        }

        TerminalNetwork.sendToPlayer(new SPacketOpenEnergyAnalyzerUI(initialIndex, snapshots, machines, stack, theme), player);
    }

    private static ItemStack findAnalyzerItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof EnergyAnalyzerItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void handle(SPacketOpenEnergyAnalyzerUI msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.gtceuterminal.client.network.ClientPacketHandlers.handleOpenEnergyAnalyzer(
                        msg.initialIndex, msg.snapshots, msg.machines, msg.itemStack, msg.theme)));
        context.setPacketHandled(true);
    }
}
