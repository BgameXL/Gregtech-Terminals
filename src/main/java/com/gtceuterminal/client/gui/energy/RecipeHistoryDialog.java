package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.common.energy.EnergySnapshot;
import com.gtceuterminal.common.energy.RecipeHistoryEntry;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;

import java.util.List;

// Dialog that shows the recipe execution history for a selected machine.
public final class RecipeHistoryDialog {

    private static final int C_GOLD  = 0xFFFFAA00;
    private static final int C_GRAY  = 0xFFAAAAAA;
    private static final int C_WHITE = 0xFFFFFFFF;

    private RecipeHistoryDialog() {}

    public static void open(WidgetGroup rootGroup, EnergySnapshot snap, int parentW, int parentH) {
        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(0xA0000000));
        dialog.setClickClose(true);

        int entries = snap.recipeHistory.size();
        int dW = 340;
        int dH = Math.min(300, 55 + Math.max(1, entries) * 14 + 20);
        int dX = (parentW - dW) / 2;
        int dY = (parentH - dH) / 2;

        WidgetGroup panel = new WidgetGroup(dX, dY, dW, dH);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF1E1E1E),
                ColorPattern.GRAY.borderTexture(-1)));
        dialog.addWidget(panel);

        WidgetGroup titleBar = new WidgetGroup(0, 0, dW, 18);
        titleBar.setBackground(new ColorRectTexture(0xFF2D2D2D));
        panel.addWidget(titleBar);

        LabelWidget titleLbl = new LabelWidget(8, 4,
                Component.translatable("gui.gtceuterminal.energy_analyzer.recipe_log.title",
                        snap.getMachineTitle()).getString());
        titleLbl.setTextColor(C_GOLD);
        titleBar.addWidget(titleLbl);

        ButtonWidget closeBtn = new ButtonWidget(dW - 18, 1, 16, 16,
                new ColorRectTexture(0x80FF0000), cd -> dialog.close());
        closeBtn.setButtonTexture(
                new TextTexture("§fX").setWidth(16).setType(TextTexture.TextType.NORMAL));
        closeBtn.setHoverTexture(new ColorRectTexture(0xFFFF3333));
        panel.addWidget(closeBtn);

        int cy = 22;
        panel.addWidget(mkLabel(18,  cy,
                Component.translatable("gui.gtceuterminal.energy_analyzer.recipe_log.output").getString(), 0xFF888888));
        panel.addWidget(mkLabel(210, cy,
                Component.translatable("gui.gtceuterminal.energy_analyzer.recipe_log.time").getString(), 0xFF888888));
        panel.addWidget(mkLabel(275, cy,
                Component.translatable("gui.gtceuterminal.energy_analyzer.recipe_log.when").getString(), 0xFF888888));
        cy += 10;

        WidgetGroup sep = new WidgetGroup(0, cy, dW, 1);
        sep.setBackground(new ColorRectTexture(0xFF333333));
        panel.addWidget(sep);
        cy += 4;

        if (snap.recipeHistory.isEmpty()) {
            panel.addWidget(mkLabel(10, cy,
                    Component.translatable("gui.gtceuterminal.energy_analyzer.recipe_log.no_recipes").getString(),
                    C_GRAY));
        } else {
            List<RecipeHistoryEntry> list = snap.recipeHistory;
            int shown = Math.min(list.size(), 17);
            for (int i = list.size() - 1; i >= list.size() - shown; i--) {
                RecipeHistoryEntry e = list.get(i);

                WidgetGroup dot = new WidgetGroup(8, cy + 3, 6, 6);
                dot.setBackground(new ColorRectTexture(e.completed ? 0xFF00CC55 : 0xFFDD3333));
                panel.addWidget(dot);

                panel.addWidget(mkLabel(18,  cy, EnergyDisplayHelper.truncate(e.outputName, 22),
                        e.completed ? C_WHITE : 0xFF888888));
                panel.addWidget(mkLabel(210, cy, "§7" + e.durationStr(), 0xFF888888));

                int ago = e.secondsAgo();
                String agoStr = ago < 60 ? ago + "s" : (ago / 60) + "m " + (ago % 60) + "s";
                panel.addWidget(mkLabel(275, cy, "§8" + agoStr, 0xFF555555));
                cy += 13;
            }
        }

        LabelWidget hint = new LabelWidget(dW / 2 - 50, dH - 14,
                Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.recipe_log.hint_click_outside_close").getString());
        hint.setTextColor(0xFF444444);
        panel.addWidget(hint);
    }

    private static LabelWidget mkLabel(int x, int y, String text, int color) {
        LabelWidget l = new LabelWidget(x, y, text);
        l.setTextColor(color);
        return l;
    }
}
