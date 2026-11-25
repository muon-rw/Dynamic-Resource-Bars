package dev.muon.dynamic_resource_bars.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

#if NEWER_THAN_20_1
import net.minecraft.client.DeltaTracker;
#endif

public class AirBarRenderer {
    private static long airTextStartTime = 0;
    private static boolean shouldShowAirText = false;

    private static boolean airBarSetVisible = true;
    private static long airBarDisabledStartTime = 0L;
    private static final OrbRenderHelper.FillSmoother fillSmoother = new OrbRenderHelper.FillSmoother();

    private static final ResourceLocation AIR_BG_TEX = DynamicResourceBars.loc("textures/gui/air_background.png");
    private static final ResourceLocation AIR_FG_TEX = DynamicResourceBars.loc("textures/gui/air_foreground.png");
    private static final ResourceLocation AIR_BAR_TEX = DynamicResourceBars.loc("textures/gui/air_bar.png");
    private static final ResourceLocation AIR_BAR_CRIT_TEX = DynamicResourceBars.loc("textures/gui/air_bar_critical.png");

    private enum AirIcon {
        NONE("air_0"),
        LOW("air_1"),
        LOW_POP("air_1_pop"),
        MEDIUM("air_2"),
        MEDIUM_POP("air_2_pop"),
        HIGH("air_3"),
        HIGH_POP("air_3_pop"),
        FULL("air_4"),
        FULL_POP("air_4_pop");

        private final String texture;

        AirIcon(String texture) {
            this.texture = texture;
        }

        public String getTexture() {
            return texture;
        }

        public static AirIcon fromAirValue(int airValue, int maxAir) {
            if (airValue <= 0 || maxAir <= 0) return NONE;
            float percent = (float) airValue / maxAir;
            final float POP_RANGE_RATIO = 0.1f;
            final float LOW_UPPER_BOUND = 0.25f;
            final float MEDIUM_UPPER_BOUND = 0.5f;
            final float HIGH_UPPER_BOUND = 0.75f;

            if (percent > HIGH_UPPER_BOUND) {
                return percent <= HIGH_UPPER_BOUND + (1.0f - HIGH_UPPER_BOUND) * POP_RANGE_RATIO ? FULL_POP : FULL;
            } else if (percent > MEDIUM_UPPER_BOUND) {
                return percent <= MEDIUM_UPPER_BOUND + (HIGH_UPPER_BOUND - MEDIUM_UPPER_BOUND) * POP_RANGE_RATIO ? HIGH_POP : HIGH;
            } else if (percent > LOW_UPPER_BOUND) {
                return percent <= LOW_UPPER_BOUND + (MEDIUM_UPPER_BOUND - LOW_UPPER_BOUND) * POP_RANGE_RATIO ? MEDIUM_POP : MEDIUM;
            } else {
                return percent <= LOW_UPPER_BOUND * POP_RANGE_RATIO ? LOW_POP : LOW;
            }
        }
    }

    public static ScreenRect getScreenRect(Player player) {
        if (player == null) return new ScreenRect(0, 0, 0, 0);
        var config = ModConfigManager.getClient();
        Position anchorPos = HUDPositioning.getPositionFromAnchor(config.airBarAnchor);
        Position finalPos = anchorPos.offset(config.airTotalXOffset, config.airTotalYOffset);
        return new ScreenRect(finalPos.x(), finalPos.y(), config.airBackgroundWidth, config.airBackgroundHeight);
    }

