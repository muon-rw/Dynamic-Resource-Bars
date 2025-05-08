package dev.muon.dynamic_resource_bars.foundation.config;

import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;
import dev.muon.dynamic_resource_bars.util.TextBehavior;
import toni.lib.config.ConfigBase;

public class CClient extends ConfigBase {

    // --- General Defaults ---
    public static final float DEFAULT_TEXT_SCALING_FACTOR = 0.5f;
    public static final boolean DEFAULT_DISABLE_DEFAULT_ARMOR = true;

    public final ConfigGroup general = group(0, "general", "Customize shared settings");
    public final ConfigFloat textScalingFactor = f(DEFAULT_TEXT_SCALING_FACTOR, 0.0f,"textScalingFactor", "The amount to adjust the size of text rendered on resource bars");
    public final ConfigBool disableDefaultArmor = b(DEFAULT_DISABLE_DEFAULT_ARMOR, "disableDefaultArmor", "Whether to hide the vanilla armor bar from the HUD");

    /**
     * Health
     */
    // --- Health Defaults ---
    public static final boolean DEFAULT_ENABLE_HEALTH_BAR = true;
    public static final HUDPositioning.BarPlacement DEFAULT_HEALTH_BAR_ANCHOR = HUDPositioning.BarPlacement.HEALTH;
    public static final boolean DEFAULT_FADE_HEALTH_WHEN_FULL = false;
    public static final TextBehavior DEFAULT_SHOW_HEALTH_TEXT = TextBehavior.WHEN_NOT_FULL;
    public static final HorizontalAlignment DEFAULT_HEALTH_TEXT_ALIGN = HorizontalAlignment.CENTER;
    public static final boolean DEFAULT_ENABLE_HEALTH_FOREGROUND = false;
    public static final boolean DEFAULT_ENABLE_HEALTH_BACKGROUND = true;
    public static final int DEFAULT_HEALTH_BACKGROUND_WIDTH = 80;
    public static final int DEFAULT_HEALTH_BACKGROUND_HEIGHT = 10;
    public static final int DEFAULT_HEALTH_BAR_WIDTH = 74;
    public static final int DEFAULT_HEALTH_BAR_HEIGHT = 4;
    public static final int DEFAULT_HEALTH_OVERLAY_WIDTH = 80;
    public static final int DEFAULT_HEALTH_OVERLAY_HEIGHT = 10;
    public static final int DEFAULT_HEALTH_BAR_X_OFFSET = 3;
    public static final int DEFAULT_HEALTH_BAR_Y_OFFSET = 3;
    public static final int DEFAULT_HEALTH_TOTAL_X_OFFSET = 0; // Recalculated based on anchor
    public static final int DEFAULT_HEALTH_TOTAL_Y_OFFSET = 0;
    public static final int DEFAULT_HEALTH_OVERLAY_X_OFFSET = 0;
    public static final int DEFAULT_HEALTH_OVERLAY_Y_OFFSET = -3;


    public final ConfigGroup health = group(0, "health", "Customize hearts / the health bar");
    public final ConfigBool enableHealthBar = b(DEFAULT_ENABLE_HEALTH_BAR, "enableHealthBar", "Whether to render a custom bar instead of hearts.");
    public final ConfigEnum<HUDPositioning.BarPlacement> healthBarAnchor = e(DEFAULT_HEALTH_BAR_ANCHOR, "healthBarAnchor", "Anchor point for the health bar.");
    public final ConfigBool fadeHealthWhenFull = b(DEFAULT_FADE_HEALTH_WHEN_FULL, "fadeHealthWhenFull", "Whether to dynamically hide the health bar when the player is at full health");
    public final ConfigEnum<TextBehavior> showHealthText = e(DEFAULT_SHOW_HEALTH_TEXT, "Show Health Text", "When health current/maximum values should be rendered as a text overlay. Always hidden when the bar is invisible, even if set to ALWAYS");
    public final ConfigEnum<HorizontalAlignment> healthTextAlign = e(DEFAULT_HEALTH_TEXT_ALIGN, "Health Text Align", "Horizontal alignment for the health value text.");
    public final ConfigBool enableHealthForeground = b(DEFAULT_ENABLE_HEALTH_FOREGROUND, "Enable Foreground Layer", "Render an extra layer on top of the health bar. Can be toggled in HUD editor.");
    public final ConfigBool enableHealthBackground = b(DEFAULT_ENABLE_HEALTH_BACKGROUND, "Enable Background Layer", "Render an extra layer behind the health bar. Can be toggled in HUD editor.");

