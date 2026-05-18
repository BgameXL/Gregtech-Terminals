package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.common.multiblock.MultiblockInfo;
import com.gtceuterminal.common.multiblock.MultiblockStatus;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
final class MultiblockListPanel {

    private MultiblockListPanel() {}

    static WidgetGroup build(
            int panelX,
            int panelY,
            int panelW,
            int panelH,
            int entryH,
            int entryStep,
            int selectedIndex,
            List<MultiblockInfo> multiblocks,
            int colorBgDark,
            int colorBgMedium,
            int colorBgLight,
            int colorBorderDark,
            int colorBorderLight,
            int colorTextGray,
            int colorHover,
            Consumer<Integer> onEntryClick,
            Consumer<MultiblockInfo> onHighlight,
            Consumer<MultiblockInfo> onRename,
            DraggableScrollableWidgetGroup[] scrollRef
    ) {
        WidgetGroup panel = new WidgetGroup(panelX, panelY, panelW, panelH);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(colorBgDark),
                new ColorBorderTexture(1, colorBorderDark)));

        int scrollW = panelW - 14;
        DraggableScrollableWidgetGroup scroll =
                new DraggableScrollableWidgetGroup(2, 2, scrollW, panelH - 4);
        scroll.setYScrollBarWidth(8);
        scroll.setYBarStyle(
                new ColorRectTexture(colorBorderDark),
                new ColorRectTexture(colorBorderLight));
        scrollRef[0] = scroll;

        populateScroll(scroll, multiblocks, selectedIndex,
                scrollW, entryH, entryStep,
                colorBgMedium, colorBgLight, colorTextGray, colorHover,
                onEntryClick, onHighlight, onRename);

        panel.addWidget(scroll);
        return panel;
    }

    static void repopulate(
            DraggableScrollableWidgetGroup scroll,
            List<MultiblockInfo> multiblocks,
            int selectedIndex,
            int scrollW,
            int entryH,
            int entryStep,
            int colorBgMedium,
            int colorBgLight,
            int colorTextGray,
            int colorHover,
            Consumer<Integer> onEntryClick,
            Consumer<MultiblockInfo> onHighlight,
            Consumer<MultiblockInfo> onRename
    ) {
        scroll.clearAllWidgets();
        populateScroll(scroll, multiblocks, selectedIndex,
                scrollW, entryH, entryStep,
                colorBgMedium, colorBgLight, colorTextGray, colorHover,
                onEntryClick, onHighlight, onRename);
    }

    private static void populateScroll(
            DraggableScrollableWidgetGroup scroll,
            List<MultiblockInfo> multiblocks,
            int selectedIndex,
            int scrollW,
            int entryH,
            int entryStep,
            int colorBgMedium,
            int colorBgLight,
            int colorTextGray,
            int colorHover,
            Consumer<Integer> onEntryClick,
            Consumer<MultiblockInfo> onHighlight,
            Consumer<MultiblockInfo> onRename
    ) {
        if (multiblocks.isEmpty()) {
            LabelWidget empty = new LabelWidget(10, 12,
                    Component.translatable("gui.gtceuterminal.multiblock_manager.none_found").getString());
            empty.setTextColor(colorTextGray);
            scroll.addWidget(empty);
            return;
        }
        int entryW = scrollW - 4;
        int y = 2;
        for (int i = 0; i < multiblocks.size(); i++) {
            final int idx = i;
            MultiblockInfo mb = multiblocks.get(i);
            scroll.addWidget(buildEntry(mb, idx, y, entryW, entryH,
                    selectedIndex, colorBgMedium, colorBgLight, colorTextGray, colorHover,
                    onEntryClick, onHighlight, onRename));
            y += entryStep;
        }
    }

    private static WidgetGroup buildEntry(
            MultiblockInfo mb,
            int index,
            int yPos,
            int entryW,
            int entryH,
            int selectedIndex,
            int colorBgMedium,
            int colorBgLight,
            int colorTextGray,
            int colorHover,
            Consumer<Integer> onEntryClick,
            Consumer<MultiblockInfo> onHighlight,
            Consumer<MultiblockInfo> onRename
    ) {
        WidgetGroup entry = new WidgetGroup(0, yPos, entryW, entryH);
        entry.setBackground(new ColorRectTexture(index == selectedIndex ? colorBgLight : colorBgMedium));

        ButtonWidget clickBtn = new ButtonWidget(0, 0, entryW - 72, entryH,
                new ColorRectTexture(0x00000000), cd -> onEntryClick.accept(index));
        clickBtn.setHoverTexture(new ColorRectTexture(colorHover));
        entry.addWidget(clickBtn);

        MultiblockStatus status = mb.getStatus();
        entry.addWidget(new LabelWidget(6, 5, "§" + statusColor(status) + "● " + status.getDisplayName()));

        String rawName = mb.getName();
        String resolved = Component.translatable(rawName).getString();
        if (resolved.equals(rawName) && rawName.contains(".")) {
            String last = rawName.substring(rawName.lastIndexOf('.') + 1);
            resolved = Character.toUpperCase(last.charAt(0)) + last.substring(1).replace('_', ' ');
        }
        String displayName = truncate(resolved, 28);
        String nameText = mb.isVanillaGTCEu()
                ? "§f" + displayName
                : "§" + modColor(mb.getModColor()) + displayName;
        entry.addWidget(new LabelWidget(6, 16, nameText));

        LabelWidget dist = new LabelWidget(entryW - 70, 10, mb.getDistanceString());
        dist.setTextColor(colorTextGray);
        entry.addWidget(dist);

        ButtonWidget highlightBtn = new ButtonWidget(entryW - 48, 6, 20, 16,
                new ColorRectTexture(0x00000000), cd -> onHighlight.accept(mb));
        highlightBtn.setButtonTexture(new TextTexture("§e◉").setWidth(20).setType(TextTexture.TextType.NORMAL));
        highlightBtn.setHoverTexture(new ColorRectTexture(0x33FFFF00));
        highlightBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.multiblock_manager.highlight_tooltip").getString());
        entry.addWidget(highlightBtn);

        ButtonWidget renameBtn = new ButtonWidget(entryW - 26, 6, 20, 16,
                new ColorRectTexture(0x00000000), cd -> onRename.accept(mb));
        renameBtn.setButtonTexture(new TextTexture("§7✎").setWidth(20).setType(TextTexture.TextType.NORMAL));
        renameBtn.setHoverTexture(new ColorRectTexture(0x33FFFFFF));
        renameBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.multiblock_manager.rename_tooltip").getString());
        entry.addWidget(renameBtn);

        return entry;
    }

    private static char statusColor(MultiblockStatus s) {
        return switch (s) {
            case ACTIVE            -> 'a';
            case IDLE              -> 'e';
            case NEEDS_MAINTENANCE -> '6';
            case NO_POWER          -> 'c';
            case DISABLED          -> '8';
            case UNFORMED          -> 'c';
            case OUTPUT_FULL       -> 'b';
        };
    }

    private static char modColor(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        if (g > 200 && r < 100 && b < 100) return 'a';
        if (r > 200 && g > 200 && b < 100) return 'e';
        if (r < 100 && g > 200 && b > 200) return 'b';
        if (r > 200 && g < 100 && b > 200) return 'd';
        if (r > 200 && g > 100 && b < 50)  return '6';
        return '7';
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}