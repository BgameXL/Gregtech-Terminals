package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.common.config.ItemsConfig;
import com.gtceuterminal.common.energy.EnergyDataCollector;
import com.gtceuterminal.common.energy.EnergySnapshot;
import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.gui.factory.EnergyAnalyzerUIFactory;
import com.gtceuterminal.common.network.CPacketRequestEnergyUpdate;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnergyUpdateWidget extends Widget {

    private static final int UPDATE_ID = 1;
    private static EnergyUpdateWidget activeClient;

    private final EnergyAnalyzerUIFactory.EnergyAnalyzerHolder holder;

    private Runnable rebuildCallback;

    public EnergyUpdateWidget(EnergyAnalyzerUIFactory.EnergyAnalyzerHolder holder) {
        super(0, 0, 0, 0);
        this.holder = holder;
    }

    public static void onServerUpdate(List<EnergySnapshot> fresh) {
        EnergyUpdateWidget w = activeClient;
        if (w != null) w.applyUpdate(fresh);
    }

    public void setRebuildCallback(Runnable callback) {
        this.rebuildCallback = callback;
        activeClient = this;
    }

    private void applyUpdate(List<EnergySnapshot> fresh) {
        int cap = Math.max(2, ItemsConfig.getEAHistorySeconds() * 20);
        List<EnergySnapshot> prev = holder.snapshots;
        for (int i = 0; i < fresh.size(); i++) {
            EnergySnapshot s = fresh.get(i);
            EnergySnapshot p = i < prev.size() ? prev.get(i) : null;
            long[] prevIn = p != null ? p.inputHistory : new long[0];
            long[] prevOut = p != null ? p.outputHistory : new long[0];
            long prevTick = p != null ? p.clientLastTick : 0L;
            if (s.sampleTick > prevTick) {
                s.inputHistory = appendCapped(prevIn, s.inputPerSec, cap);
                s.outputHistory = appendCapped(prevOut, s.outputPerSec, cap);
                s.clientLastTick = s.sampleTick;
            } else {
                s.inputHistory = prevIn;
                s.outputHistory = prevOut;
                s.clientLastTick = prevTick;
            }
        }
        holder.snapshots.clear();
        holder.snapshots.addAll(fresh);
        if (rebuildCallback != null) rebuildCallback.run();
    }

    private static long[] appendCapped(long[] prev, long value, int cap) {
        int n = Math.min(prev.length + 1, cap);
        long[] out = new long[n];
        int skip = prev.length + 1 - n;
        System.arraycopy(prev, skip, out, 0, prev.length - skip);
        out[n - 1] = value;
        return out;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        TerminalNetwork.sendToServer(new CPacketRequestEnergyUpdate());
    }

    @Override
    public void detectAndSendChanges() {
        if (gui == null) return;
        ServerPlayer player = (ServerPlayer) gui.entityPlayer;
        if (player == null) return;

        List<EnergySnapshot> snapshots = collectSnapshots(player);

        writeUpdateInfo(UPDATE_ID, buf -> {
            buf.writeInt(snapshots.size());
            for (EnergySnapshot s : snapshots) s.encode(buf);
        });
    }

    private List<EnergySnapshot> collectSnapshots(ServerPlayer player) {
        List<LinkedMachineData> machines = holder.machines;
        List<EnergySnapshot> result = new ArrayList<>(machines.size());
        Map<String, ServerLevel> levelsByDim = new HashMap<>();
        for (ServerLevel sl : player.getServer().getAllLevels()) {
            levelsByDim.put(sl.dimension().location().toString(), sl);
        }

        for (LinkedMachineData m : machines) {
            ServerLevel targetLevel = levelsByDim.get(m.getDimensionId());
            if (targetLevel != null) {
                result.add(EnergyDataCollector.collect(
                        targetLevel, m.getPos(), m.getCustomName(), m.getControllerBlockKey()));
            } else {
                EnergySnapshot offline = new EnergySnapshot();
                m.applyToSnapshotIdentity(offline);
                offline.mode = EnergySnapshot.MachineMode.UNKNOWN;
                offline.isFormed = false;
                result.add(offline);
            }
        }
        return result;
    }

    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        if (id != UPDATE_ID) return;

        int count = buffer.readInt();
        List<EnergySnapshot> fresh = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            fresh.add(EnergySnapshot.decode(buffer));
        }

        applyUpdate(fresh);
    }
}