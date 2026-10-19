package com.gtceuterminal.client.gui.widget;

import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalText;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class TerminalRenameDialog {

    private static final int DW = 280;
    private static final int TITLE_H = 26;
    private static final int SUBTITLE_MAX = 38;

    private TerminalRenameDialog() {
    }

    public static void open(WidgetGroup rootGroup, int parentW, int parentH,
                            String titleKey, String subtitle, String initialValue,
                            DraggableScrollableWidgetGroup listToDisable,
                            Consumer<String> onConfirm) {
        TerminalTheme tokens = TerminalUI.tokens();
        boolean hasSubtitle = subtitle != null && !subtitle.isEmpty();
        int fieldY = hasSubtitle ? 46 : 34;
        int buttonY = fieldY + 28;
        int dh = buttonY + 30;

        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(Palette.SCRIM));
        dialog.setClickClose(false);
        if (listToDisable != null) {
            listToDisable.setActive(false);
            dialog.setOnClosed(() -> listToDisable.setActive(true));
        }

        WidgetGroup panel = new WidgetGroup((parentW - DW) / 2, (parentH - dh) / 2, DW, dh);
        panel.setBackground(TerminalUI.cardBg());
        dialog.addWidget(panel);

        WidgetGroup titleBar = new WidgetGroup(0, 0, DW, TITLE_H);
        titleBar.setBackground(new ColorRectTexture(tokens.panel()));
        panel.addWidget(titleBar);

        LabelWidget titleLbl = new LabelWidget(8, 7,
                TerminalText.strip(Component.translatable(titleKey).getString()));
        titleLbl.setColor(tokens.textStrong());
        titleBar.addWidget(titleLbl);

        ButtonWidget closeBtn = TerminalButton.icon3d(DW - 23, 4, 18, TerminalButton.ICON_CLOSE, cd -> dialog.close());
        closeBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        titleBar.addWidget(closeBtn);

        if (hasSubtitle) {
            LabelWidget sub = new LabelWidget(8, 32, TerminalText.truncate(subtitle, SUBTITLE_MAX));
            sub.setColor(tokens.textMuted());
            panel.addWidget(sub);
        }

        TextFieldWidget textField = new TextFieldWidget(8, fieldY, DW - 16, 16, null, s -> {
        });
        textField.setMaxStringLength(32);
        textField.setBordered(false);
        textField.setTextColor(tokens.textStrong());
        textField.setBackground(new GuiTextureGroup(
                new ColorRectTexture(tokens.panelDisabled()),
                new ColorBorderTexture(1, tokens.border())));
        textField.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(tokens.panelDisabled()),
                new ColorBorderTexture(1, tokens.textStrong())));
        textField.setCurrentString(initialValue == null ? "" : initialValue);
        panel.addWidget(textField);

        int btnW = (DW - 22) / 2;
        ButtonWidget cancelBtn = TerminalButton.text3d(8, buttonY, btnW, 18,
                Component.translatable("gui.gtceuterminal.rename_dialog.cancel").getString(),
                cd -> dialog.close());
        panel.addWidget(cancelBtn);

        ButtonWidget confirmBtn = TerminalButton.text3d(14 + btnW, buttonY, btnW, 18,
                Component.translatable("gui.gtceuterminal.rename_dialog.confirm").getString(),
                tokens.successSurface(),
                cd -> {
                    String name = textField.getCurrentString().trim();
                    dialog.close();
                    onConfirm.accept(name);
                });
        panel.addWidget(confirmBtn);
    }
}