    // --- Sizing ---
    public final ConfigInt healthBackgroundWidth = i(DEFAULT_HEALTH_BACKGROUND_WIDTH, 0, "healthBackgroundWidth", "Width of the health bar's background sprite, in pixels.");
    public final ConfigInt healthBackgroundHeight = i(DEFAULT_HEALTH_BACKGROUND_HEIGHT, 0, "healthBackgroundHeight", "Height of the health bar's background sprite, in pixels.");
    public final ConfigInt healthBarWidth = i(DEFAULT_HEALTH_BAR_WIDTH, 0, "healthBarWidth", "Width of the actual animated bar, in pixels.");
    public final ConfigInt healthBarHeight = i(DEFAULT_HEALTH_BAR_HEIGHT, 0, "healthBarHeight", "Height of the actual animated bar, in pixels.");
    public final ConfigInt healthBarAnimationCycles = i(33, 0, "healthBarAnimationCycles", "Number of animation frames in the bar animation.");
    public final ConfigInt healthBarFrameHeight = i(6, 0, "healthBarFrameHeight", "Height of each frame in the health bar animation.");

    // --- New Foreground Sizing ---
    public final ConfigInt healthOverlayWidth = i(DEFAULT_HEALTH_OVERLAY_WIDTH, 0, "healthOverlayWidth", "Width of the health bar's foreground sprite, in pixels.");
    public final ConfigInt healthOverlayHeight = i(DEFAULT_HEALTH_OVERLAY_HEIGHT, 0, "healthOverlayHeight", "Height of the health bar's foreground sprite, in pixels.");

    // --- Positioning ---
    public final ConfigInt healthBarXOffset = i(DEFAULT_HEALTH_BAR_X_OFFSET, "healthBarXOffset", "Bar X offset relative to background. Adjusted via HUD editor focus mode.");
    public final ConfigInt healthBarYOffset = i(DEFAULT_HEALTH_BAR_Y_OFFSET, "healthBarYOffset", "Bar Y offset relative to background. Adjusted via HUD editor focus mode.");
    public final ConfigInt healthTotalXOffset = i(DEFAULT_HEALTH_TOTAL_X_OFFSET, "healthTotalXOffset", "Overall bar complex X offset relative to anchor. Adjusted via HUD editor.");
    public final ConfigInt healthTotalYOffset = i(DEFAULT_HEALTH_TOTAL_Y_OFFSET, "healthTotalYOffset", "Overall bar complex Y offset relative to anchor. Adjusted via HUD editor.");
    public final ConfigInt healthOverlayXOffset = i(DEFAULT_HEALTH_OVERLAY_X_OFFSET, "healthOverlayXOffset", "Foreground overlay X offset. Adjusted via HUD editor focus mode.");
    public final ConfigInt healthOverlayYOffset = i(DEFAULT_HEALTH_OVERLAY_Y_OFFSET, "healthOverlayYOffset", "Foreground overlay Y offset. Adjusted via HUD editor focus mode.");


