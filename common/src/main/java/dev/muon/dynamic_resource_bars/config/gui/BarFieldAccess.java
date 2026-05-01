package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.util.BarVisibility;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import dev.muon.dynamic_resource_bars.util.TextBehavior;

/**
 * Indirection layer that lets the editor screens read and write a bar's layout fields
 * without a switch on {@link DraggableElement} at every call site.
 *
 * <p>Adding a new bar = add a new {@code public static final BarFieldAccess} constant
 * and a case in {@link #forElement(DraggableElement)}.
 */
public abstract class BarFieldAccess {

    // ----- Background -----
    public abstract int bgWidth(ClientConfig c);
    public abstract void setBgWidth(ClientConfig c, int v);
    public abstract int bgHeight(ClientConfig c);
    public abstract void setBgHeight(ClientConfig c, int v);
    public abstract int bgX(ClientConfig c);
    public abstract void setBgX(ClientConfig c, int v);
    public abstract int bgY(ClientConfig c);
    public abstract void setBgY(ClientConfig c, int v);

    // ----- Bar fill -----
    public abstract int barWidth(ClientConfig c);
    public abstract void setBarWidth(ClientConfig c, int v);
    public abstract int barHeight(ClientConfig c);
    public abstract void setBarHeight(ClientConfig c, int v);
    public abstract int barX(ClientConfig c);
    public abstract void setBarX(ClientConfig c, int v);
    public abstract int barY(ClientConfig c);
    public abstract void setBarY(ClientConfig c, int v);

    // ----- Foreground -----
    public abstract int overlayWidth(ClientConfig c);
    public abstract void setOverlayWidth(ClientConfig c, int v);
    public abstract int overlayHeight(ClientConfig c);
    public abstract void setOverlayHeight(ClientConfig c, int v);
    public abstract int overlayX(ClientConfig c);
    public abstract void setOverlayX(ClientConfig c, int v);
    public abstract int overlayY(ClientConfig c);
    public abstract void setOverlayY(ClientConfig c, int v);

    // ----- Text -----
    public abstract int textX(ClientConfig c);
    public abstract void setTextX(ClientConfig c, int v);
    public abstract int textY(ClientConfig c);
    public abstract void setTextY(ClientConfig c, int v);

    // ----- Whole-element -----
    public abstract int totalX(ClientConfig c);
    public abstract void setTotalX(ClientConfig c, int v);
    public abstract int totalY(ClientConfig c);
    public abstract void setTotalY(ClientConfig c, int v);

    // ----- Visibility / text behavior — exposed here so HudEditorScreen doesn't need a switch on element. -----
    public abstract BarVisibility visibility(ClientConfig c);
    public abstract void setVisibility(ClientConfig c, BarVisibility v);
    public abstract TextBehavior textBehavior(ClientConfig c);
    public abstract void setTextBehavior(ClientConfig c, TextBehavior v);

    /**
     * Resets the per-bar non-layout fields (behavior, visibility, text) from {@code defaults} into
     * {@code live}. Behavior is the per-bar enum (BarRenderBehavior for armor/air/health,
     * StaminaBarBehavior, ManaBarBehavior) — handled in the subclass.
     */
    public abstract void resetBehaviorFields(ClientConfig live, ClientConfig defaults);

    /** Reads the X offset for a sub-element. Convenience for focus-mode movement. */
    public int subElementX(ClientConfig c, SubElementType sub) {
        return switch (sub) {
            case BACKGROUND -> bgX(c);
            case BAR_MAIN -> barX(c);
            case FOREGROUND -> overlayX(c);
            case TEXT -> textX(c);
            case ICON -> iconX(c);
            case ABSORPTION_TEXT -> absorptionTextX(c);
        };
    }

    public void setSubElementX(ClientConfig c, SubElementType sub, int v) {
        switch (sub) {
            case BACKGROUND -> setBgX(c, v);
            case BAR_MAIN -> setBarX(c, v);
            case FOREGROUND -> setOverlayX(c, v);
            case TEXT -> setTextX(c, v);
            case ICON -> setIconX(c, v);
            case ABSORPTION_TEXT -> setAbsorptionTextX(c, v);
        }
    }

    public int subElementY(ClientConfig c, SubElementType sub) {
        return switch (sub) {
            case BACKGROUND -> bgY(c);
            case BAR_MAIN -> barY(c);
            case FOREGROUND -> overlayY(c);
            case TEXT -> textY(c);
            case ICON -> iconY(c);
            case ABSORPTION_TEXT -> absorptionTextY(c);
        };
    }

