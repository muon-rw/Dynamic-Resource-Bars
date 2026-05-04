package dev.muon.dynamic_resource_bars.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.util.AbsorptionDisplayMode;
import dev.muon.dynamic_resource_bars.util.AnimationMetadata;
import dev.muon.dynamic_resource_bars.util.AnimationMetadataCache;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.EditModeManager;
import dev.muon.dynamic_resource_bars.util.FillDirection;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;
import dev.muon.dynamic_resource_bars.util.NineSliceRenderer;
import dev.muon.dynamic_resource_bars.util.RenderUtil;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import dev.muon.dynamic_resource_bars.util.TickHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import dev.muon.dynamic_resource_bars.compat.AppleSkinCompat;
import dev.muon.dynamic_resource_bars.compat.FarmersDelightCompat;
import dev.muon.dynamic_resource_bars.compat.ThermooCompat;
import net.minecraft.world.item.ItemStack;

public class HealthBarRenderer extends AbstractBarRenderer {

    public static final HealthBarRenderer INSTANCE = new HealthBarRenderer();

    private enum BarType {
        NORMAL("health_bar"),
        POISON("health_bar_poisoned"),
        WITHER("health_bar_withered"),
        FROZEN("health_bar_frozen"),
        SCORCHED("health_bar_scorched");

        private final Identifier id;

        BarType(String texture) { this.id = Constants.loc("textures/gui/" + texture + ".png"); }

        public Identifier loc() { return id; }

        public static BarType fromPlayerState(Player player) {
            if (player.hasEffect(MobEffects.POISON)) return POISON;
            if (player.hasEffect(MobEffects.WITHER)) return WITHER;
            if (isFrozen(player)) return FROZEN;
            if (isScorched(player)) return SCORCHED;
            return NORMAL;
        }
    }

    // Cached identifiers — these used to be allocated every frame inside render hooks.
    private static final Identifier BACKGROUND_ID = Constants.loc("textures/gui/health_background.png");
    private static final Identifier FOREGROUND_ID = Constants.loc("textures/gui/health_foreground.png");
    private static final Identifier HEAT_OVERLAY = Constants.loc("textures/gui/heat_overlay.png");
    private static final Identifier COLD_OVERLAY = Constants.loc("textures/gui/cold_overlay.png");
    private static final Identifier ABSORPTION_OVERLAY = Constants.loc("textures/gui/absorption_overlay.png");
    private static final Identifier ABSORPTION_BAR = Constants.loc("textures/gui/absorption_bar.png");
    private static final Identifier REGENERATION_OVERLAY = Constants.loc("textures/gui/regeneration_overlay.png");
    private static final Identifier HARDCORE_OVERLAY = Constants.loc("textures/gui/hardcore_overlay.png");
    private static final Identifier WETNESS_OVERLAY = Constants.loc("textures/gui/wetness_overlay.png");
    private static final Identifier COMFORT_OVERLAY = Constants.loc("textures/gui/comfort_overlay.png");

    @Override protected DraggableElement draggable() { return DraggableElement.HEALTH_BAR; }

    @Override
    protected BarConfig config() {
        ClientConfig c = ModConfigManager.getClient();
        return new BarConfig(
                c.healthBackgroundWidth, c.healthBackgroundHeight,
                c.healthBackgroundXOffset, c.healthBackgroundYOffset,
                c.healthBarWidth, c.healthBarHeight, c.healthBarXOffset, c.healthBarYOffset,
                c.healthOverlayWidth, c.healthOverlayHeight, c.healthOverlayXOffset, c.healthOverlayYOffset,
                c.healthTextXOffset, c.healthTextYOffset, c.healthTextWidth, c.healthTextHeight,
                c.healthTextColor, c.healthTextOpacity, c.healthTextAlign,
                c.healthTotalXOffset, c.healthTotalYOffset,
                c.healthBarAnchor,
                c.enableHealthBackground, c.enableHealthForeground, c.healthBarVisibility,
                c.healthFillDirection, c.showHealthText
        );
    }

    @Override protected Identifier backgroundTexture() { return BACKGROUND_ID; }
    @Override protected Identifier foregroundTexture() { return FOREGROUND_ID; }
    @Override protected AnimationMetadata.AnimationData barAnimation() { return AnimationMetadataCache.getHealthBarAnimation(); }
    @Override protected AnimationMetadata.ScalingInfo backgroundScaling() { return AnimationMetadataCache.getHealthBackgroundScaling(); }
    @Override protected AnimationMetadata.ScalingInfo foregroundScaling() { return AnimationMetadataCache.getHealthForegroundScaling(); }