    /**
     * Stamina
     */
    // --- Stamina Defaults ---
    public static final boolean DEFAULT_ENABLE_STAMINA_BAR = true;
    public static final HUDPositioning.BarPlacement DEFAULT_STAMINA_BAR_ANCHOR = HUDPositioning.BarPlacement.HUNGER;
    public static final boolean DEFAULT_FADE_STAMINA_WHEN_FULL = false;
    public static final TextBehavior DEFAULT_SHOW_STAMINA_TEXT = TextBehavior.NEVER;
    public static final HorizontalAlignment DEFAULT_STAMINA_TEXT_ALIGN = HorizontalAlignment.CENTER;
    public static final boolean DEFAULT_ENABLE_STAMINA_FOREGROUND = false;
    public static final boolean DEFAULT_ENABLE_STAMINA_BACKGROUND = true;
    public static final int DEFAULT_STAMINA_BACKGROUND_WIDTH = 80;
    public static final int DEFAULT_STAMINA_BACKGROUND_HEIGHT = 10;
    public static final int DEFAULT_STAMINA_BAR_WIDTH = 74;
    public static final int DEFAULT_STAMINA_BAR_HEIGHT = 4;
    public static final int DEFAULT_STAMINA_OVERLAY_WIDTH = 80;
    public static final int DEFAULT_STAMINA_OVERLAY_HEIGHT = 10;
    public static final int DEFAULT_STAMINA_OVERLAY_X_OFFSET = 0;
    public static final int DEFAULT_STAMINA_OVERLAY_Y_OFFSET = -3;
    public static final int DEFAULT_STAMINA_BAR_X_OFFSET = 3;
    public static final int DEFAULT_STAMINA_BAR_Y_OFFSET = 3;
    public static final int DEFAULT_STAMINA_TOTAL_X_OFFSET = 0; // Recalculated based on anchor
    public static final int DEFAULT_STAMINA_TOTAL_Y_OFFSET = 0;

    public final ConfigGroup stamina = group(0, "stamina", "Customize hunger / the stamina bar");
    public final ConfigBool enableStaminaBar = b(DEFAULT_ENABLE_STAMINA_BAR, "enableStaminaBar", "Whether to render a custom bar instead of hunger.");
    public final ConfigEnum<HUDPositioning.BarPlacement> staminaBarAnchor = e(DEFAULT_STAMINA_BAR_ANCHOR, "staminaBarAnchor", "Anchor point for the stamina bar.");
    public final ConfigBool fadeStaminaWhenFull = b(DEFAULT_FADE_STAMINA_WHEN_FULL, "fadeStaminaWhenFull", "Whether to dynamically hide the stamina bar when the player is at full hunger/stamina");
    public final ConfigEnum<TextBehavior> showStaminaText = e(DEFAULT_SHOW_STAMINA_TEXT, "showStaminaText", "When mana current/maximum values should be rendered as a text overlay. Always hidden when the bar is invisible, even if set to ALWAYS");
    public final ConfigEnum<HorizontalAlignment> staminaTextAlign = e(DEFAULT_STAMINA_TEXT_ALIGN, "Stamina Text Align", "Horizontal alignment for the stamina value text.");
    public final ConfigBool enableStaminaForeground = b(DEFAULT_ENABLE_STAMINA_FOREGROUND, "Enable Foreground Layer", "Render an extra layer on top of the stamina bar. Can be toggled in HUD editor.");
    public final ConfigBool enableStaminaBackground = b(DEFAULT_ENABLE_STAMINA_BACKGROUND, "Enable Background Layer", "Render an extra layer behind the stamina bar. Can be toggled in HUD editor.");

    // --- Sizing ---
    public final ConfigInt staminaBackgroundWidth = i(DEFAULT_STAMINA_BACKGROUND_WIDTH, 0, "staminaBackgroundWidth", "Width of the stamina bar's background sprite, in pixels.");
    public final ConfigInt staminaBackgroundHeight = i(DEFAULT_STAMINA_BACKGROUND_HEIGHT, 0, "staminaBackgroundHeight", "Height of the stamina bar's background sprite, in pixels.");
    public final ConfigInt staminaBarWidth = i(DEFAULT_STAMINA_BAR_WIDTH, 0, "staminaBarWidth", "Width of the actual animated bar, in pixels.");
    public final ConfigInt staminaBarHeight = i(DEFAULT_STAMINA_BAR_HEIGHT, 0, "staminaBarHeight", "Height of the actual animated bar, in pixels.");
    public final ConfigInt staminaBarAnimationCycles = i(33, 0, "staminaBarAnimationCycles", "Number of animation frames in the bar animation.");
    public final ConfigInt staminaBarFrameHeight = i(6, 0, "staminaBarFrameHeight", "Height of each frame in the stamina bar animation.");

