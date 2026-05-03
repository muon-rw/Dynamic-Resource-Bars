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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StaminaBarRenderer extends AbstractBarRenderer {

    public static final StaminaBarRenderer INSTANCE = new StaminaBarRenderer();

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
        dev.muon.dynamic_resource_bars.util.BarVisibility v =
                currentMount(player) != null ? cfg.healthBarVisibility : cfg.staminaBarVisibility;
        return switch (v) {
            case ALWAYS -> false;
            case NEVER -> true;
            case SMART_FADE -> current >= max;
        };
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

    /** AppleSkin held-food preview: a flashing chunk of the bar fill in the would-restore region. */
    @Override
    protected void renderBetweenBarAndForeground(GuiGraphicsExtractor graphics, Player player,
                                                 float current, float max, ScreenRect barRect,
                                                 int animOffset, AnimationMetadata.AnimationData animData) {
        if (currentMount(player) != null) return;
        if (ModConfigManager.getClient().staminaBarBehavior != StaminaBarBehavior.FOOD) return;
        if (!Services.PLATFORM.isModLoaded(AppleSkinCompat.MOD_ID)) return;
        ItemStack held = AppleSkinCompat.pickHeldFood(player);
        if (held.isEmpty()) return;
        int restore = AppleSkinCompat.getFoodNutrition(player, held);
        renderRestorePreviewChunk(graphics, player, current, max, restore, barRect, animOffset, animData);
    }
}
