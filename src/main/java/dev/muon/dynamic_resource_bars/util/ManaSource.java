package dev.muon.dynamic_resource_bars.util;

public enum ManaSource {
    NONE("none", "gui.dynamic_resource_bars.mana_source.none"),
    IRONS_SPELLBOOKS("irons_spellbooks", "gui.dynamic_resource_bars.mana_source.irons_spellbooks"),
    ARS_NOUVEAU("ars_nouveau", "gui.dynamic_resource_bars.mana_source.ars_nouveau"),
    RPG_MANA("rpgmana", "gui.dynamic_resource_bars.mana_source.rpg_mana"),
    MANA_ATTRIBUTES("manaattributes", "gui.dynamic_resource_bars.mana_source.mana_attributes");
    
    private final String modId;
    private final String translationKey;
    
    ManaSource(String modId, String translationKey) {
        this.modId = modId;
        this.translationKey = translationKey;
    }
    
    public String getModId() {
        return modId;
    }
    
    public String getTranslationKey() {
        return translationKey;
    }
    
    public ManaSource getNext() {
        ManaSource[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
} 