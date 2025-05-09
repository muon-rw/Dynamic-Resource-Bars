package dev.muon.dynamic_resource_bars.foundation.config;

import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;
import dev.muon.dynamic_resource_bars.util.TextBehavior;
// Removed: import toni.lib.config.ConfigBase;

// Import necessary (Neo)ForgeConfigSpec classes
#if FABRIC
    #if AFTER_21_1
        import net.neoforged.neoforge.common.ModConfigSpec;
        import net.neoforged.neoforge.common.ModConfigSpec.*;
    #else
        import net.minecraftforge.common.ForgeConfigSpec;
        import net.minecraftforge.common.ForgeConfigSpec.*;
    #endif
#elif FORGE
    import net.minecraftforge.common.ForgeConfigSpec;
    import net.minecraftforge.common.ForgeConfigSpec.*;
#elif NEO
    import net.neoforged.neoforge.common.ModConfigSpec;
    import net.neoforged.neoforge.common.ModConfigSpec.*;
#endif


public class CClient {

    // --- General Defaults ---
    public static final float DEFAULT_TEXT_SCALING_FACTOR = 0.5f;
    public static final boolean DEFAULT_DISABLE_DEFAULT_ARMOR = true;

    // New config value fields
    public final DoubleValue textScalingFactor;
    public final BooleanValue disableDefaultArmor;

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

    public final BooleanValue enableHealthBar;
    public final EnumValue<HUDPositioning.BarPlacement> healthBarAnchor;
    public final BooleanValue fadeHealthWhenFull;
    public final EnumValue<TextBehavior> showHealthText;
    public final EnumValue<HorizontalAlignment> healthTextAlign;
    public final BooleanValue enableHealthForeground;
    public final BooleanValue enableHealthBackground;

    // --- Sizing ---
    public final IntValue healthBackgroundWidth;
    public final IntValue healthBackgroundHeight;
    public final IntValue healthBarWidth;
    public final IntValue healthBarHeight;
    public final IntValue healthBarAnimationCycles;
    public final IntValue healthBarFrameHeight;

    // --- New Foreground Sizing ---
    public final IntValue healthOverlayWidth;
    public final IntValue healthOverlayHeight;

    // --- Positioning ---
    public final IntValue healthBarXOffset;
    public final IntValue healthBarYOffset;
    public final IntValue healthTotalXOffset;
    public final IntValue healthTotalYOffset;
    public final IntValue healthOverlayXOffset;
    public final IntValue healthOverlayYOffset;

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
    public static final int DEFAULT_STAMINA_TOTAL_X_OFFSET = -74; // Recalculated based on anchor
    public static final int DEFAULT_STAMINA_TOTAL_Y_OFFSET = 0;

    public final BooleanValue enableStaminaBar;
    public final EnumValue<HUDPositioning.BarPlacement> staminaBarAnchor;
    public final BooleanValue fadeStaminaWhenFull;
    public final EnumValue<TextBehavior> showStaminaText;
    public final EnumValue<HorizontalAlignment> staminaTextAlign;
    public final BooleanValue enableStaminaForeground;
    public final BooleanValue enableStaminaBackground;
    public final IntValue staminaBackgroundWidth;
    public final IntValue staminaBackgroundHeight;
    public final IntValue staminaBarWidth;
    public final IntValue staminaBarHeight;
    public final IntValue staminaBarAnimationCycles;
    public final IntValue staminaBarFrameHeight;
    public final IntValue staminaOverlayWidth;
    public final IntValue staminaOverlayHeight;
    public final IntValue staminaOverlayXOffset;
    public final IntValue staminaOverlayYOffset;
    public final IntValue staminaBarXOffset;
    public final IntValue staminaBarYOffset;
    public final IntValue staminaTotalXOffset;
    public final IntValue staminaTotalYOffset;

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

