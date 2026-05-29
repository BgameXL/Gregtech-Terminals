package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.energy.EnergySnapshot;
import com.gtceuterminal.common.energy.LinkedMachineData;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

@OnlyIn(Dist.CLIENT)
final class EnergyAnalyzerSidebar {

    private EnergyAnalyzerSidebar() {}

    static WidgetGroup build(
            int sidebarW,
            int headerH,
            int guiH,
            int pad,
            int selectedIndex,
            List<LinkedMachineData> machines,
            int itemH,
            int colorPanel,
            int colorSel,
            int colorGray,
            int colorRed,
            int colorOrange,
            int colorGreen,
            IntFunction<EnergySnapshot> snapAt,
            java.util.function.IntConsumer onSelect,
            java.util.function.IntConsumer onGear,
            WidgetGroup[] selectionRefs
    ) {
        int semi = (colorPanel & 0x00FFFFFF) | 0xCC000000;
        WidgetGroup g = new WidgetGroup(0, headerH, sidebarW + 2, guiH - headerH);
        g.setBackground(new ColorRectTexture(semi));

        if (machines.isEmpty()) {
            LabelWidget lbl = new LabelWidget(pad, 16,
                    Component.translatable("gui.gtceuterminal.energy_analyzer.no_machines_linked").getString());
            lbl.setTextColor(colorGray);
            g.addWidget(lbl);
            return g;
        }

        for (int i = 0; i < machines.size(); i++) {
            final int idx = i;
            LinkedMachineData m = machines.get(i);
            int y = pad + idx * (itemH + 2);

            WidgetGroup sel = new WidgetGroup(2, y, sidebarW - 4, itemH);
            sel.setBackground(new ColorRectTexture(colorSel));
            sel.setVisible(idx == selectedIndex);
            selectionRefs[i] = sel;
            g.addWidget(sel);

            ImageWidget dot = new ImageWidget(pad + 2, y + 15, 7, 7,
                    new ColorRectTexture(EnergyDisplayHelper.statusColor(snapAt.apply(idx), colorRed, colorOrange, colorGreen)));
            g.addWidget(dot);

            LabelWidget name = new LabelWidget(pad + 12, y + 4,
                    () -> EnergyDisplayHelper.truncate(m.getDisplayNameComponent().getString(), 14));
            name.setClientSideWidget();
            name.setTextColor(0xFFDDDDDD);
            g.addWidget(name);

            LabelWidget status = new LabelWidget(pad + 12, y + 16, () -> statusText(snapAt.apply(idx)));
            status.setClientSideWidget();
            status.setTextColor(colorGray);
            g.addWidget(status);

            LabelWidget rate = new LabelWidget(pad + 12, y + 27, () -> {
                EnergySnapshot s = snapAt.apply(idx);
                return (s != null && s.isFormed) ? EnergyDisplayHelper.formatEU(s.inputPerSec / 20) + "/t" : "---";
            });
            rate.setClientSideWidget();
            rate.setTextColor(colorGray);
            g.addWidget(rate);

            ButtonWidget rowBtn = new ButtonWidget(2, y, sidebarW - 22, itemH,
                    new ColorRectTexture(0x00000000), cd -> onSelect.accept(idx));
            g.addWidget(rowBtn);

            ButtonWidget gearBtn = TerminalButton.ghostIcon(sidebarW - 20, y + 8, 16, "⚙", "§7",
                    cd -> onGear.accept(idx));
            g.addWidget(gearBtn);
        }

        return g;
    }

    private static String statusText(EnergySnapshot s) {
        if (s == null || !s.isFormed) return "§c● NO POWER";
        if (s.inputPerSec > 0 || s.outputPerSec > 0) return "§a● ACTIVE";
        return "§e● IDLE";
    }
}