package dev.muon.dynamic_resource_bars.util;

import dev.muon.dynamic_resource_bars.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache for animation metadata to avoid reloading .mcmeta files every frame.
 * Automatically refreshes when resource packs are reloaded.
 */
public class AnimationMetadataCache {
    
    private static final Map<Identifier, AnimationMetadata.AnimationData> animationCache = new HashMap<>();
    private static final Map<Identifier, AnimationMetadata.BarDimensions> dimensionsCache = new HashMap<>();
    private static final Map<Identifier, AnimationMetadata.ScalingInfo> scalingCache = new HashMap<>();
    private static final Map<Identifier, AnimationMetadata.TextureDimensions> textureDimensionsCache = new HashMap<>();
    private static boolean needsRefresh = true;
    
    /** Mark cache as needing refresh — called on resource pack reload via the loader's reload listener. */
    public static void markDirty() {
        needsRefresh = true;
        Constants.LOG.debug("Animation metadata cache marked for refresh");
    }

    /** Clear the cache — called on resource pack reload via the loader's reload listener. */
    public static void clear() {
        animationCache.clear();
        dimensionsCache.clear();
        scalingCache.clear();
        textureDimensionsCache.clear();
        needsRefresh = true;
        Constants.LOG.debug("Texture metadata cache cleared");
    }
    
    /**
     * Get animation data for health bar.
     * All health bar variants (normal, poisoned, withered, etc.) share the same animation settings.
     */
    public static AnimationMetadata.AnimationData getHealthBarAnimation() {
        Identifier location = Constants.loc("textures/gui/health_bar.png");
        return getOrLoad(location);
    }

    /** Animation data for the SQUEEZE-mode absorption fill texture. */
    public static AnimationMetadata.AnimationData getAbsorptionBarAnimation() {
        Identifier location = Constants.loc("textures/gui/absorption_bar.png");
        return getOrLoad(location);
    }
    
    /**
     * Get animation data for stamina bar.
     * All stamina bar variants (normal, critical, mounted, etc.) share the same animation settings.
     */
    public static AnimationMetadata.AnimationData getStaminaBarAnimation() {
        Identifier location = Constants.loc("textures/gui/stamina_bar.png");
        return getOrLoad(location);
    }
    
    /**
     * Get animation data for mana bar
     */
    public static AnimationMetadata.AnimationData getManaBarAnimation() {
        Identifier location = Constants.loc("textures/gui/mana_bar.png");
        return getOrLoad(location);
    }
    
    /**
     * Get animation data for air bar
     */
    public static AnimationMetadata.AnimationData getAirBarAnimation() {
        Identifier location = Constants.loc("textures/gui/air_bar.png");
        return getOrLoad(location);
    }

    /**
     * Get animation data for armor bar (static — no .mcmeta — but the cache returns a single-frame stub).
     */
    public static AnimationMetadata.AnimationData getArmorBarAnimation() {
        Identifier location = Constants.loc("textures/gui/armor_bar.png");
        return getOrLoad(location);
    }
    
    /**
     * Internal method to get or load animation data
     */
    private static AnimationMetadata.AnimationData getOrLoad(Identifier location) {
        if (needsRefresh) {
            Constants.LOG.debug("CACHE: Refreshing animation cache...");
            animationCache.clear();
            dimensionsCache.clear();
            scalingCache.clear();
            needsRefresh = false;
        }

        if (animationCache.containsKey(location)) {
            return animationCache.get(location);
        }

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        AnimationMetadata.AnimationData data = AnimationMetadata.loadAnimationData(
            resourceManager, location
        );
        animationCache.put(location, data);
        Constants.LOG.debug("CACHE: Cached animation data for {}: frameHeight={}, totalFrames={}, textureSize={}x{}",
            location, data.frameHeight, data.totalFrames, data.textureWidth, data.textureHeight);
        return data;
    }
    
    /**
     * Get or load scaling info for any texture with specified type.
     * Public method for textures without dedicated getters (e.g., armor_background).
     */
    public static AnimationMetadata.ScalingInfo getScalingOrLoad(Identifier location, AnimationMetadata.TextureType textureType) {
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
        return getScalingOrLoad(Constants.loc("textures/gui/health_background.png"), 
            AnimationMetadata.TextureType.BACKGROUND);
    }
    
    public static AnimationMetadata.ScalingInfo getHealthForegroundScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/health_foreground.png"), 
            AnimationMetadata.TextureType.FOREGROUND);
    }
    
    public static AnimationMetadata.ScalingInfo getManaBackgroundScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/mana_background.png"), 
            AnimationMetadata.TextureType.BACKGROUND);
    }
    
    public static AnimationMetadata.ScalingInfo getManaForegroundScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/mana_foreground.png"), 
            AnimationMetadata.TextureType.FOREGROUND);
    }
    
    public static AnimationMetadata.ScalingInfo getStaminaBackgroundScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/stamina_background.png"), 
            AnimationMetadata.TextureType.BACKGROUND);
    }
    
    public static AnimationMetadata.ScalingInfo getStaminaForegroundScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/stamina_foreground.png"), 
            AnimationMetadata.TextureType.FOREGROUND);
    }
    
    public static AnimationMetadata.ScalingInfo getAirBackgroundScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/air_background.png"),
            AnimationMetadata.TextureType.BACKGROUND);
    }

    public static AnimationMetadata.ScalingInfo getArmorForegroundScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/armor_foreground.png"),
            AnimationMetadata.TextureType.FOREGROUND);
    }

    public static AnimationMetadata.ScalingInfo getAirForegroundScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/air_foreground.png"),
            AnimationMetadata.TextureType.FOREGROUND);
    }
    
    // Overlay scaling getters
    public static AnimationMetadata.ScalingInfo getAbsorptionOverlayScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/absorption_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
    
    public static AnimationMetadata.ScalingInfo getRegenerationOverlayScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/regeneration_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
    
    public static AnimationMetadata.ScalingInfo getHeatOverlayScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/heat_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
    
    public static AnimationMetadata.ScalingInfo getColdOverlayScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/cold_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
    
    public static AnimationMetadata.ScalingInfo getWetnessOverlayScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/wetness_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_STATIC);
    }
    
    public static AnimationMetadata.ScalingInfo getHardcoreOverlayScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/hardcore_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_STATIC);
    }
    
    public static AnimationMetadata.ScalingInfo getComfortOverlayScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/comfort_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
    
    public static AnimationMetadata.ScalingInfo getNourishmentOverlayScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/nourishment_overlay.png"),
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }

    public static AnimationMetadata.ScalingInfo getSaturationOverlayScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/saturation_overlay.png"),
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
    
    public static AnimationMetadata.ScalingInfo getProtectionOverlayScaling() {
        return getScalingOrLoad(Constants.loc("textures/gui/protection_overlay.png"), 
            AnimationMetadata.TextureType.OVERLAY_ANIMATED);
    }
    
    /**
     * Get texture dimensions for any texture (backgrounds, overlays, etc.)
     * Used to get correct texture sheet dimensions for non-animated textures.
     */
    public static AnimationMetadata.TextureDimensions getTextureDimensions(Identifier location) {
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

