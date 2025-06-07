package dev.muon.dynamic_resource_bars.render;

import com.mojang.blaze3d.systems.RenderSystem;
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
import dev.muon.dynamic_resource_bars.util.SubElementType;
import dev.muon.dynamic_resource_bars.util.EditModeManager;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;
import dev.muon.dynamic_resource_bars.util.TextBehavior;
import net.minecraft.client.Minecraft;
import dev.muon.dynamic_resource_bars.util.FillDirection;

#if NEWER_THAN_20_1
import net.minecraft.client.DeltaTracker;
#endif

public class AirBarRenderer {
    private static long airTextStartTime = 0;
    private static boolean shouldShowAirText = false;
    
    // Fade behavior tracking
    private static boolean airBarSetVisible = true;
    private static long airBarDisabledStartTime = 0L;

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
            if (airValue <= 0) return NONE;
            if (maxAir <= 0) return NONE; 

            float percent = (float) airValue / maxAir;
            final float POP_RANGE_RATIO = 0.1f;

            final float LOW_UPPER_BOUND = 0.25f;    // Category LOW: (0, 0.25]
            final float MEDIUM_UPPER_BOUND = 0.5f;  // Category MEDIUM: (0.25, 0.5]
            final float HIGH_UPPER_BOUND = 0.75f;   // Category HIGH: (0.5, 0.75]
            // Category FULL is implicitly (0.75, 1.0]

            if (percent > HIGH_UPPER_BOUND) { // In FULL range: (0.75, 1.0]
                if (percent <= HIGH_UPPER_BOUND + (1.0f - HIGH_UPPER_BOUND) * POP_RANGE_RATIO) {
                    return FULL_POP;
                }
                return FULL;
            } else if (percent > MEDIUM_UPPER_BOUND) { // In HIGH range: (0.5, 0.75]
                if (percent <= MEDIUM_UPPER_BOUND + (HIGH_UPPER_BOUND - MEDIUM_UPPER_BOUND) * POP_RANGE_RATIO) {
                    return HIGH_POP;
                }
                return HIGH;
            } else if (percent > LOW_UPPER_BOUND) { // In MEDIUM range: (0.25, 0.5]
                if (percent <= LOW_UPPER_BOUND + (MEDIUM_UPPER_BOUND - LOW_UPPER_BOUND) * POP_RANGE_RATIO) {
                    return MEDIUM_POP;
                }
                return MEDIUM;
            } else { // In LOW range: (0, 0.25] (percent > 0 is implied by airValue > 0 check)
                if (percent <= LOW_UPPER_BOUND * POP_RANGE_RATIO) { // Lower bound of this range is effectively 0
                    return LOW_POP;
                }
                return LOW;
            }
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
                // Text area roughly matches bar area but with text offsets
                ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
                return new ScreenRect(barRect.x() + config.airTextXOffset, 
                                      barRect.y() + config.airTextYOffset, 
                                      barRect.width(), 
                                      barRect.height());
            case ICON:
                // Icon positioned at the left side of the bar, not the background
                ScreenRect barRectForIcon = getSubElementRect(SubElementType.BAR_MAIN, player);
                return new ScreenRect(barRectForIcon.x() - config.airIconSize / 2 + config.airIconXOffset, 
                                      barRectForIcon.y() + (barRectForIcon.height() - config.airIconSize) / 2 + config.airIconYOffset, 
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
        
        // Set visibility based on air status (fade when full) unless in edit mode or underwater
        setAirBarVisibility(currentAir < maxAir || player.isUnderWater() || EditModeManager.isEditModeEnabled());
        
        // Don't render if fully faded and not in edit mode
        if (!isAirBarVisible() && !EditModeManager.isEditModeEnabled() && 
            (System.currentTimeMillis() - airBarDisabledStartTime) > RenderUtil.BAR_FADEOUT_DURATION) {
            return;
        }
        
        // Get current alpha for rendering
        float currentAlphaForRender = getAirBarAlpha();
        if (EditModeManager.isEditModeEnabled() && !isAirBarVisible()) {
            currentAlphaForRender = 1.0f; // Show fully in edit mode
        }
        
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);

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

