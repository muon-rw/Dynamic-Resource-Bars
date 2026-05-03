package dev.muon.dynamic_resource_bars.util;

public enum DraggableElement {
    HEALTH_BAR("gui.dynamic_resource_bars.element.health"),
    MANA_BAR("gui.dynamic_resource_bars.element.mana"),
    STAMINA_BAR("gui.dynamic_resource_bars.element.stamina"),
    ARMOR_BAR("gui.dynamic_resource_bars.element.armor"),
    AIR_BAR("gui.dynamic_resource_bars.element.air");

    private final String translationKey;

    DraggableElement(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}

