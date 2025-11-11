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
 * Comprehensive texture metadata loader that handles:
 * - Texture dimensions (auto-detected from PNG)
 * - Animation properties (from .mcmeta)
 * - UV mapping (from .mcmeta custom section)
 * - Nine-slice scaling (from .mcmeta custom section)
 */
public class TextureMetadata {
    
    public static class AnimationInfo {
        public final int frametime;
        public final int frameHeight;
        public final int totalFrames;
        public final boolean interpolate;
        
        public AnimationInfo(int frametime, int frameHeight, int totalFrames, boolean interpolate) {
            this.frametime = frametime;
            this.frameHeight = frameHeight;
            this.totalFrames = totalFrames;
            this.interpolate = interpolate;
        }
    }
    
    public static class UVMapping {
        public final int uOffset;
        public final int vOffset;
        public final int width;
        public final int height;
        
        public UVMapping(int uOffset, int vOffset, int width, int height) {
            this.uOffset = uOffset;
            this.vOffset = vOffset;
            this.width = width;
            this.height = height;
        }
        
        public static UVMapping createDefault(int width, int height) {
            return new UVMapping(0, 0, width, height);
        }
    }
    
    public static class NineSliceInfo {
        public final int left;
        public final int right;
        public final int top;
        public final int bottom;
        
        public NineSliceInfo(int left, int right, int top, int bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
        
        public boolean isEnabled() {
            return left > 0 || right > 0 || top > 0 || bottom > 0;
        }
    }
    
    public static class FullMetadata {
        public final int textureWidth;
        public final int textureHeight;
        public final AnimationInfo animation;
        public final UVMapping uvMapping;
        public final NineSliceInfo nineSlice;
        
        public FullMetadata(int textureWidth, int textureHeight, AnimationInfo animation, 
                           UVMapping uvMapping, NineSliceInfo nineSlice) {
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.animation = animation;
            this.uvMapping = uvMapping;
            this.nineSlice = nineSlice;
        }
    }
    
    /**
     * Load full texture metadata including dimensions, animation, UV, and nine-slice.
     * 
     * @param resourceManager The resource manager
     * @param textureLocation The texture location (without .mcmeta)
     * @return Complete metadata for the texture
     */
    public static FullMetadata loadFullMetadata(ResourceManager resourceManager, ResourceLocation textureLocation) {
        // 1. Load texture dimensions from PNG
        int textureWidth = 256;  // Default
        int textureHeight = 1024; // Default for animated bars
        
        try {
            Optional<Resource> texResourceOpt = resourceManager.getResource(textureLocation);
            if (texResourceOpt.isPresent()) {
                try (InputStream stream = texResourceOpt.get().open()) {
                    NativeImage image = NativeImage.read(stream);
                    textureWidth = image.getWidth();
                    textureHeight = image.getHeight();
                    image.close();
                    DynamicResourceBars.LOGGER.info("Detected texture size for {}: {}x{}", textureLocation, textureWidth, textureHeight);
                }
            }
        } catch (Exception e) {
            DynamicResourceBars.LOGGER.warn("Could not read texture dimensions for {}, using defaults", textureLocation);
        }
        
        // 2. Load .mcmeta file
        AnimationInfo animation = null;
        UVMapping uvMapping = null;
        NineSliceInfo nineSlice = null;
        
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
            if (resourceOpt.isPresent()) {
                Resource resource = resourceOpt.get();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                    
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    
                    // Parse animation section
                    animation = parseAnimation(root, textureLocation, textureHeight);
                    
                    // Parse custom section for our extensions
                    if (root.has("dynamic_resource_bars")) {
                        JsonObject custom = root.getAsJsonObject("dynamic_resource_bars");
                        
                        // Parse UV mapping
                        if (custom.has("uv")) {
                            uvMapping = parseUVMapping(custom.getAsJsonObject("uv"), textureWidth, textureHeight);
                        }
                        
                        // Parse nine-slice
                        if (custom.has("nine_slice")) {
                            nineSlice = parseNineSlice(custom.getAsJsonObject("nine_slice"));
                        }
                    }
                    
                } catch (Exception e) {
                    DynamicResourceBars.LOGGER.error("ERROR parsing .mcmeta for {}: {}", textureLocation, e.getMessage());
                }
            }
        } catch (Exception e) {
            DynamicResourceBars.LOGGER.info("No .mcmeta for {} or could not load", textureLocation);
        }
        