    public static ScreenRect getSubElementRect(SubElementType type, Player player) {
        ScreenRect complexRect = getScreenRect(player);
        if (complexRect == null || (complexRect.width() == 0 && complexRect.height() == 0)) {
            return new ScreenRect(0, 0, 0, 0);
        }

        ClientConfig config = ModConfigManager.getClient();
        int x = complexRect.x();
        int y = complexRect.y();

        switch (type) {
            case BACKGROUND:
                return new ScreenRect(x + config.airBackgroundXOffset,
                        y + config.airBackgroundYOffset,
                        config.airBackgroundWidth,
                        config.airBackgroundHeight);
            case BAR_MAIN:
                return new ScreenRect(x + config.airBarXOffset,
                        y + config.airBarYOffset,
                        config.airBarWidth,
                        config.airBarHeight);
            case TEXT:
                return new ScreenRect(x + config.airTextXOffset,
                        y + config.airTextYOffset,
                        config.airBarWidth,
                        config.airBarHeight);
            case ICON:
                return new ScreenRect(x + config.airIconXOffset,
                        y + config.airIconYOffset,
                        config.airIconSize,
                        config.airIconSize);
            default:
                return new ScreenRect(0, 0, 0, 0);
        }
    }

    public static void render(GuiGraphics graphics, Player player, #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif) {
        ClientConfig config = ModConfigManager.getClient();

        int maxAir = player.getMaxAirSupply();
        int currentAir = player.getAirSupply();

        setAirBarVisibility(!config.fadeAirWhenFull || currentAir < maxAir || player.isUnderWater() || EditModeManager.isEditModeEnabled());

        if (!isAirBarVisible() && !EditModeManager.isEditModeEnabled() &&
                (System.currentTimeMillis() - airBarDisabledStartTime) > RenderUtil.BAR_FADEOUT_DURATION) {
            return;
        }

        float currentAlphaForRender = getAirBarAlpha();
        if (EditModeManager.isEditModeEnabled() && !isAirBarVisible()) {
            currentAlphaForRender = 1.0f;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);

        boolean isRightAnchored = config.airBarAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT;
        float partial = #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif;
        float ticks = player.tickCount + partial;

        ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
        OrbRenderHelper.blitFullTexture(graphics, AIR_BG_TEX, bgRect.x(), bgRect.y(), bgRect.width(), bgRect.height());

        float airPercent = (maxAir <= 0) ? 0.0f : Math.max(0.0f, Math.min(1.0f, (float) currentAir / maxAir));
        ResourceLocation barTex = (airPercent <= 0.25f && OrbRenderHelper.resourceExists(AIR_BAR_CRIT_TEX))
                ? AIR_BAR_CRIT_TEX : AIR_BAR_TEX;
        OrbRenderHelper.AnimationData animData = OrbRenderHelper.getAnimationData(barTex);
        int animOffset = OrbRenderHelper.calculateAnimationOffset(animData, ticks);

        ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
        boolean vertical = config.airFillDirection == FillDirection.VERTICAL;
        int span = vertical ? barRect.height() : barRect.width();
        int filled = Math.round(span * airPercent);
        if (currentAir > 0 && filled == 0) filled = 1;
        int liveEdge = fillSmoother.step(filled);

        if (liveEdge > 0) {
            OrbRenderHelper.paintFillSpan(graphics, barTex, barRect, vertical, isRightAnchored,
                    animOffset, animData, 0, liveEdge, false, true, ticks);
        }

        if (OrbRenderHelper.resourceExists(AIR_FG_TEX)) {
            OrbRenderHelper.blitFullTexture(graphics, AIR_FG_TEX, bgRect.x(), bgRect.y(), bgRect.width(), bgRect.height());
        }

        if (shouldRenderText() || EditModeManager.isEditModeEnabled()) {
            ScreenRect textRect = getSubElementRect(SubElementType.TEXT, player);
            int textX = textRect.x() + (textRect.width() / 2);
            int textY = textRect.y() + (textRect.height() / 2);
            int color = getTextColor();
            HorizontalAlignment alignment = config.airTextAlign;

            int baseX = textRect.x();
            if (alignment == HorizontalAlignment.CENTER) {
                baseX = textX;
            } else if (alignment == HorizontalAlignment.RIGHT) {
                baseX = textRect.x() + textRect.width();
            }

            RenderUtil.renderText(currentAir, maxAir, graphics, baseX, textY, color, alignment);
        }

        if (config.enableAirIcon || EditModeManager.isEditModeEnabled()) {
            int displayAir = EditModeManager.isEditModeEnabled() && currentAir >= maxAir ? maxAir / 2 : currentAir;
            AirIcon icon = AirIcon.fromAirValue(displayAir, maxAir);
            ScreenRect iconRect = getSubElementRect(SubElementType.ICON, player);
            OrbRenderHelper.blitFullTexture(graphics,
                    DynamicResourceBars.loc("textures/gui/air/" + icon.getTexture() + ".png"),
                    iconRect.x(), iconRect.y(), iconRect.width(), iconRect.height());
        }

        if (EditModeManager.isEditModeEnabled()) {
            ScreenRect complexRect = getScreenRect(player);
            if (EditModeManager.getFocusedElement() == DraggableElement.AIR_BAR) {
                graphics.renderOutline(bgRect.x() - 1, bgRect.y() - 1, bgRect.width() + 2, bgRect.height() + 2, 0xA0FFFF00);
                graphics.renderOutline(barRect.x() - 1, barRect.y() - 1, barRect.width() + 2, barRect.height() + 2, 0xA0ADD8E6);
                graphics.renderOutline(complexRect.x() - 2, complexRect.y() - 2, complexRect.width() + 4, complexRect.height() + 4, 0x80FFFFFF);
            } else {
                graphics.renderOutline(complexRect.x() - 1, complexRect.y() - 1, complexRect.width() + 2, complexRect.height() + 2, 0x80FFFFFF);
            }
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static int getTextColor() {
        ClientConfig config = ModConfigManager.getClient();
        TextBehavior behavior = config.showAirText;
        long timeSinceNotFull = airTextStartTime > 0 ? System.currentTimeMillis() - airTextStartTime : 0;

        int baseColor = config.airTextColor & 0xFFFFFF;
        int alpha = config.airTextOpacity;

        if (behavior == TextBehavior.WHEN_NOT_FULL && !shouldShowAirText) {
            alpha = (int) (alpha * (RenderUtil.calculateTextAlpha(timeSinceNotFull) / (float) RenderUtil.BASE_TEXT_ALPHA));
        }

        alpha = (int) (alpha * getAirBarAlpha());
        alpha = Math.max(10, Math.min(255, alpha));
        return (alpha << 24) | baseColor;
    }

    private static boolean shouldRenderText() {
        TextBehavior behavior = ModConfigManager.getClient().showAirText;
        if (EditModeManager.isEditModeEnabled()) {
            if (behavior == TextBehavior.ALWAYS || behavior == TextBehavior.WHEN_NOT_FULL) return true;
        }
        if (behavior == TextBehavior.NEVER) return false;
        if (behavior == TextBehavior.ALWAYS) return true;

        int currentAir = Minecraft.getInstance().player.getAirSupply();
        int maxAir = Minecraft.getInstance().player.getMaxAirSupply();
        boolean isNotFull = currentAir < maxAir;

        if (isNotFull) {
            shouldShowAirText = true;
            airTextStartTime = System.currentTimeMillis();
            return true;
        } else {
            if (shouldShowAirText) shouldShowAirText = false;
            return (System.currentTimeMillis() - airTextStartTime) < RenderUtil.TEXT_DISPLAY_DURATION;
        }
    }

    public static void triggerTextDisplay() {
        airTextStartTime = System.currentTimeMillis();
        shouldShowAirText = true;
    }

    public static void stopTextDisplay() {
        shouldShowAirText = false;
    }

    private static void setAirBarVisibility(boolean visible) {
        if (airBarSetVisible != visible) {
            if (!visible) airBarDisabledStartTime = System.currentTimeMillis();
            airBarSetVisible = visible;
        }
    }

    private static boolean isAirBarVisible() {
        return airBarSetVisible;
    }

    private static float getAirBarAlpha() {
        if (isAirBarVisible()) return 1.0f;
        long timeSinceDisabled = System.currentTimeMillis() - airBarDisabledStartTime;
        if (timeSinceDisabled >= RenderUtil.BAR_FADEOUT_DURATION) return 0.0f;
        return Math.max(0.0f, 1.0f - (timeSinceDisabled / (float) RenderUtil.BAR_FADEOUT_DURATION));
    }
}