        // Animation config
        int animationCycles = config.airBarAnimationCycles;
        int frameHeightForAnim = config.airBarFrameHeight; // This is the V-offset step in texture per animation cycle
        FillDirection fillDirection = config.airFillDirection;

        int xPos = airPos.x();
        int yPos = airPos.y();

        // Calculate animation offset
        float ticks = player.tickCount + (#if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif);
        int animOffset = (int) ((ticks / 3) % animationCycles) * frameHeightForAnim;

        graphics.blit(
                DynamicResourceBars.loc("textures/gui/air_background.png"),
                xPos + config.airBackgroundXOffset, 
                yPos + config.airBackgroundYOffset, 
                0, 0, backgroundWidth, backgroundHeight, 256, 256
        );

        float airPercent = (maxAir == 0) ? 0.0f : Math.min(1.0f, (float) currentAir / maxAir);

        if (fillDirection == FillDirection.VERTICAL) {
            int filledHeight = Math.round(barHeight * airPercent);
            if (currentAir > 0 && filledHeight == 0) filledHeight = 1; // Ensure 1 pixel visible if there's air

            if (filledHeight > 0) {
                int barRenderX = xPos + barOnlyXOffset;
                int barRenderY = yPos + barOnlyYOffset + (barHeight - filledHeight); // Fill from bottom up
                // Texture V offset needs to match the visible portion of the bar's animation frame
                // and step through the animation strip.
                int textureVOffset = animOffset + (barHeight - filledHeight);

                graphics.blit(
                        DynamicResourceBars.loc("textures/gui/air_bar.png"),
                        barRenderX, barRenderY,
                        0, textureVOffset,       // uOffset, vOffset
                        barWidth, filledHeight,   // imageWidth, imageHeight (drawn size)
                        256, 1024                // textureSheetWidth, textureSheetHeight
                );
            }
        } else { // HORIZONTAL
            int filledWidth = Math.round(barWidth * airPercent);
            if (currentAir > 0 && filledWidth == 0) filledWidth = 1; // Ensure 1 pixel visible if there's air

            if (filledWidth > 0) {
                int barRenderX = xPos + barOnlyXOffset;
                if (isRightAnchored) {
                    barRenderX = xPos + barOnlyXOffset + barWidth - filledWidth;
                }

                graphics.blit(
                        DynamicResourceBars.loc("textures/gui/air_bar.png"),
                        barRenderX, yPos + barOnlyYOffset,
                        0, animOffset,             // uOffset, vOffset (animOffset is the start of the current animation frame)
                        filledWidth, barHeight,     // imageWidth, imageHeight (drawn size)
                        256, 1024                  // textureSheetWidth, textureSheetHeight
                );
            }
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

            RenderUtil.renderText(currentAir, maxAir,
                    graphics, baseX, textY, color, alignment);
        }

        if (config.enableAirIcon || EditModeManager.isEditModeEnabled()) {
            int displayAir = EditModeManager.isEditModeEnabled() && currentAir >= maxAir ? maxAir / 2 : currentAir;
            AirIcon icon = AirIcon.fromAirValue(displayAir, maxAir);
            ScreenRect iconRect = getSubElementRect(SubElementType.ICON, player);
            
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/air/" + icon.getTexture() + ".png"),
                    iconRect.x(),
                    iconRect.y(),
                    0, 0,
                    iconRect.width(), iconRect.height(),
                    iconRect.width(), iconRect.height()
            );
        }
        
