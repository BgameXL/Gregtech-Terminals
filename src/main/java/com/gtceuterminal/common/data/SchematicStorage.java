package com.gtceuterminal.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

public final class SchematicStorage {

    public static final String CLIPBOARD = "Clipboard";
    public static final String SAVED = "SavedSchematics";

    private static final String WRAPPED_LIST = "L";
    public static final int MAX_ITEM_NBT_BYTES = 1_900_000;

    private SchematicStorage() {
    }

    public static void putClipboard(CompoundTag itemTag, CompoundTag clipboard) {
        itemTag.putByteArray(CLIPBOARD, compress(clipboard));
    }

    @Nullable
    public static CompoundTag getClipboard(@Nullable CompoundTag itemTag) {
        if (itemTag == null) {
            return null;
        }
        if (itemTag.contains(CLIPBOARD, Tag.TAG_BYTE_ARRAY)) {
            return decompress(itemTag.getByteArray(CLIPBOARD));
        }
        if (itemTag.contains(CLIPBOARD, Tag.TAG_COMPOUND)) {
            return itemTag.getCompound(CLIPBOARD);
        }
        return null;
    }

    public static boolean hasClipboard(@Nullable CompoundTag itemTag) {
        return itemTag != null
                && (itemTag.contains(CLIPBOARD, Tag.TAG_BYTE_ARRAY) || itemTag.contains(CLIPBOARD, Tag.TAG_COMPOUND));
    }

    public static int clipboardHash(@Nullable CompoundTag itemTag) {
        if (itemTag == null) {
            return 0;
        }
        if (itemTag.contains(CLIPBOARD, Tag.TAG_BYTE_ARRAY)) {
            return Arrays.hashCode(itemTag.getByteArray(CLIPBOARD));
        }
        if (itemTag.contains(CLIPBOARD, Tag.TAG_COMPOUND)) {
            return itemTag.getCompound(CLIPBOARD).hashCode();
        }
        return 0;
    }

    public static void putSaved(CompoundTag itemTag, ListTag saved) {
        CompoundTag wrapper = new CompoundTag();
        wrapper.put(WRAPPED_LIST, saved);
        itemTag.putByteArray(SAVED, compress(wrapper));
    }

    public static ListTag getSaved(@Nullable CompoundTag itemTag) {
        if (itemTag == null) {
            return new ListTag();
        }
        if (itemTag.contains(SAVED, Tag.TAG_BYTE_ARRAY)) {
            return decompress(itemTag.getByteArray(SAVED)).getList(WRAPPED_LIST, Tag.TAG_COMPOUND);
        }
        if (itemTag.contains(SAVED, Tag.TAG_LIST)) {
            return itemTag.getList(SAVED, Tag.TAG_COMPOUND);
        }
        return new ListTag();
    }

    public static boolean migrate(ItemStack stack) {
        CompoundTag itemTag = stack.getTag();
        if (itemTag == null) {
            return false;
        }
        boolean changed = false;
        if (itemTag.contains(CLIPBOARD, Tag.TAG_COMPOUND)) {
            itemTag.putByteArray(CLIPBOARD, compress(itemTag.getCompound(CLIPBOARD)));
            changed = true;
        }
        if (itemTag.contains(SAVED, Tag.TAG_LIST)) {
            putSaved(itemTag, itemTag.getList(SAVED, Tag.TAG_COMPOUND));
            changed = true;
        }
        return changed;
    }

    public static int itemTagSize(@Nullable CompoundTag itemTag) {
        if (itemTag == null) {
            return 0;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            NbtIo.write(itemTag, new DataOutputStream(out));
            return out.size();
        } catch (IOException e) {
            return 0;
        }
    }

    private static byte[] compress(CompoundTag tag) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static CompoundTag decompress(byte[] data) {
        try {
            return NbtIo.readCompressed(new ByteArrayInputStream(data));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
