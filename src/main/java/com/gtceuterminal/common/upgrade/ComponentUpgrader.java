package com.gtceuterminal.common.upgrade;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.ae2.MENetworkItemExtractor;
import com.gtceuterminal.common.config.ItemsConfig;
import com.gtceuterminal.common.material.ComponentUpgradeHelper;
import com.gtceuterminal.common.material.MaterialAvailability;
import com.gtceuterminal.common.material.MaterialCalculator;
import com.gtceuterminal.common.multiblock.ComponentInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;
import java.util.Map;

public class ComponentUpgrader {

    public static UpgradeResult upgradeComponent(
            ComponentInfo component,
            int targetTier,
            String targetUpgradeId,
            Player player,
            Level level,
            boolean consumeMaterials,
            ItemStack linkedManager
    ) {
        boolean hasId = targetUpgradeId != null && !targetUpgradeId.isBlank();

        if (hasId) {
            ResourceLocation rl = ResourceLocation.tryParse(targetUpgradeId);
            if (rl == null) return fail("Invalid upgrade id: " + targetUpgradeId);
            if (BuiltInRegistries.BLOCK.get(rl) == Blocks.AIR) return fail("Unknown upgrade block: " + targetUpgradeId);
        } else {
            if (!player.isCreative() && !ComponentUpgradeHelper.canUpgrade(component, targetTier))
                return fail("Cannot upgrade to tier " + targetTier);
        }

        Map<Item, Integer> required = hasId
                ? ComponentUpgradeHelper.getUpgradeItemsForBlockId(targetUpgradeId)
                : ComponentUpgradeHelper.getUpgradeItems(component, targetTier);
        if (required.isEmpty()) return fail("No upgrade item found for this component");

        String extractionSource = "";
        if (!player.isCreative() && consumeMaterials) {
            ExtractionResult er = extractMaterials(required, player, level, linkedManager);
            if (!er.success) return fail(er.message);
            extractionSource = er.source;
        }

        // Resolve target block
        Block newBlock;
        if (hasId) {
            newBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(targetUpgradeId));
        } else {
            newBlock = Block.byItem(required.keySet().iterator().next());
        }

        if (newBlock == null || newBlock == Blocks.AIR) {
            refund(required, player, consumeMaterials);
            return fail("Invalid upgrade block");
        }

        BlockPos pos = component.getPosition();
        BlockState oldState = component.getState();
        Block oldBlock = oldState.getBlock();
        ItemStack oldStack = itemStackFor(oldBlock, player);
        BlockState newState = copyFacing(oldState, newBlock.defaultBlockState());

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        if (!level.setBlock(pos, newState, 3)) {
            refund(required, player, consumeMaterials);
            return fail("Failed to place upgraded block");
        }

        if (!level.isClientSide && level instanceof ServerLevel sl) postPlaceInitialize(sl, pos, player);
        level.sendBlockUpdated(pos, oldState, newState, 3);
        level.blockUpdated(pos, newBlock);
        updateNeighborBlocks(level, pos, newBlock);
        level.sendBlockUpdated(pos, oldState, newState, 11);
        level.getChunkAt(pos).setUnsaved(true);

        if (oldStack != null) {
            if (!player.getInventory().add(oldStack)) player.drop(oldStack, false);
        }

        String targetLabel = hasId ? targetUpgradeId : com.gregtechceu.gtceu.api.GTValues.VN[targetTier];

