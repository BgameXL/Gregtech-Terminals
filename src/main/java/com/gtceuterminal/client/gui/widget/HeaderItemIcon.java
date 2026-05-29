package com.gtceuterminal.client.gui.widget;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.world.item.ItemStack;

public final class HeaderItemIcon {

    private HeaderItemIcon() {}

    public static WidgetGroup build(int x, int y, int size, ItemStack stack, int bgColor, int borderColor) {
        WidgetGroup icon = new WidgetGroup(x, y, size, size);
        icon.setBackground(new GuiTextureGroup(
                new ColorRectTexture(bgColor),
                new ColorBorderTexture(1, borderColor)));

        if (stack != null && !stack.isEmpty()) {
            ItemStack renderStack = stack.copy();
            renderStack.setCount(1);
            icon.addWidget(new ImageWidget(1, 1, size - 2, size - 2, new ItemStackTexture(renderStack)));
        }

        return icon;
    }
}
