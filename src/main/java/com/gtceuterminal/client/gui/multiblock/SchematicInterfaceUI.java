package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.client.gui.widget.WallpaperWidget;
import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.widget.SchematicPreviewWidget;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.network.CPacketSchematicAction;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.client.gui.factory.SchematicItemUIFactory;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SchematicInterfaceUI {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int GUI_W      = 600;
    private static final int GUI_H      = 480;
    private static final int HEADER_H   = 32;
    private static final int FOOTER_H   = 40;
    private static final int LEFT_W     = 190;
    private static final int PAD        = 8;

    // ── Colors ────────────────────────────────────────────────────────────────
    private int COLOR_BG_DARK;
    private int COLOR_BG_MEDIUM;
    private int COLOR_BG_LIGHT;
    private int COLOR_BORDER_LIGHT;
    private static final int COLOR_BORDER_DARK = 0xFF0A0A0A;
    private static final int COLOR_TEXT_WHITE  = 0xFFFFFFFF;
    private static final int COLOR_TEXT_GRAY   = 0xFFAAAAAA;
    private static final int COLOR_HOVER       = 0x40FFFFFF;
    private static final int COLOR_SUCCESS     = 0xFF4CAF50;
    private static final int COLOR_ERROR       = 0xFFFF5252;
    private static final int COLOR_INFO        = 0xFF42A5F5;

    // ── State ─────────────────────────────────────────────────────────────────
    private final IUIHolder  uiHolder;
    private final ItemStack  terminalItem;
    private final Player     player;
    private       ItemTheme  theme;

    private List<SchematicData> schematics = new ArrayList<>();
    private int selectedIndex = -1;

    private ModularUI                      gui;
    private WidgetGroup                    mainGroup;
    private TextFieldWidget                nameInput;
    private WidgetGroup                    rightPanel;
    private DraggableScrollableWidgetGroup schematicsListWidget;

    // ── Constructors ──────────────────────────────────────────────────────────

    public SchematicInterfaceUI(HeldItemUIFactory.HeldItemHolder heldHolder) {
        this.uiHolder     = heldHolder;
        this.terminalItem = heldHolder.held;
        this.player       = heldHolder.player;
        applyTheme(ItemTheme.load(terminalItem));
        loadSchematics();
    }

    public SchematicInterfaceUI(SchematicItemUIFactory.Holder holder, Player player) {
        this.uiHolder     = holder;
        this.terminalItem = holder.getTerminalItem();
        this.player       = player;
        applyTheme(ItemTheme.load(terminalItem));
        loadSchematics();
    }

    private void applyTheme(ItemTheme t) {
        this.theme         = t;
        COLOR_BG_DARK      = t.bgColor;
        COLOR_BG_MEDIUM    = t.panelColor;
        COLOR_BG_LIGHT     = t.isNativeStyle() ? 0xFF3A3A3A : t.accent(0xAA);
        COLOR_BORDER_LIGHT = t.isNativeStyle() ? 0xFF555555 : t.accent(0xFF);
    }

    // ── Data ──────────────────────────────────────────────────────────────────
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
        GTCEUTerminalMod.LOGGER.info("Loaded {} schematics, selectedIndex={}", schematics.size(), selectedIndex);
    }

    // ── Parade ────────────────────────────────────────────────────────────────
    private void setupParade() {
        com.gtceuterminal.client.ClientEvents.clearActiveParade();
        if (!theme.isBundleStyle()) return;
        com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
        if (bundle == null) return;
        com.gtceuterminal.client.gui.widget.MultiblockParadeWidget parade =
                bundle.createParadeWidget(0, 0, GUI_W, GUI_H);
        if (parade == null || parade.isEmpty()) return;
        parade.setGuiCenter(GUI_W / 2f, GUI_H / 2f);
        com.gtceuterminal.client.ClientEvents.setActiveParade(parade, theme.paradeMode);
    }

    // ── UI construction ───────────────────────────────────────────────────────
    public ModularUI createUI() {
        this.mainGroup = new WidgetGroup(0, 0, GUI_W, GUI_H);
        mainGroup.setBackground(new ColorRectTexture(0x00000000));
        if (!theme.isNativeStyle()) {
            mainGroup.addWidget(new WallpaperWidget(0, 0, GUI_W, GUI_H, () -> this.theme));
        }

        mainGroup.addWidget(buildOuterBorder());
        mainGroup.addWidget(buildHeader());
        mainGroup.addWidget(buildLeftPanel());

        this.rightPanel = buildRightPanel();
        mainGroup.addWidget(rightPanel);

        mainGroup.addWidget(buildFooter());

        setupParade();
        ModularUI ui = createUIWithViewport(mainGroup);
        this.gui = ui;
        return ui;
    }

    private ModularUI createUIWithViewport(WidgetGroup content) {
        try {
            var mc   = net.minecraft.client.Minecraft.getInstance();
            int sw   = mc.getWindow().getGuiScaledWidth();
            int sh   = mc.getWindow().getGuiScaledHeight();
            int margin = 10;
            int maxW = sw - margin * 2;
            int maxH = sh - margin * 2;

            if (GUI_W <= maxW && GUI_H <= maxH) {
                ModularUI ui = new ModularUI(new Size(GUI_W, GUI_H), uiHolder, player);
                ui.widget(content);
                ui.background(theme.modularUIBackground());
                return ui;
            }

            int viewW = Math.min(GUI_W, maxW);
            int viewH = Math.min(GUI_H, maxH);
            DraggableScrollableWidgetGroup viewport =
                    new DraggableScrollableWidgetGroup(0, 0, viewW, viewH);
            viewport.setYScrollBarWidth(8);
            viewport.setYBarStyle(
                    new ColorRectTexture(COLOR_BORDER_DARK),
                    new ColorRectTexture(COLOR_BORDER_LIGHT));
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

    // ── Outer border ──────────────────────────────────────────────────────────
    private WidgetGroup buildOuterBorder() {
        WidgetGroup g = new WidgetGroup(0, 0, GUI_W, GUI_H);
        g.addWidget(new ImageWidget(0,        0,       GUI_W, 2,     new ColorRectTexture(COLOR_BORDER_LIGHT)));
        g.addWidget(new ImageWidget(0,        0,       2,     GUI_H, new ColorRectTexture(COLOR_BORDER_LIGHT)));
        g.addWidget(new ImageWidget(GUI_W - 2, 0,      2,     GUI_H, new ColorRectTexture(COLOR_BORDER_DARK)));
        g.addWidget(new ImageWidget(0,        GUI_H - 2, GUI_W, 2,   new ColorRectTexture(COLOR_BORDER_DARK)));
        return g;
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private WidgetGroup buildHeader() {
        WidgetGroup header = new WidgetGroup(2, 2, GUI_W - 4, HEADER_H);
        header.setBackground(theme.headerTexture());

        int titleX = 12;
        if (theme.isBundleStyle()) {
            com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                    com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
            if (bundle != null) {
                com.lowdragmc.lowdraglib.gui.texture.IGuiTexture icon = bundle.iconTexture();
                if (icon != null) {
                    int iconSize = 20;
                    int iconY    = (HEADER_H - iconSize) / 2;
                    header.addWidget(new ImageWidget(titleX, iconY, iconSize, iconSize, icon));
                    titleX += iconSize + 4;
                }
            }
        }

        LabelWidget title = new LabelWidget(titleX, 11,
                Component.translatable("gui.gtceuterminal.schematic_interface.title").getString());
        title.setTextColor(theme.isBundleStyle() ? theme.labelColor() : COLOR_TEXT_WHITE);
        header.addWidget(title);

        boolean hasClip = hasClipboard();
        String clipText = hasClip
                ? Component.translatable("gui.gtceuterminal.schematic_interface.clipboard.ready").getString()
                : Component.translatable("gui.gtceuterminal.schematic_interface.clipboard.empty").getString();
        LabelWidget clipLabel = new LabelWidget(GUI_W - 220, 11, clipText);
        clipLabel.setTextColor(hasClip ? COLOR_SUCCESS : COLOR_TEXT_GRAY);
        header.addWidget(clipLabel);

        
        ButtonWidget gearBtn = new ButtonWidget(GUI_W - 50, 7, 18, 18,
                new ColorRectTexture(0x00000000),
                cd -> ThemeEditorDialog.open(mainGroup, ItemTheme.load(terminalItem)));
        gearBtn.setButtonTexture(new TextTexture("§7⚙").setWidth(18).setType(TextTexture.TextType.NORMAL));
        gearBtn.setHoverTexture(new ColorRectTexture(COLOR_HOVER));
        gearBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.theme_settings").getString());
        header.addWidget(gearBtn);

        
        ButtonWidget closeBtn = new ButtonWidget(GUI_W - 28, 7, 20, 18,
                new GuiTextureGroup(
                        new ColorRectTexture(COLOR_BG_MEDIUM),
                        new ColorBorderTexture(1, COLOR_BORDER_LIGHT)),
                cd -> gui.entityPlayer.closeContainer());
        closeBtn.setButtonTexture(new TextTexture("§c✕").setWidth(20).setType(TextTexture.TextType.NORMAL));
        closeBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(0xFFAA0000),
                new ColorBorderTexture(1, COLOR_TEXT_WHITE)));
        header.addWidget(closeBtn);

        return header;
    }

    // ── Left panel (list + name input) ────────────────────────────────────────
    private static final int LIST_Y       = 2 + HEADER_H + 4;
    private static final int LIST_H       = GUI_H - LIST_Y - FOOTER_H - 4;
    private static final int ENTRY_W      = LEFT_W - 20; // fits inside the scroll
    private static final int ENTRY_H      = 44;
    private static final int ENTRY_STEP   = 46;

    private WidgetGroup buildLeftPanel() {
        WidgetGroup panel = new WidgetGroup(PAD, LIST_Y, LEFT_W, LIST_H);
        panel.setBackground(theme.panelTexture());

        LabelWidget nameLabel = new LabelWidget(8, 6,
                Component.translatable("gui.gtceuterminal.schematic_interface.name_label").getString());
        nameLabel.setTextColor(COLOR_TEXT_GRAY);
        panel.addWidget(nameLabel);

        this.nameInput = new TextFieldWidget(8, 18, LEFT_W - 16, 16, null, s -> {});
        nameInput.setMaxStringLength(32);
        nameInput.setTextColor(COLOR_TEXT_WHITE);
        nameInput.setBackground(theme.backgroundTexture());
        nameInput.setBordered(true);
        panel.addWidget(nameInput);

        LabelWidget listLabel = new LabelWidget(8, 40,
                Component.translatable("gui.gtceuterminal.schematic_interface.saved_schematics_label").getString());
        listLabel.setTextColor(COLOR_TEXT_GRAY);
        panel.addWidget(listLabel);

        int scrollH = LIST_H - 56;
        this.schematicsListWidget = new DraggableScrollableWidgetGroup(8, 54, LEFT_W - 16, scrollH);
        schematicsListWidget.setBackground(theme.backgroundTexture());
        schematicsListWidget.setYScrollBarWidth(6);
        schematicsListWidget.setYBarStyle(
                new ColorRectTexture(COLOR_BORDER_DARK),
                new ColorRectTexture(COLOR_BORDER_LIGHT));
        panel.addWidget(schematicsListWidget);

        populateSchematicsList();
        return panel;
    }

    private void populateSchematicsList() {
        if (schematicsListWidget == null) return;
        schematicsListWidget.clearAllWidgets();

        if (schematics.isEmpty()) {
            LabelWidget empty = new LabelWidget(6, 10,
                    Component.translatable("gui.gtceuterminal.schematic_interface.no_schematics_saved").getString());
            empty.setTextColor(COLOR_TEXT_GRAY);
            schematicsListWidget.addWidget(empty);

            LabelWidget hint1 = new LabelWidget(6, 26,
                    Component.translatable("gui.gtceuterminal.schematic_interface.hint_formed_1").getString());
            hint1.setTextColor(0xFF666666);
            schematicsListWidget.addWidget(hint1);

            LabelWidget hint2 = new LabelWidget(6, 38,
                    Component.translatable("gui.gtceuterminal.schematic_interface.hint_formed_2").getString());
            hint2.setTextColor(0xFF666666);
            schematicsListWidget.addWidget(hint2);
            return;
        }

        int yPos = 2;
        for (int i = 0; i < schematics.size(); i++) {
            schematicsListWidget.addWidget(buildSchematicEntry(schematics.get(i), i, yPos));
            yPos += ENTRY_STEP;
        }
    }

    private WidgetGroup buildSchematicEntry(SchematicData schematic, int index, int yPos) {
        boolean isSelected = (index == selectedIndex);

        WidgetGroup entry = new WidgetGroup(0, yPos, ENTRY_W, ENTRY_H);
        entry.setBackground(new ColorRectTexture(isSelected ? COLOR_BG_LIGHT : COLOR_BG_MEDIUM));

        if (isSelected) {
            entry.addWidget(new ImageWidget(0, 0, 3, ENTRY_H, new ColorRectTexture(COLOR_INFO)));
        }

        ButtonWidget click = new ButtonWidget(0, 0, ENTRY_W, ENTRY_H,
                new ColorRectTexture(0x00000000),
                cd -> {
                    if (this.selectedIndex != index) {
                        this.selectedIndex = index;
                        refreshLeftPanel();
                    }
                });
        click.setHoverTexture(new ColorRectTexture(COLOR_HOVER));
        entry.addWidget(click);

        String name = schematic.getName();
        if (name.length() > 20) name = name.substring(0, 18) + "…";
        LabelWidget nameLabel = new LabelWidget(8, 6, "§f" + name);
        nameLabel.setTextColor(COLOR_TEXT_WHITE);
        entry.addWidget(nameLabel);

        String info = Component.translatable(
                "gui.gtceuterminal.schematic_interface.entry.blocks",
                schematic.getBlocks().size()).getString();
        LabelWidget infoLabel = new LabelWidget(8, 20, info);
        infoLabel.setTextColor(COLOR_TEXT_GRAY);
        entry.addWidget(infoLabel);

        BlockPos sz = schematic.getSize();
        String sizeStr = sz.getX() + "×" + sz.getY() + "×" + sz.getZ();
        LabelWidget sizeLabel = new LabelWidget(8, 30, "§8" + sizeStr);
        entry.addWidget(sizeLabel);

        return entry;
    }

    // ── Right panel (3D preview) ───────────────────────────────────────────────
    private static final int RIGHT_X = PAD + LEFT_W + 4;
    private static final int RIGHT_W = GUI_W - RIGHT_X - PAD;
    private static final int RIGHT_H = GUI_H - LIST_Y - FOOTER_H - 4;

    private WidgetGroup buildRightPanel() {
        WidgetGroup panel = new WidgetGroup(RIGHT_X, LIST_Y, RIGHT_W, RIGHT_H);
        panel.setBackground(theme.panelTexture());
        populateRightPanel(panel);
        return panel;
    }

    private void populateRightPanel(WidgetGroup panel) {
        int previewW = RIGHT_W - 16;
        int infoH    = 72; // space for text below preview
        int previewH = RIGHT_H - 16 - infoH;

        WidgetGroup previewArea = new WidgetGroup(8, 8, previewW, RIGHT_H - 16);
        previewArea.setBackground(theme.backgroundTexture());

        if (selectedIndex >= 0 && selectedIndex < schematics.size()) {
            SchematicData sel = schematics.get(selectedIndex);

            if (player.level().isClientSide) {
                SchematicPreviewWidget preview = new SchematicPreviewWidget(
                        0, 0, previewW, previewH, sel);
                preview.setBackground(new ColorRectTexture(0xFF080808));
                previewArea.addWidget(preview);
            }

            int ty = previewH + 6;

            String displayName = getMultiblockName(sel);
            if (displayName.length() > 28) displayName = displayName.substring(0, 26) + "…";
            LabelWidget nameLbl = new LabelWidget(4, ty, "§f§l" + displayName);
            nameLbl.setTextColor(COLOR_TEXT_WHITE);
            previewArea.addWidget(nameLbl);
            ty += 14;

            BlockPos size = sel.getSize();
            String info = Component.translatable(
                    "gui.gtceuterminal.schematic_interface.preview.info",
                    sel.getBlocks().size(),
                    size.getX(), size.getY(), size.getZ()).getString();
            LabelWidget infoLbl = new LabelWidget(4, ty, info);
            infoLbl.setTextColor(COLOR_TEXT_GRAY);
            previewArea.addWidget(infoLbl);
            ty += 12;

            LabelWidget zoom = new LabelWidget(4, ty,
                    Component.translatable("gui.gtceuterminal.schematic_interface.preview.zoom_hint").getString());
            zoom.setTextColor(0xFF555555);
            previewArea.addWidget(zoom);
            ty += 12;

            LabelWidget rot = new LabelWidget(4, ty,
                    Component.translatable("gui.gtceuterminal.schematic_interface.preview.rotation_hint").getString());
            rot.setTextColor(0xFF555555);
            previewArea.addWidget(rot);

        } else if (hasClipboard()) {
            LabelWidget clipInfo = new LabelWidget(10, previewH / 2 - 10,
                    Component.translatable("gui.gtceuterminal.schematic_interface.preview.clipboard_content").getString());
            clipInfo.setTextColor(COLOR_SUCCESS);
            previewArea.addWidget(clipInfo);

            LabelWidget hint = new LabelWidget(10, previewH / 2 + 4,
                    Component.translatable("gui.gtceuterminal.schematic_interface.preview.save_to_see").getString());
            hint.setTextColor(COLOR_TEXT_GRAY);
            previewArea.addWidget(hint);
        } else {
            LabelWidget none = new LabelWidget(previewW / 2 - 40, RIGHT_H / 2 - 10,
                    Component.translatable("gui.gtceuterminal.schematic_interface.preview.no_preview").getString());
            none.setTextColor(COLOR_TEXT_GRAY);
            previewArea.addWidget(none);
        }

        panel.addWidget(previewArea);
    }

    // ── Footer (action buttons) ────────────────────────────────────────────────
    private WidgetGroup buildFooter() {
        int footerY = GUI_H - FOOTER_H - 2;
        WidgetGroup footer = new WidgetGroup(PAD, footerY, GUI_W - PAD * 2, FOOTER_H);
        footer.setBackground(theme.panelTexture());

        int btnH = 24, btnW = 100, spacing = 8, x = 8;

        ButtonWidget save = makeButton(x, 8, btnW, btnH, COLOR_SUCCESS,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.save").getString(),
                cd -> saveSchematic());
        footer.addWidget(save);
        x += btnW + spacing;

        ButtonWidget load = makeButton(x, 8, btnW, btnH, COLOR_INFO,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.load").getString(),
                cd -> loadSchematic());
        footer.addWidget(load);
        x += btnW + spacing;

        ButtonWidget delete = makeButton(x, 8, btnW, btnH, COLOR_ERROR,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.delete").getString(),
                cd -> deleteSchematic());
        footer.addWidget(delete);

        return footer;
    }

    private ButtonWidget makeButton(int x, int y, int w, int h, int color, String label,
                                    java.util.function.Consumer<com.lowdragmc.lowdraglib.gui.util.ClickData> action) {
        ButtonWidget btn = new ButtonWidget(x, y, w, h,
                new GuiTextureGroup(
                        new ColorRectTexture(color),
                        new ColorBorderTexture(1, COLOR_BORDER_LIGHT)),
                action);
        btn.setButtonTexture(new TextTexture(label).setWidth(w).setType(TextTexture.TextType.NORMAL));
        btn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(color),
                new ColorBorderTexture(2, COLOR_TEXT_WHITE)));
        return btn;
    }

    // ── Refresh helpers ───────────────────────────────────────────────────────
    private void refreshLeftPanel() {
        populateSchematicsList();
        refreshRightPanel();
    }

    private void refreshRightPanel() {
        if (rightPanel == null) return;
        rightPanel.clearAllWidgets();
        populateRightPanel(rightPanel);
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void saveSchematic() {
        if (!hasClipboard()) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.no_clipboard"); return;
        }
        String name = nameInput.getCurrentString().trim();
        if (name.isEmpty()) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.enter_name"); return;
        }
        if ("Clipboard".equalsIgnoreCase(name)) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.reserved_name"); return;
        }
        if (schematics.stream().anyMatch(s -> s.getName().equalsIgnoreCase(name))) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.name_exists"); return;
        }

        TerminalNetwork.CHANNEL.sendToServer(
                new CPacketSchematicAction(CPacketSchematicAction.ActionType.SAVE, name, -1));

        CompoundTag itemTag = terminalItem.getTag();
        if (itemTag != null && itemTag.contains("Clipboard")) {
            try {
                SchematicData clip = SchematicData.fromNBT(
                        itemTag.getCompound("Clipboard"), player.level().registryAccess());
                schematics.add(new SchematicData(name, clip.getMultiblockType(),
                        clip.getBlocks(), clip.getBlockEntities(), clip.getOriginalFacing()));
                selectedIndex = schematics.size() - 1;
            } catch (Exception e) {
                GTCEUTerminalMod.LOGGER.error("Failed to build local schematic copy after save", e);
            }
        }

        nameInput.setCurrentString("");
        refreshLeftPanel();
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.schematic_interface.chat.success.saved", name), true);
    }

    private void loadSchematic() {
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

    private void deleteSchematic() {
        if (selectedIndex < 0 || selectedIndex >= schematics.size()) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.no_schematic_selected"); return;
        }
        String name = schematics.get(selectedIndex).getName();
        TerminalNetwork.CHANNEL.sendToServer(
                new CPacketSchematicAction(CPacketSchematicAction.ActionType.DELETE, name, -1));

        schematics.remove(selectedIndex);
        if (selectedIndex >= schematics.size()) selectedIndex = schematics.size() - 1;

        refreshLeftPanel();
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.schematic_interface.chat.success.deleted", name), true);
    }

    private void msg(String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private boolean hasClipboard() {
        CompoundTag tag = terminalItem.getTag();
        if (tag == null || !tag.contains("Clipboard")) return false;
        CompoundTag clip = tag.getCompound("Clipboard");
        return clip.contains("Blocks") && !clip.getList("Blocks", 10).isEmpty();
    }

    private String getMultiblockName(SchematicData schematic) {
        if (schematic == null || schematic.getBlocks().isEmpty()) {
            return Component.translatable("gui.gtceuterminal.schematic_interface.fallback.multiblock_structure").getString();
        }
        for (Map.Entry<BlockPos, BlockState> e : schematic.getBlocks().entrySet()) {
            String id = e.getValue().getBlock().getDescriptionId().toLowerCase();
            if (id.contains("gtceu") && !id.contains("casing") && !id.contains("hatch")
                    && !id.contains("pipe") && !id.contains("coil") && !id.contains("glass")) {
                String n = e.getValue().getBlock().getName().getString();
                return n.length() > 35 ? n.substring(0, 32) + "…" : n;
            }
        }
        String type = schematic.getMultiblockType();
        if (type != null && !type.isEmpty()) {
            type = type.replace("WorkableElectricMultiblockMachine", "Electric Machine")
                    .replace("CoilWorkableElectricMultiblockMachine", "Coil Machine")
                    .replace("ElectricMultiblockMachine", "Electric Machine");
            return type.length() > 35 ? type.substring(0, 32) + "…" : type;
        }
        return Component.translatable("gui.gtceuterminal.schematic_interface.fallback.multiblock_structure").getString();
    }

    // ── Static factory methods ────────────────────────────────────────────────
    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder) {
        return new SchematicInterfaceUI(heldHolder).createUI();
    }

    public static ModularUI create(SchematicItemUIFactory.Holder holder, Player player) {
        return new SchematicInterfaceUI(holder, player).createUI();
    }
}