package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.client.gui.style.BevelTexture;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.common.energy.LinkedMachineData;
import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.network.CPacketSetCustomMultiblockName;
import com.gtceuterminal.common.network.TerminalNetwork;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class MultiblockRenameDialog {

    private MultiblockRenameDialog() {
    }

    static void open(
            WidgetGroup rootGroup,
            int parentW,
            int parentH,
            MultiblockInfo mb,
            Player player,
            DraggableScrollableWidgetGroup listScroll,
            Runnable onConfirm
    ) {
        final int DW = 280, DH = 104;
        TerminalTheme tokens = TerminalUI.tokens();

        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(Palette.SCRIM));
        dialog.setClickClose(false);
        dialog.setOnClosed(() -> {
            if (listScroll != null) listScroll.setActive(true);
        });

        if (listScroll != null) listScroll.setActive(false);

        WidgetGroup panel = new WidgetGroup((parentW - DW) / 2, (parentH - DH) / 2, DW, DH);
        panel.setBackground(new BevelTexture(tokens.surface()));
        dialog.addWidget(panel);

        WidgetGroup titleBar = new WidgetGroup(0, 0, DW, 26);
        titleBar.setBackground(new ColorRectTexture(tokens.panel()));
        panel.addWidget(titleBar);

        LabelWidget titleLbl = new LabelWidget(8, 7,
                stripFormatting(Component.translatable(
                        "gui.gtceuterminal.multiblock_manager.rename_dialog.title").getString()));
        titleLbl.setColor(tokens.textStrong());
        titleBar.addWidget(titleLbl);

        ButtonWidget closeBtn = TerminalButton.icon3d(DW - 23, 4, 18, "×", cd -> dialog.close());
        closeBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        titleBar.addWidget(closeBtn);

        LabelWidget sub = new LabelWidget(8, 32, truncate(mb.getMachineTypeName(), 38));
        sub.setColor(tokens.textMuted());
        panel.addWidget(sub);

        TextFieldWidget textField = new TextFieldWidget(8, 46, DW - 16, 16, null, s -> {
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
        textField.setCurrentString(mb.getCustomDisplayName());
        panel.addWidget(textField);

        int btnW = (DW - 22) / 2;

        ButtonWidget cancelBtn = TerminalButton.text3d(8, 74, btnW, 18,
                Component.translatable("gui.gtceuterminal.multiblock_manager.rename_dialog.cancel").getString(),
                cd -> dialog.close());
        panel.addWidget(cancelBtn);

        ButtonWidget confirmBtn = TerminalButton.text3d(14 + btnW, 74, btnW, 18,
                Component.translatable("gui.gtceuterminal.multiblock_manager.rename_dialog.confirm").getString(),
                tokens.successSurface(),
                cd -> {
                    String dimId = LinkedMachineData.dimId(player.level());
                    String name = textField.getCurrentString().trim();
                    TerminalNetwork.CHANNEL.sendToServer(
                            new CPacketSetCustomMultiblockName(null, mb.posKey(dimId), name, name.isEmpty()));
                    mb.setCustomDisplayName(name.isEmpty() ? null : name);
                    dialog.close();
                    onConfirm.run();
                });
        panel.addWidget(confirmBtn);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("§.", "");
    }
}