    @Override protected Identifier barTexture(Player player, float current, float max) {
        return BarType.fromPlayerState(player).loc();
    }

    @Override protected float currentValue(Player player) { return player.getHealth(); }
    @Override protected float maxValue(Player player) { return player.getMaxHealth(); }

    /** Smart fade keeps the bar visible while absorption is present even when health is full. */
    @Override
    protected boolean smartFadeTrigger(Player player, float current, float max) {
        return current >= max && player.getAbsorptionAmount() == 0f;
    }

    /**
     * In SQUEEZE mode the absorption pool joins the denominator, so the health fill width becomes
     * {@code current / (max + abs)} and the freed-up space gets filled by {@link #ABSORPTION_BAR}.
     * OVERLAY mode (default) leaves {@code max} alone — absorption surfaces only as the pulsing
     * overlay layered on top.
     */
    @Override
    protected float effectiveBarMax(Player player, float current, float max) {
        if (ModConfigManager.getClient().healthAbsorptionDisplayMode == AbsorptionDisplayMode.SQUEEZE) {
            float abs = player.getAbsorptionAmount();
            if (abs > 0) return max + abs;
        }
        return max;
    }

    @Override
    protected void renderBarOverlays(GuiGraphicsExtractor graphics, Player player,
                                     float current, float max, ScreenRect barRect, float alpha) {
        int tint = RenderUtil.whiteWithAlpha(alpha);

        float tempScale = getTemperatureScale(player);
        if (tempScale != 0) {
            Identifier tex = tempScale > 0 ? HEAT_OVERLAY : COLD_OVERLAY;
            int width = (int) (barRect.width() * Math.abs(tempScale));
            AnimationMetadata.ScalingInfo scaling = tempScale > 0
                    ? AnimationMetadataCache.getHeatOverlayScaling()
                    : AnimationMetadataCache.getColdOverlayScaling();
            AnimationMetadata.TextureDimensions dims = AnimationMetadataCache.getTextureDimensions(tex);
            NineSliceRenderer.renderWithScaling(graphics, tex, scaling,
                    barRect.x(), barRect.y(), width, barRect.height(),
                    dims.width, dims.height, tint);
        }

        // Pulse overlay is OVERLAY-mode only — in SQUEEZE the bar's absorption slice carries
        // the visual signal, so doubling it with the pulse adds noise.
        float absorption = player.getAbsorptionAmount();
        if (absorption > 0
                && ModConfigManager.getClient().healthAbsorptionDisplayMode == AbsorptionDisplayMode.OVERLAY) {
            float pulseAlpha = 0.5f + (TickHandler.getOverlayFlashAlpha() * 0.5f);
            AnimationMetadata.TextureDimensions dims = AnimationMetadataCache.getTextureDimensions(ABSORPTION_OVERLAY);
            NineSliceRenderer.renderWithScaling(graphics, ABSORPTION_OVERLAY,
                    AnimationMetadataCache.getAbsorptionOverlayScaling(),
                    barRect.x(), barRect.y(), barRect.width(), barRect.height(),
                    dims.width, dims.height,
                    RenderUtil.whiteWithAlpha(pulseAlpha * alpha));
        }

        if (player.hasEffect(MobEffects.REGENERATION)) {
            float pulse = TickHandler.getOverlayFlashAlpha();
            AnimationMetadata.TextureDimensions dims = AnimationMetadataCache.getTextureDimensions(REGENERATION_OVERLAY);
            NineSliceRenderer.renderWithScaling(graphics, REGENERATION_OVERLAY,
                    AnimationMetadataCache.getRegenerationOverlayScaling(),
                    barRect.x(), barRect.y(), barRect.width(), barRect.height(),
                    dims.width, dims.height,
                    RenderUtil.whiteWithAlpha(pulse * alpha));
        }
    }

