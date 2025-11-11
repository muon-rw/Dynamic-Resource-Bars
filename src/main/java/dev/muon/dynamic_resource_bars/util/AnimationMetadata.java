package dev.muon.dynamic_resource_bars.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Utility for loading and parsing .mcmeta files with extended features:
 * - Animation properties
 * - Bar dimensions (auto-sizing)
 * - Mask layers (shaped bars)
 * - UV mapping (texture atlases)
 * - Nine-slice scaling (borders)
 */
public class AnimationMetadata {
    
    public static class AnimationData {
        public final int frametime;
        public final int frameHeight;
        public final int totalFrames;
        public final boolean interpolate;
        public final int textureWidth;
        public final int textureHeight;
        
        public AnimationData(int frametime, int frameHeight, int totalFrames, boolean interpolate, 
                            int textureWidth, int textureHeight) {
            this.frametime = frametime;
            this.frameHeight = frameHeight;
            this.totalFrames = totalFrames;
            this.interpolate = interpolate;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }
    }
    
    public static class BarDimensions {
        public final int width;
        public final int height;
        public final boolean fromMcmeta;
        
        public BarDimensions(int width, int height, boolean fromMcmeta) {
            this.width = width;
            this.height = height;
            this.fromMcmeta = fromMcmeta;
        }
    }
    
    public static class MaskInfo {
        public final String maskTexturePath;
        public final boolean enabled;
        
        public MaskInfo(String maskTexturePath, boolean enabled) {
            this.maskTexturePath = maskTexturePath;
            this.enabled = enabled;
        }
        
        public static MaskInfo disabled() {
            return new MaskInfo(null, false);
        }
    }
    
    /**
     * Scaling modes for textures
     */
    public enum ScalingMode {
        NONE,       // No scaling - 1:1 UV sampling (sample exactly what fits)
        STRETCH,    // Simple stretch/scale
        TILE,       // Simple tile/repeat
        NINE_SLICE  // 9-region scaling with configurable modes
    }
    
    /**
     * Scaling information for a texture.
     * Supports simple stretch/tile or advanced nine-slice with per-region modes.
     */
    public static class ScalingInfo {
        public final ScalingMode mode;
        public final NineSliceData nineSlice; // null if not nine-slice mode
        public final SourceRegion sourceRegion; // null if using full texture
        
        private ScalingInfo(ScalingMode mode, NineSliceData nineSlice, SourceRegion sourceRegion) {
            this.mode = mode;
            this.nineSlice = nineSlice;
            this.sourceRegion = sourceRegion;
        }
        
        public static ScalingInfo none() {
            return new ScalingInfo(ScalingMode.NONE, null, null);
        }
        
        public static ScalingInfo none(SourceRegion sourceRegion) {
            return new ScalingInfo(ScalingMode.NONE, null, sourceRegion);
        }
        
        public static ScalingInfo stretch() {
            return new ScalingInfo(ScalingMode.STRETCH, null, null);
        }
        
        public static ScalingInfo stretch(SourceRegion sourceRegion) {
            return new ScalingInfo(ScalingMode.STRETCH, null, sourceRegion);
        }
        
        public static ScalingInfo tile() {
            return new ScalingInfo(ScalingMode.TILE, null, null);
        }
        
        public static ScalingInfo tile(SourceRegion sourceRegion) {
            return new ScalingInfo(ScalingMode.TILE, null, sourceRegion);
        }
        
        public static ScalingInfo nineSlice(int left, int right, int top, int bottom, 
                                           ScalingMode edgeMode, ScalingMode centerMode) {
            return new ScalingInfo(ScalingMode.NINE_SLICE, 
                new NineSliceData(left, right, top, bottom, edgeMode, centerMode, null), null);
        }
        
        public static ScalingInfo nineSlice(int left, int right, int top, int bottom, 
                                           ScalingMode edgeMode, ScalingMode centerMode,
                                           SourceRegion sourceRegion) {
            return new ScalingInfo(ScalingMode.NINE_SLICE, 
                new NineSliceData(left, right, top, bottom, edgeMode, centerMode, sourceRegion), sourceRegion);
        }
        
        public boolean isNineSlice() {
            return mode == ScalingMode.NINE_SLICE && nineSlice != null;
        }
    }
    
    /**
     * Nine-slice border sizes and scaling modes
     */
    public static class NineSliceData {
        public final int left;
        public final int right;
        public final int top;
        public final int bottom;
        public final ScalingMode edgeMode;   // How edges scale (tile or stretch)
        public final ScalingMode centerMode; // How center scales (tile or stretch)
        public final SourceRegion sourceRegion; // Which part of texture to use (null = entire texture)
        