    // --- New Foreground Sizing ---
    public final ConfigInt staminaOverlayWidth = i(DEFAULT_STAMINA_OVERLAY_WIDTH, 0, "staminaOverlayWidth", "Width of the stamina bar's foreground sprite, in pixels.");
    public final ConfigInt staminaOverlayHeight = i(DEFAULT_STAMINA_OVERLAY_HEIGHT, 0, "staminaOverlayHeight", "Height of the stamina bar's foreground sprite, in pixels.");

    // --- Positioning ---
    public final ConfigInt staminaOverlayXOffset = i(DEFAULT_STAMINA_OVERLAY_X_OFFSET, "staminaOverlayXOffset", "Foreground overlay X offset. Adjusted via HUD editor focus mode.");
    public final ConfigInt staminaOverlayYOffset = i(DEFAULT_STAMINA_OVERLAY_Y_OFFSET, "staminaOverlayYOffset", "Foreground overlay Y offset. Adjusted via HUD editor focus mode.");
    public final ConfigInt staminaBarXOffset = i(DEFAULT_STAMINA_BAR_X_OFFSET, "staminaBarXOffset", "Bar X offset relative to background. Adjusted via HUD editor focus mode.");
    public final ConfigInt staminaBarYOffset = i(DEFAULT_STAMINA_BAR_Y_OFFSET, "staminaBarYOffset", "Bar Y offset relative to background. Adjusted via HUD editor focus mode.");
    public final ConfigInt staminaTotalXOffset = i(DEFAULT_STAMINA_TOTAL_X_OFFSET, "staminaTotalXOffset", "Overall bar complex X offset relative to anchor. Adjusted via HUD editor.");
    public final ConfigInt staminaTotalYOffset = i(DEFAULT_STAMINA_TOTAL_Y_OFFSET, "staminaTotalYOffset", "Overall bar complex Y offset relative to anchor. Adjusted via HUD editor.");

    /**
     * Mana
     */
    // --- Mana Defaults ---
    public static final boolean DEFAULT_ENABLE_MANA_BAR = true;
    public static final HUDPositioning.BarPlacement DEFAULT_MANA_BAR_ANCHOR = HUDPositioning.BarPlacement.ABOVE_UTILITIES;
    public static final boolean DEFAULT_FADE_MANA_WHEN_FULL = true;
    public static final TextBehavior DEFAULT_SHOW_MANA_TEXT = TextBehavior.WHEN_NOT_FULL;
    public static final HorizontalAlignment DEFAULT_MANA_TEXT_ALIGN = HorizontalAlignment.CENTER;
    public static final boolean DEFAULT_ENABLE_MANA_FOREGROUND = true;
    public static final boolean DEFAULT_ENABLE_MANA_BACKGROUND = true;
    public static final int DEFAULT_MANA_BACKGROUND_WIDTH = 80;
    public static final int DEFAULT_MANA_BACKGROUND_HEIGHT = 10;
    public static final int DEFAULT_MANA_BAR_WIDTH = 74;
    public static final int DEFAULT_MANA_BAR_HEIGHT = 4;
    public static final int DEFAULT_MANA_OVERLAY_WIDTH = 80;
    public static final int DEFAULT_MANA_OVERLAY_HEIGHT = 10;
    public static final int DEFAULT_MANA_BAR_X_OFFSET = 3;
    public static final int DEFAULT_MANA_BAR_Y_OFFSET = 3;
    public static final int DEFAULT_MANA_TOTAL_X_OFFSET = -40; // Recalculated based on anchor (ABOVE_UTILITIES is centered)
    public static final int DEFAULT_MANA_TOTAL_Y_OFFSET = 0;
    public static final int DEFAULT_MANA_OVERLAY_X_OFFSET = 0;
    public static final int DEFAULT_MANA_OVERLAY_Y_OFFSET = -3;