    public void setSubElementY(ClientConfig c, SubElementType sub, int v) {
        switch (sub) {
            case BACKGROUND -> setBgY(c, v);
            case BAR_MAIN -> setBarY(c, v);
            case FOREGROUND -> setOverlayY(c, v);
            case TEXT -> setTextY(c, v);
            case ICON -> setIconY(c, v);
            case ABSORPTION_TEXT -> setAbsorptionTextY(c, v);
        }
    }

    /**
     * Resize accessors. Width/height map to the obvious fields for BACKGROUND/BAR_MAIN/FOREGROUND;
     * for ICON they both map to {@code iconSize} (icons are square) so a width-handle drag and a
     * height-handle drag both scale the icon uniformly.
     */
    public int subElementWidth(ClientConfig c, SubElementType sub) {
        return switch (sub) {
            case BACKGROUND -> bgWidth(c);
            case BAR_MAIN -> barWidth(c);
            case FOREGROUND -> overlayWidth(c);
            case ICON -> iconSize(c);
            case TEXT, ABSORPTION_TEXT -> 0;
        };
    }

    public void setSubElementWidth(ClientConfig c, SubElementType sub, int v) {
        switch (sub) {
            case BACKGROUND -> setBgWidth(c, v);
            case BAR_MAIN -> setBarWidth(c, v);
            case FOREGROUND -> setOverlayWidth(c, v);
            case ICON -> setIconSize(c, v);
            case TEXT, ABSORPTION_TEXT -> {}
        }
    }

    public int subElementHeight(ClientConfig c, SubElementType sub) {
        return switch (sub) {
            case BACKGROUND -> bgHeight(c);
            case BAR_MAIN -> barHeight(c);
            case FOREGROUND -> overlayHeight(c);
            case ICON -> iconSize(c);
            case TEXT, ABSORPTION_TEXT -> 0;
        };
    }

    public void setSubElementHeight(ClientConfig c, SubElementType sub, int v) {
        switch (sub) {
            case BACKGROUND -> setBgHeight(c, v);
            case BAR_MAIN -> setBarHeight(c, v);
            case FOREGROUND -> setOverlayHeight(c, v);
            case ICON -> setIconSize(c, v);
            case TEXT, ABSORPTION_TEXT -> {}
        }
    }

    public static final BarFieldAccess HEALTH = new BarFieldAccess() {
        public int bgWidth(ClientConfig c) { return c.healthBackgroundWidth; }
        public void setBgWidth(ClientConfig c, int v) { c.healthBackgroundWidth = v; }
        public int bgHeight(ClientConfig c) { return c.healthBackgroundHeight; }
        public void setBgHeight(ClientConfig c, int v) { c.healthBackgroundHeight = v; }
        public int bgX(ClientConfig c) { return c.healthBackgroundXOffset; }
        public void setBgX(ClientConfig c, int v) { c.healthBackgroundXOffset = v; }
        public int bgY(ClientConfig c) { return c.healthBackgroundYOffset; }
        public void setBgY(ClientConfig c, int v) { c.healthBackgroundYOffset = v; }
        public int barWidth(ClientConfig c) { return c.healthBarWidth; }
        public void setBarWidth(ClientConfig c, int v) { c.healthBarWidth = v; }
        public int barHeight(ClientConfig c) { return c.healthBarHeight; }
        public void setBarHeight(ClientConfig c, int v) { c.healthBarHeight = v; }
        public int barX(ClientConfig c) { return c.healthBarXOffset; }
        public void setBarX(ClientConfig c, int v) { c.healthBarXOffset = v; }
        public int barY(ClientConfig c) { return c.healthBarYOffset; }
        public void setBarY(ClientConfig c, int v) { c.healthBarYOffset = v; }
        public int overlayWidth(ClientConfig c) { return c.healthOverlayWidth; }
        public void setOverlayWidth(ClientConfig c, int v) { c.healthOverlayWidth = v; }
        public int overlayHeight(ClientConfig c) { return c.healthOverlayHeight; }
        public void setOverlayHeight(ClientConfig c, int v) { c.healthOverlayHeight = v; }
        public int overlayX(ClientConfig c) { return c.healthOverlayXOffset; }
        public void setOverlayX(ClientConfig c, int v) { c.healthOverlayXOffset = v; }
        public int overlayY(ClientConfig c) { return c.healthOverlayYOffset; }
        public void setOverlayY(ClientConfig c, int v) { c.healthOverlayYOffset = v; }
        public int textX(ClientConfig c) { return c.healthTextXOffset; }
        public void setTextX(ClientConfig c, int v) { c.healthTextXOffset = v; }
        public int textY(ClientConfig c) { return c.healthTextYOffset; }
        public void setTextY(ClientConfig c, int v) { c.healthTextYOffset = v; }
        public int totalX(ClientConfig c) { return c.healthTotalXOffset; }
        public void setTotalX(ClientConfig c, int v) { c.healthTotalXOffset = v; }
        public int totalY(ClientConfig c) { return c.healthTotalYOffset; }
        public void setTotalY(ClientConfig c, int v) { c.healthTotalYOffset = v; }
        public int absorptionTextX(ClientConfig c) { return c.healthAbsorptionTextXOffset; }
        public void setAbsorptionTextX(ClientConfig c, int v) { c.healthAbsorptionTextXOffset = v; }
        public int absorptionTextY(ClientConfig c) { return c.healthAbsorptionTextYOffset; }
        public void setAbsorptionTextY(ClientConfig c, int v) { c.healthAbsorptionTextYOffset = v; }
        public BarVisibility visibility(ClientConfig c) { return c.healthBarVisibility; }
        public void setVisibility(ClientConfig c, BarVisibility v) { c.healthBarVisibility = v; }
        public TextBehavior textBehavior(ClientConfig c) { return c.showHealthText; }
        public void setTextBehavior(ClientConfig c, TextBehavior v) { c.showHealthText = v; }
        public void resetBehaviorFields(ClientConfig live, ClientConfig defaults) {
            live.healthBarBehavior = defaults.healthBarBehavior;
            live.healthBarVisibility = defaults.healthBarVisibility;
            live.showHealthText = defaults.showHealthText;
        }
    };

