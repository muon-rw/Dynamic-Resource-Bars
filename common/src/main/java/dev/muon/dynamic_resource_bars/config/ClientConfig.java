package dev.muon.dynamic_resource_bars.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.muon.dynamic_resource_bars.Constants; // For logging
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.util.AbsorptionDisplayMode;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;
import dev.muon.dynamic_resource_bars.util.TextBehavior;
import dev.muon.dynamic_resource_bars.util.FillDirection;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import dev.muon.dynamic_resource_bars.util.BarVisibility;
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

    private static final String COMBAT_ATTRIBUTES_MOD_ID = "combat_attributes";

    // One-way marker: flips to true the first time we load with Combat Attributes present.
    // Lets us upgrade the legacy stamina default (FOOD) to COMBAT_ATTRIBUTES exactly once,
    // without ever overriding choices the user made while CA was already on the classpath.
    public boolean combatAttributesSeen = false;

    /** Milliseconds the bar (and its text) holds at full opacity after a smart-fade trigger fires before it begins fading. */
    public static final int DEFAULT_FADE_HOLD_DURATION = 1500;
    public int fadeHoldDuration = DEFAULT_FADE_HOLD_DURATION;

    // Global text defaults
    public static final int DEFAULT_TEXT_COLOR = 0xFFFFFF; // White
    public static final int DEFAULT_TEXT_OPACITY = 200; // Out of 255
    public static final float DEFAULT_GLOBAL_TEXT_SIZE = 1.0f;
    public static final int DEFAULT_BAR_TEXT_WIDTH = 40;
    public static final int DEFAULT_BAR_TEXT_HEIGHT = 4;

    // Global text fields
    public int globalTextColor = DEFAULT_TEXT_COLOR;
    public int globalTextOpacity = DEFAULT_TEXT_OPACITY;
    public float globalTextSize = DEFAULT_GLOBAL_TEXT_SIZE;

    // Health Defaults & Fields
    public static final BarRenderBehavior DEFAULT_HEALTH_BAR_BEHAVIOR = BarRenderBehavior.CUSTOM;
    public static final HUDPositioning.BarPlacement DEFAULT_HEALTH_BAR_ANCHOR = HUDPositioning.BarPlacement.HEALTH;
    public static final BarVisibility DEFAULT_HEALTH_BAR_VISIBILITY = BarVisibility.ALWAYS;
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
    public static final int DEFAULT_HEALTH_TEXT_X_OFFSET = 20;
    public static final int DEFAULT_HEALTH_TEXT_Y_OFFSET = 3;
    public static final int DEFAULT_HEALTH_TEXT_COLOR = DEFAULT_TEXT_COLOR;
    public static final int DEFAULT_HEALTH_TEXT_OPACITY = DEFAULT_TEXT_OPACITY;
    public static final int DEFAULT_HEALTH_TEXT_WIDTH = DEFAULT_BAR_TEXT_WIDTH;
    public static final int DEFAULT_HEALTH_TEXT_HEIGHT = DEFAULT_BAR_TEXT_HEIGHT;
    public static final int DEFAULT_HEALTH_ABSORPTION_TEXT_X_OFFSET = 43;
    public static final int DEFAULT_HEALTH_ABSORPTION_TEXT_Y_OFFSET = 3;
    public static final int DEFAULT_HEALTH_ABSORPTION_TEXT_WIDTH = 30;
    public static final int DEFAULT_HEALTH_ABSORPTION_TEXT_HEIGHT = 4;
    public static final boolean DEFAULT_ENABLE_HEALTH_ABSORPTION_TEXT = true;
    public static final HorizontalAlignment DEFAULT_HEALTH_ABSORPTION_TEXT_ALIGN = HorizontalAlignment.RIGHT;
    public static final AbsorptionDisplayMode DEFAULT_HEALTH_ABSORPTION_DISPLAY_MODE = AbsorptionDisplayMode.OVERLAY;

    public BarRenderBehavior healthBarBehavior;
    public HUDPositioning.BarPlacement healthBarAnchor;
    public BarVisibility healthBarVisibility;
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
    public int healthTextWidth;
    public int healthTextHeight;
    public int healthAbsorptionTextXOffset;
    public int healthAbsorptionTextYOffset;
    public int healthAbsorptionTextWidth;
    public int healthAbsorptionTextHeight;
    public boolean enableHealthAbsorptionText;
    public HorizontalAlignment healthAbsorptionTextAlign;
    public AbsorptionDisplayMode healthAbsorptionDisplayMode;

    // Stamina Defaults & Fields
    public static final boolean DEFAULT_ENABLE_STAMINA_BAR = true;
    public static final StaminaBarBehavior DEFAULT_STAMINA_BAR_BEHAVIOR = StaminaBarBehavior.FOOD;
    public static final boolean DEFAULT_MERGE_MOUNT_HEALTH = true;
    public static final boolean DEFAULT_ENABLE_MOUNT_HEALTH = true;
    public static final HUDPositioning.BarPlacement DEFAULT_STAMINA_BAR_ANCHOR = HUDPositioning.BarPlacement.HUNGER;
    public static final BarVisibility DEFAULT_STAMINA_BAR_VISIBILITY = BarVisibility.ALWAYS;
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
    public static final int DEFAULT_STAMINA_TEXT_X_OFFSET = 20;
    public static final int DEFAULT_STAMINA_TEXT_Y_OFFSET = 3;
    public static final int DEFAULT_STAMINA_TEXT_COLOR = DEFAULT_TEXT_COLOR;
    public static final int DEFAULT_STAMINA_TEXT_OPACITY = DEFAULT_TEXT_OPACITY;
    public static final int DEFAULT_STAMINA_TEXT_WIDTH = DEFAULT_BAR_TEXT_WIDTH;
    public static final int DEFAULT_STAMINA_TEXT_HEIGHT = DEFAULT_BAR_TEXT_HEIGHT;

    public StaminaBarBehavior staminaBarBehavior;
    public boolean mergeMountHealth;
    public boolean enableMountHealth;
    public HUDPositioning.BarPlacement staminaBarAnchor;
    public BarVisibility staminaBarVisibility;
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
    public int staminaTextWidth;
    public int staminaTextHeight;

    // Mana Defaults & Fields
    public static final ManaBarBehavior DEFAULT_MANA_BAR_BEHAVIOR = ManaBarBehavior.COMBAT_ATTRIBUTES;
    public static final HUDPositioning.BarPlacement DEFAULT_MANA_BAR_ANCHOR = HUDPositioning.BarPlacement.ABOVE_UTILITIES;
    public static final boolean DEFAULT_ENABLE_MANA_BACKGROUND = true;
    public static final boolean DEFAULT_ENABLE_MANA_FOREGROUND = true;
    public static final BarVisibility DEFAULT_MANA_BAR_VISIBILITY = BarVisibility.SMART_FADE;
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
    public static final int DEFAULT_MANA_TEXT_X_OFFSET = 20;
    public static final int DEFAULT_MANA_TEXT_Y_OFFSET = 3;
    public static final int DEFAULT_MANA_TEXT_WIDTH = DEFAULT_BAR_TEXT_WIDTH;
    public static final int DEFAULT_MANA_TEXT_HEIGHT = DEFAULT_BAR_TEXT_HEIGHT;

    public ManaBarBehavior manaBarBehavior = DEFAULT_MANA_BAR_BEHAVIOR;
    public HUDPositioning.BarPlacement manaBarAnchor = DEFAULT_MANA_BAR_ANCHOR;
    public boolean enableManaBackground = DEFAULT_ENABLE_MANA_BACKGROUND;
    public boolean enableManaForeground = DEFAULT_ENABLE_MANA_FOREGROUND;
    public BarVisibility manaBarVisibility;
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
    public int manaTextWidth;
    public int manaTextHeight;

    // Armor Defaults & Fields
    public static final BarRenderBehavior DEFAULT_ARMOR_BAR_BEHAVIOR = BarRenderBehavior.HIDDEN;
    public static final HUDPositioning.BarPlacement DEFAULT_ARMOR_BAR_ANCHOR = HUDPositioning.BarPlacement.ARMOR;
    public static final int DEFAULT_MAX_EXPECTED_ARMOR = 20;
    public static final int DEFAULT_MAX_EXPECTED_PROT = 16;
    public static final boolean DEFAULT_ENABLE_ARMOR_BACKGROUND = true;
    public static final BarVisibility DEFAULT_ARMOR_BAR_VISIBILITY = BarVisibility.SMART_FADE; // armor uses "fade when empty" — overridden in renderer
    public static final FillDirection DEFAULT_ARMOR_FILL_DIRECTION = FillDirection.HORIZONTAL;
    public static final int DEFAULT_ARMOR_BACKGROUND_WIDTH = 80;
    public static final int DEFAULT_ARMOR_BACKGROUND_HEIGHT = 10;
    public static final int DEFAULT_ARMOR_BAR_WIDTH = 74;
    public static final int DEFAULT_ARMOR_BAR_HEIGHT = 4;
    public static final int DEFAULT_ARMOR_BAR_X_OFFSET = 3;
    public static final int DEFAULT_ARMOR_BAR_Y_OFFSET = 3;
    public static final int DEFAULT_ARMOR_TOTAL_X_OFFSET = 0;
    public static final int DEFAULT_ARMOR_TOTAL_Y_OFFSET = 0;
    public static final boolean DEFAULT_ENABLE_ARMOR_ICON = true;
    public static final int DEFAULT_ARMOR_ICON_WIDTH = 16;
    public static final int DEFAULT_ARMOR_ICON_HEIGHT = 16;
    public static final int DEFAULT_PROT_OVERLAY_ANIMATION_CYCLES = 16;
    public static final int DEFAULT_PROT_OVERLAY_FRAME_HEIGHT = 4;
    public static final int DEFAULT_ARMOR_ICON_X_OFFSET = 0;
    public static final int DEFAULT_ARMOR_ICON_Y_OFFSET = -4;
    public static final boolean DEFAULT_ENABLE_ARMOR_FOREGROUND = false;
    public static final int DEFAULT_ARMOR_OVERLAY_WIDTH = 80;
    public static final int DEFAULT_ARMOR_OVERLAY_HEIGHT = 10;
    public static final int DEFAULT_ARMOR_OVERLAY_X_OFFSET = 0;
    public static final int DEFAULT_ARMOR_OVERLAY_Y_OFFSET = -3;
    public static final int DEFAULT_ARMOR_TEXT_X_OFFSET = 20;
    public static final int DEFAULT_ARMOR_TEXT_Y_OFFSET = 3;
    public static final int DEFAULT_ARMOR_TEXT_COLOR = DEFAULT_TEXT_COLOR;
    public static final int DEFAULT_ARMOR_TEXT_OPACITY = DEFAULT_TEXT_OPACITY;
    public static final int DEFAULT_ARMOR_TEXT_WIDTH = DEFAULT_BAR_TEXT_WIDTH;
    public static final int DEFAULT_ARMOR_TEXT_HEIGHT = DEFAULT_BAR_TEXT_HEIGHT;
    public static final TextBehavior DEFAULT_SHOW_ARMOR_TEXT = TextBehavior.NEVER;
    public static final HorizontalAlignment DEFAULT_ARMOR_TEXT_ALIGN = HorizontalAlignment.CENTER;
    public static final int DEFAULT_ARMOR_BACKGROUND_X_OFFSET = 0;
    public static final int DEFAULT_ARMOR_BACKGROUND_Y_OFFSET = 0;

    public BarRenderBehavior armorBarBehavior;
    public HUDPositioning.BarPlacement armorBarAnchor;
    public int maxExpectedArmor;
    public int maxExpectedProt;
    public boolean enableArmorBackground;
    public BarVisibility armorBarVisibility;
    public FillDirection armorFillDirection;
    public int armorBackgroundWidth;
    public int armorBackgroundHeight;
    public int armorBarWidth;
    public int armorBarHeight;
    public int armorBarXOffset;
    public int armorBarYOffset;
    public int armorTotalXOffset;
    public int armorTotalYOffset;
    public boolean enableArmorIcon;
    public int armorIconWidth;
    public int armorIconHeight;
    public int protOverlayAnimationCycles;
    public int protOverlayFrameHeight;
    public int armorIconXOffset;
    public int armorIconYOffset;
    public boolean enableArmorForeground;
    public int armorOverlayWidth;
    public int armorOverlayHeight;
    public int armorOverlayXOffset;
    public int armorOverlayYOffset;
    public int armorTextXOffset;
    public int armorTextYOffset;
    public int armorTextColor;
    public int armorTextOpacity;
    public int armorTextWidth;
    public int armorTextHeight;
    public TextBehavior showArmorText;
    public HorizontalAlignment armorTextAlign;
    public int armorBackgroundXOffset;
    public int armorBackgroundYOffset;

    // Air Defaults & Fields
    public static final BarRenderBehavior DEFAULT_AIR_BAR_BEHAVIOR = BarRenderBehavior.CUSTOM;
    public static final HUDPositioning.BarPlacement DEFAULT_AIR_BAR_ANCHOR = HUDPositioning.BarPlacement.AIR;
    public static final boolean DEFAULT_ENABLE_AIR_BACKGROUND = true;
    public static final BarVisibility DEFAULT_AIR_BAR_VISIBILITY = BarVisibility.SMART_FADE;
    public static final int DEFAULT_AIR_BACKGROUND_WIDTH = 80;
    public static final int DEFAULT_AIR_BACKGROUND_HEIGHT = 10;
    public static final int DEFAULT_AIR_BAR_WIDTH = 74;
    public static final int DEFAULT_AIR_BAR_HEIGHT = 4;
    public static final int DEFAULT_AIR_BAR_X_OFFSET = 3;
    public static final int DEFAULT_AIR_BAR_Y_OFFSET = 3;
    public static final int DEFAULT_AIR_TOTAL_X_OFFSET = -80;
    public static final int DEFAULT_AIR_TOTAL_Y_OFFSET = 0;
    public static final boolean DEFAULT_ENABLE_AIR_ICON = true;
    public static final int DEFAULT_AIR_ICON_WIDTH = 16;
    public static final int DEFAULT_AIR_ICON_HEIGHT = 16;
    public static final int DEFAULT_AIR_ICON_X_OFFSET = 66;
    public static final int DEFAULT_AIR_ICON_Y_OFFSET = -4;
    public static final boolean DEFAULT_ENABLE_AIR_FOREGROUND = false;
    public static final int DEFAULT_AIR_OVERLAY_WIDTH = 80;
    public static final int DEFAULT_AIR_OVERLAY_HEIGHT = 10;
    public static final int DEFAULT_AIR_OVERLAY_X_OFFSET = 0;
    public static final int DEFAULT_AIR_OVERLAY_Y_OFFSET = -3;
    public static final int DEFAULT_AIR_TEXT_X_OFFSET = 20;
    public static final int DEFAULT_AIR_TEXT_Y_OFFSET = 3;
    public static final int DEFAULT_AIR_TEXT_COLOR = DEFAULT_TEXT_COLOR;
    public static final int DEFAULT_AIR_TEXT_OPACITY = DEFAULT_TEXT_OPACITY;
    public static final int DEFAULT_AIR_TEXT_WIDTH = DEFAULT_BAR_TEXT_WIDTH;
    public static final int DEFAULT_AIR_TEXT_HEIGHT = DEFAULT_BAR_TEXT_HEIGHT;
    public static final TextBehavior DEFAULT_SHOW_AIR_TEXT = TextBehavior.NEVER;
    public static final HorizontalAlignment DEFAULT_AIR_TEXT_ALIGN = HorizontalAlignment.CENTER;
    public static final int DEFAULT_AIR_BACKGROUND_X_OFFSET = 0;
    public static final int DEFAULT_AIR_BACKGROUND_Y_OFFSET = 0;
    public static final FillDirection DEFAULT_AIR_FILL_DIRECTION = FillDirection.HORIZONTAL;

    public BarRenderBehavior airBarBehavior;
    public HUDPositioning.BarPlacement airBarAnchor;
    public boolean enableAirBackground;
    public BarVisibility airBarVisibility;
    public int airBackgroundWidth;
    public int airBackgroundHeight;
    public int airBarWidth;
    public int airBarHeight;
    public int airBarXOffset;
    public int airBarYOffset;
    public int airTotalXOffset;
    public int airTotalYOffset;
    public boolean enableAirIcon;
    public int airIconWidth;
    public int airIconHeight;
    public int airIconXOffset;
    public int airIconYOffset;
    public boolean enableAirForeground;
    public int airOverlayWidth;
    public int airOverlayHeight;
    public int airOverlayXOffset;
    public int airOverlayYOffset;
    public int airTextXOffset;
    public int airTextYOffset;
    public int airTextColor;
    public int airTextOpacity;
    public int airTextWidth;
    public int airTextHeight;
    public TextBehavior showAirText;
    public HorizontalAlignment airTextAlign;
    public int airBackgroundXOffset;
    public int airBackgroundYOffset;
    public FillDirection airFillDirection;

    private static transient ClientConfig instance; // Marked transient so GSON doesn't try to save it

    /** Builds a fresh, defaults-only instance — used by editor "reset to defaults" actions. */
    public static ClientConfig createDefaults() {
        ClientConfig cfg = new ClientConfig();
        applyCombatAttributesDefaults(cfg);
        return cfg;
    }

    /**
     * Upgrades the legacy stamina default (FOOD) to COMBAT_ATTRIBUTES the first time we observe
     * CA on the classpath. Runs at most once per config — once {@link #combatAttributesSeen}
     * flips to true, the user's choices are treated as deliberate and never overridden, even if
     * CA is later removed and re-added.
     *
     * <p>Mana is not migrated: its default is already COMBAT_ATTRIBUTES, and the only non-default
     * value (OFF) is necessarily a deliberate user choice.
     *
     * @return true if any field changed (caller should persist).
     */
    private static boolean applyCombatAttributesDefaults(ClientConfig cfg) {
        if (!Services.PLATFORM.isModLoaded(COMBAT_ATTRIBUTES_MOD_ID)) return false;
        if (cfg.combatAttributesSeen) return false;
        if (cfg.staminaBarBehavior == StaminaBarBehavior.FOOD) {
            cfg.staminaBarBehavior = StaminaBarBehavior.COMBAT_ATTRIBUTES;
        }
        cfg.combatAttributesSeen = true;
        return true;
    }

    // Private constructor to enforce singleton via getInstance and initialize defaults
    private ClientConfig() {
        this.combatAttributesSeen = false;
        this.fadeHoldDuration = DEFAULT_FADE_HOLD_DURATION;
        this.globalTextColor = DEFAULT_TEXT_COLOR;
        this.globalTextOpacity = DEFAULT_TEXT_OPACITY;
        this.globalTextSize = DEFAULT_GLOBAL_TEXT_SIZE;

        this.healthBarBehavior = DEFAULT_HEALTH_BAR_BEHAVIOR;
        this.healthBarAnchor = DEFAULT_HEALTH_BAR_ANCHOR;
        this.healthBarVisibility = DEFAULT_HEALTH_BAR_VISIBILITY;
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
        this.healthTextWidth = DEFAULT_HEALTH_TEXT_WIDTH;
        this.healthTextHeight = DEFAULT_HEALTH_TEXT_HEIGHT;
        this.healthAbsorptionTextXOffset = DEFAULT_HEALTH_ABSORPTION_TEXT_X_OFFSET;
        this.healthAbsorptionTextYOffset = DEFAULT_HEALTH_ABSORPTION_TEXT_Y_OFFSET;
        this.healthAbsorptionTextWidth = DEFAULT_HEALTH_ABSORPTION_TEXT_WIDTH;
        this.healthAbsorptionTextHeight = DEFAULT_HEALTH_ABSORPTION_TEXT_HEIGHT;
        this.enableHealthAbsorptionText = DEFAULT_ENABLE_HEALTH_ABSORPTION_TEXT;
        this.healthAbsorptionTextAlign = DEFAULT_HEALTH_ABSORPTION_TEXT_ALIGN;
        this.healthAbsorptionDisplayMode = DEFAULT_HEALTH_ABSORPTION_DISPLAY_MODE;

        this.staminaBarBehavior = DEFAULT_STAMINA_BAR_BEHAVIOR;
        this.mergeMountHealth = DEFAULT_MERGE_MOUNT_HEALTH;
        this.enableMountHealth = DEFAULT_ENABLE_MOUNT_HEALTH;
        this.staminaBarAnchor = DEFAULT_STAMINA_BAR_ANCHOR;
        this.staminaBarVisibility = DEFAULT_STAMINA_BAR_VISIBILITY;
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
        this.staminaTextWidth = DEFAULT_STAMINA_TEXT_WIDTH;
        this.staminaTextHeight = DEFAULT_STAMINA_TEXT_HEIGHT;

        this.manaBarBehavior = DEFAULT_MANA_BAR_BEHAVIOR;
        this.manaBarAnchor = DEFAULT_MANA_BAR_ANCHOR;
        this.manaBarVisibility = DEFAULT_MANA_BAR_VISIBILITY;
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
        this.manaTextWidth = DEFAULT_MANA_TEXT_WIDTH;
        this.manaTextHeight = DEFAULT_MANA_TEXT_HEIGHT;

        this.armorBarBehavior = DEFAULT_ARMOR_BAR_BEHAVIOR;
        this.armorBarAnchor = DEFAULT_ARMOR_BAR_ANCHOR;
        this.maxExpectedArmor = DEFAULT_MAX_EXPECTED_ARMOR;
        this.maxExpectedProt = DEFAULT_MAX_EXPECTED_PROT;
        this.enableArmorBackground = DEFAULT_ENABLE_ARMOR_BACKGROUND;
        this.armorBarVisibility = DEFAULT_ARMOR_BAR_VISIBILITY;
        this.armorFillDirection = DEFAULT_ARMOR_FILL_DIRECTION;
        this.armorBackgroundWidth = DEFAULT_ARMOR_BACKGROUND_WIDTH;
        this.armorBackgroundHeight = DEFAULT_ARMOR_BACKGROUND_HEIGHT;
        this.armorBarWidth = DEFAULT_ARMOR_BAR_WIDTH;
        this.armorBarHeight = DEFAULT_ARMOR_BAR_HEIGHT;
        this.armorBarXOffset = DEFAULT_ARMOR_BAR_X_OFFSET;
        this.armorBarYOffset = DEFAULT_ARMOR_BAR_Y_OFFSET;
        this.armorTotalXOffset = DEFAULT_ARMOR_TOTAL_X_OFFSET;
        this.armorTotalYOffset = DEFAULT_ARMOR_TOTAL_Y_OFFSET;
        this.enableArmorIcon = DEFAULT_ENABLE_ARMOR_ICON;
        this.armorIconWidth = DEFAULT_ARMOR_ICON_WIDTH;
        this.armorIconHeight = DEFAULT_ARMOR_ICON_HEIGHT;
        this.protOverlayAnimationCycles = DEFAULT_PROT_OVERLAY_ANIMATION_CYCLES;
        this.protOverlayFrameHeight = DEFAULT_PROT_OVERLAY_FRAME_HEIGHT;
        this.armorIconXOffset = DEFAULT_ARMOR_ICON_X_OFFSET;
        this.armorIconYOffset = DEFAULT_ARMOR_ICON_Y_OFFSET;
        this.enableArmorForeground = DEFAULT_ENABLE_ARMOR_FOREGROUND;
        this.armorOverlayWidth = DEFAULT_ARMOR_OVERLAY_WIDTH;
        this.armorOverlayHeight = DEFAULT_ARMOR_OVERLAY_HEIGHT;
        this.armorOverlayXOffset = DEFAULT_ARMOR_OVERLAY_X_OFFSET;
        this.armorOverlayYOffset = DEFAULT_ARMOR_OVERLAY_Y_OFFSET;
        this.armorTextXOffset = DEFAULT_ARMOR_TEXT_X_OFFSET;
        this.armorTextYOffset = DEFAULT_ARMOR_TEXT_Y_OFFSET;
        this.armorTextColor = DEFAULT_ARMOR_TEXT_COLOR;
        this.armorTextOpacity = DEFAULT_ARMOR_TEXT_OPACITY;
        this.armorTextWidth = DEFAULT_ARMOR_TEXT_WIDTH;
        this.armorTextHeight = DEFAULT_ARMOR_TEXT_HEIGHT;
        this.showArmorText = DEFAULT_SHOW_ARMOR_TEXT;
        this.armorTextAlign = DEFAULT_ARMOR_TEXT_ALIGN;
        this.armorBackgroundXOffset = DEFAULT_ARMOR_BACKGROUND_X_OFFSET;
        this.armorBackgroundYOffset = DEFAULT_ARMOR_BACKGROUND_Y_OFFSET;

        this.airBarBehavior = DEFAULT_AIR_BAR_BEHAVIOR;
        this.airBarAnchor = DEFAULT_AIR_BAR_ANCHOR;
        this.enableAirBackground = DEFAULT_ENABLE_AIR_BACKGROUND;
        this.airBarVisibility = DEFAULT_AIR_BAR_VISIBILITY;
        this.airBackgroundWidth = DEFAULT_AIR_BACKGROUND_WIDTH;
        this.airBackgroundHeight = DEFAULT_AIR_BACKGROUND_HEIGHT;
        this.airBarWidth = DEFAULT_AIR_BAR_WIDTH;
        this.airBarHeight = DEFAULT_AIR_BAR_HEIGHT;
        this.airBarXOffset = DEFAULT_AIR_BAR_X_OFFSET;
        this.airBarYOffset = DEFAULT_AIR_BAR_Y_OFFSET;
        this.airTotalXOffset = DEFAULT_AIR_TOTAL_X_OFFSET;
        this.airTotalYOffset = DEFAULT_AIR_TOTAL_Y_OFFSET;
        this.enableAirIcon = DEFAULT_ENABLE_AIR_ICON;
        this.airIconWidth = DEFAULT_AIR_ICON_WIDTH;
        this.airIconHeight = DEFAULT_AIR_ICON_HEIGHT;
        this.airIconXOffset = DEFAULT_AIR_ICON_X_OFFSET;
        this.airIconYOffset = DEFAULT_AIR_ICON_Y_OFFSET;
        this.enableAirForeground = DEFAULT_ENABLE_AIR_FOREGROUND;
        this.airOverlayWidth = DEFAULT_AIR_OVERLAY_WIDTH;
        this.airOverlayHeight = DEFAULT_AIR_OVERLAY_HEIGHT;
        this.airOverlayXOffset = DEFAULT_AIR_OVERLAY_X_OFFSET;
        this.airOverlayYOffset = DEFAULT_AIR_OVERLAY_Y_OFFSET;
        this.airTextXOffset = DEFAULT_AIR_TEXT_X_OFFSET;
        this.airTextYOffset = DEFAULT_AIR_TEXT_Y_OFFSET;
        this.airTextColor = DEFAULT_AIR_TEXT_COLOR;
        this.airTextOpacity = DEFAULT_AIR_TEXT_OPACITY;
        this.airTextWidth = DEFAULT_AIR_TEXT_WIDTH;
        this.airTextHeight = DEFAULT_AIR_TEXT_HEIGHT;
        this.showAirText = DEFAULT_SHOW_AIR_TEXT;
        this.airTextAlign = DEFAULT_AIR_TEXT_ALIGN;
        this.airBackgroundXOffset = DEFAULT_AIR_BACKGROUND_X_OFFSET;
        this.airBackgroundYOffset = DEFAULT_AIR_BACKGROUND_Y_OFFSET;
        this.airFillDirection = DEFAULT_AIR_FILL_DIRECTION;
    }

    public static void setConfigPath(Path path) {
        if (CONFIG_FILE_PATH != null && !CONFIG_FILE_PATH.equals(path)) {
            Constants.LOG.warn("ClientConfig path is being changed after initial setup. This might indicate an issue.");
        }
        CONFIG_FILE_PATH = path;
    }

    public static ClientConfig getInstance() {
        if (instance == null) {
            if (CONFIG_FILE_PATH == null) {
                Constants.LOG.error("CRITICAL: ClientConfig.CONFIG_FILE_PATH was not initialized before getInstance() was called. Config will not be saved or loaded correctly.");
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
                    Constants.LOG.warn("Config file {} was empty or malformed. Creating new default config.", CONFIG_FILE_PATH);
                    loadedConfig = new ClientConfig(); // Create a new instance with defaults
                    newConfigCreated = true;
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to load client config from {}. A new default config will be created. Error: ", CONFIG_FILE_PATH, e);
                loadedConfig = new ClientConfig(); // Create a new instance with defaults
                newConfigCreated = true;
            }
        } else {
            Constants.LOG.info("No config file found at {}. Creating new default config.", CONFIG_FILE_PATH);
            loadedConfig = new ClientConfig(); // Create a new instance with defaults
            newConfigCreated = true;
        }

        // Ensure all fields are present, applying defaults for any missing ones
        // This is a simple way to handle upgrades where new config options are added
        // A more sophisticated approach might involve version numbers and explicit migration
        boolean modifiedByDefaults = ensureDefaults(loadedConfig);
        boolean migratedForCombatAttributes = applyCombatAttributesDefaults(loadedConfig);


        if (newConfigCreated || modifiedByDefaults || migratedForCombatAttributes) {
            Constants.LOG.info("Saving new or updated default config to {}.", CONFIG_FILE_PATH);
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

        // Negative values would invert the fade math; clamp before render reads it.
        if (cfg.fadeHoldDuration < 0) { cfg.fadeHoldDuration = DEFAULT_FADE_HOLD_DURATION; modified = true; }

        // Health
        if (cfg.healthBarAnchor == null) { cfg.healthBarAnchor = DEFAULT_HEALTH_BAR_ANCHOR; modified = true; }
        if (cfg.showHealthText == null) { cfg.showHealthText = DEFAULT_SHOW_HEALTH_TEXT; modified = true; }
        if (cfg.healthTextAlign == null) { cfg.healthTextAlign = DEFAULT_HEALTH_TEXT_ALIGN; modified = true; }
        if (cfg.healthFillDirection == null) { cfg.healthFillDirection = DEFAULT_HEALTH_FILL_DIRECTION; modified = true; }
        if (cfg.healthTextWidth < 1) { cfg.healthTextWidth = DEFAULT_HEALTH_TEXT_WIDTH; modified = true; }
        if (cfg.healthTextHeight < 1) { cfg.healthTextHeight = DEFAULT_HEALTH_TEXT_HEIGHT; modified = true; }
        if (cfg.healthAbsorptionTextWidth < 1) { cfg.healthAbsorptionTextWidth = DEFAULT_HEALTH_ABSORPTION_TEXT_WIDTH; modified = true; }
        if (cfg.healthAbsorptionTextHeight < 1) { cfg.healthAbsorptionTextHeight = DEFAULT_HEALTH_ABSORPTION_TEXT_HEIGHT; modified = true; }
        if (cfg.healthAbsorptionTextAlign == null) { cfg.healthAbsorptionTextAlign = DEFAULT_HEALTH_ABSORPTION_TEXT_ALIGN; modified = true; }
        if (cfg.healthAbsorptionDisplayMode == null) { cfg.healthAbsorptionDisplayMode = DEFAULT_HEALTH_ABSORPTION_DISPLAY_MODE; modified = true; }

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
        if (cfg.staminaTextWidth < 1) { cfg.staminaTextWidth = DEFAULT_STAMINA_TEXT_WIDTH; modified = true; }
        if (cfg.staminaTextHeight < 1) { cfg.staminaTextHeight = DEFAULT_STAMINA_TEXT_HEIGHT; modified = true; }

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
        if (cfg.manaTextWidth < 1) { cfg.manaTextWidth = DEFAULT_MANA_TEXT_WIDTH; modified = true; }
        if (cfg.manaTextHeight < 1) { cfg.manaTextHeight = DEFAULT_MANA_TEXT_HEIGHT; modified = true; }

        // Ensure mana overlay dimensions are within valid ranges
        if (cfg.manaOverlayWidth > 256) { cfg.manaOverlayWidth = 256; modified = true; }
        if (cfg.manaOverlayHeight > 256) { cfg.manaOverlayHeight = 256; modified = true; }
        if (cfg.manaOverlayWidth < 1) { cfg.manaOverlayWidth = DEFAULT_MANA_OVERLAY_WIDTH; modified = true; }
        if (cfg.manaOverlayHeight < 1) { cfg.manaOverlayHeight = DEFAULT_MANA_OVERLAY_HEIGHT; modified = true; }

        // Armor
        if (cfg.armorBarBehavior == null) { cfg.armorBarBehavior = DEFAULT_ARMOR_BAR_BEHAVIOR; modified = true; }
        if (cfg.armorBarAnchor == null) { cfg.armorBarAnchor = DEFAULT_ARMOR_BAR_ANCHOR; modified = true; }
        if (cfg.armorBarVisibility == null) { cfg.armorBarVisibility = DEFAULT_ARMOR_BAR_VISIBILITY; modified = true; }
        if (cfg.showArmorText == null) { cfg.showArmorText = DEFAULT_SHOW_ARMOR_TEXT; modified = true; }
        if (cfg.armorTextAlign == null) { cfg.armorTextAlign = DEFAULT_ARMOR_TEXT_ALIGN; modified = true; }
        if (cfg.armorFillDirection == null) { cfg.armorFillDirection = DEFAULT_ARMOR_FILL_DIRECTION; modified = true; }
        if (cfg.armorOverlayWidth > 256) { cfg.armorOverlayWidth = 256; modified = true; }
        if (cfg.armorOverlayHeight > 256) { cfg.armorOverlayHeight = 256; modified = true; }
        if (cfg.armorOverlayWidth < 1) { cfg.armorOverlayWidth = DEFAULT_ARMOR_OVERLAY_WIDTH; modified = true; }
        if (cfg.armorOverlayHeight < 1) { cfg.armorOverlayHeight = DEFAULT_ARMOR_OVERLAY_HEIGHT; modified = true; }
        if (cfg.armorIconWidth < 1) { cfg.armorIconWidth = DEFAULT_ARMOR_ICON_WIDTH; modified = true; }
        if (cfg.armorIconHeight < 1) { cfg.armorIconHeight = DEFAULT_ARMOR_ICON_HEIGHT; modified = true; }
        if (cfg.armorTextWidth < 1) { cfg.armorTextWidth = DEFAULT_ARMOR_TEXT_WIDTH; modified = true; }
        if (cfg.armorTextHeight < 1) { cfg.armorTextHeight = DEFAULT_ARMOR_TEXT_HEIGHT; modified = true; }

        // Air
        if (cfg.airBarBehavior == null) { cfg.airBarBehavior = DEFAULT_AIR_BAR_BEHAVIOR; modified = true; }
        if (cfg.airBarAnchor == null) { cfg.airBarAnchor = DEFAULT_AIR_BAR_ANCHOR; modified = true; }
        if (cfg.airBarVisibility == null) { cfg.airBarVisibility = DEFAULT_AIR_BAR_VISIBILITY; modified = true; }
        if (cfg.showAirText == null) { cfg.showAirText = DEFAULT_SHOW_AIR_TEXT; modified = true; }
        if (cfg.airTextAlign == null) { cfg.airTextAlign = DEFAULT_AIR_TEXT_ALIGN; modified = true; }
        if (cfg.airFillDirection == null) { cfg.airFillDirection = DEFAULT_AIR_FILL_DIRECTION; modified = true; }
        if (cfg.airOverlayWidth > 256) { cfg.airOverlayWidth = 256; modified = true; }
        if (cfg.airOverlayHeight > 256) { cfg.airOverlayHeight = 256; modified = true; }
        if (cfg.airOverlayWidth < 1) { cfg.airOverlayWidth = DEFAULT_AIR_OVERLAY_WIDTH; modified = true; }
        if (cfg.airOverlayHeight < 1) { cfg.airOverlayHeight = DEFAULT_AIR_OVERLAY_HEIGHT; modified = true; }
        if (cfg.airIconWidth < 1) { cfg.airIconWidth = DEFAULT_AIR_ICON_WIDTH; modified = true; }
        if (cfg.airIconHeight < 1) { cfg.airIconHeight = DEFAULT_AIR_ICON_HEIGHT; modified = true; }
        if (cfg.airTextWidth < 1) { cfg.airTextWidth = DEFAULT_AIR_TEXT_WIDTH; modified = true; }
        if (cfg.airTextHeight < 1) { cfg.airTextHeight = DEFAULT_AIR_TEXT_HEIGHT; modified = true; }

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
            Constants.LOG.error("ClientConfig.CONFIG_FILE_PATH is null, cannot save config.");
            return;
        }
        try {
            Files.createDirectories(CONFIG_FILE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to save client config to {}:", CONFIG_FILE_PATH, e);
        }
    }
}
