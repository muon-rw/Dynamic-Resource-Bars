package dev.muon.dynamic_resource_bars.foundation.config;

import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import toni.lib.config.ConfigBase;

public class CClient extends ConfigBase {

    public final ConfigGroup general = group(0, "general", "Customize shared settings");
    public final ConfigFloat textScalingFactor = f(0.5f, 0.0f,"textScalingFactor", "The amount to adjust the size of text rendered on resource bars");

    public final ConfigGroup health = group(0, "health", "Customize hearts / the health bar");
    public final ConfigBool enableHealthBar = b(true, "enableHealthBar", "Whether to render a custom bar instead of hearts.");
    public final ConfigEnum<HUDPositioning.BarPlacement> healthBarAnchor = e(HUDPositioning.BarPlacement.HEALTH, "healthBarAnchor", "Anchor point for the health bar.");
    public final ConfigInt healthBackgroundWidth = i(80, 0, "healthBackgroundWidth", "Width of the health bar's background sprite, in pixels.");
    public final ConfigInt healthBackgroundHeight = i(10, 0, "healthBackgroundHeight", "Height of the health bar's background sprite, in pixels.");
    public final ConfigInt healthBarWidth = i(74, 0, "healthBarWidth", "Width of the actual animated bar, in pixels.");
    public final ConfigInt healthBarHeight = i(4, 0, "healthBarHeight", "Height of the actual animated bar, in pixels.");
    public final ConfigInt healthBarXOffset = i(3, "healthBarXOffset", "How much to shift the animated bar to the right relative to the background. In other words, the thickness of the background's left border.");
    public final ConfigInt healthBarYOffset = i(3, "healthBarYOffset", "How much to shift the animated bar upward relative to the background. In other words, the thickness of the bottom background's bottom border.");
    public final ConfigInt healthTotalXOffset = i(0, "healthTotalXOffset", "How much to shift the entire bar+background complex to the right");
    public final ConfigInt healthTotalYOffset = i(0, "healthTotalYOffset", "How much to shift the entire bar+background complex upward");
    public final ConfigInt healthBarAnimationCycles = i(33, 0, "healthBarAnimationCycles", "Number of animation frames in the bar animation.");
    public final ConfigInt healthBarFrameHeight = i(6, 0, "healthBarFrameHeight", "Height of each frame in the health bar animation.");
    public final ConfigBool healthDetailOverlay = b(false, "healthDetailOverlay", "Enable extra fancy health overlay layer.");
    public final ConfigInt healthOverlayXOffset = i(0, "healthOverlayXOffset", "How much to shift the fancy detailed overlay to the right.");
    public final ConfigInt healthOverlayYOffset = i(-3, "healthOverlayYOffset", "How much to shift the fancy detailed overlay upward.");


    public final ConfigGroup stamina = group(0, "stamina", "Customize hunger / the stamina bar");
    // TODO: staminaSource config, for mods like Feathers/Paragliders, and create barBehavior ConfigEnum + staminaText implementation
    public final ConfigEnum<HUDPositioning.BarPlacement> staminaBarAnchor = e(HUDPositioning.BarPlacement.HUNGER, "staminaBarAnchor", "Anchor point for the stamina bar.");
    public final ConfigBool enableStaminaBar = b(true, "enableStaminaBar", "Whether to render a custom bar instead of hunger.");
    public final ConfigBool alwaysRenderStamina = b(true, "alwaysRenderStamina", "Whether to always show the stamina bar, instead of hiding it contextually");
    public final ConfigInt staminaBackgroundWidth = i(80, 0, "staminaBackgroundWidth", "Width of the stamina bar's background sprite, in pixels.");
    public final ConfigInt staminaBackgroundHeight = i(10, 0, "staminaBackgroundHeight", "Height of the stamina bar's background sprite, in pixels.");
    public final ConfigInt staminaBarWidth = i(74, 0, "staminaBarWidth", "Width of the actual animated bar, in pixels.");
    public final ConfigInt staminaBarHeight = i(4, 0, "staminaBarHeight", "Height of the actual animated bar, in pixels.");
    public final ConfigInt staminaBarXOffset = i(3, "staminaBarXOffset", "How much to shift the animated bar to the right relative to the background. In other words, the thickness of the background's left border.");
    public final ConfigInt staminaBarYOffset = i(3, "staminaBarYOffset", "How much to shift the animated bar upward relative to the background. In other words, the thickness of the background's bottom border.");
    public final ConfigInt staminaTotalXOffset = i(0, "staminaTotalXOffset", "How much to shift the entire bar+background complex to the right");
    public final ConfigInt staminaTotalYOffset = i(0, "staminaTotalYOffset", "How much to shift the entire bar+background complex upward");
    public final ConfigInt staminaBarAnimationCycles = i(33, 0, "staminaBarAnimationCycles", "Number of animation frames in the bar animation.");
    public final ConfigInt staminaBarFrameHeight = i(6, 0, "staminaBarFrameHeight", "Height of each frame in the stamina bar animation.");
    public final ConfigBool staminaDetailOverlay = b(false, "staminaDetailOverlay", "Enable extra fancy stamina overlay layer.");
    public final ConfigInt staminaOverlayXOffset = i(0, "staminaOverlayXOffset", "How much to shift the fancy detailed overlay to the right.");
    public final ConfigInt staminaOverlayYOffset = i(-3, "staminaOverlayYOffset", "How much to shift the fancy detailed overlay upward.");


