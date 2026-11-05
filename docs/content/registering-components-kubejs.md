# Registering Components (KubeJS)

GTCEu Terminals exposes a startup event for registering **existing** blocks into Terminals. Scripts go in
`kubejs/startup_scripts/`.

```js
GTCEuTerminalEvents.components(event => {
    // register things here
});
```

## What this event does

This event **registers existing blocks** so Terminals knows about them. It tells Terminals how to classify, display, and
upgrade a block that already exists in the game.

It does not create new blocks. KubeJS cannot create hatch part machines because GTM does not expose
`MultiblockPartMachine`, `IO`, or `EnergyHatchPartMachine` as KubeJS bindings so creating a new hatch requires Java.

What this event is for:

- A mod/addon already created a hatch block and registered it with a GTM `PartAbility`.
- You want Terminals to recognize it so show it in the component list, and offer it as an upgrade candidate.

**Automatic detection** works for any block registered under a standard GTM `PartAbility` (`INPUT_ENERGY`,
`IMPORT_FLUIDS`, `INPUT_LASER`, etc.). Terminals scans the multiblock and component groups automatically. In that case
this event is only needed to populate the upgrade candidate list with the correct metadata.

For blocks using a custom `PartAbility` from another addon, automatic detection requires a Java `detector` lambda.
See [Registering Components (Java)](registering-components-java.md).

---

## Coils

```js
event.addCoil(
    "mymod:superludicrous_coil",  // block registry ID, must already exist
    "Superludicrous Coil",        // display name
    8,                            // tier
    9600                          // temperature
);
```

---

## Energy Hatches

`HatchType`: `"INPUT"` for energy hatches, `"OUTPUT"` for dynamo hatches.
`amperage`: `"2A"`, `"4A"`, `"16A"`.

```js
event.addEnergyHatch("mymod:uev_energy_hatch", "UEV Energy Hatch", "INPUT", "2A", 9);
event.addEnergyHatch("mymod:uev_dynamo_hatch", "UEV Dynamo Hatch", "OUTPUT", "2A", 9);
event.addEnergyHatch("mymod:uev_4a_hatch", "UEV 4A Hatch", "INPUT", "4A", 9);
event.addEnergyHatch("mymod:uev_16a_hatch", "UEV 16A Hatch", "INPUT", "16A", 9);
```

## Laser Hatches

`amperage`: `"256A"`, `"1024A"`, `"4096A"`.

```js
event.addLaserHatch("mymod:uev_laser_input", "UEV Laser Input", "INPUT", "256A", 9);
event.addLaserHatch("mymod:uev_laser_output", "UEV Laser Output", "OUTPUT", "4096A", 9);
```

## Fluid Hatches

`capacity`: `"1x"`, `"4x"`, `"9x"`.

```js
event.addFluidHatch("mymod:uev_fluid_input", "UEV Input Hatch", "INPUT", "1x", 9);
event.addFluidHatch("mymod:uev_fluid_output", "UEV Output Hatch", "OUTPUT", "1x", 9);
event.addFluidHatch("mymod:uev_4x_input", "UEV 4x Hatch", "INPUT", "4x", 9);
event.addFluidHatch("mymod:uev_9x_output", "UEV 9x Hatch", "OUTPUT", "9x", 9);
```

## Item Buses

```js
event.addBus("mymod:uev_input_bus", "UEV Input Bus", "INPUT", 9);
event.addBus("mymod:uev_output_bus", "UEV Output Bus", "OUTPUT", 9);
```

## Dual Hatches

```js
event.addDualHatch("mymod:uev_dual_input", "UEV Dual Input", "INPUT", 9);
event.addDualHatch("mymod:uev_dual_output", "UEV Dual Output", "OUTPUT", 9);
```

## Muffler Hatches

```js
event.addMufflerHatch("mymod:uev_muffler", "UEV Muffler Hatch", 9);
```

---

## Generic Component

Registers a block into any category without needing a specific method. Useful for blocks that do not fit a standard
type, or when you need extra attributes.

```js
event.addComponent(
    "energy_hatches",
    "mymod:uev_64a_energy_hatch",  // block registry ID, must already exist
    "UEV 64A Energy Hatch",
    9,
    {hatchType: "INPUT", amperage: "64A"}  // optional attrs map
);

// Without attrs
event.addComponent("coils", "mymod:graphene_coil", "Graphene Coil", 8);
```

## Custom Groups

Registers a new component group. In Java, pairing a group with a `detector` lambda makes its blocks show up as
their own category in the [detail](https://github.com/BgameXL/GTCEu-Terminals/blob/main/src/main/java/com/gtceuterminal/client/gui/multiblock/MultiblockDetailPanel.java) and [upgrade](https://github.com/BgameXL/GTCEu-Terminals/blob/main/src/main/java/com/gtceuterminal/client/gui/multiblock/InlineUpgradePanel.java) panels during multiblock scanning.

From KubeJS you cannot pass a detector, so a group created here will not form a scanned category on its own. Use it to
give the blocks you add with `addComponent` a home and the correct upgrade metadata; for a group that is detected
automatically, create it in Java with a `detector` (see [Registering Components (Java)](registering-components-java.md)).

```js
// 1. Register the group
event.addGroup(
    "my_custom_hatch",   // unique id
    "My Custom Hatch",   // display name
    0xFF00AAFF,          // color as 0xAARRGGBB
    "energy",            // handlerType: "energy", "fluid", "item", "data", "special", or null
    "energy_hatches",    // registryCategory, where upgrade candidates are looked up
    true                 // upgradeable
);

// 2. Register the blocks into that category so they appear as upgrade candidates
event.addComponent("energy_hatches", "mymod:my_hatch_input", "My Input Hatch", 9, {hatchType: "INPUT"});
event.addComponent("energy_hatches", "mymod:my_hatch_output", "My Output Hatch", 9, {hatchType: "OUTPUT"});
```

`handlerType` controls which section of the UI the group appears in:

| Value       | Section                                         |
|-------------|-------------------------------------------------|
| `"energy"`  | Energy handlers                                 |
| `"fluid"`   | Fluid handlers                                  |
| `"item"`    | Item handlers                                   |
| `"data"`    | Data handlers                                   |
| `"special"` | Special components (maintenance, muffler, etc.) |
| `null`      | Generic / uncategorized                         |

> **Note:** Only `"energy"` and `"data"` currently change behavior (they control which groups the Energy Analyzer
> inspects). `"fluid"`, `"item"`, and `"special"` are accepted and stored, but nothing reads them yet, so they have no
> functional effect today. Set them so the metadata is correct for the future, but do not rely on them changing anything
> for now.

---

## Tier Reference

See [Tier Reference](tier-reference.md) for the tier number to voltage name table.
