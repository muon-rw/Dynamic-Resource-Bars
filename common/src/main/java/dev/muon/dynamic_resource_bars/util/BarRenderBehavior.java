package dev.muon.dynamic_resource_bars.util;

public enum BarRenderBehavior {
    VANILLA("gui.dynamic_resource_bars.bar_behavior.vanilla"),
    CUSTOM("gui.dynamic_resource_bars.bar_behavior.custom"),
    HIDDEN("gui.dynamic_resource_bars.bar_behavior.hidden");

    private final String translationKey;

    BarRenderBehavior(String translationKey) { this.translationKey = translationKey; }

    public String getTranslationKey() { return translationKey; }
}