        // Apply defaults for missing sections
        if (animation == null) {
            // Default animation: 32 frames, 32px per frame, 3 ticks frametime
            int frameHeight = 32;
            int totalFrames = textureHeight / frameHeight;
            animation = new AnimationInfo(3, frameHeight, totalFrames, false);
            DynamicResourceBars.LOGGER.warn("No animation data in .mcmeta for {}, using defaults (frametime=3, height=32)", textureLocation);
        }
        
        if (uvMapping == null) {
            // Default UV: use frame dimensions from animation
            uvMapping = UVMapping.createDefault(textureWidth, animation.frameHeight);
        }
        
        if (nineSlice == null) {
            // No nine-slice by default
            nineSlice = new NineSliceInfo(0, 0, 0, 0);
        }
        
        return new FullMetadata(textureWidth, textureHeight, animation, uvMapping, nineSlice);
    }
    
    private static AnimationInfo parseAnimation(JsonObject root, ResourceLocation location, int textureHeight) {
        if (!root.has("animation")) {
            return null; // Will use defaults
        }
        
        JsonObject animation = root.getAsJsonObject("animation");
        
        int frametime = animation.has("frametime") ? animation.get("frametime").getAsInt() : 3;
        boolean interpolate = animation.has("interpolate") && animation.get("interpolate").getAsBoolean();
        
        int frameHeight;
        if (animation.has("height")) {
            frameHeight = animation.get("height").getAsInt();
        } else if (animation.has("frameHeight")) {
            frameHeight = animation.get("frameHeight").getAsInt();
        } else {
            DynamicResourceBars.LOGGER.error("INVALID .mcmeta for {}: Missing 'height' field in animation! Using 32px.", location);
            frameHeight = 32;
        }
        
        int totalFrames = textureHeight / frameHeight;
        
        DynamicResourceBars.LOGGER.info("Loaded animation for {}: frametime={}, frameHeight={}, totalFrames={}", 
            location, frametime, frameHeight, totalFrames);
        
        return new AnimationInfo(frametime, frameHeight, totalFrames, interpolate);
    }
    
    private static UVMapping parseUVMapping(JsonObject uv, int textureWidth, int defaultHeight) {
        int u = uv.has("u") ? uv.get("u").getAsInt() : 0;
        int v = uv.has("v") ? uv.get("v").getAsInt() : 0;
        int width = uv.has("width") ? uv.get("width").getAsInt() : textureWidth;
        int height = uv.has("height") ? uv.get("height").getAsInt() : defaultHeight;
        
        DynamicResourceBars.LOGGER.info("Loaded UV mapping: u={}, v={}, width={}, height={}", u, v, width, height);
        return new UVMapping(u, v, width, height);
    }
    
    private static NineSliceInfo parseNineSlice(JsonObject nineSlice) {
        int left = nineSlice.has("left") ? nineSlice.get("left").getAsInt() : 0;
        int right = nineSlice.has("right") ? nineSlice.get("right").getAsInt() : 0;
        int top = nineSlice.has("top") ? nineSlice.get("top").getAsInt() : 0;
        int bottom = nineSlice.has("bottom") ? nineSlice.get("bottom").getAsInt() : 0;
        
        DynamicResourceBars.LOGGER.info("Loaded nine-slice: left={}, right={}, top={}, bottom={}", left, right, top, bottom);
        return new NineSliceInfo(left, right, top, bottom);
    }
}