    public static final BarFieldAccess MANA = new BarFieldAccess() {
        public int bgWidth(ClientConfig c) { return c.manaBackgroundWidth; }
        public void setBgWidth(ClientConfig c, int v) { c.manaBackgroundWidth = v; }
        public int bgHeight(ClientConfig c) { return c.manaBackgroundHeight; }
        public void setBgHeight(ClientConfig c, int v) { c.manaBackgroundHeight = v; }
        public int bgX(ClientConfig c) { return c.manaBackgroundXOffset; }
        public void setBgX(ClientConfig c, int v) { c.manaBackgroundXOffset = v; }
        public int bgY(ClientConfig c) { return c.manaBackgroundYOffset; }
        public void setBgY(ClientConfig c, int v) { c.manaBackgroundYOffset = v; }
        public int barWidth(ClientConfig c) { return c.manaBarWidth; }
        public void setBarWidth(ClientConfig c, int v) { c.manaBarWidth = v; }
        public int barHeight(ClientConfig c) { return c.manaBarHeight; }
        public void setBarHeight(ClientConfig c, int v) { c.manaBarHeight = v; }
        public int barX(ClientConfig c) { return c.manaBarXOffset; }
        public void setBarX(ClientConfig c, int v) { c.manaBarXOffset = v; }
        public int barY(ClientConfig c) { return c.manaBarYOffset; }
        public void setBarY(ClientConfig c, int v) { c.manaBarYOffset = v; }
        public int overlayWidth(ClientConfig c) { return c.manaOverlayWidth; }
        public void setOverlayWidth(ClientConfig c, int v) { c.manaOverlayWidth = v; }
        public int overlayHeight(ClientConfig c) { return c.manaOverlayHeight; }
        public void setOverlayHeight(ClientConfig c, int v) { c.manaOverlayHeight = v; }
        public int overlayX(ClientConfig c) { return c.manaOverlayXOffset; }
        public void setOverlayX(ClientConfig c, int v) { c.manaOverlayXOffset = v; }
        public int overlayY(ClientConfig c) { return c.manaOverlayYOffset; }
        public void setOverlayY(ClientConfig c, int v) { c.manaOverlayYOffset = v; }
        public int textX(ClientConfig c) { return c.manaTextXOffset; }
        public void setTextX(ClientConfig c, int v) { c.manaTextXOffset = v; }
        public int textY(ClientConfig c) { return c.manaTextYOffset; }
        public void setTextY(ClientConfig c, int v) { c.manaTextYOffset = v; }
        public int totalX(ClientConfig c) { return c.manaTotalXOffset; }
        public void setTotalX(ClientConfig c, int v) { c.manaTotalXOffset = v; }
        public int totalY(ClientConfig c) { return c.manaTotalYOffset; }
        public void setTotalY(ClientConfig c, int v) { c.manaTotalYOffset = v; }
        public BarVisibility visibility(ClientConfig c) { return c.manaBarVisibility; }
        public void setVisibility(ClientConfig c, BarVisibility v) { c.manaBarVisibility = v; }
        public TextBehavior textBehavior(ClientConfig c) { return c.showManaText; }
        public void setTextBehavior(ClientConfig c, TextBehavior v) { c.showManaText = v; }
        public void resetBehaviorFields(ClientConfig live, ClientConfig defaults) {
            live.manaBarBehavior = defaults.manaBarBehavior;
            live.manaBarVisibility = defaults.manaBarVisibility;
            live.showManaText = defaults.showManaText;
        }
    };

