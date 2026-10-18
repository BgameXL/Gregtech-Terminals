package com.gtceuterminal.client.gui.energy;

import com.gtceuterminal.client.gui.style.TerminalText;
import com.gtceuterminal.client.gui.style.BevelTexture;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.widget.TerminalButton;

import com.gtceuterminal.common.energy.EnergySnapshot;
import com.gtceuterminal.common.energy.RecipeHistoryEntry;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class RecipeHistoryDialog {

    private RecipeHistoryDialog() {
    }

    public static void open(WidgetGroup rootGroup, EnergySnapshot snap, int parentW, int parentH) {
        TerminalTheme tokens = TerminalUI.tokens();
        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(Palette.SCRIM));
        dialog.setClickClose(true);

        int entries = snap.recipeHistory.size();
        int dW = 340;
        int dH = Math.min(300, 55 + Math.max(1, entries) * 14 + 20);
        int dX = (parentW - dW) / 2;
        int dY = (parentH - dH) / 2;

        WidgetGroup panel = new WidgetGroup(dX, dY, dW, dH);
        panel.setBackground(TerminalUI.cardBg());
        dialog.addWidget(panel);

        WidgetGroup titleBar = new WidgetGroup(0, 0, dW, 18);
        titleBar.setBackground(new BevelTexture(tokens.panel()));
        panel.addWidget(titleBar);

        String titleText = TerminalText.strip(Component.translatable(
                "gui.gtceuterminal.energy_analyzer.recipe_log.title",
                snap.getMachineTitle()).getString());
        LabelWidget titleLbl = new LabelWidget(8, 4,
                fitText(titleText, dW - 36));
        titleLbl.setColor(tokens.textStrong());
        titleBar.addWidget(titleLbl);

        ButtonWidget closeBtn = TerminalButton.icon3d(dW - 18, 1, 16, TerminalButton.ICON_CLOSE, cd -> dialog.close());
        closeBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        titleBar.addWidget(closeBtn);

        int cy = 22;
        panel.addWidget(mkLabel(18, cy,
                TerminalText.strip(Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.recipe_log.output").getString()), tokens.textMuted()));
        panel.addWidget(mkLabel(210, cy,
                TerminalText.strip(Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.recipe_log.time").getString()), tokens.textMuted()));
        panel.addWidget(mkLabel(275, cy,
                TerminalText.strip(Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.recipe_log.when").getString()), tokens.textMuted()));
        cy += 10;

        WidgetGroup sep = new WidgetGroup(0, cy, dW, 1);
        sep.setBackground(new ColorRectTexture(tokens.borderDark()));
        panel.addWidget(sep);
        cy += 4;

        if (snap.recipeHistory.isEmpty()) {
            panel.addWidget(mkLabel(10, cy,
                    TerminalText.strip(Component.translatable(
                            "gui.gtceuterminal.energy_analyzer.recipe_log.no_recipes").getString()),
                    tokens.textMuted()));
        } else {
            List<RecipeHistoryEntry> list = snap.recipeHistory;
            int shown = Math.min(list.size(), 17);
            for (int i = list.size() - 1; i >= list.size() - shown; i--) {
                RecipeHistoryEntry e = list.get(i);

                WidgetGroup dot = new WidgetGroup(8, cy + 3, 6, 6);
                dot.setBackground(new ColorRectTexture(e.completed ? tokens.success() : tokens.error()));
                panel.addWidget(dot);

                panel.addWidget(mkLabel(18, cy, EnergyDisplayHelper.truncate(e.outputName, 22),
                        e.completed ? tokens.textStrong() : tokens.textMuted()));
                panel.addWidget(mkLabel(210, cy, e.durationStr(), tokens.textMuted()));

                int ago = e.secondsAgo();
                String agoStr = ago < 60 ? ago + "s" : (ago / 60) + "m " + (ago % 60) + "s";
                panel.addWidget(mkLabel(275, cy, agoStr, tokens.textFaint()));
                cy += 13;
            }
        }

        LabelWidget hint = new LabelWidget(10, dH - 14,
                TerminalText.strip(Component.translatable(
                        "gui.gtceuterminal.energy_analyzer.recipe_log.hint_click_outside_close").getString()));
        hint.setColor(tokens.textFaint());
        panel.addWidget(hint);
    }

    private static LabelWidget mkLabel(int x, int y, String text, int color) {
        LabelWidget l = new LabelWidget(x, y, text);
        l.setColor(color);
        return l;
    }

    private static String fitText(String text, int maxWidth) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        return font.substrByWidth(Component.literal(text),
                Math.max(0, maxWidth - font.width(ellipsis))).getString() + ellipsis;
    }

}
