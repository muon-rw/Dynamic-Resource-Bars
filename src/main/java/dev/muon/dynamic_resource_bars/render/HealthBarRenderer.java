package dev.muon.dynamic_resource_bars.render;


import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.foundation.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.Position;
import dev.muon.dynamic_resource_bars.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif

import java.lang.reflect.Method;

import static dev.muon.dynamic_resource_bars.util.RenderUtil.TEXT_DISPLAY_DURATION;
import dev.muon.dynamic_resource_bars.util.EditModeManager;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import dev.muon.dynamic_resource_bars.util.TextBehavior;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;

public class HealthBarRenderer {

    private static float lastHealth = -1;
    private static long fullHealthStartTime = 0;
    private static boolean healthBarSetVisible = true; // Default to visible
    private static long healthBarDisabledStartTime = 0L;

    private enum BarType {
        NORMAL("health_bar"),
        POISON("health_bar_poison"),
        WITHER("health_bar_wither"),
        FROZEN("health_bar_frozen"),
        SCORCHED("health_bar_scorched");

        private final String texture;

        BarType(String texture) {
            this.texture = texture;
        }

        BarType() {
            this.texture = name().toLowerCase();
        }

        public String getTexture() {
            return texture;
        }

        public static BarType fromPlayerState(Player player) {
            if (player.hasEffect(MobEffects.POISON)) return POISON;
            if (player.hasEffect(MobEffects.WITHER)) return WITHER;
            if (isFrozen(player)) return FROZEN;
            if (isScorched(player)) return SCORCHED;
            return NORMAL;
        }
    }

    public static ScreenRect getScreenRect(Player player) {
        if (player == null) return new ScreenRect(0,0,0,0);
        Position healthPos = HUDPositioning.getPositionFromAnchor(ModConfigManager.getClient().healthBarAnchor.get())
                .offset(ModConfigManager.getClient().healthTotalXOffset.get(), ModConfigManager.getClient().healthTotalYOffset.get());
        int backgroundWidth = ModConfigManager.getClient().healthBackgroundWidth.get();
        int backgroundHeight = ModConfigManager.getClient().healthBackgroundHeight.get();
        return new ScreenRect(healthPos.x(), healthPos.y(), backgroundWidth, backgroundHeight);
    }

    public static ScreenRect getSubElementRect(SubElementType type, Player player) {
        ScreenRect complexRect = getScreenRect(player);
        if (complexRect == null || complexRect.width() == 0 && complexRect.height() == 0) return new ScreenRect(0,0,0,0);

        int x = complexRect.x();
        int y = complexRect.y();

        switch (type) {
            case BACKGROUND:
                return new ScreenRect(x, y, 
                                      ModConfigManager.getClient().healthBackgroundWidth.get(), 
                                      ModConfigManager.getClient().healthBackgroundHeight.get());
            case BAR_MAIN:
                return new ScreenRect(x + ModConfigManager.getClient().healthBarXOffset.get(), 
                                      y + ModConfigManager.getClient().healthBarYOffset.get(), 
                                      ModConfigManager.getClient().healthBarWidth.get(), 
                                      ModConfigManager.getClient().healthBarHeight.get());
            case FOREGROUND_DETAIL:
                return new ScreenRect(x + ModConfigManager.getClient().healthOverlayXOffset.get(), 
                                      y + ModConfigManager.getClient().healthOverlayYOffset.get(), 
                                      ModConfigManager.getClient().healthOverlayWidth.get(),
                                      ModConfigManager.getClient().healthOverlayHeight.get());
            default:
                return new ScreenRect(0,0,0,0);
        }
    }

    public static void render(GuiGraphics graphics, Player player, float maxHealth, float actualHealth, int absorptionAmount,
            #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif) {

        boolean shouldFade = ModConfigManager.getClient().fadeHealthWhenFull.get() && actualHealth >= maxHealth && absorptionAmount == 0;
        setHealthBarVisibility(!shouldFade || EditModeManager.isEditModeEnabled());

        if (!isHealthBarVisible() && !EditModeManager.isEditModeEnabled() && (System.currentTimeMillis() - healthBarDisabledStartTime) > RenderUtil.BAR_FADEOUT_DURATION) {
            return;
        }

        if (!Minecraft.getInstance().gameMode.canHurtPlayer() && !EditModeManager.isEditModeEnabled()) {
            return;
        }

        float currentAlphaForRender = getHealthBarAlpha();
        if (EditModeManager.isEditModeEnabled() && !isHealthBarVisible()) {
            currentAlphaForRender = 1.0f; // Show fully if in edit mode, even if normally faded
        }

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);

