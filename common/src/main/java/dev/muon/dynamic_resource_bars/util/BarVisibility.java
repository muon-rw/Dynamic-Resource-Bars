package dev.muon.dynamic_resource_bars.util;

/**
 * 3-state visibility rule for a bar (and its overlays). Used to be a {@code fadeWhenFull}
 * boolean — expanded to ALWAYS / NEVER / SMART_FADE so the user can hide a bar entirely
 * without flipping the {@code BarRenderBehavior} away from {@code CUSTOM}.
 *
 * <p>{@code SMART_FADE}'s exact trigger is per-bar (health fades at full, armor fades at empty,
 * mana defers to its provider, air keeps showing underwater) — see each renderer's
 * {@code shouldFadeWhenFull} override. ALWAYS/NEVER short-circuit to false/true unconditionally.
 */
public enum BarVisibility {
    ALWAYS("gui.dynamic_resource_bars.bar_visibility.always"),
    SMART_FADE("gui.dynamic_resource_bars.bar_visibility.smart_fade"),
    NEVER("gui.dynamic_resource_bars.bar_visibility.never");

    private final String translationKey;

    BarVisibility(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
