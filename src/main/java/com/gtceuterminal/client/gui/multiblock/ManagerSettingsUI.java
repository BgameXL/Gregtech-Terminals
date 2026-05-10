package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.common.ae2.MENetworkScanner;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.client.gui.factory.MultiStructureManagerUIFactory;

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
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ManagerSettingsUI {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int GUI_W    = 280;
    private static final int GUI_H    = 215;
    private static final int HEADER_H = 28;
    private static final int PAD      = 8;
    private static final int ROW_H    = 32; // height per setting row
    private static final int CTL_W    = 56; // control (button / input) width
    private static final int CTL_H    = 18;

    // ── Colors ────────────────────────────────────────────────────────────────
    private int COLOR_BG_DARK;
    private int COLOR_BG_MEDIUM;
    private int COLOR_BG_LIGHT;
    private int COLOR_BORDER_LIGHT;
    private static final int COLOR_BORDER_DARK = 0xFF0A0A0A;
    private static final int COLOR_TEXT_WHITE  = 0xFFFFFFFF;
    private static final int COLOR_TEXT_GRAY   = 0xFFAAAAAA;
    private static final int COLOR_HINT        = 0xFF888888;
    private static final int COLOR_ON          = 0xFF1A4A1A;
    private static final int COLOR_ON_BORDER   = 0xFF00CC00;
    private static final int COLOR_OFF         = 0xFF3A1A1A;
    private static final int COLOR_OFF_BORDER  = 0xFF884444;

    // ── State ─────────────────────────────────────────────────────────────────
    private ItemTheme    theme;
    private final IUIHolder  uiHolder;
    private final ItemStack  itemStack;
    private final Player     player;

    // ── Constructors ──────────────────────────────────────────────────────────
    public ManagerSettingsUI(HeldItemUIFactory.HeldItemHolder heldHolder) {
        this.uiHolder  = heldHolder;
        this.itemStack = heldHolder.held;
        this.player    = heldHolder.player;
        applyTheme();
    }

    public ManagerSettingsUI(MultiStructureManagerUIFactory.Holder holder, Player player) {
        this.uiHolder  = holder;
        this.itemStack = holder.getTerminalItem();
        this.player    = player;
        applyTheme();
    }

    private void applyTheme() {
        this.theme         = ItemTheme.load(itemStack);
        COLOR_BG_DARK      = theme.bgColor;
        COLOR_BG_MEDIUM    = theme.panelColor;
        COLOR_BG_LIGHT     = theme.isNativeStyle() ? 0xFF3A3A3A : theme.accent(0xAA);
        COLOR_BORDER_LIGHT = theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF);
    }

    // ── UI construction ───────────────────────────────────────────────────────
    public ModularUI createUI() {
        WidgetGroup root = new WidgetGroup(0, 0, GUI_W, GUI_H);
        root.setBackground(new com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture(0x00000000));

        if (!theme.isNativeStyle()) {
            root.addWidget(new com.gtceuterminal.client.gui.widget.WallpaperWidget(
                    0, 0, GUI_W, GUI_H, () -> this.theme));
        } else {
            root.setBackground(theme.backgroundTexture());
        }

        // Outer border
        root.addWidget(new ImageWidget(0,        0,        GUI_W, 2,     new ColorRectTexture(COLOR_BORDER_LIGHT)));
        root.addWidget(new ImageWidget(0,        0,        2,     GUI_H, new ColorRectTexture(COLOR_BORDER_LIGHT)));
        root.addWidget(new ImageWidget(GUI_W - 2, 0,       2,     GUI_H, new ColorRectTexture(COLOR_BORDER_DARK)));
        root.addWidget(new ImageWidget(0,        GUI_H - 2, GUI_W, 2,   new ColorRectTexture(COLOR_BORDER_DARK)));

        root.addWidget(buildHeader());
        root.addWidget(buildSettingsPanel());

        setupParade(root);
        ModularUI gui = new ModularUI(new Size(GUI_W, GUI_H), uiHolder, player);
        gui.widget(root);
        gui.background(theme.modularUIBackground());
        return gui;
    }

    // ── Parade ────────────────────────────────────────────────────────────────
    private void setupParade(com.lowdragmc.lowdraglib.gui.widget.WidgetGroup root) {
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

    // ── Header ────────────────────────────────────────────────────────────────
    private WidgetGroup buildHeader() {
        WidgetGroup header = new WidgetGroup(2, 2, GUI_W - 4, HEADER_H);
        header.setBackground(theme.headerTexture());

        // Bundle icon — shown left of the title when a modpack theme is active
        int titleX = 10;
        if (theme.isBundleStyle()) {
            com.gtceuterminal.common.theme.bundle.ThemeBundle bundle =
                    com.gtceuterminal.common.theme.bundle.ThemeBundleRegistry.get(theme.bundleId);
            if (bundle != null) {
                com.lowdragmc.lowdraglib.gui.texture.IGuiTexture icon = bundle.iconTexture();
                if (icon != null) {
                    int iconSize = 18;
                    int iconY    = (HEADER_H - iconSize) / 2;
                    header.addWidget(new ImageWidget(titleX, iconY, iconSize, iconSize, icon));
                    titleX += iconSize + 4;
                }
            }
        }

        LabelWidget title = new LabelWidget(titleX, 9,
                Component.translatable("gui.gtceuterminal.manager_settings.title").getString());
        title.setTextColor(theme.isBundleStyle() ? theme.labelColor() : COLOR_TEXT_WHITE);
        header.addWidget(title);

        return header;
    }

    // ── Settings panel ────────────────────────────────────────────────────────
    private WidgetGroup buildSettingsPanel() {
        int panelY = 2 + HEADER_H + 3;
        int panelH = GUI_H - panelY - 4;
        WidgetGroup panel = new WidgetGroup(PAD, panelY, GUI_W - PAD * 2, panelH);
        panel.setBackground(theme.panelTexture());

        int y = 8;
        int innerW = GUI_W - PAD * 2;

        // 1. No Hatch Mode
        y = addToggleRow(panel, y, innerW,
                Component.translatable("gui.gtceuterminal.manager_settings.hatch_mode.label").getString(),
                Component.translatable("gui.gtceuterminal.manager_settings.hatch_mode.hint_toggle").getString(),
                Component.translatable("gui.gtceuterminal.manager_settings.hatch_mode.tooltip").getString(),
                () -> getNoHatchMode() == 1,
                () -> toggleNoHatchMode());

        // 2. Tier Mode
        y = addInputRow(panel, y, innerW,
                Component.translatable("gui.gtceuterminal.manager_settings.tier_mode.label").getString(),
                Component.translatable("gui.gtceuterminal.manager_settings.tier_mode.hint_scroll_type").getString(),
                Component.translatable("gui.gtceuterminal.manager_settings.tier_mode.tooltip").getString(),
                () -> String.valueOf(getTierMode()),
                val -> setTierMode(parseIntSafe(val, 1)),
                1, 16);

        // 3. Repeat Count
        y = addInputRow(panel, y, innerW,
                Component.translatable("gui.gtceuterminal.manager_settings.repeat_count.label").getString(),
                Component.translatable("gui.gtceuterminal.manager_settings.repeat_count.hint_layers").getString(),
                Component.translatable("gui.gtceuterminal.manager_settings.repeat_count.tooltip").getString(),
                () -> String.valueOf(getRepeatCount()),
                val -> setRepeatCount(parseIntSafe(val, 0)),
                0, 99);

        // 4. Use AE2 (only if AE2 is present)
        if (MENetworkScanner.isAE2Available()) {
            addToggleRow(panel, y, innerW,
                    Component.translatable("gui.gtceuterminal.manager_settings.use_ae2.label").getString(),
                    Component.translatable("gui.gtceuterminal.manager_settings.use_ae2.hint_use_materials").getString(),
                    Component.translatable("gui.gtceuterminal.manager_settings.use_ae2.tooltip").getString(),
                    () -> getIsUseAE() == 1,
                    () -> toggleIsUseAE());
        }

        return panel;
    }

    // ── Row helpers ───────────────────────────────────────────────────────────
    private int addToggleRow(WidgetGroup panel, int y, int panelW,
                             String label, String hint, String tooltip,
                             java.util.function.BooleanSupplier getter,
                             Runnable toggler) {
        int ctlX = panelW - CTL_W - 4;

        LabelWidget lbl = new LabelWidget(8, y, label);
        lbl.setTextColor(COLOR_TEXT_WHITE);
        lbl.setHoverTooltips(Component.literal(tooltip));
        panel.addWidget(lbl);

        // Toggle button — texture is set initially and re-set on each click
        boolean[] state = { getter.getAsBoolean() };

        ButtonWidget[] btnRef = new ButtonWidget[1];
        btnRef[0] = new ButtonWidget(ctlX, y - 2, CTL_W, CTL_H,
                new ColorRectTexture(0x00000000),
                cd -> {
                    toggler.run();
                    state[0] = getter.getAsBoolean();
                    updateToggleTexture(btnRef[0], state[0]);
                });
        btnRef[0].setHoverTooltips(Component.literal(tooltip));
        updateToggleTexture(btnRef[0], state[0]);

        ButtonWidget btn = btnRef[0];
        panel.addWidget(btn);

        // Hint
        if (hint != null && !hint.isBlank()) {
            LabelWidget hintLbl = new LabelWidget(8, y + 14, hint);
            hintLbl.setTextColor(COLOR_HINT);
            panel.addWidget(hintLbl);
        }

        return y + ROW_H;
    }

    private int addInputRow(WidgetGroup panel, int y, int panelW,
                            String label, String hint, String tooltip,
                            java.util.function.Supplier<String> getter,
                            java.util.function.Consumer<String> setter,
                            int min, int max) {
        int ctlX = panelW - CTL_W - 4;

        LabelWidget lbl = new LabelWidget(8, y, label);
        lbl.setTextColor(COLOR_TEXT_GRAY);
        lbl.setHoverTooltips(Component.literal(tooltip));
        panel.addWidget(lbl);

        TextFieldWidget input = new TextFieldWidget(ctlX, y - 2, CTL_W, CTL_H, getter, setter);
        input.setNumbersOnly(min, max);
        input.setTextColor(COLOR_TEXT_WHITE);
        input.setBackground(new GuiTextureGroup(
                new ColorRectTexture(COLOR_BG_DARK),
                new ColorBorderTexture(1, COLOR_BORDER_LIGHT)));
        input.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(COLOR_BG_DARK),
                new ColorBorderTexture(1, COLOR_TEXT_WHITE)));
        input.setWheelDur(1);
        input.setHoverTooltips(Component.literal(tooltip));
        panel.addWidget(input);

        if (hint != null && !hint.isBlank()) {
            LabelWidget hintLbl = new LabelWidget(8, y + 14, hint);
            hintLbl.setTextColor(COLOR_HINT);
            panel.addWidget(hintLbl);
        }

        return y + ROW_H;
    }

    private void updateToggleTexture(ButtonWidget btn, boolean on) {
        int bg     = on ? COLOR_ON  : COLOR_OFF;
        int border = on ? COLOR_ON_BORDER : COLOR_OFF_BORDER;
        String txt = on ? "§aYES" : "§cNO";
        GuiTextureGroup tex = new GuiTextureGroup(
                new ColorRectTexture(bg),
                new ColorBorderTexture(1, border),
                new TextTexture(txt).setWidth(CTL_W).setType(TextTexture.TextType.NORMAL));
        btn.setButtonTexture(tex);
        btn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(bg),
                new ColorBorderTexture(1, COLOR_TEXT_WHITE),
                new TextTexture(txt).setWidth(CTL_W).setType(TextTexture.TextType.NORMAL)));
    }

    // ── NBT helpers ───────────────────────────────────────────────────────────
    private int getNoHatchMode() {
        CompoundTag tag = itemStack.getTag();
        return (tag != null && tag.contains("NoHatchMode")) ? tag.getInt("NoHatchMode") : 0;
    }
    private void toggleNoHatchMode() {
        itemStack.getOrCreateTag().putInt("NoHatchMode", getNoHatchMode() == 1 ? 0 : 1);
    }

    private int getTierMode() {
        CompoundTag tag = itemStack.getTag();
        return (tag != null && tag.contains("TierMode")) ? tag.getInt("TierMode") : 1;
    }
    private void setTierMode(int tier) {
        itemStack.getOrCreateTag().putInt("TierMode", Math.max(1, Math.min(16, tier)));
    }

    private int getRepeatCount() {
        CompoundTag tag = itemStack.getTag();
        return (tag != null && tag.contains("RepeatCount")) ? tag.getInt("RepeatCount") : 0;
    }
    private void setRepeatCount(int count) {
        itemStack.getOrCreateTag().putInt("RepeatCount", Math.max(0, Math.min(99, count)));
    }

    private int getIsUseAE() {
        CompoundTag tag = itemStack.getTag();
        return (tag != null && tag.contains("IsUseAE")) ? tag.getInt("IsUseAE") : 0;
    }
    private void toggleIsUseAE() {
        itemStack.getOrCreateTag().putInt("IsUseAE", getIsUseAE() == 1 ? 0 : 1);
    }

    private int parseIntSafe(String value, int defaultValue) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) { return defaultValue; }
    }
}