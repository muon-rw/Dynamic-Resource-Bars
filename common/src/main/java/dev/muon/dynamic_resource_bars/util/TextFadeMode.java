package dev.muon.dynamic_resource_bars.util;

public enum TextFadeMode {
    DRAIN("gui.dynamic_resource_bars.text_fade_mode.drain"),
    ALPHA("gui.dynamic_resource_bars.text_fade_mode.alpha");

    private final String translationKey;

    TextFadeMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
