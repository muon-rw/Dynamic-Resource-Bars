package dev.muon.dynamic_resource_bars.util;

public enum FillDirection {
    HORIZONTAL("fill_direction.horizontal"),
    VERTICAL("fill_direction.vertical");

    private final String translationKey;

    FillDirection(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
