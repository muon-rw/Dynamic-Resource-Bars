package dev.muon.dynamic_resource_bars.util;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache for animation metadata to avoid reloading .mcmeta files every frame.
 * Automatically refreshes when resource packs are reloaded.
 */
public class AnimationMetadataCache {
    
    private static final Map<ResourceLocation, AnimationMetadata.AnimationData> animationCache = new HashMap<>();
    private static final Map<ResourceLocation, AnimationMetadata.MaskInfo> maskCache = new HashMap<>();
    private static final Map<ResourceLocation, AnimationMetadata.BarDimensions> dimensionsCache = new HashMap<>();
    private static final Map<ResourceLocation, AnimationMetadata.ScalingInfo> scalingCache = new HashMap<>();
    private static boolean needsRefresh = true;
    
    /**
     * Mark cache as needing refresh (called on resource pack reload)
     */
    // TODO: doesn't seem to be hooked
    public static void markDirty() {
        needsRefresh = true;
        DynamicResourceBars.LOGGER.debug("Animation metadata cache marked for refresh");
    }
    
    /**
     * Clear the cache (called on resource pack reload)
     */
    // TODO: doesn't seem to be hooked
    public static void clear() {
        animationCache.clear();
        maskCache.clear();
        dimensionsCache.clear();
        needsRefresh = true;
        DynamicResourceBars.LOGGER.info("Texture metadata cache cleared");
    }
    
    /**
     * Get animation data for health bar.
     * All health bar variants (normal, poisoned, withered, etc.) share the same animation settings.
     */
    public static AnimationMetadata.AnimationData getHealthBarAnimation() {
        ResourceLocation location = DynamicResourceBars.loc("textures/gui/health_bar.png");
        return getOrLoad(location);
    }
    
    /**
     * Get animation data for stamina bar.
     * All stamina bar variants (normal, critical, mounted, etc.) share the same animation settings.
     */
    public static AnimationMetadata.AnimationData getStaminaBarAnimation() {
        ResourceLocation location = DynamicResourceBars.loc("textures/gui/stamina_bar.png");
        return getOrLoad(location);
    }
    
    /**
     * Get animation data for mana bar
     */
    public static AnimationMetadata.AnimationData getManaBarAnimation() {
        ResourceLocation location = DynamicResourceBars.loc("textures/gui/mana_bar.png");
        return getOrLoad(location);
    }
    
    /**
     * Get animation data for air bar
     */
    public static AnimationMetadata.AnimationData getAirBarAnimation() {
        ResourceLocation location = DynamicResourceBars.loc("textures/gui/air_bar.png");
        return getOrLoad(location);
    }
    
    /**
     * Get mask info for health bar
     */
    public static AnimationMetadata.MaskInfo getHealthBarMask() {
        ResourceLocation location = DynamicResourceBars.loc("textures/gui/health_bar.png");
        return getMaskOrLoad(location);
    }
    
    /**
     * Get mask info for stamina bar
     */
    public static AnimationMetadata.MaskInfo getStaminaBarMask() {
        ResourceLocation location = DynamicResourceBars.loc("textures/gui/stamina_bar.png");
        return getMaskOrLoad(location);
    }
    
    /**
     * Get mask info for mana bar
     */
    public static AnimationMetadata.MaskInfo getManaBarMask() {
        ResourceLocation location = DynamicResourceBars.loc("textures/gui/mana_bar.png");
        return getMaskOrLoad(location);
    }
    
    /**
     * Get mask info for air bar
     */
    public static AnimationMetadata.MaskInfo getAirBarMask() {
        ResourceLocation location = DynamicResourceBars.loc("textures/gui/air_bar.png");
        return getMaskOrLoad(location);
    }
    
    /**
     * Internal method to get or load animation data
     */
    private static AnimationMetadata.AnimationData getOrLoad(ResourceLocation location) {
        // Refresh cache if needed
        if (needsRefresh) {
            animationCache.clear();
            maskCache.clear();
            dimensionsCache.clear();
            scalingCache.clear();
            needsRefresh = false;
        }
        
        // Return cached value if available
        if (animationCache.containsKey(location)) {
            return animationCache.get(location);
        }
        
        // Load and cache
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        AnimationMetadata.AnimationData data = AnimationMetadata.loadAnimationData(
            resourceManager, location
        );
        animationCache.put(location, data);
        return data;
    }
    
