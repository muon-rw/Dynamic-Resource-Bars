package dev.muon.dynamic_resource_bars.util;

/**
 * Identity of a single drawable layer within a bar's complex.
 *
 * <p>Capability flags drive editor behavior generically — {@link #isResizable()} controls
 * which layers expose drag handles in focus mode, {@link #isToggleable()} controls which
 * layers participate in the auto-toggle context-menu wiring. A layer that opts out of either
 * has its own bespoke handling (e.g. {@link #TEXT} uses {@code TextBehavior} for visibility
 * and the global text scale for size; {@link #BAR_MAIN} can't sensibly be hidden).
 */
public enum SubElementType {
    BACKGROUND("gui.dynamic_resource_bars.subelement.background", true, true),
    BAR_MAIN("gui.dynamic_resource_bars.subelement.bar_main", true, false),
    FOREGROUND("gui.dynamic_resource_bars.subelement.foreground", true, true),
    // TEXT carries its own bounding box now: vertical resize → font scale, horizontal resize
    // affects align math but is otherwise cosmetic. Visibility still uses TextBehavior, hence
    // toggleable=false here.
    TEXT("gui.dynamic_resource_bars.subelement.text", true, false),
    ICON("gui.dynamic_resource_bars.subelement.icon", true, true),
    // Absorption text is a per-bar feature on the health bar; its toggle flips the dedicated
    // {@code enableHealthAbsorptionText} field. Other bars don't override the no-op defaults.
    ABSORPTION_TEXT("gui.dynamic_resource_bars.subelement.absorption_text", true, true);

    private final String translationKey;
    private final boolean resizable;
    private final boolean toggleable;

    SubElementType(String translationKey, boolean resizable, boolean toggleable) {
        this.translationKey = translationKey;
        this.resizable = resizable;
        this.toggleable = toggleable;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean isResizable() {
        return resizable;
    }

    public boolean isToggleable() {
        return toggleable;
    }
}
