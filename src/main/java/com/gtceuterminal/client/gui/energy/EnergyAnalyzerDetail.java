package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.client.gui.widget.EnergyGraphWidget;
import com.gtceuterminal.common.energy.EnergySnapshot;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
final class EnergyAnalyzerDetail {

    static final int MAX_HATCHES = 5;

    // refs returned to the orchestrator for dynamic updates
    EnergyGraphWidget graphWidget;
    WidgetGroup energyBarFill;
    WidgetGroup recipeBarFill;
    WidgetGroup recipeBarBg;
    ButtonWidget recipeBtn;

    private final int detailW;
    private final int graphW;
    private final int graphH;

    private static final int C_WHITE  = 0xFFFFFFFF;
    private static final int C_GRAY   = 0xFFAAAAAA;
    private static final int C_GOLD   = 0xFFFFAA00;
    private static final int C_GREEN  = 0xFF55FF55;
    private static final int C_RED    = 0xFFFF5555;
    private static final int C_BLUE   = 0xFF5599FF;
    private static final int C_ORANGE = 0xFFFF8800;

    EnergyAnalyzerDetail(int detailW, int graphW, int graphH) {
        this.detailW = detailW;
        this.graphW  = graphW;
        this.graphH  = graphH;
    }

    WidgetGroup build(
            int detailX,
            int headerH,
            int guiH,
            int pad,
            Supplier<EnergySnapshot> selSnap,
            Runnable onRecipeHistoryClick
    ) {
        WidgetGroup g = new WidgetGroup(detailX, headerH, detailW, guiH - headerH);
        int y = pad;

        LabelWidget titleLbl = dynLabel(0, y, () -> {
            EnergySnapshot s = selSnap.get();
            return s != null ? s.getMachineTitle().getString() : "";
        }, C_WHITE);
        g.addWidget(titleLbl); y += 14;

        LabelWidget badge = dynLabel(0, y, () -> {
            EnergySnapshot s = selSnap.get();
            return s != null ? EnergyDisplayHelper.modeBadge(s) : "";
        }, C_BLUE);
        g.addWidget(badge); y += 16;

        LabelWidget storedLbl = dynLabel(0, y, () -> {
            EnergySnapshot s = selSnap.get();
            return (s != null && s.isFormed)
                    ? Component.translatable("gui.gtceuterminal.energy_analyzer.storage_label",
                            EnergyDisplayHelper.storedStr(s)).getString()
                    : "";
        }, C_WHITE);
        g.addWidget(storedLbl); y += 13;

        WidgetGroup barBg = new WidgetGroup(0, y, detailW - 4, 8);
        barBg.setBackground(new ColorRectTexture(0xFF333333));
        g.addWidget(barBg);

        energyBarFill = new WidgetGroup(0, y, 0, 8);
        energyBarFill.setBackground(new ColorRectTexture(C_GREEN));
        g.addWidget(energyBarFill); y += 10;

        LabelWidget lowBuf = dynLabel(0, y, () -> {
            EnergySnapshot s = selSnap.get();
            return (s != null && s.isFormed && s.chargePercent() < 0.10f && s.energyCapacity > 0)
                    ? Component.translatable("gui.gtceuterminal.energy_analyzer.low_buffer").getString()
                    : "";
        }, C_RED);
        g.addWidget(lowBuf); y += 12;

        LabelWidget etaLbl = dynLabel(0, y, () -> {
            EnergySnapshot s = selSnap.get();
            if (s == null || !s.isFormed) return "";
            long secs = s.secondsUntilChange();
            if (secs < 0 || secs >= 86400) return "";
            String t = EnergyDisplayHelper.formatTime(secs);
            return (s.netPerSec() > 0)
                    ? Component.translatable("gui.gtceuterminal.energy_analyzer.eta_full_in", t).getString()
                    : Component.translatable("gui.gtceuterminal.energy_analyzer.eta_empty_in", t).getString();
        }, C_GRAY);
        g.addWidget(etaLbl); y += 12;

        LabelWidget inL = dynLabel(0, y, () -> {
            EnergySnapshot s = selSnap.get();
            if (s == null || !s.isFormed) return "";
            return Component.translatable("gui.gtceuterminal.energy_analyzer.in_line",
                    EnergyDisplayHelper.formatEU(s.inputPerSec / 20),
                    EnergyDisplayHelper.formatEU(s.inputPerSec)).getString();
        }, C_GREEN);
        g.addWidget(inL); y += 12;

        LabelWidget outL = dynLabel(0, y, () -> {
            EnergySnapshot s = selSnap.get();
            if (s == null || !s.isFormed) return "";
            return Component.translatable("gui.gtceuterminal.energy_analyzer.out_line",
                    EnergyDisplayHelper.formatEU(s.outputPerSec / 20),
                    EnergyDisplayHelper.formatEU(s.outputPerSec)).getString();
        }, C_RED);
        g.addWidget(outL); y += 12;

        LabelWidget netL = dynLabel(0, y, () -> {
            EnergySnapshot s = selSnap.get();
            if (s == null || !s.isFormed) return "";
            long net = s.netPerSec();
            return Component.translatable("gui.gtceuterminal.energy_analyzer.net_line",
                    (net >= 0 ? "+" : "") + EnergyDisplayHelper.formatEU(net)).getString();
        }, C_GRAY);
        g.addWidget(netL); y += 12;

        LabelWidget volL = dynLabel(0, y, () -> {
            EnergySnapshot s = selSnap.get();
            if (s == null || !s.isFormed || s.inputVoltage <= 0) return "";
            return Component.translatable("gui.gtceuterminal.energy_analyzer.voltage_line",
                    EnergyDisplayHelper.getVoltageTier(s.inputVoltage),
                    s.inputVoltage, s.inputAmperage).getString();
        }, C_BLUE);
        g.addWidget(volL); y += 14;

        recipeBtn = new ButtonWidget(0, y, 95, 10,
                new ColorRectTexture(0x00000000), cd -> onRecipeHistoryClick.run());
        recipeBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.energy_analyzer.recipe_button").getString())
                .setWidth(95).setType(TextTexture.TextType.LEFT));
        recipeBtn.setHoverTexture(new ColorRectTexture(0x22FFFFFF));
        recipeBtn.setVisible(false);
        g.addWidget(recipeBtn); y += 12;

