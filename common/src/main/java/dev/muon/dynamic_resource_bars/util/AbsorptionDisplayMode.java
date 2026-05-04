package dev.muon.dynamic_resource_bars.util;

/**
 * Controls how absorption hearts are visualised on the health bar.
 *
 * <p>{@link #OVERLAY} keeps the legacy behaviour: the health fill stays at {@code current/max}
 * width and a pulsing {@code absorption_overlay.png} is layered over the entire bar zone whenever
 * absorption is present.
 *
 * <p>{@link #SQUEEZE} treats the absorption pool as part of the denominator: the bar fill ratio
 * becomes {@code current / (max + absorption)}, so the live health portion shrinks when absorption
 * is active and the freed-up space is filled by {@code absorption_bar.png}. The pulsing overlay
 * is suppressed in this mode — the bar texture itself communicates the state.
 */
public enum AbsorptionDisplayMode {
    OVERLAY("gui.dynamic_resource_bars.absorption_display_mode.overlay"),
    SQUEEZE("gui.dynamic_resource_bars.absorption_display_mode.squeeze");

    private final String translationKey;

    AbsorptionDisplayMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
