# Registering Components — Java API

Use [TerminalAPI](https://github.com/BgameXL/GTCEu-Terminals/blob/main/src/main/java/com/gtceuterminal/api/TerminalAPI.java) to expose existing blocks to Terminals so it can recognize, display,
and offer them as upgrade candidates.

Call these methods inside `FMLCommonSetupEvent`:

```java
import com.gtceuterminal.api.TerminalAPI;
import com.gtceuterminal.common.config.ComponentEntry;
import com.gtceuterminal.common.config.ComponentRegistry;
import com.gtceuterminal.common.multiblock.ComponentGroup;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MyModEvents {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            TerminalAPI.registerCoil(...);
            TerminalAPI.registerEnergyHatch(...);
            // etc.
        });
    }
}
```

---

## Coils

```java
TerminalAPI.registerCoil(
    "mymod:graphene_coil",  // block registry ID
    "Graphene Coil",        // display name
    8,                      // tier (see tier table below)
    9600                    // temperature in K
);
```

---

## Energy Hatches

`hatchType`: `"INPUT"` for energy hatches, `"OUTPUT"` for dynamo hatches.
`amperage`: `"2A"`, `"4A"`, `"16A"`.

```java
TerminalAPI.registerEnergyHatch("mymod:uev_energy_hatch", "UEV Energy Hatch", "INPUT",  "2A",  9);
TerminalAPI.registerEnergyHatch("mymod:uev_dynamo_hatch", "UEV Dynamo Hatch",  "OUTPUT", "2A",  9);
TerminalAPI.registerEnergyHatch("mymod:uev_4a_hatch",     "UEV 4A Hatch",      "INPUT",  "4A",  9);
TerminalAPI.registerEnergyHatch("mymod:uev_16a_hatch",    "UEV 16A Hatch",     "INPUT",  "16A", 9);
```

## Substation Hatches

```java
TerminalAPI.registerEnergyHatch("mymod:uev_substation_input",  "UEV Substation Input",  "INPUT",  "64A", 9);
TerminalAPI.registerEnergyHatch("mymod:uev_substation_output", "UEV Substation Output", "OUTPUT", "64A", 9);
```

## Laser Hatches

`amperage`: `"256A"`, `"1024A"`, `"4096A"`.

```java
TerminalAPI.registerLaserHatch("mymod:uev_laser_input",  "UEV Laser Input",  "INPUT",  "256A",  9);
TerminalAPI.registerLaserHatch("mymod:uev_laser_output", "UEV Laser Output", "OUTPUT", "4096A", 9);
```

## Fluid Hatches

`capacity`: `"1x"`, `"4x"`, `"9x"`.

```java
TerminalAPI.registerFluidHatch("mymod:uev_fluid_input",  "UEV Input Hatch",  "INPUT",  "1x", 9);
TerminalAPI.registerFluidHatch("mymod:uev_fluid_output", "UEV Output Hatch", "OUTPUT", "1x", 9);
TerminalAPI.registerFluidHatch("mymod:uev_4x_input",     "UEV 4x Hatch",     "INPUT",  "4x", 9);
TerminalAPI.registerFluidHatch("mymod:uev_9x_output",    "UEV 9x Hatch",     "OUTPUT", "9x", 9);
```

## Item Buses

```java
TerminalAPI.registerBus("mymod:uev_input_bus",  "UEV Input Bus",  "INPUT",  9);
TerminalAPI.registerBus("mymod:uev_output_bus", "UEV Output Bus", "OUTPUT", 9);
```

## Dual Hatches

```java
TerminalAPI.registerDualHatch("mymod:uev_dual_input",  "UEV Dual Input",  "INPUT",  9);
TerminalAPI.registerDualHatch("mymod:uev_dual_output", "UEV Dual Output", "OUTPUT", 9);
```

## Muffler Hatches

```java
TerminalAPI.registerMufflerHatch("mymod:uev_muffler", "UEV Muffler Hatch", 9);
```

---

## Generic Component

For blocks that don't fit any standard category, or when you need extra attributes:

```java
TerminalAPI.registerComponent(
    ComponentRegistry.ENERGY_HATCHES,   // or any custom String category
    ComponentEntry.builder("mymod:special_hatch", "Special Hatch", "UEV", 9)
        .attr("hatchType", "INPUT")
        .attr("amperage", "64A")
        .build()
);
```

Built-in category constants on `ComponentRegistry`:

| Constant | String value |
|---|---|
| `ENERGY_HATCHES` | `"energy_hatches"` |
| `FLUID_HATCHES` | `"fluid_hatches"` |
| `BUSES` | `"buses"` |
| `LASER_HATCHES` | `"laser_hatches"` |
| `WIRELESS_HATCHES` | `"wireless_hatches"` |
| `SUBSTATION_HATCHES` | `"substation_hatches"` |
| `PARALLEL_HATCHES` | `"parallel_hatches"` |
| `MUFFLER_HATCHES` | `"muffler_hatches"` |
| `MAINTENANCE_HATCHES` | `"maintenance_hatches"` |
| `DUAL_HATCHES` | `"dual_hatches"` |
| `COILS` | `"coils"` |

---

## Custom Groups

Use this when your mod/addon introduces a `PartAbility` that Terminals doesn't know about.
The `detector` lambda enables automatic detection during multiblock scanning. Without it,
blocks only appear in Terminals if registered manually via `TerminalAPI.registerComponent()`.

```java
TerminalAPI.registerGroup(
    ComponentGroup.builder("my_hatch", "My Hatch")
        .color(0xFF00AAFF)
        .energyHandler()
        .registryCategory(ComponentRegistry.WIRELESS_HATCHES)
        .detector(b -> MyAddon.MY_ABILITY.isApplicable(b))
        .build(),
    "my_ability_name"   // PartAbility.getName()
);
```

## Tier Reference

| Tier | Name |
|---|---|
| 0 | ULV |
| 1 | LV |
| 2 | MV |
| 3 | HV |
| 4 | EV |
| 5 | IV |
| 6 | LuV |
| 7 | ZPM |
| 8 | UV |
| 9 | UHV |
| 10 | UEV |
| 11 | UIV |
| 12 | UXV |
| 13 | OpV |
| 14 | MAX |