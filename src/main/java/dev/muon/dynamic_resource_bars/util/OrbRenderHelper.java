package dev.muon.dynamic_resource_bars.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OrbRenderHelper {

    public static final float CHUNK_HOLD_ALPHA = 0.5f;
    public static final long CHUNK_DIM_DURATION = 120L;
    public static final long CHUNK_DRAIN_DELAY = 450L;
    public static final float CHUNK_DRAIN_ROWS_PER_MS = 30f / 1000f;
    public static final long CHUNK_MAX_LIFETIME = 30_000L;
    public static final int MAX_FADING_CHUNKS = 16;

    private static final float FILL_ROWS_PER_MS = 40f / 1000f;
    private static final long FILL_MAX_STEP_GAP = 250L;
    private static final float FLOW_MAX_AMPLITUDE = 1.5f;
    private static final float FLOW_REFERENCE_WIDTH = 23f;

    private static final Map<ResourceLocation, TextureDimensions> DIMENSIONS_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, AnimationData> ANIMATION_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Boolean> EXISTS_CACHE = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> PRE_REGISTERED = ConcurrentHashMap.newKeySet();

    public static final class TextureDimensions {
        public final int width;
        public final int height;

        public TextureDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public static final class AnimationData {
        public final int frametime;
        public final int frameHeight;
        public final int totalFrames;
        public final int textureWidth;
        public final int textureHeight;

        public AnimationData(int frametime, int frameHeight, int totalFrames, int textureWidth, int textureHeight) {
            this.frametime = frametime;
            this.frameHeight = frameHeight;
            this.totalFrames = totalFrames;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }
    }

    public static final class FillSmoother {
        private float displayedFillPx = -1f;
        private long lastUpdateTime = 0L;
        private int liveEdgePx = 0;

        public int step(int targetEdge) {
            long now = System.currentTimeMillis();
            long gap = now - lastUpdateTime;
            lastUpdateTime = now;
            if (displayedFillPx < 0f || targetEdge <= displayedFillPx || gap > FILL_MAX_STEP_GAP) {
                displayedFillPx = targetEdge;
            } else {
                displayedFillPx = Math.min(targetEdge, displayedFillPx + gap * FILL_ROWS_PER_MS);
            }
            liveEdgePx = (int) displayedFillPx;
            return liveEdgePx;
        }

        public int getLiveEdgePx() {
            return liveEdgePx;
        }

        public void reset() {
            displayedFillPx = -1f;
            liveEdgePx = 0;
        }
    }

    public static final class DrainingChunk {
        public float startValue;
        public final float endValue;
        public final float maxValue;
        public final long creationTime;
        public final String texture;
        public final int animOffset;
        public final boolean isMounted;
        public boolean drained;

        public DrainingChunk(float startValue, float endValue, float maxValue, String texture, int animOffset) {
            this(startValue, endValue, maxValue, texture, animOffset, false);
        }

        public DrainingChunk(float startValue, float endValue, float maxValue, String texture, int animOffset, boolean isMounted) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.maxValue = maxValue;
            this.creationTime = System.currentTimeMillis();
            this.texture = texture;
            this.animOffset = animOffset;
            this.isMounted = isMounted;
        }

        public float getAlpha() {
            long elapsed = System.currentTimeMillis() - creationTime;
            if (elapsed >= CHUNK_DIM_DURATION) return CHUNK_HOLD_ALPHA;
            return 1f - (1f - CHUNK_HOLD_ALPHA) * (elapsed / (float) CHUNK_DIM_DURATION);
        }

        public int drainedRows() {
            long drainElapsed = System.currentTimeMillis() - creationTime - CHUNK_DRAIN_DELAY;
            return drainElapsed <= 0 ? 0 : (int) (drainElapsed * CHUNK_DRAIN_ROWS_PER_MS);
        }

        public boolean isExpired() {
            return drained || (System.currentTimeMillis() - creationTime) > CHUNK_MAX_LIFETIME;
        }
    }

    public static void clearCache() {
        DIMENSIONS_CACHE.clear();
        ANIMATION_CACHE.clear();
        EXISTS_CACHE.clear();
    }

    public static boolean resourceExists(ResourceLocation loc) {
        return EXISTS_CACHE.computeIfAbsent(loc, key -> {
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            return rm.getResource(key).isPresent();
        });
    }

    public static TextureDimensions getTextureDimensions(ResourceLocation loc) {
        return DIMENSIONS_CACHE.computeIfAbsent(loc, OrbRenderHelper::loadTextureDimensions);
    }

    public static AnimationData getAnimationData(ResourceLocation loc) {
        return ANIMATION_CACHE.computeIfAbsent(loc, OrbRenderHelper::loadAnimationData);
    }

    private static TextureDimensions loadTextureDimensions(ResourceLocation loc) {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            Optional<Resource> resOpt = rm.getResource(loc);
            if (resOpt.isPresent()) {
                try (InputStream stream = resOpt.get().open();
                     NativeImage image = NativeImage.read(stream)) {
                    return new TextureDimensions(image.getWidth(), image.getHeight());
                }
            }
        } catch (Exception e) {
            DynamicResourceBars.LOGGER.warn("Failed to read texture dimensions for {}: {}", loc, e.getMessage());
        }
        return new TextureDimensions(256, 256);
    }

    private static AnimationData loadAnimationData(ResourceLocation loc) {
        TextureDimensions dims = getTextureDimensions(loc);
        int frametime = 3;
        int frameHeight = 32;
        boolean hasAnimSection = false;

        ResourceLocation mcmetaLoc = new ResourceLocation(loc.getNamespace(), loc.getPath() + ".mcmeta");
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            Optional<Resource> resOpt = rm.getResource(mcmetaLoc);
            if (resOpt.isPresent()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resOpt.get().open(), StandardCharsets.UTF_8))) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("animation")) {
                        hasAnimSection = true;
                        JsonObject anim = root.getAsJsonObject("animation");
                        if (anim.has("frametime")) {
                            frametime = Math.max(1, anim.get("frametime").getAsInt());
                        }
                        if (anim.has("height")) {
                            frameHeight = Math.max(1, anim.get("height").getAsInt());
                        } else if (anim.has("frameHeight")) {
                            frameHeight = Math.max(1, anim.get("frameHeight").getAsInt());
                        }
                    }
                }
            }
        } catch (Exception e) {
            DynamicResourceBars.LOGGER.warn("Failed to parse .mcmeta for {}: {}", loc, e.getMessage());
        }

        // Pre-register as SimpleTexture so GeckoLib does not replace the strip with a single-frame texture
        if (hasAnimSection && PRE_REGISTERED.add(loc)) {
            Minecraft.getInstance().getTextureManager().register(loc, new SimpleTexture(loc));
        }

        int totalFrames = Math.max(1, dims.height / frameHeight);
        return new AnimationData(frametime, frameHeight, totalFrames, dims.width, dims.height);
    }

    public static int calculateAnimationOffset(AnimationData data, float ticks) {
        int frame = Math.floorMod((int) (ticks / Math.max(1, data.frametime)), Math.max(1, data.totalFrames));
        return frame * data.frameHeight;
    }

    /** Draws a full non-animated texture stretched to the target rect using its real PNG dimensions. */
    public static void blitFullTexture(GuiGraphics graphics, ResourceLocation loc, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        TextureDimensions dims = getTextureDimensions(loc);
        graphics.blit(loc, x, y, width, height, 0.0f, 0.0f, dims.width, dims.height, dims.width, dims.height);
    }

    /**
     * Paints a strip of a bar texture between fromPx and toPx from the leading edge.
     * In vertical mode, applies the animated liquid surface wave per 1px column.
     */
    public static void paintFillSpan(GuiGraphics graphics, ResourceLocation texture, ScreenRect barRect,
                                     boolean vertical, boolean rightAnchored, int animOffset,
                                     AnimationData animData, int fromPx, int toPx,
                                     boolean flowingFrom, boolean flowingTo, float currentTicks) {
        int span = vertical ? barRect.height() : barRect.width();
        if (!vertical || (!flowingFrom && !flowingTo)) {
            blitSpanRun(graphics, texture, barRect, vertical, rightAnchored, animOffset,
                    animData.textureWidth, animData.textureHeight,
                    0, vertical ? barRect.width() : barRect.height(),
                    Math.max(0, fromPx), Math.min(span, toPx));
            return;
        }

        int lanes = barRect.width();
        int runStart = 0;
        int runLo = 0;
        int runHi = 0;
        for (int lane = 0; lane <= lanes; lane++) {
            int lo = 0;
            int hi = 0;
            if (lane < lanes) {
                lo = Math.max(0, fromPx + (flowingFrom ? flowOffset(lane, lanes, fromPx, span, currentTicks) : 0));
                hi = Math.min(span, toPx + (flowingTo ? flowOffset(lane, lanes, toPx, span, currentTicks) : 0));
            }
            if (lane > 0 && (lane == lanes || lo != runLo || hi != runHi)) {
                blitSpanRun(graphics, texture, barRect, true, rightAnchored, animOffset,
                        animData.textureWidth, animData.textureHeight,
                        runStart, lane - runStart, runLo, runHi);
                runStart = lane;
            }
            runLo = lo;
            runHi = hi;
        }
    }

    private static void blitSpanRun(GuiGraphics graphics, ResourceLocation texture, ScreenRect barRect,
                                    boolean vertical, boolean rightAnchored, int animOffset,
                                    int textureWidth, int textureHeight,
                                    int laneStart, int laneCount, int lo, int hi) {
        int length = hi - lo;
        if (length <= 0 || laneCount <= 0) return;
        if (vertical) {
            int fromTop = barRect.height() - hi;
            graphics.blit(texture,
                    barRect.x() + laneStart, barRect.y() + fromTop,
                    (float) laneStart, (float) (animOffset + fromTop),
                    laneCount, length, textureWidth, textureHeight);
            return;
        }
        int u = rightAnchored ? barRect.width() - hi : lo;
        graphics.blit(texture,
                barRect.x() + u, barRect.y() + laneStart,
                (float) u, (float) (animOffset + laneStart),
                length, laneCount, textureWidth, textureHeight);
    }

    private static int flowOffset(int lane, int lanes, int edgePx, int span, float currentTicks) {
        float amplitude = Math.min(FLOW_MAX_AMPLITUDE, Math.min(edgePx, span - edgePx));
        if (amplitude <= 0f) return 0;
        float x = lane * (FLOW_REFERENCE_WIDTH / (float) Math.max(1, lanes));
        float wave = 0.6f * (float) Math.sin(x * 0.35f + currentTicks * 0.22f)
                + 0.4f * (float) Math.sin(x * 0.83f - currentTicks * 0.31f);
        return Math.round(amplitude * wave);
    }
}