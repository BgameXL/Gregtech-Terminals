package com.gtceuterminal.client.gui.multiblock;

import com.gtceuterminal.client.gui.style.TerminalText;
import com.gtceuterminal.client.gui.widget.TerminalParade;
import com.gtceuterminal.common.ae2.MENetworkScanner;
import com.gtceuterminal.client.gui.style.BevelTexture;
import com.gtceuterminal.client.gui.style.Palette;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;
import com.gtceuterminal.client.gui.widget.HeaderItemIcon;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.common.gui.factory.MultiStructureManagerUIFactory;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ManagerSettingsUI {

    private static final int GUI_W = 280;
    private static final int GUI_H = 215;
    private static final int HEADER_H = 28;
    private static final int PAD = 8;
    private static final int ROW_H = 32;
    private static final int CTL_W = 56;
    private static final int CTL_H = 18;

    private ItemTheme theme;
    private final IUIHolder uiHolder;
    private final ItemStack itemStack;
    private final Player player;

    private int noHatchMode;
    private int tierMode;
    private int repeatCount;
    private int isUseAE;

    public ManagerSettingsUI(HeldItemUIFactory.HeldItemHolder heldHolder) {
        this.uiHolder = heldHolder;
        this.itemStack = heldHolder.held;
        this.player = heldHolder.player;
        loadFromItem();
        applyTheme();
    }

    public ManagerSettingsUI(MultiStructureManagerUIFactory.Holder holder, Player player) {
        this.uiHolder = holder;
        this.itemStack = holder.getTerminalItem();
        this.player = player;
        loadFromItem();
        applyTheme();
    }

    ManagerSettingsUI(ItemStack itemStack, Player player) {
        this.uiHolder = null;
        this.itemStack = itemStack;
        this.player = player;
        loadFromItem();
        applyTheme();
    }

    private void loadFromItem() {
        CompoundTag tag = itemStack.getTag();
        this.noHatchMode = tag != null && tag.contains("NoHatchMode") ? tag.getInt("NoHatchMode") : 0;
        this.tierMode = tag != null && tag.contains("TierMode") ? tag.getInt("TierMode") : 0;
        this.repeatCount = tag != null && tag.contains("RepeatCount") ? tag.getInt("RepeatCount") : 0;
        this.isUseAE = tag != null && tag.contains("IsUseAE") ? tag.getInt("IsUseAE") : 0;
    }

    private void applyTheme() {
        this.theme = ItemTheme.load(itemStack);
        TerminalUI.use(theme);
    }

    public ModularUI createUI() {
        WidgetGroup root = new WidgetGroup(0, 0, GUI_W, GUI_H);
        root.setBackground(new ColorRectTexture(Palette.CLEAR));

        if (!theme.isNativeStyle()) {
            root.addWidget(new com.gtceuterminal.client.gui.widget.WallpaperWidget(
                    0, 0, GUI_W, GUI_H, () -> this.theme));
        } else {
            root.setBackground(theme.backgroundTexture());
        }

        addFrame(root);

        root.addWidget(buildHeader());
        root.addWidget(buildSettingsPanel());

        TerminalParade.setup(theme, GUI_W, GUI_H);

        ModularUI gui = new ModularUI(new Size(GUI_W, GUI_H), uiHolder, player);
        gui.widget(root);
        gui.background(theme.modularUIBackground());
        return gui;
    }

    public static void openDialog(WidgetGroup rootGroup, int parentW, int parentH,
                                  ItemStack item, Player player) {
        new ManagerSettingsUI(item, player).buildDialog(rootGroup, parentW, parentH);
    }

    private void buildDialog(WidgetGroup rootGroup, int parentW, int parentH) {
        DialogWidget dialog = new DialogWidget(rootGroup, true);
        dialog.setBackground(new ColorRectTexture(Palette.SCRIM));
        dialog.setClickClose(false);

        int ox = (parentW - GUI_W) / 2;
        int oy = (parentH - GUI_H) / 2;

        WidgetGroup root = new WidgetGroup(ox, oy, GUI_W, GUI_H);
        root.setBackground(new ColorRectTexture(Palette.CLEAR));

        if (!theme.isNativeStyle()) {
            root.addWidget(new com.gtceuterminal.client.gui.widget.WallpaperWidget(
                    0, 0, GUI_W, GUI_H, () -> this.theme));
        } else {
            root.setBackground(theme.backgroundTexture());
        }

        addFrame(root);

        WidgetGroup header = buildHeader();
        ButtonWidget close = TerminalButton.icon3d(
                header.getSize().width - 24, (HEADER_H - 18) / 2, 18, "×", cd -> dialog.close());
        close.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        header.addWidget(close);
        root.addWidget(header);

        root.addWidget(buildSettingsPanel());

        dialog.addWidget(root);
    }

    private WidgetGroup buildHeader() {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup header = new WidgetGroup(2, 2, GUI_W - 4, HEADER_H);
        header.setBackground(new BevelTexture(tokens.panel()));

        int titleX = 10;
        int iconSize = 18;
        int iconY = (HEADER_H - iconSize) / 2;
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
            header.addWidget(HeaderItemIcon.build(titleX, iconY, iconSize, itemStack,
                    tokens.panelDisabled(), tokens.border()));
            titleX += iconSize + 6;
        }

        LabelWidget title = new LabelWidget(titleX, 9,
                Component.translatable("gui.gtceuterminal.manager_settings.title").getString());
        title.setColor(theme.isBundleStyle() ? theme.labelColor() : tokens.textStrong());
        header.addWidget(title);

        return header;
    }

    private WidgetGroup buildSettingsPanel() {
        int panelY = 2 + HEADER_H + 3;
        int panelH = GUI_H - panelY - 4;
        return buildSettingsPanel(PAD, panelY, GUI_W - PAD * 2, panelH);
    }

    WidgetGroup buildEmbeddedPanel(int x, int y, int width, int height) {
        return buildSettingsPanel(x, y, width, height);
    }

    private WidgetGroup buildSettingsPanel(int x, int y, int width, int height) {
        TerminalTheme tokens = TerminalUI.tokens();
        WidgetGroup panel = new WidgetGroup(x, y, width, height);
        panel.setBackground(new ColorRectTexture(tokens.surface()));

        int rowY = 8;
        int innerW = width;

        rowY = addToggleRow(panel, rowY, innerW,
                Component.translatable("gui.gtceuterminal.manager_settings.hatch_mode.label").getString(),
                Component.translatable("gui.gtceuterminal.manager_settings.hatch_mode.hint_toggle").getString(),
                Component.translatable("gui.gtceuterminal.manager_settings.hatch_mode.tooltip").getString(),
                () -> getNoHatchMode() == 1,
                () -> toggleNoHatchMode());

        rowY = addInputRow(panel, rowY, innerW,
                Component.translatable("gui.gtceuterminal.manager_settings.tier_mode.label").getString(),
                Component.translatable("gui.gtceuterminal.manager_settings.tier_mode.hint_scroll_type").getString(),
                Component.translatable("gui.gtceuterminal.manager_settings.tier_mode.tooltip").getString(),
                () -> String.valueOf(getTierMode()),
                val -> {
                    try {
                        setTierMode(Integer.parseInt(val));
                    } catch (NumberFormatException ignored) {
                    }
                },
                0, 14);

        rowY = addInputRow(panel, rowY, innerW,
                Component.translatable("gui.gtceuterminal.manager_settings.repeat_count.label").getString(),
                Component.translatable("gui.gtceuterminal.manager_settings.repeat_count.hint_layers").getString(),
                Component.translatable("gui.gtceuterminal.manager_settings.repeat_count.tooltip").getString(),
                () -> String.valueOf(getRepeatCount()),
                val -> {
                    try {
                        setRepeatCount(Integer.parseInt(val));
                    } catch (NumberFormatException ignored) {
                    }
                },
                0, 99);

        if (MENetworkScanner.isAE2Available()) {
            addToggleRow(panel, rowY, innerW,
                    Component.translatable("gui.gtceuterminal.manager_settings.use_ae2.label").getString(),
                    Component.translatable("gui.gtceuterminal.manager_settings.use_ae2.hint_use_materials").getString(),
                    Component.translatable("gui.gtceuterminal.manager_settings.use_ae2.tooltip").getString(),
                    () -> getIsUseAE() == 1,
                    () -> toggleIsUseAE());
        }

        return panel;
    }

    private int addToggleRow(WidgetGroup panel, int y, int panelW,
                             String label, String hint, String tooltip,
                             java.util.function.BooleanSupplier getter,
                             Runnable toggler) {
        int ctlX = panelW - CTL_W - 4;

        LabelWidget lbl = new LabelWidget(8, y, label);
        lbl.setColor(TerminalUI.tokens().textStrong());
        lbl.setHoverTooltips(Component.literal(tooltip));
        panel.addWidget(lbl);

        boolean[] state = {getter.getAsBoolean()};

        ButtonWidget[] btnRef = new ButtonWidget[1];
        btnRef[0] = new ButtonWidget(ctlX, y - 2, CTL_W, CTL_H,
                new ColorRectTexture(Palette.CLEAR),
                cd -> {
                    toggler.run();
                    state[0] = getter.getAsBoolean();
                    updateToggleTexture(btnRef[0], state[0]);
                });
        btnRef[0].setHoverTooltips(Component.literal(tooltip));
        updateToggleTexture(btnRef[0], state[0]);

        ButtonWidget btn = btnRef[0];
        panel.addWidget(btn);

        if (hint != null && !hint.isBlank()) {
            LabelWidget hintLbl = new LabelWidget(8, y + 14, hint);
            hintLbl.setColor(TerminalUI.tokens().textFaint());
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
        TerminalTheme tokens = TerminalUI.tokens();
        lbl.setColor(tokens.textMuted());
        lbl.setHoverTooltips(Component.literal(tooltip));
        panel.addWidget(lbl);

        TextFieldWidget input = new TextFieldWidget(ctlX, y - 2, CTL_W, CTL_H, getter, setter);
        input.setClientSideWidget();
        input.setNumbersOnly(min, max);
        input.setTextColor(tokens.textStrong());
        input.setBackground(new GuiTextureGroup(
                new ColorRectTexture(tokens.panelDisabled()),
                new ColorBorderTexture(1, tokens.border())));
        input.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(tokens.panelDisabled()),
                new ColorBorderTexture(1, tokens.textStrong())));
        input.setWheelDur(1);
        input.setHoverTooltips(Component.literal(tooltip));
        panel.addWidget(input);

        if (hint != null && !hint.isBlank()) {
            LabelWidget hintLbl = new LabelWidget(8, y + 14, hint);
            hintLbl.setColor(tokens.textFaint());
            panel.addWidget(hintLbl);
        }

        return y + ROW_H;
    }

    private void updateToggleTexture(ButtonWidget btn, boolean on) {
        TerminalTheme tokens = TerminalUI.tokens();
        int fill = on ? tokens.successSurface() : tokens.errorSurface();
        int text = on ? tokens.success() : tokens.error();
        String label = TerminalText.strip(Component.translatable(on
                ? "gui.gtceuterminal.manager_settings.common.yes"
                : "gui.gtceuterminal.manager_settings.common.no").getString());

        btn.setButtonTexture(toggleTexture(fill, text, label));
        btn.setHoverTexture(new GuiTextureGroup(
                new BevelTexture(Palette.shade(fill, 1.30f)),
                new TextTexture(label, text).setWidth(CTL_W).setType(TextTexture.TextType.NORMAL)));
    }

    private GuiTextureGroup toggleTexture(int fill, int text, String label) {
        return new GuiTextureGroup(
                new BevelTexture(fill),
                new TextTexture(label, text).setWidth(CTL_W).setType(TextTexture.TextType.NORMAL));
    }

    private void addFrame(WidgetGroup root) {
        TerminalTheme tokens = TerminalUI.tokens();
        root.addWidget(new ImageWidget(0, 0, GUI_W, 2, new ColorRectTexture(tokens.border())));
        root.addWidget(new ImageWidget(0, 0, 2, GUI_H, new ColorRectTexture(tokens.border())));
        root.addWidget(new ImageWidget(GUI_W - 2, 0, 2, GUI_H, new ColorRectTexture(tokens.borderDark())));
        root.addWidget(new ImageWidget(0, GUI_H - 2, GUI_W, 2, new ColorRectTexture(tokens.borderDark())));
    }

    // NBT helpers
    private int getNoHatchMode() {
        return noHatchMode;
    }

    private void toggleNoHatchMode() {
        noHatchMode = noHatchMode == 1 ? 0 : 1;
        syncToServer();
    }

    private int getTierMode() {
        return tierMode;
    }

    private void setTierMode(int tier) {
        tierMode = Math.max(0, Math.min(14, tier));
        syncToServer();
    }

    private int getRepeatCount() {
        return repeatCount;
    }

    private void setRepeatCount(int count) {
        repeatCount = Math.max(0, Math.min(99, count));
        syncToServer();
    }

    private int getIsUseAE() {
        return isUseAE;
    }

    private void toggleIsUseAE() {
        isUseAE = isUseAE == 1 ? 0 : 1;
        syncToServer();
    }

    private void syncToServer() {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putInt("NoHatchMode", noHatchMode);
        snapshot.putInt("TierMode", tierMode);
        snapshot.putInt("RepeatCount", repeatCount);
        snapshot.putInt("IsUseAE", isUseAE);
        com.gtceuterminal.common.network.TerminalNetwork.sendToServer(
                new com.gtceuterminal.common.network.CPacketSaveManagerSettings(snapshot));
    }

}
