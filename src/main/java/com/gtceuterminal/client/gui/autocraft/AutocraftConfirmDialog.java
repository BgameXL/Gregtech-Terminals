package com.gtceuterminal.client.gui.autocraft;

import com.gtceuterminal.common.autocraft.AnalysisResult;
import com.gtceuterminal.common.network.CPacketConfirmAutobuild;
import com.gtceuterminal.common.network.TerminalNetwork;
import com.gtceuterminal.common.theme.ItemTheme;
import com.gtceuterminal.client.gui.style.BevelTexture;
import com.gtceuterminal.client.gui.style.TerminalTheme;
import com.gtceuterminal.client.gui.style.TerminalUI;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.gtceuterminal.client.gui.widget.TerminalButton;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class AutocraftConfirmDialog extends DialogWidget {

    private static final int DESIGN_W = 480;
    private static final int DESIGN_H = 380;
    private static final int HEADER_H = 30;
    private static final int FOOTER_H = 38;
    private static final int CARD_INSET = 2;

    private final AnalysisResult result;
    private final ItemTheme theme;
    private final TerminalTheme tokens;

    private int w = DESIGN_W;
    private int h = DESIGN_H;

    private DraggableScrollableWidgetGroup ingredientScroll;
    private int ingredientRowW;
    private final java.util.Set<Integer> expanded = new java.util.HashSet<>();
    private final java.util.Map<Integer, com.gtceuterminal.common.compat.RecipeReader.RecipeView> recipeCache = new java.util.HashMap<>();

    public AutocraftConfirmDialog(WidgetGroup parent, AnalysisResult result, Player player) {
        super(parent, true);
        this.result = result;
        this.theme = ItemTheme.loadFromPlayer(player);
        TerminalUI.use(theme);
        this.tokens = TerminalUI.tokens();

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
        var mc = Minecraft.getInstance();
        int sw = mc.screen != null ? mc.screen.width : mc.getWindow().getGuiScaledWidth();
        int sh = mc.screen != null ? mc.screen.height : mc.getWindow().getGuiScaledHeight();

        w = Math.min(DESIGN_W, sw - 20);
        h = Math.min(DESIGN_H, sh - 20);

        Position abs = parent.getPosition();
        setSelfPosition(new Position(
                Math.max(0, (sw - w) / 2) - abs.x,
                Math.max(0, (sh - h) / 2) - abs.y));
        setSize(new Size(w, h));
        setBackground(theme.backgroundTexture());

        addWidget(buildHeader());
        addWidget(buildBody());
    }

    private WidgetGroup buildHeader() {
        WidgetGroup header = new WidgetGroup(2, 2, w - 4, HEADER_H);
        header.setBackground(new BevelTexture(tokens.panel()));

        String title = result.kind == AnalysisResult.Kind.UPGRADE
                ? Component.translatable("gui.gtceuterminal.autocraft.title.upgrade").getString()
                : Component.translatable("gui.gtceuterminal.autocraft.title.build").getString();

        LabelWidget lbl = new LabelWidget(10, 10, title);
        lbl.setColor(tokens.textStrong());
        header.addWidget(lbl);

        int closeSize = 20;
        ButtonWidget close = TerminalButton.icon3d(
                header.getSizeWidth() - closeSize - 6, (HEADER_H - closeSize) / 2,
                closeSize, "×", cd -> close());
        close.setHoverTooltips(Component.translatable("gui.gtceuterminal.close").getString());
        header.addWidget(close);
        return header;
    }

    private WidgetGroup buildBody() {
        int bodyY = HEADER_H + 1;
        int bodyH = h - bodyY - 2;
        WidgetGroup body = new WidgetGroup(2, bodyY, w - 4, bodyH);
        body.setBackground(TerminalUI.cardBg());

        int innerW = body.getSizeWidth() - CARD_INSET * 2;
        int contentH = bodyH - FOOTER_H - CARD_INSET;
        int colL = innerW * 6 / 10;
        int colR = innerW - colL - 1;

        WidgetGroup left = new WidgetGroup(CARD_INSET, CARD_INSET, colL, contentH);
        left.setBackground(new ColorRectTexture(tokens.surface()));
        left.addWidget(buildTargetHatch(colL));
        left.addWidget(buildIngredientList(colL, contentH));
        body.addWidget(left);

        body.addWidget(new ImageWidget(CARD_INSET + colL, CARD_INSET, 1, contentH,
                new ColorRectTexture(tokens.borderDark())));

        WidgetGroup right = new WidgetGroup(CARD_INSET + colL + 1, CARD_INSET, colR, contentH);
        right.setBackground(new ColorRectTexture(tokens.surface()));
        right.addWidget(buildAvailabilityList(colR, contentH));
        body.addWidget(right);

        body.addWidget(buildFooter(CARD_INSET, contentH + CARD_INSET, innerW));

        return body;
    }

    private WidgetGroup buildTargetHatch(int panelW) {
        int hatchH = 48;
        WidgetGroup group = new WidgetGroup(0, 0, panelW, hatchH);
        group.setBackground(new ColorRectTexture(tokens.panel()));

        ItemStack target = result.targetItemStack;
        if (!target.isEmpty()) {
            ItemStackTransfer transfer = new ItemStackTransfer(target.copy());
            SlotWidget slot = new SlotWidget(transfer, 0, 8, 10, false, false);
            slot.setBackgroundTexture(new ColorBorderTexture(1, tokens.border()));
            group.addWidget(slot);
        }

        String name = target.isEmpty()
                ? Component.translatable("gui.gtceuterminal.autocraft.unknown_component").getString()
                : target.getHoverName().getString();
        LabelWidget nameLbl = new LabelWidget(34, 12, fitText(name, panelW - 42));
        nameLbl.setColor(tokens.textStrong());
        group.addWidget(nameLbl);

        if (result.componentCount > 1) {
            String countStr = Component.translatable("gui.gtceuterminal.autocraft.replace_count",
                    result.componentCount).getString();
            LabelWidget countLbl = new LabelWidget(34, 24, fitText(countStr, panelW - 42));
            countLbl.setColor(tokens.textMuted());
            group.addWidget(countLbl);
        }

        group.addWidget(new ImageWidget(0, hatchH - 1, panelW, 1,
                new ColorRectTexture(tokens.borderDark())));
        return group;
    }

    private WidgetGroup buildIngredientList(int panelW, int panelH) {
        int offsetY = 48;
        int listH = panelH - offsetY;

        LabelWidget header = new LabelWidget(8, offsetY + 4,
                Component.translatable("gui.gtceuterminal.autocraft.ingredients_total").getString());
        header.setColor(tokens.textMuted());

        ingredientRowW = panelW - 10;
        ingredientScroll = new DraggableScrollableWidgetGroup(
                0, offsetY + 16, panelW, listH - 16);
        ingredientScroll.setYScrollBarWidth(0);
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
                    LabelWidget none = new LabelWidget(28, y + 3,
                            Component.translatable("gui.gtceuterminal.autocraft.no_recipe").getString());
                    none.setColor(tokens.textFaint());
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
        ButtonWidget toggle = TerminalButton.icon3d(0, 2, 16,
                isOpen ? "-" : "+", cd -> {
                    if (!expanded.add(index)) expanded.remove(index);
                    populateIngredients();
                });
        toggle.setHoverTooltips(Component.translatable("gui.gtceuterminal.autocraft.show_recipe").getString());
        row.addWidget(toggle);

        ItemStackTransfer transfer = new ItemStackTransfer(entry.stack.copy());
        SlotWidget slot = new SlotWidget(transfer, 0, 16, 1, false, false);
        slot.setBackgroundTexture(new ColorBorderTexture(1, tokens.borderSubtle()));
        row.addWidget(slot);

        String name = entry.stack.getHoverName().getString();
        int quantityWidth = 40;
        LabelWidget nameLbl = new LabelWidget(38, 5,
                fitText(name, Math.max(0, rowW - 38 - quantityWidth - 4)));
        nameLbl.setColor(tokens.textStrong());
        row.addWidget(nameLbl);

        String qty = "× " + entry.needed();
        LabelWidget qtyLbl = new LabelWidget(rowW - quantityWidth, 5, qty);
        qtyLbl.setColor(tokens.textMuted());
        row.addWidget(qtyLbl);

        return row;
    }

    private WidgetGroup buildRecipeSubRow(ItemStack ing, int rowW, int y) {
        WidgetGroup row = new WidgetGroup(24, y, rowW - 24, 16);

        ItemStackTransfer transfer = new ItemStackTransfer(ing.copy());
        SlotWidget slot = new SlotWidget(transfer, 0, 0, 0, false, false);
        slot.setBackgroundTexture(new ColorBorderTexture(1, tokens.borderSubtle()));
        row.addWidget(slot);

        LabelWidget nameLbl = new LabelWidget(20, 4,
                fitText(ing.getHoverName().getString(), Math.max(0, rowW - 78)));
        nameLbl.setColor(tokens.textMuted());
        row.addWidget(nameLbl);

        LabelWidget qtyLbl = new LabelWidget(rowW - 54, 4, "× " + ing.getCount());
        qtyLbl.setColor(tokens.textFaint());
        row.addWidget(qtyLbl);

        return row;
    }

    private WidgetGroup buildFluidSubRow(com.gtceuterminal.common.compat.RecipeReader.FluidLine fluid, int rowW, int y) {
        WidgetGroup row = new WidgetGroup(24, y, rowW - 24, 16);

        ImageWidget marker = new ImageWidget(2, 3, 10, 10, new GuiTextureGroup(
                new ColorRectTexture(tokens.accentSoft()), new ColorBorderTexture(1, tokens.border())));
        row.addWidget(marker);

        LabelWidget nameLbl = new LabelWidget(20, 4,
                fitText(fluid.name(), Math.max(0, rowW - 100)));
        nameLbl.setColor(tokens.border());
        row.addWidget(nameLbl);

        LabelWidget qtyLbl = new LabelWidget(rowW - 76, 4, "× " + fluid.amount() + " mB");
        qtyLbl.setColor(tokens.textFaint());
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
        scroll.setYScrollBarWidth(0);

        int y = 1;
        for (AnalysisResult.Entry entry : result.entries) {
            scroll.addWidget(buildAvailRow(entry, panelW, y));
            y += 22;
        }

        WidgetGroup wrapper = new WidgetGroup(0, 0, panelW, panelH);

        LabelWidget hdr = new LabelWidget(6, 4,
                Component.translatable("gui.gtceuterminal.autocraft.available").getString());
        hdr.setColor(tokens.textMuted());
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
            bgColor = tokens.successSurface();
            borderColor = tokens.success();
            label = have + " / " + entry.needed();
        } else if (entry.craftable) {
            bgColor = tokens.warningSurface();
            borderColor = tokens.warning();
            label = have + " / " + entry.needed();
        } else {
            bgColor = tokens.errorSurface();
            borderColor = tokens.error();
            label = have + " / " + entry.needed();
        }

        row.setBackground(new GuiTextureGroup(
                new ColorRectTexture(bgColor),
                new ColorBorderTexture(1, borderColor)));

        LabelWidget qty = new LabelWidget(5, 5, label);
        qty.setColor(borderColor);
        row.addWidget(qty);

        String tag = entry.hasAll() ? "have" : (entry.craftable ? "craft" : "missing");
        LabelWidget tagLbl = new LabelWidget(rowW - 60, 5, tag);
        tagLbl.setColor(borderColor);
        row.addWidget(tagLbl);

        return row;
    }

    private WidgetGroup buildFooter(int x, int y, int innerW) {
        WidgetGroup footer = new WidgetGroup(x, y, innerW, FOOTER_H);
        footer.addWidget(new ImageWidget(0, 0, innerW, 1,
                new ColorRectTexture(tokens.borderDark())));

        int btnH = 24;
        int btnY = (FOOTER_H - btnH) / 2;

        boolean hasMissing = result.anyMissing();
        int requestW = 160;
        ButtonWidget request = hasMissing
                ? TerminalButton.disabled(0, btnY, requestW, btnH,
                Component.translatable("gui.gtceuterminal.autocraft.request").getString())
                : TerminalButton.text3d(0, btnY, requestW, btnH,
                Component.translatable("gui.gtceuterminal.autocraft.request").getString(),
                tokens.action(), cd -> onRequest());
        footer.addWidget(request);

        int cancelW = 110;
        ButtonWidget cancel = TerminalButton.text3d(innerW - cancelW, btnY, cancelW, btnH,
                Component.translatable("gui.gtceuterminal.autocraft.cancel").getString(),
                tokens.errorSurface(), cd -> close());
        footer.addWidget(cancel);

        if (hasMissing) {
            String missing = Component.translatable("gui.gtceuterminal.autocraft.missing_items",
                    result.missingCount()).getString();
            LabelWidget warn = new LabelWidget(requestW + 8, btnY + 6,
                    fitText(stripFormatting(missing), innerW - requestW - cancelW - 24));
            warn.setColor(tokens.error());
            footer.addWidget(warn);
        }

        return footer;
    }

    private void onRequest() {
        TerminalNetwork.sendToServer(new CPacketConfirmAutobuild(result));
        close();
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
}
