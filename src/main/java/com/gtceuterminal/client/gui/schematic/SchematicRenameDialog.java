package com.gtceuterminal.client.gui.schematic;

import com.gtceuterminal.client.gui.widget.TerminalRenameDialog;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
final class SchematicRenameDialog {

    private SchematicRenameDialog() {
    }

    static void open(WidgetGroup rootGroup, int parentW, int parentH,
                     String currentName, Consumer<String> onConfirm) {
        TerminalRenameDialog.open(rootGroup, parentW, parentH,
                "gui.gtceuterminal.schematic_interface.rename_dialog.title",
                null, currentName, null, onConfirm);
    }
}