    public static final BarFieldAccess STAMINA = new BarFieldAccess() {
        public int bgWidth(ClientConfig c) { return c.staminaBackgroundWidth; }
        public void setBgWidth(ClientConfig c, int v) { c.staminaBackgroundWidth = v; }
        public int bgHeight(ClientConfig c) { return c.staminaBackgroundHeight; }
        public void setBgHeight(ClientConfig c, int v) { c.staminaBackgroundHeight = v; }
        public int bgX(ClientConfig c) { return c.staminaBackgroundXOffset; }
        public void setBgX(ClientConfig c, int v) { c.staminaBackgroundXOffset = v; }
        public int bgY(ClientConfig c) { return c.staminaBackgroundYOffset; }
        public void setBgY(ClientConfig c, int v) { c.staminaBackgroundYOffset = v; }
        public int barWidth(ClientConfig c) { return c.staminaBarWidth; }
        public void setBarWidth(ClientConfig c, int v) { c.staminaBarWidth = v; }
        public int barHeight(ClientConfig c) { return c.staminaBarHeight; }
        public void setBarHeight(ClientConfig c, int v) { c.staminaBarHeight = v; }
        public int barX(ClientConfig c) { return c.staminaBarXOffset; }
        public void setBarX(ClientConfig c, int v) { c.staminaBarXOffset = v; }
        public int barY(ClientConfig c) { return c.staminaBarYOffset; }
        public void setBarY(ClientConfig c, int v) { c.staminaBarYOffset = v; }
        public int overlayWidth(ClientConfig c) { return c.staminaOverlayWidth; }
        public void setOverlayWidth(ClientConfig c, int v) { c.staminaOverlayWidth = v; }
        public int overlayHeight(ClientConfig c) { return c.staminaOverlayHeight; }
        public void setOverlayHeight(ClientConfig c, int v) { c.staminaOverlayHeight = v; }
        public int overlayX(ClientConfig c) { return c.staminaOverlayXOffset; }
        public void setOverlayX(ClientConfig c, int v) { c.staminaOverlayXOffset = v; }
        public int overlayY(ClientConfig c) { return c.staminaOverlayYOffset; }
        public void setOverlayY(ClientConfig c, int v) { c.staminaOverlayYOffset = v; }
        public int textX(ClientConfig c) { return c.staminaTextXOffset; }
        public void setTextX(ClientConfig c, int v) { c.staminaTextXOffset = v; }
        public int textY(ClientConfig c) { return c.staminaTextYOffset; }
        public void setTextY(ClientConfig c, int v) { c.staminaTextYOffset = v; }
        public int totalX(ClientConfig c) { return c.staminaTotalXOffset; }
        public void setTotalX(ClientConfig c, int v) { c.staminaTotalXOffset = v; }
        public int totalY(ClientConfig c) { return c.staminaTotalYOffset; }
        public void setTotalY(ClientConfig c, int v) { c.staminaTotalYOffset = v; }
        public BarVisibility visibility(ClientConfig c) { return c.staminaBarVisibility; }
        public void setVisibility(ClientConfig c, BarVisibility v) { c.staminaBarVisibility = v; }
        public TextBehavior textBehavior(ClientConfig c) { return c.showStaminaText; }
        public void setTextBehavior(ClientConfig c, TextBehavior v) { c.showStaminaText = v; }
        public void resetBehaviorFields(ClientConfig live, ClientConfig defaults) {
            live.staminaBarBehavior = defaults.staminaBarBehavior;
            live.staminaBarVisibility = defaults.staminaBarVisibility;
            live.showStaminaText = defaults.showStaminaText;
        }
    };

