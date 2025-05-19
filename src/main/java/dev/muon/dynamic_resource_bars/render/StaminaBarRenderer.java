package dev.muon.dynamic_resource_bars.render;


import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.Position;
import dev.muon.dynamic_resource_bars.util.RenderUtil;
import dev.muon.dynamic_resource_bars.util.TextBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif

import dev.muon.dynamic_resource_bars.util.EditModeManager;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;

public class StaminaBarRenderer {
    private static final float CRITICAL_THRESHOLD = 6.0f;
    private static float lastStamina = -1;
    private static long fullStaminaStartTime = 0;
    private static boolean staminaBarSetVisible = true; // Default to visible
    private static long staminaBarDisabledStartTime = 0L;
    // TODO: Appleskin compat
    // TODO: Pulse when hunger drains a tick, similar to vanilla shake

    private enum BarType {
        NORMAL("stamina_bar"),
        HUNGER("stamina_bar_hunger"),
        CRITICAL("stamina_bar_critical");

        private final String texture;

        BarType(String texture) {
            this.texture = texture;
        }

        public String getTexture() {
            return texture;
        }

        public static BarType fromPlayerState(Player player, float stamina) {
            if (player.hasEffect(MobEffects.HUNGER)) return HUNGER;
            if (stamina <= CRITICAL_THRESHOLD) return CRITICAL;
            return NORMAL;
        }
    }

    public static ScreenRect getScreenRect(Player player) {
        if (player == null) return new ScreenRect(0,0,0,0);
        Position staminaPosBase = HUDPositioning.getPositionFromAnchor(ModConfigManager.getClient().staminaBarAnchor.get());
        Position staminaPos = staminaPosBase.offset(ModConfigManager.getClient().staminaTotalXOffset.get(), ModConfigManager.getClient().staminaTotalYOffset.get());
        int backgroundWidth = ModConfigManager.getClient().staminaBackgroundWidth.get();
        int backgroundHeight = ModConfigManager.getClient().staminaBackgroundHeight.get();
        return new ScreenRect(staminaPos.x(), staminaPos.y(), backgroundWidth, backgroundHeight);
    }

    public static ScreenRect getSubElementRect(SubElementType type, Player player) {
        ScreenRect complexRect = getScreenRect(player); 
        if (complexRect == null || complexRect.width() == 0 && complexRect.height() == 0) return new ScreenRect(0,0,0,0);

        int x = complexRect.x();
        int y = complexRect.y();

        switch (type) {
            case BACKGROUND:
                return new ScreenRect(x, y, 
                                      ModConfigManager.getClient().staminaBackgroundWidth.get(), 
                                      ModConfigManager.getClient().staminaBackgroundHeight.get());
            case BAR_MAIN:
                return new ScreenRect(x + ModConfigManager.getClient().staminaBarXOffset.get(), 
                                      y + ModConfigManager.getClient().staminaBarYOffset.get(), 
                                      ModConfigManager.getClient().staminaBarWidth.get(), 
                                      ModConfigManager.getClient().staminaBarHeight.get());
            case FOREGROUND_DETAIL:
                 return new ScreenRect(x + ModConfigManager.getClient().staminaOverlayXOffset.get(), 
                                       y + ModConfigManager.getClient().staminaOverlayYOffset.get(), 
                                       ModConfigManager.getClient().staminaOverlayWidth.get(),
                                       ModConfigManager.getClient().staminaOverlayHeight.get());
            default:
                return new ScreenRect(0,0,0,0); 
        }
    }

    public static void render(GuiGraphics graphics, Player player, #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif) {
        boolean shouldFade = ModConfigManager.getClient().fadeStaminaWhenFull.get() && player.getFoodData().getFoodLevel() >= 20;
        setStaminaBarVisibility(!shouldFade || EditModeManager.isEditModeEnabled());

        if (!isStaminaBarVisible() && !EditModeManager.isEditModeEnabled() && (System.currentTimeMillis() - staminaBarDisabledStartTime) > RenderUtil.BAR_FADEOUT_DURATION) {
            return;
        }

        if (!Minecraft.getInstance().gameMode.canHurtPlayer() && !EditModeManager.isEditModeEnabled()) {
            return;
        }

        float currentAlphaForRender = getStaminaBarAlpha();
        if (EditModeManager.isEditModeEnabled() && !isStaminaBarVisible()) {
            currentAlphaForRender = 1.0f; // Show fully if in edit mode
        }

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);

        ScreenRect complexRect = getScreenRect(player);
        
