## 0.5.0
- Bars will now always render while in editing mode
- Text is now a draggable element
- Text color/size/max opacity can be changed via edit boxes
- Armor/air icons are now draggable elements
- Air and armor background/foregrounds can now be properly moved/resized
- Updated the default textures of the armor/air bars: `air_bar` is now an animated texture

## 0.4.0
- Add mount health bar. Replaces stamina bar when mounted, but uses health text settings.
- Added `stamina_bar_mounted`
- Improved the default color of `health_bar_scorched`

## 0.3.0
- Fix Health bar partial size only changing by whole-number health values on 1.20.1 Fabric
- Add AppleSkin Compat
- Add Farmer's Delight / Farmer's Delight Refabricated Compat
- Added `regeneration_overlay`
- Added `saturation_overlay` (for AppleSkin Compat)
- Added `comfort_overlay` and `nourishment_overlay` (for Farmer's Delight Compat)
- Changed the `protection_overlay` default sprite (pulse animations of this type are now handled programatically)

## 0.2.1
- Fix the manual element resize edit boxes not properly saving their values
- Make background layers independently moveable 

## 0.2.0
- Split `detail_overlay` layer into 3 sprites: `health_foreground`, `stamina_foreground` `mana_foreground`

## 0.1.0
- Initial release