package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.client.gui.widget.EnergyGraphWidget;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.config.ItemsConfig;
import com.gtceuterminal.common.energy.EnergySnapshot;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
final class EnergyAnalyzerDetail {

    static final int MAX_HATCHES = 5;

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

    private static final int GRAPH_H_WIDGET = 80;
    private static final int STATS_CELL_H   = 38;
    private static final int HATCH_ROW_H    = 24;

    EnergyAnalyzerDetail(int detailW, int graphW, int graphH) {
        this.detailW = detailW;
        this.graphW  = graphW;
        this.graphH  = graphH;
    }

    WidgetGroup build(
            int detailX, int headerH, int guiH, int pad,
            Supplier<EnergySnapshot> selSnap,
            Runnable onRecipeHistoryClick
    ) {
        int totalH = guiH - headerH;
        WidgetGroup g = new WidgetGroup(detailX, headerH, detailW, totalH);

        int y = pad;

        // Graphed window = history samples × seconds-per-sample (one sample per refresh interval).
        int windowSec = Math.max(1, Math.round(
                ItemsConfig.getEAHistorySeconds() * ItemsConfig.getEARefreshIntervalTicks() / 20f));
        LabelWidget graphHdr = new LabelWidget(pad, y, "§8EU/t — last " + windowSec + "s");
        graphHdr.setTextColor(0xFF666666);
        g.addWidget(graphHdr);
        y += 13;

        graphWidget = new EnergyGraphWidget(pad, y, graphW - pad, GRAPH_H_WIDGET, new long[0], new long[0]);
        g.addWidget(graphWidget);

        LabelWidget legIn = new LabelWidget(pad, y + GRAPH_H_WIDGET + 3, "§ain");
        legIn.setTextColor(0xFF00CC55);
        g.addWidget(legIn);
        LabelWidget legOut = new LabelWidget(pad + 24, y + GRAPH_H_WIDGET + 3, "§cout");
        legOut.setTextColor(0xFFDD3333);
        g.addWidget(legOut);
        y += GRAPH_H_WIDGET + 16;

        int gap = 4;
        int cellW = (detailW - pad * 2 - gap * 3) / 4;
        g.addWidget(buildStatCell(pad, y, cellW, STATS_CELL_H,
                "Avg Input",
                () -> { EnergySnapshot s = selSnap.get(); return s != null && s.isFormed ? EnergyDisplayHelper.formatEU(s.inputPerSec / 20) + "/t" : "---"; },
                C_GREEN));
        g.addWidget(buildStatCell(pad + (cellW + gap), y, cellW, STATS_CELL_H,
                "Avg Output",
                () -> { EnergySnapshot s = selSnap.get(); return s != null && s.isFormed ? EnergyDisplayHelper.formatEU(s.outputPerSec / 20) + "/t" : "---"; },
                C_RED));
        g.addWidget(buildStatCell(pad + (cellW + gap) * 2, y, cellW, STATS_CELL_H,
                "Peak", () -> peakText(selSnap.get()), C_GOLD));
        g.addWidget(buildStatCell(pad + (cellW + gap) * 3, y, cellW, STATS_CELL_H,
                "Stored", () -> storedPercent(selSnap.get()), C_BLUE));
        y += STATS_CELL_H + 8;

        g.addWidget(new ImageWidget(pad, y, detailW - pad * 2, 1, new ColorRectTexture(0xFF222222)));
        y += 6;

        LabelWidget progressHdr = new LabelWidget(pad, y, "§8Recipe Progress");
        progressHdr.setTextColor(0xFF666666);
        g.addWidget(progressHdr);
        LabelWidget progressPct = new LabelWidget(detailW - pad - 60, y, () -> recipeProgressText(selSnap.get()));
        progressPct.setClientSideWidget();
        progressPct.setTextColor(C_GREEN);
        g.addWidget(progressPct);
        y += 10;

        recipeBarBg = new WidgetGroup(pad, y, graphW - pad, 8);
        recipeBarBg.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF1A1A1A),
                new ColorBorderTexture(1, 0xFF333333)));
        g.addWidget(recipeBarBg);
        recipeBarFill = new WidgetGroup(pad + 1, y + 1, 0, 6);
        recipeBarFill.setBackground(new ColorRectTexture(C_GREEN));
        g.addWidget(recipeBarFill);
        y += 12;

        recipeBtn = TerminalButton.action(pad, y, 112, 18, "§7Recipe History",
                0xFF2A2A2A, 0xFF3A3A3A, cd -> onRecipeHistoryClick.run());
        g.addWidget(recipeBtn);
        y += 24;

        if (ItemsConfig.getEAShowPerHatchDetails()) {
            LabelWidget hatchesHdr = new LabelWidget(pad, y, () -> {
                EnergySnapshot s = selSnap.get();
                int count = s == null ? 0 : s.hatches.size();
                return "§8Energy Hatches (" + count + ")";
            });
            hatchesHdr.setClientSideWidget();
            hatchesHdr.setTextColor(0xFF666666);
            g.addWidget(hatchesHdr);
            y += 13;

            for (int i = 0; i < MAX_HATCHES; i++) {
                g.addWidget(buildHatchRow(pad, y + i * HATCH_ROW_H, detailW - pad * 2, i, selSnap));
            }
        }

        energyBarFill = null;

        return g;
    }

    private WidgetGroup buildStatCell(int x, int y, int w, int h,
                                      String lbl, Supplier<String> valSup, int valColor) {
        WidgetGroup cell = new WidgetGroup(x, y, w, h);
        cell.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF111111),
                new ColorBorderTexture(1, 0xFF1E1E1E)));

        LabelWidget label = new LabelWidget(6, 6, lbl);
        label.setTextColor(0xFF666666);
        cell.addWidget(label);

        LabelWidget val = new LabelWidget(6, 20, valSup);
        val.setClientSideWidget();
        val.setTextColor(valColor);
        cell.addWidget(val);

        return cell;
    }

    private WidgetGroup buildHatchRow(int x, int y, int w, int index,
                                      Supplier<EnergySnapshot> selSnap) {
        WidgetGroup row = new WidgetGroup(x, y, w, HATCH_ROW_H - 2);
        row.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF111111),
                new ColorBorderTexture(1, 0xFF1A1A1A)));

        WidgetGroup slot = new WidgetGroup(4, 1, 20, 20);
        slot.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF1A1A1A),
                new ColorBorderTexture(1, 0xFF333333)));
        ImageWidget icon = new ImageWidget(1, 1, 18, 18, hatchIconSupplier(index, selSnap));
        icon.setClientSideWidget();
        slot.addWidget(icon);
        row.addWidget(slot);

        LabelWidget nameLbl = new LabelWidget(30, 7, () -> {
            EnergySnapshot s = selSnap.get();
            if (s == null || index >= s.hatches.size()) return "";
            EnergySnapshot.HatchInfo h = s.hatches.get(index);
            String name = Component.translatable(h.blockNameKey()).getString();
            return "§f" + EnergyDisplayHelper.truncate(name, 28);
        });
        nameLbl.setClientSideWidget();
        nameLbl.setTextColor(0xFFCCCCCC);
        row.addWidget(nameLbl);

        LabelWidget euLbl = new LabelWidget(w - 88, 7, () -> {
            EnergySnapshot s = selSnap.get();
            if (s == null || index >= s.hatches.size()) return "";
            EnergySnapshot.HatchInfo h = s.hatches.get(index);
            long rate = Math.max(0, h.voltage() * h.amperage());
            return (h.isInput() ? "§a" : "§c") + EnergyDisplayHelper.formatEU(rate) + "/t";
        });
        euLbl.setClientSideWidget();
        euLbl.setTextColor(C_GREEN);
        row.addWidget(euLbl);

        return row;
    }

    private static final IGuiTexture EMPTY_TEX = new ColorRectTexture(0x00000000);

    private static Supplier<IGuiTexture> hatchIconSupplier(int index, Supplier<EnergySnapshot> selSnap) {
        final String[] lastId = { null };
        final IGuiTexture[] cached = { EMPTY_TEX };
        return () -> {
            EnergySnapshot s = selSnap.get();
            String id = (s != null && index < s.hatches.size()) ? s.hatches.get(index).blockId() : null;
            if (id == null || id.isBlank()) {
                lastId[0] = null;
                return cached[0] = EMPTY_TEX;
            }
            if (!id.equals(lastId[0])) {
                lastId[0] = id;
                cached[0] = hatchTexture(id);
            }
            return cached[0];
        };
    }

    private static IGuiTexture hatchTexture(String blockId) {
        try {
            Block b = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(blockId));
            if (b != null && b != Blocks.AIR) {
                ItemStack stack = new ItemStack(b);
                if (!stack.isEmpty()) return new ItemStackTexture(stack);
            }
        } catch (Exception ignored) {}
        return EMPTY_TEX;
    }

    private static String peakText(EnergySnapshot s) {
        if (s == null || !s.isFormed) return "---";
        long peak = 0;
        for (long v : s.inputHistory) peak = Math.max(peak, v);
        for (long v : s.outputHistory) peak = Math.max(peak, v);
        if (peak <= 0) peak = Math.max(s.inputPerSec, s.outputPerSec);
        return EnergyDisplayHelper.formatEU(peak / 20) + "/t";
    }

    private static String pct(int value) {
        return value + "%%";
    }

    private static String storedPercent(EnergySnapshot s) {
        if (s == null || !s.isFormed) return "---";
        return pct(Math.round(Math.max(0f, Math.min(1f, s.chargePercent())) * 100f));
    }

    private static String recipeProgressText(EnergySnapshot s) {
        if (s == null || !s.isFormed || !s.isRecipeActive) return "§8" + pct(0);
        return "§a" + pct(Math.round(Math.max(0f, Math.min(1f, s.recipeProgress)) * 100f));
    }

    void updateDynamic(EnergySnapshot snap) {
        if (energyBarFill != null) {
            float pct = snap != null ? snap.chargePercent() : 0f;
            int fw = (int) ((graphW - 12) * Math.min(1f, Math.max(0f, pct)));
            energyBarFill.setSize(new Size(Math.max(fw, 0), 6));
            if (snap != null) {
                int fc = pct < 0.1f ? C_RED : pct < 0.25f ? C_ORANGE : C_GREEN;
                energyBarFill.setBackground(new ColorRectTexture(fc));
            }
        }

        if (recipeBarFill != null && recipeBarBg != null) {
            float pct = (snap != null && snap.isFormed && snap.isRecipeActive)
                    ? Math.max(0f, Math.min(1f, snap.recipeProgress))
                    : 0f;
            int fw = (int) ((recipeBarBg.getSizeWidth() - 2) * pct);
            recipeBarFill.setSize(new Size(Math.max(0, fw), 6));
            recipeBarFill.setBackground(new ColorRectTexture(pct > 0f ? C_GREEN : 0xFF333333));
        }

        if (graphWidget != null) {
            graphWidget.updateData(
                    snap != null ? snap.inputHistory  : new long[0],
                    snap != null ? snap.outputHistory : new long[0]);
        }
    }
}