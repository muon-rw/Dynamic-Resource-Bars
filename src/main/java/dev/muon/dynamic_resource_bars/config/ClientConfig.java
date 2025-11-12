package dev.muon.dynamic_resource_bars.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.muon.dynamic_resource_bars.DynamicResourceBars; // For logging
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;
import dev.muon.dynamic_resource_bars.util.TextBehavior;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import dev.muon.dynamic_resource_bars.util.FillDirection;
import dev.muon.dynamic_resource_bars.provider.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ClientConfig {

    private static Path CONFIG_FILE_PATH;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls() // Optional: good for ensuring all fields are present in JSON
            .enableComplexMapKeySerialization() // Good practice
            // .registerTypeAdapter(Path.class, new PathTypeAdapter()) // Example if complex types need adapters
            .create();

    // General
    public static final float DEFAULT_TEXT_SCALING_FACTOR = 0.5f;
    public double textScalingFactor = DEFAULT_TEXT_SCALING_FACTOR;
    
    // Global text defaults
    public static final int DEFAULT_TEXT_COLOR = 0xFFFFFF; // White
    public static final int DEFAULT_TEXT_OPACITY = 200; // Out of 255
    public static final float DEFAULT_TEXT_SIZE = 1.0f;
    
    // Global text fields
    public int globalTextColor = DEFAULT_TEXT_COLOR;
    public int globalTextOpacity = DEFAULT_TEXT_OPACITY;
    public float globalTextSize = DEFAULT_TEXT_SIZE;

    // Health Defaults & Fields
    public static final boolean DEFAULT_ENABLE_HEALTH_BAR = true;
    public static final HUDPositioning.BarPlacement DEFAULT_HEALTH_BAR_ANCHOR = HUDPositioning.BarPlacement.HEALTH;
    public static final boolean DEFAULT_FADE_HEALTH_WHEN_FULL = false;
    public static final TextBehavior DEFAULT_SHOW_HEALTH_TEXT = TextBehavior.WHEN_NOT_FULL;
    public static final HorizontalAlignment DEFAULT_HEALTH_TEXT_ALIGN = HorizontalAlignment.CENTER;
    public static final boolean DEFAULT_ENABLE_HEALTH_FOREGROUND = false;
    public static final boolean DEFAULT_ENABLE_HEALTH_BACKGROUND = true;
    public static final FillDirection DEFAULT_HEALTH_FILL_DIRECTION = FillDirection.HORIZONTAL;
    public static final int DEFAULT_HEALTH_BACKGROUND_WIDTH = 80;
    public static final int DEFAULT_HEALTH_BACKGROUND_HEIGHT = 10;
    public static final int DEFAULT_HEALTH_BAR_WIDTH = 74;
    public static final int DEFAULT_HEALTH_BAR_HEIGHT = 4;
    public static final int DEFAULT_HEALTH_OVERLAY_WIDTH = 80;
    public static final int DEFAULT_HEALTH_OVERLAY_HEIGHT = 10;
    public static final int DEFAULT_HEALTH_BAR_X_OFFSET = 3;
    public static final int DEFAULT_HEALTH_BAR_Y_OFFSET = 3;
    public static final int DEFAULT_HEALTH_TOTAL_X_OFFSET = 0;
    public static final int DEFAULT_HEALTH_TOTAL_Y_OFFSET = 0;
    public static final int DEFAULT_HEALTH_OVERLAY_X_OFFSET = 0;
    public static final int DEFAULT_HEALTH_OVERLAY_Y_OFFSET = -3;
    public static final int DEFAULT_HEALTH_BACKGROUND_X_OFFSET = 0;
    public static final int DEFAULT_HEALTH_BACKGROUND_Y_OFFSET = 0;
    public static final int DEFAULT_HEALTH_TEXT_X_OFFSET = 3;
    public static final int DEFAULT_HEALTH_TEXT_Y_OFFSET = 3;
    public static final int DEFAULT_HEALTH_TEXT_COLOR = DEFAULT_TEXT_COLOR;
    public static final int DEFAULT_HEALTH_TEXT_OPACITY = DEFAULT_TEXT_OPACITY;
    public static final float DEFAULT_HEALTH_TEXT_SIZE = DEFAULT_TEXT_SIZE;
    public static final int DEFAULT_HEALTH_ABSORPTION_TEXT_X_OFFSET = 65;
    public static final int DEFAULT_HEALTH_ABSORPTION_TEXT_Y_OFFSET = 3;

    public boolean enableHealthBar;
    public HUDPositioning.BarPlacement healthBarAnchor;
    public boolean fadeHealthWhenFull;
    public TextBehavior showHealthText;
    public HorizontalAlignment healthTextAlign;
    public boolean enableHealthForeground;
    public boolean enableHealthBackground;
    public FillDirection healthFillDirection;
    public int healthBackgroundWidth;
    public int healthBackgroundHeight;
    public int healthBarWidth;
    public int healthBarHeight;
    public int healthOverlayWidth;
    public int healthOverlayHeight;
    public int healthBarXOffset;
    public int healthBarYOffset;
    public int healthTotalXOffset;
    public int healthTotalYOffset;
    public int healthOverlayXOffset;
    public int healthOverlayYOffset;
    public int healthBackgroundXOffset;
    public int healthBackgroundYOffset;
    public int healthTextXOffset;
    public int healthTextYOffset;
    public int healthTextColor;
    public int healthTextOpacity;
    public float healthTextSize;
    public int healthAbsorptionTextXOffset;
    public int healthAbsorptionTextYOffset;

    // Stamina Defaults & Fields
    public static final boolean DEFAULT_ENABLE_STAMINA_BAR = true;
    public static final StaminaBarBehavior DEFAULT_STAMINA_BAR_BEHAVIOR = StaminaBarBehavior.FOOD;
    public static final boolean DEFAULT_MERGE_MOUNT_HEALTH = true;
    public static final boolean DEFAULT_ENABLE_MOUNT_HEALTH = true;
    public static final HUDPositioning.BarPlacement DEFAULT_STAMINA_BAR_ANCHOR = HUDPositioning.BarPlacement.HUNGER;
    public static final boolean DEFAULT_FADE_STAMINA_WHEN_FULL = false;
    public static final TextBehavior DEFAULT_SHOW_STAMINA_TEXT = TextBehavior.NEVER;
    public static final HorizontalAlignment DEFAULT_STAMINA_TEXT_ALIGN = HorizontalAlignment.CENTER;
    public static final boolean DEFAULT_ENABLE_STAMINA_FOREGROUND = false;
    public static final boolean DEFAULT_ENABLE_STAMINA_BACKGROUND = true;
    public static final FillDirection DEFAULT_STAMINA_FILL_DIRECTION = FillDirection.HORIZONTAL;
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
    public static final int DEFAULT_STAMINA_TOTAL_X_OFFSET = -80;
    public static final int DEFAULT_STAMINA_TOTAL_Y_OFFSET = 0;
    public static final int DEFAULT_STAMINA_BACKGROUND_X_OFFSET = 0;
    public static final int DEFAULT_STAMINA_BACKGROUND_Y_OFFSET = 0;
    public static final int DEFAULT_STAMINA_TEXT_X_OFFSET = 3;
    public static final int DEFAULT_STAMINA_TEXT_Y_OFFSET = 3;
    public static final int DEFAULT_STAMINA_TEXT_COLOR = DEFAULT_TEXT_COLOR;
    public static final int DEFAULT_STAMINA_TEXT_OPACITY = DEFAULT_TEXT_OPACITY;
    public static final float DEFAULT_STAMINA_TEXT_SIZE = DEFAULT_TEXT_SIZE;

    public StaminaBarBehavior staminaBarBehavior;
    public boolean mergeMountHealth;
    public boolean enableMountHealth;
    public HUDPositioning.BarPlacement staminaBarAnchor;
    public boolean fadeStaminaWhenFull;
    public TextBehavior showStaminaText;
    public HorizontalAlignment staminaTextAlign;
    public boolean enableStaminaForeground;
    public boolean enableStaminaBackground;
    public FillDirection staminaFillDirection;
    public int staminaBackgroundWidth;
    public int staminaBackgroundHeight;
    public int staminaBarWidth;
    public int staminaBarHeight;
    public int staminaOverlayWidth;
    public int staminaOverlayHeight;
    public int staminaOverlayXOffset;
    public int staminaOverlayYOffset;
    public int staminaBarXOffset;
    public int staminaBarYOffset;
    public int staminaTotalXOffset;
    public int staminaTotalYOffset;
    public int staminaBackgroundXOffset;
    public int staminaBackgroundYOffset;
    public int staminaTextXOffset;
    public int staminaTextYOffset;
    public int staminaTextColor;
    public int staminaTextOpacity;
    public float staminaTextSize;

    // Mana Defaults & Fields
    public static final ManaBarBehavior DEFAULT_MANA_BAR_BEHAVIOR = ManaBarBehavior.OFF;
    public static final HUDPositioning.BarPlacement DEFAULT_MANA_BAR_ANCHOR = HUDPositioning.BarPlacement.ABOVE_UTILITIES;
    public static final boolean DEFAULT_ENABLE_MANA_BACKGROUND = true;
    public static final boolean DEFAULT_ENABLE_MANA_FOREGROUND = true;
    public static final boolean DEFAULT_FADE_MANA_WHEN_FULL = true;
    public static final TextBehavior DEFAULT_SHOW_MANA_TEXT = TextBehavior.WHEN_NOT_FULL;
    public static final HorizontalAlignment DEFAULT_MANA_TEXT_ALIGN = HorizontalAlignment.CENTER;
    public static final FillDirection DEFAULT_MANA_FILL_DIRECTION = FillDirection.HORIZONTAL;
    public static final int DEFAULT_MANA_TOTAL_X_OFFSET = -40;
    public static final int DEFAULT_MANA_TOTAL_Y_OFFSET = 0;
    public static final int DEFAULT_MANA_BACKGROUND_X_OFFSET = 0;
    public static final int DEFAULT_MANA_BACKGROUND_Y_OFFSET = 0;
    public static final int DEFAULT_MANA_BACKGROUND_WIDTH = 80;
    public static final int DEFAULT_MANA_BACKGROUND_HEIGHT = 10;
    public static final int DEFAULT_MANA_BAR_X_OFFSET = 3;
    public static final int DEFAULT_MANA_BAR_Y_OFFSET = 3;
    public static final int DEFAULT_MANA_BAR_WIDTH = 74;
    public static final int DEFAULT_MANA_BAR_HEIGHT = 4;
    public static final int DEFAULT_MANA_OVERLAY_X_OFFSET = 0;
    public static final int DEFAULT_MANA_OVERLAY_Y_OFFSET = -3;
    public static final int DEFAULT_MANA_OVERLAY_WIDTH = 81;
    public static final int DEFAULT_MANA_OVERLAY_HEIGHT = 9;
    public static final int DEFAULT_MANA_TEXT_X_OFFSET = 3;
    public static final int DEFAULT_MANA_TEXT_Y_OFFSET = 3;

    public ManaBarBehavior manaBarBehavior = DEFAULT_MANA_BAR_BEHAVIOR;
    public HUDPositioning.BarPlacement manaBarAnchor = DEFAULT_MANA_BAR_ANCHOR;
    public boolean enableManaBackground = DEFAULT_ENABLE_MANA_BACKGROUND;
    public boolean enableManaForeground = DEFAULT_ENABLE_MANA_FOREGROUND;
    public boolean fadeManaWhenFull;
    public TextBehavior showManaText;
    public HorizontalAlignment manaTextAlign;
    public FillDirection manaFillDirection;
    public int manaBackgroundWidth;
    public int manaBackgroundHeight;
    public int manaBarWidth;
    public int manaBarHeight;
    public int manaOverlayWidth;
    public int manaOverlayHeight;
    public int manaBarXOffset;
    public int manaBarYOffset;
    public int manaTotalXOffset;
    public int manaTotalYOffset;
    public int manaOverlayXOffset;
    public int manaOverlayYOffset;
    public int manaBackgroundXOffset;
    public int manaBackgroundYOffset;
    public int manaTextXOffset;
    public int manaTextYOffset;
    public int manaTextColor;
    public int manaTextOpacity;
    public float manaTextSize;

    // Armor Defaults & Fields
    public static final BarRenderBehavior DEFAULT_ARMOR_BAR_BEHAVIOR = BarRenderBehavior.HIDDEN;
    public static final HUDPositioning.BarPlacement DEFAULT_ARMOR_BAR_ANCHOR = HUDPositioning.BarPlacement.ARMOR;
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
    public static final int DEFAULT_ARMOR_ICON_Y_OFFSET = -4;
    public static final int DEFAULT_ARMOR_TEXT_X_OFFSET = 3;
    public static final int DEFAULT_ARMOR_TEXT_Y_OFFSET = 3;
    public static final int DEFAULT_ARMOR_TEXT_COLOR = DEFAULT_TEXT_COLOR;
    public static final int DEFAULT_ARMOR_TEXT_OPACITY = DEFAULT_TEXT_OPACITY;
    public static final float DEFAULT_ARMOR_TEXT_SIZE = DEFAULT_TEXT_SIZE;
    public static final TextBehavior DEFAULT_SHOW_ARMOR_TEXT = TextBehavior.NEVER;
    public static final HorizontalAlignment DEFAULT_ARMOR_TEXT_ALIGN = HorizontalAlignment.CENTER;
    public static final int DEFAULT_ARMOR_BACKGROUND_X_OFFSET = 0;
    public static final int DEFAULT_ARMOR_BACKGROUND_Y_OFFSET = 0;

    public BarRenderBehavior armorBarBehavior;
    public HUDPositioning.BarPlacement armorBarAnchor;
    public int maxExpectedArmor;
    public int maxExpectedProt;
    public int armorBackgroundWidth;
    public int armorBackgroundHeight;
    public int armorBarWidth;
    public int armorBarHeight;
    public int armorBarXOffset;
    public int armorBarYOffset;
    public int armorTotalXOffset;
    public int armorTotalYOffset;
    public boolean enableArmorIcon;
    public int armorIconSize;
    public int protOverlayAnimationCycles;
    public int protOverlayFrameHeight;
    public int armorIconXOffset;
    public int armorIconYOffset;
    public int armorTextXOffset;
    public int armorTextYOffset;
    public int armorTextColor;
    public int armorTextOpacity;
    public float armorTextSize;
    public TextBehavior showArmorText;
    public HorizontalAlignment armorTextAlign;
    public int armorBackgroundXOffset;
    public int armorBackgroundYOffset;

    // Air Defaults & Fields
    public static final BarRenderBehavior DEFAULT_AIR_BAR_BEHAVIOR = BarRenderBehavior.CUSTOM;
    public static final HUDPositioning.BarPlacement DEFAULT_AIR_BAR_ANCHOR = HUDPositioning.BarPlacement.AIR;
    public static final int DEFAULT_AIR_BACKGROUND_WIDTH = 80;
    public static final int DEFAULT_AIR_BACKGROUND_HEIGHT = 10;
    public static final int DEFAULT_AIR_BAR_WIDTH = 74;
    public static final int DEFAULT_AIR_BAR_HEIGHT = 4;
    public static final int DEFAULT_AIR_BAR_X_OFFSET = 3;
    public static final int DEFAULT_AIR_BAR_Y_OFFSET = 3;
    public static final int DEFAULT_AIR_TOTAL_X_OFFSET = -80;
    public static final int DEFAULT_AIR_TOTAL_Y_OFFSET = 0;
    public static final boolean DEFAULT_ENABLE_AIR_ICON = true;
    public static final int DEFAULT_AIR_ICON_SIZE = 16;
    public static final int DEFAULT_AIR_ICON_X_OFFSET = 66;
    public static final int DEFAULT_AIR_ICON_Y_OFFSET = -4;
    public static final int DEFAULT_AIR_TEXT_X_OFFSET = 3;
    public static final int DEFAULT_AIR_TEXT_Y_OFFSET = 3;
    public static final int DEFAULT_AIR_TEXT_COLOR = DEFAULT_TEXT_COLOR;
    public static final int DEFAULT_AIR_TEXT_OPACITY = DEFAULT_TEXT_OPACITY;
    public static final float DEFAULT_AIR_TEXT_SIZE = DEFAULT_TEXT_SIZE;
    public static final TextBehavior DEFAULT_SHOW_AIR_TEXT = TextBehavior.NEVER;
    public static final HorizontalAlignment DEFAULT_AIR_TEXT_ALIGN = HorizontalAlignment.CENTER;
    public static final int DEFAULT_AIR_BACKGROUND_X_OFFSET = 0;
    public static final int DEFAULT_AIR_BACKGROUND_Y_OFFSET = 0;
    public static final FillDirection DEFAULT_AIR_FILL_DIRECTION = FillDirection.HORIZONTAL;

    public BarRenderBehavior airBarBehavior;
    public HUDPositioning.BarPlacement airBarAnchor;
    public int airBackgroundWidth;
    public int airBackgroundHeight;
    public int airBarWidth;
    public int airBarHeight;
    public int airBarXOffset;
    public int airBarYOffset;
    public int airTotalXOffset;
    public int airTotalYOffset;
    public boolean enableAirIcon;
    public int airIconSize;
    public int airIconXOffset;
    public int airIconYOffset;
    public int airTextXOffset;
    public int airTextYOffset;
    public int airTextColor;
    public int airTextOpacity;
    public float airTextSize;
    public TextBehavior showAirText;
    public HorizontalAlignment airTextAlign;
    public int airBackgroundXOffset;
    public int airBackgroundYOffset;
    public FillDirection airFillDirection;

    private static transient ClientConfig instance; // Marked transient so GSON doesn't try to save it

    // Private constructor to enforce singleton via getInstance and initialize defaults
    private ClientConfig() {
        this.textScalingFactor = DEFAULT_TEXT_SCALING_FACTOR;
        this.globalTextColor = DEFAULT_TEXT_COLOR;
        this.globalTextOpacity = DEFAULT_TEXT_OPACITY;
        this.globalTextSize = DEFAULT_TEXT_SIZE;

        this.enableHealthBar = DEFAULT_ENABLE_HEALTH_BAR;
        this.healthBarAnchor = DEFAULT_HEALTH_BAR_ANCHOR;
        this.fadeHealthWhenFull = DEFAULT_FADE_HEALTH_WHEN_FULL;
        this.showHealthText = DEFAULT_SHOW_HEALTH_TEXT;
        this.healthTextAlign = DEFAULT_HEALTH_TEXT_ALIGN;
        this.enableHealthForeground = DEFAULT_ENABLE_HEALTH_FOREGROUND;
        this.enableHealthBackground = DEFAULT_ENABLE_HEALTH_BACKGROUND;
        this.healthFillDirection = DEFAULT_HEALTH_FILL_DIRECTION;
        this.healthBackgroundWidth = DEFAULT_HEALTH_BACKGROUND_WIDTH;
        this.healthBackgroundHeight = DEFAULT_HEALTH_BACKGROUND_HEIGHT;
        this.healthBarWidth = DEFAULT_HEALTH_BAR_WIDTH;
        this.healthBarHeight = DEFAULT_HEALTH_BAR_HEIGHT;
        this.healthOverlayWidth = DEFAULT_HEALTH_OVERLAY_WIDTH;
        this.healthOverlayHeight = DEFAULT_HEALTH_OVERLAY_HEIGHT;
        this.healthBarXOffset = DEFAULT_HEALTH_BAR_X_OFFSET;
        this.healthBarYOffset = DEFAULT_HEALTH_BAR_Y_OFFSET;
        this.healthTotalXOffset = DEFAULT_HEALTH_TOTAL_X_OFFSET;
        this.healthTotalYOffset = DEFAULT_HEALTH_TOTAL_Y_OFFSET;
        this.healthOverlayXOffset = DEFAULT_HEALTH_OVERLAY_X_OFFSET;
        this.healthOverlayYOffset = DEFAULT_HEALTH_OVERLAY_Y_OFFSET;
        this.healthBackgroundXOffset = DEFAULT_HEALTH_BACKGROUND_X_OFFSET;
        this.healthBackgroundYOffset = DEFAULT_HEALTH_BACKGROUND_Y_OFFSET;
        this.healthTextXOffset = DEFAULT_HEALTH_TEXT_X_OFFSET;
        this.healthTextYOffset = DEFAULT_HEALTH_TEXT_Y_OFFSET;
        this.healthTextColor = DEFAULT_HEALTH_TEXT_COLOR;
        this.healthTextOpacity = DEFAULT_HEALTH_TEXT_OPACITY;
        this.healthTextSize = DEFAULT_HEALTH_TEXT_SIZE;
        this.healthAbsorptionTextXOffset = DEFAULT_HEALTH_ABSORPTION_TEXT_X_OFFSET;
        this.healthAbsorptionTextYOffset = DEFAULT_HEALTH_ABSORPTION_TEXT_Y_OFFSET;

        this.staminaBarBehavior = DEFAULT_STAMINA_BAR_BEHAVIOR;
        this.mergeMountHealth = DEFAULT_MERGE_MOUNT_HEALTH;
        this.enableMountHealth = DEFAULT_ENABLE_MOUNT_HEALTH;
        this.staminaBarAnchor = DEFAULT_STAMINA_BAR_ANCHOR;
        this.fadeStaminaWhenFull = DEFAULT_FADE_STAMINA_WHEN_FULL;
        this.showStaminaText = DEFAULT_SHOW_STAMINA_TEXT;
        this.staminaTextAlign = DEFAULT_STAMINA_TEXT_ALIGN;
        this.enableStaminaForeground = DEFAULT_ENABLE_STAMINA_FOREGROUND;
        this.enableStaminaBackground = DEFAULT_ENABLE_STAMINA_BACKGROUND;
        this.staminaFillDirection = DEFAULT_STAMINA_FILL_DIRECTION;
        this.staminaBackgroundWidth = DEFAULT_STAMINA_BACKGROUND_WIDTH;
        this.staminaBackgroundHeight = DEFAULT_STAMINA_BACKGROUND_HEIGHT;
        this.staminaBarWidth = DEFAULT_STAMINA_BAR_WIDTH;
        this.staminaBarHeight = DEFAULT_STAMINA_BAR_HEIGHT;
        this.staminaOverlayWidth = DEFAULT_STAMINA_OVERLAY_WIDTH;
        this.staminaOverlayHeight = DEFAULT_STAMINA_OVERLAY_HEIGHT;
        this.staminaOverlayXOffset = DEFAULT_STAMINA_OVERLAY_X_OFFSET;
        this.staminaOverlayYOffset = DEFAULT_STAMINA_OVERLAY_Y_OFFSET;
        this.staminaBarXOffset = DEFAULT_STAMINA_BAR_X_OFFSET;
        this.staminaBarYOffset = DEFAULT_STAMINA_BAR_Y_OFFSET;
        this.staminaTotalXOffset = DEFAULT_STAMINA_TOTAL_X_OFFSET;
        this.staminaTotalYOffset = DEFAULT_STAMINA_TOTAL_Y_OFFSET;
        this.staminaBackgroundXOffset = DEFAULT_STAMINA_BACKGROUND_X_OFFSET;
        this.staminaBackgroundYOffset = DEFAULT_STAMINA_BACKGROUND_Y_OFFSET;
        this.staminaTextXOffset = DEFAULT_STAMINA_TEXT_X_OFFSET;
        this.staminaTextYOffset = DEFAULT_STAMINA_TEXT_Y_OFFSET;
        this.staminaTextColor = DEFAULT_STAMINA_TEXT_COLOR;
        this.staminaTextOpacity = DEFAULT_STAMINA_TEXT_OPACITY;
        this.staminaTextSize = DEFAULT_STAMINA_TEXT_SIZE;

        this.manaBarBehavior = DEFAULT_MANA_BAR_BEHAVIOR;
        this.manaBarAnchor = DEFAULT_MANA_BAR_ANCHOR;
        this.fadeManaWhenFull = DEFAULT_FADE_MANA_WHEN_FULL;
        this.showManaText = DEFAULT_SHOW_MANA_TEXT;
        this.manaTextAlign = DEFAULT_MANA_TEXT_ALIGN;
        this.enableManaForeground = DEFAULT_ENABLE_MANA_FOREGROUND;
        this.enableManaBackground = DEFAULT_ENABLE_MANA_BACKGROUND;
        this.manaFillDirection = DEFAULT_MANA_FILL_DIRECTION;
        this.manaBackgroundWidth = DEFAULT_MANA_BACKGROUND_WIDTH;
        this.manaBackgroundHeight = DEFAULT_MANA_BACKGROUND_HEIGHT;
        this.manaBarWidth = DEFAULT_MANA_BAR_WIDTH;
        this.manaBarHeight = DEFAULT_MANA_BAR_HEIGHT;
        this.manaOverlayWidth = DEFAULT_MANA_OVERLAY_WIDTH;
        this.manaOverlayHeight = DEFAULT_MANA_OVERLAY_HEIGHT;
        this.manaBarXOffset = DEFAULT_MANA_BAR_X_OFFSET;
        this.manaBarYOffset = DEFAULT_MANA_BAR_Y_OFFSET;
        this.manaTotalXOffset = DEFAULT_MANA_TOTAL_X_OFFSET;
        this.manaTotalYOffset = DEFAULT_MANA_TOTAL_Y_OFFSET;
        this.manaOverlayXOffset = DEFAULT_MANA_OVERLAY_X_OFFSET;
        this.manaOverlayYOffset = DEFAULT_MANA_OVERLAY_Y_OFFSET;
        this.manaBackgroundXOffset = DEFAULT_MANA_BACKGROUND_X_OFFSET;
        this.manaBackgroundYOffset = DEFAULT_MANA_BACKGROUND_Y_OFFSET;
        this.manaTextXOffset = DEFAULT_MANA_TEXT_X_OFFSET;
        this.manaTextYOffset = DEFAULT_MANA_TEXT_Y_OFFSET;
        this.manaTextColor = DEFAULT_TEXT_COLOR;
        this.manaTextOpacity = DEFAULT_TEXT_OPACITY;
        this.manaTextSize = DEFAULT_TEXT_SIZE;

        this.armorBarBehavior = DEFAULT_ARMOR_BAR_BEHAVIOR;
        this.armorBarAnchor = DEFAULT_ARMOR_BAR_ANCHOR;
        this.maxExpectedArmor = DEFAULT_MAX_EXPECTED_ARMOR;
        this.maxExpectedProt = DEFAULT_MAX_EXPECTED_PROT;
        this.armorBackgroundWidth = DEFAULT_ARMOR_BACKGROUND_WIDTH;
        this.armorBackgroundHeight = DEFAULT_ARMOR_BACKGROUND_HEIGHT;
        this.armorBarWidth = DEFAULT_ARMOR_BAR_WIDTH;
        this.armorBarHeight = DEFAULT_ARMOR_BAR_HEIGHT;
        this.armorBarXOffset = DEFAULT_ARMOR_BAR_X_OFFSET;
        this.armorBarYOffset = DEFAULT_ARMOR_BAR_Y_OFFSET;
        this.armorTotalXOffset = DEFAULT_ARMOR_TOTAL_X_OFFSET;
        this.armorTotalYOffset = DEFAULT_ARMOR_TOTAL_Y_OFFSET;
        this.enableArmorIcon = DEFAULT_ENABLE_ARMOR_ICON;
        this.armorIconSize = DEFAULT_ARMOR_ICON_SIZE;
        this.protOverlayAnimationCycles = DEFAULT_PROT_OVERLAY_ANIMATION_CYCLES;
        this.protOverlayFrameHeight = DEFAULT_PROT_OVERLAY_FRAME_HEIGHT;
        this.armorIconXOffset = DEFAULT_ARMOR_ICON_X_OFFSET;
        this.armorIconYOffset = DEFAULT_ARMOR_ICON_Y_OFFSET;
        this.armorTextXOffset = DEFAULT_ARMOR_TEXT_X_OFFSET;
        this.armorTextYOffset = DEFAULT_ARMOR_TEXT_Y_OFFSET;
        this.armorTextColor = DEFAULT_ARMOR_TEXT_COLOR;
        this.armorTextOpacity = DEFAULT_ARMOR_TEXT_OPACITY;
        this.armorTextSize = DEFAULT_ARMOR_TEXT_SIZE;
        this.showArmorText = DEFAULT_SHOW_ARMOR_TEXT;
        this.armorTextAlign = DEFAULT_ARMOR_TEXT_ALIGN;
        this.armorBackgroundXOffset = DEFAULT_ARMOR_BACKGROUND_X_OFFSET;
        this.armorBackgroundYOffset = DEFAULT_ARMOR_BACKGROUND_Y_OFFSET;

        this.airBarBehavior = DEFAULT_AIR_BAR_BEHAVIOR;
        this.airBarAnchor = DEFAULT_AIR_BAR_ANCHOR;
        this.airBackgroundWidth = DEFAULT_AIR_BACKGROUND_WIDTH;
        this.airBackgroundHeight = DEFAULT_AIR_BACKGROUND_HEIGHT;
        this.airBarWidth = DEFAULT_AIR_BAR_WIDTH;
        this.airBarHeight = DEFAULT_AIR_BAR_HEIGHT;
        this.airBarXOffset = DEFAULT_AIR_BAR_X_OFFSET;
        this.airBarYOffset = DEFAULT_AIR_BAR_Y_OFFSET;
        this.airTotalXOffset = DEFAULT_AIR_TOTAL_X_OFFSET;
        this.airTotalYOffset = DEFAULT_AIR_TOTAL_Y_OFFSET;
        this.enableAirIcon = DEFAULT_ENABLE_AIR_ICON;
        this.airIconSize = DEFAULT_AIR_ICON_SIZE;
        this.airIconXOffset = DEFAULT_AIR_ICON_X_OFFSET;
        this.airIconYOffset = DEFAULT_AIR_ICON_Y_OFFSET;
        this.airTextXOffset = DEFAULT_AIR_TEXT_X_OFFSET;
        this.airTextYOffset = DEFAULT_AIR_TEXT_Y_OFFSET;
        this.airTextColor = DEFAULT_AIR_TEXT_COLOR;
        this.airTextOpacity = DEFAULT_AIR_TEXT_OPACITY;
        this.airTextSize = DEFAULT_AIR_TEXT_SIZE;
        this.showAirText = DEFAULT_SHOW_AIR_TEXT;
        this.airTextAlign = DEFAULT_AIR_TEXT_ALIGN;
        this.airBackgroundXOffset = DEFAULT_AIR_BACKGROUND_X_OFFSET;
        this.airBackgroundYOffset = DEFAULT_AIR_BACKGROUND_Y_OFFSET;
        this.airFillDirection = DEFAULT_AIR_FILL_DIRECTION;
    }

    public static void setConfigPath(Path path) {
        if (CONFIG_FILE_PATH != null && !CONFIG_FILE_PATH.equals(path)) {
            DynamicResourceBars.LOGGER.warn("ClientConfig path is being changed after initial setup. This might indicate an issue.");
        }
        CONFIG_FILE_PATH = path;
    }

    public static ClientConfig getInstance() {
        if (instance == null) {
            if (CONFIG_FILE_PATH == null) {
                DynamicResourceBars.LOGGER.error("CRITICAL: ClientConfig.CONFIG_FILE_PATH was not initialized before getInstance() was called. Config will not be saved or loaded correctly.");
                throw new IllegalStateException("ClientConfig.CONFIG_FILE_PATH must be set before getInstance() is called.");
            }
            instance = load();
        }
        return instance;
    }

    private static ClientConfig load() {
        ClientConfig loadedConfig = null;
        boolean newConfigCreated = false;

        if (Files.exists(CONFIG_FILE_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE_PATH)) {
                loadedConfig = GSON.fromJson(reader, ClientConfig.class);
                if (loadedConfig == null) { // GSON might return null for empty or malformed JSON
                    DynamicResourceBars.LOGGER.warn("Config file {} was empty or malformed. Creating new default config.", CONFIG_FILE_PATH);
                    loadedConfig = new ClientConfig(); // Create a new instance with defaults
                    newConfigCreated = true;
                }
            } catch (Exception e) {
                DynamicResourceBars.LOGGER.error("Failed to load client config from {}. A new default config will be created. Error: ", CONFIG_FILE_PATH, e);
                loadedConfig = new ClientConfig(); // Create a new instance with defaults
                newConfigCreated = true;
            }
        } else {
            DynamicResourceBars.LOGGER.info("No config file found at {}. Creating new default config.", CONFIG_FILE_PATH);
            loadedConfig = new ClientConfig(); // Create a new instance with defaults
            newConfigCreated = true;
        }

        // Ensure all fields are present, applying defaults for any missing ones
        // This is a simple way to handle upgrades where new config options are added
        // A more sophisticated approach might involve version numbers and explicit migration
        boolean modifiedByDefaults = ensureDefaults(loadedConfig);


        if (newConfigCreated || modifiedByDefaults) {
            DynamicResourceBars.LOGGER.info("Saving new or updated default config to {}.", CONFIG_FILE_PATH);
            loadedConfig.save(); // Save if it's new or if defaults were applied
        }

        return loadedConfig;
    }
    
    // Helper to ensure all fields have values, applying defaults if necessary
    // This makes the config resilient to being manually edited with missing fields
    private static boolean ensureDefaults(ClientConfig cfg) {
        boolean modified = false;
        // For each field, check if it's null (GSON might leave it null if not in JSON)
        // or if it's a value that indicates it needs a default (e.g. enums being null)
        // This is verbose but clear. A more reflection-based approach is possible but complex.

        // General - textScalingFactor is double, defaults are handled by constructor/GSON field init.

        // Health
        if (cfg.healthBarAnchor == null) { cfg.healthBarAnchor = DEFAULT_HEALTH_BAR_ANCHOR; modified = true; }
        if (cfg.showHealthText == null) { cfg.showHealthText = DEFAULT_SHOW_HEALTH_TEXT; modified = true; }
        if (cfg.healthTextAlign == null) { cfg.healthTextAlign = DEFAULT_HEALTH_TEXT_ALIGN; modified = true; }
        if (cfg.healthFillDirection == null) { cfg.healthFillDirection = DEFAULT_HEALTH_FILL_DIRECTION; modified = true; }
        
        // Ensure health overlay dimensions are within valid ranges
        if (cfg.healthOverlayWidth > 256) { cfg.healthOverlayWidth = 256; modified = true; }
        if (cfg.healthOverlayHeight > 256) { cfg.healthOverlayHeight = 256; modified = true; }
        if (cfg.healthOverlayWidth < 1) { cfg.healthOverlayWidth = DEFAULT_HEALTH_OVERLAY_WIDTH; modified = true; }
        if (cfg.healthOverlayHeight < 1) { cfg.healthOverlayHeight = DEFAULT_HEALTH_OVERLAY_HEIGHT; modified = true; }

        // Stamina
        if (cfg.staminaBarBehavior == null) { cfg.staminaBarBehavior = DEFAULT_STAMINA_BAR_BEHAVIOR; modified = true; }
        if (cfg.staminaBarAnchor == null) { cfg.staminaBarAnchor = DEFAULT_STAMINA_BAR_ANCHOR; modified = true; }
        if (cfg.showStaminaText == null) { cfg.showStaminaText = DEFAULT_SHOW_STAMINA_TEXT; modified = true; }
        if (cfg.staminaTextAlign == null) { cfg.staminaTextAlign = DEFAULT_STAMINA_TEXT_ALIGN; modified = true; }
        if (cfg.staminaFillDirection == null) { cfg.staminaFillDirection = DEFAULT_STAMINA_FILL_DIRECTION; modified = true; }
        
        // Ensure stamina overlay dimensions are within valid ranges
        if (cfg.staminaOverlayWidth > 256) { cfg.staminaOverlayWidth = 256; modified = true; }
        if (cfg.staminaOverlayHeight > 256) { cfg.staminaOverlayHeight = 256; modified = true; }
        if (cfg.staminaOverlayWidth < 1) { cfg.staminaOverlayWidth = DEFAULT_STAMINA_OVERLAY_WIDTH; modified = true; }
        if (cfg.staminaOverlayHeight < 1) { cfg.staminaOverlayHeight = DEFAULT_STAMINA_OVERLAY_HEIGHT; modified = true; }

        // Mana
        if (cfg.manaBarAnchor == null) { cfg.manaBarAnchor = DEFAULT_MANA_BAR_ANCHOR; modified = true; }
        if (cfg.showManaText == null) { cfg.showManaText = DEFAULT_SHOW_MANA_TEXT; modified = true; }
        if (cfg.manaTextAlign == null) { cfg.manaTextAlign = DEFAULT_MANA_TEXT_ALIGN; modified = true; }
        if (cfg.manaFillDirection == null) { cfg.manaFillDirection = DEFAULT_MANA_FILL_DIRECTION; modified = true; }
        if (cfg.manaBarBehavior == null) { cfg.manaBarBehavior = DEFAULT_MANA_BAR_BEHAVIOR; modified = true; }
        
        // Ensure mana overlay dimensions are within valid ranges
        if (cfg.manaOverlayWidth > 256) { cfg.manaOverlayWidth = 256; modified = true; }
        if (cfg.manaOverlayHeight > 256) { cfg.manaOverlayHeight = 256; modified = true; }
        if (cfg.manaOverlayWidth < 1) { cfg.manaOverlayWidth = DEFAULT_MANA_OVERLAY_WIDTH; modified = true; }
        if (cfg.manaOverlayHeight < 1) { cfg.manaOverlayHeight = DEFAULT_MANA_OVERLAY_HEIGHT; modified = true; }

        // Armor
        if (cfg.armorBarBehavior == null) { cfg.armorBarBehavior = DEFAULT_ARMOR_BAR_BEHAVIOR; modified = true; }
        if (cfg.armorBarAnchor == null) { cfg.armorBarAnchor = DEFAULT_ARMOR_BAR_ANCHOR; modified = true; }
        if (cfg.showArmorText == null) { cfg.showArmorText = DEFAULT_SHOW_ARMOR_TEXT; modified = true; }
        if (cfg.armorTextAlign == null) { cfg.armorTextAlign = DEFAULT_ARMOR_TEXT_ALIGN; modified = true; }
        
        // Air
        if (cfg.airBarBehavior == null) { cfg.airBarBehavior = DEFAULT_AIR_BAR_BEHAVIOR; modified = true; }
        if (cfg.airBarAnchor == null) { cfg.airBarAnchor = DEFAULT_AIR_BAR_ANCHOR; modified = true; }
        if (cfg.showAirText == null) { cfg.showAirText = DEFAULT_SHOW_AIR_TEXT; modified = true; }
        if (cfg.airTextAlign == null) { cfg.airTextAlign = DEFAULT_AIR_TEXT_ALIGN; modified = true; }
        if (cfg.airFillDirection == null) { cfg.airFillDirection = DEFAULT_AIR_FILL_DIRECTION; modified = true; }

        // Primitive types like int, boolean, double will have their Java defaults (0, false, 0.0)
        // if not present in JSON and not initialized by GSON to the POJO's initialized values.
        // GSON usually respects field initializers if the field is missing in JSON.
        // The constructor already sets all defaults, so `new ClientConfig()` handles this for primitives.
        // This `ensureDefaults` method is primarily for making sure enum (Object) fields are not null 
        // if the JSON was incomplete or manually edited to remove them.

        return modified;
    }


    public void save() {
        if (CONFIG_FILE_PATH == null) {
            DynamicResourceBars.LOGGER.error("ClientConfig.CONFIG_FILE_PATH is null, cannot save config.");
            return;
        }
        try {
            Files.createDirectories(CONFIG_FILE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            DynamicResourceBars.LOGGER.error("Failed to save client config to {}:", CONFIG_FILE_PATH, e);
        }
    }
}
