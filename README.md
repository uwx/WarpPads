# Spigot Warp Pads

A teletransportation plugin for [Spigot](https://www.spigotmc.org/). Remake of [SmoochyPit/Warp-Pads](https://github.com/SmoochyPit/Warp-Pads)

![Warp Pads Demo GIF 1](https://i.imgur.com/vLwdrmH.gif "Warp Pads Demo GIF 1")

![Warp Pads Demo GIF 2](https://i.imgur.com/fwrORns.gif "Warp Pads Demo GIF 2")

Plugin template from [sgrayme/SpigotPlugin](https://github.com/sgrayme/SpigotPlugin)

## Installation

The plugin currently has to be compiled manually with `mvn package`.

## Usage

All the recipes have recipe book entries, so you can refer to those if in doubt.

* Make a Warp Pad. The most basic one, Tier 1, can be made out of a Block of Gold, 4 Nether Quartz, and a Golden Apple.
  It has a max teleport circular range of 500 blocks.
* Place two Warp Pads.
* Step on one Warp Pad.
* Look towards the redstone dust guide (you may have to turn on particles), it'll glow purple, or whatever color you dye
  your warp pad to, when it's hihglighted.
* Sneak to teleport to the highlighted Warp Pad.
* Congratulations! You've achieved teletransportation!

### Renaming Warp Pads

Warp Pads can be renamed in an anvil. The blocks can be mined with a pickaxe, and will drop a Warp Pad item with the
same name and label.

### Coloring Warp Pads

You can dye a Warp Pad by right clicking it (placed in the world) with any dye. The dye won't get consumed. This will
change the color of the label and the redstone dust highlight particle for the Warp Pad.

If you're unsatisfied with the color, you can right click the warp pad with a Bucket of Water to restore the original
purplish color. This will consume the water in the bucket.

### Private Warp Pads

Right-click the Warp Pad with a diamond to make it private. The diamond will be consumed. Private Warp Pads are only
visible (can be warped to) for players authorized through the /warpallow and /warpdeny commands.

*Warning: The warp pad will be made public and the diamond will be lost when it is broken.*

### Tiers of Warp Pads

Tier 1: Has a maximum circular range of 500 blocks. Crafted using a Block of Gold, 4 Nether Quartz, and a Golden Apple.

![Tier 1 Crafting Demo](https://i.imgur.com/NMw49Ur.png "Tier 1 Crafting Demo")


Tier 2: Has a maximum circular range of 3000 blocks. Crafted using a Block of Emerald, 4 Phantom Membranes, and a Ghast
Tear.

![Tier 2 Crafting Demo](https://i.imgur.com/QPvqCDi.png "Tier 2 Crafting Demo")


Tier 3: Has an unlimited range. Crafted using a Block of Netherite, 4 Ender Pearls, and a Nether Star.

![Tier 3 Crafting Demo](https://i.imgur.com/yS7u9lV.png "Tier 3 Crafting Demo")


All Warp Pads from tiers 1 to 3, side-by-side

![All Warp Pads from tiers 1 to 3, side-by-side](https://i.imgur.com/axIE0i4.png "All Warp Pads from tiers 1 to 3, side-by-side")