        int animationCycles = ModConfigManager.getClient().staminaBarAnimationCycles.get();
        int frameHeight = ModConfigManager.getClient().staminaBarFrameHeight.get();
        int animOffset = (int) (((player.tickCount + #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif) / 3) % animationCycles) * frameHeight;
        boolean isRightAnchored = ModConfigManager.getClient().staminaBarAnchor.get().getSide() == HUDPositioning.AnchorSide.RIGHT;

        if (ModConfigManager.getClient().enableStaminaBackground.get()) {
            ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/stamina_background.png"),
                    bgRect.x(), bgRect.y(), 0, 0, bgRect.width(), bgRect.height(), 256, 256
            );
        }

        float maxStamina = 20f;
        float currentStamina = player.getFoodData().getFoodLevel();
        ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
        renderBaseBar(graphics, player, currentStamina, maxStamina, 
                      barRect,
                      animOffset, isRightAnchored);

        if (ModConfigManager.getClient().enableStaminaForeground.get()) {
             ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
             graphics.blit(
                    DynamicResourceBars.loc("textures/gui/detail_overlay.png"),
                    fgRect.x(), fgRect.y(),
                    0, 0, fgRect.width(), fgRect.height(),
                    256, 256
            );
        }
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); 

        if (shouldRenderStaminaText(currentStamina, maxStamina, player)) {
            int color = getStaminaTextColor(currentStamina, maxStamina);
            HorizontalAlignment alignment = ModConfigManager.getClient().staminaTextAlign.get();

            int baseX = barRect.x();
            if (alignment == HorizontalAlignment.CENTER) {
                baseX = barRect.x() + (barRect.width() / 2);
            } else if (alignment == HorizontalAlignment.RIGHT) {
                baseX = barRect.x() + barRect.width();
            }

            int baseY = barRect.y() + (barRect.height() / 2);

            RenderUtil.renderText(currentStamina, maxStamina, graphics, baseX, baseY, color, alignment);
        }

        if (EditModeManager.isEditModeEnabled()) {
            DraggableElement currentBarType = DraggableElement.STAMINA_BAR;
            if (EditModeManager.getFocusedElement() == currentBarType) {
                int focusedBorderColor = 0xA0FFFF00;
                ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
                if (ModConfigManager.getClient().enableStaminaBackground.get()) {
                    graphics.renderOutline(bgRect.x()-1, bgRect.y()-1, bgRect.width()+2, bgRect.height()+2, focusedBorderColor);
                }
                
                ScreenRect barRectOutline = getSubElementRect(SubElementType.BAR_MAIN, player);
                graphics.renderOutline(barRectOutline.x()-1, barRectOutline.y()-1, barRectOutline.width()+2, barRectOutline.height()+2, 0xA0FFA500);
                
                if (ModConfigManager.getClient().enableStaminaForeground.get()) {
                    ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                    graphics.renderOutline(fgRect.x()-1, fgRect.y()-1, fgRect.width()+2, fgRect.height()+2, 0xA0FF00FF);
                }
                 graphics.renderOutline(complexRect.x()-2, complexRect.y()-2, complexRect.width()+4, complexRect.height()+4, focusedBorderColor);
            } else {
                int borderColor = 0x80FFFFFF; 
                graphics.renderOutline(complexRect.x()-1, complexRect.y()-1, complexRect.width()+2, complexRect.height()+2, borderColor);
            }
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static void renderBaseBar(GuiGraphics graphics, Player player, float currentStamina, float maxStamina,
                                      ScreenRect barAreaRect,
                                      int animOffset, boolean isRightAnchored) {
        BarType barType = BarType.fromPlayerState(player, currentStamina);
        int totalBarWidth = barAreaRect.width();
        int barHeight = barAreaRect.height();

        int partialBarWidth = (int) (totalBarWidth * (currentStamina / maxStamina));
        if (partialBarWidth <= 0 && currentStamina > 0) partialBarWidth = 1;
        if (partialBarWidth > totalBarWidth) partialBarWidth = totalBarWidth;

        int barX = barAreaRect.x();
        int barY = barAreaRect.y();
        
        if (isRightAnchored) {
            barX = barAreaRect.x() + totalBarWidth - partialBarWidth;
        }

        if (partialBarWidth > 0) {
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/" + barType.getTexture() + ".png"),
                    barX, barY,
                    0, animOffset,
                    partialBarWidth, barHeight,
                    256, 1024
            );
        }
    }

    private static boolean shouldRenderStaminaText(float currentStamina, float maxStamina, Player player) {
        TextBehavior textBehavior = ModConfigManager.getClient().showStaminaText.get();
        if (textBehavior == TextBehavior.NEVER) {
            return false;
        }
        if (textBehavior == TextBehavior.ALWAYS) {
            return true;
        }
        // WHEN_NOT_FULL logic
        boolean isFull = currentStamina >= maxStamina;
        if (isFull) {
            if (lastStamina < maxStamina || lastStamina == -1) { // Just became full or first check
                fullStaminaStartTime = System.currentTimeMillis(); // Use System.currentTimeMillis()
            }
            lastStamina = currentStamina;
            // Show for a short duration after becoming full
            return (System.currentTimeMillis() - fullStaminaStartTime) < RenderUtil.TEXT_DISPLAY_DURATION;
        } else {
            lastStamina = currentStamina;
            return true; // Not full, so show
        }
    }

    private static int getStaminaTextColor(float currentStamina, float maxStamina) {
        TextBehavior textBehavior = ModConfigManager.getClient().showStaminaText.get();
        int baseColor = 0xFFFFFF; // White
        int alpha = RenderUtil.BASE_TEXT_ALPHA;

        if (textBehavior == TextBehavior.WHEN_NOT_FULL && currentStamina >= maxStamina) {
            long timeSinceFull = System.currentTimeMillis() - fullStaminaStartTime;
            alpha = RenderUtil.calculateTextAlpha(timeSinceFull);
        }
        
        alpha = (int) (alpha * getStaminaBarAlpha()); // Modulate with bar alpha
        alpha = Math.max(10, alpha); // Ensure minimum visibility
        return (alpha << 24) | baseColor;
    }

    // New helper methods for bar visibility and alpha
    private static void setStaminaBarVisibility(boolean visible) {
        if (staminaBarSetVisible != visible) {
            if (!visible) {
                staminaBarDisabledStartTime = System.currentTimeMillis();
            }
            staminaBarSetVisible = visible;
        }
    }

    private static boolean isStaminaBarVisible() {
        return staminaBarSetVisible;
    }

    private static float getStaminaBarAlpha() {
        if (isStaminaBarVisible()) {
            return 1.0f;
        }
        long timeSinceDisabled = System.currentTimeMillis() - staminaBarDisabledStartTime;
        if (timeSinceDisabled >= RenderUtil.BAR_FADEOUT_DURATION) {
            return 0.0f;
        }
        return Math.max(0.0f, 1.0f - (timeSinceDisabled / (float) RenderUtil.BAR_FADEOUT_DURATION));
    }
}