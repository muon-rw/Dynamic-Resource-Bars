# Dynamic RPG Resource Bars

Animated, adjustable resource bars for Minecraft.

[![CurseForge](https://img.shields.io/badge/CurseForge-Download-orange)](https://www.curseforge.com/minecraft/mc-mods/dynamic-rpg-resource-bars)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-green)](https://modrinth.com/mod/dynamic-resource-bars)

---

# Resource Pack Guide
> [!WARNING] 
> This guide applies to **26.1.2 *only***. 

This guide covers everything you need to make a resource pack: file layout, animation, scaling, and the in-game HUD editor.

## Quick start: recolor the bars

1. Create a pack at `resourcepacks/my_bars/` with a `pack.mcmeta`.
2. Drop a recolored `health_bar.png` into `assets/dynamic_resource_bars/textures/gui/`.
3. Enable the pack in-game and reload with **F3+T**.

That's it! No additional `.mcmeta` or config is required if you aren't resizing things. 
Even if you are, for non-animated textures, the mod generally auto-detects PNG dimensions and applies sensible defaults per layer.

## File layout

```
my_pack/
├── pack.mcmeta
└── assets/dynamic_resource_bars/textures/gui/
    ├── health_bar.png              ─┐
    ├── health_bar.png.mcmeta        │
    ├── stamina_bar.png              │  Bars (animated vertical strips,
    ├── stamina_bar.png.mcmeta       │  default 256×1024 = 32 frames of 32px)
    ├── mana_bar.png                 │
    ├── mana_bar.png.mcmeta          │
    ├── air_bar.png                  │
    ├── air_bar.png.mcmeta          ─┘
    │
    ├── health_bar_poisoned.png     ─┐
    ├── health_bar_withered.png      │
    ├── health_bar_frozen.png        │  Bar variants inherit
    ├── health_bar_scorched.png      │  their parent's animation
    ├── stamina_bar_warning.png      │  settings
    ├── stamina_bar_critical.png     │
    ├── stamina_bar_hunger.png       │
    ├── stamina_bar_mounted.png      │
    ├── stamina_bar_blood.png       ─┘
    │
    ├── health_background.png       ─┐
    ├── health_foreground.png        │
    ├── stamina_background.png       │  Static layers
    ├── stamina_foreground.png       │  (default 256×256)
    ├── mana_background.png          │
    ├── mana_foreground.png          │
    ├── air_background.png           │
    ├── armor_background.png         │
    ├── armor_bar.png               ─┘  Armor bar is static by default, so no .mcmeta here
    │
    ├── absorption_overlay.png      ─┐
    ├── regeneration_overlay.png     │
    ├── protection_overlay.png       │
    ├── heat_overlay.png             │  Status overlays
    ├── cold_overlay.png             │
    ├── wetness_overlay.png          │
    ├── hardcore_overlay.png         │
    ├── comfort_overlay.png          │
    ├── nourishment_overlay.png     ─┘
    │
    ├── air/                        ── Bubble icons by tier — air_0…air_4
    │   ├── air_0.png … air_4.png      plus *_pop variants that flash briefly
    │   └── air_1_pop.png … air_4_pop.png   when crossing a tier
    │
    └── armors/                     ── Armor icons — tier_0.png … tier_10.png,
        └── tier_0.png … tier_10.png    picked by armorValue / maxExpectedArmor
```

All files are optional. The mod falls back to its bundled defaults for anything you don't override.

## Texture sizes

PNG dimensions are auto-detected. Use whatever resolution you want as long as the bar's height divides evenly into frames:

| Style    | Bar size     | Frame height |
|----------|--------------|-------------:|
| Standard | 256 × 1024   |           32 |
| HD 2x    | 512 × 2048   |           64 |
| Compact  | 128 × 512    |           32 |

Bars use vertical strips: each frame is stacked top-to-bottom in the PNG. Backgrounds, foregrounds, and overlays can be any size.

## Animation (`.mcmeta` for bars)

Standard Minecraft animation format. Only the `height` field is required for non-default frame sizes.

```json
{
  "animation": {
    "frametime": 3,
    "interpolate": false,
    "height": 32
  }
}
```

| Field         | Default | Notes                               |
|---------------|--------:|-------------------------------------|
| `frametime`   |       3 | Ticks per frame (lower = faster).   |
| `height`      |      32 | Frame height in pixels.             |
| `interpolate` |   false | Smooth blending between frames.     |

Variants (`health_bar_poisoned.png`, etc.) inherit the base bar's `.mcmeta`, so all variants for a bar must share frame dimensions.

## Scaling (`.mcmeta` for backgrounds, foregrounds, overlays)

Bars sample UV space to show fill percentage and **don't support scaling**. Everything else does, via a `dynamic_resource_bars.scaling` block.

### Simple modes

```json
{ "dynamic_resource_bars": { "scaling": "stretch" } }
```

`"none"` (1:1 sampling), `"stretch"`, or `"tile"`.

### Nine-slice

```json
{
  "dynamic_resource_bars": {
    "scaling": {
      "type": "nine_slice",
      "left": 4, "right": 4, "top": 3, "bottom": 3,
      "edges": "stretch",
      "center": "stretch"
    }
  }
}
```

Corners are fixed; `edges` and `center` can each be `"stretch"` (default) or `"tile"`.

### Source region (texture smaller than its sheet)

If your graphic only fills part of the PNG (e.g., 80×10 in a 256×256 sheet), point the renderer at the right area:

```json
{
  "dynamic_resource_bars": {
    "scaling": {
      "type": "nine_slice",
      "left": 2, "right": 2, "top": 2, "bottom": 2,
      "source": { "u": 0, "v": 0, "width": 80, "height": 10 }
    }
  }
}
```

`u`/`v` default to 0. `source` also works on simple modes.

### Defaults when no `.mcmeta` is present

| Layer                    | Default scaling                   |
|--------------------------|-----------------------------------|
| Background / Foreground  | nine-slice `(4,4,3,3)`, stretch   |
| Animated overlay         | nine-slice `(3,3,2,2)`, tile      |
| Static overlay           | stretch                           |
| Bar                      | n/a (UV sampling)                 |

The bundled `EXAMPLE_*.mcmeta` files in the mod's assets show every mode in working form — copy one and tweak.

## In-game HUD editor

Open via the mod config menu. The editor lets you reposition every element live.

- **Drag** to move; **Tab** cycles through bars; **arrow keys** nudge by 1px; **Shift+arrows** resize.
- **Right-click** any element for a context menu (anchor, fill direction, reset, etc.).
- Each bar has draggable sub-elements: background, bar, foreground, and text. The selected element is outlined.
- Anchor a bar to one of nine screen regions so it stays in place when the window resizes.
- Fill direction can be horizontal or vertical.

Changes save to `config/dynamic_resource_bars-client.toml`.

## Troubleshooting

**Custom textures don't show up.** Check the pack is enabled and at the top of the list, the path is exactly `assets/dynamic_resource_bars/textures/gui/`, and filenames match (e.g., `health_bar.png`, not `healthbar.png`). Reload with **F3+T**.

**Bar looks stretched or cuts mid-frame.** Texture height must be evenly divisible by `height` in the `.mcmeta`. All variants of a bar must use the same frame dimensions.

**Nine-slice tiles in empty space.** Your graphic doesn't fill the sheet — add a `source` region.

**Tiled overlay has visible seams.** The texture isn't seamless. Use your editor's offset/wrap test, or switch to nine-slice with stretched edges.

**HD bars work but config sliders cap at 256px.** Known limitation. Edit `config/dynamic_resource_bars-client.toml` directly and set, e.g., `healthBarWidth = 512`.

For anything else, check `logs/latest.log` — the mod logs detected dimensions and any `.mcmeta` parse errors.