    public final ConfigGroup mana = group(0, "mana", "Customize the mana bar");
    public final ConfigEnum<HUDPositioning.BarPlacement> manaBarAnchor = e(HUDPositioning.BarPlacement.ABOVE_UTILITIES, "manaBarAnchor", "Anchor point for the mana bar.");
    public final ConfigBool enableManaBar = b(true, "enableManaBar", "Whether to render a custom bar instead of supported mods' built-in mana bars.");
    public final ConfigBool alwaysRenderMana = b(false, "alwaysRenderMana", "Whether to always show the mana bar, instead of hiding it contextually");
    public final ConfigInt manaBackgroundWidth = i(80, 0, "manaBackgroundWidth", "Width of the mana bar's background sprite, in pixels.");
    public final ConfigInt manaBackgroundHeight = i(10, 0, "manaBackgroundHeight", "Height of the mana bar's background sprite, in pixels.");
    public final ConfigInt manaBarWidth = i(74, 0, "manaBarWidth", "Width of the actual animated bar, in pixels.");
    public final ConfigInt manaBarHeight = i(4, 0, "manaBarHeight", "Height of the actual animated bar, in pixels.");
    public final ConfigInt manaBarXOffset = i(3, "manaBarXOffset", "How much to shift the animated bar to the right relative to the background. In other words, the thickness of the background's left border.");
    public final ConfigInt manaBarYOffset = i(3, "manaBarYOffset", "How much to shift the animated bar upward relative to the background. In other words, the thickness of the background's bottom border.");
    public final ConfigInt manaTotalXOffset = i(0, "manaTotalXOffset", "How much to shift the entire bar+background complex to the right");
    public final ConfigInt manaTotalYOffset = i(0, "manaTotalYOffset", "How much to shift the entire bar+background complex upward");
    public final ConfigInt manaBarAnimationCycles = i(33, 0, "manaBarAnimationCycles", "Number of animation frames in the bar animation.");
    public final ConfigInt manaBarFrameHeight = i(6, 0, "manaBarFrameHeight", "Height of each frame in the mana bar animation.");
    public final ConfigBool manaDetailOverlay = b(true, "manaDetailOverlay", "Enable extra fancy mana overlay layer.");
    public final ConfigInt manaOverlayXOffset = i(0, "manaOverlayXOffset", "How much to shift the fancy detailed overlay to the right.");
    public final ConfigInt manaOverlayYOffset = i(-3, "manaOverlayYOffset", "How much to shift the fancy detailed overlay upward.");


