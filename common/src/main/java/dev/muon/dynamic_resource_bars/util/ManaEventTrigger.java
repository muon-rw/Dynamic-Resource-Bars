package dev.muon.dynamic_resource_bars.util;

public enum ManaEventTrigger {
    NEVER("gui.dynamic_resource_bars.mana_event.never"),
    ON_DRAIN("gui.dynamic_resource_bars.mana_event.on_drain"),
    ON_DRAIN_ATTEMPT("gui.dynamic_resource_bars.mana_event.on_drain_attempt");

    private final String translationKey;

    ManaEventTrigger(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
