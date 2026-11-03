package com.gtceuterminal.common.network;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.SchematicData;
import com.gtceuterminal.common.data.SchematicStorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// Client → Server: perform an action (save, load, delete) on a schematic in the Schematic Interface.
public class CPacketSchematicAction {
    private final ActionType actionType;
    private final String schematicName;
    private final int schematicIndex;

    public CPacketSchematicAction(ActionType actionType, String schematicName, int schematicIndex) {
        this.actionType = actionType;
        this.schematicName = schematicName;
        this.schematicIndex = schematicIndex;
    }

    public CPacketSchematicAction(FriendlyByteBuf buf) {
        this.actionType = buf.readEnum(ActionType.class);
        this.schematicName = buf.readUtf();
        this.schematicIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.actionType);
        buf.writeUtf(this.schematicName);
        buf.writeInt(this.schematicIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = findSchematicInterface(player);
            if (stack.isEmpty()) {
                return;
            }

            CompoundTag stackTag = stack.getOrCreateTag();

            switch (this.actionType) {
                case SAVE:
                    if (SchematicStorage.hasClipboard(stackTag)) {
                        player.displayClientMessage(
                                Component.translatable("item.gtceuterminal.schematic_action.message.no_clipboard_to_save"),
                                true
                        );
                        return;
                    }

                    CompoundTag clipboardTag = SchematicStorage.getClipboard(stackTag);
                    if (clipboardTag == null || !clipboardTag.contains("Blocks") || clipboardTag.getList("Blocks", 10).isEmpty()) {
                        player.displayClientMessage(
                                Component.translatable("item.gtceuterminal.schematic_action.message.clipboard_empty"),
                                true
                        );
                        return;
                    }

                    SchematicData clipboard = SchematicData.fromNBT(clipboardTag, player.level().registryAccess());

                    List<SchematicData> existingSchematics = loadSchematics(stack, player);
                    boolean isDuplicate = existingSchematics.stream()
                            .anyMatch(s -> s.name().equals(this.schematicName));

                    if (isDuplicate) {
                        player.displayClientMessage(
                                Component.translatable("item.gtceuterminal.schematic_action.message.schematic_name_already_exists"),
                                true
                        );
                        return;
                    }

                    SchematicData namedSchematic = new SchematicData(
                            this.schematicName,
                            clipboard.multiblockType(),
                            clipboard.blocks(),
                            clipboard.blockEntities(),
                            clipboard.originalFacing()
                    );

                    existingSchematics.add(namedSchematic);
                    saveSchematics(stack, existingSchematics);

                    if (SchematicStorage.itemTagSize(stack.getTag()) > SchematicStorage.MAX_ITEM_NBT_BYTES) {
                        existingSchematics.remove(namedSchematic);
                        saveSchematics(stack, existingSchematics);
                        player.displayClientMessage(
                                Component.translatable("item.gtceuterminal.schematic_action.message.too_large"),
                                true
                        );
                        return;
                    }

                    GTCEUTerminalMod.LOGGER.info("Saved schematic '{}' with originalFacing: {}",
                            this.schematicName, clipboard.originalFacing());

                    player.displayClientMessage(
                            Component.translatable(
                                    "item.gtceuterminal.schematic_action.message.saved_schematic",
                                    this.schematicName
                            ),
                            true
                    );
                    break;

                case LOAD:
                    List<SchematicData> schematics = loadSchematics(stack, player);
                    if (this.schematicIndex >= 0 && this.schematicIndex < schematics.size()) {
                        SchematicData schematic = schematics.get(this.schematicIndex);

                        SchematicStorage.putClipboard(stackTag, schematic.toNBT());

                        GTCEUTerminalMod.LOGGER.info("Loaded schematic '{}' with originalFacing: {}",
                                schematic.name(), schematic.originalFacing());

                        player.displayClientMessage(
                                Component.translatable(
                                        "item.gtceuterminal.schematic_action.message.loaded_schematic",
                                        schematic.name()
                                ),
                                true
                        );
                    }
                    break;

                case DELETE:
                    List<SchematicData> allSchematics = loadSchematics(stack, player);
                    boolean removed = allSchematics.removeIf(s -> s.name().equals(this.schematicName));
                    if (removed) {
                        saveSchematics(stack, allSchematics);
                        player.displayClientMessage(
                                Component.translatable(
                                        "item.gtceuterminal.schematic_action.message.deleted_schematic",
                                        this.schematicName
                                ),
                                true
                        );
                    } else {
                        GTCEUTerminalMod.LOGGER.warn("DELETE: schematic '{}' not found on server", this.schematicName);
                    }
                    break;

                case RENAME: {
                    List<SchematicData> toRename = loadSchematics(stack, player);
                    if (this.schematicIndex >= 0 && this.schematicIndex < toRename.size()) {
                        String newName = this.schematicName;
                        boolean nameTaken = false;
                        for (int i = 0; i < toRename.size(); i++) {
                            if (i != this.schematicIndex
                                    && toRename.get(i).name().equalsIgnoreCase(newName)) {
                                nameTaken = true;
                                break;
                            }
                        }
                        if (nameTaken) {
                            player.displayClientMessage(
                                    Component.translatable("item.gtceuterminal.schematic_action.message.schematic_name_already_exists"),
                                    true);
                            return;
                        }
                        SchematicData old = toRename.get(this.schematicIndex);
                        toRename.set(this.schematicIndex, new SchematicData(newName, old.multiblockType(),
                                old.blocks(), old.blockEntities(), old.originalFacing()));
                        saveSchematics(stack, toRename);
                        player.displayClientMessage(
                                Component.translatable("item.gtceuterminal.schematic_action.message.renamed_schematic", newName),
                                true);
                    }
                    break;
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private ItemStack findSchematicInterface(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem().toString().contains("schematic_interface")) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem().toString().contains("schematic_interface")) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private List<SchematicData> loadSchematics(ItemStack stack, ServerPlayer player) {
        List<SchematicData> schematics = new ArrayList<>();

        ListTag savedList = SchematicStorage.getSaved(stack.getTag());
        for (int i = 0; i < savedList.size(); i++) {
            try {
                schematics.add(SchematicData.fromNBT(savedList.getCompound(i), player.level().registryAccess()));
            } catch (Exception e) {
                GTCEUTerminalMod.LOGGER.error("Error loading schematic {}: {}", i, e.getMessage());
            }
        }

        return schematics;
    }

    private void saveSchematics(ItemStack stack, List<SchematicData> schematics) {
        ListTag savedList = new ListTag();
        for (SchematicData schematic : schematics) {
            savedList.add(schematic.toNBT());
        }
        SchematicStorage.putSaved(stack.getOrCreateTag(), savedList);
    }

    public enum ActionType {
        SAVE,
        LOAD,
        DELETE,
        RENAME
    }
}