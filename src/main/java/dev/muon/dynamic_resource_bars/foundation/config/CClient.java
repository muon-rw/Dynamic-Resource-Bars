package dev.muon.dynamic_resource_bars.foundation.config;

import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import toni.lib.config.ConfigBase;

public class CClient extends ConfigBase {

    public final ConfigGroup client = group(0, "client", "Client-only settings - If you're looking for general settings, look inside your world's serverconfig folder!");

    public final ConfigFloat textScalingFactor = f(0.5f, 0.0f,"textScalingFactor", "The amount to adjust the size of text rendered on resource bars");

    // TODO: Option to hide health text
    public final ConfigBool enableHealthBar = b(true, "enableHealthBar", "Whether to render a custom bar instead of hearts.");
    public final ConfigEnum<HUDPositioning.BarPlacement> healthBarPlacement = e(HUDPositioning.BarPlacement.HEALTH, "healthBarPlacement", "Placement of the health bar.");
    public final ConfigInt healthBorderWidth = i(80, 0, "healthBorderWidth", "Width of the health bar's background sprite, in pixels.");
    public final ConfigInt healthBorderHeight = i(10, 0, "healthBorderHeight", "Height of the health bar's background sprite, in pixels.");
    public final ConfigInt healthBarWidth = i(74, 0, "healthBarWidth", "Width of the actual animated bar, in pixels.");
    public final ConfigInt healthBarHeight = i(4, 0, "healthBarHeight", "Height of the actual animated bar, in pixels.");
    public final ConfigInt healthBarXOffset = i(3, "healthBarXOffset", "How much to shift the animated bar to the right, in pixels. In other words, the thickness of the left health border.");
    public final ConfigInt healthBarYOffset = i(3, "healthBarYOffset", "How much to shift the animated bar upward, in pixels. In other words, the thickness of the bottom health border.");
    public final ConfigInt healthBarAnimationCycles = i(33, 0, "healthBarAnimationCycles", "Number of animation frames in the bar animation.");
    public final ConfigInt healthBarFrameHeight = i(6, 0, "healthBarFrameHeight", "Height of each frame in the health bar animation.");
    public final ConfigBool healthDetailOverlay = b(false, "healthDetailOverlay", "Enable extra fancy health overlay layer.");
    public final ConfigInt healthOverlayXOffset = i(0, "healthOverlayXOffset", "How much to shift the fancy detailed overlay to the right.");
    public final ConfigInt healthOverlayYOffset = i(-3, "healthOverlayYOffset", "How much to shift the fancy detailed overlay upward.");

    // TODO: staminaSource config, for mods like Feathers/Paragliders, and create barBehavior ConfigEnum + staminaText implementation
    public final ConfigEnum<HUDPositioning.BarPlacement> staminaBarPlacement = e(HUDPositioning.BarPlacement.HUNGER, "staminaBarPlacement", "Placement of the stamina bar.");
    public final ConfigBool enableStaminaBar = b(true, "enableStaminaBar", "Whether to render a custom bar instead of hunger.");
    public final ConfigInt staminaBorderWidth = i(80, 0, "staminaBorderWidth", "Width of the stamina bar's background sprite, in pixels.");
    public final ConfigInt staminaBorderHeight = i(10, 0, "staminaBorderHeight", "Height of the stamina bar's background sprite, in pixels.");
    public final ConfigInt staminaBarWidth = i(74, 0, "staminaBarWidth", "Width of the actual animated bar, in pixels.");
    public final ConfigInt staminaBarHeight = i(4, 0, "staminaBarHeight", "Height of the actual animated bar, in pixels.");
    public final ConfigInt staminaBarXOffset = i(3, "staminaBarXOffset", "X offset for the stamina bar.");
    public final ConfigInt staminaBarYOffset = i(3, "staminaBarYOffset", "Y offset for the stamina bar.");
    public final ConfigInt staminaBarAnimationCycles = i(33, 0, "staminaBarAnimationCycles", "Number of animation frames in the bar animation.");
    public final ConfigInt staminaBarFrameHeight = i(6, 0, "staminaBarFrameHeight", "Height of each frame in the stamina bar animation.");
    public final ConfigBool staminaDetailOverlay = b(false, "staminaDetailOverlay", "Enable extra fancy stamina overlay layer.");
    public final ConfigInt staminaOverlayXOffset = i(0, "staminaOverlayXOffset", "How much to shift the fancy detailed overlay to the right.");
    public final ConfigInt staminaOverlayYOffset = i(-3, "staminaOverlayYOffset", "How much to shift the fancy detailed overlay upward.");

    // TODO: Use manaText config bool
    public final ConfigEnum<HUDPositioning.BarPlacement> manaBarPlacement = e(HUDPositioning.BarPlacement.ABOVE_UTILITIES, "manaBarPlacement", "Placement of the mana bar.");
    public final ConfigBool enableManaBar = b(true, "enableManaBar", "Whether to render a custom bar instead of supported mods' built-in mana bars.");
    public final ConfigInt manaBorderWidth = i(80, 0, "manaBorderWidth", "Width of the mana bar's background sprite, in pixels.");
    public final ConfigInt manaBorderHeight = i(10, 0, "manaBorderHeight", "Height of the mana bar's background sprite, in pixels.");
    public final ConfigInt manaBarWidth = i(74, 0, "manaBarWidth", "Width of the actual animated bar, in pixels.");
    public final ConfigInt manaBarHeight = i(4, 0, "manaBarHeight", "Height of the actual animated bar, in pixels.");
    public final ConfigInt manaBarXOffset = i(3, "manaBarXOffset", "X offset for the mana bar.");
    public final ConfigInt manaBarYOffset = i(3, "manaBarYOffset", "Y offset for the mana bar.");
    public final ConfigInt manaBarAnimationCycles = i(33, 0, "manaBarAnimationCycles", "Number of animation frames in the bar animation.");
    public final ConfigInt manaBarFrameHeight = i(6, 0, "manaBarFrameHeight", "Height of each frame in the mana bar animation.");
    public final ConfigBool manaDetailOverlay = b(true, "manaDetailOverlay", "Enable extra fancy mana overlay layer.");
    public final ConfigBool manaText = b(true, "manaText", "Enable mana text display.");
    public final ConfigInt manaOverlayXOffset = i(0, "manaOverlayXOffset", "How much to shift the fancy detailed overlay to the right.");
    public final ConfigInt manaOverlayYOffset = i(-3, "manaOverlayYOffset", "How much to shift the fancy detailed overlay upward.");


    @Override
    public String getName() {
        return "client";
    }
}
