package dev.muon.dynamic_resource_bars.util;


public enum TextBehavior {
    ALWAYS("text_behavior.always"),
    NEVER("text_behavior.never"),
    WHEN_NOT_FULL("text_behavior.when_not_full");

    private final String translationKey;

    TextBehavior(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
