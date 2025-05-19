package dev.muon.dynamic_resource_bars.render;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.Position;
import dev.muon.dynamic_resource_bars.util.RenderUtil;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class AirBarRenderer {
    private static long airTextStartTime = 0;
    private static boolean shouldShowAirText = false;

    private enum AirIcon {
        NONE("air_0"),
        LOW("air_1"),
        MEDIUM("air_2"),
        HIGH("air_3"),
        FULL("air_4");

        private final String texture;

        AirIcon(String texture) {
            this.texture = texture;
        }

        public String getTexture() {
            return texture;
        }

        public static AirIcon fromAirValue(int airValue) {
            if (airValue <= 0) return NONE;
            float percent = airValue / 300f; // Max air is 300 ticks
            if (percent <= 0.25f) return LOW;
            if (percent <= 0.5f) return MEDIUM;
            if (percent <= 0.75f) return HIGH;
            return FULL;
        }
    }

    public static ScreenRect getScreenRect(Player player) {
        if (player == null) return new ScreenRect(0,0,0,0);
        var config = ModConfigManager.getClient();
        Position anchorPos = HUDPositioning.getPositionFromAnchor(config.airBarAnchor);
        
        Position finalPos = anchorPos.offset(config.airTotalXOffset, config.airTotalYOffset);
        int backgroundWidth = config.airBackgroundWidth;
        int backgroundHeight = config.airBackgroundHeight;
        return new ScreenRect(finalPos.x(), finalPos.y(), backgroundWidth, backgroundHeight);
    }

    public static void render(GuiGraphics graphics, Player player) {
        ClientConfig config = ModConfigManager.getClient();

        int maxAir = player.getMaxAirSupply();
        int currentAir = player.getAirSupply();
        if (currentAir >= maxAir && !player.isUnderWater()) return;

        Position airPos = HUDPositioning.getPositionFromAnchor(config.airBarAnchor);
        boolean isRightAnchored = config.airBarAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT;

        airPos = airPos.offset(config.airTotalXOffset, config.airTotalYOffset);

        int backgroundWidth = config.airBackgroundWidth;
        int backgroundHeight = config.airBackgroundHeight;
        int barWidth = config.airBarWidth;
        int barHeight = config.airBarHeight;
        int barOnlyXOffset = config.airBarXOffset;
        int barOnlyYOffset = config.airBarYOffset;
        int iconSize = config.airIconSize;

        int xPos = airPos.x();
        int yPos = airPos.y();

        graphics.blit(
                DynamicResourceBars.loc("textures/gui/air_background.png"),
                xPos, yPos, 0, 0, backgroundWidth, backgroundHeight, 256, 256
        );

        float airPercent = Math.min(1.0f, (float) currentAir / maxAir);
        int filledWidth = Math.round(barWidth * airPercent);

        if (filledWidth > 0) {
            int barX = xPos + barOnlyXOffset;
            if (isRightAnchored) {
                barX = xPos + barOnlyXOffset + barWidth - filledWidth;
            }

            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/air_bar.png"),
                    barX,
                    yPos + barOnlyYOffset,
                    0, 0,
                    filledWidth,
                    barHeight,
                    256, 256
            );
        }

        if (shouldRenderText()) {
            int textX = (xPos + (backgroundWidth / 2));
            int textY = (yPos + barOnlyYOffset);
            int color = getTextColor();

            RenderUtil.renderText(currentAir, maxAir,
                    graphics, textX, textY, color);
        }

        if (config.enableAirIcon) {
            AirIcon icon = AirIcon.fromAirValue(currentAir);
            int iconX = isRightAnchored ?
                    xPos + backgroundWidth - iconSize + config.airIconXOffset :
                    xPos - 1 + config.airIconXOffset;

            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/air/" + icon.getTexture() + ".png"),
                    iconX,
                    yPos + (backgroundHeight - iconSize) / 2 - 2 + config.airIconYOffset,
                    0, 0,
                    iconSize, iconSize,
                    iconSize, iconSize
            );
        }
    }

    private static int getTextColor() {
        long timeSinceTextTrigger = airTextStartTime > 0 ?
                System.currentTimeMillis() - airTextStartTime : 0;

        int alpha = RenderUtil.calculateTextAlpha(timeSinceTextTrigger);
        alpha = Math.max(10, alpha);

        return (alpha << 24) | 0xFFFFFF;
    }

    private static boolean shouldRenderText() {
        long timeSinceTextTrigger = airTextStartTime > 0 ?
                System.currentTimeMillis() - airTextStartTime : 0;
        return shouldShowAirText ||
                (airTextStartTime > 0 && timeSinceTextTrigger < RenderUtil.TEXT_DISPLAY_DURATION);
    }

    public static void triggerTextDisplay() {
        airTextStartTime = System.currentTimeMillis();
        shouldShowAirText = true;
    }

    public static void stopTextDisplay() {
        shouldShowAirText = false;
    }
}