package com.gtceuterminal.common.multiblock;

import java.util.ArrayList;
import java.util.List;

// Groups ComponentInfo instances of the same type/tier for display in the UI.
public class ComponentInfoGroup {

    private final ComponentGroup group;
    private final int tier;
    private final String blockName;
    private final List<ComponentInfo> components = new ArrayList<>();

    public ComponentInfoGroup(ComponentGroup group, int tier, String blockName) {
        this.group     = group;
        this.tier      = tier;
        this.blockName = blockName;
    }

    public static String getGroupKey(ComponentGroup group, int tier, String blockName) {
        return group.id + ":" + tier + ":" + blockName;
    }

    public void addComponent(ComponentInfo comp) { components.add(comp); }

    public ComponentGroup getGroup()            { return group; }
    public int getTier()                        { return tier; }
    public String getBlockName()                { return blockName; }
    public List<ComponentInfo> getComponents()  { return components; }
    public int getCount()                       { return components.size(); }

    public ComponentInfo getRepresentative() {
        return components.isEmpty() ? null : components.get(0);
    }
}