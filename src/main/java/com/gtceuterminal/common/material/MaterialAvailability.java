package com.gtceuterminal.common.material;

import net.minecraft.world.item.Item;

public class MaterialAvailability {
    private final Item item;
    private final int required;
    private int inInventory;
    private int inNearbyChests;
    private long inMENetwork;

    public MaterialAvailability(Item item, int required) {
        this.item = item;
        this.required = required;
        this.inInventory = 0;
        this.inNearbyChests = 0;
        this.inMENetwork = 0;
    }

    public Item getItem() {
        return item;
    }

    public int getRequired() {
        return required;
    }

    public int getInInventory() {
        return inInventory;
    }

    public void setInInventory(int amount) {
        this.inInventory = amount;
    }

    public int getInNearbyChests() {
        return inNearbyChests;
    }

    public void setInNearbyChests(int amount) {
        this.inNearbyChests = amount;
    }

    public long getInMENetwork() {
        return inMENetwork;
    }

    public void setInMENetwork(long amount) {
        this.inMENetwork = amount;
    }

    public long getTotalAvailable() {
        return inInventory + inNearbyChests + inMENetwork;
    }

    public boolean hasEnough() {
        return getTotalAvailable() >= required;
    }

    public int getMissing() {
        return Math.max(0, required - (int) getTotalAvailable());
    }

    public String getItemName() {
        return item.getDescription().getString();
    }
}