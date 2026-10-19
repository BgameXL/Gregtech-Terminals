package com.gtceuterminal.client.gui.widget;

import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TerminalScrollGroup extends DraggableScrollableWidgetGroup {

    public TerminalScrollGroup(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public int getScrollYOffset() {
        return scrollYOffset;
    }
}