    /**
     * Drawn after the live health fill but before any overlays/foreground:
     * <ol>
     *   <li>The SQUEEZE-mode absorption slice (so it sits behind the foreground frame and any
     *       regen/temperature overlays, just like the health fill).</li>
     *   <li>The AppleSkin held-food preview chunk shown in the would-restore region.</li>
     * </ol>
     */
    @Override
    protected void renderBetweenBarAndForeground(GuiGraphicsExtractor graphics, Player player,
                                                 float current, float max, ScreenRect barRect,
                                                 int animOffset, AnimationMetadata.AnimationData animData) {
        // Resolve the food preview width up-front so SQUEEZE absorption can slide right by exactly
        // that much, leaving a clean gap for the preview chunk between the live health fill and
        // where absorption would sit post-consumption.
        int previewShift = 0;
        ItemStack heldFood = ItemStack.EMPTY;
        float restoreAmount = 0f;
        if (Services.PLATFORM.isModLoaded(AppleSkinCompat.MOD_ID)) {
            heldFood = AppleSkinCompat.pickHeldFood(player);
            if (!heldFood.isEmpty()) {
                restoreAmount = AppleSkinCompat.getEstimatedHealthRestoration(player, heldFood);
                previewShift = restorePreviewWidth(player, current, max, restoreAmount, barRect);
            }
        }

        renderSqueezeAbsorption(graphics, player, current, max, barRect, previewShift);

        if (!heldFood.isEmpty()) {
            renderRestorePreviewChunk(graphics, player, current, max, restoreAmount, barRect, animOffset, animData);
        }
    }

    /**
     * Paints {@link #ABSORPTION_BAR} immediately adjacent to the live health fill — to the right
     * of it on left-anchored bars, to the left on right-anchored bars, above it on vertical fills.
     * Width is proportional to {@code abs / (max + abs)} of the bar zone, mirroring the health
     * portion's {@code current / (max + abs)}. Uses its own animation strip (independent .mcmeta).
     *
     * <p>{@code previewShift} pushes the absorption slice further away from the health fill so a
     * food-restore preview can occupy the freed-up gap (horizontal bars only — vertical fills
     * always pass 0).
     */
    private void renderSqueezeAbsorption(GuiGraphicsExtractor graphics, Player player,
                                         float current, float max, ScreenRect barRect, int previewShift) {
        if (ModConfigManager.getClient().healthAbsorptionDisplayMode != AbsorptionDisplayMode.SQUEEZE) return;
        float absorption = player.getAbsorptionAmount();
        if (absorption <= 0f) return;
        float denom = max + absorption;
        if (denom <= 0f) return;

        BarConfig cfg = config();
        AnimationMetadata.AnimationData animData = AnimationMetadataCache.getAbsorptionBarAnimation();
        int absAnimOffset = AnimationMetadata.calculateAnimationOffset(animData, currentTicks());
        int tint = RenderUtil.whiteWithAlpha(currentAlpha());

        if (cfg.fillDirection() == FillDirection.VERTICAL) {
            int healthFilled = (int) (barRect.height() * (current / denom));
            // Derive absFilled from the combined boundary so health + abs always sum to the same
            // pixel count — independent (int)-truncation would leave a 1px transparent seam.
            int combinedFilled = (int) (barRect.height() * ((current + absorption) / denom));
            int absFilled = combinedFilled - healthFilled;
            if (absFilled <= 0) return;
            int y = barRect.y() + (barRect.height() - combinedFilled);
            RenderUtil.blitWithBinding(graphics, ABSORPTION_BAR,
                    barRect.x(), y, 0, absAnimOffset, barRect.width(), absFilled,
                    animData.textureWidth, animData.textureHeight, tint);
            return;
        }

        int healthFilled = (int) (barRect.width() * (current / denom));
        int combinedFilled = (int) (barRect.width() * ((current + absorption) / denom));
        int absFilled = combinedFilled - healthFilled;
        if (absFilled <= 0) return;
        boolean rightAnchored = cfg.anchor().getSide() == HUDPositioning.AnchorSide.RIGHT;
        int x = rightAnchored
                ? barRect.x() + barRect.width() - healthFilled - previewShift - absFilled
                : barRect.x() + healthFilled + previewShift;
        RenderUtil.blitWithBinding(graphics, ABSORPTION_BAR,
                x, barRect.y(), 0, absAnimOffset, absFilled, barRect.height(),
                animData.textureWidth, animData.textureHeight, tint);
    }

