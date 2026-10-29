package com.gtceuterminal.client.gui.schematic;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.gui.style.TerminalText;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
import com.gtceuterminal.client.gui.widget.HeaderItemIcon;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.client.gui.widget.TerminalTabs;
import com.gtceuterminal.client.gui.widget.WallpaperWidget;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.gui.factory.SchematicItemUIFactory;
import com.gtceuterminal.common.network.CPacketSchematicAction;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.gtceuterminal.common.theme.ItemTheme;
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SchematicInterfaceUI {

    private static final int GUI_W = 660;
    private static final int GUI_H = 480;
    private static final int HEADER_H = 30;
    private static final int BODY_Y = HEADER_H + 1;
    private static final int BODY_H = GUI_H - BODY_Y - 2;
    private static final int CARD_INSET = 2;
    private static final int LEFT_W = 190;
    private static final int TOOLS_W = 120;
    private static final int CLIP_H = 38;
    private static final int DIVIDER = 1;
    private static final int BODY_W = GUI_W - 4;
    private static final int INNER_H = BODY_H - CARD_INSET * 2;

    private final IUIHolder uiHolder;
    private final ItemStack terminalItem;
    private final Player player;
    private final ItemTheme theme;
    private final TerminalTheme tokens;

    private List<SchematicData> schematics = new ArrayList<>();
    private int selectedIndex = -1;
    private View activeView = View.SCHEMATIC;
    private ModularUI gui;
    private WidgetGroup mainGroup;
    private WidgetGroup bodyGroup;
    private WidgetGroup previewArea;
    private TextFieldWidget nameInput;
    private DraggableScrollableWidgetGroup schematicsScroll;
    private SchematicData clipboard;
    private ButtonWidget saveEnabledBtn;
    private ButtonWidget saveDisabledBtn;
    private LabelWidget clipboardIndicator;
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

    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder) {
        return new SchematicInterfaceUI(heldHolder).createUI();
    }

    public static ModularUI create(SchematicItemUIFactory.Holder holder, Player player) {
        return new SchematicInterfaceUI(holder, player).createUI();
    }

    private void loadSchematics() {
        schematics = new ArrayList<>();
        CompoundTag tag = terminalItem.getTag();
        if (tag != null && tag.contains("SavedSchematics")) {
            ListTag list = tag.getList("SavedSchematics", 10);
            for (int i = 0; i < list.size(); i++) {
                SchematicData data = SchematicData.fromNBT(list.getCompound(i), player.level().registryAccess());
                if (!"Clipboard".equals(data.name())) schematics.add(data);
            }
        }

        this.clipboard = null;
        if (tag != null && tag.contains("Clipboard")) {
            try {
                SchematicData clip = SchematicData.fromNBT(tag.getCompound("Clipboard"), player.level().registryAccess());
                if (!clip.blocks().isEmpty()) this.clipboard = clip;
            } catch (Exception e) {
                GTCEUTerminalMod.LOGGER.debug("Failed to read clipboard schematic: {}", e.getMessage());
            }
        }
        GTCEUTerminalMod.LOGGER.debug("Loaded {} schematics", schematics.size());
    }

    public ModularUI createUI() {
        this.mainGroup = new WidgetGroup(0, 0, GUI_W, GUI_H);
        mainGroup.setBackground(TerminalUI.transparent());
        this.gui = buildModularUI(mainGroup);
        rebuildContent();
        return gui;
    }

    private void rebuildContent() {
        if (mainGroup == null) return;
        mainGroup.clearAllWidgets();

        if (!theme.isNativeStyle()) {
            mainGroup.addWidget(new WallpaperWidget(2, BODY_Y, BODY_W, BODY_H, () -> this.theme));
        }

        mainGroup.addWidget(buildHeader());
        mainGroup.addWidget(activeView == View.SCHEMATIC ? buildBody() : buildBlueprintBody());
    }

    private void switchView(View view) {
        if (view == activeView || mainGroup == null) return;
        activeView = view;
        rebuildContent();
    }

    private WidgetGroup buildBlueprintBody() {
        WidgetGroup body = new WidgetGroup(2, BODY_Y, BODY_W, BODY_H);
        body.setBackground(TerminalUI.cardBG());
        this.bodyGroup = body;
        this.schematicsScroll = null;
        this.previewArea = null;

        String msg = Component.translatable("gui.gtceuterminal.schematic_interface.blueprint.coming_soon").getString();
        int tw = Minecraft.getInstance().font.width(msg);
        LabelWidget label = new LabelWidget((BODY_W - tw) / 2, BODY_H / 2 - 4, msg);
        label.setColor(tokens.textMuted());
        body.addWidget(label);
        return body;
    }

    private WidgetGroup buildHeader() {
        WidgetGroup header = new WidgetGroup(0, 0, GUI_W, HEADER_H + 2);

        int tabY = 2;
        int iconSize = 18;
        int x = 2;
        int gap = 4;

        int schW = 112;
        WidgetGroup schTab = TerminalTabs.tabCard(x, tabY, schW, HEADER_H,
                activeView == View.SCHEMATIC, cd -> switchView(View.SCHEMATIC));
        schTab.addWidget(HeaderItemIcon.build(8, (HEADER_H - iconSize) / 2, iconSize, terminalItem,
                tokens.panelDisabled(), tokens.border()));
        LabelWidget schLabel = new LabelWidget(8 + iconSize + 6, (HEADER_H - 8) / 2,
                Component.translatable("gui.gtceuterminal.schematic_interface.tab.schematic").getString());
        schLabel.setColor(tokens.textStrong());
        schTab.addWidget(schLabel);
        header.addWidget(schTab);
        x += schW + gap;

        int bpW = 96;
        WidgetGroup bpTab = TerminalTabs.tabCard(x, tabY, bpW, HEADER_H,
                activeView == View.BLUEPRINT, cd -> switchView(View.BLUEPRINT));
        LabelWidget bpLabel = new LabelWidget(10, (HEADER_H - 8) / 2,
                Component.translatable("gui.gtceuterminal.schematic_interface.tab.blueprint").getString());
        bpLabel.setColor(tokens.textStrong());
        bpTab.addWidget(bpLabel);
        header.addWidget(bpTab);

        header.addWidget(TerminalTabs.actionCard(GUI_W - 2 - TerminalTabs.ACTION_CARD_W, tabY, HEADER_H,
                cd -> ThemeEditorDialog.open(mainGroup, ItemTheme.load(terminalItem)),
                cd -> gui.entityPlayer.closeContainer()));

        return header;
    }

    private WidgetGroup buildBody() {
        WidgetGroup body = new WidgetGroup(2, BODY_Y, BODY_W, BODY_H);
        body.setBackground(TerminalUI.cardBG());
        this.bodyGroup = body;

        int innerW = BODY_W - CARD_INSET * 2;
        int topH = INNER_H - CLIP_H - DIVIDER;
        int previewW = innerW - LEFT_W - TOOLS_W - DIVIDER * 2;
        int previewX = CARD_INSET + LEFT_W + DIVIDER;
        int toolsX = previewX + previewW + DIVIDER;

        DraggableScrollableWidgetGroup[] scrollRef = new DraggableScrollableWidgetGroup[1];
        WidgetGroup leftPanel = SchematicListPanel.build(
                CARD_INSET, CARD_INSET, LEFT_W, topH,
                selectedIndex, schematics, this::onSelect, scrollRef);
        this.schematicsScroll = scrollRef[0];
        body.addWidget(leftPanel);

        body.addWidget(new ImageWidget(CARD_INSET + LEFT_W, CARD_INSET, DIVIDER, topH,
                TerminalUI.divider()));

        WidgetGroup preview = new WidgetGroup(previewX, CARD_INSET, previewW, topH);
        preview.setBackground(TerminalUI.surface());
        this.previewArea = preview;
        refreshPreview();
        body.addWidget(preview);

        body.addWidget(new ImageWidget(toolsX - DIVIDER, CARD_INSET, DIVIDER, topH,
                TerminalUI.divider()));

        body.addWidget(buildToolsColumn(toolsX, CARD_INSET, TOOLS_W, topH));

        int clipY = CARD_INSET + topH + DIVIDER;
        body.addWidget(new ImageWidget(CARD_INSET, clipY - DIVIDER, innerW, DIVIDER,
                TerminalUI.divider()));
        body.addWidget(buildClipboardBar(CARD_INSET, clipY, innerW, CLIP_H));

        return body;
    }

    private WidgetGroup buildToolsColumn(int x, int y, int w, int h) {
        WidgetGroup col = new WidgetGroup(x, y, w, h);
        col.setBackground(TerminalUI.panel());

        int pad = 8;
        LabelWidget title = new LabelWidget(pad, 8, TerminalText.strip(Component.translatable(
                "gui.gtceuterminal.schematic_interface.tools_label").getString()));
        title.setColor(tokens.textMuted());
        col.addWidget(title);

        int bx = pad, bw = w - pad * 2, btnH = 22, gap = 6, by = 24;

        col.addWidget(TerminalButton.text3D(bx, by, bw, btnH,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.load").getString(),
                tokens.action(), cd -> pasteSchematic()));
        by += btnH + gap;

        col.addWidget(TerminalButton.text3D(bx, by, bw, btnH,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.rename").getString(),
                tokens.panel(), cd -> renameSchematic()));
        by += btnH + gap;

        col.addWidget(TerminalButton.text3D(bx, by, bw, btnH,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.delete").getString(),
                tokens.errorSurface(), cd -> deleteSchematic()));

        return col;
    }

    private WidgetGroup buildClipboardBar(int x, int y, int w, int h) {
        WidgetGroup bar = new WidgetGroup(x, y, w, h);
        bar.setBackground(TerminalUI.panel());

        LabelWidget clipLbl = new LabelWidget(10, (h - 8) / 2, clipboardIndicatorText());
        clipLbl.setColor(tokens.textMuted());
        this.clipboardIndicator = clipLbl;
        bar.addWidget(clipLbl);

        int pad = 8;
        int gap = 8;
        int saveW = 62, saveH = 20;
        int fieldW = 220, fieldH = 18;
        int saveX = w - pad - saveW;
        int fieldX = saveX - gap - fieldW;
        int saveY = (h - saveH) / 2;

        TextFieldWidget nameField = new TextFieldWidget(fieldX, (h - fieldH) / 2, fieldW, fieldH, null, s -> updateSaveState());
        nameField.setClientSideWidget();
        nameField.setMaxStringLength(32);
        nameField.setBordered(false);
        nameField.setTextColor(tokens.textStrong());
        this.nameInput = nameField;
        bar.addWidget(nameField);

        this.saveDisabledBtn = TerminalButton.disabled(saveX, saveY, saveW, saveH,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.save").getString());
        bar.addWidget(saveDisabledBtn);

        this.saveEnabledBtn = TerminalButton.text3D(saveX, saveY, saveW, saveH,
                Component.translatable("gui.gtceuterminal.schematic_interface.button.save").getString(),
                tokens.successSurface(), cd -> saveSchematic());
        bar.addWidget(saveEnabledBtn);

        updateSaveState();
        return bar;
    }

    private boolean isDuplicateName(String name) {
        for (SchematicData s : schematics) {
            if (s.name().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private void updateSaveState() {
        if (nameInput == null || saveEnabledBtn == null || saveDisabledBtn == null) return;
        String name = nameInput.getCurrentString().trim();
        boolean dup = !name.isEmpty() && isDuplicateName(name);
        boolean valid = hasClipboard() && !name.isEmpty() && !dup;

        saveEnabledBtn.setVisible(valid);
        saveDisabledBtn.setVisible(!valid);

        int border = dup ? tokens.error() : tokens.border();
        nameInput.setBackground(TerminalUI.field(border));
        nameInput.setHoverTexture(TerminalUI.field(dup ? tokens.error() : tokens.textStrong()));
    }

    private String clipboardIndicatorText() {
        return TerminalText.strip(Component.translatable(
                "gui.gtceuterminal.schematic_interface.clipboard_label").getString()) + ": " + clipboardLabel();
    }

    private void refreshClipboardIndicator() {
        if (clipboardIndicator != null) clipboardIndicator.setText(clipboardIndicatorText());
    }

    private String clipboardLabel() {
        if (clipboard == null || clipboard.blocks().isEmpty()) {
            return Component.translatable("gui.gtceuterminal.schematic_interface.clipboard.empty").getString();
        }
        String blocksWord = Component.translatable("gui.gtceuterminal.schematic_interface.clipboard.blocks").getString();
        String typeName = clipboardTypeName();
        return (typeName != null ? typeName + " · " : "") + clipboard.getBlockCount() + " " + blocksWord;
    }

    private String clipboardTypeName() {
        String type = clipboard.multiblockType();
        if (type == null || type.isEmpty()) return null;
        try {
            var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(new net.minecraft.resources.ResourceLocation(type));
            if (block != net.minecraft.world.level.block.Blocks.AIR) {
                String n = block.getName().getString();
                return n.length() > 22 ? n.substring(0, 21) + "…" : n;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void refreshPreview() {
        if (previewArea == null) return;
        previewArea.clearAllWidgets();
        SchematicPreviewPanel.repopulate(previewArea,
                previewArea.getSize().width, previewArea.getSize().height,
                selectedIndex, schematics, player);
    }

    private void onSelect(int index) {
        selectedIndex = (selectedIndex == index) ? -1 : index;
        refreshLeft();
        refreshPreview();
    }

    private void refreshLeft() {
        if (schematicsScroll == null) return;
        SchematicListPanel.repopulate(
                schematicsScroll, schematics, selectedIndex, LEFT_W - 2, this::onSelect);
    }

    private void pasteSchematic() {
        if (selectedIndex < 0 || selectedIndex >= schematics.size()) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.no_schematic_selected");
            return;
        }
        SchematicData s = schematics.get(selectedIndex);
        TerminalNetwork.CHANNEL.sendToServer(
                new CPacketSchematicAction(CPacketSchematicAction.ActionType.LOAD, s.name(), selectedIndex));
        this.clipboard = s;
        refreshClipboardIndicator();
        updateSaveState();
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.schematic_interface.chat.success.loaded_to_clipboard",
                        s.name()), true);
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
        if (schematics.stream().anyMatch(s -> s.name().equalsIgnoreCase(name))) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.name_exists");
            return;
        }

        TerminalNetwork.CHANNEL.sendToServer(new CPacketSchematicAction(CPacketSchematicAction.ActionType.SAVE, name, -1));

        if (clipboard != null) {
            schematics.add(new SchematicData(name, clipboard.multiblockType(),
                    clipboard.blocks(), clipboard.blockEntities(), clipboard.originalFacing()));
            selectedIndex = schematics.size() - 1;
        }

        if (nameInput != null) nameInput.setCurrentString("");
        refreshLeft();
        refreshPreview();
        updateSaveState();
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.schematic_interface.chat.success.saved", name), true);
    }

    private void deleteSchematic() {
        if (selectedIndex < 0 || selectedIndex >= schematics.size()) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.no_schematic_selected");
            return;
        }
        String name = schematics.get(selectedIndex).name();
        TerminalNetwork.CHANNEL.sendToServer(
                new CPacketSchematicAction(CPacketSchematicAction.ActionType.DELETE, name, -1));
        schematics.remove(selectedIndex);
        if (selectedIndex >= schematics.size()) selectedIndex = schematics.size() - 1;
        refreshLeft();
        refreshPreview();
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.schematic_interface.chat.success.deleted", name), true);
    }

    private void renameSchematic() {
        if (selectedIndex < 0 || selectedIndex >= schematics.size()) {
            msg("gui.gtceuterminal.schematic_interface.chat.error.no_schematic_selected");
            return;
        }
        int idx = selectedIndex;
        String current = schematics.get(idx).name();
        SchematicRenameDialog.open(mainGroup, GUI_W, GUI_H, current, newName -> {
            String name = newName.trim();
            if (name.isEmpty() || "Clipboard".equalsIgnoreCase(name)) {
                msg("gui.gtceuterminal.schematic_interface.chat.error.enter_name");
                return;
            }
            if (name.equals(current)) return;
            for (int i = 0; i < schematics.size(); i++) {
                if (i != idx && schematics.get(i).name().equalsIgnoreCase(name)) {
                    msg("gui.gtceuterminal.schematic_interface.chat.error.name_exists");
                    return;
                }
            }
            TerminalNetwork.CHANNEL.sendToServer(
                    new CPacketSchematicAction(CPacketSchematicAction.ActionType.RENAME, name, idx));
            SchematicData old = schematics.get(idx);
            schematics.set(idx, new SchematicData(name, old.multiblockType(),
                    old.blocks(), old.blockEntities(), old.originalFacing()));
            refreshLeft();
            updateSaveState();
        });
    }

    private boolean hasClipboard() {
        return clipboard != null && !clipboard.blocks().isEmpty();
    }

    private void msg(String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private ModularUI buildModularUI(WidgetGroup content) {
        ModularUI ui = new ModularUI(new Size(GUI_W, GUI_H), uiHolder, player);
        ui.widget(content);
        ui.background(TerminalUI.transparent());
        return ui;
    }

    private enum View {
        SCHEMATIC,
        BLUEPRINT
    }
}
