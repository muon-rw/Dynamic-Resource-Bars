package dev.muon.dynamic_resource_bars.render;


import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.muon.dynamic_resource_bars.compat.AppleSkinCompat;
import dev.muon.dynamic_resource_bars.compat.StaminaProviderManager;
import dev.muon.dynamic_resource_bars.compat.BewitchmentCompat;

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif


import vectorwing.farmersdelight.common.registry.ModEffects;
import dev.muon.dynamic_resource_bars.config.ClientConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StaminaBarRenderer {
    private static float lastStamina = -1;
    private static long fullStaminaStartTime = 0;
    private static boolean staminaBarSetVisible = true; // Default to visible
    private static long staminaBarDisabledStartTime = 0L;
    
    // Mount health tracking
    private static float lastMountHealth = -1;
    private static float lastMountMaxHealth = -1;
    private static long fullMountHealthStartTime = 0;
    
    // Fadeout chunking system
    private static final long CHUNK_FADEOUT_DURATION = RenderUtil.BAR_FADEOUT_DURATION / 3;
    private static final List<FadingChunk> fadingChunks = new ArrayList<>();
    private static float previousValue = -1;
    private static float previousMax = -1;
    private static boolean wasMounted = false;
    
    private static class FadingChunk {
        final float startValue;    // Where the chunk starts (in resource units)
        final float endValue;      // Where it ends
        final float maxValue;      // Max value at time of creation
        final long creationTime;   // When this chunk started fading
        final String texture;      // Which bar texture to use
        final int animOffset;      // Animation frame at creation
        final boolean isMounted;   // Whether this was mount health
        
        FadingChunk(float startValue, float endValue, float maxValue, String texture, int animOffset, boolean isMounted) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.maxValue = maxValue;
            this.creationTime = System.currentTimeMillis();
            this.texture = texture;
            this.animOffset = animOffset;
            this.isMounted = isMounted;
        }
        
        float getAlpha() {
            long elapsed = System.currentTimeMillis() - creationTime;
            if (elapsed >= CHUNK_FADEOUT_DURATION) return 0f;
            return 1f - (elapsed / (float) CHUNK_FADEOUT_DURATION);
        }
        
        boolean isExpired() {
            return getAlpha() <= 0f;
        }
    }

    public static ScreenRect getScreenRect(Player player) {
        if (player == null) return new ScreenRect(0, 0, 0, 0);
        Position staminaPosBase = HUDPositioning.getPositionFromAnchor(ModConfigManager.getClient().staminaBarAnchor);
        Position staminaPos = staminaPosBase.offset(ModConfigManager.getClient().staminaTotalXOffset, ModConfigManager.getClient().staminaTotalYOffset);
        int backgroundWidth = ModConfigManager.getClient().staminaBackgroundWidth;
        int backgroundHeight = ModConfigManager.getClient().staminaBackgroundHeight;
        return new ScreenRect(staminaPos.x(), staminaPos.y(), backgroundWidth, backgroundHeight);
    }

    public static ScreenRect getSubElementRect(SubElementType type, Player player) {
        ScreenRect complexRect = getScreenRect(player);
        if (complexRect == null || complexRect.width() == 0 && complexRect.height() == 0)
            return new ScreenRect(0, 0, 0, 0);

        int x = complexRect.x();
        int y = complexRect.y();

        switch (type) {
            case BACKGROUND:
                return new ScreenRect(x + ModConfigManager.getClient().staminaBackgroundXOffset,
                        y + ModConfigManager.getClient().staminaBackgroundYOffset,
                        ModConfigManager.getClient().staminaBackgroundWidth,
                        ModConfigManager.getClient().staminaBackgroundHeight);
            case BAR_MAIN:
                return new ScreenRect(x + ModConfigManager.getClient().staminaBarXOffset,
                        y + ModConfigManager.getClient().staminaBarYOffset,
                        ModConfigManager.getClient().staminaBarWidth,
                        ModConfigManager.getClient().staminaBarHeight);
            case FOREGROUND_DETAIL:
                return new ScreenRect(x + ModConfigManager.getClient().staminaOverlayXOffset,
                        y + ModConfigManager.getClient().staminaOverlayYOffset,
                        ModConfigManager.getClient().staminaOverlayWidth,
                        ModConfigManager.getClient().staminaOverlayHeight);
            case TEXT:
                // Text area now positioned relative to complexRect, using staminaBarWidth/Height for its dimensions
                return new ScreenRect(x + ModConfigManager.getClient().staminaTextXOffset, 
                                      y + ModConfigManager.getClient().staminaTextYOffset, 
                                      ModConfigManager.getClient().staminaBarWidth, 
                                      ModConfigManager.getClient().staminaBarHeight);
            default:
                return new ScreenRect(0, 0, 0, 0);
        }
    }

    public static void render(GuiGraphics graphics, Player player, #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif ) {
        if (!Minecraft.getInstance().gameMode.canHurtPlayer() && !EditModeManager.isEditModeEnabled()) {
            return;
        }
        
        // Get the current stamina provider
        StaminaProvider staminaProvider = StaminaProviderManager.getCurrentProvider();
        if (staminaProvider == null) {
            return;
        }
        
        // Determine the bar values based on player state and config
        BarValues values = getBarValues(player, staminaProvider);
        
        // Track value changes for chunk creation
        updateChunkTracking(player, values, staminaProvider, #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif);
        
        // Determine fade behavior
        boolean shouldFade = shouldBarFade(player, values);
        setStaminaBarVisibility(!shouldFade || EditModeManager.isEditModeEnabled());

        if (!isStaminaBarVisible() && !EditModeManager.isEditModeEnabled() && (System.currentTimeMillis() - staminaBarDisabledStartTime) > RenderUtil.BAR_FADEOUT_DURATION) {
            return;
        }

        float currentAlphaForRender = getStaminaBarAlpha();
        if (EditModeManager.isEditModeEnabled() && !isStaminaBarVisible()) {
            currentAlphaForRender = 1.0f; // Show fully if in edit mode
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);

        ScreenRect complexRect = getScreenRect(player);

        // Load animation data and mask info from .mcmeta
        AnimationMetadata.AnimationData animData = AnimationMetadataCache.getStaminaBarAnimation();
        AnimationMetadata.MaskInfo maskInfo = AnimationMetadataCache.getStaminaBarMask();
        float ticks = player.tickCount + (#if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif);
        int animOffset = AnimationMetadata.calculateAnimationOffset(animData, ticks);
        boolean isRightAnchored = ModConfigManager.getClient().staminaBarAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT;

        if (ModConfigManager.getClient().enableStaminaBackground) {
            ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
            AnimationMetadata.ScalingInfo bgScaling = AnimationMetadataCache.getStaminaBackgroundScaling();
            NineSliceRenderer.renderWithScaling(graphics,
                    DynamicResourceBars.loc("textures/gui/stamina_background.png"),
                    bgScaling,
                    bgRect.x(), bgRect.y(), bgRect.width(), bgRect.height(), 256, 256
            );
        }

        ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
        renderBaseBar(graphics, player, values, staminaProvider,
                barRect,
                animOffset, isRightAnchored, maskInfo, animData);
        
        // Render fading chunks after the main bar
        renderFadingChunks(graphics, barRect, values, isRightAnchored, currentAlphaForRender, maskInfo, animData);

        // Show overlays only if the provider supports them and we're not showing mount health
        if (staminaProvider.shouldShowOverlays() && !values.isMounted) {
            if (PlatformUtil.isModLoaded("appleskin")) {
                ItemStack heldFood = getHeldFood(player);
                renderHungerRestoredOverlay(graphics, player, heldFood, barRect, #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif , animOffset, isRightAnchored, maskInfo, animData);
                renderSaturationOverlay(graphics, player, barRect, animOffset, isRightAnchored, maskInfo, animData);
            }

            if (PlatformUtil.isModLoaded("farmersdelight") && hasNourishmentEffect(player)) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                float pulseAlpha = TickHandler.getOverlayFlashAlpha();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulseAlpha);

                AnimationMetadata.ScalingInfo nourishmentScaling = AnimationMetadataCache.getNourishmentOverlayScaling();
                NineSliceRenderer.renderWithScaling(graphics,
                        DynamicResourceBars.loc("textures/gui/nourishment_overlay.png"),
                        nourishmentScaling,
                        barRect.x(), barRect.y(),
                        barRect.width(), barRect.height(),
                        256, 256
                );
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.disableBlend();
            }
        }

        if (ModConfigManager.getClient().enableStaminaForeground) {
            ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
            AnimationMetadata.ScalingInfo fgScaling = AnimationMetadataCache.getStaminaForegroundScaling();
            NineSliceRenderer.renderWithScaling(graphics,
                    DynamicResourceBars.loc("textures/gui/stamina_foreground.png"),
                    fgScaling,
                    fgRect.x(), fgRect.y(), fgRect.width(), fgRect.height(),
                    256, 256
            );
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (shouldRenderStaminaText(values.current, values.max, player, values.isMounted)) {
            ScreenRect textRect = getSubElementRect(SubElementType.TEXT, player);
            int textX = textRect.x() + (textRect.width() / 2);
            int textY = textRect.y() + (textRect.height() / 2);
            
            int color = getStaminaTextColor(values.current, values.max, values.isMounted);
            HorizontalAlignment alignment = ModConfigManager.getClient().staminaTextAlign;

            int baseX = textRect.x();
            if (alignment == HorizontalAlignment.CENTER) {
                baseX = textX;
            } else if (alignment == HorizontalAlignment.RIGHT) {
                baseX = textRect.x() + textRect.width();
            }

            RenderUtil.renderText(values.current, values.max, graphics, baseX, textY, color, alignment);
        }

        if (EditModeManager.isEditModeEnabled()) {
            DraggableElement currentBarType = DraggableElement.STAMINA_BAR;
            if (EditModeManager.getFocusedElement() == currentBarType) {
                int focusedBorderColor = 0xA0FFFF00;
                ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
                if (ModConfigManager.getClient().enableStaminaBackground) {
                    graphics.renderOutline(bgRect.x() - 1, bgRect.y() - 1, bgRect.width() + 2, bgRect.height() + 2, focusedBorderColor);
                }

                ScreenRect barRectOutline = getSubElementRect(SubElementType.BAR_MAIN, player);
                graphics.renderOutline(barRectOutline.x() - 1, barRectOutline.y() - 1, barRectOutline.width() + 2, barRectOutline.height() + 2, 0xA0FFA500);

                if (ModConfigManager.getClient().enableStaminaForeground) {
                    ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                    graphics.renderOutline(fgRect.x() - 1, fgRect.y() - 1, fgRect.width() + 2, fgRect.height() + 2, 0xA0FF00FF);
                }
                graphics.renderOutline(complexRect.x() - 2, complexRect.y() - 2, complexRect.width() + 4, complexRect.height() + 4, 0x80FFFFFF);
            } else {
                int borderColor = 0x80FFFFFF;
                graphics.renderOutline(complexRect.x() - 1, complexRect.y() - 1, complexRect.width() + 2, complexRect.height() + 2, borderColor);
            }
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
    
    private static void updateChunkTracking(Player player, BarValues values, StaminaProvider provider, float partialTicks) {
        // Clean up expired chunks and those covered by current fill
        Iterator<FadingChunk> it = fadingChunks.iterator();
        while (it.hasNext()) {
            FadingChunk chunk = it.next();
            // Remove if expired or if current value covers this chunk
            if (chunk.isExpired() || 
                (chunk.isMounted == values.isMounted && values.current >= chunk.endValue)) {
                it.remove();
            }
        }
        
        // Check if we need to create a new chunk
        if (previousValue > 0 && values.current < previousValue && 
            wasMounted == values.isMounted && previousMax == values.max) {
            
            // Get the texture and animation state
            String texture;
            if (values.isMounted) {
                float healthPercentage = values.current / values.max;
                texture = healthPercentage <= 0.2f ? "stamina_bar_critical" : "stamina_bar_mounted";
            } else {
                texture = provider.getBarTexture(player, values.current);
            }
            
            // Load animation data (all stamina bar variants share the same animation properties)
            AnimationMetadata.AnimationData animData = AnimationMetadataCache.getStaminaBarAnimation();
            float ticks = player.tickCount + partialTicks;
            int animOffset = AnimationMetadata.calculateAnimationOffset(animData, ticks);
            
            // Create chunk for the lost portion, clamping to 0 minimum
            float chunkStart = Math.max(0, values.current);
            fadingChunks.add(new FadingChunk(chunkStart, previousValue, values.max, texture, animOffset, values.isMounted));
        }
        
        // Update tracking values
        previousValue = values.current;
        previousMax = values.max;
        wasMounted = values.isMounted;
    }
    
    private static void renderFadingChunks(GuiGraphics graphics, ScreenRect barRect, BarValues currentValues,
                                          boolean isRightAnchored, float parentAlpha, AnimationMetadata.MaskInfo maskInfo,
                                          AnimationMetadata.AnimationData animData) {
        if (fadingChunks.isEmpty()) return;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        FillDirection fillDirection = ModConfigManager.getClient().staminaFillDirection;
        
        for (FadingChunk chunk : fadingChunks) {
            // Skip chunks that don't match current mount state
            if (chunk.isMounted != currentValues.isMounted) continue;
            
            float alpha = chunk.getAlpha() * parentAlpha;
            if (alpha <= 0) continue;
            
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            
            ResourceLocation chunkTexture = DynamicResourceBars.loc("textures/gui/" + chunk.texture + ".png");
            
            if (fillDirection == FillDirection.VERTICAL) {
                // Calculate heights for the chunk
                float startRatio = chunk.startValue / chunk.maxValue;
                float endRatio = chunk.endValue / chunk.maxValue;
                int startHeight = (int) (barRect.height() * startRatio);
                int endHeight = (int) (barRect.height() * endRatio);
                int chunkHeight = endHeight - startHeight;
                
                if (chunkHeight > 0) {
                    int yPos = barRect.y() + (barRect.height() - endHeight);
                    int textureVOffset = chunk.animOffset + (barRect.height() - endHeight);
                    
                    MaskRenderUtil.renderWithMask(
                        graphics, chunkTexture, maskInfo,
                        barRect.x(), yPos,
                        0, textureVOffset,
                        barRect.width(), chunkHeight,
                        animData.textureWidth, animData.textureHeight
                    );
                }
            } else { // HORIZONTAL
                // Calculate widths for the chunk
                float startRatio = chunk.startValue / chunk.maxValue;
                float endRatio = chunk.endValue / chunk.maxValue;
                int startWidth = (int) (barRect.width() * startRatio);
                int endWidth = (int) (barRect.width() * endRatio);
                int chunkWidth = endWidth - startWidth;
                
                if (chunkWidth > 0) {
                    int xPos, uOffset;
                    if (isRightAnchored) {
                        // Chunk position from right
                        xPos = barRect.x() + barRect.width() - endWidth;
                        uOffset = barRect.width() - endWidth;
                    } else {
                        // Chunk position from left
                        xPos = barRect.x() + startWidth;
                        uOffset = startWidth;
                    }
                    
                    MaskRenderUtil.renderWithMask(
                        graphics, chunkTexture, maskInfo,
                        xPos, barRect.y(),
                        uOffset, chunk.animOffset,
                        chunkWidth, barRect.height(),
                        animData.textureWidth, animData.textureHeight
                    );
                }
            }
        }
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    // Helper class to hold bar values
    private static class BarValues {
        float current;
        float max;
        boolean isMounted;
        
        BarValues(float current, float max, boolean isMounted) {
            this.current = current;
            this.max = max;
            this.isMounted = isMounted;
        }
    }
    
    // Clean method to determine bar values based on player state
    private static BarValues getBarValues(Player player, StaminaProvider provider) {
        // Check if mounted and mergeMountHealth is true
        if (ModConfigManager.getClient().mergeMountHealth && player.getVehicle() instanceof LivingEntity mount) {
            return new BarValues(mount.getHealth(), mount.getMaxHealth(), true);
        }
        
        // Otherwise use the stamina provider
        return new BarValues(provider.getCurrentStamina(player), provider.getMaxStamina(player), false);
    }
    
    // Clean method to determine fade behavior
    private static boolean shouldBarFade(Player player, BarValues values) {
        if (values.isMounted) {
            return ModConfigManager.getClient().fadeHealthWhenFull && values.current >= values.max;
        } else {
            return ModConfigManager.getClient().fadeStaminaWhenFull && values.current >= values.max;
        }
    }

    private static void renderBaseBar(GuiGraphics graphics, Player player, BarValues values, StaminaProvider provider,
                                      ScreenRect barAreaRect,
                                      int animOffset, boolean isRightAnchored, AnimationMetadata.MaskInfo maskInfo,
                                      AnimationMetadata.AnimationData animData) {
        String barTextureStr;
        if (values.isMounted) {
            // Mount health uses special textures
            float healthPercentage = values.current / values.max;
            if (healthPercentage <= 0.2f) {
                barTextureStr = "stamina_bar_critical";
            } else {
                barTextureStr = "stamina_bar_mounted";
            }
        } else {
            // Use provider's texture
            barTextureStr = provider.getBarTexture(player, values.current);
        }
        
        ResourceLocation barTexture = DynamicResourceBars.loc("textures/gui/" + barTextureStr + ".png");
        
        int totalBarWidth = barAreaRect.width();
        int barHeight = barAreaRect.height();
        float currentStaminaRatio = (values.max == 0) ? 0.0f : (values.current / values.max);

        FillDirection fillDirection = ModConfigManager.getClient().staminaFillDirection;

        if (fillDirection == FillDirection.VERTICAL) {
            int partialBarHeight = (int) (barHeight * currentStaminaRatio);
            if (partialBarHeight <= 0 && values.current > 0) partialBarHeight = 1;
            if (partialBarHeight > barHeight) partialBarHeight = barHeight;

            int barX = barAreaRect.x();
            int barY = barAreaRect.y() + (barHeight - partialBarHeight); // Fill from bottom up
            // Adjust texture V offset to draw the correct part of the texture
            int textureVOffset = animOffset + (barHeight - partialBarHeight);

            if (partialBarHeight > 0) {
                MaskRenderUtil.renderWithMask(
                        graphics, barTexture, maskInfo,
                        barX, barY,
                        0, textureVOffset, // Use 0 for U, adjusted V for vertical fill
                        totalBarWidth, partialBarHeight, // Use full width, partial height
                        animData.textureWidth, animData.textureHeight
                );
            }
        } else { // HORIZONTAL
            int partialBarWidth = (int) (totalBarWidth * currentStaminaRatio);
            if (partialBarWidth <= 0 && values.current > 0) partialBarWidth = 1;
            if (partialBarWidth > totalBarWidth) partialBarWidth = totalBarWidth;

            int barRenderX = barAreaRect.x();
            int barRenderY = barAreaRect.y();
            int uTexOffset = 0; // Default for left-anchored

            if (isRightAnchored) {
                barRenderX = barAreaRect.x() + totalBarWidth - partialBarWidth;
                uTexOffset = totalBarWidth - partialBarWidth; // Sample the right part of the texture
            }
            if (uTexOffset < 0) uTexOffset = 0; // Prevent negative texture offset

            if (partialBarWidth > 0) {
                MaskRenderUtil.renderWithMask(
                        graphics, barTexture, maskInfo,
                        barRenderX, barRenderY,
                        uTexOffset, animOffset, // Use calculated uTexOffset
                        partialBarWidth, barHeight,
                        animData.textureWidth, animData.textureHeight
                );
            }
        }
    }

    private static boolean shouldRenderStaminaText(float currentValue, float maxValue, Player player, boolean isMounted) {
        TextBehavior textBehavior = isMounted ? 
            ModConfigManager.getClient().showHealthText : 
            ModConfigManager.getClient().showStaminaText;

        if (EditModeManager.isEditModeEnabled()) {
            if (textBehavior == TextBehavior.ALWAYS || textBehavior == TextBehavior.WHEN_NOT_FULL) {
                return true;
            }
        }
        if (textBehavior == TextBehavior.NEVER) {
            return false;
        }
        if (textBehavior == TextBehavior.ALWAYS) {
            return true;
        }
        
        // WHEN_NOT_FULL logic
        if (isMounted) {
            // Handle mount health separately
            boolean isFull = currentValue >= maxValue;
            if (isFull) {
                // Check if just became full or values changed
                if (lastMountHealth < maxValue || lastMountMaxHealth != maxValue || lastMountHealth == -1) {
                    fullMountHealthStartTime = System.currentTimeMillis();
                }
                lastMountHealth = currentValue;
                lastMountMaxHealth = maxValue;
                // Show for a short duration after becoming full
                return (System.currentTimeMillis() - fullMountHealthStartTime) < RenderUtil.TEXT_DISPLAY_DURATION;
            } else {
                lastMountHealth = currentValue;
                lastMountMaxHealth = maxValue;
                return true; // Not full, so show
            }
        } else {
            // Handle stamina normally
            boolean isFull = currentValue >= maxValue;
            if (isFull) {
                if (lastStamina < maxValue || lastStamina == -1) { // Just became full or first check
                    fullStaminaStartTime = System.currentTimeMillis();
                }
                lastStamina = currentValue;
                // Show for a short duration after becoming full
                return (System.currentTimeMillis() - fullStaminaStartTime) < RenderUtil.TEXT_DISPLAY_DURATION;
            } else {
                lastStamina = currentValue;
                return true; // Not full, so show
            }
        }
    }

    private static int getStaminaTextColor(float currentValue, float maxValue, boolean isMounted) {
        TextBehavior textBehavior = isMounted ? 
            ModConfigManager.getClient().showHealthText : 
            ModConfigManager.getClient().showStaminaText;
        ClientConfig config = ModConfigManager.getClient();
        int baseColor;
        int alpha;

        if (isMounted) {
            baseColor = config.healthTextColor & 0xFFFFFF;
            alpha = config.healthTextOpacity;
        } else {
            baseColor = config.staminaTextColor & 0xFFFFFF;
            alpha = config.staminaTextOpacity;
        }

        if (textBehavior == TextBehavior.WHEN_NOT_FULL && currentValue >= maxValue) {
            long timeSinceFull;
            if (isMounted) {
                timeSinceFull = System.currentTimeMillis() - fullMountHealthStartTime;
            } else {
                timeSinceFull = System.currentTimeMillis() - fullStaminaStartTime;
            }
            alpha = (int)(alpha * (RenderUtil.calculateTextAlpha(timeSinceFull) / (float)RenderUtil.BASE_TEXT_ALPHA));
        }

        alpha = (int) (alpha * getStaminaBarAlpha()); // Modulate with bar alpha
        alpha = Math.max(10, Math.min(255, alpha)); // Ensure minimum visibility
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

    private static void renderSaturationOverlay(GuiGraphics graphics, Player player, ScreenRect barRect, int animOffset, boolean isRightAnchored, AnimationMetadata.MaskInfo maskInfo, AnimationMetadata.AnimationData animData) {
        if (!PlatformUtil.isModLoaded("appleskin")) {
            return;
        }

        float saturation = player.getFoodData().getSaturationLevel();
        if (saturation <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float saturationPercent = Math.min(1.0f, saturation / 20f);
        FillDirection fillDirection = ModConfigManager.getClient().staminaFillDirection;

        // Use pulsing opacity instead of frame animation
        float pulseAlpha = 0.5f + (TickHandler.getOverlayFlashAlpha() * 0.5f); // Range from 0.5 to 1.0
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulseAlpha);
        
        ResourceLocation overlayTexture = DynamicResourceBars.loc("textures/gui/protection_overlay.png");

        if (fillDirection == FillDirection.VERTICAL) {
            int overlayHeight = (int) (barRect.height() * saturationPercent);
            if (overlayHeight > 0) {
                MaskRenderUtil.renderWithMask(
                        graphics, overlayTexture, maskInfo,
                        barRect.x(),
                        barRect.y() + (barRect.height() - overlayHeight),
                        0, 0,
                        barRect.width(), overlayHeight,
                        animData.textureWidth, animData.textureHeight
                );
            }
        } else { // HORIZONTAL
            int overlayWidth = (int) (barRect.width() * saturationPercent);
            if (overlayWidth > 0) {
                int xPos;
                int uTexOffset;
                if (isRightAnchored) {
                    xPos = barRect.x() + barRect.width() - overlayWidth;
                    uTexOffset = barRect.width() - overlayWidth; // Sample rightmost part of the texture
                } else {
                    xPos = barRect.x();
                    uTexOffset = 0; // Sample leftmost part of the texture
                }
                if (uTexOffset < 0) uTexOffset = 0;

                MaskRenderUtil.renderWithMask(
                        graphics, overlayTexture, maskInfo,
                        xPos, barRect.y(),
                        uTexOffset, 0, // Use calculated uTexOffset, vOffset usually 0 for horizontal overlays unless animated differently
                        overlayWidth, barRect.height(),
                        animData.textureWidth, animData.textureHeight
                );
            }
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static void renderHungerRestoredOverlay(GuiGraphics graphics, Player player, ItemStack heldFood,
                                                    ScreenRect barRect, float partialTicks, int animOffset, boolean isRightAnchored, AnimationMetadata.MaskInfo maskInfo, AnimationMetadata.AnimationData animData) {
        if (!PlatformUtil.isModLoaded("appleskin")) {
            return;
        }

        AppleSkinCompat.FoodData foodData = AppleSkinCompat.getFoodValues(heldFood, player);
        if (foodData.isEmpty()) {
            return;
        }

        float currentHunger = player.getFoodData().getFoodLevel();
        float restoredHunger = Math.min(20f, currentHunger + foodData.hunger);

        if (restoredHunger <= currentHunger) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, TickHandler.getOverlayFlashAlpha());

        FillDirection fillDirection = ModConfigManager.getClient().staminaFillDirection;
        
        // Get the texture that would apply at the restored hunger level
        // We need to get the food provider specifically since this is a food overlay
        StaminaProvider provider = StaminaProviderManager.getCurrentProvider();
        String barTextureStr = "stamina_bar"; // default
        
        // Only use the provider's texture if it's the food provider
        if (provider instanceof dev.muon.dynamic_resource_bars.compat.FoodStaminaProvider) {
            barTextureStr = provider.getBarTexture(player, restoredHunger);
        }
        
        ResourceLocation barTexture = DynamicResourceBars.loc("textures/gui/" + barTextureStr + ".png");

        if (fillDirection == FillDirection.VERTICAL) {
            int currentHeight = (int) (barRect.height() * (currentHunger / 20f));
            int restoredHeight = (int) (barRect.height() * (restoredHunger / 20f));
            int overlayHeight = restoredHeight - currentHeight;

            if (overlayHeight > 0) {
                int yPos = barRect.y() + (barRect.height() - restoredHeight);
                int textureVOffset = animOffset + (barRect.height() - restoredHeight);

                MaskRenderUtil.renderWithMask(
                        graphics, barTexture, maskInfo,
                        barRect.x(), yPos,
                        0, textureVOffset,
                        barRect.width(), overlayHeight,
                        animData.textureWidth, animData.textureHeight
                );
            }
        } else { // HORIZONTAL
            int currentWidth = (int) (barRect.width() * (currentHunger / 20f));
            int restoredWidth = (int) (barRect.width() * (restoredHunger / 20f));
            int overlayWidth = restoredWidth - currentWidth;

            if (overlayWidth > 0) {
                int xDrawPos;
                int uTexOffset;

                if (isRightAnchored) {
                    xDrawPos = barRect.x() + barRect.width() - restoredWidth;
                    uTexOffset = barRect.width() - restoredWidth; 
                } else {
                    xDrawPos = barRect.x() + currentWidth;
                    uTexOffset = currentWidth;
                }
                if (uTexOffset < 0) uTexOffset = 0;

                MaskRenderUtil.renderWithMask(
                        graphics, barTexture, maskInfo,
                        xDrawPos, barRect.y(),
                        uTexOffset, animOffset, // Use the calculated uTexOffset
                        overlayWidth, barRect.height(),
                        animData.textureWidth, animData.textureHeight
                );
            }
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static ItemStack getHeldFood(Player player) {
        ItemStack mainHand = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (AppleSkinCompat.canConsume(mainHand, player)) {
            return mainHand;
        }

        ItemStack offHand = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);
        if (AppleSkinCompat.canConsume(offHand, player)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static boolean hasNourishmentEffect(Player player) {
            #if UPTO_20_1
                // 1.20.1 Forge: RegistryObject<MobEffect>
                // 1.20.1 Fabric: Supplier<MobEffect>
            var nourishmentEffect = ModEffects.NOURISHMENT.get();
            return player.hasEffect(nourishmentEffect);
            #elif NEWER_THAN_20_1
                // 1.21.1 Fabric/NeoForge - Holder<MobEffect>
            var nourishmentEffect = ModEffects.NOURISHMENT;
            return player.hasEffect(nourishmentEffect);
            #else
        return false;
            #endif
    }

    public static boolean isVampire(Player player) {
        #if UPTO_20_1 && FABRIC
            if (PlatformUtil.isModLoaded("bewitchment")) {
                return BewitchmentCompat.isVampire(player);
            }
        #endif
            // TODO: More vampire transformation mods here
        return false;
    }
}