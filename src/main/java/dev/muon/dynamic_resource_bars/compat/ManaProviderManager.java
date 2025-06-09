package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.ManaProvider;
import dev.muon.dynamic_resource_bars.util.ManaProviderRegistry;
import dev.muon.dynamic_resource_bars.util.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.util.PlatformUtil;
import net.minecraft.client.Minecraft;

public class ManaProviderManager {
    private static IronsSpellbooksManaProvider ironsProvider;
    private static ArsNouveauManaProvider arsProvider;
    private static RPGManaManaProvider rpgManaProvider;
    private static ManaAttributesManaProvider manaAttributesProvider;
    
    // Empty provider for when no mana mod is selected
    private static final ManaProvider EMPTY_PROVIDER = new ManaProvider() {
        @Override
        public double getCurrentMana() { return 0; }
        
        @Override
        public float getMaxMana() { return 0; }
        
        @Override
        public float getReservedMana() { return 0; }
        
        @Override
        public long getGameTime() {
            if (Minecraft.getInstance().level != null) {
                return Minecraft.getInstance().level.getGameTime();
            }
            return 0;
        }
    };
    
    public static void init() {
        // Register all available providers
        ManaProviderRegistry.clear();
        
        // Always register the empty provider for "OFF"
        ManaProviderRegistry.registerProvider(() -> EMPTY_PROVIDER);
        
        // Register providers for mods that are loaded
        if (PlatformUtil.isModLoaded("irons_spellbooks")) {
            if (ironsProvider == null) ironsProvider = new IronsSpellbooksManaProvider();
            ManaProviderRegistry.registerProvider(() -> ironsProvider);
        }
        if (PlatformUtil.isModLoaded("ars_nouveau")) {
            if (arsProvider == null) arsProvider = new ArsNouveauManaProvider();
            ManaProviderRegistry.registerProvider(() -> arsProvider);
        }
        if (PlatformUtil.isModLoaded("rpgmana")) {
            if (rpgManaProvider == null) rpgManaProvider = new RPGManaManaProvider();
            ManaProviderRegistry.registerProvider(() -> rpgManaProvider);
        }
        if (PlatformUtil.isModLoaded("manaattributes")) {
            if (manaAttributesProvider == null) manaAttributesProvider = new ManaAttributesManaProvider();
            ManaProviderRegistry.registerProvider(() -> manaAttributesProvider);
        }
    }
    
    public static ManaProvider getProviderForBehavior(ManaBarBehavior behavior) {
        switch (behavior) {
            case IRONS_SPELLBOOKS:
                return ironsProvider != null ? ironsProvider : EMPTY_PROVIDER;
            case ARS_NOUVEAU:
                return arsProvider != null ? arsProvider : EMPTY_PROVIDER;
            case RPG_MANA:
                return rpgManaProvider != null ? rpgManaProvider : EMPTY_PROVIDER;
            case MANA_ATTRIBUTES:
                return manaAttributesProvider != null ? manaAttributesProvider : EMPTY_PROVIDER;
            case OFF:
            default:
                return EMPTY_PROVIDER;
        }
    }
    
    public static ManaProvider getCurrentProvider() {
        ManaBarBehavior currentBehavior = ModConfigManager.getClient().manaBarBehavior;
        return getProviderForBehavior(currentBehavior);
    }
    
    public static boolean isModLoaded(String modId) {
        return PlatformUtil.isModLoaded(modId);
    }

    public static boolean isModLoaded(ManaBarBehavior behavior) {
        switch (behavior) {
            case IRONS_SPELLBOOKS:
                return PlatformUtil.isModLoaded("irons_spellbooks");
            case ARS_NOUVEAU:
                return PlatformUtil.isModLoaded("ars_nouveau");
            case RPG_MANA:
                return PlatformUtil.isModLoaded("rpgmana");
            case MANA_ATTRIBUTES:
                return PlatformUtil.isModLoaded("manaattributes");
            case OFF:
                return true;
            default:
                return false;
        }
    }
    
    public static boolean hasAnyManaMods() {
        return PlatformUtil.isModLoaded("irons_spellbooks") ||
               PlatformUtil.isModLoaded("ars_nouveau") ||
               PlatformUtil.isModLoaded("rpgmana") ||
               PlatformUtil.isModLoaded("manaattributes");
    }
    
    public static void initialize() {
        // Initialize providers if mods are loaded
        if (PlatformUtil.isModLoaded("irons_spellbooks")) {
            ironsProvider = new IronsSpellbooksManaProvider();
        }
        if (PlatformUtil.isModLoaded("ars_nouveau")) {
            arsProvider = new ArsNouveauManaProvider();
        }
        if (PlatformUtil.isModLoaded("rpgmana")) {
            rpgManaProvider = new RPGManaManaProvider();
        }
        if (PlatformUtil.isModLoaded("manaattributes")) {
            manaAttributesProvider = new ManaAttributesManaProvider();
        }
        
        // Set the active provider based on config
        updateActiveProvider();
    }
    
    public static void updateActiveProvider() {
        ManaBarBehavior behavior = ModConfigManager.getClient().manaBarBehavior;
        if (behavior == null) {
            behavior = ManaBarBehavior.OFF;
        }
        
        ManaProvider newProvider = EMPTY_PROVIDER;
        
        switch(behavior) {
            case IRONS_SPELLBOOKS:
                if (ironsProvider != null) newProvider = ironsProvider;
                break;
            case ARS_NOUVEAU:
                if (arsProvider != null) newProvider = arsProvider;
                break;
            case RPG_MANA:
                if (rpgManaProvider != null) newProvider = rpgManaProvider;
                break;
            case MANA_ATTRIBUTES:
                if (manaAttributesProvider != null) newProvider = manaAttributesProvider;
                break;
        }
        
        ManaProviderRegistry.setActiveProvider(newProvider);
    }
} 