        // Add focus mode outline rendering
        if (EditModeManager.isEditModeEnabled()) {
            ScreenRect complexRect = getScreenRect(player);
            if (EditModeManager.getFocusedElement() == dev.muon.dynamic_resource_bars.util.DraggableElement.AIR_BAR) {
                int focusedBorderColor = 0xA0FFFF00;
                ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
                graphics.renderOutline(bgRect.x()-1, bgRect.y()-1, bgRect.width()+2, bgRect.height()+2, focusedBorderColor);
                
                ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
                graphics.renderOutline(barRect.x()-1, barRect.y()-1, barRect.width()+2, barRect.height()+2, 0xA0ADD8E6);
                
                graphics.renderOutline(complexRect.x()-2, complexRect.y()-2, complexRect.width()+4, complexRect.height()+4, 0x80FFFFFF);
            } else {
                int borderColor = 0x80FFFFFF;
                graphics.renderOutline(complexRect.x()-1, complexRect.y()-1, complexRect.width()+2, complexRect.height()+2, borderColor);
            }
        }
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static int getTextColor() {
        ClientConfig config = ModConfigManager.getClient();
        TextBehavior behavior = config.showAirText;
        long timeSinceNotFull = airTextStartTime > 0 ?
                System.currentTimeMillis() - airTextStartTime : 0;

        int baseColor = config.airTextColor & 0xFFFFFF;
        int alpha = config.airTextOpacity;

        if (behavior == TextBehavior.WHEN_NOT_FULL && shouldShowAirText) {
             // Already handled by shouldRenderText logic, just render with full alpha
        } else if (behavior == TextBehavior.WHEN_NOT_FULL && !shouldShowAirText) {
            alpha = (int)(alpha * (RenderUtil.calculateTextAlpha(timeSinceNotFull) / (float)RenderUtil.BASE_TEXT_ALPHA));
        }
        
        alpha = (int) (alpha * getAirBarAlpha()); // Modulate with bar alpha
        alpha = Math.max(10, Math.min(255, alpha));

        return (alpha << 24) | baseColor;
    }

    private static boolean shouldRenderText() {
        TextBehavior behavior = ModConfigManager.getClient().showAirText;

        if (EditModeManager.isEditModeEnabled()) {
            if (behavior == TextBehavior.ALWAYS || behavior == TextBehavior.WHEN_NOT_FULL) {
                return true;
            }
        }
        if (behavior == TextBehavior.NEVER) {
            return false;
        }
        if (behavior == TextBehavior.ALWAYS) {
            return true;
        }

        // WHEN_NOT_FULL logic
        int currentAir = Minecraft.getInstance().player.getAirSupply();
        int maxAir = Minecraft.getInstance().player.getMaxAirSupply();
        boolean isNotFull = currentAir < maxAir;

        if (isNotFull) {
            shouldShowAirText = true;
            airTextStartTime = System.currentTimeMillis(); // Keep resetting timer while not full
            return true;
        } else {
            // Was not full, but now is. Fade out.
            if (shouldShowAirText) {
                shouldShowAirText = false; // Stop persistent rendering
            }
            long timeSinceFull = System.currentTimeMillis() - airTextStartTime;
            return timeSinceFull < RenderUtil.TEXT_DISPLAY_DURATION;
        }
    }

    public static void triggerTextDisplay() {
        // This method may no longer be necessary with the new logic, but keeping for now
        airTextStartTime = System.currentTimeMillis();
        shouldShowAirText = true;
    }

    public static void stopTextDisplay() {
        // This method may no longer be necessary with the new logic, but keeping for now
        shouldShowAirText = false;
    }

    // New fade behavior methods
    private static void setAirBarVisibility(boolean visible) {
        if (airBarSetVisible != visible) {
            if (!visible) {
                airBarDisabledStartTime = System.currentTimeMillis();
            }
            airBarSetVisible = visible;
        }
    }

    private static boolean isAirBarVisible() {
        return airBarSetVisible;
    }

    private static float getAirBarAlpha() {
        if (isAirBarVisible()) {
            return 1.0f;
        }
        long timeSinceDisabled = System.currentTimeMillis() - airBarDisabledStartTime;
        if (timeSinceDisabled >= RenderUtil.BAR_FADEOUT_DURATION) {
            return 0.0f;
        }
        return Math.max(0.0f, 1.0f - (timeSinceDisabled / (float) RenderUtil.BAR_FADEOUT_DURATION));
    }
}