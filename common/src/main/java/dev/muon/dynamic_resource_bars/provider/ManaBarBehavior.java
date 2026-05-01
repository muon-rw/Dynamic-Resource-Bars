package dev.muon.dynamic_resource_bars.provider;

public enum ManaBarBehavior {
    OFF("gui.dynamic_resource_bars.mana_behavior.off"),
    COMBAT_ATTRIBUTES("gui.dynamic_resource_bars.mana_behavior.combat_attributes");
    
    private final String translationKey;
    
    ManaBarBehavior(String translationKey) {
        this.translationKey = translationKey;
    }
    
    public String getTranslationKey() {
        return translationKey;
    }
    
    public ManaBarBehavior getNext() {
        ManaBarBehavior[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
} 