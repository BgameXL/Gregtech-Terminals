package com.gtceuterminal.common.upgrade;

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Catalog of possible upgrades for parts in a multiblock machine, keyed by their position.
public class UniversalUpgradeCatalog {

    private final Map<BlockPos, UpgradeOptions> optionsByPos = new HashMap<>();

    public void put(BlockPos pos, UpgradeOptions options) {
        optionsByPos.put(pos, options);
    }

    public UpgradeOptions get(BlockPos pos) {
        return optionsByPos.get(pos);
    }

    public boolean isEmpty() {
        return optionsByPos.isEmpty();
    }

    public record InstalledPart(
            BlockPos pos,
            String blockId,
            int tier,
            Set<PartAbility> abilities
    ) {
    }

    public record CandidatePart(
            String blockId,
            int tier,
            PartAbility ability
    ) {
    }

    public record UpgradeOptions(
            InstalledPart installed,
            List<CandidatePart> candidates
    ) {
    }
}