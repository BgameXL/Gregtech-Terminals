package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.client.gui.widget.WallpaperWidget;
import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.client.BlockListWidget;
import com.gtceuterminal.client.gui.widget.SchematicPreviewWidget;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.multiblock.DismantleScanner;
import com.gtceuterminal.common.network.CPacketDismantle;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.gtceuterminal.client.gui.factory.DismantlerItemUIFactory;
import com.gtceuterminal.client.gui.theme.ThemeEditorDialog;
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

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DismantlerUI {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int GUI_W      = 600;
    private static final int GUI_H      = 480;
    private static final int HEADER_H   = 30;
    private static final int INFO_H     = 40;
    private static final int FOOTER_H   = 34;
    private static final int PAD        = 10;
    private static final int SPLIT_X    = 255; // x where block-list panel starts

    // Derived positions
    private static final int CONTENT_Y  = 2 + HEADER_H + 4;
    private static final int CONTENT_H  = GUI_H - CONTENT_Y - INFO_H - FOOTER_H - 6;
    private static final int INFO_Y     = CONTENT_Y + CONTENT_H + 4;
    private static final int FOOTER_Y   = GUI_H - FOOTER_H - 2;

    // ── Colors ────────────────────────────────────────────────────────────────
    private int COLOR_BG_DARK;
    private int COLOR_BG_MEDIUM;
    private int COLOR_BG_LIGHT;
    private int COLOR_BORDER_LIGHT;
    private static final int COLOR_BORDER_DARK = 0xFF0A0A0A;
    private static final int COLOR_TEXT_WHITE  = 0xFFFFFFFF;
    private static final int COLOR_TEXT_GRAY   = 0xFFAAAAAA;
    private static final int COLOR_SUCCESS     = 0xFF00CC00;
    private static final int COLOR_WARNING     = 0xFFFFAA00;
    private static final int COLOR_ERROR       = 0xFFFF4444;
    private static final int COLOR_DISMANTLE   = 0xFF7A1A1A; // dark red for destructive action

    // ── State ─────────────────────────────────────────────────────────────────
    private WidgetGroup mainGroup;
    private ItemTheme   theme;

    private final IUIHolder  uiHolder;
    private final BlockPos   controllerPos;
    private final Player     player;
    private final DismantleScanner.ScanResult scanResult;

    // ── Constructors ──────────────────────────────────────────────────────────
    public DismantlerUI(HeldItemUIFactory.HeldItemHolder heldHolder, BlockPos controllerPos) {
        this.uiHolder      = heldHolder;
        this.controllerPos = controllerPos;
        this.player        = heldHolder.player;
        applyTheme(ItemTheme.load(heldHolder.held));
        this.scanResult    = computeScanResult(player, controllerPos);
    }

    public DismantlerUI(DismantlerItemUIFactory.Holder holder, Player player) {
        this.uiHolder      = holder;
        this.controllerPos = holder.controllerPos;
        this.player        = player;
        applyTheme(ItemTheme.load(holder.item));
        this.scanResult    = buildScanResult(player, holder.controllerPos, holder.scannedPositions);
    }

    private void applyTheme(ItemTheme t) {
        this.theme         = t;
        COLOR_BG_DARK      = t.bgColor;
        COLOR_BG_MEDIUM    = t.panelColor;
        COLOR_BG_LIGHT     = t.isNativeStyle() ? 0xFF3A3A3A : t.accent(0xAA);
        COLOR_BORDER_LIGHT = t.isNativeStyle() ? 0xFF555555 : t.accent(0xFF);
    }

    private static DismantleScanner.ScanResult buildScanResult(Player player, BlockPos controllerPos,
                                                               java.util.Set<BlockPos> preScanned) {
        if (!preScanned.isEmpty()) {
            // Build counts from the pre-scanned positions using the client level
            var level = player.level();
            java.util.Map<net.minecraft.world.level.block.Block, Integer> counts = new java.util.HashMap<>();
            java.util.Map<net.minecraft.world.level.block.Block, java.util.List<BlockPos>> positions = new java.util.HashMap<>();
            for (BlockPos pos : preScanned) {
                var state = level.getBlockState(pos);
                if (state.isAir()) continue;
                var block = state.getBlock();
                counts.merge(block, 1, Integer::sum);
                positions.computeIfAbsent(block, k -> new java.util.ArrayList<>()).add(pos);
            }
            return new DismantleScanner.ScanResult(controllerPos, preScanned, counts, positions);
        }
        return computeScanResult(player, controllerPos);
    }

    private static DismantleScanner.ScanResult computeScanResult(Player player, BlockPos pos) {
        try {
            var be = player.level().getBlockEntity(pos);
            if (be instanceof com.gregtechceu.gtceu.api.machine.IMachineBlockEntity mbe) {
                var meta = mbe.getMetaMachine();
                if (meta instanceof com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine ctrl) {
                    return DismantleScanner.scanMultiblock(player.level(), ctrl);
                }
            }
        } catch (Throwable t) {
            GTCEUTerminalMod.LOGGER.warn("DismantlerUI: could not scan multiblock at {}", pos, t);
        }
        return DismantleScanner.ScanResult.empty();
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
        mainGroup.addWidget(buildPreviewPanel());
        mainGroup.addWidget(buildBlockListPanel());
        mainGroup.addWidget(buildInfoBar());
        mainGroup.addWidget(buildFooter());

        setupParade();
        return createUIWithViewport(mainGroup);
    }

    private ModularUI createUIWithViewport(WidgetGroup content) {
        try {
            var mc     = net.minecraft.client.Minecraft.getInstance();
            int sw     = mc.getWindow().getGuiScaledWidth();
            int sh     = mc.getWindow().getGuiScaledHeight();
            int margin = 10;
            int maxW   = sw - margin * 2;
            int maxH   = sh - margin * 2;

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
        g.addWidget(new ImageWidget(0,        0,        GUI_W, 2,     new ColorRectTexture(COLOR_BORDER_LIGHT)));
        g.addWidget(new ImageWidget(0,        0,        2,     GUI_H, new ColorRectTexture(COLOR_BORDER_LIGHT)));
        g.addWidget(new ImageWidget(GUI_W - 2, 0,       2,     GUI_H, new ColorRectTexture(COLOR_BORDER_DARK)));
        g.addWidget(new ImageWidget(0,        GUI_H - 2, GUI_W, 2,   new ColorRectTexture(COLOR_BORDER_DARK)));
        return g;
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private WidgetGroup buildHeader() {
        WidgetGroup header = new WidgetGroup(2, 2, GUI_W - 4, HEADER_H);
        header.setBackground(theme.headerTexture());

        int titleX = 10;
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

        // Title — left
        LabelWidget title = new LabelWidget(titleX, 10,
                Component.translatable("gui.gtceuterminal.dismantler.title").getString());
        title.setTextColor(theme.isBundleStyle() ? theme.labelColor() : COLOR_TEXT_WHITE);
        header.addWidget(title);

        // Coords — center
        String coords = String.format("(%d, %d, %d)",
                controllerPos.getX(), controllerPos.getY(), controllerPos.getZ());
        LabelWidget coordsLabel = new LabelWidget((GUI_W - 4) / 2 - 40, 10, "§7" + coords);
        coordsLabel.setTextColor(COLOR_TEXT_GRAY);
        header.addWidget(coordsLabel);

        
        ButtonWidget gearBtn = new ButtonWidget(GUI_W - 50, 6, 18, 18,
                new ColorRectTexture(0x00000000),
                cd -> ThemeEditorDialog.open(mainGroup, ItemTheme.load(player.getMainHandItem())));
        gearBtn.setButtonTexture(new TextTexture("§7⚙").setWidth(18).setType(TextTexture.TextType.NORMAL));
        gearBtn.setHoverTexture(new ColorRectTexture(0x40FFFFFF));
        gearBtn.setHoverTooltips(Component.translatable("gui.gtceuterminal.theme_settings").getString());
        header.addWidget(gearBtn);

        
        ButtonWidget closeBtn = new ButtonWidget(GUI_W - 28, 6, 20, 18,
                new GuiTextureGroup(
                        new ColorRectTexture(COLOR_BG_MEDIUM),
                        new ColorBorderTexture(1, COLOR_BORDER_LIGHT)),
                cd -> closeUI());
        closeBtn.setButtonTexture(new TextTexture("§c✕").setWidth(20).setType(TextTexture.TextType.NORMAL));
        closeBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(0xFFAA0000),
                new ColorBorderTexture(1, COLOR_TEXT_WHITE)));
        header.addWidget(closeBtn);

        return header;
    }

    // ── Preview panel (left) ──────────────────────────────────────────────────
    private WidgetGroup buildPreviewPanel() {
        int w = SPLIT_X - PAD - 4;
        WidgetGroup panel = new WidgetGroup(PAD, CONTENT_Y, w, CONTENT_H);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(COLOR_BG_LIGHT),
                new ColorBorderTexture(1, COLOR_BORDER_DARK)));

        LabelWidget label = new LabelWidget(6, 5,
                Component.translatable("gui.gtceuterminal.dismantler.preview_label").getString());
        label.setTextColor(COLOR_TEXT_GRAY);
        panel.addWidget(label);

        if (scanResult != null && scanResult.getTotalBlocks() > 0) {
            SchematicData previewData = buildSchematicFromScan();
            if (previewData != null && !previewData.getBlocks().isEmpty()) {
                panel.addWidget(new SchematicPreviewWidget(6, 20, w - 12, CONTENT_H - 26, previewData));
            } else {
                addNoDataLabel(panel, w);
            }
        } else {
            addNoDataLabel(panel, w);
        }

        return panel;
    }

    private void addNoDataLabel(WidgetGroup panel, int w) {
        LabelWidget noData = new LabelWidget(w / 2 - 30, CONTENT_H / 2,
                Component.translatable("gui.gtceuterminal.dismantler.no_data").getString());
        noData.setTextColor(COLOR_ERROR);
        panel.addWidget(noData);
    }

    private SchematicData buildSchematicFromScan() {
        try {
            var level = player.level();
            java.util.Map<BlockPos, BlockState> blocks = new java.util.HashMap<>();
            java.util.Map<BlockPos, CompoundTag> blockEntities = new java.util.HashMap<>();

            for (BlockPos worldPos : scanResult.getAllBlocks()) {
                BlockState state = level.getBlockState(worldPos);
                if (state.isAir()) continue;
                BlockPos rel = worldPos.subtract(controllerPos);
                blocks.put(rel, state);
                BlockEntity be = level.getBlockEntity(worldPos);
                if (be != null) {
                    try {
                        blockEntities.put(rel, be.saveWithFullMetadata());
                    } catch (RuntimeException e) {
                        GTCEUTerminalMod.LOGGER.debug("DismantlerUI: could not serialize block entity at {}: {}", worldPos, e.getMessage());
                    }
                }
            }
            return new SchematicData("preview", "", blocks, blockEntities, "south");
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("DismantlerUI: failed to build preview schematic", e);
            return null;
        }
    }

    // ── Block list panel (right) ───────────────────────────────────────────────
    private WidgetGroup buildBlockListPanel() {
        int x = SPLIT_X;
        int w = GUI_W - x - PAD;
        WidgetGroup panel = new WidgetGroup(x, CONTENT_Y, w, CONTENT_H);
        panel.setBackground(new GuiTextureGroup(
                new ColorRectTexture(COLOR_BG_LIGHT),
                new ColorBorderTexture(1, COLOR_BORDER_DARK)));

        LabelWidget recoverLabel = new LabelWidget(6, 5,
                Component.translatable("gui.gtceuterminal.dismantler.blocks_to_recover").getString());
        recoverLabel.setTextColor(COLOR_TEXT_GRAY);
        panel.addWidget(recoverLabel);

        if (scanResult != null) {
            LabelWidget totalLabel = new LabelWidget(6, 18,
                    Component.translatable("gui.gtceuterminal.dismantler.total_blocks",
                            scanResult.getTotalBlocks()).getString());
            totalLabel.setTextColor(COLOR_TEXT_WHITE);
            panel.addWidget(totalLabel);

            panel.addWidget(new BlockListWidget(6, 34, w - 12, CONTENT_H - 40, scanResult));
        }

        return panel;
    }

    // ── Info bar ──────────────────────────────────────────────────────────────
    private WidgetGroup buildInfoBar() {
        WidgetGroup bar = new WidgetGroup(PAD, INFO_Y, GUI_W - PAD * 2, INFO_H);
        bar.setBackground(new GuiTextureGroup(
                new ColorRectTexture(COLOR_BG_MEDIUM),
                new ColorBorderTexture(1, COLOR_BORDER_DARK)));

        // Count empty inventory slots
        int emptySlots = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).isEmpty()) emptySlots++;
        }

        bar.addWidget(new LabelWidget(10, 8,
                Component.translatable("gui.gtceuterminal.dismantler.inventory_empty_slots",
                        emptySlots).getString())
                .setTextColor(COLOR_TEXT_WHITE));

        // Compare against stacks needed (not distinct types)
        int slotsNeeded = (scanResult != null) ? scanResult.getItemsToRecover().size() : 0;
        boolean hasSpace = emptySlots >= slotsNeeded;

        bar.addWidget(new LabelWidget(10, 22,
                Component.translatable(hasSpace
                        ? "gui.gtceuterminal.dismantler.enough_space"
                        : "gui.gtceuterminal.dismantler.warning_not_enough_space").getString())
                .setTextColor(hasSpace ? COLOR_SUCCESS : COLOR_WARNING));

        return bar;
    }

    // ── Footer (action buttons) ───────────────────────────────────────────────
    private WidgetGroup buildFooter() {
        WidgetGroup footer = new WidgetGroup(PAD, FOOTER_Y, GUI_W - PAD * 2, FOOTER_H);

        int btnH = 24;
        int btnW = (GUI_W - PAD * 2 - 10) / 2;

        // Dismantle — red-tinted (destructive)
        ButtonWidget dismantleBtn = new ButtonWidget(0, (FOOTER_H - btnH) / 2, btnW, btnH,
                new GuiTextureGroup(
                        new ColorRectTexture(COLOR_DISMANTLE),
                        new ColorBorderTexture(1, COLOR_ERROR)),
                cd -> performDismantle());
        dismantleBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.dismantler.action.dismantle").getString())
                .setWidth(btnW).setType(TextTexture.TextType.NORMAL));
        dismantleBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(0xFF9A2A2A),
                new ColorBorderTexture(2, COLOR_TEXT_WHITE)));
        footer.addWidget(dismantleBtn);

        // Cancel — neutral
        ButtonWidget cancelBtn = new ButtonWidget(btnW + 10, (FOOTER_H - btnH) / 2, btnW, btnH,
                new GuiTextureGroup(
                        new ColorRectTexture(COLOR_BG_MEDIUM),
                        new ColorBorderTexture(1, COLOR_BORDER_LIGHT)),
                cd -> closeUI());
        cancelBtn.setButtonTexture(new TextTexture(
                Component.translatable("gui.gtceuterminal.dismantler.action.cancel").getString())
                .setWidth(btnW).setType(TextTexture.TextType.NORMAL));
        cancelBtn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(COLOR_BG_MEDIUM),
                new ColorBorderTexture(2, COLOR_TEXT_WHITE)));
        footer.addWidget(cancelBtn);

        return footer;
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void performDismantle() {
        GTCEUTerminalMod.LOGGER.info("Dismantling multiblock at {}", controllerPos);
        TerminalNetwork.CHANNEL.sendToServer(new CPacketDismantle(controllerPos));
        closeUI();
        player.displayClientMessage(
                Component.translatable("gui.gtceuterminal.dismantler.chat.success"), false);
    }

    private void closeUI() {
        if (player.containerMenu != null) player.closeContainer();
    }

    // ── Static factory methods ────────────────────────────────────────────────
    public static ModularUI create(HeldItemUIFactory.HeldItemHolder heldHolder, BlockPos controllerPos) {
        return new DismantlerUI(heldHolder, controllerPos).createUI();
    }

    public static ModularUI create(DismantlerItemUIFactory.Holder holder, Player player) {
        return new DismantlerUI(holder, player).createUI();
    }
}