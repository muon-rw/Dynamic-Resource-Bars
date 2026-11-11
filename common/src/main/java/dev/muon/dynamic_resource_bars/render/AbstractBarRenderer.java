package dev.muon.dynamic_resource_bars.render;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.AnimationMetadata;
import dev.muon.dynamic_resource_bars.util.AnimationMetadataCache;
import dev.muon.dynamic_resource_bars.util.BarVisibility;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.EditModeManager;
import dev.muon.dynamic_resource_bars.util.FillDirection;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;
import dev.muon.dynamic_resource_bars.util.NineSliceRenderer;
import dev.muon.dynamic_resource_bars.util.Position;
import dev.muon.dynamic_resource_bars.util.RenderUtil;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import dev.muon.dynamic_resource_bars.util.TextBehavior;
import dev.muon.dynamic_resource_bars.util.TickHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Common skeleton for HUD resource bars (health, mana, stamina, …).
 *
 * <p>Subclasses provide the bar-specific value source, textures, config snapshot, and any
 * unique overlay passes; this class owns the visibility state machine, fading-chunk
 * tracking on value loss, animated bar fill, sub-element rect math, text rendering, and
 * edit-mode outlines.
 *
 * <p>Adding a new bar</p>
 * <ol>
 *   <li>Add config fields to {@link dev.muon.dynamic_resource_bars.config.ClientConfig} for
 *       layout (background/bar/foreground/text rects), anchor, fill direction, text behavior.</li>
 *   <li>Subclass {@link AbstractBarRenderer}, expose a {@code public static final INSTANCE},
 *       and implement the abstract methods.</li>
 *   <li>Drop the bar texture(s) under {@code assets/dynamic_resource_bars/textures/gui/} —
 *       animation comes for free if you ship a {@code .mcmeta}.</li>
 *   <li>Wire it up from {@link dev.muon.dynamic_resource_bars.client.HudBarOrchestrator}
 *       (vanilla replacement) or directly from the loader's HUD wiring class
 *       (free-standing, like mana).</li>
 * </ol>
 */
public abstract class AbstractBarRenderer {

    private static final float CHUNK_HOLD_ALPHA = 0.5f;
    private static final long CHUNK_DIM_DURATION = 120L;
    private static final long CHUNK_DRAIN_DELAY = 450L;
    private static final float CHUNK_DRAIN_ROWS_PER_MS = 30f / 1000f;
    /** Upper bound so a chunk that never gets painted (and so never learns it drained) still goes away. */
    private static final long CHUNK_MAX_LIFETIME = 30_000L;
    private static final float FILL_ROWS_PER_MS = 40f / 1000f;
    /** Frame gaps longer than this (bar hidden, game paused) snap the fill instead of replaying the rise. */
    private static final long FILL_MAX_STEP_GAP = 250L;
    private static final float FLOW_MAX_AMPLITUDE = 1.5f;
    /** Bar width (the default mana and air orbs) whose wave shape every other width is stretched to match. */
    private static final float FLOW_REFERENCE_WIDTH = 23f;
    /** Cap on simultaneously fading "ghost" chunks per bar — prevents unbounded growth under sustained DoT damage. */
    protected static final int MAX_FADING_CHUNKS = 16;

    // ===== Per-instance render state =====
    private boolean barSetVisible = true;
    private long barDisabledStartTime = 0L;
    private float lastValue = -1f;
    private long fullValueStartTime = 0L;
    /** Captured at the top of {@link #render} so subclass hooks can produce smooth (partial-tick)
     *  animation offsets without us having to thread {@code DeltaTracker} through every hook. */
    private float currentTicks = 0f;

    private final List<FadingChunk> fadingChunks = new ArrayList<>();
    private float previousValue = -1f;
    private float previousMax = -1f;

    private float displayedFillPx = -1f;
    private long lastFillUpdateTime = 0L;
    private int liveFillEdgePx = 0;

    /** Per-loss "ghost" segment that dims, holds, then drains row by row above the live bar fill. */
    protected static final class FadingChunk {
        float startValue;
        final float endValue;
        final float maxValue;
        /** {@link #effectiveBarMax} as observed at chunk creation time. Frozen here so a chunk
         *  doesn't visually jump when the live denominator changes (e.g. absorption depleting in
         *  SQUEEZE mode shifts the live denom from {@code max+abs} back to {@code max}). */
        final float denominatorAtCreation;
        final long creationTime;
        final Identifier texture;
        final int animOffset;

