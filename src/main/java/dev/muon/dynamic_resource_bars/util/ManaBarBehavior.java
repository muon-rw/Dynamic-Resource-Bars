package dev.muon.dynamic_resource_bars.util;

public enum ManaBarBehavior {
    OFF("gui.dynamic_resource_bars.mana_behavior.off"),
    IRONS_SPELLBOOKS("gui.dynamic_resource_bars.mana_behavior.irons_spellbooks"),
    ARS_NOUVEAU("gui.dynamic_resource_bars.mana_behavior.ars_nouveau"),
    RPG_MANA("gui.dynamic_resource_bars.mana_behavior.rpg_mana"),
    MANA_ATTRIBUTES("gui.dynamic_resource_bars.mana_behavior.mana_attributes");
    
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