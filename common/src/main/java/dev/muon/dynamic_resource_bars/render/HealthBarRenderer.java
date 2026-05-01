package dev.muon.dynamic_resource_bars.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.util.AnimationMetadata;
import dev.muon.dynamic_resource_bars.util.AnimationMetadataCache;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.EditModeManager;
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
                c.healthTextXOffset, c.healthTextYOffset, c.healthTextColor, c.healthTextOpacity, c.healthTextAlign,
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

        float absorption = player.getAbsorptionAmount();
        if (absorption > 0) {
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

    /** AppleSkin held-food preview: a flashing chunk of the bar fill shown in the would-restore region. */
    @Override
    protected void renderBetweenBarAndForeground(GuiGraphicsExtractor graphics, Player player,
                                                 float current, float max, ScreenRect barRect,
                                                 int animOffset, AnimationMetadata.AnimationData animData) {
        if (!Services.PLATFORM.isModLoaded(AppleSkinCompat.MOD_ID)) return;
        ItemStack held = AppleSkinCompat.pickHeldFood(player);
        if (held.isEmpty()) return;
        float est = AppleSkinCompat.getEstimatedHealthRestoration(player, held);
        renderRestorePreviewChunk(graphics, player, current, max, est, barRect, animOffset, animData);
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
        float absorption = player.getAbsorptionAmount();
        if (absorption <= 0 && !EditModeManager.isEditModeEnabled()) return;
        String text = "+" + (EditModeManager.isEditModeEnabled() && absorption == 0 ? "8" : (int) absorption);
        ScreenRect rect = getSubElementRect(SubElementType.ABSORPTION_TEXT, player);
        int x = rect.x();
        int y = rect.y() + (rect.height() / 2);
        ClientConfig c = ModConfigManager.getClient();
        int baseColor = c.healthTextColor & 0xFFFFFF;
        int textAlpha = (int) (c.healthTextOpacity * alpha);
        textAlpha = Math.max(10, Math.min(255, textAlpha));
        int color = (textAlpha << 24) | baseColor;
        RenderUtil.renderAdditionText(text, graphics, x, y, color);
    }

    @Override
    protected ScreenRect getCustomSubElementRect(SubElementType type, Player player, ScreenRect complexRect) {
        if (type == SubElementType.ABSORPTION_TEXT) {
            ClientConfig c = ModConfigManager.getClient();
            return new ScreenRect(
                    complexRect.x() + c.healthAbsorptionTextXOffset,
                    complexRect.y() + c.healthAbsorptionTextYOffset,
                    50,
                    c.healthBarHeight);
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