        FadingChunk(float startValue, float endValue, float maxValue, float denominatorAtCreation,
                    Identifier texture, int animOffset) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.maxValue = maxValue;
            this.denominatorAtCreation = denominatorAtCreation;
            this.creationTime = System.currentTimeMillis();
            this.texture = texture;
            this.animOffset = animOffset;
        }

        /** Set once a paint pass finds every row drained, since only painting knows the bar's pixel span. */
        boolean drained;

        float getAlpha() {
            long elapsed = System.currentTimeMillis() - creationTime;
            if (elapsed >= CHUNK_DIM_DURATION) return CHUNK_HOLD_ALPHA;
            return 1f - (1f - CHUNK_HOLD_ALPHA) * (elapsed / (float) CHUNK_DIM_DURATION);
        }

        int drainedRows() {
            long drainElapsed = System.currentTimeMillis() - creationTime - CHUNK_DRAIN_DELAY;
            return drainElapsed <= 0 ? 0 : (int) (drainElapsed * CHUNK_DRAIN_ROWS_PER_MS);
        }

        boolean isExpired() {
            return drained || System.currentTimeMillis() - creationTime > CHUNK_MAX_LIFETIME;
        }
    }

    // ===== Required per-bar contract =====

    /** Identifies this bar in {@link EditModeManager} (focus highlights). */
    protected abstract DraggableElement draggable();

    /** Bar-specific config snapshot. Built fresh each render — cheap. */
    protected abstract BarConfig config();

    /** Path to the (animated) static background texture. */
    protected abstract Identifier backgroundTexture();

    /** Path to the (animated) foreground texture. */
    protected abstract Identifier foregroundTexture();

    /** Bar fill texture for the current player state (poison/wither/critical/etc). */
    protected abstract Identifier barTexture(Player player, float current, float max);

    /** Animation metadata shared across all variants of this bar. */
    protected abstract AnimationMetadata.AnimationData barAnimation();

    /** Animation scaling for the background nine-slice. */
    protected abstract AnimationMetadata.ScalingInfo backgroundScaling();

    /** Animation scaling for the foreground nine-slice. */
    protected abstract AnimationMetadata.ScalingInfo foregroundScaling();

    /** Current dynamic value (e.g. health, mana, stamina). */
    protected abstract float currentValue(Player player);

    /** Max dynamic value. */
    protected abstract float maxValue(Player player);

    // ===== Optional hooks (default no-op) =====

    /** Override to suppress rendering entirely (e.g. provider says "off"). */
    protected boolean shouldRender(Player player) {
        return Minecraft.getInstance().gameMode.canHurtPlayer() || EditModeManager.isEditModeEnabled();
    }

    /**
     * Should the bar fade out this frame? Bridges {@link BarVisibility} to a per-bar fade rule.
     * ALWAYS/NEVER short-circuit; SMART_FADE delegates to {@link #smartFadeTrigger}. Subclasses
     * with simple "fade when X" conditions only override {@code smartFadeTrigger}; subclasses
     * whose fade depends on which BarVisibility field to read (stamina-on-mount) or on a
     * provider's bespoke logic (mana) override this method instead.
     */
    protected boolean shouldFadeWhenFull(Player player, float current, float max) {
        return switch (config().barVisibility()) {
            case ALWAYS -> false;
            case NEVER -> true;
            case SMART_FADE -> smartFadeTrigger(player, current, max);
        };
    }

    /** Default SMART_FADE rule: fade once the bar is full. Subclasses override to differ. */
    protected boolean smartFadeTrigger(Player player, float current, float max) {
        return current >= max;
    }

    /**
     * Denominator used when computing the bar fill ratio. Defaults to {@code max} so a normal
     * fill is {@code current/max} of the bar zone. Subclasses override to incorporate a secondary
     * pool — e.g. health adds the active absorption amount in {@code SQUEEZE} mode so the live
     * health portion shrinks to make room for the absorption slice.
     *
     * <p>Used by {@link #renderBaseBar}, {@link #renderFadingChunks}, and
     * {@link #renderRestorePreviewChunk} so chunks and food previews stay aligned with the
     * (possibly squeezed) live fill.
     */
    protected float effectiveBarMax(Player player, float current, float max) {
        return max;
    }

    /**
     * Pixel offset from the leading edge of the bar zone where the live fill starts. Used when
     * the bar has a "reserve" pool that should render <i>before</i> the live fill — e.g.
     * Paragliders' extra stamina, drained only after the base pool runs out. Default is 0
     * (live fill starts flush against the leading edge).
     *
     * <p>The leading edge is the left for left-anchored horizontal bars, the right for
     * right-anchored, and the bottom for vertical bottom-up fills. The reserve itself is painted
     * by the subclass (typically in {@link #renderBetweenBarAndForeground}) at the leading-edge
     * region <i>before</i> the offset; this hook only tells the live fill, fading chunks, and
     * food previews to shift past that region so they don't overlap it.
     *
     * <p>Contrast with {@link #effectiveBarMax}: that hook grows the denominator (used by both
     * health absorption SQUEEZE and stamina extra), while this hook chooses where on the bar
     * the live portion sits. Health's SQUEEZE absorption returns 0 here so absorption ends up at
     * the trailing edge (acts as a shield, drained first); stamina's extra returns the extra
     * width so the live base ends up shifted right and extra sits at the leading edge (acts as
     * a reserve, drained last).
     */
    protected int leadingFillOffset(Player player, float current, float max, ScreenRect barRect) {
        return 0;
    }

    /** Drawn after the bar fill but before foreground/text. Pulses, attribute glows, etc. */
    protected void renderBarOverlays(GuiGraphicsExtractor graphics, Player player,
                                     float current, float max, ScreenRect barRect, float alpha) {}

    /** Drawn over the entire complex (after foreground). Hardcore border, wetness, etc. */
    protected void renderBackgroundOverlays(GuiGraphicsExtractor graphics, Player player,
                                            ScreenRect complexRect, float alpha) {}

    /** Drawn after the main text — e.g. health's "+8" absorption tag. */
    protected void renderAuxiliaryText(GuiGraphicsExtractor graphics, Player player, float alpha) {}

    /** Optional overlay drawn between bar fill and foreground (e.g. mana's reserved region). */
    protected void renderBetweenBarAndForeground(GuiGraphicsExtractor graphics, Player player,
                                                 float current, float max, ScreenRect barRect,
                                                 int animOffset, AnimationMetadata.AnimationData animData) {}

    /**
     * Pixel width of the food-restore preview segment for the current frame, or 0 if no preview
     * applies (vertical fill, zero restore, etc.). Exposed so subclasses can shift other content
     * out of the preview's footprint — e.g. health's SQUEEZE absorption slice slides right by
     * this much so the preview doesn't paint on top of it.
     */
    protected final int restorePreviewWidth(Player player, float current, float max,
                                            float restoreAmount, ScreenRect barRect) {
        if (restoreAmount <= 0f || max <= 0f) return 0;
        float denom = effectiveBarMax(player, current, max);
        if (denom <= 0f) return 0;
        // Clamp against the empty *primary* zone (max - current), not the entire empty bar — when
        // SQUEEZE inflates the denominator, the trailing absorption slice is empty in this sense
        // but the preview must not paint over it.
        float clampedRestore = Math.max(0f, Math.min(restoreAmount, max - current));
        if (clampedRestore <= 0f) return 0;
        float fillRatio = Math.max(0f, Math.min(1f, current / denom));
        float restoreRatio = clampedRestore / denom;
        int span = config().fillDirection() == FillDirection.VERTICAL ? barRect.height() : barRect.width();
        int filled = (int) (span * fillRatio);
        int combined = (int) (span * (fillRatio + restoreRatio));
        return combined - filled;
    }

    protected final void renderRestorePreviewChunk(GuiGraphicsExtractor graphics, Player player,
                                                   float current, float max, float restoreAmount,
                                                   ScreenRect barRect, int animOffset,
                                                   AnimationMetadata.AnimationData animData) {
        int restoreSize = restorePreviewWidth(player, current, max, restoreAmount, barRect);
        if (restoreSize <= 0) return;
        BarConfig cfg = config();
        float denom = effectiveBarMax(player, current, max);
        int leadingOffset = leadingFillOffset(player, current, max, barRect);
        float pulse = 0.5f + (TickHandler.getOverlayFlashAlpha() * 0.5f);
        int tint = RenderUtil.whiteWithAlpha(pulse * currentAlpha());
        Identifier texture = barTexture(player, current, max);

        if (cfg.fillDirection() == FillDirection.VERTICAL) {
            paintFillSpan(graphics, texture, barRect, true, false, animOffset,
                    animData.textureWidth, animData.textureHeight,
                    liveFillEdgePx, liveFillEdgePx + restoreSize, true, true, tint);
            return;
        }

        int filled = (int) (barRect.width() * Math.max(0f, Math.min(1f, current / denom)));
        boolean rightAnchored = cfg.anchor().getSide() == HUDPositioning.AnchorSide.RIGHT;
        int x, u;
        if (rightAnchored) {
            x = barRect.x() + barRect.width() - filled - restoreSize - leadingOffset;
            u = barRect.width() - filled - restoreSize - leadingOffset;
        } else {
            x = barRect.x() + leadingOffset + filled;
            u = leadingOffset + filled;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, barRect.y(), u, animOffset,
                restoreSize, barRect.height(), animData.textureWidth, animData.textureHeight, tint);
    }

    /** Returns true if the main current-of-max text should render this frame. */
    protected boolean shouldRenderText(float current, float max) {
        TextBehavior behavior = config().textBehavior();
        if (EditModeManager.isEditModeEnabled()) {
            if (behavior != TextBehavior.NEVER) return true;
            if (EditModeManager.isFocusMode()
                    && EditModeManager.getFocusedElement() == draggable()
                    && EditModeManager.getFocusedSubElement() == SubElementType.TEXT) {
                return true;
            }
        }
        return switch (behavior) {
            case NEVER -> false;
            case ALWAYS -> true;
            case WHEN_NOT_FULL -> {
                boolean isFull = current >= max;
                if (isFull) {
                    if (lastValue < max || lastValue == -1f) {
                        fullValueStartTime = System.currentTimeMillis();
                    }
                    lastValue = current;
                    yield (System.currentTimeMillis() - fullValueStartTime) < RenderUtil.textDisplayDuration();
                }
                lastValue = current;
                yield true;
            }
        };
    }

    /** Edit-mode bar outline color. Subclass to differentiate (default: green). */
    protected int editModeBarOutlineColor() {
        return 0xA000FF00;
    }

    // ===== Template method =====

    public final void render(GuiGraphicsExtractor graphics, Player player, DeltaTracker deltaTracker) {
        if (player == null) return;
        if (!shouldRender(player)) return;

        BarConfig cfg = config();
        float current = currentValue(player);
        float max = maxValue(player);

        updateChunkTracking(player, current, max, deltaTracker.getGameTimeDeltaTicks(), cfg);

        boolean shouldFade = shouldFadeWhenFull(player, current, max);
        setVisibility(!shouldFade || EditModeManager.isEditModeEnabled());

        long offDuration = ModConfigManager.getClient().fadeHoldDuration + RenderUtil.BAR_FADEOUT_DURATION;
        if (!isVisible() && !EditModeManager.isEditModeEnabled()
                && (System.currentTimeMillis() - barDisabledStartTime) > offDuration) {
            return;
        }

        float alpha = currentAlpha();
        if (EditModeManager.isEditModeEnabled() && !isVisible()) alpha = 1.0f;
        int tint = RenderUtil.whiteWithAlpha(alpha);

        ScreenRect complexRect = getScreenRect(player, cfg);

        if (cfg.enableBackground()) {
            ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player, cfg);
            Identifier bgTex = backgroundTexture();
            AnimationMetadata.TextureDimensions bgDims = AnimationMetadataCache.getTextureDimensions(bgTex);
            NineSliceRenderer.renderWithScaling(graphics, bgTex, backgroundScaling(),
                    bgRect.x(), bgRect.y(), bgRect.width(), bgRect.height(),
                    bgDims.width, bgDims.height, tint);
        }

        AnimationMetadata.AnimationData animData = barAnimation();
        float ticks = player.tickCount + deltaTracker.getGameTimeDeltaTicks();
        this.currentTicks = ticks;
        int animOffset = AnimationMetadata.calculateAnimationOffset(animData, ticks);

        ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player, cfg);
        boolean rightAnchored = cfg.anchor().getSide() == HUDPositioning.AnchorSide.RIGHT;

        renderBaseBar(graphics, player, current, max, barRect, animOffset, rightAnchored, animData, cfg, tint);
        renderFadingChunks(graphics, player, barRect, current, max, rightAnchored, alpha, animData, cfg);
        renderBetweenBarAndForeground(graphics, player, current, max, barRect, animOffset, animData);
        renderBarOverlays(graphics, player, current, max, barRect, alpha);

        if (cfg.enableForeground()) {
            ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND, player, cfg);
            Identifier fgTex = foregroundTexture();
            AnimationMetadata.TextureDimensions fgDims = AnimationMetadataCache.getTextureDimensions(fgTex);
            NineSliceRenderer.renderWithScaling(graphics, fgTex, foregroundScaling(),
                    fgRect.x(), fgRect.y(), fgRect.width(), fgRect.height(),
                    fgDims.width, fgDims.height, tint);
        }

        renderBackgroundOverlays(graphics, player, complexRect, alpha);


        if (shouldRenderText(current, max)) {
            renderText(graphics, player, current, max, alpha, cfg);
        }
        renderAuxiliaryText(graphics, player, alpha);

        if (EditModeManager.isEditModeEnabled()) {
            renderEditModeOutlines(graphics, player, complexRect, cfg);
        }

    }

    // ===== Sub-element math =====

    public final ScreenRect getScreenRect(Player player) {
        return getScreenRect(player, config());
    }

    private ScreenRect getScreenRect(Player player, BarConfig cfg) {
        if (player == null) return new ScreenRect(0, 0, 0, 0);
        Position base = HUDPositioning.getPositionFromAnchor(cfg.anchor())
                .offset(cfg.totalXOffset(), cfg.totalYOffset());
        return new ScreenRect(base.x(), base.y(), cfg.backgroundWidth(), cfg.backgroundHeight());
    }

    public final ScreenRect getSubElementRect(SubElementType type, Player player) {
        return getSubElementRect(type, player, config());
    }

    private ScreenRect getSubElementRect(SubElementType type, Player player, BarConfig cfg) {
        ScreenRect complex = getScreenRect(player, cfg);
        if (complex.width() == 0 && complex.height() == 0) return new ScreenRect(0, 0, 0, 0);
        int x = complex.x();
        int y = complex.y();
        return switch (type) {
            case BACKGROUND -> new ScreenRect(x + cfg.backgroundXOffset(), y + cfg.backgroundYOffset(),
                    cfg.backgroundWidth(), cfg.backgroundHeight());
            case BAR_MAIN -> new ScreenRect(x + cfg.barXOffset(), y + cfg.barYOffset(),
                    cfg.barWidth(), cfg.barHeight());
            case FOREGROUND -> new ScreenRect(x + cfg.overlayXOffset(), y + cfg.overlayYOffset(),
                    cfg.overlayWidth(), cfg.overlayHeight());
            case TEXT -> new ScreenRect(x + cfg.textXOffset(), y + cfg.textYOffset(),
                    cfg.textWidth(), cfg.textHeight());
            case ICON, ABSORPTION_TEXT -> getCustomSubElementRect(type, player, complex);
        };
    }

    /**
     * Hook for bar-specific sub-elements (ICON, ABSORPTION_TEXT) that aren't part of the
     * universal BACKGROUND/BAR_MAIN/FOREGROUND/TEXT layout. Default returns an empty rect
     * — subclasses override to expose their own sub-elements to the editor's focus mode.
     *
     * @param complexRect the bar's enclosing rect, anchored + offset (saves the subclass a recompute).
     */
    protected ScreenRect getCustomSubElementRect(SubElementType type, Player player, ScreenRect complexRect) {
        return new ScreenRect(0, 0, 0, 0);
    }

    // ===== Bar fill =====

    private void renderBaseBar(GuiGraphicsExtractor graphics, Player player, float current, float max,
                               ScreenRect barRect, int animOffset, boolean rightAnchored,
                               AnimationMetadata.AnimationData animData, BarConfig cfg, int tint) {
        Identifier tex = barTexture(player, current, max);
        float denom = effectiveBarMax(player, current, max);
        float ratio = (denom <= 0f) ? 0f : Math.max(0f, Math.min(1f, current / denom));
        int leadingOffset = leadingFillOffset(player, current, max, barRect);
        boolean vertical = cfg.fillDirection() == FillDirection.VERTICAL;
        int span = vertical ? barRect.height() : barRect.width();

        int filled = (int) (span * ratio);
        if (filled <= 0 && current > 0) filled = 1;
        int targetEdge = filled <= 0 ? leadingOffset : leadingOffset + filled;
        liveFillEdgePx = stepDisplayedFill(targetEdge);
        if (liveFillEdgePx <= leadingOffset) return;

        paintFillSpan(graphics, tex, barRect, vertical, rightAnchored, animOffset,
                animData.textureWidth, animData.textureHeight,
                leadingOffset, liveFillEdgePx, false, true, tint);
    }

    /** Snaps down instantly (the fading chunk covers losses) but rises toward gains one row at a time. */
    private int stepDisplayedFill(int targetEdge) {
        long now = System.currentTimeMillis();
        long gap = now - lastFillUpdateTime;
        lastFillUpdateTime = now;
        if (displayedFillPx < 0f || targetEdge <= displayedFillPx || gap > FILL_MAX_STEP_GAP) {
            displayedFillPx = targetEdge;
        } else {
            displayedFillPx = Math.min(targetEdge, displayedFillPx + gap * FILL_ROWS_PER_MS);
        }
        return (int) displayedFillPx;
    }

    /**
     * Distance in pixels from the leading edge to the live fill's surface as drawn this frame,
     * including any {@link #leadingFillOffset}. Lags the true value while a gain animates in, so
     * content stacked on the fill should anchor here to stay flush with it.
     */
    protected final int liveFillEdgePx() {
        return liveFillEdgePx;
    }

    /**
     * Blits the strip of a bar texture between {@code fromPx} and {@code toPx}, measured from the
     * bar's leading edge. On vertical bars, each flagged edge gets the animated "liquid surface"
     * offset per column; two strips sharing an edge position get identical offsets, so they meet
     * without seams.
     */
    protected final void paintFillSpan(GuiGraphicsExtractor graphics, Identifier texture, ScreenRect barRect,
                                       boolean vertical, boolean rightAnchored, int animOffset,
                                       int textureWidth, int textureHeight, int fromPx, int toPx,
                                       boolean flowingFrom, boolean flowingTo, int tint) {
        int span = vertical ? barRect.height() : barRect.width();
        if (!vertical || (!flowingFrom && !flowingTo)) {
            blitSpanRun(graphics, texture, barRect, vertical, rightAnchored, animOffset, textureWidth, textureHeight,
                    0, vertical ? barRect.width() : barRect.height(),
                    Math.max(0, fromPx), Math.min(span, toPx), tint);
            return;
        }

        int lanes = barRect.width();
        int runStart = 0, runLo = 0, runHi = 0;
        for (int lane = 0; lane <= lanes; lane++) {
            int lo = 0, hi = 0;
            if (lane < lanes) {
                lo = Math.max(0, fromPx + (flowingFrom ? flowOffset(lane, lanes, fromPx, span) : 0));
                hi = Math.min(span, toPx + (flowingTo ? flowOffset(lane, lanes, toPx, span) : 0));
            }
            if (lane > 0 && (lane == lanes || lo != runLo || hi != runHi)) {
                blitSpanRun(graphics, texture, barRect, true, rightAnchored, animOffset, textureWidth, textureHeight,
                        runStart, lane - runStart, runLo, runHi, tint);
                runStart = lane;
            }
            runLo = lo;
            runHi = hi;
        }
    }

    private static void blitSpanRun(GuiGraphicsExtractor graphics, Identifier texture, ScreenRect barRect,
                                    boolean vertical, boolean rightAnchored, int animOffset,
                                    int textureWidth, int textureHeight,
                                    int laneStart, int laneCount, int lo, int hi, int tint) {
        int length = hi - lo;
        if (length <= 0 || laneCount <= 0) return;
        if (vertical) {
            int fromTop = barRect.height() - hi;
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, barRect.x() + laneStart, barRect.y() + fromTop,
                    laneStart, animOffset + fromTop, laneCount, length, textureWidth, textureHeight, tint);
            return;
        }
        int u = rightAnchored ? barRect.width() - hi : lo;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, barRect.x() + u, barRect.y() + laneStart,
                u, animOffset + laneStart, length, laneCount, textureWidth, textureHeight, tint);
    }

    /**
     * Surface offset for one column of an edge at {@code edgePx}. Two out-of-phase sine waves keep
     * the motion from looking like a single rolling band, and the amplitude tapers to zero at the
     * ends of the bar so a full or empty bar has a flat edge. Wavelength scales with the bar's
     * width so every orb shows the same number of crests, while the amplitude stays in pixels.
     */
    private int flowOffset(int lane, int lanes, int edgePx, int span) {
        float amplitude = Math.min(FLOW_MAX_AMPLITUDE, Math.min(edgePx, span - edgePx));
        if (amplitude <= 0f) return 0;
        float x = lane * (FLOW_REFERENCE_WIDTH / (float) Math.max(1, lanes));
        float t = currentTicks;
        float wave = 0.6f * (float) Math.sin(x * 0.35f + t * 0.22f)
                + 0.4f * (float) Math.sin(x * 0.83f - t * 0.31f);
        return Math.round(amplitude * wave);
    }

    // ===== Loss-spillover chunks =====

    private void updateChunkTracking(Player player, float current, float max, float partialTicks, BarConfig cfg) {
        Iterator<FadingChunk> it = fadingChunks.iterator();
        while (it.hasNext()) {
            FadingChunk chunk = it.next();
            if (chunk.isExpired() || current >= chunk.endValue) it.remove();
        }
        if (previousValue > 0 && current < previousValue && previousMax == max) {
            float chunkStart = Math.max(0f, current);
            float denomAtCreation = effectiveBarMax(player, current, max);
            Identifier texture = barTexture(player, current, max);
            FadingChunk newest = fadingChunks.isEmpty() ? null : fadingChunks.get(fadingChunks.size() - 1);
            // Extending the newest chunk keeps sustained damage as one draining column instead of a
            // stack of chunks whose drains would open gaps between each other.
            if (newest != null && newest.denominatorAtCreation == denomAtCreation && newest.texture.equals(texture)) {
                newest.startValue = Math.min(newest.startValue, chunkStart);
            } else {
                if (fadingChunks.size() >= MAX_FADING_CHUNKS) fadingChunks.remove(0);
                int animOffset = AnimationMetadata.calculateAnimationOffset(barAnimation(),
                        player.tickCount + partialTicks);
                fadingChunks.add(new FadingChunk(chunkStart, previousValue, max, denomAtCreation,
                        texture, animOffset));
            }
        }
        previousValue = current;
        previousMax = max;
    }

    private void renderFadingChunks(GuiGraphicsExtractor graphics, Player player, ScreenRect barRect,
                                    float current, float max, boolean rightAnchored, float parentAlpha,
                                    AnimationMetadata.AnimationData animData, BarConfig cfg) {
        // Base-pool chunks share the live fill's leading offset so they appear in the same zone
        // (post-reserve) where the base used to be, not in the reserve region. They also sit on the
        // live fill's flowing surface, so a gain rising into a chunk eats it from below.
        paintFadingChunks(graphics, fadingChunks, barRect, rightAnchored,
                leadingFillOffset(player, current, max, barRect), liveFillEdgePx, true,
                parentAlpha, animData, cfg);
    }

    /**
     * Paints any list of {@link FadingChunk} entries at their snapshot value ranges, with an
     * optional {@code zoneOffset} pushing the strip past a reserve region. Each chunk uses the
     * denominator captured when it was created, so chunks don't visually drift if the live pool's
     * denominator shifts mid-fade.
     *
     * <p>Subclasses with a secondary fading pool (e.g. stamina's extra-stamina reserve) call this
     * with {@code zoneOffset = 0} to render in the reserve zone, while the built-in base-pool
     * pass uses {@link #leadingFillOffset} so chunks end up in the live region.
     */
    protected final void paintFadingChunks(GuiGraphicsExtractor graphics, List<FadingChunk> chunks,
                                           ScreenRect barRect, boolean rightAnchored, int zoneOffset,
                                           float parentAlpha, AnimationMetadata.AnimationData animData,
                                           BarConfig cfg) {
        paintFadingChunks(graphics, chunks, barRect, rightAnchored, zoneOffset, 0, false,
                parentAlpha, animData, cfg);
    }

    private void paintFadingChunks(GuiGraphicsExtractor graphics, List<FadingChunk> chunks,
                                   ScreenRect barRect, boolean rightAnchored, int zoneOffset,
                                   int floorPx, boolean flowingEdges, float parentAlpha,
                                   AnimationMetadata.AnimationData animData, BarConfig cfg) {
        if (chunks.isEmpty()) return;
        boolean vertical = cfg.fillDirection() == FillDirection.VERTICAL;
        int span = vertical ? barRect.height() : barRect.width();
        for (FadingChunk chunk : chunks) {
            float denom = chunk.denominatorAtCreation;
            if (denom <= 0f) continue;
            float startRatio = Math.max(0f, Math.min(1f, chunk.startValue / denom));
            float endRatio = Math.max(0f, Math.min(1f, chunk.endValue / denom));
            int bottom = Math.max(floorPx, zoneOffset + (int) (span * startRatio));
            int top = zoneOffset + (int) (span * endRatio) - chunk.drainedRows();
            if (top <= bottom) {
                chunk.drained = true;
                continue;
            }
            float a = chunk.getAlpha() * parentAlpha;
            if (a <= 0) continue;
            paintFillSpan(graphics, chunk.texture, barRect, vertical, rightAnchored, chunk.animOffset,
                    animData.textureWidth, animData.textureHeight, bottom, top,
                    flowingEdges, flowingEdges, RenderUtil.whiteWithAlpha(a));
        }
    }

    // ===== Text =====

    private void renderText(GuiGraphicsExtractor graphics, Player player, float current, float max,
                            float barAlpha, BarConfig cfg) {
        ScreenRect textRect = getSubElementRect(SubElementType.TEXT, player, cfg);
        int textX = textRect.x() + (textRect.width() / 2);
        int textY = textRect.y() + (textRect.height() / 2);
        int color = computeTextColor(current, max, barAlpha, cfg);
        HorizontalAlignment alignment = cfg.textAlign();
        int baseX = switch (alignment) {
            case CENTER -> textX;
            case RIGHT -> textRect.x() + textRect.width();
            case LEFT -> textRect.x();
        };
        RenderUtil.renderText(current, max, graphics, baseX, textY, cfg.textScale(), color, alignment);
    }

    private int computeTextColor(float current, float max, float barAlpha, BarConfig cfg) {
        int baseColor = cfg.textColor() & 0xFFFFFF;
        int alpha = cfg.textOpacity();
        if (!EditModeManager.isEditModeEnabled()
                && cfg.textBehavior() == TextBehavior.WHEN_NOT_FULL
                && current >= max) {
            long timeSinceFull = System.currentTimeMillis() - fullValueStartTime;
            alpha = (int) (alpha * (RenderUtil.calculateTextAlpha(timeSinceFull) / (float) RenderUtil.BASE_TEXT_ALPHA));
        }
        alpha = (int) (alpha * barAlpha);
        alpha = Math.max(10, Math.min(255, alpha));
        return (alpha << 24) | baseColor;
    }

    // ===== Edit mode =====

    private void renderEditModeOutlines(GuiGraphicsExtractor graphics, Player player,
                                        ScreenRect complex, BarConfig cfg) {
        if (EditModeManager.getFocusedElement() == draggable()) {
            if (!EditModeManager.isFocusMode()) {
                graphics.outline(complex.x() - 1, complex.y() - 1, complex.width() + 2, complex.height() + 2, 0xFFFFD84D);
                graphics.outline(complex.x() - 2, complex.y() - 2, complex.width() + 4, complex.height() + 4, 0x60FFD84D);
            }
        } else {
            graphics.outline(complex.x() - 1, complex.y() - 1, complex.width() + 2, complex.height() + 2, 0x60FFFFFF);
        }
    }

    // ===== Visibility state machine =====

    private void setVisibility(boolean visible) {
        if (barSetVisible != visible) {
            if (!visible) barDisabledStartTime = System.currentTimeMillis();
            barSetVisible = visible;
        }
    }

    private boolean isVisible() {
        return barSetVisible;
    }

    /** Most recent {@code tickCount + partialTicks} captured at the top of {@link #render}.
     *  Subclass hooks read this when they need a smooth animation offset for textures other than
     *  the main bar fill (e.g. health's SQUEEZE absorption strip). Returns 0 outside of a render. */
    protected final float currentTicks() {
        return currentTicks;
    }

    /** Externally accessible alpha for hooks that need it (e.g. health's temperature overlay). */
    public final float currentAlpha() {
        if (isVisible()) return 1f;
        long elapsed = System.currentTimeMillis() - barDisabledStartTime;
        long hold = ModConfigManager.getClient().fadeHoldDuration;
        if (elapsed < hold) return 1f;
        long fadeElapsed = elapsed - hold;
        if (fadeElapsed >= RenderUtil.BAR_FADEOUT_DURATION) return 0f;
        return Math.max(0f, 1f - (fadeElapsed / (float) RenderUtil.BAR_FADEOUT_DURATION));
    }
}