        LabelWidget recipeNameL = dynLabel(0, y, () -> {
            EnergySnapshot s = selSnap.get();
            if (s == null || !s.isRecipeActive || s.recipeId.isEmpty()) return "";
            return EnergyDisplayHelper.truncate(s.recipeId, 38);
        }, C_WHITE);
        g.addWidget(recipeNameL); y += 12;

        recipeBarBg = new WidgetGroup(0, y, graphW, 6);
        recipeBarBg.setBackground(new ColorRectTexture(0x00000000));
        g.addWidget(recipeBarBg);

        recipeBarFill = new WidgetGroup(0, y, 0, 6);
        recipeBarFill.setBackground(new ColorRectTexture(0xFF4488FF));
        g.addWidget(recipeBarFill); y += 9;

        LabelWidget recipePctL = dynLabel(0, y, () -> {
            EnergySnapshot s = selSnap.get();
            if (s == null || !s.isRecipeActive || s.recipeDuration == 0) return "";
            int pct      = (int) (s.recipeProgress * 100);
            int secsLeft = Math.max(0, (s.recipeDuration - s.recipeProgressTicks) / 20);
            int secsDone = s.recipeProgressTicks / 20;
            int secsTotal = s.recipeDuration / 20;
            String timeStr = secsLeft > 0
                    ? Component.translatable("gui.gtceuterminal.energy_analyzer.recipe_time_left",
                            EnergyDisplayHelper.formatTime(secsLeft)).getString()
                    : Component.translatable("gui.gtceuterminal.energy_analyzer.recipe_finishing").getString();
            return pct + "%  " + timeStr + "  (" + secsDone + "s / " + secsTotal + "s)";
        }, C_GRAY);
        g.addWidget(recipePctL); y += 14;

        LabelWidget hatchHdr = dynLabel(0, y, () -> {
            EnergySnapshot s = selSnap.get();
            return (s != null && !s.hatches.isEmpty())
                    ? Component.translatable("gui.gtceuterminal.energy_analyzer.hatches_label").getString()
                    : "";
        }, C_GOLD);
        g.addWidget(hatchHdr); y += 12;