    /**
     * Internal method to get or load mask info
     */
    private static AnimationMetadata.MaskInfo getMaskOrLoad(ResourceLocation location) {
        // Refresh cache if needed (same check as animation)
        if (needsRefresh) {
            animationCache.clear();
            maskCache.clear();
            dimensionsCache.clear();
            scalingCache.clear();
            needsRefresh = false;
        }
        
        // Return cached value if available
        if (maskCache.containsKey(location)) {
            return maskCache.get(location);
        }
        
        // Load and cache
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        AnimationMetadata.MaskInfo maskInfo = AnimationMetadata.loadMaskInfo(
            resourceManager, location
        );
        maskCache.put(location, maskInfo);
        return maskInfo;
    }
    
    /**
     * Internal method to get or load scaling info
     */
    private static AnimationMetadata.ScalingInfo getScalingOrLoad(ResourceLocation location, AnimationMetadata.TextureType textureType) {
        // Refresh cache if needed
        if (needsRefresh) {
            animationCache.clear();
            maskCache.clear();
            dimensionsCache.clear();
            scalingCache.clear();
            needsRefresh = false;
        }
        
        // Return cached value if available
        if (scalingCache.containsKey(location)) {
            return scalingCache.get(location);
        }
        
        // Load and cache
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        AnimationMetadata.ScalingInfo scalingInfo = AnimationMetadata.loadScalingInfo(
            resourceManager, location, textureType
        );
        scalingCache.put(location, scalingInfo);
        return scalingInfo;
    }
    
    // === Scaling info getters ===
    
    public static AnimationMetadata.ScalingInfo getHealthBackgroundScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/health_background.png"), 
            AnimationMetadata.TextureType.BACKGROUND);
    }
    
    public static AnimationMetadata.ScalingInfo getHealthForegroundScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/health_foreground.png"), 
            AnimationMetadata.TextureType.FOREGROUND);
    }
    
    public static AnimationMetadata.ScalingInfo getManaBackgroundScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/mana_background.png"), 
            AnimationMetadata.TextureType.BACKGROUND);
    }
    
    public static AnimationMetadata.ScalingInfo getManaForegroundScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/mana_foreground.png"), 
            AnimationMetadata.TextureType.FOREGROUND);
    }
    
    public static AnimationMetadata.ScalingInfo getStaminaBackgroundScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/stamina_background.png"), 
            AnimationMetadata.TextureType.BACKGROUND);
    }
    
    public static AnimationMetadata.ScalingInfo getStaminaForegroundScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/stamina_foreground.png"), 
            AnimationMetadata.TextureType.FOREGROUND);
    }
    
    public static AnimationMetadata.ScalingInfo getAirBackgroundScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/air_background.png"), 
            AnimationMetadata.TextureType.BACKGROUND);
    }
    
    // Overlay scaling getters
    public static AnimationMetadata.ScalingInfo getAbsorptionOverlayScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/absorption_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
    
    public static AnimationMetadata.ScalingInfo getRegenerationOverlayScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/regeneration_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
    
    public static AnimationMetadata.ScalingInfo getHeatOverlayScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/heat_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
    
    public static AnimationMetadata.ScalingInfo getColdOverlayScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/cold_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
    
    public static AnimationMetadata.ScalingInfo getWetnessOverlayScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/wetness_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_STATIC);
    }
    
    public static AnimationMetadata.ScalingInfo getHardcoreOverlayScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/hardcore_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_STATIC);
    }
    
    public static AnimationMetadata.ScalingInfo getComfortOverlayScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/comfort_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
    
    public static AnimationMetadata.ScalingInfo getNourishmentOverlayScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/nourishment_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
    
    public static AnimationMetadata.ScalingInfo getProtectionOverlayScaling() {
        return getScalingOrLoad(DynamicResourceBars.loc("textures/gui/protection_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
}