    public final ConfigGroup mana = group(0, "mana", "Customize the mana bar");
    public final ConfigBool enableManaBar = b(DEFAULT_ENABLE_MANA_BAR, "enableManaBar", "Whether to render a custom bar instead of supported mods' built-in mana bars.");
    public final ConfigEnum<HUDPositioning.BarPlacement> manaBarAnchor = e(DEFAULT_MANA_BAR_ANCHOR, "manaBarAnchor", "Anchor point for the mana bar.");
    public final ConfigBool fadeManaWhenFull = b(DEFAULT_FADE_MANA_WHEN_FULL, "fadeManaWhenFull", "Whether to dynamically hide the mana bar when mana is full. ");
    public final ConfigEnum<TextBehavior> showManaText = e(DEFAULT_SHOW_MANA_TEXT, "showManaText", "When mana current/maximum values should be rendered as a text overlay. Always hidden when the bar is invisible, even if set to ALWAYS");
    public final ConfigEnum<HorizontalAlignment> manaTextAlign = e(DEFAULT_MANA_TEXT_ALIGN, "Mana Text Align", "Horizontal alignment for the mana value text.");
    public final ConfigBool enableManaForeground = b(DEFAULT_ENABLE_MANA_FOREGROUND, "enableManaForeground", "Render an extra layer on top of the resource bar. Can be toggled in HUD editor.");
    public final ConfigBool enableManaBackground = b(DEFAULT_ENABLE_MANA_BACKGROUND, "enableManaBackground", "Render an extra layer behind the resource bar. Can be toggled in HUD editor.");

    // --- Sizing ---
    public final ConfigInt manaBackgroundWidth = i(DEFAULT_MANA_BACKGROUND_WIDTH, 0, "manaBackgroundWidth", "Width of the mana bar's background sprite, in pixels.");
    public final ConfigInt manaBackgroundHeight = i(DEFAULT_MANA_BACKGROUND_HEIGHT, 0, "manaBackgroundHeight", "Height of the mana bar's background sprite, in pixels.");
    public final ConfigInt manaBarWidth = i(DEFAULT_MANA_BAR_WIDTH, 0, "manaBarWidth", "Width of the actual animated bar, in pixels.");
    public final ConfigInt manaBarHeight = i(DEFAULT_MANA_BAR_HEIGHT, 0, "manaBarHeight", "Height of the actual animated bar, in pixels.");
    public final ConfigInt manaBarAnimationCycles = i(33, 0, "manaBarAnimationCycles", "Number of animation frames in the bar animation.");
    public final ConfigInt manaBarFrameHeight = i(6, 0, "manaBarFrameHeight", "Height of each frame in the mana bar animation.");

    // --- New Foreground Sizing ---
    public final ConfigInt manaOverlayWidth = i(DEFAULT_MANA_OVERLAY_WIDTH, 0, "manaOverlayWidth", "Width of the mana bar's foreground sprite, in pixels.");
    public final ConfigInt manaOverlayHeight = i(DEFAULT_MANA_OVERLAY_HEIGHT, 0, "manaOverlayHeight", "Height of the mana bar's foreground sprite, in pixels.");

    // --- Positioning ---
    public final ConfigInt manaBarXOffset = i(DEFAULT_MANA_BAR_X_OFFSET, "manaBarXOffset", "Bar X offset relative to background. Adjusted via HUD editor focus mode.");
    public final ConfigInt manaBarYOffset = i(DEFAULT_MANA_BAR_Y_OFFSET, "manaBarYOffset", "Bar Y offset relative to background. Adjusted via HUD editor focus mode.");
    public final ConfigInt manaTotalXOffset = i(DEFAULT_MANA_TOTAL_X_OFFSET, "manaTotalXOffset", "Overall bar complex X offset relative to anchor. Adjusted via HUD editor.");
    public final ConfigInt manaTotalYOffset = i(DEFAULT_MANA_TOTAL_Y_OFFSET, "manaTotalYOffset", "Overall bar complex Y offset relative to anchor. Adjusted via HUD editor.");
    public final ConfigInt manaOverlayXOffset = i(DEFAULT_MANA_OVERLAY_X_OFFSET, "manaOverlayXOffset", "Foreground overlay X offset. Adjusted via HUD editor focus mode.");
    public final ConfigInt manaOverlayYOffset = i(DEFAULT_MANA_OVERLAY_Y_OFFSET, "manaOverlayYOffset", "Foreground overlay Y offset. Adjusted via HUD editor focus mode.");


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
