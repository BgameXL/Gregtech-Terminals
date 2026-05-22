package com.gtceuterminal.client.gui.schematic;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
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
    private static final int HEADER_H = 32;
    private static final int FOOTER_H = 40;
    private static final int LEFT_W   = 190;
    private static final int PAD      = 8;

    private static final int RIGHT_X = PAD + LEFT_W + 4;
    private static final int RIGHT_W = GUI_W - RIGHT_X - PAD;
    private static final int LIST_Y  = 2 + HEADER_H + 4;
    private static final int LIST_H  = GUI_H - LIST_Y - FOOTER_H - 4;
    private static final int RIGHT_H = LIST_H;

    private static final int C_BORDER_DARK = 0xFF0A0A0A;
    private static final int C_TEXT_WHITE  = 0xFFFFFFFF;
    private static final int C_TEXT_GRAY   = 0xFFAAAAAA;
    private static final int C_HOVER       = 0x40FFFFFF;
    private static final int C_SUCCESS     = 0xFF4CAF50;
    private static final int C_ERROR       = 0xFFFF5252;
    private static final int C_INFO        = 0xFF42A5F5;

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
        GTCEUTerminalMod.LOGGER.info("Loaded {} schematics, selectedIndex={}", schematics.size(), selectedIndex);
    }

    public ModularUI createUI() {
        this.mainGroup = new WidgetGroup(0, 0, GUI_W, GUI_H);
        mainGroup.setBackground(new ColorRectTexture(0x00000000));

        if (!theme.isNativeStyle()) {
            mainGroup.addWidget(new WallpaperWidget(0, 0, GUI_W, GUI_H, () -> this.theme));
        }

        mainGroup.addWidget(buildOuterBorder());
        mainGroup.addWidget(buildHeader());
        mainGroup.addWidget(buildLeftPanel());

        this.rightPanel = SchematicPreviewPanel.build(
                RIGHT_X, LIST_Y, RIGHT_W, RIGHT_H,
                selectedIndex, schematics, hasClipboard(), theme, player);
        mainGroup.addWidget(rightPanel);

        mainGroup.addWidget(buildFooter());

        setupParade();
        this.gui = buildModularUI(mainGroup);
        return gui;
    }

    private ModularUI buildModularUI(WidgetGroup content) {
        try {
            var mc   = net.minecraft.client.Minecraft.getInstance();
            int sw   = mc.getWindow().getGuiScaledWidth();
            int sh   = mc.getWindow().getGuiScaledHeight();
            int margin = 10;
            int maxW = sw - margin * 2, maxH = sh - margin * 2;

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

    private WidgetGroup buildOuterBorder() {
        int bl = colorBorderLight;
        WidgetGroup g = new WidgetGroup(0, 0, GUI_W, GUI_H);
        g.addWidget(new ImageWidget(0,         0,          GUI_W, 2,     new ColorRectTexture(bl)));
        g.addWidget(new ImageWidget(0,         0,          2,     GUI_H, new ColorRectTexture(bl)));
        g.addWidget(new ImageWidget(GUI_W - 2, 0,          2,     GUI_H, new ColorRectTexture(C_BORDER_DARK)));
        g.addWidget(new ImageWidget(0,         GUI_H - 2,  GUI_W, 2,     new ColorRectTexture(C_BORDER_DARK)));
        return g;
    }

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
                    int iconSize = 20, iconY = (HEADER_H - iconSize) / 2;
                    header.addWidget(new ImageWidget(titleX, iconY, iconSize, iconSize, icon));
                    titleX += iconSize + 4;
                }
            }
        }

        LabelWidget title = new LabelWidget(titleX, 11,
                Component.translatable("gui.gtceuterminal.schematic_interface.title").getString());
        title.setTextColor(theme.isBundleStyle() ? theme.labelColor() : C_TEXT_WHITE);
        header.addWidget(title);

        boolean hasClip = hasClipboard();
        LabelWidget clipLabel = new LabelWidget(GUI_W - 220, 11,
                hasClip
                        ? Component.translatable("gui.gtceuterminal.schematic_interface.clipboard.ready").getString()
                        : Component.translatable("gui.gtceuterminal.schematic_interface.clipboard.empty").getString());
        clipLabel.setTextColor(hasClip ? C_SUCCESS : C_TEXT_GRAY);
        header.addWidget(clipLabel);

        ButtonWidget gearBtn = new ButtonWidget(GUI_W - 50, 7, 18, 18,
                new ColorRectTexture(0x00000000),
                cd -> ThemeEditorDialog.open(mainGroup, ItemTheme.load(terminalItem)));
        gearBtn.setButtonTexture(new TextTexture("§7⚙").setWidth(18).setType(TextTexture.TextType.NORMAL));
        gearBtn.setHoverTexture(new ColorRectTexture(C_HOVER));
        gearBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.theme_settings").getString());
        header.addWidget(gearBtn);

        ButtonWidget closeBtn = new ButtonWidget(GUI_W - 28, 7, 20, 18,
                new GuiTextureGroup(new ColorRectTexture(theme.panelColor), new ColorBorderTexture(1, colorBorderLight)),
                cd -> gui.entityPlayer.closeContainer());
        closeBtn.setButtonTexture(new TextTexture("§c✕").setWidth(20).setType(TextTexture.TextType.NORMAL));
        closeBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(0xFFAA0000), new ColorBorderTexture(1, C_TEXT_WHITE)));
        header.addWidget(closeBtn);

        return header;
    }

    private WidgetGroup buildLeftPanel() {
        TextFieldWidget[] nameRef = new TextFieldWidget[1];
        DraggableScrollableWidgetGroup[] scrollRef = new DraggableScrollableWidgetGroup[1];

        WidgetGroup panel = SchematicListPanel.build(
                PAD, LIST_Y, LEFT_W, LIST_H, PAD,
                C_BORDER_DARK, colorBorderLight, C_TEXT_GRAY,
                theme, selectedIndex, schematics,
                this::onSelect,
                nameRef, scrollRef);

        this.nameInput      = nameRef[0];
        this.schematicsScroll = scrollRef[0];
        return panel;
    }

    private WidgetGroup buildFooter() {
        int footerY = GUI_H - FOOTER_H - 2;
        WidgetGroup footer = new WidgetGroup(PAD, footerY, GUI_W - PAD * 2, FOOTER_H);
        footer.setBackground(theme.panelTexture());

        int btnH = 24, btnW = 100, spacing = 8, x = 8;

        footer.addWidget(makeButton(x, 8, btnW, btnH, C_SUCCESS,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.save").getString(),
                cd -> saveSchematic()));
        x += btnW + spacing;

        footer.addWidget(makeButton(x, 8, btnW, btnH, C_INFO,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.load").getString(),
                cd -> loadSchematic()));
        x += btnW + spacing;

        footer.addWidget(makeButton(x, 8, btnW, btnH, C_ERROR,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.delete").getString(),
                cd -> deleteSchematic()));

        return footer;
    }

    private ButtonWidget makeButton(int x, int y, int w, int h, int color, String label,
                                    java.util.function.Consumer<com.lowdragmc.lowdraglib.gui.util.ClickData> action) {
        ButtonWidget btn = new ButtonWidget(x, y, w, h,
                new GuiTextureGroup(new ColorRectTexture(color), new ColorBorderTexture(1, colorBorderLight)),
                action);
        btn.setButtonTexture(new TextTexture(label).setWidth(w).setType(TextTexture.TextType.NORMAL));
        btn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(color), new ColorBorderTexture(2, C_TEXT_WHITE)));
        return btn;
    }

    private void onSelect(int index) {
        if (selectedIndex == index) return;
        selectedIndex = index;
        refreshLeft();
        refreshRight();
    }

    private void refreshLeft() {
        if (schematicsScroll == null) return;
        int entryW = LEFT_W - 16 - 8;
        SchematicListPanel.repopulate(schematicsScroll, schematics, selectedIndex,
                entryW, C_TEXT_GRAY, theme, this::onSelect);
    }

    private void refreshRight() {
        if (rightPanel == null) return;
        SchematicPreviewPanel.repopulate(rightPanel, RIGHT_W, RIGHT_H,
                selectedIndex, schematics, hasClipboard(), theme, player);
    }

    private void saveSchematic() {
        if (!hasClipboard()) { msg("gui.gtceuterminal.schematic_interface.chat.error.no_clipboard"); return; }
        String name = nameInput.getCurrentString().trim();
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

        nameInput.setCurrentString("");
        refreshLeft();
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
        refreshLeft();
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

    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder) {
        return new SchematicInterfaceUI(heldHolder).createUI();
    }

    public static ModularUI create(SchematicItemUIFactory.Holder holder, Player player) {
        return new SchematicInterfaceUI(holder, player).createUI();
    }
}