    /** Icon X/Y/size — 0 by default for bars without an icon (health/mana/stamina). */
    public int iconX(ClientConfig c) { return 0; }
    public void setIconX(ClientConfig c, int v) {}
    public int iconY(ClientConfig c) { return 0; }
    public void setIconY(ClientConfig c, int v) {}
    public int iconSize(ClientConfig c) { return 0; }
    public void setIconSize(ClientConfig c, int v) {}

    /** Absorption text X/Y — only meaningful for the health bar; default 0 elsewhere. */
    public int absorptionTextX(ClientConfig c) { return 0; }
    public void setAbsorptionTextX(ClientConfig c, int v) {}
    public int absorptionTextY(ClientConfig c) { return 0; }
    public void setAbsorptionTextY(ClientConfig c, int v) {}

    public static final BarFieldAccess ARMOR = new BarFieldAccess() {
        public int bgWidth(ClientConfig c) { return c.armorBackgroundWidth; }
        public void setBgWidth(ClientConfig c, int v) { c.armorBackgroundWidth = v; }
        public int bgHeight(ClientConfig c) { return c.armorBackgroundHeight; }
        public void setBgHeight(ClientConfig c, int v) { c.armorBackgroundHeight = v; }
        public int bgX(ClientConfig c) { return c.armorBackgroundXOffset; }
        public void setBgX(ClientConfig c, int v) { c.armorBackgroundXOffset = v; }
        public int bgY(ClientConfig c) { return c.armorBackgroundYOffset; }
        public void setBgY(ClientConfig c, int v) { c.armorBackgroundYOffset = v; }
        public int barWidth(ClientConfig c) { return c.armorBarWidth; }
        public void setBarWidth(ClientConfig c, int v) { c.armorBarWidth = v; }
        public int barHeight(ClientConfig c) { return c.armorBarHeight; }
        public void setBarHeight(ClientConfig c, int v) { c.armorBarHeight = v; }
        public int barX(ClientConfig c) { return c.armorBarXOffset; }
        public void setBarX(ClientConfig c, int v) { c.armorBarXOffset = v; }
        public int barY(ClientConfig c) { return c.armorBarYOffset; }
        public void setBarY(ClientConfig c, int v) { c.armorBarYOffset = v; }
        // Armor bars don't render a foreground; widths/heights echo the bar so resets stay sane.
        public int overlayWidth(ClientConfig c) { return c.armorBarWidth; }
        public void setOverlayWidth(ClientConfig c, int v) {}
        public int overlayHeight(ClientConfig c) { return c.armorBarHeight; }
        public void setOverlayHeight(ClientConfig c, int v) {}
        public int overlayX(ClientConfig c) { return 0; }
        public void setOverlayX(ClientConfig c, int v) {}
        public int overlayY(ClientConfig c) { return 0; }
        public void setOverlayY(ClientConfig c, int v) {}
        public int textX(ClientConfig c) { return c.armorTextXOffset; }
        public void setTextX(ClientConfig c, int v) { c.armorTextXOffset = v; }
        public int textY(ClientConfig c) { return c.armorTextYOffset; }
        public void setTextY(ClientConfig c, int v) { c.armorTextYOffset = v; }
        public int totalX(ClientConfig c) { return c.armorTotalXOffset; }
        public void setTotalX(ClientConfig c, int v) { c.armorTotalXOffset = v; }
        public int totalY(ClientConfig c) { return c.armorTotalYOffset; }
        public void setTotalY(ClientConfig c, int v) { c.armorTotalYOffset = v; }
        public int iconX(ClientConfig c) { return c.armorIconXOffset; }
        public void setIconX(ClientConfig c, int v) { c.armorIconXOffset = v; }
        public int iconY(ClientConfig c) { return c.armorIconYOffset; }
        public void setIconY(ClientConfig c, int v) { c.armorIconYOffset = v; }
        public int iconSize(ClientConfig c) { return c.armorIconSize; }
        public void setIconSize(ClientConfig c, int v) { c.armorIconSize = v; }
        public BarVisibility visibility(ClientConfig c) { return c.armorBarVisibility; }
        public void setVisibility(ClientConfig c, BarVisibility v) { c.armorBarVisibility = v; }
        public TextBehavior textBehavior(ClientConfig c) { return c.showArmorText; }
        public void setTextBehavior(ClientConfig c, TextBehavior v) { c.showArmorText = v; }
        public void resetBehaviorFields(ClientConfig live, ClientConfig defaults) {
            live.armorBarBehavior = defaults.armorBarBehavior;
            live.armorBarVisibility = defaults.armorBarVisibility;
            live.showArmorText = defaults.showArmorText;
        }
    };