        for (int h = 0; h < MAX_HATCHES; h++) {
            final int hi = h;
            LabelWidget hatchL = dynLabel(0, y, () -> {
                EnergySnapshot s = selSnap.get();
                if (s == null || hi >= s.hatches.size()) return "";
                EnergySnapshot.HatchInfo hatch = s.hatches.get(hi);
                String inOut = hatch.isInput()
                        ? Component.translatable("gui.gtceuterminal.energy_analyzer.hatch_in").getString()
                        : Component.translatable("gui.gtceuterminal.energy_analyzer.hatch_out").getString();
                String ampSuffix = hatch.amperage() > 1
                        ? Component.translatable("gui.gtceuterminal.energy_analyzer.hatch_amp_suffix",
                                hatch.amperage()).getString()
                        : "";
                String blockKey = hatch.blockNameKey();
                if (blockKey == null || blockKey.isBlank()) return "";
                return Component.translatable("gui.gtceuterminal.energy_analyzer.hatch_line",
                        inOut, Component.translatable(blockKey), hatch.voltage(), ampSuffix).getString();
            }, C_GREEN);
            g.addWidget(hatchL); y += 11;
        }

        LabelWidget moreHatches = dynLabel(0, y, () -> {
            EnergySnapshot s = selSnap.get();
            if (s == null || s.hatches.size() <= MAX_HATCHES) return "";
            return Component.translatable("gui.gtceuterminal.energy_analyzer.more_hatches",
                    s.hatches.size() - MAX_HATCHES).getString();
        }, C_GRAY);
        g.addWidget(moreHatches);

        buildGraph(g, guiH, headerH, pad, selSnap);

        return g;
    }

    private void buildGraph(WidgetGroup g, int guiH, int headerH, int pad, Supplier<EnergySnapshot> selSnap) {
        final int graphBlockH  = 13 + graphH + 12;
        final int graphSectionY = (guiH - headerH) - graphBlockH - pad;
        final int graphY        = graphSectionY + 13;

        LabelWidget graphHdr = new LabelWidget(0, graphSectionY,
                Component.translatable("gui.gtceuterminal.energy_analyzer.graph_header").getString());
        graphHdr.setTextColor(C_GOLD);
        g.addWidget(graphHdr);

        graphWidget = new EnergyGraphWidget(0, graphY, graphW, graphH, new long[0], new long[0]);
        g.addWidget(graphWidget);

        for (int i = 0; i <= 4; i++) {
            final int ti = i;
            LabelWidget axisL = dynLabel(graphW + 3, graphY + (graphH * i) / 4 - 4, () -> {
                EnergySnapshot s = selSnap.get();
                if (s == null) return "";
                long max = 1;
                for (long v : s.inputHistory)  max = Math.max(max, v);
                for (long v : s.outputHistory) max = Math.max(max, v);
                long val = (max / 20) - ((max / 20 * ti) / 4);
                return EnergyDisplayHelper.formatEU(val) + "/t";
            }, 0xFF666666);
            g.addWidget(axisL);
        }

        LabelWidget legIn = new LabelWidget(0, graphY + graphH + 5,
                Component.translatable("gui.gtceuterminal.energy_analyzer.legend_in").getString());
        legIn.setTextColor(0xFF00CC55);
        g.addWidget(legIn);

        LabelWidget legOut = new LabelWidget(34, graphY + graphH + 5,
                Component.translatable("gui.gtceuterminal.energy_analyzer.legend_out").getString());
        legOut.setTextColor(0xFFDD3333);
        g.addWidget(legOut);
    }

    void updateDynamic(EnergySnapshot snap) {
        if (energyBarFill != null) {
            float pct = snap != null ? snap.chargePercent() : 0f;
            int fw = (int) ((detailW - 4) * Math.min(1f, Math.max(0f, pct)));
            energyBarFill.setSize(new Size(Math.max(fw, 0), 8));
            if (snap != null) {
                int fc = pct < 0.1f ? C_RED : pct < 0.25f ? C_ORANGE : C_GREEN;
                energyBarFill.setBackground(new ColorRectTexture(fc));
            }
        }

        boolean recipeActive = snap != null && snap.isRecipeActive;
        if (recipeBtn != null) recipeBtn.setVisible(recipeActive);
        if (recipeBarBg != null)
            recipeBarBg.setBackground(new ColorRectTexture(recipeActive ? 0xFF333333 : 0x00000000));
        if (recipeBarFill != null) {
            int rw = recipeActive ? (int) (graphW * Math.min(1f, Math.max(0f, snap.recipeProgress))) : 0;
            recipeBarFill.setSize(new Size(Math.max(rw, 0), 6));
        }

        if (graphWidget != null) {
            graphWidget.updateData(
                    snap != null ? snap.inputHistory  : new long[0],
                    snap != null ? snap.outputHistory : new long[0]);
        }
    }

    private static LabelWidget dynLabel(int x, int y, Supplier<String> sup, int color) {
        LabelWidget l = new LabelWidget(x, y, sup);
        l.setClientSideWidget();
        l.setTextColor(color);
        return l;
    }
}