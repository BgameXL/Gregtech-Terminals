package com.gtceuterminal.client.gui.schematic;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.style.BevelTexture;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
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
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.client.Minecraft;
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

    private static final int GUI_W = 600;
    private static final int GUI_H = 480;
    private static final int HEADER_H = 30;
    private static final int BODY_Y = HEADER_H + 1;
    private static final int BODY_H = GUI_H - BODY_Y - 2;
    private static final int CARD_INSET = 2;
    private static final int LEFT_W = 202;
    private static final int DIVIDER = 1;
    private static final int BODY_W = GUI_W - 4;
    private static final int INNER_H = BODY_H - CARD_INSET * 2;
    private static final int RIGHT_X = CARD_INSET + LEFT_W + DIVIDER;
    private static final int RIGHT_W = BODY_W - CARD_INSET * 2 - LEFT_W - DIVIDER;

    private final IUIHolder uiHolder;
    private final ItemStack terminalItem;
    private final Player player;
    private final ItemTheme theme;
    private final TerminalTheme tokens;

    private List<SchematicData> schematics = new ArrayList<>();
    private int selectedIndex = -1;

    private ModularUI gui;
    private WidgetGroup mainGroup;
    private WidgetGroup rightPanel;
    private WidgetGroup bodyGroup;
    private TextFieldWidget nameInput;
    private DraggableScrollableWidgetGroup schematicsScroll;

    public SchematicInterfaceUI(HeldItemUIFactory.HeldItemHolder heldHolder) {
        this.uiHolder = heldHolder;
        this.terminalItem = heldHolder.held;
        this.player = heldHolder.player;
        this.theme = ItemTheme.load(terminalItem);
        TerminalUI.use(theme);
        this.tokens = TerminalUI.tokens();
        loadSchematics();
    }

    public SchematicInterfaceUI(SchematicItemUIFactory.Holder holder, Player player) {
        this.uiHolder = holder;
        this.terminalItem = holder.getTerminalItem();
        this.player = player;
        this.theme = ItemTheme.load(terminalItem);
        TerminalUI.use(theme);
        this.tokens = TerminalUI.tokens();
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
        mainGroup.setBackground(new ColorRectTexture(Palette.CLEAR));

        if (!theme.isNativeStyle()) {
            mainGroup.addWidget(new WallpaperWidget(0, 0, GUI_W, GUI_H, () -> this.theme));
        }

        mainGroup.addWidget(buildHeader());
        mainGroup.addWidget(buildBody());

        setupParade();
        this.gui = buildModularUI(mainGroup);
        return gui;
    }

    private WidgetGroup buildHeader() {
        WidgetGroup header = new WidgetGroup(2, 2, GUI_W - 4, HEADER_H);
        header.setBackground(new BevelTexture(tokens.panel()));

        int titleX = 8;
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
                    tokens.panelDisabled(), tokens.border()));
            titleX += iconSize + 6;
        }

        LabelWidget itemName = new LabelWidget(titleX, 11,
                fitText(terminalItem.getHoverName().getString(), 240));
        itemName.setColor(theme.isBundleStyle() ? theme.labelColor() : tokens.textStrong());
        header.addWidget(itemName);

        int btnSize = 20;
        int btnY = (HEADER_H - btnSize) / 2;
        int closeX = header.getSizeWidth() - btnSize - 6;
        int themeX = closeX - btnSize - 4;

        ButtonWidget gearBtn = TerminalButton.icon3d(themeX, btnY, btnSize, "✦",
                cd -> ThemeEditorDialog.open(mainGroup, ItemTheme.load(terminalItem)));
        gearBtn.setHoverTooltips(
                Component.translatable("gui.gtceuterminal.theme_settings").getString());
        header.addWidget(gearBtn);

        ButtonWidget closeBtn = TerminalButton.icon3d(closeX, btnY, btnSize, "×",
                cd -> gui.entityPlayer.closeContainer());
        closeBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        header.addWidget(closeBtn);

        return header;
    }

    private WidgetGroup buildBody() {
        WidgetGroup body = new WidgetGroup(2, BODY_Y, BODY_W, BODY_H);
        body.setBackground(TerminalUI.cardBg());
        this.bodyGroup = body;

        DraggableScrollableWidgetGroup[] scrollRef = new DraggableScrollableWidgetGroup[1];
        WidgetGroup leftPanel = SchematicListPanel.build(
                CARD_INSET, CARD_INSET, LEFT_W, INNER_H,
                selectedIndex, schematics, this::onSelect, scrollRef);
        this.schematicsScroll = scrollRef[0];
        body.addWidget(leftPanel);

        body.addWidget(new ImageWidget(CARD_INSET + LEFT_W, CARD_INSET, DIVIDER, INNER_H,
                new ColorRectTexture(tokens.borderDark())));

        this.rightPanel = buildRightPanel();
        body.addWidget(rightPanel);

        return body;
    }

    private WidgetGroup buildRightPanel() {
        WidgetGroup panel = new WidgetGroup(RIGHT_X, CARD_INSET, RIGHT_W, INNER_H);
        panel.setBackground(new ColorRectTexture(tokens.surface()));

        int actionsH = 72;
        int previewH = INNER_H - actionsH - 1;

        WidgetGroup previewArea = new WidgetGroup(0, 0, RIGHT_W, previewH);
        previewArea.setBackground(new ColorRectTexture(tokens.surface()));
        populatePreview(previewArea, previewH);
        panel.addWidget(previewArea);

        panel.addWidget(new ImageWidget(0, previewH, RIGHT_W, 1,
                new ColorRectTexture(tokens.borderDark())));

        WidgetGroup actions = new WidgetGroup(0, previewH + 1, RIGHT_W, actionsH - 1);
        actions.setBackground(new ColorRectTexture(tokens.panel()));
        buildActions(actions);
        panel.addWidget(actions);

        return panel;
    }

    private void populatePreview(WidgetGroup area, int previewH) {
        area.clearAllWidgets();
        SchematicPreviewPanel.repopulate(area, RIGHT_W, previewH,
                selectedIndex, schematics, hasClipboard(), player);
    }

    private void buildActions(WidgetGroup panel) {
        String nameLabelText = stripFormatting(Component.translatable(
                "gui.gtceuterminal.schematic_interface.name_label").getString());
        LabelWidget nameLabel = new LabelWidget(10, 12, nameLabelText);
        nameLabel.setColor(tokens.textMuted());
        panel.addWidget(nameLabel);

        int fieldX = 104;
        int fieldW = RIGHT_W - fieldX - 10;
        TextFieldWidget nameField = new TextFieldWidget(fieldX, 5, fieldW, 20, null, s -> {
        });
        nameField.setMaxStringLength(32);
        nameField.setBordered(false);
        nameField.setTextColor(tokens.textStrong());
        nameField.setBackground(new GuiTextureGroup(
                new ColorRectTexture(tokens.panelDisabled()),
                new ColorBorderTexture(1, tokens.border())));
        nameField.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(tokens.panelDisabled()),
                new ColorBorderTexture(1, tokens.textStrong())));

        nameField.setCurrentString(selectedIndex >= 0 && selectedIndex < schematics.size()
                ? schematics.get(selectedIndex).getName() : "");
        this.nameInput = nameField;
        panel.addWidget(nameField);

        int btnH = 20, btnY = 39, x = 10, spacing = 6;

        panel.addWidget(TerminalButton.text3d(x, btnY, 90, btnH,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.load").getString(),
                tokens.action(), cd -> pasteSchematic()));
        x += 90 + spacing;

        panel.addWidget(TerminalButton.text3d(x, btnY, 80, btnH,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.save").getString(),
                tokens.successSurface(), cd -> saveSchematic()));
        x += 80 + spacing;

        panel.addWidget(TerminalButton.text3d(x, btnY, 60, btnH,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.delete").getString(),
                tokens.errorSurface(), cd -> deleteSchematic()));
        x += 60 + spacing;

        panel.addWidget(TerminalButton.text3d(x, btnY, 60, btnH,
                        Component.translatable("gui.gtceuterminal.schematic_interface.button.export").getString(),
                        tokens.panelDisabled(),
                        cd -> player.displayClientMessage(
                                Component.translatable("gui.gtceuterminal.schematic_interface.export_not_implemented"), true))
                .setHoverTooltips(Component.translatable("gui.gtceuterminal.schematic_interface.not_yet_implemented")));
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
        SchematicListPanel.repopulate(
                schematicsScroll, schematics, selectedIndex, LEFT_W - 2, this::onSelect);
    }

    private void refreshRight() {
        if (rightPanel == null || bodyGroup == null) return;
        bodyGroup.removeWidget(rightPanel);
        this.rightPanel = buildRightPanel();
        bodyGroup.addWidget(rightPanel);
    }

    private void pasteSchematic() {
        if (selectedIndex < 0 || selectedIndex >= schematics.size()) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.no_schematic_selected");
            return;
        }
        SchematicData s = schematics.get(selectedIndex);
        TerminalNetwork.CHANNEL.sendToServer(
                new CPacketSchematicAction(CPacketSchematicAction.ActionType.LOAD, s.getName(), selectedIndex));
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.schematic_interface.chat.success.loaded_to_clipboard",
                        s.getName()), true);
    }

    private void saveSchematic() {
        if (!hasClipboard()) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.no_clipboard");
            return;
        }
        String name = nameInput != null ? nameInput.getCurrentString().trim() : "";
        if (name.isEmpty()) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.enter_name");
            return;
        }
        if ("Clipboard".equalsIgnoreCase(name)) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.reserved_name");
            return;
        }
        if (schematics.stream().anyMatch(s -> s.getName().equalsIgnoreCase(name))) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.name_exists");
            return;
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
            msg("gui.gtceuterminal.schematic_interface.chat.error.no_schematic_selected");
            return;
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
        ModularUI ui = new ModularUI(new Size(GUI_W, GUI_H), uiHolder, player);
        ui.widget(content);
        ui.background(new ColorRectTexture(Palette.CLEAR));
        return ui;
    }

    private static String fitText(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) return "";
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        return font.substrByWidth(Component.literal(text),
                Math.max(0, maxWidth - font.width(ellipsis))).getString() + ellipsis;
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("§.", "");
    }

    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder) {
        return new SchematicInterfaceUI(heldHolder).createUI();
    }

    public static ModularUI create(SchematicItemUIFactory.Holder holder, Player player) {
        return new SchematicInterfaceUI(holder, player).createUI();
    }
}
