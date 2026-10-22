# GregTech Terminals

A lightweight GregTech CEu Modern addon. It adds four gadgets that inspect and manipulate GTM multiblocks, plus a public API for mods and KubeJS packs to register their own tiered components.

## Gadgets

- **[Multi-Structure Manager](multi-structure-manager.md)**: sees every multiblock within a 32-block radius and upgrades any tiered block (coils, I/O hatches, I/O buses, energy hatches, parallel hatches, mufflers, custom tiered casings) straight from your inventory or AE2. It can also auto-build a structure and request missing blocks from your ME network.
- **[Schematic Interface](schematic-interface.md)**: shift + right-click a controller to copy a formed structure as a named schematic, then paste it later (optionally pulling blocks from AE2).
- **[Dismantler](dismantler.md)**: shift + right-click a controller to pick up every block of the structure into your inventory.
- **[Energy Analyzer](energy-analyzer.md)**: Monitor the energy usage of machines, multiblocks, energy hatches, singleblocks and battery boxes.

## For pack and mod developers

Terminals can recognize and upgrade blocks from other mods. Register them through either surface:

- **[Registering Components (Java)](registering-components-java.md)**: the full `TerminalAPI`, every component category, custom groups and detectors.
- **[Registering Components (KubeJS)](registering-components-kubejs.md)**: the `GTCEuTerminalEvents.components` startup event for packs.
- **[Tier Reference](tier-reference.md)**: the shared tier number to voltage name table used by both.

## Supported mods and packs

Specially tailored integrations for unique hatches and tiered casings:

- AstroGreg: Exsilium
- TerraFirmaGreg
- Phoenix Forge Technologies
- GTM Things
- Star Technology: Beyond

Report pack-specific bugs on the [issue tracker](https://github.com/BgameXL/GTCEu-Terminals/issues) or on [Discord](https://discord.gg/kAKk3wRAVh). To request an integration, leave a suggestion.

## Credits

- Haze Vista, for textures and ideas.
- yan (yan072600), for the simplified Chinese translation and making the mod translatable.
- GTM Things, for inspiring the Multi-Structure Manager.
- GregTech CEu Modern.
- WhitePhant0m, for AE2 Wireless terminal support in Curios slots.

Contributors are welcome and encouraged.
