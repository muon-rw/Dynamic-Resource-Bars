package dev.muon.dynamic_resource_bars.render;

import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.compat.AppleSkinCompat;
import dev.muon.dynamic_resource_bars.compat.StaminaProviderManager;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import dev.muon.dynamic_resource_bars.provider.StaminaProvider;
import dev.muon.dynamic_resource_bars.util.AnimationMetadata;
import dev.muon.dynamic_resource_bars.util.AnimationMetadataCache;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.FillDirection;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.NineSliceRenderer;
import dev.muon.dynamic_resource_bars.util.RenderUtil;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import dev.muon.dynamic_resource_bars.util.TickHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StaminaBarRenderer extends AbstractBarRenderer {

    public static final StaminaBarRenderer INSTANCE = new StaminaBarRenderer();

    private final List<FadingChunk> reserveChunks = new ArrayList<>();
    /** Last-seen extra value, used to detect drains and spawn fading chunks. {@code -1} means uninitialized. */
    private float previousExtra = -1f;

    @Override protected DraggableElement draggable() { return DraggableElement.STAMINA_BAR; }
    @Override protected int editModeBarOutlineColor() { return 0xA0FFA500; }

    @Override
    protected BarConfig config() {
        ClientConfig c = ModConfigManager.getClient();
        return new BarConfig(
                c.staminaBackgroundWidth, c.staminaBackgroundHeight,
                c.staminaBackgroundXOffset, c.staminaBackgroundYOffset,
                c.staminaBarWidth, c.staminaBarHeight, c.staminaBarXOffset, c.staminaBarYOffset,
                c.staminaOverlayWidth, c.staminaOverlayHeight, c.staminaOverlayXOffset, c.staminaOverlayYOffset,
                c.staminaTextXOffset, c.staminaTextYOffset, c.staminaTextWidth, c.staminaTextHeight,
                c.staminaTextColor, c.staminaTextOpacity, c.staminaTextAlign,
                c.staminaTotalXOffset, c.staminaTotalYOffset,
                c.staminaBarAnchor,
                c.enableStaminaBackground, c.enableStaminaForeground, c.staminaBarVisibility,
                c.staminaFillDirection, c.showStaminaText
        );
    }

    private static final Identifier BACKGROUND_ID = Constants.loc("textures/gui/stamina_background.png");
    private static final Identifier FOREGROUND_ID = Constants.loc("textures/gui/stamina_foreground.png");
    private static final Identifier BAR_DEFAULT = Constants.loc("textures/gui/stamina_bar.png");
    private static final Identifier BAR_CRITICAL = Constants.loc("textures/gui/stamina_bar_critical.png");
    private static final Identifier BAR_MOUNTED = Constants.loc("textures/gui/stamina_bar_mounted.png");
    private static final Identifier SATURATION_OVERLAY = Constants.loc("textures/gui/saturation_overlay.png");
    private static final Identifier EXTRA_STAMINA_BAR = Constants.loc("textures/gui/extra_stamina_bar.png");
    /** Per-name cache for provider-supplied bar textures (FoodStaminaProvider, CA, etc.). */
    private static final Map<String, Identifier> BAR_TEXTURE_CACHE = new ConcurrentHashMap<>();

    @Override protected Identifier backgroundTexture() { return BACKGROUND_ID; }
    @Override protected Identifier foregroundTexture() { return FOREGROUND_ID; }
    @Override protected AnimationMetadata.AnimationData barAnimation() { return AnimationMetadataCache.getStaminaBarAnimation(); }
    @Override protected AnimationMetadata.ScalingInfo backgroundScaling() { return AnimationMetadataCache.getStaminaBackgroundScaling(); }
    @Override protected AnimationMetadata.ScalingInfo foregroundScaling() { return AnimationMetadataCache.getStaminaForegroundScaling(); }

    @Override
    protected float currentValue(Player player) {
        LivingEntity mount = currentMount(player);
        if (mount != null) return mount.getHealth();
        StaminaProvider provider = StaminaProviderManager.getCurrentProvider();
        return provider == null ? 0f : provider.getCurrentStamina(player);
    }

    @Override
    protected float maxValue(Player player) {
        LivingEntity mount = currentMount(player);
        if (mount != null) return mount.getMaxHealth();
        StaminaProvider provider = StaminaProviderManager.getCurrentProvider();
        return provider == null ? 0f : provider.getMaxStamina(player);
    }

    @Override
    protected Identifier barTexture(Player player, float current, float max) {
        LivingEntity mount = currentMount(player);
        if (mount != null) {
            float ratio = max <= 0 ? 0 : current / max;
            return ratio <= 0.2f ? BAR_CRITICAL : BAR_MOUNTED;
        }
        StaminaProvider provider = StaminaProviderManager.getCurrentProvider();
        if (provider == null) return BAR_DEFAULT;
        return BAR_TEXTURE_CACHE.computeIfAbsent(
                provider.getBarTexture(player, current),
                name -> Constants.loc("textures/gui/" + name + ".png"));
    }

    /** While mounted, stamina mirrors the health bar's visibility rule (since it shows mount HP). */
    @Override
    protected boolean shouldFadeWhenFull(Player player, float current, float max) {
        ClientConfig cfg = ModConfigManager.getClient();
        boolean mounted = currentMount(player) != null;
        dev.muon.dynamic_resource_bars.util.BarVisibility v =
                mounted ? cfg.healthBarVisibility : cfg.staminaBarVisibility;
        return switch (v) {
            case ALWAYS -> false;
            case NEVER -> true;
            // Keep the bar visible while extra stamina is draining (Paragliders' overshield),
            // even if the base pool is full — same idea as health staying up during absorption.
            case SMART_FADE -> current >= max && (mounted || extraStamina(player) <= 0f);
        };
    }

    /**
     * Extra stamina (Paragliders' BotW-style overshield) inflates the denominator so the live
     * fill becomes {@code current / (max + extra)}, leaving room for the extra-stamina slice
     * painted in {@link #renderBetweenBarAndForeground}. Mount mode is unaffected — the bar is
     * sourcing mount HP, not stamina, in that case.
     */
    @Override
    protected float effectiveBarMax(Player player, float current, float max) {
        if (currentMount(player) != null) return max;
        float extra = extraStamina(player);
        return extra > 0f ? max + extra : max;
    }

    /**
     * Pushes the live base fill past the extra-stamina reserve so extra ends up at the leading
     * edge of the bar (drained <i>last</i>, after base) instead of the trailing edge (which is
     * how absorption renders, drained <i>first</i>). Without this offset the squeeze paint would
     * sit between the live fill and the empty zone, which is the wrong polarity for a reserve.
     *
     * <p>Derived from the combined boundary ({@code current + extra}) rather than {@code extra}
     * alone so that {@code offset + filled} (where {@code filled} is the {@code (int)} cast in
     * {@link #renderBaseBar}) sums to exactly the combined boundary. Independent floor()s of
     * each ratio would lose up to 1px to truncation, leaving a transparent seam at the trailing
     * edge of the live fill when both pools are full.
     */
    @Override
    protected int leadingFillOffset(Player player, float current, float max, ScreenRect barRect) {
        if (currentMount(player) != null) return 0;
        float extra = extraStamina(player);
        if (extra <= 0f) return 0;
        float denom = max + extra;
        if (denom <= 0f) return 0;
        BarConfig cfg = config();
        int barLength = cfg.fillDirection() == FillDirection.VERTICAL ? barRect.height() : barRect.width();
        int combined = (int) (barLength * (current + extra) / denom);
        int filled = (int) (barLength * current / denom);
        return combined - filled;
    }

    private static float extraStamina(Player player) {
        StaminaProvider provider = StaminaProviderManager.getCurrentProvider();
        return provider == null ? 0f : provider.getExtraStamina(player);
    }

    /** When riding a mount and {@code mergeMountHealth} is enabled, the stamina bar shows mount HP. */
    private static LivingEntity currentMount(Player player) {
        ClientConfig cfg = ModConfigManager.getClient();
        if (!cfg.mergeMountHealth || !cfg.enableMountHealth) return null;
        if (player.getVehicle() instanceof LivingEntity living) return living;
        return null;
    }

    /**
     * AppleSkin saturation overlay — only meaningful when our stamina is sourced from vanilla food.
     * Saturation is the buffer Minecraft drains before food itself, so it sits behind the food fill;
     * rendered here as a darker overlay scaled to saturation. The held-food preview is drawn earlier
     * via {@link #renderBetweenBarAndForeground} so it appears under this overlay.
     */
    @Override
    protected void renderBarOverlays(GuiGraphicsExtractor graphics, Player player,
                                     float current, float max, ScreenRect barRect, float alpha) {
        if (currentMount(player) != null) return; // Mount-health mode hides food-related overlays.
        if (ModConfigManager.getClient().staminaBarBehavior != StaminaBarBehavior.FOOD) return;
        if (!Services.PLATFORM.isModLoaded(AppleSkinCompat.MOD_ID)) return;

        float saturation = player.getFoodData().getSaturationLevel();
        if (saturation <= 0 || max <= 0) return;
        float satRatio = Math.max(0f, Math.min(1f, saturation / max));
        int satWidth = (int) (barRect.width() * satRatio);
        if (satWidth <= 0) return;
        AnimationMetadata.TextureDimensions dims = AnimationMetadataCache.getTextureDimensions(SATURATION_OVERLAY);
        NineSliceRenderer.renderWithScaling(graphics, SATURATION_OVERLAY,
                AnimationMetadataCache.getSaturationOverlayScaling(),
                barRect.x(), barRect.y(), satWidth, barRect.height(),
                dims.width, dims.height,
                RenderUtil.whiteWithAlpha(alpha));
    }

    /**
     * Three passes between the live fill and the foreground frame:
     * <ol>
     *   <li>{@link #renderSqueezeExtraStamina} — overshield slice painted in the space freed up
     *       by the squeezed denominator (Paragliders' extra stamina).</li>
     *   <li>{@link #renderReserveFadingChunks} — fade-out ghosts of recently drained reserve.</li>
     *   <li>AppleSkin held-food preview chunk in the would-restore region (FOOD source only).</li>
     * </ol>
     *
     * <p>Reserve tracking ({@link #updateReserveTracking}) runs before both, so drains that happen
     * in the same frame as a render produce a chunk this frame.
     */
    @Override
    protected void renderBetweenBarAndForeground(GuiGraphicsExtractor graphics, Player player,
                                                 float current, float max, ScreenRect barRect,
                                                 int animOffset, AnimationMetadata.AnimationData animData) {
        // Track unconditionally so previousExtra stays in sync even during mount mode (otherwise a
        // multi-second mount would show a giant chunk on dismount).
        updateReserveTracking(player);

        if (currentMount(player) != null) return;

        renderSqueezeExtraStamina(graphics, player, current, max, barRect);
        renderReserveFadingChunks(barRect, graphics);

        if (ModConfigManager.getClient().staminaBarBehavior != StaminaBarBehavior.FOOD) return;
        if (!Services.PLATFORM.isModLoaded(AppleSkinCompat.MOD_ID)) return;
        ItemStack held = AppleSkinCompat.pickHeldFood(player);
        if (held.isEmpty()) return;
        int restore = AppleSkinCompat.getFoodNutrition(player, held);
        renderRestorePreviewChunk(graphics, player, current, max, restore, barRect, animOffset, animData);
    }

    /**
     * Spawns a {@link FadingChunk} on the reserve list whenever extra stamina drops between frames.
     * Parallels {@code AbstractBarRenderer.updateChunkTracking} but for the reserve pool, since
     * extra has its own value and denominator independent of {@code currentValue}.
     */
    private void updateReserveTracking(Player player) {
        StaminaProvider provider = StaminaProviderManager.getCurrentProvider();
        float extra = provider == null ? 0f : provider.getExtraStamina(player);

        Iterator<FadingChunk> it = reserveChunks.iterator();
        while (it.hasNext()) {
            FadingChunk chunk = it.next();
            if (chunk.isExpired() || extra >= chunk.endValue) it.remove();
        }

        if (previousExtra > 0f && extra < previousExtra) {
            if (reserveChunks.size() >= MAX_FADING_CHUNKS) reserveChunks.remove(0);
            float maxStamina = provider == null ? 0f : provider.getMaxStamina(player);
            int animOffset = AnimationMetadata.calculateAnimationOffset(
                    AnimationMetadataCache.getExtraStaminaBarAnimation(), currentTicks());
            // Snapshot the denominator at creation so the chunk doesn't drift if extra continues to
            // drain during the fade — same approach as base chunks.
            reserveChunks.add(new FadingChunk(extra, previousExtra, maxStamina,
                    maxStamina + previousExtra, EXTRA_STAMINA_BAR, animOffset));
        }

        previousExtra = extra;
    }

    /**
     * Paints fade-out ghosts of drained reserve in the reserve zone (zoneOffset = 0 — the reserve
     * itself <i>is</i> the leading-edge zone). Each chunk's value range maps via its snapshot
     * denominator, so chunks recede inward as the live reserve shrinks past them.
     */
    private void renderReserveFadingChunks(ScreenRect barRect, GuiGraphicsExtractor graphics) {
        BarConfig cfg = config();
        boolean rightAnchored = cfg.anchor().getSide() == HUDPositioning.AnchorSide.RIGHT;
        paintFadingChunks(graphics, reserveChunks, barRect, rightAnchored, 0,
                currentAlpha(), AnimationMetadataCache.getExtraStaminaBarAnimation(), cfg);
    }

    /**
     * Paints {@link #EXTRA_STAMINA_BAR} at the leading edge of the bar zone — to the left on
     * left-anchored bars, to the right on right-anchored bars, at the bottom on vertical fills.
     * The base fill is shifted past this region by {@link #leadingFillOffset}, so the layout is
     * {@code [extra reserve][base live][empty trailing]}. Reserve width matches {@code leadingFillOffset}
     * exactly so there's no seam between the reserve and the live base fill.
     *
     * <p>The whole zone is painted as a solid extra-stamina fill (no separate "current vs. max
     * extra" sub-portion) because Paragliders only exposes the current extra value — the max is
     * implicit and shifts as extra is gained or used.
     */
    private void renderSqueezeExtraStamina(GuiGraphicsExtractor graphics, Player player,
                                           float current, float max, ScreenRect barRect) {
        int extraSize = leadingFillOffset(player, current, max, barRect);
        if (extraSize <= 0) return;

        BarConfig cfg = config();
        AnimationMetadata.AnimationData animData = AnimationMetadataCache.getExtraStaminaBarAnimation();
        int extraAnimOffset = AnimationMetadata.calculateAnimationOffset(animData, currentTicks());
        int tint = RenderUtil.whiteWithAlpha(currentAlpha());

        if (cfg.fillDirection() == FillDirection.VERTICAL) {
            int y = barRect.y() + barRect.height() - extraSize;
            RenderUtil.blitWithBinding(graphics, EXTRA_STAMINA_BAR,
                    barRect.x(), y, 0, extraAnimOffset, barRect.width(), extraSize,
                    animData.textureWidth, animData.textureHeight, tint);
            return;
        }

        boolean rightAnchored = cfg.anchor().getSide() == HUDPositioning.AnchorSide.RIGHT;
        int x = rightAnchored
                ? barRect.x() + barRect.width() - extraSize
                : barRect.x();
        RenderUtil.blitWithBinding(graphics, EXTRA_STAMINA_BAR,
                x, barRect.y(), 0, extraAnimOffset, extraSize, barRect.height(),
                animData.textureWidth, animData.textureHeight, tint);
    }
}
