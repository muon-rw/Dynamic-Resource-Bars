package dev.muon.dynamic_resource_bars.util;

public enum HorizontalAlignment {
    LEFT("horizontal_alignment.left"),
    CENTER("horizontal_alignment.center"),
    RIGHT("horizontal_alignment.right");

    private final String translationKey;

    HorizontalAlignment(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