        ScreenRect complexRect = getScreenRect(player);
        int xPos = complexRect.x();
        int yPos = complexRect.y();

        int backgroundWidth = ModConfigManager.getClient().healthBackgroundWidth.get();
        int backgroundHeight = ModConfigManager.getClient().healthBackgroundHeight.get();
        int barWidth = ModConfigManager.getClient().healthBarWidth.get();
        int barHeight = ModConfigManager.getClient().healthBarHeight.get();
        int barOnlyXOffset = ModConfigManager.getClient().healthBarXOffset.get();
        int barOnlyYOffset = ModConfigManager.getClient().healthBarYOffset.get();
        int animationCycles = ModConfigManager.getClient().healthBarAnimationCycles.get();
        int frameHeight = ModConfigManager.getClient().healthBarFrameHeight.get();

        if (ModConfigManager.getClient().enableHealthBackground.get()) {
            ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/health_background.png"), 
                    bgRect.x(), bgRect.y(), 0, 0, bgRect.width(), bgRect.height(), 256, 256
            );
        }

        int animOffset = (int) (((player.tickCount + #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif) / 3) % animationCycles) * frameHeight;

        ScreenRect mainBarRect = getSubElementRect(SubElementType.BAR_MAIN, player);
        renderBaseBar(graphics, player, maxHealth, actualHealth, 
                      mainBarRect.x(), mainBarRect.y(), mainBarRect.width(), mainBarRect.height(), 
                      0, 0,
                      animOffset);

        renderBarOverlays(graphics, player, absorptionAmount, 
                          mainBarRect.x(), mainBarRect.y(), mainBarRect.width(), mainBarRect.height(), 
                          0,0);

        renderBackgroundOverlays(graphics, player, xPos, yPos, backgroundWidth, backgroundHeight);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int textX = mainBarRect.x() + (mainBarRect.width() / 2);
        int textY = mainBarRect.y() + (mainBarRect.height() - Minecraft.getInstance().font.lineHeight) / 2;

        if (shouldRenderHealthText(actualHealth, maxHealth, player)) {
            int color = getHealthTextColor(actualHealth, maxHealth);
            HorizontalAlignment alignment = ModConfigManager.getClient().healthTextAlign.get();

            int baseX = mainBarRect.x();
            if (alignment == HorizontalAlignment.CENTER) {
                baseX = mainBarRect.x() + (mainBarRect.width() / 2);
            } else if (alignment == HorizontalAlignment.RIGHT) {
                baseX = mainBarRect.x() + mainBarRect.width();
            }
            // For LEFT, baseX is already mainBarRect.x()

            int baseY = mainBarRect.y() + (mainBarRect.height() / 2); // Center Y for the text

            RenderUtil.renderText(actualHealth, maxHealth,
                    graphics, baseX, baseY, color, alignment);
        }

        if (absorptionAmount > 0) {
            String absorptionText = "+" + absorptionAmount;
            Minecraft mc = Minecraft.getInstance();
            float scalingFactor = ModConfigManager.getClient().textScalingFactor.get().floatValue();
            // Width of the text *before* our utility scales it
            int unscaledTextWidth = mc.font.width(absorptionText);

            // baseX for renderAdditionText should be the intended *start* of the text *before* internal scaling in RenderUtil
            // Position it to the right of the background, with some padding
            int absorptionTextX = complexRect.x() + backgroundWidth - (int)(unscaledTextWidth * scalingFactor) - 2; // 2px padding from the right edge
            int absorptionTextY = mainBarRect.y() + (mainBarRect.height() / 2); // Vertically centered with the main bar

            RenderUtil.renderAdditionText(absorptionText, graphics, absorptionTextX, absorptionTextY, ((int)(RenderUtil.BASE_TEXT_ALPHA * currentAlphaForRender) << 24) | 0xFFFFFF);
        }

        if (EditModeManager.isEditModeEnabled()) {
            DraggableElement currentBarType = DraggableElement.HEALTH_BAR;
            if (EditModeManager.getFocusedElement() == currentBarType) {
                int focusedBorderColor = 0xA0FFFF00;
                ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
                graphics.renderOutline(bgRect.x()-1, bgRect.y()-1, bgRect.width()+2, bgRect.height()+2, focusedBorderColor);
                
                ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
                graphics.renderOutline(barRect.x()-1, barRect.y()-1, barRect.width()+2, barRect.height()+2, 0xA000FF00);
                
                if (ModConfigManager.getClient().enableHealthForeground.get()) {
                    ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                    graphics.renderOutline(fgRect.x()-1, fgRect.y()-1, fgRect.width()+2, fgRect.height()+2, 0xA0FF00FF);
                }
                graphics.renderOutline(complexRect.x()-2, complexRect.y()-2, complexRect.width()+4, complexRect.height()+4, focusedBorderColor);
            } else {
                int borderColor = 0x80FFFFFF;
                graphics.renderOutline(complexRect.x()-1, complexRect.y()-1, complexRect.width()+2, complexRect.height()+2, borderColor);
            }
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Reset shader color
        RenderSystem.disableBlend(); // Ensure blend is disabled after rendering this bar
    }

    private static void renderBaseBar(GuiGraphics graphics, Player player, float maxHealth, float actualHealth,
                                      int barAbsX, int barAbsY, int barAbsWidth, int barAbsHeight, 
                                      int barXOffsetWithinTexture, int barYOffsetWithinTexture,
                                      int animOffset) {
        BarType barType = BarType.fromPlayerState(player);
        int partialBarWidth = (int) (barAbsWidth * (actualHealth / maxHealth));
        if (partialBarWidth <= 0 && actualHealth > 0) partialBarWidth = 1;
        if (partialBarWidth > 0) {
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/" + barType.getTexture() + ".png"),
                    barAbsX, barAbsY,
                    barXOffsetWithinTexture, animOffset + barYOffsetWithinTexture,
                    partialBarWidth, barAbsHeight,
                    256, 256
            );
        }
    }

    private static void renderBarOverlays(GuiGraphics graphics, Player player, int absorptionAmount,
                                          int barAbsX, int barAbsY, int barAbsWidth, int barAbsHeight,
                                          int barXOffset, int barYOffset) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float tempScale = getTemperatureScale(player);
        renderTemperatureOverlay(graphics, tempScale, barAbsX + barXOffset, barAbsY + barYOffset, barAbsWidth, barAbsHeight, 0, 0);

        if (absorptionAmount > 0) {
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/absorption_overlay.png"),
                    barAbsX + barXOffset, barAbsY + barYOffset,
                    0, 0, barAbsWidth, barAbsHeight,
                    256, 256
            );
        }
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void renderBackgroundOverlays(GuiGraphics graphics, Player player,
                                             int complexX, int complexY, int backgroundWidth, int backgroundHeight) {
        if (player.level().getLevelData().isHardcore()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/hardcore_overlay.png"),
                    complexX, complexY,
                    0, 0, backgroundWidth, backgroundHeight,
                    256, 256
            );
            RenderSystem.disableBlend();
        }

        float wetScale = getWetnessScale(player);
        if (wetScale > 0) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, wetScale);
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/wetness_overlay.png"),
                    complexX, complexY,
                    0, 0, backgroundWidth, backgroundHeight,
                    256, 256
            );
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }

        if (ModConfigManager.getClient().enableHealthForeground.get()) {
            ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/detail_overlay.png"),
                    fgRect.x(), fgRect.y(),
                    0, 0, fgRect.width(), fgRect.height(),
                    256, 256
            );
        }
    }

    private static void renderTemperatureOverlay(GuiGraphics graphics, float tempScale,
                                                 int xPos, int yPos, int barWidth, int barHeight,
                                                 int barXOffset, int barYOffset) {
        if (tempScale > 0) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, getHealthBarAlpha()); // Apply alpha to heat overlay
            int heatWidth = (int) (barWidth * tempScale);
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/heat_overlay.png"),
                    xPos + barXOffset, yPos + barYOffset,
                    0, 0, heatWidth, barHeight,
                    256, 256
            );
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Reset after heat overlay
        } else if (tempScale < 0) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, getHealthBarAlpha()); // Apply alpha to cold overlay
            int coldWidth = (int) (barWidth * -tempScale);
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/cold_overlay.png"),
                    xPos + barXOffset, yPos + barYOffset,
                    0, 0, coldWidth, barHeight,
                    256, 256
            );
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Reset after cold overlay
        }
    }

    private static float getWetnessScale(Player player) {
        try {
            Method getWetScale = player.getClass().getMethod("thermoo$getSoakedScale");
            return (float) getWetScale.invoke(player);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    private static boolean isScorched(Player player) {
        try {
            Method getMaxTemp = player.getClass().getMethod("thermoo$getMaxTemperature");
            Method getTemp = player.getClass().getMethod("thermoo$getTemperature");

            int maxTemperature = (int) getMaxTemp.invoke(player);
            int temperature = (int) getTemp.invoke(player);

            return temperature >= maxTemperature - 1;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isFrozen(Player player) {
        if (player.isFullyFrozen()) {
            return true;
        }

        try {
            Method getTemp = player.getClass().getMethod("thermoo$getTemperature");
            Method getTempScale = player.getClass().getMethod("thermoo$getTemperatureScale");

            int minTemperature = (int) getTemp.invoke(player);
            if (minTemperature < 0) {
                float tempScale = (float) getTempScale.invoke(player);
                return tempScale <= -0.99f;
            }
        } catch (Exception e) {
            // Revert to vanilla check
        }

        return false;
    }

    private static float getTemperatureScale(Player player) {
        try {
            Method getTempScale = player.getClass().getMethod("thermoo$getTemperatureScale");
            return (float) getTempScale.invoke(player);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    private static int getHealthTextColor(float currentHealth, float maxHealth) {
        TextBehavior behavior = ModConfigManager.getClient().showHealthText.get();
        // Alpha will be combined with the color later. Here we just get the base color.
        int baseColor = 0xFFFFFF; // White

        int alpha = RenderUtil.BASE_TEXT_ALPHA;
        if (behavior == TextBehavior.WHEN_NOT_FULL && currentHealth >= maxHealth) {
            long timeSinceFull = System.currentTimeMillis() - fullHealthStartTime;
            alpha = RenderUtil.calculateTextAlpha(timeSinceFull);
        }
        alpha = (int) (alpha * getHealthBarAlpha()); // Modulate with bar alpha
        alpha = Math.max(10, alpha);

        return (alpha << 24) | baseColor;
    }

    private static boolean shouldRenderHealthText(float currentHealth, float maxHealth, Player player) {
        TextBehavior behavior = ModConfigManager.getClient().showHealthText.get();
        if (behavior == TextBehavior.NEVER) {
            return false;
        }
        if (behavior == TextBehavior.ALWAYS) {
            return true;
        }
        // WHEN_NOT_FULL logic
        boolean isFull = currentHealth >= maxHealth;
        if (isFull) {
            if (lastHealth < maxHealth || lastHealth == -1) { // Just became full or first check
                fullHealthStartTime = System.currentTimeMillis(); // Use System.currentTimeMillis()
            }
            lastHealth = currentHealth;
            // Show for a short duration after becoming full
            return (System.currentTimeMillis() - fullHealthStartTime) < RenderUtil.TEXT_DISPLAY_DURATION;
        } else {
            lastHealth = currentHealth;
            return true; // Not full, so show
        }
    }

    // New helper methods for bar visibility and alpha
    private static void setHealthBarVisibility(boolean visible) {
        if (healthBarSetVisible != visible) {
            if (!visible) {
                healthBarDisabledStartTime = System.currentTimeMillis();
            }
            healthBarSetVisible = visible;
        }
    }

    private static boolean isHealthBarVisible() {
        return healthBarSetVisible;
    }

    private static float getHealthBarAlpha() {
        if (isHealthBarVisible()) {
            return 1.0f;
        }
        long timeSinceDisabled = System.currentTimeMillis() - healthBarDisabledStartTime;
        if (timeSinceDisabled >= RenderUtil.BAR_FADEOUT_DURATION) {
            return 0.0f;
        }
        return Math.max(0.0f, 1.0f - (timeSinceDisabled / (float) RenderUtil.BAR_FADEOUT_DURATION));
    }
}