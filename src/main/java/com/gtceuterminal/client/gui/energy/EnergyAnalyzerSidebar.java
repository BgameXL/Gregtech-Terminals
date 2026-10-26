package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.energy.EnergySnapshot;
import com.gtceuterminal.common.energy.LinkedMachineData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.IntFunction;

@OnlyIn(Dist.CLIENT)
final class EnergyAnalyzerSidebar {

    private EnergyAnalyzerSidebar() {
    }

    static WidgetGroup build(
            int x,
            int y,
            int sidebarW,
            int sidebarH,
            int pad,
            int selectedIndex,
            List<LinkedMachineData> machines,
            int itemH,
            IntFunction<EnergySnapshot> snapAt,
            java.util.function.IntConsumer onSelect,
            java.util.function.IntConsumer onGear,
            WidgetGroup[] selectionRefs
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup g = new WidgetGroup(x, y, sidebarW, sidebarH);
        g.setBackground(TerminalUI.surface());

        if (machines.isEmpty()) {
            LabelWidget lbl = new LabelWidget(pad, 16,
                    Component.translatable("gui.gtceuterminal.energy_analyzer.no_machines_linked").getString());
            lbl.setColor(tokens.textMuted());
            g.addWidget(lbl);
            return g;
        }

        for (int i = 0; i < machines.size(); i++) {
            final int idx = i;
            LinkedMachineData m = machines.get(i);
            int rowY = pad + idx * (itemH + 2);

            WidgetGroup sel = new WidgetGroup(1, rowY, sidebarW - 2, itemH);
            sel.setBackground(TerminalUI.outlined(tokens.accentSoft(), tokens.border()));
            sel.setVisible(idx == selectedIndex);
            selectionRefs[i] = sel;
            g.addWidget(sel);

            ImageWidget dot = new ImageWidget(pad + 2, rowY + 15, 7, 7,
                    TerminalUI.color(EnergyDisplayHelper.statusColor(
                            snapAt.apply(idx), tokens.error(), tokens.warning(), tokens.success())));
            g.addWidget(dot);

            LabelWidget name = new LabelWidget(pad + 12, rowY + 4,
                    () -> EnergyDisplayHelper.truncate(m.getDisplayNameComponent().getString(), 14));
            name.setClientSideWidget();
            name.setColor(tokens.textStrong());
            g.addWidget(name);

            LabelWidget status = new LabelWidget(pad + 12, rowY + 16, () -> statusText(snapAt.apply(idx)));
            status.setClientSideWidget();
            status.setColor(tokens.textMuted());
            g.addWidget(status);

            LabelWidget rate = new LabelWidget(pad + 12, rowY + 27, () -> {
                EnergySnapshot s = snapAt.apply(idx);
                return (s != null && s.isFormed) ? EnergyDisplayHelper.formatEU(s.inputPerSec / 20) + "/t" : "---";
            });
            rate.setClientSideWidget();
            rate.setColor(tokens.textMuted());
            g.addWidget(rate);

            ButtonWidget rowBtn = new ButtonWidget(1, rowY, sidebarW - 2, itemH,
                    TerminalUI.transparent(), cd -> onSelect.accept(idx));
            rowBtn.setHoverTexture(TerminalUI.hover());
            g.addWidget(rowBtn);

            ButtonWidget gearBtn = TerminalButton.icon3d(sidebarW - 21, rowY + 8, 18, TerminalButton.ICON_CONFIG,
                    cd -> onGear.accept(idx));
            gearBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.energy_analyzer.mode.config").getString());
            g.addWidget(gearBtn);
        }

        return g;
    }

    private static String statusText(EnergySnapshot s) {
        if (s == null || !s.isFormed) return "NO POWER";
        if (s.inputPerSec > 0 || s.outputPerSec > 0) return "ACTIVE";
        return "IDLE";
    }
}
