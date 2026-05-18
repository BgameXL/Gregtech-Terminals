package com.gtceuterminal.common.ae2;

import appeng.api.features.IGridLinkableHandler;

import com.gtceuterminal.common.item.MultiStructureManagerItem;
import com.gtceuterminal.common.item.SchematicInterfaceItem;

import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

public class TerminalGridLinkableHandler implements IGridLinkableHandler {

    private static final String TAG_ACCESS_POINT_POS = "accessPoint";

    @Override
    public boolean canLink(ItemStack stack) {
        return stack.getItem() instanceof MultiStructureManagerItem
                || stack.getItem() instanceof SchematicInterfaceItem;
    }

    @Override
    public void link(ItemStack itemStack, GlobalPos pos) {
        GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos)
                .result()
                .ifPresent(tag -> itemStack.getOrCreateTag().put(TAG_ACCESS_POINT_POS, tag));
    }

    @Override
    public void unlink(ItemStack itemStack) {
        if (itemStack.hasTag()) {
            itemStack.getTag().remove(TAG_ACCESS_POINT_POS);
        }
    }
}