    public final ConfigGroup armor = group(0, "armor", "Customize the armor bar");
    public final ConfigEnum<HUDPositioning.BarPlacement> armorBarAnchor = e(HUDPositioning.BarPlacement.ARMOR, "armorBarAnchor", "Anchor point for the armor bar.");
    public final ConfigBool enableArmorBar = b(true, "enableArmorBar", "Whether to render a custom bar instead of the vanilla armor bar");
    public final ConfigBool hideArmorBar = b(true, "hideArmorBar", "Whether to hide armor entirely, including vanilla armor points. Requires enableArmorBar to be enabled. ");
    public final ConfigInt maxExpectedArmor = i(20, "maxExpectedArmor", "The maximum obtainable armor value for your modpack, for the purpose of how much bar to render + the icon index");
    public final ConfigInt maxExpectedProt = i(16, "maxExpectedProt", "The maximum obtainable Protection value for your modpack, for the purpose of how much overlay to render");
    public final ConfigInt armorBackgroundWidth = i(80, 0, "armorBackgroundWidth", "Width of the armor bar's background sprite, in pixels.");
    public final ConfigInt armorBackgroundHeight = i(10, 0, "armorBackgroundHeight", "Height of the armor bar's background sprite, in pixels.");
    public final ConfigInt armorBarWidth = i(74, 0, "armorBarWidth", "Width of the actual filled bar, in pixels.");
    public final ConfigInt armorBarHeight = i(4, 0, "armorBarHeight", "Height of the actual filled bar, in pixels.");
    public final ConfigInt armorBarXOffset = i(3, "armorBarXOffset", "How much to shift the filled bar to the right relative to the background. In other words, the thickness of the background's left border.");
    public final ConfigInt armorBarYOffset = i(3, "armorBarYOffset", "How much to shift the filled bar upward relative to the background. In other words, the thickness of the background's bottom border.");
    public final ConfigInt armorTotalXOffset = i(0, "armorTotalXOffset", "How much to shift the entire bar+background complex to the right");
    public final ConfigInt armorTotalYOffset = i(0, "armorTotalYOffset", "How much to shift the entire bar+background complex upward");
    public final ConfigBool enableArmorIcon = b(true, "enableArmorIcon", "Whether to render a sprite on the side of the armor bar, based on armor points");
    public final ConfigInt armorIconSize = i(16, 0, "armorIconSize", "Sprite size for the armor overlay icons");
    public final ConfigInt protOverlayAnimationCycles = i(16, 0, "protOverlayAnimationCycles", "Number of animation frames in the protection overlay animation.");
    public final ConfigInt protOverlayFrameHeight = i(4, 0, "protOverlayFrameHeight", "Height of each frame in the protection overlay animation.");
    public final ConfigInt armorIconXOffset = i(0, "armorIconXOffset", "How much to shift the armor icon to the right.");
    public final ConfigInt armorIconYOffset = i(0, "armorIconYOffset", "How much to shift the armor icon upward.");

    public final ConfigGroup air = group(0, "air", "Customize the air bar");
    public final ConfigEnum<HUDPositioning.BarPlacement> airBarAnchor = e(HUDPositioning.BarPlacement.AIR, "airBarAnchor", "Anchor point for the air bar.");
    public final ConfigBool enableAirBar = b(true, "enableAirBar", "Whether to render a custom bar instead of the vanilla air bar");
    public final ConfigBool hideAirBar = b(false, "hideAirBar", "Whether to hide air entirely, including vanilla air points. Requires enableAirBar to be enabled. ");
    public final ConfigInt airBackgroundWidth = i(80, 0, "airBackgroundWidth", "Width of the air bar's background sprite, in pixels.");
    public final ConfigInt airBackgroundHeight = i(10, 0, "airBackgroundHeight", "Height of the air bar's background sprite, in pixels.");
    public final ConfigInt airBarWidth = i(74, 0, "airBarWidth", "Width of the actual filled bar, in pixels.");
    public final ConfigInt airBarHeight = i(4, 0, "airBarHeight", "Height of the actual filled bar, in pixels.");
    public final ConfigInt airBarXOffset = i(3, "airBarXOffset", "How much to shift the filled bar to the right relative to the background. In other words, the thickness of the background's left border.");
    public final ConfigInt airBarYOffset = i(3, "airBarYOffset", "How much to shift the filled bar upward relative to the background. In other words, the thickness of the background's bottom border.");
    public final ConfigInt airTotalXOffset = i(0, "airTotalXOffset", "How much to shift the entire bar+background complex to the right");
    public final ConfigInt airTotalYOffset = i(0, "airTotalYOffset", "How much to shift the entire bar+background complex upward");
    public final ConfigBool enableAirIcon = b(true, "enableAirIcon", "Whether to render a sprite on the side of the air bar, based on air points");
    public final ConfigInt airIconSize = i(16, 0, "airIconSize", "Sprite size for the air overlay icons");
    public final ConfigInt airIconXOffset = i(0, "airIconXOffset", "How much to shift the air icon to the right.");
    public final ConfigInt airIconYOffset = i(0, "airIconYOffset", "How much to shift the air icon upward.");


    @Override
    public String getName() {
        return "client";
    }
}
