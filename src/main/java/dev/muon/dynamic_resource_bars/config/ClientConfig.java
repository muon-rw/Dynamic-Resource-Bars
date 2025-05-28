package dev.muon.dynamic_resource_bars.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.muon.dynamic_resource_bars.DynamicResourceBars; // For logging
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;
import dev.muon.dynamic_resource_bars.util.TextBehavior;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import dev.muon.dynamic_resource_bars.util.FillDirection;

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
    public static final int DEFAULT_HEALTH_BAR_ANIMATION_CYCLES = 32;
    public static final int DEFAULT_HEALTH_BAR_FRAME_HEIGHT = 32;
    public static final int DEFAULT_HEALTH_OVERLAY_WIDTH = 80;
    public static final int DEFAULT_HEALTH_OVERLAY_HEIGHT = 10;
    public static final int DEFAULT_HEALTH_BAR_X_OFFSET = 3;
    public static final int DEFAULT_HEALTH_BAR_Y_OFFSET = 3;
    public static final int DEFAULT_HEALTH_TOTAL_X_OFFSET = 0;
    public static final int DEFAULT_HEALTH_TOTAL_Y_OFFSET = 0;
    public static final int DEFAULT_HEALTH_OVERLAY_X_OFFSET = 0;
    public static final int DEFAULT_HEALTH_OVERLAY_Y_OFFSET = -3;

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
    public int healthBarAnimationCycles;
    public int healthBarFrameHeight;
    public int healthOverlayWidth;
    public int healthOverlayHeight;
    public int healthBarXOffset;
    public int healthBarYOffset;
    public int healthTotalXOffset;
    public int healthTotalYOffset;
    public int healthOverlayXOffset;
    public int healthOverlayYOffset;

    // Stamina Defaults & Fields
    public static final boolean DEFAULT_ENABLE_STAMINA_BAR = true;
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
    public static final int DEFAULT_STAMINA_BAR_ANIMATION_CYCLES = 32;
    public static final int DEFAULT_STAMINA_BAR_FRAME_HEIGHT = 32;
    public static final int DEFAULT_STAMINA_OVERLAY_WIDTH = 80;
    public static final int DEFAULT_STAMINA_OVERLAY_HEIGHT = 10;
    public static final int DEFAULT_STAMINA_OVERLAY_X_OFFSET = 0;
    public static final int DEFAULT_STAMINA_OVERLAY_Y_OFFSET = -3;
    public static final int DEFAULT_STAMINA_BAR_X_OFFSET = 3;
    public static final int DEFAULT_STAMINA_BAR_Y_OFFSET = 3;
    public static final int DEFAULT_STAMINA_TOTAL_X_OFFSET = -80;
    public static final int DEFAULT_STAMINA_TOTAL_Y_OFFSET = 0;

    public boolean enableStaminaBar;
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
    public int staminaBarAnimationCycles;
    public int staminaBarFrameHeight;
    public int staminaOverlayWidth;
    public int staminaOverlayHeight;
    public int staminaOverlayXOffset;
    public int staminaOverlayYOffset;
    public int staminaBarXOffset;
    public int staminaBarYOffset;
    public int staminaTotalXOffset;
    public int staminaTotalYOffset;

    // Mana Defaults & Fields
    public static final boolean DEFAULT_ENABLE_MANA_BAR = true;
    public static final HUDPositioning.BarPlacement DEFAULT_MANA_BAR_ANCHOR = HUDPositioning.BarPlacement.ABOVE_UTILITIES;
    public static final boolean DEFAULT_FADE_MANA_WHEN_FULL = true;
    public static final TextBehavior DEFAULT_SHOW_MANA_TEXT = TextBehavior.WHEN_NOT_FULL;
    public static final HorizontalAlignment DEFAULT_MANA_TEXT_ALIGN = HorizontalAlignment.CENTER;
    public static final boolean DEFAULT_ENABLE_MANA_FOREGROUND = true;
    public static final boolean DEFAULT_ENABLE_MANA_BACKGROUND = true;
    public static final FillDirection DEFAULT_MANA_FILL_DIRECTION = FillDirection.HORIZONTAL;
    public static final int DEFAULT_MANA_BACKGROUND_WIDTH = 80;
    public static final int DEFAULT_MANA_BACKGROUND_HEIGHT = 10;
    public static final int DEFAULT_MANA_BAR_WIDTH = 74;
    public static final int DEFAULT_MANA_BAR_HEIGHT = 4;
    public static final int DEFAULT_MANA_BAR_ANIMATION_CYCLES = 32;
    public static final int DEFAULT_MANA_BAR_FRAME_HEIGHT = 32;
    public static final int DEFAULT_MANA_OVERLAY_WIDTH = 80;
    public static final int DEFAULT_MANA_OVERLAY_HEIGHT = 10;
    public static final int DEFAULT_MANA_BAR_X_OFFSET = 3;
    public static final int DEFAULT_MANA_BAR_Y_OFFSET = 3;
    public static final int DEFAULT_MANA_TOTAL_X_OFFSET = -40;
    public static final int DEFAULT_MANA_TOTAL_Y_OFFSET = 0;
    public static final int DEFAULT_MANA_OVERLAY_X_OFFSET = 0;
    public static final int DEFAULT_MANA_OVERLAY_Y_OFFSET = -3;

    public boolean enableManaBar;
    public HUDPositioning.BarPlacement manaBarAnchor;
    public boolean fadeManaWhenFull;
    public TextBehavior showManaText;
    public HorizontalAlignment manaTextAlign;
    public boolean enableManaForeground;
    public boolean enableManaBackground;
    public FillDirection manaFillDirection;
    public int manaBackgroundWidth;
    public int manaBackgroundHeight;
    public int manaBarWidth;
    public int manaBarHeight;
    public int manaBarAnimationCycles;
    public int manaBarFrameHeight;
    public int manaOverlayWidth;
    public int manaOverlayHeight;
    public int manaBarXOffset;
    public int manaBarYOffset;
    public int manaTotalXOffset;
    public int manaTotalYOffset;
    public int manaOverlayXOffset;
    public int manaOverlayYOffset;

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
    public static final int DEFAULT_ARMOR_ICON_Y_OFFSET = 0;

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

    // Air Defaults & Fields
    public static final BarRenderBehavior DEFAULT_AIR_BAR_BEHAVIOR = BarRenderBehavior.VANILLA;
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
    public static final int DEFAULT_AIR_ICON_X_OFFSET = 0;
    public static final int DEFAULT_AIR_ICON_Y_OFFSET = 0;

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

    private static transient ClientConfig instance; // Marked transient so GSON doesn't try to save it

    // Private constructor to enforce singleton via getInstance and initialize defaults
    private ClientConfig() {
        this.textScalingFactor = DEFAULT_TEXT_SCALING_FACTOR;

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
        this.healthBarAnimationCycles = DEFAULT_HEALTH_BAR_ANIMATION_CYCLES;
        this.healthBarFrameHeight = DEFAULT_HEALTH_BAR_FRAME_HEIGHT;
        this.healthOverlayWidth = DEFAULT_HEALTH_OVERLAY_WIDTH;
        this.healthOverlayHeight = DEFAULT_HEALTH_OVERLAY_HEIGHT;
        this.healthBarXOffset = DEFAULT_HEALTH_BAR_X_OFFSET;
        this.healthBarYOffset = DEFAULT_HEALTH_BAR_Y_OFFSET;
        this.healthTotalXOffset = DEFAULT_HEALTH_TOTAL_X_OFFSET;
        this.healthTotalYOffset = DEFAULT_HEALTH_TOTAL_Y_OFFSET;
        this.healthOverlayXOffset = DEFAULT_HEALTH_OVERLAY_X_OFFSET;
        this.healthOverlayYOffset = DEFAULT_HEALTH_OVERLAY_Y_OFFSET;

        this.enableStaminaBar = DEFAULT_ENABLE_STAMINA_BAR;
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
        this.staminaBarAnimationCycles = DEFAULT_STAMINA_BAR_ANIMATION_CYCLES;
        this.staminaBarFrameHeight = DEFAULT_STAMINA_BAR_FRAME_HEIGHT;
        this.staminaOverlayWidth = DEFAULT_STAMINA_OVERLAY_WIDTH;
        this.staminaOverlayHeight = DEFAULT_STAMINA_OVERLAY_HEIGHT;
        this.staminaOverlayXOffset = DEFAULT_STAMINA_OVERLAY_X_OFFSET;
        this.staminaOverlayYOffset = DEFAULT_STAMINA_OVERLAY_Y_OFFSET;
        this.staminaBarXOffset = DEFAULT_STAMINA_BAR_X_OFFSET;
        this.staminaBarYOffset = DEFAULT_STAMINA_BAR_Y_OFFSET;
        this.staminaTotalXOffset = DEFAULT_STAMINA_TOTAL_X_OFFSET;
        this.staminaTotalYOffset = DEFAULT_STAMINA_TOTAL_Y_OFFSET;

        this.enableManaBar = DEFAULT_ENABLE_MANA_BAR;
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
        this.manaBarAnimationCycles = DEFAULT_MANA_BAR_ANIMATION_CYCLES;
        this.manaBarFrameHeight = DEFAULT_MANA_BAR_FRAME_HEIGHT;
        this.manaOverlayWidth = DEFAULT_MANA_OVERLAY_WIDTH;
        this.manaOverlayHeight = DEFAULT_MANA_OVERLAY_HEIGHT;
        this.manaBarXOffset = DEFAULT_MANA_BAR_X_OFFSET;
        this.manaBarYOffset = DEFAULT_MANA_BAR_Y_OFFSET;
        this.manaTotalXOffset = DEFAULT_MANA_TOTAL_X_OFFSET;
        this.manaTotalYOffset = DEFAULT_MANA_TOTAL_Y_OFFSET;
        this.manaOverlayXOffset = DEFAULT_MANA_OVERLAY_X_OFFSET;
        this.manaOverlayYOffset = DEFAULT_MANA_OVERLAY_Y_OFFSET;

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
        
        // Ensure mana overlay dimensions are within valid ranges
        if (cfg.manaOverlayWidth > 256) { cfg.manaOverlayWidth = 256; modified = true; }
        if (cfg.manaOverlayHeight > 256) { cfg.manaOverlayHeight = 256; modified = true; }
        if (cfg.manaOverlayWidth < 1) { cfg.manaOverlayWidth = DEFAULT_MANA_OVERLAY_WIDTH; modified = true; }
        if (cfg.manaOverlayHeight < 1) { cfg.manaOverlayHeight = DEFAULT_MANA_OVERLAY_HEIGHT; modified = true; }

        // Armor
        if (cfg.armorBarBehavior == null) { cfg.armorBarBehavior = DEFAULT_ARMOR_BAR_BEHAVIOR; modified = true; }
        if (cfg.armorBarAnchor == null) { cfg.armorBarAnchor = DEFAULT_ARMOR_BAR_ANCHOR; modified = true; }
        
        // Air
        if (cfg.airBarBehavior == null) { cfg.airBarBehavior = DEFAULT_AIR_BAR_BEHAVIOR; modified = true; }
        if (cfg.airBarAnchor == null) { cfg.airBarAnchor = DEFAULT_AIR_BAR_ANCHOR; modified = true; }

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