    public final BooleanValue enableManaBar;
    public final EnumValue<HUDPositioning.BarPlacement> manaBarAnchor;
    public final BooleanValue fadeManaWhenFull;
    public final EnumValue<TextBehavior> showManaText;
    public final EnumValue<HorizontalAlignment> manaTextAlign;
    public final BooleanValue enableManaForeground;
    public final BooleanValue enableManaBackground;
    public final IntValue manaBackgroundWidth;
    public final IntValue manaBackgroundHeight;
    public final IntValue manaBarWidth;
    public final IntValue manaBarHeight;
    public final IntValue manaBarAnimationCycles;
    public final IntValue manaBarFrameHeight;
    public final IntValue manaOverlayWidth;
    public final IntValue manaOverlayHeight;
    public final IntValue manaBarXOffset;
    public final IntValue manaBarYOffset;
    public final IntValue manaTotalXOffset;
    public final IntValue manaTotalYOffset;
    public final IntValue manaOverlayXOffset;
    public final IntValue manaOverlayYOffset;

    /**
     * Armor
     */
    public static final HUDPositioning.BarPlacement DEFAULT_ARMOR_BAR_ANCHOR = HUDPositioning.BarPlacement.ARMOR;
    public static final boolean DEFAULT_ENABLE_ARMOR_BAR = true;
    public static final boolean DEFAULT_HIDE_ARMOR_BAR = true;
    public static final int DEFAULT_MAX_EXPECTED_ARMOR = 20;
    public static final int DEFAULT_MAX_EXPECTED_PROT = 16;
    public static final int DEFAULT_ARMOR_BACKGROUND_WIDTH = 80;
    public static final int DEFAULT_ARMOR_BACKGROUND_HEIGHT = 10;
    public static final int DEFAULT_ARMOR_BAR_WIDTH = 74;
    public static final int DEFAULT_ARMOR_BAR_HEIGHT = 4;
    public static final int DEFAULT_ARMOR_BAR_X_OFFSET = 3;
    public static final int DEFAULT_ARMOR_BAR_Y_OFFSET = 3;
    public static final int DEFAULT_ARMOR_TOTAL_X_OFFSET = 0;
    public static final int DEFAULT_ARMOR_TOTAL_Y_OFFSET = 0;
    public static final boolean DEFAULT_ENABLE_ARMOR_ICON = true;
    public static final int DEFAULT_ARMOR_ICON_SIZE = 16;
    public static final int DEFAULT_PROT_OVERLAY_ANIMATION_CYCLES = 16;
    public static final int DEFAULT_PROT_OVERLAY_FRAME_HEIGHT = 4;
    public static final int DEFAULT_ARMOR_ICON_X_OFFSET = 0;
    public static final int DEFAULT_ARMOR_ICON_Y_OFFSET = 0;

    public final EnumValue<HUDPositioning.BarPlacement> armorBarAnchor;
    public final BooleanValue enableArmorBar;
    public final BooleanValue hideArmorBar;
    public final IntValue maxExpectedArmor;
    public final IntValue maxExpectedProt;
    public final IntValue armorBackgroundWidth;
    public final IntValue armorBackgroundHeight;
    public final IntValue armorBarWidth;
    public final IntValue armorBarHeight;
    public final IntValue armorBarXOffset;
    public final IntValue armorBarYOffset;
    public final IntValue armorTotalXOffset;
    public final IntValue armorTotalYOffset;
    public final BooleanValue enableArmorIcon;
    public final IntValue armorIconSize;
    public final IntValue protOverlayAnimationCycles;
    public final IntValue protOverlayFrameHeight;
    public final IntValue armorIconXOffset;
    public final IntValue armorIconYOffset;

