## 0.7.0
- Add Stamina Attributes support (soon: Paragliders, Epic Fight)
- Fix crash if Stamina Bar behavior was changed to vanilla while Farmer's Delight was installed (#22)

## 0.6.2
- Ported RPGMana support to 1.21.1 Fabric

## 0.6.1
- Fix Bewitchment being required on 1.20.1 Fabric

## 0.6.0
Mana mod support!
- RPGMana (1.20.1 Fabric)
- Mana Attributes (1.21.1 Fabric) *Forg if you're reading this please port RPGMana to 1.21.1 <3*
- Ars Nouveau (1.20.1 Forge, 1.21.1 Neoforge)
- Iron's Spellbooks (1.20.1 Forge, 1.21.1 Neoforge)
- Fix Appleskin being required on Forge/Neoforge (#19)

## 0.5.1
- Hide air bar for Bewitchment vampires

## 0.5.0
**BREAKING CONFIG CHANGES** (Sorry!) 

*Users will either need to use the "reset all" button and start over, or manually move icons+text to their desired positions*
- Bewitchment compatibility! Added `stamina_bar_blood` for vampires
- Bars will now always render while in editing mode
- Text is now a draggable element
- Text color/size/max opacity can be changed via edit boxes
- Armor/air icons are now draggable elements
- Air and armor background/foregrounds can now be properly moved/resized
- Updated default `protection_overlay`, `absorption_overlay`, `saturation_overlay` textures
- Updated the default textures of the `armor_bar`, `air_bar`
- `air_bar` is now an animated texture
- Fixed the "Reset All" button not resetting everything
- Fixed improper U offset for right-anchored animated bars
- Air can now be toggled separately from stamina on 1.20.1 Fabric
- Fixed Bewitchment magic bar rendering conflict on 1.20.1 Fabric, and likely many other 1.20.1 Fabric HUD altering mods

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