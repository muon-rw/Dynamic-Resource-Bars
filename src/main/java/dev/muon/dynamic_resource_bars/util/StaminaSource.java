package dev.muon.dynamic_resource_bars.util;

public enum StaminaSource {
    FOOD("minecraft", "gui.dynamic_resource_bars.stamina_source.food"),
    STAMINA_ATTRIBUTES("staminaattributes", "gui.dynamic_resource_bars.stamina_source.stamina_attributes");
    
    private final String modId;
    private final String translationKey;
    
    StaminaSource(String modId, String translationKey) {
        this.modId = modId;
        this.translationKey = translationKey;
    }
    
    public String getModId() {
        return modId;
    }
    
    public String getTranslationKey() {
        return translationKey;
    }
    
    public StaminaSource getNext() {
        StaminaSource[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
} 