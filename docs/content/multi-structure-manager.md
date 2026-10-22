# Multi-Structure Manager

The Multi-Structure Manager (MSM) scans for every multiblock within a 32-block radius of the player and lets you inspect, upgrade, and auto-build structures without making them hand by hand.

## What it does

- **Scan**: detects multiblocks nearby and lists their components grouped by category.
- **Upgrade**: swaps any tiered block in place (coils, energy hatches, I/O hatches and buses, parallel hatches, mufflers, custom tiered casings) using blocks from your inventory or your ME network.
- **Auto-build**: builds the full structure for a controller, placing blocks from your inventory or AE2 and requesting what is missing.
- **Request**: queues missing blocks as AE2 crafting.

## AE2 link

The Manager connects to your ME network through a Wireless Access Point. Link it while in range of an access point, then turn on **Use AE2** in the settings to pull upgrade and build materials from the network.

## Auto-build settings

Open the settings panel to control how the Manager builds a structure.

| Setting         | Values   | What it does                                                                                               |
|-----------------|----------|------------------------------------------------------------------------------------------------------------|
| No Hatch Mode   | Yes / No | Build the structure without placing hatches, buses or other multiblock parts (casings only).               |
| Tier Preference | 0 to 14  | The exact tier to use for tiered blocks. If that tier is not available, the nearest tier you have is used. |
| Repeat Count    | 0 to 99  | How many times to repeat a repeatable layer. 0 uses the structure minimum.                                 |
| Use AE2         | Yes / No | Pull, build and upgrade materials from the linked ME network. Requires an access point link.               |

## How Tier Preference works

Tier Preference is a priority filter, for any cell that accepts a tiered block, the Manager prefers the block whose tier matches your number exactly. If you do not have that tier (in your inventory or ME network), it fallbacks to the nearest tier you do have.

Example: set Tier Preference to 4 (EV).

- You have an EV energy hatch: it places EV.
- You only have an IV hatch: it places IV.
- You have both IV and UHV: it places IV, because IV is closer to EV than UHV.

This works for every tiered block the same way, including custom components registered by other mods. See [Registering Components (Java)](registering-components-java.md) and [Registering Components (KubeJS)](registering-components-kubejs.md).