        return new UpgradeResult(true, "Successfully upgraded to " + targetLabel + extractionSource);
    }

    public static UpgradeResult upgradeComponent(
            ComponentInfo component, int targetTier, Player player, Level level,
            boolean consumeMaterials, ItemStack linkedManager) {
        return upgradeComponent(component, targetTier, null, player, level, consumeMaterials, linkedManager);
    }

    public static UpgradeResult upgradeComponent(
            ComponentInfo component, int targetTier, Player player, Level level, boolean consumeMaterials) {
        return upgradeComponent(component, targetTier, player, level, consumeMaterials, ItemStack.EMPTY);
    }

    private static ExtractionResult extractMaterials(
            Map<Item, Integer> required, Player player, Level level, ItemStack linkedManager) {

        if (linkedManager != null && !linkedManager.isEmpty() && ItemsConfig.isMgrAllowMENetworkUpgrade()) {
            MENetworkItemExtractor.ExtractResult me =
                    MENetworkItemExtractor.tryExtractFromMEOrInventory(linkedManager, level, player, required);
            if (me.success()) {
                String src = me.source() == MENetworkItemExtractor.ExtractionSource.ME_NETWORK
                        ? " §a(ME Network)" : " §7(Inventory)";
                return new ExtractionResult(true, src, null);
            }
        }

        List<MaterialAvailability> materials = MaterialCalculator.checkMaterialsAvailability(required, player, level);
        if (!MaterialCalculator.hasEnoughMaterials(materials))
            return new ExtractionResult(false, "", "Missing materials: " + formatMissing(MaterialCalculator.getMissingMaterials(materials)));
        if (!MaterialCalculator.extractMaterials(materials, player, level, true, true))
            return new ExtractionResult(false, "", "Failed to extract materials");

        return new ExtractionResult(true, " §7(Inventory)", null);
    }

    private static UpgradeResult fail(String msg) {
        return new UpgradeResult(false, msg);
    }

    private static void refund(Map<Item, Integer> required, Player player, boolean consumeMaterials) {
        if (!consumeMaterials) return;
        for (var entry : required.entrySet()) {
            ItemStack refund = new ItemStack(entry.getKey(), entry.getValue());
            if (!player.getInventory().add(refund)) player.drop(refund, false);
        }
    }

    private static ItemStack itemStackFor(Block block, Player player) {
        if (player.isCreative()) return null;
        Item item = block.asItem();
        if (item == net.minecraft.world.item.Items.AIR) return null;
        return new ItemStack(item, 1);
    }

    private static BlockState copyFacing(BlockState oldState, BlockState newState) {
        try {
            if (oldState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    && newState.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                newState = newState.setValue(BlockStateProperties.HORIZONTAL_FACING,
                        oldState.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if (oldState.hasProperty(BlockStateProperties.FACING)
                    && newState.hasProperty(BlockStateProperties.FACING))
                newState = newState.setValue(BlockStateProperties.FACING,
                        oldState.getValue(BlockStateProperties.FACING));
        } catch (Exception e) {
            GTCEUTerminalMod.LOGGER.warn("Could not copy block state facing: {}", e.getMessage());
        }
        return newState;
    }

    private static void postPlaceInitialize(ServerLevel level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        state.getBlock().setPlacedBy(level, pos, state, player, ItemStack.EMPTY);
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) be.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        level.updateNeighborsAt(pos, state.getBlock());
        state.getBlock().onPlace(state, level, pos, level.getBlockState(pos), false);
    }

    private static void updateNeighborBlocks(Level level, BlockPos pos, Block newBlock) {
        BlockPos[] neighbors = {pos, pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()};
        for (BlockPos n : neighbors) {
            BlockState ns = level.getBlockState(n);
            level.neighborChanged(n, newBlock, pos);
            level.sendBlockUpdated(n, ns, ns, 3);
        }
        if (!level.isClientSide) {
            level.getChunkAt(pos).setUnsaved(true);
            level.getChunkSource().getLightEngine().checkBlock(pos);
        }
    }

    private static String formatMissing(Map<Item, Integer> missing) {
        StringBuilder sb = new StringBuilder();
        for (var e : missing.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey().getDescription().getString()).append(" x").append(e.getValue());
        }
        return sb.toString();
    }

    private record ExtractionResult(boolean success, String source, String message) {
    }

    public record UpgradeResult(boolean success, String message) {
    }
}
