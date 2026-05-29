package com.gtceuterminal.client.gui.dialog;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.config.ComponentEntry;
import com.gtceuterminal.common.config.ComponentRegistry;
import com.gtceuterminal.common.multiblock.ComponentInfo;
import com.gtceuterminal.common.upgrade.UniversalUpgradeCatalog;

import com.gregtechceu.gtceu.api.GTValues;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.mojang.text2speech.Narrator.LOGGER;

@OnlyIn(Dist.CLIENT)
public final class UpgradeCandidateGrid {

    private UpgradeCandidateGrid() {}

    public record UpgradeOption(String blockId, String displayName, int tier) {}

    public static List<UpgradeOption> universalCandidates(
            ComponentInfo rep,
            Integer filterTier,
            UniversalUpgradeCatalog catalog
    ) {
        if (catalog == null) return List.of();
        var opt = catalog.get(rep.getPosition());
        if (opt == null || opt.candidates() == null || opt.candidates().isEmpty()) return List.of();

        String currentId = blockId(rep);
        List<UpgradeOption> list = new ArrayList<>();
        for (var c : opt.candidates()) {
            if (c == null || c.blockId() == null) continue;
            if (currentId != null && currentId.equalsIgnoreCase(c.blockId())) continue;
            if (filterTier != null && c.tier() != filterTier) continue;
            list.add(new UpgradeOption(c.blockId(), resolveBlockName(c.blockId(), 48), c.tier()));
        }

        list.sort(Comparator.comparingInt(UpgradeOption::tier)
                .thenComparing(UpgradeOption::blockId, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public static List<UpgradeOption> maintenanceCandidates(ComponentInfo rep) {
        String currentId = blockId(rep);
        List<UpgradeOption> list = new ArrayList<>();
        for (ComponentEntry e : ComponentRegistry.getMaintenanceHatches()) {
            if (e == null || e.blockId == null) continue;
            if (currentId != null && currentId.equalsIgnoreCase(e.blockId)) continue;
            String name = trimSuffixes(resolveBlockName(e.blockId, 48));
            if (name.isBlank()) name = e.displayName != null ? e.displayName : e.blockId;
            list.add(new UpgradeOption(e.blockId, name, e.tier));
        }
        list.sort(Comparator.<UpgradeOption>comparingInt(UpgradeOption::tier)
                .thenComparing(UpgradeOption::displayName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public static List<UpgradeOption> coilCandidates(ComponentInfo rep) {
        String currentId = blockId(rep);
        List<UpgradeOption> list = new ArrayList<>();
        for (var entry : ComponentRegistry.getCoils()) {
            if (entry == null || entry.blockId == null) continue;
            if (currentId != null && currentId.equalsIgnoreCase(entry.blockId)) continue;

            String name = entry.displayName;
            if (name != null && name.startsWith("block.")) {
                name = trimSuffixes(Component.translatable(name).getString());
            }
            if (name == null || name.isBlank()) name = entry.blockId;
            list.add(new UpgradeOption(entry.blockId, name, entry.tier));
        }
        list.sort(Comparator.<UpgradeOption>comparingInt(UpgradeOption::tier)
                .thenComparing(UpgradeOption::displayName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public static boolean buildUniversalCandidates(
            ComponentInfo rep,
            DraggableScrollableWidgetGroup scroll,
            Integer filterTier,
            String selectedUpgradeId,
            UniversalUpgradeCatalog catalog,
            int colorBgLight,
            int colorBorderLight,
            java.util.function.BiConsumer<String, Integer> onSelect
    ) {
        List<UpgradeOption> list = universalCandidates(rep, filterTier, catalog);
        if (list.isEmpty()) return false;

        int btnW = 120, btnH = 26, spacing = 6, perRow = 3, xPos = 0, yPos = 0, added = 0;
        for (UpgradeOption c : list) {
            if (added > 0 && added % perRow == 0) { xPos = 0; yPos += btnH + spacing; }
            String display = truncate(c.displayName(), 16);
            boolean sel = c.blockId().equalsIgnoreCase(selectedUpgradeId);
            scroll.addWidget(makeOptionBtn(
                    "§f" + display + "\n§7(" + tierName(c.tier()) + ")",
                    sel, xPos, yPos, btnW, btnH,
                    colorBgLight, colorBorderLight,
                    () -> onSelect.accept(c.blockId(), c.tier())));
            xPos += btnW + spacing;
            added++;
        }
        return added > 0;
    }

    public static void buildMaintenanceGrid(
            ComponentInfo rep,
            DraggableScrollableWidgetGroup scroll,
            String selectedUpgradeId,
            int colorBgLight,
            int colorBorderLight,
            java.util.function.BiConsumer<String, Integer> onSelect
    ) {
        List<UpgradeOption> entries = maintenanceCandidates(rep);
        int btnW = 118, btnH = 26, spacing = 4, perRow = 3, xPos = 0, yPos = 0, added = 0;
        for (UpgradeOption e : entries) {
            if (added > 0 && added % perRow == 0) { xPos = 0; yPos += btnH + spacing; }
            String name = truncate(e.displayName(), 20);
            String tierTag = tierName(e.tier());
            boolean sel = e.blockId().equals(selectedUpgradeId);
            scroll.addWidget(makeOptionBtn("§f" + name + "\n§7(" + tierTag + ")", sel,
                    xPos, yPos, btnW, btnH, colorBgLight, colorBorderLight,
                    () -> onSelect.accept(e.blockId(), e.tier())));
            xPos += btnW + spacing;
            added++;
        }
    }

    public static void buildCoilGrid(
            ComponentInfo rep,
            DraggableScrollableWidgetGroup scroll,
            String selectedUpgradeId,
            int colorBgLight,
            int colorBorderLight,
            java.util.function.BiConsumer<String, Integer> onSelect
    ) {

        LOGGER.info("buildCoilGrid: {} coils in registry", com.gtceuterminal.common.config.ComponentRegistry.getCoils().size());
        List<UpgradeOption> list = coilCandidates(rep);
        int btnW = 118, btnH = 26, spacing = 4, perRow = 3, xPos = 0, yPos = 0, added = 0;
        for (UpgradeOption e : list) {
            if (added > 0 && added % perRow == 0) { xPos = 0; yPos += btnH + spacing; }
            boolean sel = e.blockId().equals(selectedUpgradeId);
            scroll.addWidget(makeOptionBtn("§f" + truncate(e.displayName(), 20) + "\n§7(" + tierName(e.tier()) + ")",
                    sel, xPos, yPos, btnW, btnH, colorBgLight, colorBorderLight,
                    () -> onSelect.accept(e.blockId(), e.tier())));
            xPos += btnW + spacing;
            added++;
        }
    }

    static ButtonWidget makeOptionBtn(String textLines, boolean selected,
                                      int x, int y, int w, int h,
                                      int colorBgLight, int colorBorderLight,
                                      Runnable onClick) {
        int bg     = selected ? 0x6600FF00 : colorBgLight;
        int border = selected ? 0xFF00FF00 : colorBorderLight;
        TextTexture text = new TextTexture(textLines).setWidth(w).setType(TextTexture.TextType.NORMAL);
        ButtonWidget btn = new ButtonWidget(x, y, w, h,
                new GuiTextureGroup(new ColorRectTexture(bg), new ColorBorderTexture(1, border), text),
                cd -> onClick.run());
        btn.setHoverTexture(new GuiTextureGroup(
                new ColorRectTexture(bg), new ColorBorderTexture(1, 0xFFFFFFFF), text));
        return btn;
    }

    public static String blockId(ComponentInfo rep) {
        try { return rep.getState().getBlock().builtInRegistryHolder().key().location().toString(); }
        catch (IllegalStateException e) {
            GTCEUTerminalMod.LOGGER.debug("UpgradeCandidateGrid: could not get block id: {}", e.getMessage());
            return null;
        }
    }

    static String resolveBlockName(String blockId, int maxLen) {
        String display = blockId;
        try {
            Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(blockId));
            if (b != null) {
                String loc = Component.translatable(b.getDescriptionId()).getString();
                if (loc != null && !loc.isBlank()) display = loc;
            }
        } catch (IllegalArgumentException e) {
            GTCEUTerminalMod.LOGGER.debug("UpgradeCandidateGrid: could not resolve '{}': {}", blockId, e.getMessage());
        }
        return display.length() > maxLen ? display.substring(0, maxLen - 1) + "…" : display;
    }

    public static String tierName(int tier) {
        String[] vn = GTValues.VN;
        if (tier >= 0 && tier < vn.length) { String s = vn[tier]; if (s != null && !s.isBlank()) return s; }
        return "T" + tier;
    }

    static String trimSuffixes(String s) {
        if (s == null) return "";
        return s.replace(" Coil Block", "").replace(" Heating Coil", "")
                .replace(" Maintenance Hatch", " Maintenance").trim();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen - 1) + "…" : s;
    }
}