    /**
     * Air
     */
    public static final HUDPositioning.BarPlacement DEFAULT_AIR_BAR_ANCHOR = HUDPositioning.BarPlacement.AIR;
    public static final boolean DEFAULT_ENABLE_AIR_BAR = true;
    public static final boolean DEFAULT_HIDE_AIR_BAR = false;
    public static final int DEFAULT_AIR_BACKGROUND_WIDTH = 80;
    public static final int DEFAULT_AIR_BACKGROUND_HEIGHT = 10;
    public static final int DEFAULT_AIR_BAR_WIDTH = 74;
    public static final int DEFAULT_AIR_BAR_HEIGHT = 4;
    public static final int DEFAULT_AIR_BAR_X_OFFSET = 3;
    public static final int DEFAULT_AIR_BAR_Y_OFFSET = 3;
    public static final int DEFAULT_AIR_TOTAL_X_OFFSET = 0;
    public static final int DEFAULT_AIR_TOTAL_Y_OFFSET = 0;
    public static final boolean DEFAULT_ENABLE_AIR_ICON = true;
    public static final int DEFAULT_AIR_ICON_SIZE = 16;
    public static final int DEFAULT_AIR_ICON_X_OFFSET = 0;
    public static final int DEFAULT_AIR_ICON_Y_OFFSET = 0;

    public final EnumValue<HUDPositioning.BarPlacement> airBarAnchor;
    public final BooleanValue enableAirBar;
    public final BooleanValue hideAirBar;
    public final IntValue airBackgroundWidth;
    public final IntValue airBackgroundHeight;
    public final IntValue airBarWidth;
    public final IntValue airBarHeight;
    public final IntValue airBarXOffset;
    public final IntValue airBarYOffset;
    public final IntValue airTotalXOffset;
    public final IntValue airTotalYOffset;
    public final BooleanValue enableAirIcon;
    public final IntValue airIconSize;
    public final IntValue airIconXOffset;
    public final IntValue airIconYOffset;


