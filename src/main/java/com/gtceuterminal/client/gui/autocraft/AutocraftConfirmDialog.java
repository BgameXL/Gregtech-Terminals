package com.gtceuterminal.client.gui.autocraft;

import com.gtceuterminal.common.autocraft.AnalysisResult;
import com.gtceuterminal.common.network.CPacketConfirmAutobuild;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.gtceuterminal.common.theme.ItemTheme;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class AutocraftConfirmDialog extends DialogWidget {

    private static final int W       = 480;
    private static final int H       = 380;
    private static final int PAD     = 10;
    private static final int HEADER_H = 28;
    private static final int FOOTER_H = 36;

    private static final int C_GREEN  = 0xFF2D6B2D;
    private static final int C_GREEN_T= 0x441a3a1a;
    private static final int C_ORANGE = 0xFF8a5a10;
    private static final int C_ORANGE_T=0x443a2a0a;
    private static final int C_RED    = 0xFF8a2020;
    private static final int C_RED_T  = 0x443a1010;
    private static final int C_WHITE  = 0xFFFFFFFF;
    private static final int C_GRAY   = 0xFFAAAAAA;
    private static final int C_DARK   = 0xFF0A0A0A;

    private final AnalysisResult result;
    private final ItemTheme      theme;
    private final int colorBg, colorPanel, colorBorder;

    private DraggableScrollableWidgetGroup ingredientScroll;
    private int ingredientRowW;
    private final java.util.Set<Integer> expanded = new java.util.HashSet<>();
    private final java.util.Map<Integer, com.gtceuterminal.common.compat.RecipeReader.RecipeView> recipeCache = new java.util.HashMap<>();

    public AutocraftConfirmDialog(WidgetGroup parent, AnalysisResult result, Player player) {
        super(parent, true);
        this.result = result;
        this.theme  = ItemTheme.loadFromPlayer(player);

        colorBg     = theme.bgColor;
        colorPanel  = theme.panelColor;
        colorBorder = theme.isNativeStyle() ? 0xFF555555 : theme.accent(0xFF);

        initDialog();
    }

    @Override
    public void close() {
        super.close();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.closeContainer();
        } else {
            mc.setScreen(null);
        }
    }

    private void initDialog() {
        var mc  = Minecraft.getInstance();
        int sw  = mc.screen != null ? mc.screen.width  : mc.getWindow().getGuiScaledWidth();
        int sh  = mc.screen != null ? mc.screen.height : mc.getWindow().getGuiScaledHeight();

        Position abs = parent.getPosition();
        setSelfPosition(new Position(
                (sw - W) / 2 - abs.x,
                Mth.clamp((sh - H) / 2, 10, sh - H - 10) - abs.y));
        setSize(new Size(W, H));
        setBackground(theme.backgroundTexture());

        addWidget(buildHeader());
        addWidget(buildBody());
        addWidget(buildFooter());
    }

    private WidgetGroup buildHeader() {
        WidgetGroup header = new WidgetGroup(2, 2, W - 4, HEADER_H);
        header.setBackground(theme.headerTexture());

        String title = result.kind == AnalysisResult.Kind.UPGRADE
                ? Component.translatable("gui.gtceuterminal.autocraft.title.upgrade").getString()
                : Component.translatable("gui.gtceuterminal.autocraft.title.build").getString();

        LabelWidget lbl = new LabelWidget(10, 9, "§f" + title);
        lbl.setTextColor(C_WHITE);
        header.addWidget(lbl);

        ButtonWidget close = TerminalButton.close(W - 28, 4, 20, colorPanel, colorBorder, cd -> close());
        close.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        header.addWidget(close);
        return header;
    }

    private WidgetGroup buildBody() {
        int bodyY = 2 + HEADER_H + 3;
        int bodyH = H - bodyY - FOOTER_H - 6;
        WidgetGroup body = new WidgetGroup(PAD, bodyY, W - PAD * 2, bodyH);
        body.setBackground(new ColorRectTexture(colorBg));

        int colL = (W - PAD * 2) * 6 / 10;
        int colR = (W - PAD * 2) - colL - 1;

        WidgetGroup left = new WidgetGroup(0, 0, colL, bodyH);
        left.setBackground(new GuiTextureGroup(
                new ColorRectTexture(colorPanel),
                new ColorBorderTexture(1, colorBorder)));
        left.addWidget(buildTargetHatch(colL));
        left.addWidget(buildIngredientList(colL, bodyH));
        body.addWidget(left);

        body.addWidget(new ImageWidget(colL, 0, 1, bodyH, new ColorRectTexture(colorBorder)));

        WidgetGroup right = new WidgetGroup(colL + 1, 0, colR, bodyH);
        right.setBackground(new GuiTextureGroup(
                new ColorRectTexture(colorPanel),
                new ColorBorderTexture(1, colorBorder)));
        right.addWidget(buildAvailabilityList(colR, bodyH));
        body.addWidget(right);

        return body;
    }

    private WidgetGroup buildTargetHatch(int panelW) {
        int hatchH = 48;
        WidgetGroup group = new WidgetGroup(0, 0, panelW, hatchH);
        group.setBackground(new ColorRectTexture(colorBg));

        ItemStack target = result.targetItemStack;
        if (!target.isEmpty()) {
            ItemStackTransfer transfer = new ItemStackTransfer(target.copy());
            SlotWidget slot = new SlotWidget(transfer, 0, 8, 8, false, false);
            slot.setBackgroundTexture(new ColorBorderTexture(1, colorBorder));
            group.addWidget(slot);
        }

        String name = target.isEmpty()
                ? Component.translatable("gui.gtceuterminal.autocraft.unknown_component").getString()
                : target.getHoverName().getString();
        LabelWidget nameLbl = new LabelWidget(34, 10, "§f" + name);
        nameLbl.setTextColor(C_WHITE);
        group.addWidget(nameLbl);

        if (result.componentCount > 1) {
            String countStr = Component.translatable("gui.gtceuterminal.autocraft.replace_count",
                    result.componentCount).getString();
            LabelWidget countLbl = new LabelWidget(34, 22, "§7" + countStr);
            countLbl.setTextColor(C_GRAY);
            group.addWidget(countLbl);
        }

        group.addWidget(new ImageWidget(0, hatchH - 1, panelW, 1, new ColorRectTexture(colorBorder)));
        return group;
    }

    private WidgetGroup buildIngredientList(int panelW, int panelH) {
        int offsetY = 48;
        int listH   = panelH - offsetY;

        LabelWidget header = new LabelWidget(8, offsetY + 4, "§7" +
                Component.translatable("gui.gtceuterminal.autocraft.ingredients_total").getString());
        header.setTextColor(C_GRAY);

        ingredientRowW   = panelW - 10;
        ingredientScroll = new DraggableScrollableWidgetGroup(
                0, offsetY + 16, panelW, listH - 16);
        ingredientScroll.setYScrollBarWidth(6);
        ingredientScroll.setYBarStyle(new ColorRectTexture(C_DARK), new ColorRectTexture(colorBorder));
        populateIngredients();

        WidgetGroup wrapper = new WidgetGroup(0, 0, panelW, panelH);
        wrapper.addWidget(header);
        wrapper.addWidget(ingredientScroll);
        return wrapper;
    }

    private void populateIngredients() {
        ingredientScroll.clearAllWidgets();
        int y = 0;
        for (int i = 0; i < result.entries.size(); i++) {
            AnalysisResult.Entry entry = result.entries.get(i);
            ingredientScroll.addWidget(buildIngredientRow(i, entry, ingredientRowW, y));
            y += 22;

            if (expanded.contains(i)) {
                com.gtceuterminal.common.compat.RecipeReader.RecipeView recipe = recipeFor(i, entry.stack);
                if (recipe.isEmpty()) {
                    LabelWidget none = new LabelWidget(28, y + 3, "§8" +
                            Component.translatable("gui.gtceuterminal.autocraft.no_recipe").getString());
                    ingredientScroll.addWidget(none);
                    y += 16;
                } else {
                    for (ItemStack ing : recipe.items) {
                        ingredientScroll.addWidget(buildRecipeSubRow(ing, ingredientRowW, y));
                        y += 18;
                    }
                    for (var fluid : recipe.fluids) {
                        ingredientScroll.addWidget(buildFluidSubRow(fluid, ingredientRowW, y));
                        y += 18;
                    }
                }
            }
        }
        ingredientScroll.computeMax();
    }

    private WidgetGroup buildIngredientRow(int index, AnalysisResult.Entry entry, int rowW, int y) {
        WidgetGroup row = new WidgetGroup(4, y, rowW, 20);

        boolean isOpen = expanded.contains(index);
        ButtonWidget toggle = TerminalButton.ghost(0, 2, 16, 16,
                isOpen ? "§e[-]" : "§7[+]", cd -> {
                    if (!expanded.add(index)) expanded.remove(index);
                    populateIngredients();
                });
        toggle.setHoverTooltips(Component.translatable("gui.gtceuterminal.autocraft.show_recipe").getString());
        row.addWidget(toggle);

        ItemStackTransfer transfer = new ItemStackTransfer(entry.stack.copy());
        SlotWidget slot = new SlotWidget(transfer, 0, 16, 1, false, false);
        slot.setBackgroundTexture(new ColorBorderTexture(1, 0xFF333333));
        row.addWidget(slot);

        String name = entry.stack.getHoverName().getString();
        LabelWidget nameLbl = new LabelWidget(38, 5, "§f" + name);
        nameLbl.setTextColor(C_WHITE);
        row.addWidget(nameLbl);

        String qty = "× " + entry.needed();
        LabelWidget qtyLbl = new LabelWidget(rowW - 36, 5, "§7" + qty);
        qtyLbl.setTextColor(C_GRAY);
        row.addWidget(qtyLbl);

        return row;
    }

    private WidgetGroup buildRecipeSubRow(ItemStack ing, int rowW, int y) {
        WidgetGroup row = new WidgetGroup(24, y, rowW - 24, 16);

        ItemStackTransfer transfer = new ItemStackTransfer(ing.copy());
        SlotWidget slot = new SlotWidget(transfer, 0, 0, 0, false, false);
        slot.setBackgroundTexture(new ColorBorderTexture(1, 0xFF2A2A2A));
        row.addWidget(slot);

        LabelWidget nameLbl = new LabelWidget(20, 4, "§7" + ing.getHoverName().getString());
        nameLbl.setTextColor(C_GRAY);
        row.addWidget(nameLbl);

        LabelWidget qtyLbl = new LabelWidget(rowW - 24 - 30, 4, "§8× " + ing.getCount());
        row.addWidget(qtyLbl);

        return row;
    }

    private WidgetGroup buildFluidSubRow(com.gtceuterminal.common.compat.RecipeReader.FluidLine fluid, int rowW, int y) {
        WidgetGroup row = new WidgetGroup(24, y, rowW - 24, 16);

        ImageWidget marker = new ImageWidget(2, 3, 10, 10, new GuiTextureGroup(
                new ColorRectTexture(0xFF1a4a8a), new ColorBorderTexture(1, 0xFF2a5acc)));
        row.addWidget(marker);

        LabelWidget nameLbl = new LabelWidget(20, 4, "§b" + fluid.name());
        nameLbl.setTextColor(0xFF66CCFF);
        row.addWidget(nameLbl);

        LabelWidget qtyLbl = new LabelWidget(rowW - 24 - 52, 4, "§8× " + fluid.amount() + " mB");
        row.addWidget(qtyLbl);

        return row;
    }

    private com.gtceuterminal.common.compat.RecipeReader.RecipeView recipeFor(int index, ItemStack out) {
        return recipeCache.computeIfAbsent(index, i -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return com.gtceuterminal.common.compat.RecipeReader.RecipeView.EMPTY;
            return com.gtceuterminal.common.compat.RecipeReader.findIngredients(
                    mc.level.getRecipeManager(), mc.level.registryAccess(), out);
        });
    }

    private WidgetGroup buildAvailabilityList(int panelW, int panelH) {
        int headerH = 18;
        DraggableScrollableWidgetGroup scroll = new DraggableScrollableWidgetGroup(
                0, headerH, panelW, panelH - headerH);
        scroll.setYScrollBarWidth(6);
        scroll.setYBarStyle(new ColorRectTexture(C_DARK), new ColorRectTexture(colorBorder));

        int y = 0;
        for (AnalysisResult.Entry entry : result.entries) {
            scroll.addWidget(buildAvailRow(entry, panelW, y));
            y += 22;
        }

        WidgetGroup wrapper = new WidgetGroup(0, 0, panelW, panelH);

        LabelWidget hdr = new LabelWidget(6, 4, "§7" +
                Component.translatable("gui.gtceuterminal.autocraft.available").getString());
        hdr.setTextColor(C_GRAY);
        wrapper.addWidget(hdr);
        wrapper.addWidget(scroll);
        return wrapper;
    }

    private WidgetGroup buildAvailRow(AnalysisResult.Entry entry, int rowW, int y) {
        WidgetGroup row = new WidgetGroup(2, y, rowW - 4, 20);

        int bgColor, borderColor;
        String label;

        long have = entry.totalAvailable();
        if (entry.hasAll()) {
            bgColor     = C_GREEN_T;
            borderColor = C_GREEN;
            label       = "§a" + have + " / " + entry.needed();
        } else if (entry.craftable) {
            bgColor     = C_ORANGE_T;
            borderColor = C_ORANGE;
            label       = "§6" + have + " / " + entry.needed();
        } else {
            bgColor     = C_RED_T;
            borderColor = C_RED;
            label       = "§c" + have + " / " + entry.needed();
        }

        row.setBackground(new GuiTextureGroup(
                new ColorRectTexture(bgColor),
                new ColorBorderTexture(1, borderColor)));

        LabelWidget qty = new LabelWidget(5, 5, label);
        row.addWidget(qty);

        String tag = entry.hasAll() ? "have" : (entry.craftable ? "craft" : "missing");
        String tagColor = entry.hasAll() ? "§a" : (entry.craftable ? "§6" : "§c");
        LabelWidget tagLbl = new LabelWidget(rowW - 60, 5, tagColor + tag);
        row.addWidget(tagLbl);

        return row;
    }

    private WidgetGroup buildFooter() {
        int footerY = H - FOOTER_H - 2;
        int innerW  = W - PAD * 2;
        WidgetGroup footer = new WidgetGroup(PAD, footerY, innerW, FOOTER_H);

        int btnH = 24;

        boolean hasMissing = result.anyMissing();
        int requestW = 160;
        ButtonWidget request = hasMissing
                ? TerminalButton.disabled(0, 4, requestW, btnH,
                Component.translatable("gui.gtceuterminal.autocraft.request").getString())
                : TerminalButton.action(0, 4, requestW, btnH,
                Component.translatable("gui.gtceuterminal.autocraft.request").getString(),
                0xFF1a3a8a, 0xFF2a5acc, cd -> onRequest());
        footer.addWidget(request);

        int cancelW = 110;
        ButtonWidget cancel = TerminalButton.action(innerW - cancelW, 4, cancelW, btnH,
                Component.translatable("gui.gtceuterminal.autocraft.cancel").getString(),
                0xFF6b3fa0, 0xFF8a55c0, cd -> close());
        footer.addWidget(cancel);

        if (hasMissing) {
            String missing = Component.translatable("gui.gtceuterminal.autocraft.missing_items",
                    result.missingCount()).getString();
            LabelWidget warn = new LabelWidget(requestW + 8, 10, "§c" + missing);
            warn.setTextColor(0xFFFF4444);
            footer.addWidget(warn);
        }

        return footer;
    }

    private void onRequest() {
        TerminalNetwork.sendToServer(new CPacketConfirmAutobuild(result));
        close();
    }
}