    @Override
    protected void renderBackgroundOverlays(GuiGraphicsExtractor graphics, Player player,
                                            ScreenRect complexRect, float alpha) {
        int tint = RenderUtil.whiteWithAlpha(alpha);
        if (player.level().getLevelData().isHardcore()) {
            AnimationMetadata.TextureDimensions dims = AnimationMetadataCache.getTextureDimensions(HARDCORE_OVERLAY);
            NineSliceRenderer.renderWithScaling(graphics, HARDCORE_OVERLAY,
                    AnimationMetadataCache.getHardcoreOverlayScaling(),
                    complexRect.x(), complexRect.y(), complexRect.width(), complexRect.height(),
                    dims.width, dims.height, tint);
        }
        float wet = getWetnessScale(player);
        if (wet > 0) {
            AnimationMetadata.TextureDimensions dims = AnimationMetadataCache.getTextureDimensions(WETNESS_OVERLAY);
            NineSliceRenderer.renderWithScaling(graphics, WETNESS_OVERLAY,
                    AnimationMetadataCache.getWetnessOverlayScaling(),
                    complexRect.x(), complexRect.y(), complexRect.width(), complexRect.height(),
                    dims.width, dims.height,
                    RenderUtil.whiteWithAlpha(wet * alpha));
        }
        // Farmer's Delight comfort effect — soft glow on the entire bar background.
        if (Services.PLATFORM.isModLoaded(FarmersDelightCompat.MOD_ID) && FarmersDelightCompat.hasComfort(player)) {
            AnimationMetadata.TextureDimensions dims = AnimationMetadataCache.getTextureDimensions(COMFORT_OVERLAY);
            NineSliceRenderer.renderWithScaling(graphics, COMFORT_OVERLAY,
                    AnimationMetadataCache.getComfortOverlayScaling(),
                    complexRect.x(), complexRect.y(), complexRect.width(), complexRect.height(),
                    dims.width, dims.height, tint);
        }
    }

    @Override
    protected void renderAuxiliaryText(GuiGraphicsExtractor graphics, Player player, float alpha) {
        ClientConfig c = ModConfigManager.getClient();
        if (!c.enableHealthAbsorptionText) return;
        float absorption = player.getAbsorptionAmount();
        if (absorption <= 0 && !EditModeManager.isEditModeEnabled()) return;
        String text = "+" + (EditModeManager.isEditModeEnabled() && absorption == 0 ? "8" : (int) absorption);
        ScreenRect rect = getSubElementRect(SubElementType.ABSORPTION_TEXT, player);
        HorizontalAlignment alignment = c.healthAbsorptionTextAlign;
        int baseX = switch (alignment) {
            case CENTER -> rect.x() + rect.width() / 2;
            case RIGHT -> rect.x() + rect.width();
            case LEFT -> rect.x();
        };
        int y = rect.y() + (rect.height() / 2);
        int baseColor = c.healthTextColor & 0xFFFFFF;
        int textAlpha = (int) (c.healthTextOpacity * alpha);
        textAlpha = Math.max(10, Math.min(255, textAlpha));
        int color = (textAlpha << 24) | baseColor;
        RenderUtil.renderAdditionText(text, graphics, baseX, y, c.healthAbsorptionTextHeight, color, alignment);
    }

    @Override
    protected ScreenRect getCustomSubElementRect(SubElementType type, Player player, ScreenRect complexRect) {
        if (type == SubElementType.ABSORPTION_TEXT) {
            ClientConfig c = ModConfigManager.getClient();
            return new ScreenRect(
                    complexRect.x() + c.healthAbsorptionTextXOffset,
                    complexRect.y() + c.healthAbsorptionTextYOffset,
                    c.healthAbsorptionTextWidth,
                    c.healthAbsorptionTextHeight);
        }
        return super.getCustomSubElementRect(type, player, complexRect);
    }

    // ===== Thermoo (third-party) compat — direct interface dispatch behind a mod-loaded guard =====
    //
    // Thermoo injects TemperatureAware + Soakable onto every LivingEntity. We never reference those
    // interfaces from this class — the cast happens inside ThermooCompat, which the JVM only loads
    // when one of these helpers is first called. The isModLoaded check ensures we never trigger that
    // load when Thermoo is absent, so the dependency stays optional.

    private static float getTemperatureScale(Player player) {
        if (!Services.PLATFORM.isModLoaded(ThermooCompat.MOD_ID)) return 0f;
        return ThermooCompat.getTemperatureScale(player);
    }

    private static float getWetnessScale(Player player) {
        if (!Services.PLATFORM.isModLoaded(ThermooCompat.MOD_ID)) return 0f;
        return ThermooCompat.getSoakedScale(player);
    }

    private static boolean isScorched(Player player) {
        if (!Services.PLATFORM.isModLoaded(ThermooCompat.MOD_ID)) return false;
        return ThermooCompat.isScorched(player);
    }

    private static boolean isFrozen(Player player) {
        if (player.isFullyFrozen()) return true;
        if (!Services.PLATFORM.isModLoaded(ThermooCompat.MOD_ID)) return false;
        return ThermooCompat.isFrozen(player);
    }
}
