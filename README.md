# Dynamic RPG Resource Bars
### Animated, adjustable resource bars for Minecraft!

[![Download on CurseForge](https://img.shields.io/badge/CurseForge-Download-orange)](https://www.curseforge.com/minecraft/mc-mods/dynamic-rpg-resource-bars)
[![Download on Modrinth](https://img.shields.io/badge/Modrinth-Download-green)](https://modrinth.com/mod/dynamic-resource-bars)

---

# Resource Pack Creator Guide

This guide covers everything you need to create custom bar textures, animations, and shapes for Dynamic Resource Bars.

## Table of Contents
1. [Common Questions](#common-questions)
2. [Quick Start: Recoloring Bars](#quick-start-recoloring-bars)
3. [File Structure](#file-structure)
4. [Texture Requirements](#texture-requirements)
5. [Animation System](#animation-system)
6. [Scaling System](#scaling-system-nine-slice--tiling)
7. [Mask System (Shaped Bars)](#mask-system-shaped-bars)
8. [Combining Features](#combining-features-practical-examples)
9. [Feature Matrix](#feature-compatibility-matrix)
10. [In-Game HUD Editor](#in-game-hud-editor)
11. [Troubleshooting](#troubleshooting)

---

## Common Questions

**Q: How do I change bar colors?**  
A: Replace bar textures in a resource pack. See [Quick Start](#quick-start-recoloring-bars).

**Q: Can I make bars with rounded corners?**  
A: Yes! Use [mask textures](#mask-system-shaped-bars). Masks let you create any shape.

**Q: Can I use high-resolution (HD) textures?**  
A: Yes! The mod auto-detects texture dimensions. Use 512x2048 or any size. Power of 2 texture sizes are recommended!

**Q: How do I add decorative borders to backgrounds?**  
A: Use [nine-slice scaling](#scaling-system-nine-slice--tiling) with tile mode for borders.

**Q: Do I need .mcmeta files?**  
A: No! The mod uses smart defaults. Only add .mcmeta to customize.

**Q: What's the difference between texture size and rendered size?**  
A: **Texture size** = PNG file dimensions (256x1024). **Rendered size** = on-screen pixels (74x4). The mod scales automatically!

**Q: Why can't I use nine-slice on bars?**  
A: Bars use UV sampling + masks for fill percentage. Use masks for bar shapes instead!

**Q: Can I mix features (HD + masks + nine-slice)?**  
A: Yes! See [Combining Features](#combining-features-practical-examples) for examples.

---

## Quick Start: Recoloring Bars

**Q**: "How do I change bar colors?"

**A**: Replace the bar textures in a resource pack!

### Step 1: Create Resource Pack Structure
```
resourcepacks/
  my_custom_bars/
    pack.mcmeta
    assets/
      dynamic_resource_bars/
        textures/
          gui/
            health_bar.png          ← Replace this!
            health_bar.png.mcmeta   ← Optional: animation settings
            stamina_bar.png         ← Replace this!
            mana_bar.png            ← Replace this!
            air_bar.png             ← Replace this!
```

### Step 2: Create pack.mcmeta
```json
{
  "pack": {
    "pack_format": 15,
    "description": "My Custom Bars"
  }
}
```

### Step 3: Replace Bar Textures
- **Copy original textures** from the mod's assets as a starting point
- **Edit colors** in your image editor (Photoshop, GIMP, Aseprite, etc.)
- **Dimensions**: Standard is 256 x 1024 (but any size works!)
  - Want higher resolution? Use 512 x 2048! (update .mcmeta accordingly)
  - Want smaller files? Use 128 x 512!
- **Save as PNG** with transparency support

### Step 4: Load & Test
1. Place your resource pack in `.minecraft/resourcepacks/`
2. Enable it in-game (Options → Resource Packs)
3. Press **F3 + T** to reload resource packs
4. Your new colors appear instantly!

---

## File Structure

### Complete Resource Pack Layout
```
resourcepacks/
  your_pack_name/
    pack.mcmeta                     ← Required: pack metadata
    pack.png                        ← Optional: pack icon
    assets/
      dynamic_resource_bars/
        textures/
          gui/
            # === BAR TEXTURES ===
            health_bar.png          (256x1024 - 32 frames of 32px each)
            health_bar.png.mcmeta   (animation settings)
            health_bar_poisoned.png (status variant)
            health_bar_withered.png
            health_bar_frozen.png
            health_bar_scorched.png
            
            stamina_bar.png         (256x1024 - 32 frames)
            stamina_bar.png.mcmeta
            stamina_bar_warning.png
            stamina_bar_critical.png
            stamina_bar_hunger.png
            stamina_bar_mounted.png
            stamina_bar_blood.png
            
            mana_bar.png            (256x1024 - 32 frames)
            mana_bar.png.mcmeta
            
            air_bar.png             (256x1024 - 32 frames)
            air_bar.png.mcmeta
            
            # === OPTIONAL: MASKS (for shaped bars) ===
            health_bar_mask.png     (256x32 - ONE frame size)
            stamina_bar_mask.png
            mana_bar_mask.png
            air_bar_mask.png
            
            # === BACKGROUNDS ===
            health_background.png   (256x256 - static)
            stamina_background.png
            mana_background.png
            air_background.png
            
            # === FOREGROUNDS ===
            health_foreground.png   (256x256 - static overlay)
            stamina_foreground.png
            mana_foreground.png
            
            # === STATUS OVERLAYS ===
            absorption_overlay.png  (256x256)
            regeneration_overlay.png
            protection_overlay.png
            heat_overlay.png
            cold_overlay.png
            wetness_overlay.png
            hardcore_overlay.png
            comfort_overlay.png
            nourishment_overlay.png
```

---

## Texture Requirements

### Texture Dimensions

✨ **Texture sizes are auto-detected!** The mod reads your PNG dimensions automatically.

**Recommended dimensions** (standard, but not required):

| Texture Type | Standard Size | Format | Notes |
|--------------|---------------|--------|-------|
| Bar (animated) | **256 x 1024** | PNG + Alpha | 32 frames of 32px each (standard) |
| Bar (custom) | **Any width x (height × frames)** | PNG + Alpha | Width auto-detected, height ÷ frame height must be whole number |
| Mask | **Texture width x frame height** | PNG + Alpha | MUST match bar texture's frame dimensions |
| Background/Overlay | **256 x 256** | PNG + Alpha | Static (non-animated), standard size |

### How Auto-Detection Works

1. **Texture width**: Automatically detected from your PNG file
   - Standard is 256px, but you can use 512px, 128px, or any size!
   - UV calculations automatically adapt

2. **Texture height**: Automatically detected from your PNG file
   - Must be evenly divisible by frame height from `.mcmeta`
   - Example: 2048px texture ÷ 64px frame = 32 frames

3. **Mask dimensions**: Must match **width x frame_height** of the bar texture
   - Validated on load with helpful warnings
   - Example: 256x32 mask for 256x1024 bar with 32px frames
   - Example: 512x64 mask for 512x2048 bar with 64px frames

### Rendered Size vs Texture Size

**Important distinction**:
- **Texture size**: Dimensions of your PNG file (e.g., 256x1024)
- **Rendered size**: On-screen pixels (e.g., 74x4) - controlled by config/editor
- The mod automatically scales textures to fit rendered size!

### Supported Formats
- **PNG** with alpha channel (transparency)
- **8-bit RGBA** recommended for file size
- **No JPG** (doesn't support transparency)

---

## Animation System

All bars support custom animations via `.mcmeta` files (standard Minecraft format).

### Basic .mcmeta Example

**File**: `health_bar.png.mcmeta`
```json
{
  "animation": {
    "frametime": 3,
    "interpolate": false,
    "height": 32
  }
}
```

### Properties

| Property | Default | Description |
|----------|---------|-------------|
| `frametime` | 3 | Ticks per frame (3 = 0.15s at 20 TPS). Lower = faster. |
| `interpolate` | false | Smooth blending between frames. Usually false for pixel art. |
| `height` | 32 | Height of each frame in pixels. Total frames = texture height ÷ this. |

### Animation Examples

**Fast animation (2 ticks per frame)**:
```json
{
  "animation": {
    "frametime": 2,
    "height": 32
  }
}
```

**Slow animation (10 ticks per frame)**:
```json
{
  "animation": {
    "frametime": 10,
    "height": 32
  }
}
```

**Large frames (64px, 16 total frames)**:
```json
{
  "animation": {
    "frametime": 3,
    "height": 64
  }
}
```
*Note: Texture sheet must be 256 x 1024 (64 × 16 = 1024)*

**Static texture (no animation)**:
```json
{
  "animation": {
    "frametime": 999999,
    "height": 256
  }
}
```
*Or omit .mcmeta entirely and make texture 256 x 32*

### Variant Textures Share Settings

All variants of a bar type share animation settings from the base `.mcmeta`:

- `health_bar_poisoned.png`, `health_bar_withered.png`, etc. → use `health_bar.png.mcmeta`
- `stamina_bar_warning.png`, `stamina_bar_critical.png`, etc. → use `stamina_bar.png.mcmeta`

**This means**: All variants must use the same frame height and dimensions!

### Texture Size Flexibility

✨ **NEW**: The mod auto-detects your texture dimensions!

You can now use:
- **Standard**: 256 x 1024 (32 frames of 32px)
- **High-res**: 512 x 2048 (32 frames of 64px)
- **Compact**: 128 x 512 (16 frames of 32px)
- **Any size**: As long as height ÷ frame_height is a whole number

The mod reads your PNG dimensions and adapts UV calculations automatically.

### Fallback Behavior

If no `.mcmeta` file is found:
- Uses default: 32 frames, 32px per frame, 3 ticks per frame
- Texture dimensions: Auto-detected from PNG (defaults to 256x1024 if missing)

---

## Scaling System (Nine-Slice & Tiling)

Control how textures scale to fit different sizes using `.mcmeta` files!

### Scaling Modes

**Four modes available**:

1. **None**: No scaling (1:1 UV sampling, pixel-perfect)
2. **Stretch**: Simple scaling to fit
3. **Tile**: Repeat/pattern the texture
4. **Nine-slice**: Scale with borders (stretch or tile edges/center)

### Mode 1: No Scaling (1:1 Sampling)

```json
{
  "dynamic_resource_bars": {
    "scaling": "none"
  }
}
```
- Samples texture at 1:1 ratio (no transforms)
- Samples exactly `width×height` pixels from texture
- Best for: Icons, pixel-perfect overlays
- **Original behavior** before scaling was added

### Mode 2: Simple Stretch

```json
{
  "dynamic_resource_bars": {
    "scaling": "stretch"
  }
}
```
- Scales texture to fit render area
- Best for: Solid colors, gradients, simple overlays

### Mode 3: Simple Tile

```json
{
  "dynamic_resource_bars": {
    "scaling": "tile"
  }
}
```
- Repeats texture pattern
- Best for: Repeating patterns, textures

### Mode 4: Nine-Slice Scaling

```json
{
  "dynamic_resource_bars": {
    "scaling": {
      "type": "nine_slice",
      "left": 4,
      "right": 4,
      "top": 3,
      "bottom": 3,
      "edges": "stretch",
      "center": "stretch"
    }
  }
}
```

**Nine regions**:
```
┌──┬─────┬──┐
│TL│ Top │TR│  Corners = fixed size
├──┼─────┼──┤
│L │  C  │R │  Edges = stretch or tile
├──┼─────┼──┤  Center = stretch or tile
│BL│ Bot │BR│
└──┴─────┴──┘
```

- **`left/right/top/bottom`**: Border widths (pixels, fixed size)
- **`edges`**: "stretch" or "tile" (optional, default: stretch)
- **`center`**: "stretch" or "tile" (optional, default: stretch)
- **`source`**: Region to use (optional, defaults to full texture)
  - `u`, `v`: Offset in texture (default: 0, 0)
  - `width`, `height`: Size of region to use

**Use cases**:

| Use Case | Edges | Center | Example |
|----------|-------|--------|---------|
| **Smooth background** | stretch | stretch | Gradient borders |
| **Decorative frame** | tile | stretch | Ornate borders with pattern |
| **Textured overlay** | tile | tile | Heat/cold effects |

### Source Region (For Compact Textures)

**Important**: If your actual graphic doesn't fill the entire texture sheet, you **must** specify the `source` region!

#### When You Need Source Regions

If your texture file is 256×256 but your actual graphic is only 80×10 in the corner, the mod will try to scale/tile the full 256×256 (including empty space). Use `source` to tell it where the real graphic is:

```json
{
  "dynamic_resource_bars": {
    "scaling": {
      "type": "nine_slice",
      "left": 2,
      "right": 2,
      "top": 2,
      "bottom": 2,
      "edges": "stretch",
      "center": "stretch",
      "source": {
        "u": 0,
        "v": 0,
        "width": 80,
        "height": 10
      }
    }
  }
}
```

**Field meanings**:
- **`u`**: X offset in texture sheet (default: 0 = left edge)
- **`v`**: Y offset in texture sheet (default: 0 = top edge)
- **`width`**: Width of actual graphic
- **`height`**: Height of actual graphic

**Default placement**: If `u` and `v` are omitted, defaults to `(0, 0)` = **top-left corner**

**Examples**:
```json
// Graphic in top-left (most common)
"source": { "width": 80, "height": 10 }  // u:0, v:0 implied

// Graphic at offset position
"source": { "u": 20, "v": 30, "width": 80, "height": 10 }

// Full sheet (no source needed)
// Omit "source" entirely if graphic fills entire sheet
```

**When to use**:
- ✅ Small textures in large sheets (most bundled overlays are 80×10 in 256×256 sheets)
- ✅ Avoiding texture sheet resizing (which can cause UV issues)
- ✅ Keeping original file dimensions for compatibility

**When NOT needed**:
- ❌ Your graphic fills the entire texture sheet (e.g., full 256×256)

### Smart Defaults (No .mcmeta Needed!)

If you don't specify scaling, the mod applies smart defaults:

| Texture Type | Default Scaling |
|--------------|-----------------|
| **Backgrounds** | nine-slice (4,4,3,3) with stretch |
| **Foregrounds** | nine-slice (4,4,3,3) with stretch |
| **Animated overlays** | nine-slice (3,3,2,2) with tile |
| **Static overlays** | simple stretch |

### Important: Bars Don't Support Scaling

⚠️ **Bar textures** (health_bar.png, mana_bar.png, etc.) **do not support nine-slice or tiling**.

**Why?** Bars use:
- **UV sampling** to show fill percentage (not scaling)
- **Mask layers** which require fixed texture dimensions
- Applying nine-slice would require rendering 9 masked regions per frame (expensive!)

**What this means**:
- ✅ Backgrounds, foregrounds, overlays → Can use nine-slice/tiling
- ❌ Bar animated textures → Use mask layers for shapes instead
- ✅ Bar dimensions are capped at texture sheet width (auto-detected)

---

## Mask System (Shaped Bars)

Create **non-rectangular bars** (rounded corners, pill shapes, custom silhouettes) using mask textures!

### How Masks Work

1. Mask texture defines the **shape** using alpha channel
2. White pixels (255,255,255,255) = bar is **visible**
3. Transparent pixels (0,0,0,0) = bar is **invisible**
4. Gray pixels = **partially transparent**
5. Applied via OpenGL stencil buffer (no texture editing needed!)

### Creating a Mask

#### Step 1: Create Mask Texture

**Dimensions**: **bar_texture_width x frame_height** (matches ONE animation frame)

**Standard dimensions**: **256 x 32 pixels** (for standard 256x1024 bars with 32px frames)

**Visual example** (rounded corners):
```
Transparent → □□■■■■■■■■■■□□
              □■■■■■■■■■■■■□
White →       ■■■■■■■■■■■■■■  ← Visible bar area
              □■■■■■■■■■■■■□
Transparent → □□■■■■■■■■■■□□
```

**In an image editor**:
1. Check your bar texture dimensions (e.g., 256x1024)
2. Create mask with: width = bar width, height = frame height from .mcmeta
3. Paint white on transparent background where bar should show
4. Save as PNG with alpha channel

**File naming**: `{bar_name}_mask.png`
- ✅ `health_bar_mask.png`
- ✅ `mana_bar_mask.png`
- ❌ `masks/health_bar_mask.png` (no subdirectory)

#### Step 2: Reference in .mcmeta

**File**: `health_bar.png.mcmeta`
```json
{
  "animation": {
    "frametime": 3,
    "height": 32
  },
  "dynamic_resource_bars": {
    "mask": {
      "texture": "dynamic_resource_bars:textures/gui/health_bar_mask.png",
      "enabled": true
    }
  }
}
```

### Mask Dimension Validation

The mod **automatically validates** mask dimensions on resource pack load:

**Warning example**:
```
[WARN] MASK DIMENSION MISMATCH for health_bar.png: Mask is 128x16 but animation frame is 256x32.
       Mask should match the size of a single animation frame for best results.
```

**If you see this**:
- Check mask dimensions match your bar's texture width × frame height
- Standard: **256 x 32** for 256-wide bars with 32px frames
- HD: **512 x 64** for 512-wide bars with 64px frames
- Verify mask matches **frame size**, not **rendered size**

**Quick fix**:
1. Check your bar texture size (e.g., open in image editor)
2. Check frame height in `.mcmeta` (e.g., 32)
3. Resize mask to: bar_width × frame_height
4. Reload with F3+T

### Common Mask Shapes

| Shape | Description | Use Case |
|-------|-------------|----------|
| **Rounded corners** | 2-4px radius corners | Smooth, modern look |
| **Pill shape** | Fully rounded ends | Sleek, minimal design |
| **Hexagon** | Angled corners (120°) | Sci-fi or geometric theme |
| **Star/Heart** | Custom decorative | Fantasy or themed packs |
| **Segmented** | Gaps between sections | Discrete "chunk" bars |

### Tips for Mask Creation

✅ **DO**:
- Use pure white (255,255,255,255) for visible areas
- Use pure transparent (0,0,0,0) for cutouts
- Test in-game with F3+T to reload
- Start simple (rounded corners) before complex shapes

❌ **DON'T**:
- Use colored pixels (RGB is ignored, only alpha matters)
- Make mask different size than bar_width x frame_height
- Put masks in subdirectories
- Forget to enable in .mcmeta
- Confuse texture size with rendered size

---

## Combining Features: Practical Examples

### Example 1: HD Bars with Rounded Corners

**Goal**: 2x resolution bars with smooth rounded edges

**Files needed**:
```
health_bar.png (512x2048)
health_bar.png.mcmeta
health_bar_mask.png (512x64)
```

**health_bar.png.mcmeta**:
```json
{
  "animation": {
    "frametime": 3,
    "height": 64
  },
  "dynamic_resource_bars": {
    "mask": {
      "texture": "dynamic_resource_bars:textures/gui/health_bar_mask.png",
      "enabled": true
    }
  }
}
```

**Steps**:
1. Create 512x2048 bar texture (double resolution)
2. Create 512x64 mask with rounded corners
3. Add `.mcmeta` with height: 64 and mask reference
4. Reload and enjoy HD rounded bars!

**Why it works**:
- Auto-detection reads 512x2048 dimensions
- Mask validation checks 512x64 matches frame size
- Mask stencil creates rounded shape
- Everything scales automatically!

---

### Example 2: Decorative Frame with Patterned Border

**Goal**: Background with ornate border that preserves pattern

**Files needed**:
```
health_background.png (256x256)
health_background.png.mcmeta
```

**health_background.png.mcmeta**:
```json
{
  "dynamic_resource_bars": {
    "scaling": {
      "type": "nine_slice",
      "left": 8,
      "right": 8,
      "top": 6,
      "bottom": 6,
      "edges": "tile",
      "center": "stretch"
    }
  }
}
```

**Design tips**:
- Put decorative pattern in 8px left/right borders
- Put decorative pattern in 6px top/bottom borders
- Center can be solid color (will stretch)
- Corners (8x6) stay fixed

**Result**: Border pattern repeats perfectly, no matter the size!

---

### Example 3: Fire Overlay with Texture Pattern

**Goal**: Heat overlay that preserves fire texture pattern

**Files needed**:
```
heat_overlay.png (64x64)
heat_overlay.png.mcmeta
```

**heat_overlay.png.mcmeta**:
```json
{
  "dynamic_resource_bars": {
    "scaling": "tile"
  }
}
```

**Design tip**: Create seamless tileable texture (edges wrap)

**Result**: Fire pattern tiles across bar width without stretching!

---

### Example 4: Complete Themed Pack

**Goal**: Full visual overhaul with custom everything

**Files structure**:
```
fantasy_pack/
  pack.mcmeta
  assets/dynamic_resource_bars/textures/gui/
    # HD bars (512x2048)
    health_bar.png
    health_bar.png.mcmeta (animation + mask)
    health_bar_mask.png (512x64, heart shape)
    
    # Decorative backgrounds (256x256)
    health_background.png
    health_background.png.mcmeta (nine-slice tile)
    
    # Overlay foregrounds (256x256)
    health_foreground.png
    health_foreground.png.mcmeta (nine-slice stretch)
    
    # Pattern overlays (64x64)
    heat_overlay.png
    heat_overlay.png.mcmeta (tile)
    cold_overlay.png
    cold_overlay.png.mcmeta (tile)
```

**Features used**:
- ✅ HD textures (512x2048 bars)
- ✅ Shaped bars (heart mask)
- ✅ Decorative borders (nine-slice tile)
- ✅ Patterned overlays (simple tile)

**Result**: Professional-grade themed resource pack!

---

## Layer System

Bars are rendered in multiple layers for visual depth and status effects.

### Layer Order (bottom to top)

1. **Background** (`health_background.png`) - Behind everything
2. **Bar** (`health_bar.png`) - Main animated bar (with optional mask)
3. **Status Overlays** - Applied over bar (absorption, regen, heat, etc.)
4. **Foreground** (`health_foreground.png`) - Frame/border on top

### Customizing Layers

All layers are optional! Replace only what you want:

**Minimal pack** (just recolor bars):
```
assets/dynamic_resource_bars/textures/gui/
  ├── health_bar.png  ← Only this! (256x1024 standard size)
```

**High-res pack** (2x resolution):
```
assets/dynamic_resource_bars/textures/gui/
  ├── health_bar.png        ← 512x2048 (double resolution)
  ├── health_bar.png.mcmeta ← height: 64 (double frame size)
  ├── health_bar_mask.png   ← 512x64 (matches new dimensions)
```

**Full pack** (complete visual overhaul):
```
assets/dynamic_resource_bars/textures/gui/
  ├── health_bar.png           ← Animated bar (any size!)
  ├── health_bar_mask.png      ← Custom shape (matches bar dimensions)
  ├── health_background.png    ← Frame/container
  ├── health_foreground.png    ← Border overlay
  ├── absorption_overlay.png   ← Status effects
  └── regeneration_overlay.png
```

### Layer Dimensions

| Layer | Size | Alpha Channel |
|-------|------|---------------|
| Background | 256 x 256 | Required |
| Foreground | 256 x 256 | Required |
| Overlays | 256 x 256 | Required (for transparency) |

### Toggling Layers In-Game

Users can toggle layers via config:
- `enableHealthBackground` (true/false)
- `enableHealthForeground` (true/false)
- `enableStaminaBackground` (true/false)
- etc.

**Resource pack tip**: Design layers to work independently so users can mix and match!

---

## In-Game HUD Editor

Dynamic Resource Bars includes a powerful in-game editor for positioning and customizing bars.

### Opening the Editor

**In-game**: Open via mod config menu
- Mods button → Dynamic Resource Bars → Config → "Open HUD Editor" button

### Editor Features

#### 1. **Drag & Drop Positioning**
- Click and drag any bar to reposition
- Real-time preview as you drag
- Snaps to pixel grid

#### 2. **Keyboard Controls**
- **Tab**: Cycle through bars
- **Arrow keys**: Move selected bar 1px at a time
- **Shift + Arrow keys**: Resize selected bar 1px at a time
- **Green outline**: Shows currently selected bar

#### 3. **Sub-Elements**
Each bar has draggable sub-elements:
- **Background** (yellow outline when focused)
- **Bar** (green outline when focused)
- **Foreground** (magenta outline when focused)
- **Text** (cyan outline when focused)

#### 4. **Anchor Points**
Bars can anchor to screen regions:
- Top-left, Top-center, Top-right
- Middle-left, Middle-center, Middle-right
- Bottom-left, Bottom-center, Bottom-right

**Why anchors matter**: Bars stay in relative position when window resizes!

#### 5. **Fill Direction**
Toggle bar fill direction:
- **Horizontal**: Left-to-right (or right-to-left)
- **Vertical**: Bottom-to-top

### Editor Tips

✅ **Best practices**:
- Start with anchor point, then fine-tune with offsets
- Use keyboard for precise 1px adjustments
- Test at different window sizes
- Save often (editor auto-saves to config)

---

## Feature Compatibility Matrix

What works with what:

| Feature | Bars | Backgrounds | Foregrounds | Overlays |
|---------|:----:|:-----------:|:-----------:|:--------:|
| **Custom animations** | ✅ | ❌ | ❌ | ❌ |
| **Auto-detected size** | ✅ | ✅ | ✅ | ✅ |
| **Mask layers** | ✅ | ❌ | ❌ | ❌ |
| **Nine-slice scaling** | ❌ | ✅ | ✅ | ✅ |
| **Tile mode** | ❌ | ✅ | ✅ | ✅ |

**Key insights**:
- **Bars** = Animation + Masks (UV sampling for fill)
- **Backgrounds/Foregrounds/Overlays** = Nine-slice or Tile (full texture rendering)
- **Bars don't scale** because they use UV sampling to show fill percentage
- **Masks need fixed sizes** which is why bars can't use nine-slice

---

## Quick Reference: .mcmeta Syntax

### For Bars (Animated)
```json
{
  "animation": {
    "frametime": 3,
    "height": 32
  },
  "dynamic_resource_bars": {
    "mask": {
      "texture": "dynamic_resource_bars:textures/gui/health_bar_mask.png",
      "enabled": true
    }
  }
}
```

### For No Scaling (1:1 Sampling)
```json
{
  "dynamic_resource_bars": {
    "scaling": "none",
    "source": {
      "width": 80,
      "height": 10
    }
  }
}
```

### For Backgrounds (Nine-Slice Stretch)
```json
{
  "dynamic_resource_bars": {
    "scaling": {
      "type": "nine_slice",
      "left": 4,
      "right": 4,
      "top": 3,
      "bottom": 3,
      "edges": "stretch",
      "center": "stretch"
    }
  }
}
```

### For Small Texture in Large Sheet (Source Region)
```json
{
  "dynamic_resource_bars": {
    "scaling": {
      "type": "nine_slice",
      "left": 2,
      "right": 2,
      "top": 2,
      "bottom": 2,
      "source": {
        "width": 80,
        "height": 10
      }
    }
  }
}
```

### For Overlays (Nine-Slice Tile)
```json
{
  "dynamic_resource_bars": {
    "scaling": {
      "type": "nine_slice",
      "left": 3, "right": 3, "top": 2, "bottom": 2,
      "edges": "tile",
      "center": "tile"
    }
  }
}
```

### For Simple Tile
```json
{
  "dynamic_resource_bars": {
    "scaling": "tile"
  }
}
```

### No .mcmeta Needed!
Smart defaults are applied automatically based on texture type. Only add `.mcmeta` if you want to customize!

---

## Troubleshooting

### Common Issues

#### "My bars aren't showing the custom textures!"

**Checklist**:
1. Resource pack is enabled? (Options → Resource Packs)
2. Pack is above vanilla in the list? (higher priority)
3. File paths match exactly? (`dynamic_resource_bars/textures/gui/`)
4. Files are named correctly? (`health_bar.png`, not `healthbar.png`)
5. Reloaded resource packs? (F3 + T)

#### "Animation is too fast/slow!"

Edit `.mcmeta` file:
- **Too fast**: Increase `frametime` (try 5-10)
- **Too slow**: Decrease `frametime` (try 1-2)
- Reload with F3 + T

#### "Mask dimension mismatch warning!"

Your mask size doesn't match the animation frame size:
- **Check mask**: Should be 256 x 32 (or 256 x your `animation.height`)
- **Check .mcmeta**: `animation.height` should match mask height
- **Not rendered size**: Mask uses texture sheet size, not config size

#### "Bar looks stretched/wrong!"

Check these requirements:
- **Height must be divisible**: texture_height ÷ frame_height must be whole number
- **Mask must match bar**: mask dimensions = bar_width × frame_height
- **Frame height in .mcmeta**: Must match your actual frame size
- **Rendered size**: Config values are separate from texture size

#### "Colors are wrong/washed out!"

Check image editor export settings:
- Use **PNG-24** or **PNG-32** (with alpha)
- Ensure **sRGB color space**
- Disable any "web optimization" that reduces colors

#### "Nine-slice borders look wrong!"

Check your border dimensions:
- Borders must be smaller than texture size
- Example: 8px borders need at least 16px texture (8+8)
- Use smaller borders for small textures
- Test: Temporarily set edges/center to "stretch" to verify dimensions

#### "Nine-slice includes empty space from my texture sheet!"

Your texture is smaller than the full sheet (e.g., 80x10 in 256x256 sheet).

**Solution**: Use `source` to specify the region:
```json
{
  "dynamic_resource_bars": {
    "scaling": {
      "type": "nine_slice",
      "left": 2, "right": 2, "top": 2, "bottom": 2,
      "source": {
        "width": 80,
        "height": 10
      }
    }
  }
}
```

This tells the mod to only use the 80x10 region (starting at 0,0) instead of the full 256x256.

#### "Tiled overlay has visible seams!"

Your texture isn't seamless:
- Edges of texture must wrap perfectly
- Use "offset" in image editor to test wraparound
- Or use nine-slice with stretch instead of tile
- Many image editors have "make seamless" filters

#### "HD bars work but config won't let me set full width!"

Known limitation (tracked for future fix):
- Config sliders assume 256px texture width
- Workaround: Edit config file manually
  - `.minecraft/config/dynamic_resource_bars-client.toml`
  - Set `healthBarWidth = 512` (or your texture width)
- Editor drag limits also assume 256px
- Future update will auto-detect max values from texture

### Getting Help

1. **Check logs**: `.minecraft/logs/latest.log` for error messages
2. **Validation warnings**: Mod logs dimension mismatches and missing textures
3. **Test with default**: Disable your pack to verify mod works
4. **Share pack structure**: Post your file tree when asking for help

---

## Advanced Topics

### See Also
- **`EXAMPLE_with_mask.mcmeta`** - Complete working example in mod assets
- **Mod config**: `.minecraft/config/dynamic_resource_bars-client.toml`
- **In-game editor**: Access via config menu to customize positioning

### Recently Implemented Features
- ✅ **Texture auto-detection** - Use any PNG size
- ✅ **Mask layers** - Shaped bars with stencil buffers
- ✅ **Nine-slice scaling** - Tile/stretch modes for backgrounds and overlays

---

## Examples Gallery

### Minimal Recolor Pack (Standard Resolution)
```
my_red_bars/
  pack.mcmeta
  assets/dynamic_resource_bars/textures/gui/
    health_bar.png     ← Red tinted (256x1024)
    stamina_bar.png    ← Red tinted (256x1024)
    mana_bar.png       ← Red tinted (256x1024)
```

### High-Resolution Pack (2x Resolution)
```
hd_bars/
  pack.mcmeta
  assets/dynamic_resource_bars/textures/gui/
    health_bar.png            ← 512x2048 (double resolution)
    health_bar.png.mcmeta     ← height: 64 (double frame size)
    health_bar_mask.png       ← 512x64 (matches new dimensions)
    stamina_bar.png           ← 512x2048
    stamina_bar.png.mcmeta    ← height: 64
    mana_bar.png              ← 512x2048
    mana_bar.png.mcmeta       ← height: 64
```

### Rounded Bars Pack (With Masks)
```
rounded_bars/
  pack.mcmeta
  assets/dynamic_resource_bars/textures/gui/
    health_bar_mask.png       ← 256x32 rounded corners
    health_bar.png.mcmeta     ← References mask
    stamina_bar_mask.png      ← 256x32 rounded corners
    stamina_bar.png.mcmeta    ← References mask
```

### Decorative Frames Pack (Nine-Slice)
```
ornate_bars/
  pack.mcmeta
  assets/dynamic_resource_bars/textures/gui/
    health_background.png             ← 256x256 with decorative border
    health_background.png.mcmeta      ← Nine-slice with tile edges
    health_foreground.png             ← 256x256 gold frame
    health_foreground.png.mcmeta      ← Nine-slice with stretch
```

**health_background.png.mcmeta**:
```json
{
  "dynamic_resource_bars": {
    "scaling": {
      "type": "nine_slice",
      "left": 8,
      "right": 8,
      "top": 6,
      "bottom": 6,
      "edges": "tile",
      "center": "stretch"
    }
  }
}
```

### Complete Overhaul Pack
```
fantasy_bars/
  pack.mcmeta
  assets/dynamic_resource_bars/textures/gui/
    health_bar.png              ← Custom animated texture (any size)
    health_bar.png.mcmeta       ← Custom timing & mask reference
    health_bar_mask.png         ← Heart shape (matches bar dimensions)
    health_background.png       ← Ornate frame
    health_background.png.mcmeta ← Nine-slice for scaling
    health_foreground.png       ← Gold border
    health_foreground.png.mcmeta ← Nine-slice for scaling
    absorption_overlay.png      ← Custom effect
    # ... and all other bars
```

---

## Cheat Sheet: File Naming & Dimensions

### Standard Resource Pack Files

| File | Dimensions | Required? | Notes |
|------|------------|-----------|-------|
| `health_bar.png` | 256×1024 | ✅ Yes | Animated bar |
| `health_bar.png.mcmeta` | N/A | ⚠️ Optional | Animation + mask |
| `health_bar_mask.png` | 256×32 | ⚠️ Optional | For shaped bars |
| `health_background.png` | 256×256 | ⚠️ Optional | Frame/container |
| `health_background.png.mcmeta` | N/A | ⚠️ Optional | Nine-slice |
| `health_foreground.png` | 256×256 | ⚠️ Optional | Border overlay |
| `health_foreground.png.mcmeta` | N/A | ⚠️ Optional | Nine-slice |

**Pattern**: `{bar_type}_{layer}.png` where:
- `bar_type` = health, mana, stamina, air
- `layer` = bar, background, foreground, mask

### HD Resource Pack Files (2x)

| File | Dimensions | Notes |
|------|------------|-------|
| `health_bar.png` | 512×2048 | Double resolution |
| `health_bar.png.mcmeta` | N/A | Must specify height: 64 |
| `health_bar_mask.png` | 512×64 | Match frame size! |
| `health_background.png` | 512×512 | Can be any size |

### Dimension Formula Reference

```
Bar texture height = frame_height × frame_count
Mask dimensions = bar_texture_width × frame_height
Background/overlay = Any size (will scale with nine-slice)

Standard: 256 × 1024 = 32 × 32 frames
HD 2x:    512 × 2048 = 64 × 32 frames
Compact:  128 × 512  = 32 × 16 frames
```

---

## Credits & Links

- **Mod by**: muon
- **CurseForge**: https://www.curseforge.com/minecraft/mc-mods/dynamic-rpg-resource-bars
- **Modrinth**: https://modrinth.com/mod/dynamic-resource-bars
- **Report Issues**: [GitHub Issues](https://github.com/muon-rw/dynamic-resource-bars/issues)

---

**Happy bar creating!** 🎨

**Pro tip**: Check `assets/dynamic_resource_bars/textures/gui/` in the mod for `EXAMPLE_*.mcmeta` files showing all features!
