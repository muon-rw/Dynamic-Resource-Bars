package dev.muon.dynamic_resource_bars.render;


import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
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
        Position staminaPosBase = HUDPositioning.getPositionFromAnchor(AllConfigs.client().staminaBarAnchor.get());
        Position staminaPos = staminaPosBase.offset(AllConfigs.client().staminaTotalXOffset.get(), AllConfigs.client().staminaTotalYOffset.get());
        int backgroundWidth = AllConfigs.client().staminaBackgroundWidth.get();
        int backgroundHeight = AllConfigs.client().staminaBackgroundHeight.get();
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
                                      AllConfigs.client().staminaBackgroundWidth.get(), 
                                      AllConfigs.client().staminaBackgroundHeight.get());
            case BAR_MAIN:
                return new ScreenRect(x + AllConfigs.client().staminaBarXOffset.get(), 
                                      y + AllConfigs.client().staminaBarYOffset.get(), 
                                      AllConfigs.client().staminaBarWidth.get(), 
                                      AllConfigs.client().staminaBarHeight.get());
            case FOREGROUND_DETAIL:
                 return new ScreenRect(x + AllConfigs.client().staminaOverlayXOffset.get(), 
                                       y + AllConfigs.client().staminaOverlayYOffset.get(), 
                                       AllConfigs.client().staminaOverlayWidth.get(),
                                       AllConfigs.client().staminaOverlayHeight.get());
            default:
                return new ScreenRect(0,0,0,0); 
        }
    }

    public static void render(GuiGraphics graphics, Player player, #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif) {
        if (AllConfigs.client().fadeStaminaWhenFull.get() && player.getFoodData().getFoodLevel() >= 20 && !EditModeManager.isEditModeEnabled()) {
            return;
        }

        if (!Minecraft.getInstance().gameMode.canHurtPlayer() && !EditModeManager.isEditModeEnabled()) {
            return;
        }

        ScreenRect complexRect = getScreenRect(player);
        int xPos = complexRect.x();
        int yPos = complexRect.y();
        int backgroundWidth = complexRect.width();
        int backgroundHeight = complexRect.height();
        
        int animationCycles = AllConfigs.client().staminaBarAnimationCycles.get();
        int frameHeight = AllConfigs.client().staminaBarFrameHeight.get();
        int animOffset = (int) (((player.tickCount + #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif) / 3) % animationCycles) * frameHeight;
        boolean isRightAnchored = AllConfigs.client().staminaBarAnchor.get().getSide() == HUDPositioning.AnchorSide.RIGHT;

        if (AllConfigs.client().enableStaminaBackground.get()) {
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

        if (AllConfigs.client().enableStaminaForeground.get()) {
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
            HorizontalAlignment alignment = AllConfigs.client().staminaTextAlign.get();

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
                if (AllConfigs.client().enableStaminaBackground.get()) {
                    graphics.renderOutline(bgRect.x()-1, bgRect.y()-1, bgRect.width()+2, bgRect.height()+2, focusedBorderColor);
                }
                
                ScreenRect barRectOutline = getSubElementRect(SubElementType.BAR_MAIN, player);
                graphics.renderOutline(barRectOutline.x()-1, barRectOutline.y()-1, barRectOutline.width()+2, barRectOutline.height()+2, 0xA0FFA500);
                
                if (AllConfigs.client().enableStaminaForeground.get()) {
                    ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                    graphics.renderOutline(fgRect.x()-1, fgRect.y()-1, fgRect.width()+2, fgRect.height()+2, 0xA0FF00FF);
                }
                 graphics.renderOutline(complexRect.x()-2, complexRect.y()-2, complexRect.width()+4, complexRect.height()+4, focusedBorderColor);
            } else {
                int borderColor = 0x80FFFFFF; 
                graphics.renderOutline(complexRect.x()-1, complexRect.y()-1, complexRect.width()+2, complexRect.height()+2, borderColor);
            }
        }
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
                    256, 256
            );
        }
    }

    private static boolean shouldRenderStaminaText(float currentStamina, float maxStamina, Player player) {
        if (currentStamina >= maxStamina) {
            if (lastStamina < maxStamina) {
                fullStaminaStartTime = System.currentTimeMillis();
            }
        } else {
            fullStaminaStartTime = 0;
        }
        lastStamina = currentStamina;

        dev.muon.dynamic_resource_bars.util.TextBehavior textBehavior = AllConfigs.client().showStaminaText.get();
        switch (textBehavior) {
            case ALWAYS:
                return true;
            case NEVER:
                return false;
            case WHEN_NOT_FULL:
                long timeSinceFullStamina = fullStaminaStartTime > 0 ? System.currentTimeMillis() - fullStaminaStartTime : 0;
                return currentStamina < maxStamina || (fullStaminaStartTime > 0 && timeSinceFullStamina < RenderUtil.TEXT_DISPLAY_DURATION);
            default:
                return false;
        }
    }

    private static int getStaminaTextColor(float currentStamina, float maxStamina) {
        long timeSinceFullStamina = fullStaminaStartTime > 0 ?
                System.currentTimeMillis() - fullStaminaStartTime : 0;

        int alpha = RenderUtil.BASE_TEXT_ALPHA;

        dev.muon.dynamic_resource_bars.util.TextBehavior textBehavior = AllConfigs.client().showStaminaText.get();
        if (textBehavior == dev.muon.dynamic_resource_bars.util.TextBehavior.WHEN_NOT_FULL) {
            if (currentStamina >= maxStamina) {
                alpha = RenderUtil.calculateTextAlpha(timeSinceFullStamina);
            }
        }

        alpha = Math.max(10, alpha);
        return (alpha << 24) | 0xFFFFFF;
    }
}