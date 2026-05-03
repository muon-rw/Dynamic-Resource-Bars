package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.util.BarVisibility;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.FillDirection;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;
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
    public abstract int textWidth(ClientConfig c);
    public abstract void setTextWidth(ClientConfig c, int v);
    public abstract int textHeight(ClientConfig c);
    public abstract void setTextHeight(ClientConfig c, int v);

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

    // ----- Per-bar enums exposed by the context menu. Defaults included so adding a new bar
    //       only requires overriding what's distinct from the base; per-bar fields fall through
    //       to no-op-friendly defaults until they're wired. -----
    public abstract HUDPositioning.BarPlacement anchor(ClientConfig c);
    public abstract void setAnchor(ClientConfig c, HUDPositioning.BarPlacement v);
    public abstract HorizontalAlignment textAlign(ClientConfig c);
    public abstract void setTextAlign(ClientConfig c, HorizontalAlignment v);
    public abstract FillDirection fillDirection(ClientConfig c);
    public abstract void setFillDirection(ClientConfig c, FillDirection v);
    public abstract boolean enableBackground(ClientConfig c);
    public abstract void setEnableBackground(ClientConfig c, boolean v);
    public abstract boolean enableForeground(ClientConfig c);
    public abstract void setEnableForeground(ClientConfig c, boolean v);

    /**
     * Resets the per-bar non-layout fields (behavior, visibility, text, alignment, anchor,
     * fill direction, layer toggles) from {@code defaults} into {@code live}. Behavior is the
     * per-bar enum (BarRenderBehavior for armor/air/health, StaminaBarBehavior, ManaBarBehavior)
     * — handled in the subclass.
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
     * Resize accessors. ICON has independent width/height so the editor's width-handle and
     * height-handle drag its two axes independently. TEXT carries its own bounding box now —
     * vertical resize maps to font scale, horizontal resize affects alignment math but is
     * otherwise cosmetic. ABSORPTION_TEXT works the same way for the health bar's "+8" tag.
     */
    public int subElementWidth(ClientConfig c, SubElementType sub) {
        return switch (sub) {
            case BACKGROUND -> bgWidth(c);
            case BAR_MAIN -> barWidth(c);
            case FOREGROUND -> overlayWidth(c);
            case ICON -> iconWidth(c);
            case TEXT -> textWidth(c);
            case ABSORPTION_TEXT -> absorptionTextWidth(c);
        };
    }

    public void setSubElementWidth(ClientConfig c, SubElementType sub, int v) {
        switch (sub) {
            case BACKGROUND -> setBgWidth(c, v);
            case BAR_MAIN -> setBarWidth(c, v);
            case FOREGROUND -> setOverlayWidth(c, v);
            case ICON -> setIconWidth(c, v);
            case TEXT -> setTextWidth(c, v);
            case ABSORPTION_TEXT -> setAbsorptionTextWidth(c, v);
        }
    }

    public int subElementHeight(ClientConfig c, SubElementType sub) {
        return switch (sub) {
            case BACKGROUND -> bgHeight(c);
            case BAR_MAIN -> barHeight(c);
            case FOREGROUND -> overlayHeight(c);
            case ICON -> iconHeight(c);
            case TEXT -> textHeight(c);
            case ABSORPTION_TEXT -> absorptionTextHeight(c);
        };
    }

    public void setSubElementHeight(ClientConfig c, SubElementType sub, int v) {
        switch (sub) {
            case BACKGROUND -> setBgHeight(c, v);
            case BAR_MAIN -> setBarHeight(c, v);
            case FOREGROUND -> setOverlayHeight(c, v);
            case ICON -> setIconHeight(c, v);
            case TEXT -> setTextHeight(c, v);
            case ABSORPTION_TEXT -> setAbsorptionTextHeight(c, v);
        }
    }

    /**
     * Reads the visibility flag for a sub-element. Sub-elements that aren't toggleable
     * (BAR_MAIN, TEXT) always report {@code true} — they manage their own rendering via
     * other paths (TextBehavior for TEXT, the bar fill is non-optional). ABSORPTION_TEXT
     * is toggleable on bars that have one (health); the base no-op returns {@code true}.
     */
    public boolean enableSubElement(ClientConfig c, SubElementType sub) {
        return switch (sub) {
            case BACKGROUND -> enableBackground(c);
            case FOREGROUND -> enableForeground(c);
            case ICON -> enableIcon(c);
            case ABSORPTION_TEXT -> enableAbsorptionText(c);
            case BAR_MAIN, TEXT -> true;
        };
    }

    public void setEnableSubElement(ClientConfig c, SubElementType sub, boolean v) {
        switch (sub) {
            case BACKGROUND -> setEnableBackground(c, v);
            case FOREGROUND -> setEnableForeground(c, v);
            case ICON -> setEnableIcon(c, v);
            case ABSORPTION_TEXT -> setEnableAbsorptionText(c, v);
            case BAR_MAIN, TEXT -> {}
        }
    }

    /**
     * Resets a single sub-element's geometry plus its sub-specific behavior fields from
     * {@code defaults} into {@code live}. Used by the focus-mode "Reset Sub-Element" item.
     */
    public void resetSubElement(ClientConfig live, ClientConfig defaults, SubElementType sub) {
        setSubElementX(live, sub, subElementX(defaults, sub));
        setSubElementY(live, sub, subElementY(defaults, sub));
        if (sub.isResizable()) {
            setSubElementWidth(live, sub, subElementWidth(defaults, sub));
            setSubElementHeight(live, sub, subElementHeight(defaults, sub));
        }
        switch (sub) {
            case BACKGROUND -> setEnableBackground(live, enableBackground(defaults));
            case FOREGROUND -> setEnableForeground(live, enableForeground(defaults));
            case ICON -> setEnableIcon(live, enableIcon(defaults));
            case TEXT -> setTextAlign(live, textAlign(defaults));
            case ABSORPTION_TEXT -> {
                setEnableAbsorptionText(live, enableAbsorptionText(defaults));
                setAbsorptionTextAlign(live, absorptionTextAlign(defaults));
            }
            case BAR_MAIN -> {} // no behavior fields specific to BAR_MAIN
        }
    }

    // ----- Default no-op accessors for sub-elements that don't apply to every bar. ICON only
    //       lives on armor/air, ABSORPTION_TEXT only on health, etc. -----

    /** Whether this bar exposes an icon sub-element. Overridden true on ARMOR/AIR. */
    public boolean hasIcon() { return false; }
    /** Whether this bar exposes an absorption-text sub-element. Overridden true on HEALTH. */
    public boolean hasAbsorptionText() { return false; }

    public int iconX(ClientConfig c) { return 0; }
    public void setIconX(ClientConfig c, int v) {}
    public int iconY(ClientConfig c) { return 0; }
    public void setIconY(ClientConfig c, int v) {}
    public int iconWidth(ClientConfig c) { return 0; }
    public void setIconWidth(ClientConfig c, int v) {}
    public int iconHeight(ClientConfig c) { return 0; }
    public void setIconHeight(ClientConfig c, int v) {}
    public boolean enableIcon(ClientConfig c) { return true; }
    public void setEnableIcon(ClientConfig c, boolean v) {}

    public int absorptionTextX(ClientConfig c) { return 0; }
    public void setAbsorptionTextX(ClientConfig c, int v) {}
    public int absorptionTextY(ClientConfig c) { return 0; }
    public void setAbsorptionTextY(ClientConfig c, int v) {}
    public int absorptionTextWidth(ClientConfig c) { return 0; }
    public void setAbsorptionTextWidth(ClientConfig c, int v) {}
    public int absorptionTextHeight(ClientConfig c) { return 0; }
    public void setAbsorptionTextHeight(ClientConfig c, int v) {}
    public boolean enableAbsorptionText(ClientConfig c) { return true; }
    public void setEnableAbsorptionText(ClientConfig c, boolean v) {}
    public HorizontalAlignment absorptionTextAlign(ClientConfig c) { return HorizontalAlignment.CENTER; }
    public void setAbsorptionTextAlign(ClientConfig c, HorizontalAlignment v) {}

    public static final BarFieldAccess HEALTH = new BarFieldAccess() {
        public boolean hasAbsorptionText() { return true; }
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
        public int textWidth(ClientConfig c) { return c.healthTextWidth; }
        public void setTextWidth(ClientConfig c, int v) { c.healthTextWidth = v; }
        public int textHeight(ClientConfig c) { return c.healthTextHeight; }
        public void setTextHeight(ClientConfig c, int v) { c.healthTextHeight = v; }
        public int totalX(ClientConfig c) { return c.healthTotalXOffset; }
        public void setTotalX(ClientConfig c, int v) { c.healthTotalXOffset = v; }
        public int totalY(ClientConfig c) { return c.healthTotalYOffset; }
        public void setTotalY(ClientConfig c, int v) { c.healthTotalYOffset = v; }
        public int absorptionTextX(ClientConfig c) { return c.healthAbsorptionTextXOffset; }
        public void setAbsorptionTextX(ClientConfig c, int v) { c.healthAbsorptionTextXOffset = v; }
        public int absorptionTextY(ClientConfig c) { return c.healthAbsorptionTextYOffset; }
        public void setAbsorptionTextY(ClientConfig c, int v) { c.healthAbsorptionTextYOffset = v; }
        public int absorptionTextWidth(ClientConfig c) { return c.healthAbsorptionTextWidth; }
        public void setAbsorptionTextWidth(ClientConfig c, int v) { c.healthAbsorptionTextWidth = v; }
        public int absorptionTextHeight(ClientConfig c) { return c.healthAbsorptionTextHeight; }
        public void setAbsorptionTextHeight(ClientConfig c, int v) { c.healthAbsorptionTextHeight = v; }
        public boolean enableAbsorptionText(ClientConfig c) { return c.enableHealthAbsorptionText; }
        public void setEnableAbsorptionText(ClientConfig c, boolean v) { c.enableHealthAbsorptionText = v; }
        public HorizontalAlignment absorptionTextAlign(ClientConfig c) { return c.healthAbsorptionTextAlign; }
        public void setAbsorptionTextAlign(ClientConfig c, HorizontalAlignment v) { c.healthAbsorptionTextAlign = v; }
        public BarVisibility visibility(ClientConfig c) { return c.healthBarVisibility; }
        public void setVisibility(ClientConfig c, BarVisibility v) { c.healthBarVisibility = v; }
        public TextBehavior textBehavior(ClientConfig c) { return c.showHealthText; }
        public void setTextBehavior(ClientConfig c, TextBehavior v) { c.showHealthText = v; }
        public HUDPositioning.BarPlacement anchor(ClientConfig c) { return c.healthBarAnchor; }
        public void setAnchor(ClientConfig c, HUDPositioning.BarPlacement v) { c.healthBarAnchor = v; }
        public HorizontalAlignment textAlign(ClientConfig c) { return c.healthTextAlign; }
        public void setTextAlign(ClientConfig c, HorizontalAlignment v) { c.healthTextAlign = v; }
        public FillDirection fillDirection(ClientConfig c) { return c.healthFillDirection; }
        public void setFillDirection(ClientConfig c, FillDirection v) { c.healthFillDirection = v; }
        public boolean enableBackground(ClientConfig c) { return c.enableHealthBackground; }
        public void setEnableBackground(ClientConfig c, boolean v) { c.enableHealthBackground = v; }
        public boolean enableForeground(ClientConfig c) { return c.enableHealthForeground; }
        public void setEnableForeground(ClientConfig c, boolean v) { c.enableHealthForeground = v; }
        public void resetBehaviorFields(ClientConfig live, ClientConfig defaults) {
            live.healthBarBehavior = defaults.healthBarBehavior;
            live.healthBarAnchor = defaults.healthBarAnchor;
            live.healthBarVisibility = defaults.healthBarVisibility;
            live.showHealthText = defaults.showHealthText;
            live.healthTextAlign = defaults.healthTextAlign;
            live.healthFillDirection = defaults.healthFillDirection;
            live.enableHealthBackground = defaults.enableHealthBackground;
            live.enableHealthForeground = defaults.enableHealthForeground;
            live.enableHealthAbsorptionText = defaults.enableHealthAbsorptionText;
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
        public int textWidth(ClientConfig c) { return c.manaTextWidth; }
        public void setTextWidth(ClientConfig c, int v) { c.manaTextWidth = v; }
        public int textHeight(ClientConfig c) { return c.manaTextHeight; }
        public void setTextHeight(ClientConfig c, int v) { c.manaTextHeight = v; }
        public int totalX(ClientConfig c) { return c.manaTotalXOffset; }
        public void setTotalX(ClientConfig c, int v) { c.manaTotalXOffset = v; }
        public int totalY(ClientConfig c) { return c.manaTotalYOffset; }
        public void setTotalY(ClientConfig c, int v) { c.manaTotalYOffset = v; }
        public BarVisibility visibility(ClientConfig c) { return c.manaBarVisibility; }
        public void setVisibility(ClientConfig c, BarVisibility v) { c.manaBarVisibility = v; }
        public TextBehavior textBehavior(ClientConfig c) { return c.showManaText; }
        public void setTextBehavior(ClientConfig c, TextBehavior v) { c.showManaText = v; }
        public HUDPositioning.BarPlacement anchor(ClientConfig c) { return c.manaBarAnchor; }
        public void setAnchor(ClientConfig c, HUDPositioning.BarPlacement v) { c.manaBarAnchor = v; }
        public HorizontalAlignment textAlign(ClientConfig c) { return c.manaTextAlign; }
        public void setTextAlign(ClientConfig c, HorizontalAlignment v) { c.manaTextAlign = v; }
        public FillDirection fillDirection(ClientConfig c) { return c.manaFillDirection; }
        public void setFillDirection(ClientConfig c, FillDirection v) { c.manaFillDirection = v; }
        public boolean enableBackground(ClientConfig c) { return c.enableManaBackground; }
        public void setEnableBackground(ClientConfig c, boolean v) { c.enableManaBackground = v; }
        public boolean enableForeground(ClientConfig c) { return c.enableManaForeground; }
        public void setEnableForeground(ClientConfig c, boolean v) { c.enableManaForeground = v; }
        public void resetBehaviorFields(ClientConfig live, ClientConfig defaults) {
            live.manaBarBehavior = defaults.manaBarBehavior;
            live.manaBarAnchor = defaults.manaBarAnchor;
            live.manaBarVisibility = defaults.manaBarVisibility;
            live.showManaText = defaults.showManaText;
            live.manaTextAlign = defaults.manaTextAlign;
            live.manaFillDirection = defaults.manaFillDirection;
            live.enableManaBackground = defaults.enableManaBackground;
            live.enableManaForeground = defaults.enableManaForeground;
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
        public int textWidth(ClientConfig c) { return c.staminaTextWidth; }
        public void setTextWidth(ClientConfig c, int v) { c.staminaTextWidth = v; }
        public int textHeight(ClientConfig c) { return c.staminaTextHeight; }
        public void setTextHeight(ClientConfig c, int v) { c.staminaTextHeight = v; }
        public int totalX(ClientConfig c) { return c.staminaTotalXOffset; }
        public void setTotalX(ClientConfig c, int v) { c.staminaTotalXOffset = v; }
        public int totalY(ClientConfig c) { return c.staminaTotalYOffset; }
        public void setTotalY(ClientConfig c, int v) { c.staminaTotalYOffset = v; }
        public BarVisibility visibility(ClientConfig c) { return c.staminaBarVisibility; }
        public void setVisibility(ClientConfig c, BarVisibility v) { c.staminaBarVisibility = v; }
        public TextBehavior textBehavior(ClientConfig c) { return c.showStaminaText; }
        public void setTextBehavior(ClientConfig c, TextBehavior v) { c.showStaminaText = v; }
        public HUDPositioning.BarPlacement anchor(ClientConfig c) { return c.staminaBarAnchor; }
        public void setAnchor(ClientConfig c, HUDPositioning.BarPlacement v) { c.staminaBarAnchor = v; }
        public HorizontalAlignment textAlign(ClientConfig c) { return c.staminaTextAlign; }
        public void setTextAlign(ClientConfig c, HorizontalAlignment v) { c.staminaTextAlign = v; }
        public FillDirection fillDirection(ClientConfig c) { return c.staminaFillDirection; }
        public void setFillDirection(ClientConfig c, FillDirection v) { c.staminaFillDirection = v; }
        public boolean enableBackground(ClientConfig c) { return c.enableStaminaBackground; }
        public void setEnableBackground(ClientConfig c, boolean v) { c.enableStaminaBackground = v; }
        public boolean enableForeground(ClientConfig c) { return c.enableStaminaForeground; }
        public void setEnableForeground(ClientConfig c, boolean v) { c.enableStaminaForeground = v; }
        public void resetBehaviorFields(ClientConfig live, ClientConfig defaults) {
            live.staminaBarBehavior = defaults.staminaBarBehavior;
            live.staminaBarAnchor = defaults.staminaBarAnchor;
            live.staminaBarVisibility = defaults.staminaBarVisibility;
            live.showStaminaText = defaults.showStaminaText;
            live.staminaTextAlign = defaults.staminaTextAlign;
            live.staminaFillDirection = defaults.staminaFillDirection;
            live.enableStaminaBackground = defaults.enableStaminaBackground;
            live.enableStaminaForeground = defaults.enableStaminaForeground;
        }
    };

    public static final BarFieldAccess ARMOR = new BarFieldAccess() {
        public boolean hasIcon() { return true; }
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
        public int overlayWidth(ClientConfig c) { return c.armorOverlayWidth; }
        public void setOverlayWidth(ClientConfig c, int v) { c.armorOverlayWidth = v; }
        public int overlayHeight(ClientConfig c) { return c.armorOverlayHeight; }
        public void setOverlayHeight(ClientConfig c, int v) { c.armorOverlayHeight = v; }
        public int overlayX(ClientConfig c) { return c.armorOverlayXOffset; }
        public void setOverlayX(ClientConfig c, int v) { c.armorOverlayXOffset = v; }
        public int overlayY(ClientConfig c) { return c.armorOverlayYOffset; }
        public void setOverlayY(ClientConfig c, int v) { c.armorOverlayYOffset = v; }
        public int textX(ClientConfig c) { return c.armorTextXOffset; }
        public void setTextX(ClientConfig c, int v) { c.armorTextXOffset = v; }
        public int textY(ClientConfig c) { return c.armorTextYOffset; }
        public void setTextY(ClientConfig c, int v) { c.armorTextYOffset = v; }
        public int textWidth(ClientConfig c) { return c.armorTextWidth; }
        public void setTextWidth(ClientConfig c, int v) { c.armorTextWidth = v; }
        public int textHeight(ClientConfig c) { return c.armorTextHeight; }
        public void setTextHeight(ClientConfig c, int v) { c.armorTextHeight = v; }
        public int totalX(ClientConfig c) { return c.armorTotalXOffset; }
        public void setTotalX(ClientConfig c, int v) { c.armorTotalXOffset = v; }
        public int totalY(ClientConfig c) { return c.armorTotalYOffset; }
        public void setTotalY(ClientConfig c, int v) { c.armorTotalYOffset = v; }
        public int iconX(ClientConfig c) { return c.armorIconXOffset; }
        public void setIconX(ClientConfig c, int v) { c.armorIconXOffset = v; }
        public int iconY(ClientConfig c) { return c.armorIconYOffset; }
        public void setIconY(ClientConfig c, int v) { c.armorIconYOffset = v; }
        public int iconWidth(ClientConfig c) { return c.armorIconWidth; }
        public void setIconWidth(ClientConfig c, int v) { c.armorIconWidth = v; }
        public int iconHeight(ClientConfig c) { return c.armorIconHeight; }
        public void setIconHeight(ClientConfig c, int v) { c.armorIconHeight = v; }
        public boolean enableIcon(ClientConfig c) { return c.enableArmorIcon; }
        public void setEnableIcon(ClientConfig c, boolean v) { c.enableArmorIcon = v; }
        public BarVisibility visibility(ClientConfig c) { return c.armorBarVisibility; }
        public void setVisibility(ClientConfig c, BarVisibility v) { c.armorBarVisibility = v; }
        public TextBehavior textBehavior(ClientConfig c) { return c.showArmorText; }
        public void setTextBehavior(ClientConfig c, TextBehavior v) { c.showArmorText = v; }
        public HUDPositioning.BarPlacement anchor(ClientConfig c) { return c.armorBarAnchor; }
        public void setAnchor(ClientConfig c, HUDPositioning.BarPlacement v) { c.armorBarAnchor = v; }
        public HorizontalAlignment textAlign(ClientConfig c) { return c.armorTextAlign; }
        public void setTextAlign(ClientConfig c, HorizontalAlignment v) { c.armorTextAlign = v; }
        public FillDirection fillDirection(ClientConfig c) { return c.armorFillDirection; }
        public void setFillDirection(ClientConfig c, FillDirection v) { c.armorFillDirection = v; }
        public boolean enableBackground(ClientConfig c) { return c.enableArmorBackground; }
        public void setEnableBackground(ClientConfig c, boolean v) { c.enableArmorBackground = v; }
        public boolean enableForeground(ClientConfig c) { return c.enableArmorForeground; }
        public void setEnableForeground(ClientConfig c, boolean v) { c.enableArmorForeground = v; }
        public void resetBehaviorFields(ClientConfig live, ClientConfig defaults) {
            live.armorBarBehavior = defaults.armorBarBehavior;
            live.armorBarAnchor = defaults.armorBarAnchor;
            live.armorBarVisibility = defaults.armorBarVisibility;
            live.showArmorText = defaults.showArmorText;
            live.armorTextAlign = defaults.armorTextAlign;
            live.armorFillDirection = defaults.armorFillDirection;
            live.enableArmorBackground = defaults.enableArmorBackground;
            live.enableArmorForeground = defaults.enableArmorForeground;
            live.enableArmorIcon = defaults.enableArmorIcon;
        }
    };

    public static final BarFieldAccess AIR = new BarFieldAccess() {
        public boolean hasIcon() { return true; }
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
        public int overlayWidth(ClientConfig c) { return c.airOverlayWidth; }
        public void setOverlayWidth(ClientConfig c, int v) { c.airOverlayWidth = v; }
        public int overlayHeight(ClientConfig c) { return c.airOverlayHeight; }
        public void setOverlayHeight(ClientConfig c, int v) { c.airOverlayHeight = v; }
        public int overlayX(ClientConfig c) { return c.airOverlayXOffset; }
        public void setOverlayX(ClientConfig c, int v) { c.airOverlayXOffset = v; }
        public int overlayY(ClientConfig c) { return c.airOverlayYOffset; }
        public void setOverlayY(ClientConfig c, int v) { c.airOverlayYOffset = v; }
        public int textX(ClientConfig c) { return c.airTextXOffset; }
        public void setTextX(ClientConfig c, int v) { c.airTextXOffset = v; }
        public int textY(ClientConfig c) { return c.airTextYOffset; }
        public void setTextY(ClientConfig c, int v) { c.airTextYOffset = v; }
        public int textWidth(ClientConfig c) { return c.airTextWidth; }
        public void setTextWidth(ClientConfig c, int v) { c.airTextWidth = v; }
        public int textHeight(ClientConfig c) { return c.airTextHeight; }
        public void setTextHeight(ClientConfig c, int v) { c.airTextHeight = v; }
        public int totalX(ClientConfig c) { return c.airTotalXOffset; }
        public void setTotalX(ClientConfig c, int v) { c.airTotalXOffset = v; }
        public int totalY(ClientConfig c) { return c.airTotalYOffset; }
        public void setTotalY(ClientConfig c, int v) { c.airTotalYOffset = v; }
        public int iconX(ClientConfig c) { return c.airIconXOffset; }
        public void setIconX(ClientConfig c, int v) { c.airIconXOffset = v; }
        public int iconY(ClientConfig c) { return c.airIconYOffset; }
        public void setIconY(ClientConfig c, int v) { c.airIconYOffset = v; }
        public int iconWidth(ClientConfig c) { return c.airIconWidth; }
        public void setIconWidth(ClientConfig c, int v) { c.airIconWidth = v; }
        public int iconHeight(ClientConfig c) { return c.airIconHeight; }
        public void setIconHeight(ClientConfig c, int v) { c.airIconHeight = v; }
        public boolean enableIcon(ClientConfig c) { return c.enableAirIcon; }
        public void setEnableIcon(ClientConfig c, boolean v) { c.enableAirIcon = v; }
        public BarVisibility visibility(ClientConfig c) { return c.airBarVisibility; }
        public void setVisibility(ClientConfig c, BarVisibility v) { c.airBarVisibility = v; }
        public TextBehavior textBehavior(ClientConfig c) { return c.showAirText; }
        public void setTextBehavior(ClientConfig c, TextBehavior v) { c.showAirText = v; }
        public HUDPositioning.BarPlacement anchor(ClientConfig c) { return c.airBarAnchor; }
        public void setAnchor(ClientConfig c, HUDPositioning.BarPlacement v) { c.airBarAnchor = v; }
        public HorizontalAlignment textAlign(ClientConfig c) { return c.airTextAlign; }
        public void setTextAlign(ClientConfig c, HorizontalAlignment v) { c.airTextAlign = v; }
        public FillDirection fillDirection(ClientConfig c) { return c.airFillDirection; }
        public void setFillDirection(ClientConfig c, FillDirection v) { c.airFillDirection = v; }
        public boolean enableBackground(ClientConfig c) { return c.enableAirBackground; }
        public void setEnableBackground(ClientConfig c, boolean v) { c.enableAirBackground = v; }
        public boolean enableForeground(ClientConfig c) { return c.enableAirForeground; }
        public void setEnableForeground(ClientConfig c, boolean v) { c.enableAirForeground = v; }
        public void resetBehaviorFields(ClientConfig live, ClientConfig defaults) {
            live.airBarBehavior = defaults.airBarBehavior;
            live.airBarAnchor = defaults.airBarAnchor;
            live.airBarVisibility = defaults.airBarVisibility;
            live.showAirText = defaults.showAirText;
            live.airTextAlign = defaults.airTextAlign;
            live.airFillDirection = defaults.airFillDirection;
            live.enableAirBackground = defaults.enableAirBackground;
            live.enableAirForeground = defaults.enableAirForeground;
            live.enableAirIcon = defaults.enableAirIcon;
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
