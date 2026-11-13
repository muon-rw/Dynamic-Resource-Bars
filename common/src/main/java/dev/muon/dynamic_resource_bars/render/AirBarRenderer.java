package dev.muon.dynamic_resource_bars.render;

import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.compat.LsoCompat;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.AnimationMetadata;
import dev.muon.dynamic_resource_bars.util.AnimationMetadataCache;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.EditModeManager;
import dev.muon.dynamic_resource_bars.util.RenderUtil;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class AirBarRenderer extends AbstractBarRenderer {

    public static final AirBarRenderer INSTANCE = new AirBarRenderer();

    /** Bubble icon for the current air-supply tier. The {@code _pop} variants flash briefly when crossing into a new tier. */
    private enum AirIcon {
        NONE("air_0"),
        LOW("air_1"), LOW_POP("air_1_pop"),
        MEDIUM("air_2"), MEDIUM_POP("air_2_pop"),
        HIGH("air_3"), HIGH_POP("air_3_pop"),
        FULL("air_4"), FULL_POP("air_4_pop");

        private static final float POP_RATIO = 0.1f;
        private static final float LOW_UPPER = 0.25f;
        private static final float MEDIUM_UPPER = 0.50f;
        private static final float HIGH_UPPER = 0.75f;

        private final String texture;

        AirIcon(String texture) { this.texture = texture; }

        public ResourceLocation loc() { return Constants.loc("textures/gui/air/" + texture + ".png"); }

        static AirIcon forValue(int air, int max) {
            if (air <= 0 || max <= 0) return NONE;
            float pct = (float) air / max;
            if (pct > HIGH_UPPER) {
                return pct <= HIGH_UPPER + (1f - HIGH_UPPER) * POP_RATIO ? FULL_POP : FULL;
            }
            if (pct > MEDIUM_UPPER) {
                return pct <= MEDIUM_UPPER + (HIGH_UPPER - MEDIUM_UPPER) * POP_RATIO ? HIGH_POP : HIGH;
            }
            if (pct > LOW_UPPER) {
                return pct <= LOW_UPPER + (MEDIUM_UPPER - LOW_UPPER) * POP_RATIO ? MEDIUM_POP : MEDIUM;
            }
            return pct <= LOW_UPPER * POP_RATIO ? LOW_POP : LOW;
        }
    }

    @Override protected DraggableElement draggable() { return DraggableElement.AIR_BAR; }
    @Override protected int editModeBarOutlineColor() { return 0xA0ADD8E6; }

    @Override
    protected BarConfig config() {
        ClientConfig c = ModConfigManager.getClient();
        return new BarConfig(
                c.airBackgroundWidth, c.airBackgroundHeight,
                c.airBackgroundXOffset, c.airBackgroundYOffset,
                c.airBarWidth, c.airBarHeight, c.airBarXOffset, c.airBarYOffset,
                c.airOverlayWidth, c.airOverlayHeight, c.airOverlayXOffset, c.airOverlayYOffset,
                c.airTextXOffset, c.airTextYOffset, c.airTextWidth, c.airTextHeight, c.airTextScale,
                c.airTextColor, c.airTextOpacity, c.airTextAlign,
                c.airTotalXOffset, c.airTotalYOffset,
                c.airBarAnchor,
                c.enableAirBackground, c.enableAirForeground, c.airBarVisibility,
                c.airFillDirection, c.showAirText
        );
    }

    private static final float CRITICAL_RATIO = 0.2f;

    private static final ResourceLocation BACKGROUND_ID = Constants.loc("textures/gui/air_background.png");
    private static final ResourceLocation FOREGROUND_ID = Constants.loc("textures/gui/air_foreground.png");
    private static final ResourceLocation AIR_BAR = Constants.loc("textures/gui/air_bar.png");
    private static final ResourceLocation AIR_BAR_CRITICAL = Constants.loc("textures/gui/air_bar_critical.png");
    private static final ResourceLocation THIRST_BAR = Constants.loc("textures/gui/thirst_bar.png");
    private static final ResourceLocation THIRST_BAR_THIRSTY = Constants.loc("textures/gui/thirst_bar_thirsty.png");
    private static final ResourceLocation THIRST_BAR_HEAT = Constants.loc("textures/gui/thirst_bar_heat.png");
    private static final ResourceLocation THIRST_BAR_CRITICAL = Constants.loc("textures/gui/thirst_bar_critical.png");

    @Override protected ResourceLocation backgroundTexture() { return BACKGROUND_ID; }
    @Override protected ResourceLocation foregroundTexture() { return FOREGROUND_ID; }

    @Override
    protected ResourceLocation barTexture(Player player, float current, float max) {
        float ratio = max <= 0 ? 0f : current / max;
        if (isShowingThirst(player)) {
            if (LsoCompat.hasThirstEffect(player)) return THIRST_BAR_THIRSTY;
            if (LsoCompat.hasHeatThirstEffect(player)) return THIRST_BAR_HEAT;
            if (ratio <= CRITICAL_RATIO) return THIRST_BAR_CRITICAL;
            return THIRST_BAR;
        }
        return ratio <= CRITICAL_RATIO ? AIR_BAR_CRITICAL : AIR_BAR;
    }
    @Override protected AnimationMetadata.AnimationData barAnimation() { return AnimationMetadataCache.getAirBarAnimation(); }
    @Override protected AnimationMetadata.ScalingInfo backgroundScaling() { return AnimationMetadataCache.getAirBackgroundScaling(); }
    @Override protected AnimationMetadata.ScalingInfo foregroundScaling() { return AnimationMetadataCache.getAirForegroundScaling(); }

    private static boolean isShowingThirst(Player player) {
        return LsoCompat.isThirstActive(player) && player.getAirSupply() >= player.getMaxAirSupply();
    }

    @Override
    protected float currentValue(Player player) {
        return isShowingThirst(player) ? LsoCompat.getCurrentThirst(player) : player.getAirSupply();
    }

    @Override
    protected float maxValue(Player player) {
        return isShowingThirst(player) ? LsoCompat.getMaxThirst(player) : player.getMaxAirSupply();
    }

    /** Air hides when full unless underwater; thirst hides when full (when smart fade is active). */
    @Override
    protected boolean smartFadeTrigger(Player player, float current, float max) {
        if (isShowingThirst(player)) {
            return current >= max;
        }
        return !player.isUnderWater() && current >= max;
    }

    /** Override the standard "behavior == VANILLA" gating: air uses CUSTOM/HIDDEN/VANILLA. */
    @Override
    protected boolean shouldRender(Player player) {
        if (!super.shouldRender(player)) return false;
        return ModConfigManager.getClient().airBarBehavior == dev.muon.dynamic_resource_bars.util.BarRenderBehavior.CUSTOM
                || EditModeManager.isEditModeEnabled();
    }

    /** Bubble icon, overlaid after the bar fill — separate texture per air tier. */
    @Override
    protected void renderBarOverlays(GuiGraphics graphics, Player player,
                                     float current, float max, ScreenRect barRect, float alpha) {
        ClientConfig c = ModConfigManager.getClient();
        if (!c.enableAirIcon && !EditModeManager.isEditModeEnabled()) return;
        if (isShowingThirst(player) && !EditModeManager.isEditModeEnabled()) return;
        int displayAir = (EditModeManager.isEditModeEnabled() && current >= max) ? (int) (max / 2f) : (int) current;
        AirIcon icon = AirIcon.forValue(displayAir, (int) max);
        ScreenRect rect = getSubElementRect(SubElementType.ICON, player);
        if (rect.width() <= 0 || rect.height() <= 0) return;
        ResourceLocation texture = icon.loc();
        int color = RenderUtil.whiteWithAlpha(alpha);
        blitTinted(graphics, texture, rect.x(), rect.y(), 0, 0,
                rect.width(), rect.height(), rect.width(), rect.height(), color);
    }

    @Override
    protected ScreenRect getCustomSubElementRect(SubElementType type, Player player, ScreenRect complexRect) {
        if (type == SubElementType.ICON) {
            ClientConfig c = ModConfigManager.getClient();
            return new ScreenRect(
                    complexRect.x() + c.airIconXOffset,
                    complexRect.y() + c.airIconYOffset,
                    c.airIconWidth, c.airIconHeight);
        }
        return super.getCustomSubElementRect(type, player, complexRect);
    }
}
