package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.StaminaProvider;
import dev.muon.dynamic_resource_bars.provider.StaminaProviderRegistry;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import dev.muon.dynamic_resource_bars.util.PlatformUtil;
import net.minecraft.client.Minecraft;

public class StaminaProviderManager {
    private static FoodStaminaProvider foodProvider;
    private static StaminaAttributesProvider staminaAttributesProvider;
    
    // Empty provider for when stamina bar is OFF
    private static final StaminaProvider EMPTY_PROVIDER = new StaminaProvider() {
        @Override
        public float getCurrentStamina(net.minecraft.world.entity.player.Player player) { return 0; }
        
        @Override
        public float getMaxStamina(net.minecraft.world.entity.player.Player player) { return 0; }
        
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
        StaminaProviderRegistry.clear();
        
        // Always register food provider (it's built-in)
        if (foodProvider == null) foodProvider = new FoodStaminaProvider();
        StaminaProviderRegistry.registerProvider(() -> foodProvider);
        
        // Register stamina attributes if loaded
        if (PlatformUtil.isModLoaded("staminaattributes")) {
            if (staminaAttributesProvider == null) staminaAttributesProvider = new StaminaAttributesProvider();
            StaminaProviderRegistry.registerProvider(() -> staminaAttributesProvider);
        }
        
        // Always register the empty provider for "OFF"
        StaminaProviderRegistry.registerProvider(() -> EMPTY_PROVIDER);
    }
    
    public static StaminaProvider getProviderForBehavior(StaminaBarBehavior behavior) {
        switch (behavior) {
            case FOOD:
                return foodProvider != null ? foodProvider : EMPTY_PROVIDER;
            case STAMINA_ATTRIBUTES:
                return staminaAttributesProvider != null ? staminaAttributesProvider : EMPTY_PROVIDER;
            case OFF:
            default:
                return EMPTY_PROVIDER;
        }
    }
    
    public static StaminaProvider getCurrentProvider() {
        StaminaBarBehavior currentBehavior = ModConfigManager.getClient().staminaBarBehavior;
        return getProviderForBehavior(currentBehavior);
    }
    
    public static boolean isModLoaded(String modId) {
        return PlatformUtil.isModLoaded(modId);
    }

    public static boolean isModLoaded(StaminaBarBehavior behavior) {
        switch (behavior) {
            case FOOD:
                return true; // Always available
            case STAMINA_ATTRIBUTES:
                return PlatformUtil.isModLoaded("staminaattributes");
            case OFF:
                return true;
            default:
                return false;
        }
    }
    
    public static boolean hasAnyStaminaMods() {
        // Food is always available, so we only check for additional mods
        return PlatformUtil.isModLoaded("staminaattributes");
    }
    
    public static void initialize() {
        // Initialize providers
        foodProvider = new FoodStaminaProvider();
        
        if (PlatformUtil.isModLoaded("staminaattributes")) {
            staminaAttributesProvider = new StaminaAttributesProvider();
        }
        
        // Set the active provider based on config
        updateActiveProvider();
    }
    
    public static void updateActiveProvider() {
        StaminaBarBehavior behavior = ModConfigManager.getClient().staminaBarBehavior;
        if (behavior == null) {
            behavior = StaminaBarBehavior.FOOD;
        }
        
        StaminaProvider newProvider = EMPTY_PROVIDER;
        
        switch(behavior) {
            case FOOD:
                if (foodProvider != null) newProvider = foodProvider;
                break;
            case STAMINA_ATTRIBUTES:
                if (staminaAttributesProvider != null) newProvider = staminaAttributesProvider;
                break;
        }
        
        StaminaProviderRegistry.setActiveProvider(newProvider);
    }
} 