    public static final BarFieldAccess AIR = new BarFieldAccess() {
        public int bgWidth(ClientConfig c) { return c.airBackgroundWidth; }
        public void setBgWidth(ClientConfig c, int v) { c.airBackgroundWidth = v; }
        public int bgHeight(ClientConfig c) { return c.airBackgroundHeight; }
        public void setBgHeight(ClientConfig c, int v) { c.airBackgroundHeight = v; }
        public int bgX(ClientConfig c) { return c.airBackgroundXOffset; }
        public void setBgX(ClientConfig c, int v) { c.airBackgroundXOffset = v; }
        public int bgY(ClientConfig c) { return c.airBackgroundYOffset; }
        public void setBgY(ClientConfig c, int v) { c.airBackgroundYOffset = v; }
        public int barWidth(ClientConfig c) { return c.airBarWidth; }
        public void setBarWidth(ClientConfig c, int v) { c.airBarWidth = v; }
        public int barHeight(ClientConfig c) { return c.airBarHeight; }
        public void setBarHeight(ClientConfig c, int v) { c.airBarHeight = v; }
        public int barX(ClientConfig c) { return c.airBarXOffset; }
        public void setBarX(ClientConfig c, int v) { c.airBarXOffset = v; }
        public int barY(ClientConfig c) { return c.airBarYOffset; }
        public void setBarY(ClientConfig c, int v) { c.airBarYOffset = v; }
        public int overlayWidth(ClientConfig c) { return c.airBarWidth; }
        public void setOverlayWidth(ClientConfig c, int v) {}
        public int overlayHeight(ClientConfig c) { return c.airBarHeight; }
        public void setOverlayHeight(ClientConfig c, int v) {}
        public int overlayX(ClientConfig c) { return 0; }
        public void setOverlayX(ClientConfig c, int v) {}
        public int overlayY(ClientConfig c) { return 0; }
        public void setOverlayY(ClientConfig c, int v) {}
        public int textX(ClientConfig c) { return c.airTextXOffset; }
        public void setTextX(ClientConfig c, int v) { c.airTextXOffset = v; }
        public int textY(ClientConfig c) { return c.airTextYOffset; }
        public void setTextY(ClientConfig c, int v) { c.airTextYOffset = v; }
        public int totalX(ClientConfig c) { return c.airTotalXOffset; }
        public void setTotalX(ClientConfig c, int v) { c.airTotalXOffset = v; }
        public int totalY(ClientConfig c) { return c.airTotalYOffset; }
        public void setTotalY(ClientConfig c, int v) { c.airTotalYOffset = v; }
        public int iconX(ClientConfig c) { return c.airIconXOffset; }
        public void setIconX(ClientConfig c, int v) { c.airIconXOffset = v; }
        public int iconY(ClientConfig c) { return c.airIconYOffset; }
        public void setIconY(ClientConfig c, int v) { c.airIconYOffset = v; }
        public int iconSize(ClientConfig c) { return c.airIconSize; }
        public void setIconSize(ClientConfig c, int v) { c.airIconSize = v; }
        public BarVisibility visibility(ClientConfig c) { return c.airBarVisibility; }
        public void setVisibility(ClientConfig c, BarVisibility v) { c.airBarVisibility = v; }
        public TextBehavior textBehavior(ClientConfig c) { return c.showAirText; }
        public void setTextBehavior(ClientConfig c, TextBehavior v) { c.showAirText = v; }
        public void resetBehaviorFields(ClientConfig live, ClientConfig defaults) {
            live.airBarBehavior = defaults.airBarBehavior;
            live.airBarVisibility = defaults.airBarVisibility;
            live.showAirText = defaults.showAirText;
        }
    };

    public static BarFieldAccess forElement(DraggableElement e) {
        if (e == null) return null;
        return switch (e) {
            case HEALTH_BAR -> HEALTH;
            case MANA_BAR -> MANA;
            case STAMINA_BAR -> STAMINA;
            case ARMOR_BAR -> ARMOR;
            case AIR_BAR -> AIR;
        };
    }
}