        public NineSliceData(int left, int right, int top, int bottom, 
                            ScalingMode edgeMode, ScalingMode centerMode,
                            SourceRegion sourceRegion) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
            this.edgeMode = edgeMode;
            this.centerMode = centerMode;
            this.sourceRegion = sourceRegion;
        }
    }
    
    /**
     * Source region within a texture sheet.
     * Allows nine-slicing a specific area (e.g., 80x10 in top-left of 256x256 sheet).
     */
    public static class SourceRegion {
        public final int u;      // X offset in texture
        public final int v;      // Y offset in texture
        public final int width;  // Width of region to use
        public final int height; // Height of region to use
        
        public SourceRegion(int u, int v, int width, int height) {
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
        }
        
        public static SourceRegion fullTexture(int width, int height) {
            return new SourceRegion(0, 0, width, height);
        }
    }
    
    /**
     * Load animation metadata for a texture. Uses hardcoded defaults if .mcmeta not found.
     * 
     * @param resourceManager The resource manager to load from
     * @param textureLocation The base texture location (without .mcmeta extension)
     * @return AnimationData with either loaded or hardcoded default values
     */
    public static AnimationData loadAnimationData(ResourceManager resourceManager, 
                                                   ResourceLocation textureLocation) {
        // Create .mcmeta location
        ResourceLocation mcmetaLocation;
        #if NEWER_THAN_20_1
        mcmetaLocation = ResourceLocation.fromNamespaceAndPath(
            textureLocation.getNamespace(),
            textureLocation.getPath() + ".mcmeta"
        );
        #else
        mcmetaLocation = new ResourceLocation(
            textureLocation.getNamespace(),
            textureLocation.getPath() + ".mcmeta"
        );
        #endif
        
        // Hardcoded defaults if .mcmeta is missing or invalid
        final int DEFAULT_FRAMETIME = 3;
        final int DEFAULT_FRAME_HEIGHT = 32;
        final int DEFAULT_TOTAL_FRAMES = 32;
        final boolean DEFAULT_INTERPOLATE = false;
        
        // Load actual texture dimensions from the PNG file
        TextureDimensions textureDims = loadTextureDimensions(resourceManager, textureLocation);
        
        try {
            Optional<Resource> resourceOpt = resourceManager.getResource(mcmetaLocation);
            if (resourceOpt.isEmpty()) {
                // No .mcmeta file found, use hardcoded defaults
                DynamicResourceBars.LOGGER.warn("No .mcmeta found for {}, using hardcoded defaults (frametime=3, height=32)", textureLocation);
                return new AnimationData(DEFAULT_FRAMETIME, DEFAULT_FRAME_HEIGHT, DEFAULT_TOTAL_FRAMES, DEFAULT_INTERPOLATE,
                    textureDims.width, textureDims.height);
            }
            
            Resource resource = resourceOpt.get();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                
                if (!root.has("animation")) {
                    DynamicResourceBars.LOGGER.error("INVALID .mcmeta for {}: Missing 'animation' section! Using defaults.", textureLocation);
                    return new AnimationData(DEFAULT_FRAMETIME, DEFAULT_FRAME_HEIGHT, DEFAULT_TOTAL_FRAMES, DEFAULT_INTERPOLATE,
                        textureDims.width, textureDims.height);
                }
                
                JsonObject animation = root.getAsJsonObject("animation");
                
                // Parse animation properties
                int frametime = animation.has("frametime") ? animation.get("frametime").getAsInt() : DEFAULT_FRAMETIME;
                boolean interpolate = animation.has("interpolate") && animation.get("interpolate").getAsBoolean();
                
                // Get frame height - required field
                int frameHeight;
                if (animation.has("height")) {
                    frameHeight = animation.get("height").getAsInt();
                } else if (animation.has("frameHeight")) {
                    frameHeight = animation.get("frameHeight").getAsInt();
                } else {
                    DynamicResourceBars.LOGGER.error("INVALID .mcmeta for {}: Missing 'height' field! Using default (32px).", textureLocation);
                    frameHeight = DEFAULT_FRAME_HEIGHT;
                }
                
                // Calculate total frames from actual texture height
                int totalFrames = textureDims.height / frameHeight;
                
                DynamicResourceBars.LOGGER.info("Loaded animation data for {}: frametime={}, frameHeight={}, totalFrames={}, textureSize={}x{}", 
                    textureLocation, frametime, frameHeight, totalFrames, textureDims.width, textureDims.height);
                
                return new AnimationData(frametime, frameHeight, totalFrames, interpolate,
                    textureDims.width, textureDims.height);
                
            } catch (Exception e) {
                DynamicResourceBars.LOGGER.error("ERROR parsing .mcmeta for {}: {}. Using hardcoded defaults.", textureLocation, e.getMessage());
                return new AnimationData(DEFAULT_FRAMETIME, DEFAULT_FRAME_HEIGHT, DEFAULT_TOTAL_FRAMES, DEFAULT_INTERPOLATE,
                    textureDims.width, textureDims.height);
            }
            
        } catch (Exception e) {
            DynamicResourceBars.LOGGER.error("ERROR loading .mcmeta for {}: {}. Using hardcoded defaults.", textureLocation, e.getMessage());
            return new AnimationData(DEFAULT_FRAMETIME, DEFAULT_FRAME_HEIGHT, DEFAULT_TOTAL_FRAMES, DEFAULT_INTERPOLATE,
                textureDims.width, textureDims.height);
        }
    }
    
    /**
     * Helper class to hold texture dimensions
     */
    public static class TextureDimensions {
        public final int width;
        public final int height;
        
        public TextureDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
    
    /**
     * Load actual texture dimensions from PNG file.
     * This allows resource packs to use any texture size!
     */
    private static TextureDimensions loadTextureDimensions(ResourceManager resourceManager, ResourceLocation textureLocation) {
        // Defaults if we can't load the texture
        final int DEFAULT_WIDTH = 256;
        final int DEFAULT_HEIGHT = 1024;
        
        try {
            Optional<Resource> resourceOpt = resourceManager.getResource(textureLocation);
            if (resourceOpt.isEmpty()) {
                DynamicResourceBars.LOGGER.warn("Texture not found: {}. Using default dimensions {}x{}", 
                    textureLocation, DEFAULT_WIDTH, DEFAULT_HEIGHT);
                return new TextureDimensions(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            }
            
            try (InputStream stream = resourceOpt.get().open()) {
                NativeImage image = NativeImage.read(stream);
                int width = image.getWidth();
                int height = image.getHeight();
                image.close();
                
                DynamicResourceBars.LOGGER.debug("Detected texture dimensions for {}: {}x{}", textureLocation, width, height);
                return new TextureDimensions(width, height);
            }
        } catch (Exception e) {
            DynamicResourceBars.LOGGER.warn("Could not load texture dimensions for {}: {}. Using defaults {}x{}", 
                textureLocation, e.getMessage(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
            return new TextureDimensions(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }
    }
    
    /**
     * Calculate the animation offset (V coordinate in texture) for the current frame
     * 
     * @param data The animation data
     * @param ticks Current game ticks (with partial ticks added)
     * @return The V offset in pixels for the current animation frame
     */
    public static int calculateAnimationOffset(AnimationData data, float ticks) {
        // Calculate which frame we're on based on frametime
        int currentFrame = (int) ((ticks / data.frametime) % data.totalFrames);
        return currentFrame * data.frameHeight;
    }
    
    /**
     * Load bar dimensions from .mcmeta file. Returns null if not specified.
     * Resource packs can specify preferred render dimensions for their bars.
     * 
     * @param resourceManager The resource manager
     * @param textureLocation The texture location
     * @return BarDimensions if specified in .mcmeta, null otherwise
     */
    public static BarDimensions loadBarDimensions(ResourceManager resourceManager, ResourceLocation textureLocation) {
        ResourceLocation mcmetaLocation;
        #if NEWER_THAN_20_1
        mcmetaLocation = ResourceLocation.fromNamespaceAndPath(
            textureLocation.getNamespace(),
            textureLocation.getPath() + ".mcmeta"
        );
        #else
        mcmetaLocation = new ResourceLocation(
            textureLocation.getNamespace(),
            textureLocation.getPath() + ".mcmeta"
        );
        #endif
        
        try {
            Optional<Resource> resourceOpt = resourceManager.getResource(mcmetaLocation);
            if (resourceOpt.isEmpty()) {
                return null; // No .mcmeta, use config values
            }
            
            Resource resource = resourceOpt.get();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                
                if (!root.has("dynamic_resource_bars")) {
                    return null; // No custom section
                }
                
                JsonObject custom = root.getAsJsonObject("dynamic_resource_bars");
                if (!custom.has("bar")) {
                    return null; // No bar dimensions specified
                }
                
                JsonObject bar = custom.getAsJsonObject("bar");
                if (!bar.has("width") || !bar.has("height")) {
                    DynamicResourceBars.LOGGER.error("INVALID bar dimensions in .mcmeta for {}: missing width or height", textureLocation);
                    return null;
                }
                
                int width = bar.get("width").getAsInt();
                int height = bar.get("height").getAsInt();
                
                DynamicResourceBars.LOGGER.info("Loaded bar dimensions from .mcmeta for {}: {}x{}", textureLocation, width, height);
                return new BarDimensions(width, height, true);
                
            } catch (Exception e) {
                DynamicResourceBars.LOGGER.error("ERROR parsing bar dimensions for {}: {}", textureLocation, e.getMessage());
                return null;
            }
        } catch (Exception e) {
            return null; // No .mcmeta
        }
    }
    
    /**
     * Load mask info from .mcmeta file with dimension validation.
     * Returns disabled mask if not specified.
     * 
     * @param resourceManager The resource manager
     * @param textureLocation The texture location
     * @return MaskInfo from .mcmeta, or disabled mask
     */
    public static MaskInfo loadMaskInfo(ResourceManager resourceManager, ResourceLocation textureLocation) {
        ResourceLocation mcmetaLocation;
        #if NEWER_THAN_20_1
        mcmetaLocation = ResourceLocation.fromNamespaceAndPath(
            textureLocation.getNamespace(),
            textureLocation.getPath() + ".mcmeta"
        );
        #else
        mcmetaLocation = new ResourceLocation(
            textureLocation.getNamespace(),
            textureLocation.getPath() + ".mcmeta"
        );
        #endif
        
        try {
            Optional<Resource> resourceOpt = resourceManager.getResource(mcmetaLocation);
            if (resourceOpt.isEmpty()) {
                return MaskInfo.disabled();
            }
            
            Resource resource = resourceOpt.get();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                
                if (!root.has("dynamic_resource_bars")) {
                    return MaskInfo.disabled();
                }
                
                JsonObject custom = root.getAsJsonObject("dynamic_resource_bars");
                if (!custom.has("mask")) {
                    return MaskInfo.disabled();
                }
                
                JsonObject maskObj = custom.getAsJsonObject("mask");
                if (!maskObj.has("texture")) {
                    DynamicResourceBars.LOGGER.error("INVALID mask in .mcmeta for {}: missing texture field", textureLocation);
                    return MaskInfo.disabled();
                }
                
                String maskTexture = maskObj.get("texture").getAsString();
                boolean enabled = !maskObj.has("enabled") || maskObj.get("enabled").getAsBoolean();
                
                // Validate mask dimensions against animation frame size
                if (enabled) {
                    validateMaskDimensions(resourceManager, textureLocation, maskTexture);
                }
                
                DynamicResourceBars.LOGGER.info("Loaded mask for {}: texture={}, enabled={}", textureLocation, maskTexture, enabled);
                return new MaskInfo(maskTexture, enabled);
                
            } catch (Exception e) {
                DynamicResourceBars.LOGGER.error("ERROR parsing mask info for {}: {}", textureLocation, e.getMessage());
                return MaskInfo.disabled();
            }
        } catch (Exception e) {
            return MaskInfo.disabled();
        }
    }
    
    /**
     * Validate that mask dimensions match the animation frame size.
     * Logs a warning if dimensions don't match.
     */
    private static void validateMaskDimensions(ResourceManager resourceManager, ResourceLocation barTextureLocation, String maskTexturePath) {
        try {
            // Parse mask texture location
            ResourceLocation maskLocation;
            String[] parts = maskTexturePath.split(":");
            if (parts.length == 2) {
                #if NEWER_THAN_20_1
                maskLocation = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
                #else
                maskLocation = new ResourceLocation(parts[0], parts[1]);
                #endif
            } else {
                maskLocation = DynamicResourceBars.loc(maskTexturePath);
            }
            
            // Load animation data to get frame dimensions (this auto-detects texture size)
            AnimationData animData = loadAnimationData(resourceManager, barTextureLocation);
            int expectedWidth = animData.textureWidth;
            int expectedHeight = animData.frameHeight;
            
            // Load mask texture to check dimensions
            Optional<Resource> maskResourceOpt = resourceManager.getResource(maskLocation);
            if (maskResourceOpt.isEmpty()) {
                DynamicResourceBars.LOGGER.warn("MASK VALIDATION: Mask texture not found: {}", maskTexturePath);
                return;
            }
            
            try (InputStream stream = maskResourceOpt.get().open()) {
                NativeImage image = NativeImage.read(stream);
                int maskWidth = image.getWidth();
                int maskHeight = image.getHeight();
                image.close();
                
                if (maskWidth != expectedWidth || maskHeight != expectedHeight) {
                    DynamicResourceBars.LOGGER.warn(
                        "MASK DIMENSION MISMATCH for {}: Mask is {}x{} but animation frame is {}x{}. " +
                        "Mask should match the size of a single animation frame for best results.",
                        barTextureLocation, maskWidth, maskHeight, expectedWidth, expectedHeight
                    );
                } else {
                    DynamicResourceBars.LOGGER.debug("Mask dimensions validated for {}: {}x{}", barTextureLocation, maskWidth, maskHeight);
                }
            }
        } catch (Exception e) {
            DynamicResourceBars.LOGGER.debug("Could not validate mask dimensions for {}: {}", barTextureLocation, e.getMessage());
        }
    }
    
    /**
     * Load scaling info from .mcmeta file with smart defaults based on texture type.
     * 
     * @param resourceManager The resource manager
     * @param textureLocation The texture location
     * @param textureType The type of texture (for default selection)
     * @return ScalingInfo from .mcmeta or appropriate default
     */
    public static ScalingInfo loadScalingInfo(ResourceManager resourceManager, ResourceLocation textureLocation, TextureType textureType) {
        ResourceLocation mcmetaLocation;
        #if NEWER_THAN_20_1
        mcmetaLocation = ResourceLocation.fromNamespaceAndPath(
            textureLocation.getNamespace(),
            textureLocation.getPath() + ".mcmeta"
        );
        #else
        mcmetaLocation = new ResourceLocation(
            textureLocation.getNamespace(),
            textureLocation.getPath() + ".mcmeta"
        );
        #endif
        
        try {
            Optional<Resource> resourceOpt = resourceManager.getResource(mcmetaLocation);
            if (resourceOpt.isEmpty()) {
                return getDefaultScaling(textureType);
            }
            
            Resource resource = resourceOpt.get();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                
                if (!root.has("dynamic_resource_bars")) {
                    return getDefaultScaling(textureType);
                }
                
                JsonObject custom = root.getAsJsonObject("dynamic_resource_bars");
                if (!custom.has("scaling")) {
                    return getDefaultScaling(textureType);
                }
                
                // Parse scaling - can be string or object
                if (custom.get("scaling").isJsonPrimitive()) {
                    String scalingStr = custom.get("scaling").getAsString().toLowerCase();
                    
                    // Check if there's a source region at root level (for simple modes)
                    SourceRegion sourceRegion = null;
                    if (custom.has("source")) {
                        sourceRegion = parseSourceRegion(custom.getAsJsonObject("source"), textureLocation);
                    }
                    
                    switch (scalingStr) {
                        case "none":
                            return sourceRegion != null ? ScalingInfo.none(sourceRegion) : ScalingInfo.none();
                        case "tile":
                            return sourceRegion != null ? ScalingInfo.tile(sourceRegion) : ScalingInfo.tile();
                        case "stretch":
                            return sourceRegion != null ? ScalingInfo.stretch(sourceRegion) : ScalingInfo.stretch();
                        default:
                            DynamicResourceBars.LOGGER.warn("Unknown scaling mode '{}' for {}. Using default.", scalingStr, textureLocation);
                            return getDefaultScaling(textureType);
                    }
                } else if (custom.get("scaling").isJsonObject()) {
                    // Nine-slice mode
                    JsonObject scaling = custom.getAsJsonObject("scaling");
                    
                    if (!scaling.has("type") || !scaling.get("type").getAsString().equals("nine_slice")) {
                        DynamicResourceBars.LOGGER.error("INVALID scaling for {}: nine-slice object must have type='nine_slice'", textureLocation);
                        return getDefaultScaling(textureType);
                    }
                    
                    if (!scaling.has("left") || !scaling.has("right") || !scaling.has("top") || !scaling.has("bottom")) {
                        DynamicResourceBars.LOGGER.error("INVALID nine-slice for {}: missing border dimensions", textureLocation);
                        return getDefaultScaling(textureType);
                    }
                    
                    int left = scaling.get("left").getAsInt();
                    int right = scaling.get("right").getAsInt();
                    int top = scaling.get("top").getAsInt();
                    int bottom = scaling.get("bottom").getAsInt();
                    
                    // Parse edge and center modes (optional, defaults to stretch)
                    ScalingMode edgeMode = ScalingMode.STRETCH;
                    ScalingMode centerMode = ScalingMode.STRETCH;
                    
                    if (scaling.has("edges")) {
                        String edgeStr = scaling.get("edges").getAsString().toLowerCase();
                        edgeMode = edgeStr.equals("tile") ? ScalingMode.TILE : ScalingMode.STRETCH;
                    }
                    
                    if (scaling.has("center")) {
                        String centerStr = scaling.get("center").getAsString().toLowerCase();
                        centerMode = centerStr.equals("tile") ? ScalingMode.TILE : ScalingMode.STRETCH;
                    }
                    
                    // Parse optional source region (for textures that don't fill entire sheet)
                    SourceRegion sourceRegion = null;
                    if (scaling.has("source")) {
                        sourceRegion = parseSourceRegion(scaling.getAsJsonObject("source"), textureLocation);
                    }
                    
                    DynamicResourceBars.LOGGER.info("Loaded nine-slice scaling for {}: borders=({},{},{},{}), edges={}, center={}", 
                        textureLocation, left, right, top, bottom, edgeMode, centerMode);
                    
                    return ScalingInfo.nineSlice(left, right, top, bottom, edgeMode, centerMode, sourceRegion);
                }
                
                DynamicResourceBars.LOGGER.warn("Invalid scaling format for {}. Using default.", textureLocation);
                return getDefaultScaling(textureType);
                
            } catch (Exception e) {
                DynamicResourceBars.LOGGER.error("ERROR parsing scaling info for {}: {}", textureLocation, e.getMessage());
                return getDefaultScaling(textureType);
            }
        } catch (Exception e) {
            return getDefaultScaling(textureType);
        }
    }
    
    /**
     * Texture types for determining default scaling behavior.
     * Note: BAR type exists but should not use nine-slice (uses UV sampling instead).
     */
    public enum TextureType {
        BAR,                    // Animated bars - do NOT scale, use UV sampling
        BACKGROUND,             // Background layers
        FOREGROUND,             // Foreground layers
        OVERLAY_STATIC,         // Static overlays (hardcore, wetness)
        OVERLAY_ANIMATED        // Animated/temp overlays (heat, cold, absorption, regen)
    }
    
    /**
     * Get smart default scaling based on texture type.
     * 
     * Defaults:
     * - Bars: NOT APPLICABLE (bars use UV sampling, not scaling)
     * - Backgrounds/Foregrounds: nine-slice with stretch (both)
     * - Static overlays: simple stretch
     * - Animated overlays: nine-slice with tile (both)
     * 
     * Note: Scaling info should not be requested for BAR type textures.
     */
    private static ScalingInfo getDefaultScaling(TextureType type) {
        switch (type) {
            case BAR:
                // Bars should NOT use scaling - they use UV sampling to show fill percentage
                // If scaling is requested for a bar (shouldn't happen), log warning and return stretch
                DynamicResourceBars.LOGGER.warn("Scaling requested for BAR texture - bars should use UV sampling, not scaling!");
                return ScalingInfo.stretch();
                
            case BACKGROUND:
            case FOREGROUND:
                // Backgrounds/foregrounds stretch to fit smoothly with nine-slice
                return ScalingInfo.nineSlice(4, 4, 3, 3, ScalingMode.STRETCH, ScalingMode.STRETCH);
                
            case OVERLAY_STATIC:
                // Static overlays just stretch (simple is fine)
                return ScalingInfo.stretch();
                
            case OVERLAY_ANIMATED:
                // Animated overlays (heat, cold, etc.) tile to preserve patterns
                return ScalingInfo.nineSlice(3, 3, 2, 2, ScalingMode.TILE, ScalingMode.TILE);
                
            default:
                return ScalingInfo.stretch();
        }
    }
    
    /**
     * Parse source region from JSON
     */
    private static SourceRegion parseSourceRegion(JsonObject source, ResourceLocation textureLocation) {
        if (source.has("width") && source.has("height")) {
            int u = source.has("u") ? source.get("u").getAsInt() : 0;
            int v = source.has("v") ? source.get("v").getAsInt() : 0;
            int width = source.get("width").getAsInt();
            int height = source.get("height").getAsInt();
            DynamicResourceBars.LOGGER.info("Loaded source region for {}: u={}, v={}, {}x{}", 
                textureLocation, u, v, width, height);
            return new SourceRegion(u, v, width, height);
        }
        return null;
    }
}

