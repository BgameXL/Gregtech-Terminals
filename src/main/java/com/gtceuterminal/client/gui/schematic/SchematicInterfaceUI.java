package com.gtceuterminal.client.gui.schematic;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
import com.gtceuterminal.client.gui.widget.HeaderItemIcon;
import com.gtceuterminal.client.gui.widget.WallpaperWidget;
import com.gtceuterminal.common.gui.factory.SchematicItemUIFactory;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.network.CPacketSchematicAction;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SchematicInterfaceUI {

    private static final int GUI_W    = 600;
    private static final int GUI_H    = 480;
    private static final int HEADER_H = 34;
    private static final int LEFT_W   = 202;
    private static final int DIVIDER  = 1;

    private static final int BODY_Y   = HEADER_H;
    private static final int BODY_H   = GUI_H - HEADER_H;
    private static final int RIGHT_X  = LEFT_W + DIVIDER;
    private static final int RIGHT_W  = GUI_W - RIGHT_X;

    private static final int C_BORDER_DARK = 0xFF0A0A0A;
    private static final int C_TEXT_WHITE  = 0xFFFFFFFF;
    private static final int C_TEXT_GRAY   = 0xFFAAAAAA;
    private static final int C_HOVER       = 0x40FFFFFF;
    private static final int C_SUCCESS     = 0xFF4CAF50;
    private static final int C_INFO        = 0xFF42A5F5;
    private static final int C_ERROR       = 0xFFFF5252;

    private final IUIHolder uiHolder;
    private final ItemStack terminalItem;
    private final Player    player;
    private final ItemTheme theme;
    private final int colorBorderLight;

    private List<SchematicData> schematics = new ArrayList<>();
    private int selectedIndex = -1;

    private ModularUI gui;
    private WidgetGroup mainGroup;
    private WidgetGroup rightPanel;
    private WidgetGroup bodyGroup;
    private TextFieldWidget nameInput;
    private DraggableScrollableWidgetGroup schematicsScroll;

    public SchematicInterfaceUI(HeldItemUIFactory.HeldItemHolder heldHolder) {
        this.uiHolder     = heldHolder;
        this.terminalItem = heldHolder.held;
        this.player       = heldHolder.player;
        this.theme        = ItemTheme.load(terminalItem);
        this.colorBorderLight = theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF);
        loadSchematics();
    }

    public SchematicInterfaceUI(SchematicItemUIFactory.Holder holder, Player player) {
        this.uiHolder     = holder;
        this.terminalItem = holder.getTerminalItem();
        this.player       = player;
        this.theme        = ItemTheme.load(terminalItem);
        this.colorBorderLight = theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF);
        loadSchematics();
    }

    private void loadSchematics() {
        schematics = new ArrayList<>();
        CompoundTag tag = terminalItem.getTag();
        if (tag != null && tag.contains("SavedSchematics")) {
            ListTag list = tag.getList("SavedSchematics", 10);
            for (int i = 0; i < list.size(); i++) {
                SchematicData data = SchematicData.fromNBT(list.getCompound(i), player.level().registryAccess());
                if (!"Clipboard".equals(data.getName())) schematics.add(data);
            }
        }
        if (!schematics.isEmpty() && selectedIndex < 0) selectedIndex = 0;
        GTCEUTerminalMod.LOGGER.info("Loaded {} schematics", schematics.size());
    }

    public ModularUI createUI() {
        this.mainGroup = new WidgetGroup(0, 0, GUI_W, GUI_H);
        mainGroup.setBackground(new ColorRectTexture(0x00000000));

        if (!theme.isNativeStyle()) {
            mainGroup.addWidget(new WallpaperWidget(0, 0, GUI_W, GUI_H, () -> this.theme));
        }

        mainGroup.addWidget(buildOuterBorder());
        mainGroup.addWidget(buildHeader());
        mainGroup.addWidget(buildBody());

        setupParade();
        this.gui = buildModularUI(mainGroup);
        return gui;
    }

    private WidgetGroup buildOuterBorder() {
        WidgetGroup g = new WidgetGroup(0, 0, GUI_W, GUI_H);
        g.addWidget(new ImageWidget(0,         0,          GUI_W, 2,     new ColorRectTexture(colorBorderLight)));
        g.addWidget(new ImageWidget(0,         0,          2,     GUI_H, new ColorRectTexture(colorBorderLight)));
        g.addWidget(new ImageWidget(GUI_W - 2, 0,          2,     GUI_H, new ColorRectTexture(C_BORDER_DARK)));
        g.addWidget(new ImageWidget(0,         GUI_H - 2,  GUI_W, 2,     new ColorRectTexture(C_BORDER_DARK)));
        return g;
    }

    private WidgetGroup buildHeader() {
        WidgetGroup header = new WidgetGroup(0, 0, GUI_W, HEADER_H);
        header.setBackground(theme.headerTexture());

        int titleX = 12;
        int iconSize = 20, iconY = (HEADER_H - iconSize) / 2;
        if (theme.isBundleStyle()) {
            com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                    com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
            if (bundle != null) {
                com.lowdragmc.lowdraglib.gui.texture.IGuiTexture bundleIcon = bundle.iconTexture();
                if (bundleIcon != null) {
                    header.addWidget(new ImageWidget(titleX, iconY, iconSize, iconSize, bundleIcon));
                    titleX += iconSize + 4;
                }
            }
        } else {
            header.addWidget(HeaderItemIcon.build(titleX, iconY, iconSize, terminalItem,
                    theme.isNativeStyle() ? 0xFF2A2A2A : theme.accent(0x55),
                    colorBorderLight));
            titleX += iconSize + 6;
        }

        LabelWidget title = new LabelWidget(titleX, 11,
                Component.translatable("gui.gtceuterminal.schematic_interface.title").getString());
        title.setTextColor(theme.isBundleStyle() ? theme.labelColor() : C_TEXT_WHITE);
        header.addWidget(title);

        int btnY = (HEADER_H - 16) / 2;

        ButtonWidget gearBtn = TerminalButton.ghostIcon(GUI_W - 38, btnY, 14, "config",
                cd -> ThemeEditorDialog.open(mainGroup, ItemTheme.load(terminalItem)));
        gearBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.theme_settings").getString());
        header.addWidget(gearBtn);

        ButtonWidget closeBtn = new ButtonWidget(GUI_W - 20, btnY, 14, 14,
                new GuiTextureGroup(new ColorRectTexture(theme.panelColor), new ColorBorderTexture(1, colorBorderLight)),
                cd -> gui.entityPlayer.closeContainer());
        closeBtn.setButtonTexture(TerminalButton.iconTex("close"));
        closeBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        closeBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(0x66AA0000), new ColorBorderTexture(1, C_TEXT_WHITE),
                TerminalButton.iconTex("close")));
        header.addWidget(closeBtn);

        return header;
    }

    private WidgetGroup buildBody() {
        WidgetGroup body = new WidgetGroup(0, BODY_Y, GUI_W, BODY_H);
        this.bodyGroup = body;

        // Left list panel
        DraggableScrollableWidgetGroup[] scrollRef = new DraggableScrollableWidgetGroup[1];
        WidgetGroup leftPanel = SchematicListPanel.build(
                0, 0, LEFT_W, BODY_H, 0,
                C_BORDER_DARK, colorBorderLight, C_TEXT_GRAY,
                theme, selectedIndex, schematics,
                this::onSelect, null, scrollRef);
        this.schematicsScroll = scrollRef[0];
        body.addWidget(leftPanel);

        // Divider
        body.addWidget(new ImageWidget(LEFT_W, 0, DIVIDER, BODY_H,
                new ColorRectTexture(0xFF2A2A2A)));

        // Right preview + actions
        this.rightPanel = buildRightPanel();
        body.addWidget(rightPanel);

        return body;
    }

    private WidgetGroup buildRightPanel() {
        WidgetGroup panel = new WidgetGroup(RIGHT_X, 0, RIGHT_W, BODY_H);
        panel.setBackground(new ColorRectTexture(0xFF111111));

        int actionsH = 72;
        int previewH = BODY_H - actionsH - 1;

        // 3D Preview area
        WidgetGroup previewArea = new WidgetGroup(0, 0, RIGHT_W, previewH);
        previewArea.setBackground(new ColorRectTexture(0xFF111111));
        populatePreview(previewArea, previewH);
        panel.addWidget(previewArea);

        // Divider
        panel.addWidget(new ImageWidget(0, previewH, RIGHT_W, 1, new ColorRectTexture(0xFF2A2A2A)));

        // Actions area
        WidgetGroup actions = new WidgetGroup(0, previewH + 1, RIGHT_W, actionsH - 1);
        actions.setBackground(new ColorRectTexture(0xFF1A1A1A));
        buildActions(actions);
        panel.addWidget(actions);

        return panel;
    }

    private void populatePreview(WidgetGroup area, int previewH) {
        area.clearAllWidgets();
        SchematicPreviewPanel.repopulate(area, RIGHT_W, previewH,
                selectedIndex, schematics, hasClipboard(), theme, player);


    }

    private void buildActions(WidgetGroup panel) {
        int fieldW = RIGHT_W - 20;
        TextFieldWidget nameField = new TextFieldWidget(10, 8, fieldW, 20, null, s -> {});
        nameField.setMaxStringLength(32);
        nameField.setTextColor(C_TEXT_WHITE);
        nameField.setBackground(new GuiTextureGroup(
                new ColorRectTexture(0xFF141414)));

        nameField.setCurrentString(selectedIndex >= 0 && selectedIndex < schematics.size()
                ? schematics.get(selectedIndex).getName() : "");
        this.nameInput = nameField;
        panel.addWidget(nameField);

        int btnH = 20, btnY = 36, x = 10, spacing = 6;

        panel.addWidget(makeBtn(x, btnY, 90, btnH, C_SUCCESS,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.load").getString(),
                cd -> pasteSchematic()));
        x += 90 + spacing;

        panel.addWidget(makeBtn(x, btnY, 80, btnH, C_INFO,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.save").getString(),
                cd -> saveSchematic()));
        x += 80 + spacing;

        panel.addWidget(makeBtn(x, btnY, 60, btnH, 0xFF2A2A2A,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.delete").getString(),
                cd -> deleteSchematic()));
        x += 60 + spacing;

        panel.addWidget(makeBtn(x, btnY, 60, btnH, 0xFF2A2A2A,
                "Export",
                cd -> player.displayClientMessage(
                        Component.literal("Export not yet implemented"), true))
                .setHoverTooltips(Component.literal("§7Not yet implemented")));

    }

    private ButtonWidget makeBtn(int x, int y, int w, int h, int color, String label,
                                 java.util.function.Consumer<com.lowdragmc.lowdraglib.gui.util.ClickData> action) {
        ButtonWidget btn = new ButtonWidget(x, y, w, h,
                new GuiTextureGroup(new ColorRectTexture(color), new ColorBorderTexture(1, colorBorderLight)),
                action);
        btn.setButtonTexture(new TextTexture(label).setWidth(w).setType(TextTexture.TextType.NORMAL));
        btn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture((color & 0x00FFFFFF) | 0x55000000), new ColorBorderTexture(2, C_TEXT_WHITE)));
        return btn;
    }

    private void setupParade() {
        com.gtceuterminal.client.ClientEvents.clearActiveParade();
        if (!theme.isBundleStyle()) return;
        com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
        if (bundle == null) return;
        var parade = bundle.createParadeWidget(0, 0, GUI_W, GUI_H);
        if (parade == null || parade.isEmpty()) return;
        parade.setGuiCenter(GUI_W / 2f, GUI_H / 2f);
        com.gtceuterminal.client.ClientEvents.setActiveParade(parade, theme.paradeMode);
    }

    private void onSelect(int index) {
        if (selectedIndex == index) return;
        selectedIndex = index;
        refreshLeft();
        refreshRight();
        if (nameInput != null && selectedIndex >= 0 && selectedIndex < schematics.size()) {
            nameInput.setCurrentString(schematics.get(selectedIndex).getName());
        }
    }

    private void refreshLeft() {
        if (schematicsScroll == null) return;
        SchematicListPanel.repopulate(schematicsScroll, schematics, selectedIndex,
                LEFT_W - 8 - 4, C_TEXT_GRAY, theme, this::onSelect);
    }

    private void refreshRight() {
        if (rightPanel == null || bodyGroup == null) return;
        bodyGroup.removeWidget(rightPanel);
        this.rightPanel = buildRightPanel();
        bodyGroup.addWidget(rightPanel);
    }

    private void pasteSchematic() {
        if (selectedIndex < 0 || selectedIndex >= schematics.size()) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.no_schematic_selected"); return;
        }
        SchematicData s = schematics.get(selectedIndex);
        TerminalNetwork.CHANNEL.sendToServer(
                new CPacketSchematicAction(CPacketSchematicAction.ActionType.LOAD, s.getName(), selectedIndex));
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.schematic_interface.chat.success.loaded_to_clipboard",
                        s.getName()), true);
    }

    private void saveSchematic() {
        if (!hasClipboard()) { msg("gui.gtceuterminal.schematic_interface.chat.error.no_clipboard"); return; }
        String name = nameInput != null ? nameInput.getCurrentString().trim() : "";
        if (name.isEmpty()) { msg("gui.gtceuterminal.schematic_interface.chat.error.enter_name"); return; }
        if ("Clipboard".equalsIgnoreCase(name)) { msg("gui.gtceuterminal.schematic_interface.chat.error.reserved_name"); return; }
        if (schematics.stream().anyMatch(s -> s.getName().equalsIgnoreCase(name))) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.name_exists"); return;
        }

        TerminalNetwork.CHANNEL.sendToServer(new CPacketSchematicAction(CPacketSchematicAction.ActionType.SAVE, name, -1));

        CompoundTag itemTag = terminalItem.getTag();
        if (itemTag != null && itemTag.contains("Clipboard")) {
            try {
                SchematicData clip = SchematicData.fromNBT(itemTag.getCompound("Clipboard"), player.level().registryAccess());
                schematics.add(new SchematicData(name, clip.getMultiblockType(),
                        clip.getBlocks(), clip.getBlockEntities(), clip.getOriginalFacing()));
                selectedIndex = schematics.size() - 1;
            } catch (Exception e) {
                GTCEUTerminalMod.LOGGER.error("Failed to build local schematic copy after save", e);
            }
        }

        if (nameInput != null) nameInput.setCurrentString("");
        refreshLeft();
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.schematic_interface.chat.success.saved", name), true);
    }

    private void deleteSchematic() {
        if (selectedIndex < 0 || selectedIndex >= schematics.size()) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.no_schematic_selected"); return;
        }
        String name = schematics.get(selectedIndex).getName();
        TerminalNetwork.CHANNEL.sendToServer(
                new CPacketSchematicAction(CPacketSchematicAction.ActionType.DELETE, name, -1));
        schematics.remove(selectedIndex);
        if (selectedIndex >= schematics.size()) selectedIndex = schematics.size() - 1;
        refreshLeft();
        refreshRight();
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.schematic_interface.chat.success.deleted", name), true);
    }

    private boolean hasClipboard() {
        CompoundTag tag = terminalItem.getTag();
        if (tag == null || !tag.contains("Clipboard")) return false;
        CompoundTag clip = tag.getCompound("Clipboard");
        return clip.contains("Blocks") && !clip.getList("Blocks", 10).isEmpty();
    }

    private void msg(String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private ModularUI buildModularUI(WidgetGroup content) {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();
            int margin = 10, maxW = sw - margin * 2, maxH = sh - margin * 2;

            if (GUI_W <= maxW && GUI_H <= maxH) {
                ModularUI ui = new ModularUI(new Size(GUI_W, GUI_H), uiHolder, player);
                ui.widget(content);
                ui.background(theme.modularUIBackground());
                return ui;
            }

            int viewW = Math.min(GUI_W, maxW), viewH = Math.min(GUI_H, maxH);
            DraggableScrollableWidgetGroup viewport =
                    new DraggableScrollableWidgetGroup(0, 0, viewW, viewH);
            viewport.setYScrollBarWidth(8);
            viewport.setYBarStyle(new ColorRectTexture(C_BORDER_DARK), new ColorRectTexture(colorBorderLight));
            viewport.addWidget(content);
            WidgetGroup root = new WidgetGroup(0, 0, viewW, viewH);
            root.addWidget(viewport);
            ModularUI ui = new ModularUI(new Size(viewW, viewH), uiHolder, player);
            ui.widget(root);
            ui.background(theme.modularUIBackground());
            return ui;
        } catch (Throwable t) {
            ModularUI ui = new ModularUI(new Size(GUI_W, GUI_H), uiHolder, player);
            ui.widget(content);
            ui.background(theme.modularUIBackground());
            return ui;
        }
    }

    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder) {
        return new SchematicInterfaceUI(heldHolder).createUI();
    }

    public static ModularUI create(SchematicItemUIFactory.Holder holder, Player player) {
        return new SchematicInterfaceUI(holder, player).createUI();
    }
}