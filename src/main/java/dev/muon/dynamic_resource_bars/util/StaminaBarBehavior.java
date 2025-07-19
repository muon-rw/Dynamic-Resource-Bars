package dev.muon.dynamic_resource_bars.util;

public enum StaminaBarBehavior {
    FOOD("food", "gui.dynamic_resource_bars.stamina_bar_behavior.food"),
    STAMINA_ATTRIBUTES("stamina_attributes", "gui.dynamic_resource_bars.stamina_bar_behavior.stamina_attributes"),
    OFF("off", "gui.dynamic_resource_bars.stamina_bar_behavior.off");

    private final String key;
    private final String translationKey;

    StaminaBarBehavior(String key, String translationKey) {
        this.key = key;
        this.translationKey = translationKey;
    }

    public String getKey() {
        return key;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public static StaminaBarBehavior fromKey(String key) {
        for (StaminaBarBehavior behavior : values()) {
            if (behavior.key.equals(key)) {
                return behavior;
            }
        }
        return FOOD; // Default
    }

    public StaminaBarBehavior getNext() {
        StaminaBarBehavior[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
} 