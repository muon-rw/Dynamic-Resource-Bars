package dev.muon.dynamic_resource_bars.render;

import com.mojang.blaze3d.systems.RenderSystem;
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

    private static final long CHUNK_FADEOUT_DURATION = RenderUtil.BAR_FADEOUT_DURATION / 3;
    /** Cap on simultaneously fading "ghost" chunks per bar — prevents unbounded growth under sustained DoT damage. */
    private static final int MAX_FADING_CHUNKS = 16;

    // ===== Per-instance render state =====
    private boolean barSetVisible = true;
    private long barDisabledStartTime = 0L;
    private float lastValue = -1f;
    private long fullValueStartTime = 0L;

    private final List<FadingChunk> fadingChunks = new ArrayList<>();
    private float previousValue = -1f;
    private float previousMax = -1f;

    /** Per-loss "ghost" segment that fades out behind the live bar fill. */
    protected static final class FadingChunk {
        final float startValue;
        final float endValue;
        final float maxValue;
        final long creationTime;
        final Identifier texture;
        final int animOffset;

        FadingChunk(float startValue, float endValue, float maxValue, Identifier texture, int animOffset) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.maxValue = maxValue;
            this.creationTime = System.currentTimeMillis();
            this.texture = texture;
            this.animOffset = animOffset;
        }

        float getAlpha() {
            long elapsed = System.currentTimeMillis() - creationTime;
            if (elapsed >= CHUNK_FADEOUT_DURATION) return 0f;
            return 1f - (elapsed / (float) CHUNK_FADEOUT_DURATION);
        }

        boolean isExpired() {
            return getAlpha() <= 0f;
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
     * Draws a flashing chunk of the bar fill texture in the segment that would refill if the
     * player consumed {@code restoreAmount} more units. Used by the AppleSkin held-food previews
     * on health and stamina; the chunk continues seamlessly from the live fill (matching U/V
     * coords + animOffset). Horizontal-only — vertical fill direction skips the preview.
     */
    protected final void renderRestorePreviewChunk(GuiGraphicsExtractor graphics, Player player,
                                                   float current, float max, float restoreAmount,
                                                   ScreenRect barRect, int animOffset,
                                                   AnimationMetadata.AnimationData animData) {
        if (restoreAmount <= 0f || max <= 0f) return;
        BarConfig cfg = config();
        if (cfg.fillDirection() != FillDirection.HORIZONTAL) return;

        float fillRatio = Math.max(0f, Math.min(1f, current / max));
        float restoreRatio = Math.max(0f, Math.min(1f - fillRatio, restoreAmount / max));
        if (restoreRatio <= 0f) return;

        int filled = (int) (barRect.width() * fillRatio);
        int restoreWidth = (int) (barRect.width() * restoreRatio);
        if (restoreWidth <= 0) return;

        boolean rightAnchored = cfg.anchor().getSide() == HUDPositioning.AnchorSide.RIGHT;
        int x, u;
        if (rightAnchored) {
            x = barRect.x() + barRect.width() - filled - restoreWidth;
            u = barRect.width() - filled - restoreWidth;
        } else {
            x = barRect.x() + filled;
            u = filled;
        }

        float pulse = 0.5f + (TickHandler.getOverlayFlashAlpha() * 0.5f);
        int tint = RenderUtil.whiteWithAlpha(pulse * currentAlpha());
        RenderUtil.blitWithBinding(graphics, barTexture(player, current, max),
                x, barRect.y(), u, animOffset, restoreWidth, barRect.height(),
                animData.textureWidth, animData.textureHeight, tint);
    }

    /** Returns true if the main current-of-max text should render this frame. */
    protected boolean shouldRenderText(float current, float max) {
        TextBehavior behavior = config().textBehavior();
        if (EditModeManager.isEditModeEnabled() && behavior != TextBehavior.NEVER) return true;
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
                    yield (System.currentTimeMillis() - fullValueStartTime) < RenderUtil.TEXT_DISPLAY_DURATION;
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

        if (!isVisible() && !EditModeManager.isEditModeEnabled()
                && (System.currentTimeMillis() - barDisabledStartTime) > RenderUtil.BAR_FADEOUT_DURATION) {
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
        int animOffset = AnimationMetadata.calculateAnimationOffset(animData, ticks);

        ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player, cfg);
        boolean rightAnchored = cfg.anchor().getSide() == HUDPositioning.AnchorSide.RIGHT;

        renderBaseBar(graphics, player, current, max, barRect, animOffset, rightAnchored, animData, cfg, tint);
        renderFadingChunks(graphics, barRect, current, max, rightAnchored, alpha, animData, cfg);
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
                    cfg.barWidth(), cfg.barHeight());
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
        float ratio = (max <= 0f) ? 0f : Math.max(0f, Math.min(1f, current / max));

        if (cfg.fillDirection() == FillDirection.VERTICAL) {
            int filled = (int) (barRect.height() * ratio);
            if (filled <= 0 && current > 0) filled = 1;
            if (filled <= 0) return;
            int y = barRect.y() + (barRect.height() - filled);
            int v = animOffset + (barRect.height() - filled);
            RenderUtil.blitWithBinding(graphics, tex,
                    barRect.x(), y, 0, v, barRect.width(), filled,
                    animData.textureWidth, animData.textureHeight, tint);
        } else {
            int filled = (int) (barRect.width() * ratio);
            if (filled <= 0 && current > 0) filled = 1;
            if (filled <= 0) return;
            int x = barRect.x();
            int u = 0;
            if (rightAnchored) {
                x = barRect.x() + barRect.width() - filled;
                u = barRect.width() - filled;
            }
            RenderUtil.blitWithBinding(graphics, tex,
                    x, barRect.y(), u, animOffset, filled, barRect.height(),
                    animData.textureWidth, animData.textureHeight, tint);
        }
    }

    // ===== Loss-spillover chunks =====

    private void updateChunkTracking(Player player, float current, float max, float partialTicks, BarConfig cfg) {
        Iterator<FadingChunk> it = fadingChunks.iterator();
        while (it.hasNext()) {
            FadingChunk chunk = it.next();
            if (chunk.isExpired() || current >= chunk.endValue) it.remove();
        }
        if (previousValue > 0 && current < previousValue && previousMax == max) {
            if (fadingChunks.size() >= MAX_FADING_CHUNKS) fadingChunks.remove(0);
            int animOffset = AnimationMetadata.calculateAnimationOffset(barAnimation(),
                    player.tickCount + partialTicks);
            float chunkStart = Math.max(0f, current);
            fadingChunks.add(new FadingChunk(chunkStart, previousValue, max,
                    barTexture(player, current, max), animOffset));
        }
        previousValue = current;
        previousMax = max;
    }

    private void renderFadingChunks(GuiGraphicsExtractor graphics, ScreenRect barRect,
                                    float current, float max, boolean rightAnchored, float parentAlpha,
                                    AnimationMetadata.AnimationData animData, BarConfig cfg) {
        if (fadingChunks.isEmpty()) return;
        for (FadingChunk chunk : fadingChunks) {
            float a = chunk.getAlpha() * parentAlpha;
            if (a <= 0) continue;
            int chunkTint = RenderUtil.whiteWithAlpha(a);
            float startRatio = Math.max(0f, Math.min(1f, chunk.startValue / chunk.maxValue));
            float endRatio = Math.max(0f, Math.min(1f, chunk.endValue / chunk.maxValue));
            if (cfg.fillDirection() == FillDirection.VERTICAL) {
                int startH = (int) (barRect.height() * startRatio);
                int endH = (int) (barRect.height() * endRatio);
                int chunkH = endH - startH;
                if (chunkH <= 0) continue;
                int y = barRect.y() + (barRect.height() - endH);
                int v = chunk.animOffset + (barRect.height() - endH);
                RenderUtil.blitWithBinding(graphics, chunk.texture,
                        barRect.x(), y, 0, v, barRect.width(), chunkH,
                        animData.textureWidth, animData.textureHeight, chunkTint);
            } else {
                int startW = (int) (barRect.width() * startRatio);
                int endW = (int) (barRect.width() * endRatio);
                int chunkW = endW - startW;
                if (chunkW <= 0) continue;
                int x, u;
                if (rightAnchored) {
                    x = barRect.x() + barRect.width() - endW;
                    u = barRect.width() - endW;
                } else {
                    x = barRect.x() + startW;
                    u = startW;
                }
                RenderUtil.blitWithBinding(graphics, chunk.texture,
                        x, barRect.y(), u, chunk.animOffset, chunkW, barRect.height(),
                        animData.textureWidth, animData.textureHeight, chunkTint);
            }
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
        RenderUtil.renderText(current, max, graphics, baseX, textY, color, alignment);
    }

    private int computeTextColor(float current, float max, float barAlpha, BarConfig cfg) {
        int baseColor = cfg.textColor() & 0xFFFFFF;
        int alpha = cfg.textOpacity();
        if (cfg.textBehavior() == TextBehavior.WHEN_NOT_FULL && current >= max) {
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
            int focused = 0xA0FFFF00;
            if (cfg.enableBackground()) {
                ScreenRect bg = getSubElementRect(SubElementType.BACKGROUND, player, cfg);
                graphics.outline(bg.x() - 1, bg.y() - 1, bg.width() + 2, bg.height() + 2, focused);
            }
            ScreenRect bar = getSubElementRect(SubElementType.BAR_MAIN, player, cfg);
            graphics.outline(bar.x() - 1, bar.y() - 1, bar.width() + 2, bar.height() + 2, editModeBarOutlineColor());
            if (cfg.enableForeground()) {
                ScreenRect fg = getSubElementRect(SubElementType.FOREGROUND, player, cfg);
                graphics.outline(fg.x() - 1, fg.y() - 1, fg.width() + 2, fg.height() + 2, 0xA0FF00FF);
            }
            graphics.outline(complex.x() - 2, complex.y() - 2, complex.width() + 4, complex.height() + 4, 0x80FFFFFF);
        } else {
            graphics.outline(complex.x() - 1, complex.y() - 1, complex.width() + 2, complex.height() + 2, 0x80FFFFFF);
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

    /** Externally accessible alpha for hooks that need it (e.g. health's temperature overlay). */
    public final float currentAlpha() {
        if (isVisible()) return 1f;
        long elapsed = System.currentTimeMillis() - barDisabledStartTime;
        if (elapsed >= RenderUtil.BAR_FADEOUT_DURATION) return 0f;
        return Math.max(0f, 1f - (elapsed / (float) RenderUtil.BAR_FADEOUT_DURATION));
    }
}
