package com.gtceuterminal.client.gui.dismantler;

import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
import com.gtceuterminal.client.gui.widget.HeaderItemIcon;
import com.gtceuterminal.client.gui.widget.TerminalTabs;
import com.gtceuterminal.common.theme.ItemTheme;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class DismantlerHeader {

    private DismantlerHeader() {
    }

    static WidgetGroup build(
            int guiW,
            int headerH,
            BlockPos controllerPos,
            ItemStack itemStack,
            ItemTheme theme,
            WidgetGroup mainGroup,
            Runnable onClose
    ) {
        TerminalTheme tokens = TerminalUI.tokens();
        var font = Minecraft.getInstance().font;
        WidgetGroup g = new WidgetGroup(2, 2, guiW - 4, headerH);

        int iconSize = 20, padL = 6, padR = 6;
        int itemX = padL + iconSize + 6;
        int iconY = (headerH - iconSize) / 2;

        String name = itemStack.getHoverName().getString();
        String info = String.format("(%d, %d, %d)",
                controllerPos.getX(), controllerPos.getY(), controllerPos.getZ());

        int maxTitleW = g.getSizeWidth() - TerminalTabs.ACTION_CARD_W - 4;
        int titleW = Math.min(maxTitleW, itemX + Math.max(font.width(name), font.width(info)) + padR);
        int textMaxW = Math.max(0, titleW - padR - itemX);

        WidgetGroup titleCard = TerminalTabs.tabCard(0, 0, titleW, headerH, true, cd -> {
        });

        var bundleIcon = TerminalUI.bundleIcon();
        if (bundleIcon != null) {
            titleCard.addWidget(new ImageWidget(padL, iconY, iconSize, iconSize, bundleIcon));
        } else {
            titleCard.addWidget(HeaderItemIcon.build(padL, iconY, iconSize, itemStack,
                    tokens.panelDisabled(), tokens.border()));
        }

        LabelWidget nameLbl = new LabelWidget(itemX, 6, fitText(name, textMaxW));
        nameLbl.setColor(TerminalUI.itemLabelColor());
        titleCard.addWidget(nameLbl);

        LabelWidget infoLbl = new LabelWidget(itemX, 17, fitText(info, textMaxW));
        infoLbl.setColor(tokens.textMuted());
        titleCard.addWidget(infoLbl);

        g.addWidget(titleCard);

        g.addWidget(TerminalTabs.actionCard(g.getSizeWidth() - TerminalTabs.ACTION_CARD_W, 0, headerH,
                cd -> ThemeEditorDialog.open(mainGroup, ItemTheme.load(itemStack)),
                cd -> onClose.run()));
        return g;
    }

    private static String fitText(String text, int maxWidth) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        return font.substrByWidth(Component.literal(text),
                Math.max(0, maxWidth - font.width(ellipsis))).getString() + ellipsis;
    }
}