    // Constructor takes a Builder
    public CClient(#if (FABRIC && !AFTER_21_1) || FORGE ForgeConfigSpec.Builder builder #else ModConfigSpec.Builder builder #endif) {
        builder.push("general"); // Corresponds to group("general", ...)
        builder.comment("Customize shared settings");

        textScalingFactor = builder
                .comment("The amount to adjust the size of text rendered on resource bars")
                .defineInRange("textScalingFactor", DEFAULT_TEXT_SCALING_FACTOR, 0.0f, 2.0f); // Assuming a range for float

        disableDefaultArmor = builder
                .comment("Whether to hide the vanilla armor bar from the HUD")
                .define("disableDefaultArmor", DEFAULT_DISABLE_DEFAULT_ARMOR);
        builder.pop();


        builder.push("health"); // Group for health settings
        builder.comment("Customize the health bar"); // Comment for the group

        enableHealthBar = builder
                .comment("Whether to render a custom bar instead of hearts.")
                .define("enableHealthBar", DEFAULT_ENABLE_HEALTH_BAR);

        healthBarAnchor = builder
                .comment("Anchor point for the health bar.")
                .defineEnum("healthBarAnchor", DEFAULT_HEALTH_BAR_ANCHOR);

        fadeHealthWhenFull = builder
                .comment("Whether to dynamically hide the health bar when the player is at full health")
                .define("fadeHealthWhenFull", DEFAULT_FADE_HEALTH_WHEN_FULL);

        showHealthText = builder
                .comment("When health current/maximum values should be rendered as a text overlay. Always hidden when the bar is invisible, even if set to ALWAYS")
                .defineEnum("showHealthText", DEFAULT_SHOW_HEALTH_TEXT); // Changed from "Show Health Text" key

        healthTextAlign = builder
                .comment("Horizontal alignment for the health value text.")
                .defineEnum("healthTextAlign", DEFAULT_HEALTH_TEXT_ALIGN); // Changed from "Health Text Align" key

        enableHealthForeground = builder
                .comment("Render an extra layer on top of the health bar. Can be toggled in HUD editor.")
                .define("enableHealthForeground", DEFAULT_ENABLE_HEALTH_FOREGROUND); // Changed from "Enable Foreground Layer" key

        enableHealthBackground = builder
                .comment("Render an extra layer behind the health bar. Can be toggled in HUD editor.")
                .define("enableHealthBackground", DEFAULT_ENABLE_HEALTH_BACKGROUND); // Changed from "Enable Background Layer" key

        // --- Sizing ---
        healthBackgroundWidth = builder
                .comment("Width of the health bar's background sprite, in pixels.")
                .defineInRange("healthBackgroundWidth", DEFAULT_HEALTH_BACKGROUND_WIDTH, 0, Integer.MAX_VALUE);

        healthBackgroundHeight = builder
                .comment("Height of the health bar's background sprite, in pixels.")
                .defineInRange("healthBackgroundHeight", DEFAULT_HEALTH_BACKGROUND_HEIGHT, 0, Integer.MAX_VALUE);

        healthBarWidth = builder
                .comment("Width of the actual animated bar, in pixels.")
                .defineInRange("healthBarWidth", DEFAULT_HEALTH_BAR_WIDTH, 0, Integer.MAX_VALUE);

        healthBarHeight = builder
                .comment("Height of the actual animated bar, in pixels.")
                .defineInRange("healthBarHeight", DEFAULT_HEALTH_BAR_HEIGHT, 0, Integer.MAX_VALUE);

        healthBarAnimationCycles = builder
                .comment("Number of animation frames in the bar animation.")
                .defineInRange("healthBarAnimationCycles", 33, 0, Integer.MAX_VALUE);

        healthBarFrameHeight = builder
                .comment("Height of each frame in the health bar animation.")
                .defineInRange("healthBarFrameHeight", 6, 0, Integer.MAX_VALUE);

        // --- New Foreground Sizing ---
        healthOverlayWidth = builder
                .comment("Width of the health bar's foreground sprite, in pixels.")
                .defineInRange("healthOverlayWidth", DEFAULT_HEALTH_OVERLAY_WIDTH, 0, Integer.MAX_VALUE);

        healthOverlayHeight = builder
                .comment("Height of the health bar's foreground sprite, in pixels.")
                .defineInRange("healthOverlayHeight", DEFAULT_HEALTH_OVERLAY_HEIGHT, 0, Integer.MAX_VALUE);

        // --- Positioning ---
        // For offsets, providing a wide range like -1000 to 1000, or simply not using defineInRange if any int is valid.
        // Using define() for simplicity here, but defineInRange can be used for stricter control.
        healthBarXOffset = builder
                .comment("Bar X offset relative to background. Adjusted via HUD editor focus mode.")
                .defineInRange("healthBarXOffset", DEFAULT_HEALTH_BAR_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);

        healthBarYOffset = builder
                .comment("Bar Y offset relative to background. Adjusted via HUD editor focus mode.")
                .defineInRange("healthBarYOffset", DEFAULT_HEALTH_BAR_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);

        healthTotalXOffset = builder
                .comment("Overall bar complex X offset relative to anchor. Adjusted via HUD editor.")
                .defineInRange("healthTotalXOffset", DEFAULT_HEALTH_TOTAL_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);

        healthTotalYOffset = builder
                .comment("Overall bar complex Y offset relative to anchor. Adjusted via HUD editor.")
                .defineInRange("healthTotalYOffset", DEFAULT_HEALTH_TOTAL_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);

        healthOverlayXOffset = builder
                .comment("Foreground overlay X offset. Adjusted via HUD editor focus mode.")
                .defineInRange("healthOverlayXOffset", DEFAULT_HEALTH_OVERLAY_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);

        healthOverlayYOffset = builder
                .comment("Foreground overlay Y offset. Adjusted via HUD editor focus mode.")
                .defineInRange("healthOverlayYOffset", DEFAULT_HEALTH_OVERLAY_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        builder.pop(); // Pop "health" group

        // --- Stamina --- 
        builder.push("stamina");
        builder.comment("Customize hunger / the stamina bar");
        enableStaminaBar = builder.comment("Whether to render a custom bar instead of hunger.").define("enableStaminaBar", DEFAULT_ENABLE_STAMINA_BAR);
        staminaBarAnchor = builder.comment("Anchor point for the stamina bar.").defineEnum("staminaBarAnchor", DEFAULT_STAMINA_BAR_ANCHOR);
        fadeStaminaWhenFull = builder.comment("Whether to dynamically hide the stamina bar when the player is at full hunger/stamina").define("fadeStaminaWhenFull", DEFAULT_FADE_STAMINA_WHEN_FULL);
        showStaminaText = builder.comment("When stamina current/maximum values should be rendered as a text overlay. Always hidden when the bar is invisible, even if set to ALWAYS").defineEnum("showStaminaText", DEFAULT_SHOW_STAMINA_TEXT);
        staminaTextAlign = builder.comment("Horizontal alignment for the stamina value text.").defineEnum("staminaTextAlign", DEFAULT_STAMINA_TEXT_ALIGN);
        enableStaminaForeground = builder.comment("Render an extra layer on top of the stamina bar. Can be toggled in HUD editor.").define("enableStaminaForeground", DEFAULT_ENABLE_STAMINA_FOREGROUND);
        enableStaminaBackground = builder.comment("Render an extra layer behind the stamina bar. Can be toggled in HUD editor.").define("enableStaminaBackground", DEFAULT_ENABLE_STAMINA_BACKGROUND);
        staminaBackgroundWidth = builder.comment("Width of the stamina bar's background sprite, in pixels.").defineInRange("staminaBackgroundWidth", DEFAULT_STAMINA_BACKGROUND_WIDTH, 0, Integer.MAX_VALUE);
        staminaBackgroundHeight = builder.comment("Height of the stamina bar's background sprite, in pixels.").defineInRange("staminaBackgroundHeight", DEFAULT_STAMINA_BACKGROUND_HEIGHT, 0, Integer.MAX_VALUE);
        staminaBarWidth = builder.comment("Width of the actual animated bar, in pixels.").defineInRange("staminaBarWidth", DEFAULT_STAMINA_BAR_WIDTH, 0, Integer.MAX_VALUE);
        staminaBarHeight = builder.comment("Height of the actual animated bar, in pixels.").defineInRange("staminaBarHeight", DEFAULT_STAMINA_BAR_HEIGHT, 0, Integer.MAX_VALUE);
        staminaBarAnimationCycles = builder.comment("Number of animation frames in the bar animation.").defineInRange("staminaBarAnimationCycles", 33, 0, Integer.MAX_VALUE);
        staminaBarFrameHeight = builder.comment("Height of each frame in the stamina bar animation.").defineInRange("staminaBarFrameHeight", 6, 0, Integer.MAX_VALUE);
        staminaOverlayWidth = builder.comment("Width of the stamina bar's foreground sprite, in pixels.").defineInRange("staminaOverlayWidth", DEFAULT_STAMINA_OVERLAY_WIDTH, 0, Integer.MAX_VALUE);
        staminaOverlayHeight = builder.comment("Height of the stamina bar's foreground sprite, in pixels.").defineInRange("staminaOverlayHeight", DEFAULT_STAMINA_OVERLAY_HEIGHT, 0, Integer.MAX_VALUE);
        staminaOverlayXOffset = builder.comment("Foreground overlay X offset. Adjusted via HUD editor focus mode.").defineInRange("staminaOverlayXOffset", DEFAULT_STAMINA_OVERLAY_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        staminaOverlayYOffset = builder.comment("Foreground overlay Y offset. Adjusted via HUD editor focus mode.").defineInRange("staminaOverlayYOffset", DEFAULT_STAMINA_OVERLAY_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        staminaBarXOffset = builder.comment("Bar X offset relative to background. Adjusted via HUD editor focus mode.").defineInRange("staminaBarXOffset", DEFAULT_STAMINA_BAR_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        staminaBarYOffset = builder.comment("Bar Y offset relative to background. Adjusted via HUD editor focus mode.").defineInRange("staminaBarYOffset", DEFAULT_STAMINA_BAR_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        staminaTotalXOffset = builder.comment("Overall bar complex X offset relative to anchor. Adjusted via HUD editor.").defineInRange("staminaTotalXOffset", DEFAULT_STAMINA_TOTAL_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        staminaTotalYOffset = builder.comment("Overall bar complex Y offset relative to anchor. Adjusted via HUD editor.").defineInRange("staminaTotalYOffset", DEFAULT_STAMINA_TOTAL_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        builder.pop();

        // --- Mana --- 
        builder.push("mana");
        builder.comment("Customize the mana bar");
        enableManaBar = builder.comment("Whether to render a custom bar instead of supported mods' built-in mana bars.").define("enableManaBar", DEFAULT_ENABLE_MANA_BAR);
        manaBarAnchor = builder.comment("Anchor point for the mana bar.").defineEnum("manaBarAnchor", DEFAULT_MANA_BAR_ANCHOR);
        fadeManaWhenFull = builder.comment("Whether to dynamically hide the mana bar when mana is full.").define("fadeManaWhenFull", DEFAULT_FADE_MANA_WHEN_FULL);
        showManaText = builder.comment("When mana current/maximum values should be rendered as a text overlay. Always hidden when the bar is invisible, even if set to ALWAYS").defineEnum("showManaText", DEFAULT_SHOW_MANA_TEXT);
        manaTextAlign = builder.comment("Horizontal alignment for the mana value text.").defineEnum("manaTextAlign", DEFAULT_MANA_TEXT_ALIGN);
        enableManaForeground = builder.comment("Render an extra layer on top of the resource bar. Can be toggled in HUD editor.").define("enableManaForeground", DEFAULT_ENABLE_MANA_FOREGROUND);
        enableManaBackground = builder.comment("Render an extra layer behind the resource bar. Can be toggled in HUD editor.").define("enableManaBackground", DEFAULT_ENABLE_MANA_BACKGROUND);
        manaBackgroundWidth = builder.comment("Width of the mana bar's background sprite, in pixels.").defineInRange("manaBackgroundWidth", DEFAULT_MANA_BACKGROUND_WIDTH, 0, Integer.MAX_VALUE);
        manaBackgroundHeight = builder.comment("Height of the mana bar's background sprite, in pixels.").defineInRange("manaBackgroundHeight", DEFAULT_MANA_BACKGROUND_HEIGHT, 0, Integer.MAX_VALUE);
        manaBarWidth = builder.comment("Width of the actual animated bar, in pixels.").defineInRange("manaBarWidth", DEFAULT_MANA_BAR_WIDTH, 0, Integer.MAX_VALUE);
        manaBarHeight = builder.comment("Height of the actual animated bar, in pixels.").defineInRange("manaBarHeight", DEFAULT_MANA_BAR_HEIGHT, 0, Integer.MAX_VALUE);
        manaBarAnimationCycles = builder.comment("Number of animation frames in the bar animation.").defineInRange("manaBarAnimationCycles", 33, 0, Integer.MAX_VALUE);
        manaBarFrameHeight = builder.comment("Height of each frame in the mana bar animation.").defineInRange("manaBarFrameHeight", 6, 0, Integer.MAX_VALUE);
        manaOverlayWidth = builder.comment("Width of the mana bar's foreground sprite, in pixels.").defineInRange("manaOverlayWidth", DEFAULT_MANA_OVERLAY_WIDTH, 0, Integer.MAX_VALUE);
        manaOverlayHeight = builder.comment("Height of the mana bar's foreground sprite, in pixels.").defineInRange("manaOverlayHeight", DEFAULT_MANA_OVERLAY_HEIGHT, 0, Integer.MAX_VALUE);
        manaBarXOffset = builder.comment("Bar X offset relative to background. Adjusted via HUD editor focus mode.").defineInRange("manaBarXOffset", DEFAULT_MANA_BAR_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        manaBarYOffset = builder.comment("Bar Y offset relative to background. Adjusted via HUD editor focus mode.").defineInRange("manaBarYOffset", DEFAULT_MANA_BAR_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        manaTotalXOffset = builder.comment("Overall bar complex X offset relative to anchor. Adjusted via HUD editor.").defineInRange("manaTotalXOffset", DEFAULT_MANA_TOTAL_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        manaTotalYOffset = builder.comment("Overall bar complex Y offset relative to anchor. Adjusted via HUD editor.").defineInRange("manaTotalYOffset", DEFAULT_MANA_TOTAL_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        manaOverlayXOffset = builder.comment("Foreground overlay X offset. Adjusted via HUD editor focus mode.").defineInRange("manaOverlayXOffset", DEFAULT_MANA_OVERLAY_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        manaOverlayYOffset = builder.comment("Foreground overlay Y offset. Adjusted via HUD editor focus mode.").defineInRange("manaOverlayYOffset", DEFAULT_MANA_OVERLAY_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        builder.pop();

        // --- Armor --- 
        builder.push("armor");
        builder.comment("Customize the armor bar");
        armorBarAnchor = builder.comment("Anchor point for the armor bar.").defineEnum("armorBarAnchor", DEFAULT_ARMOR_BAR_ANCHOR);
        enableArmorBar = builder.comment("Whether to render a custom bar instead of the vanilla armor bar").define("enableArmorBar", DEFAULT_ENABLE_ARMOR_BAR);
        hideArmorBar = builder.comment("Whether to hide armor entirely, including vanilla armor points. Requires enableArmorBar to be enabled.").define("hideArmorBar", DEFAULT_HIDE_ARMOR_BAR);
        maxExpectedArmor = builder.comment("The maximum obtainable armor value for your modpack, for the purpose of how much bar to render + the icon index").defineInRange("maxExpectedArmor", DEFAULT_MAX_EXPECTED_ARMOR, 0, Integer.MAX_VALUE);
        maxExpectedProt = builder.comment("The maximum obtainable Protection value for your modpack, for the purpose of how much overlay to render").defineInRange("maxExpectedProt", DEFAULT_MAX_EXPECTED_PROT, 0, Integer.MAX_VALUE);
        armorBackgroundWidth = builder.comment("Width of the armor bar's background sprite, in pixels.").defineInRange("armorBackgroundWidth", DEFAULT_ARMOR_BACKGROUND_WIDTH, 0, Integer.MAX_VALUE);
        armorBackgroundHeight = builder.comment("Height of the armor bar's background sprite, in pixels.").defineInRange("armorBackgroundHeight", DEFAULT_ARMOR_BACKGROUND_HEIGHT, 0, Integer.MAX_VALUE);
        armorBarWidth = builder.comment("Width of the actual filled bar, in pixels.").defineInRange("armorBarWidth", DEFAULT_ARMOR_BAR_WIDTH, 0, Integer.MAX_VALUE);
        armorBarHeight = builder.comment("Height of the actual filled bar, in pixels.").defineInRange("armorBarHeight", DEFAULT_ARMOR_BAR_HEIGHT, 0, Integer.MAX_VALUE);
        armorBarXOffset = builder.comment("How much to shift the filled bar to the right relative to the background. In other words, the thickness of the background's left border.").defineInRange("armorBarXOffset", DEFAULT_ARMOR_BAR_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        armorBarYOffset = builder.comment("How much to shift the filled bar upward relative to the background. In other words, the thickness of the background's bottom border.").defineInRange("armorBarYOffset", DEFAULT_ARMOR_BAR_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        armorTotalXOffset = builder.comment("How much to shift the entire bar+background complex to the right").defineInRange("armorTotalXOffset", DEFAULT_ARMOR_TOTAL_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        armorTotalYOffset = builder.comment("How much to shift the entire bar+background complex upward").defineInRange("armorTotalYOffset", DEFAULT_ARMOR_TOTAL_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        enableArmorIcon = builder.comment("Whether to render a sprite on the side of the armor bar, based on armor points").define("enableArmorIcon", DEFAULT_ENABLE_ARMOR_ICON);
        armorIconSize = builder.comment("Sprite size for the armor overlay icons").defineInRange("armorIconSize", DEFAULT_ARMOR_ICON_SIZE, 0, Integer.MAX_VALUE);
        protOverlayAnimationCycles = builder.comment("Number of animation frames in the protection overlay animation.").defineInRange("protOverlayAnimationCycles", DEFAULT_PROT_OVERLAY_ANIMATION_CYCLES, 0, Integer.MAX_VALUE);
        protOverlayFrameHeight = builder.comment("Height of each frame in the protection overlay animation.").defineInRange("protOverlayFrameHeight", DEFAULT_PROT_OVERLAY_FRAME_HEIGHT, 0, Integer.MAX_VALUE);
        armorIconXOffset = builder.comment("How much to shift the armor icon to the right.").defineInRange("armorIconXOffset", DEFAULT_ARMOR_ICON_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        armorIconYOffset = builder.comment("How much to shift the armor icon upward.").defineInRange("armorIconYOffset", DEFAULT_ARMOR_ICON_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        builder.pop();

        // --- Air --- 
        builder.push("air");
        builder.comment("Customize the air bar");
        airBarAnchor = builder.comment("Anchor point for the air bar.").defineEnum("airBarAnchor", DEFAULT_AIR_BAR_ANCHOR);
        enableAirBar = builder.comment("Whether to render a custom bar instead of the vanilla air bar").define("enableAirBar", DEFAULT_ENABLE_AIR_BAR);
        hideAirBar = builder.comment("Whether to hide air entirely, including vanilla air points. Requires enableAirBar to be enabled.").define("hideAirBar", DEFAULT_HIDE_AIR_BAR);
        airBackgroundWidth = builder.comment("Width of the air bar's background sprite, in pixels.").defineInRange("airBackgroundWidth", DEFAULT_AIR_BACKGROUND_WIDTH, 0, Integer.MAX_VALUE);
        airBackgroundHeight = builder.comment("Height of the air bar's background sprite, in pixels.").defineInRange("airBackgroundHeight", DEFAULT_AIR_BACKGROUND_HEIGHT, 0, Integer.MAX_VALUE);
        airBarWidth = builder.comment("Width of the actual filled bar, in pixels.").defineInRange("airBarWidth", DEFAULT_AIR_BAR_WIDTH, 0, Integer.MAX_VALUE);
        airBarHeight = builder.comment("Height of the actual filled bar, in pixels.").defineInRange("airBarHeight", DEFAULT_AIR_BAR_HEIGHT, 0, Integer.MAX_VALUE);
        airBarXOffset = builder.comment("How much to shift the filled bar to the right relative to the background. In other words, the thickness of the background's left border.").defineInRange("airBarXOffset", DEFAULT_AIR_BAR_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        airBarYOffset = builder.comment("How much to shift the filled bar upward relative to the background. In other words, the thickness of the background's bottom border.").defineInRange("airBarYOffset", DEFAULT_AIR_BAR_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        airTotalXOffset = builder.comment("How much to shift the entire bar+background complex to the right").defineInRange("airTotalXOffset", DEFAULT_AIR_TOTAL_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        airTotalYOffset = builder.comment("How much to shift the entire bar+background complex upward").defineInRange("airTotalYOffset", DEFAULT_AIR_TOTAL_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        enableAirIcon = builder.comment("Whether to render a sprite on the side of the air bar, based on air points").define("enableAirIcon", DEFAULT_ENABLE_AIR_ICON);
        airIconSize = builder.comment("Sprite size for the air overlay icons").defineInRange("airIconSize", DEFAULT_AIR_ICON_SIZE, 0, Integer.MAX_VALUE);
        airIconXOffset = builder.comment("How much to shift the air icon to the right.").defineInRange("airIconXOffset", DEFAULT_AIR_ICON_X_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        airIconYOffset = builder.comment("How much to shift the air icon upward.").defineInRange("airIconYOffset", DEFAULT_AIR_ICON_Y_OFFSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        builder.pop();

    }
}
