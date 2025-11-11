package dev.muon.dynamic_resource_bars.util;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
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
    private static final Map<ResourceLocation, AnimationMetadata.BarDimensions> dimensionsCache = new HashMap<>();
    private static final Map<ResourceLocation, AnimationMetadata.ScalingInfo> scalingCache = new HashMap<>();
    private static final Map<ResourceLocation, AnimationMetadata.TextureDimensions> textureDimensionsCache = new HashMap<>();
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
        dimensionsCache.clear();
        scalingCache.clear();
        textureDimensionsCache.clear();
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
     * Internal method to get or load animation data
     */
    private static AnimationMetadata.AnimationData getOrLoad(ResourceLocation location) {
        // Refresh cache if needed
        if (needsRefresh) {
            DynamicResourceBars.LOGGER.info("CACHE: Refreshing animation cache...");
            animationCache.clear();
            dimensionsCache.clear();
            scalingCache.clear();
            needsRefresh = false;
        }
        
        // Return cached value if available
        if (animationCache.containsKey(location)) {
            return animationCache.get(location);
        }
        
        // Load and cache
        DynamicResourceBars.LOGGER.info("CACHE: Loading and caching animation data for {}", location);
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        AnimationMetadata.AnimationData data = AnimationMetadata.loadAnimationData(
            resourceManager, location
        );
        animationCache.put(location, data);
        DynamicResourceBars.LOGGER.info("CACHE: Cached animation data for {}: frameHeight={}, totalFrames={}, textureSize={}x{}", 
            location, data.frameHeight, data.totalFrames, data.textureWidth, data.textureHeight);
        return data;
    }
    
    /**
     * Get or load scaling info for any texture with specified type.
     * Public method for textures without dedicated getters (e.g., armor_background).
     */
    public static AnimationMetadata.ScalingInfo getScalingOrLoad(ResourceLocation location, AnimationMetadata.TextureType textureType) {
        // Refresh cache if needed
        if (needsRefresh) {
            animationCache.clear();
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
    
    /**
     * Get texture dimensions for any texture (backgrounds, overlays, etc.)
     * Used to get correct texture sheet dimensions for non-animated textures.
     */
    public static AnimationMetadata.TextureDimensions getTextureDimensions(ResourceLocation location) {
        if (needsRefresh) {
            textureDimensionsCache.clear();
        }
        
        if (textureDimensionsCache.containsKey(location)) {
            return textureDimensionsCache.get(location);
        }
        
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        AnimationMetadata.TextureDimensions dims = AnimationMetadata.loadTextureDimensions(resourceManager, location);
        textureDimensionsCache.put(location, dims);
        return dims;
    }
}

