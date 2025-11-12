package dev.muon.dynamic_resource_bars.render;


import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.muon.dynamic_resource_bars.compat.AppleSkinCompat;

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import vectorwing.farmersdelight.common.registry.ModEffects;

public class HealthBarRenderer {

    private static float lastHealth = -1;
    private static long fullHealthStartTime = 0;
    private static boolean healthBarSetVisible = true; // Default to visible
    private static long healthBarDisabledStartTime = 0L;
    
    // Fadeout chunking system
    private static final long CHUNK_FADEOUT_DURATION = RenderUtil.BAR_FADEOUT_DURATION / 3;
    private static final List<FadingChunk> fadingChunks = new ArrayList<>();
    private static float previousHealth = -1;
    private static float previousMaxHealth = -1;
    
    private static class FadingChunk {
        final float startValue;    // Where the chunk starts (in health units)
        final float endValue;      // Where it ends
        final float maxValue;      // Max value at time of creation
        final long creationTime;   // When this chunk started fading
        final String texture;      // Which bar texture to use
        final int animOffset;      // Animation frame at creation
        
        FadingChunk(float startValue, float endValue, float maxValue, String texture, int animOffset) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.maxValue = maxValue;
            this.creationTime = System.currentTimeMillis();
            this.texture = texture;
            this.animOffset = animOffset;
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

    private enum BarType {
        NORMAL("health_bar"),
        POISON("health_bar_poisoned"),
        WITHER("health_bar_withered"),
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
        Position healthPos = HUDPositioning.getPositionFromAnchor(ModConfigManager.getClient().healthBarAnchor)
                .offset(ModConfigManager.getClient().healthTotalXOffset, ModConfigManager.getClient().healthTotalYOffset);
        int backgroundWidth = ModConfigManager.getClient().healthBackgroundWidth;
        int backgroundHeight = ModConfigManager.getClient().healthBackgroundHeight;
        return new ScreenRect(healthPos.x(), healthPos.y(), backgroundWidth, backgroundHeight);
    }

    public static ScreenRect getSubElementRect(SubElementType type, Player player) {
        ScreenRect complexRect = getScreenRect(player);
        if (complexRect == null || complexRect.width() == 0 && complexRect.height() == 0) return new ScreenRect(0,0,0,0);

        int x = complexRect.x();
        int y = complexRect.y();

        switch (type) {
            case BACKGROUND:
                return new ScreenRect(x + ModConfigManager.getClient().healthBackgroundXOffset, 
                                      y + ModConfigManager.getClient().healthBackgroundYOffset, 
                                      ModConfigManager.getClient().healthBackgroundWidth, 
                                      ModConfigManager.getClient().healthBackgroundHeight);
            case BAR_MAIN:
                return new ScreenRect(x + ModConfigManager.getClient().healthBarXOffset, 
                                      y + ModConfigManager.getClient().healthBarYOffset, 
                                      ModConfigManager.getClient().healthBarWidth, 
                                      ModConfigManager.getClient().healthBarHeight);
            case FOREGROUND:
                return new ScreenRect(x + ModConfigManager.getClient().healthOverlayXOffset, 
                                      y + ModConfigManager.getClient().healthOverlayYOffset, 
                                      ModConfigManager.getClient().healthOverlayWidth,
                                      ModConfigManager.getClient().healthOverlayHeight);
            case TEXT:
                // Text area now positioned relative to complexRect, using healthBarWidth/Height for its dimensions
                return new ScreenRect(x + ModConfigManager.getClient().healthTextXOffset, 
                                      y + ModConfigManager.getClient().healthTextYOffset, 
                                      ModConfigManager.getClient().healthBarWidth, 
                                      ModConfigManager.getClient().healthBarHeight);
            case ABSORPTION_TEXT:
                // Absorption text positioned relative to complexRect, with fixed width and healthBarHeight
                return new ScreenRect(x + ModConfigManager.getClient().healthAbsorptionTextXOffset, 
                                      y + ModConfigManager.getClient().healthAbsorptionTextYOffset, 
                                      50, // Approximate width for absorption text
                                      ModConfigManager.getClient().healthBarHeight); // Height based on bar height
            default:
                return new ScreenRect(0,0,0,0);
        }
    }

    public static void render(GuiGraphics graphics, Player player, float maxHealth, float actualHealth, int absorptionAmount,
            #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif) {

        // Track value changes for chunk creation
        updateChunkTracking(player, actualHealth, maxHealth, #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif);

        // Override hideWhenFull if player has absorption. TODO: Maybe set this as config value
        boolean shouldFade = ModConfigManager.getClient().fadeHealthWhenFull && actualHealth >= maxHealth && absorptionAmount == 0;
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

        int backgroundWidth = ModConfigManager.getClient().healthBackgroundWidth;
        int backgroundHeight = ModConfigManager.getClient().healthBackgroundHeight;

        if (ModConfigManager.getClient().enableHealthBackground) {
            ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
            ResourceLocation bgTexture = DynamicResourceBars.loc("textures/gui/health_background.png");
            AnimationMetadata.ScalingInfo bgScaling = AnimationMetadataCache.getHealthBackgroundScaling();
            AnimationMetadata.TextureDimensions bgDims = AnimationMetadataCache.getTextureDimensions(bgTexture);
            NineSliceRenderer.renderWithScaling(graphics, bgTexture, bgScaling,
                    bgRect.x(), bgRect.y(), bgRect.width(), bgRect.height(), 
                    bgDims.width, bgDims.height
            );
        }

        // Load animation data from .mcmeta
        AnimationMetadata.AnimationData animData = AnimationMetadataCache.getHealthBarAnimation();
        float ticks = player.tickCount + (#if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif);
        int animOffset = AnimationMetadata.calculateAnimationOffset(animData, ticks);
        
        ScreenRect mainBarRect = getSubElementRect(SubElementType.BAR_MAIN, player);
        boolean isRightAnchored = ModConfigManager.getClient().healthBarAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT;
        renderBaseBar(graphics, player, maxHealth, actualHealth, 
                      mainBarRect.x(), mainBarRect.y(), mainBarRect.width(), mainBarRect.height(), 
                      0, 0,
                      animOffset, isRightAnchored, animData);
        
        // Render fading chunks after the main bar
        renderFadingChunks(graphics, mainBarRect, actualHealth, maxHealth, isRightAnchored, currentAlphaForRender, animData);

        // Render AppleSkin estimated health overlay
        if (PlatformUtil.isModLoaded("appleskin")) {
            ItemStack heldFood = getHeldFood(player);
            if (!heldFood.isEmpty()) {
                renderHealthRestoredOverlay(graphics, player, heldFood, actualHealth, maxHealth, mainBarRect, animOffset, isRightAnchored, animData);
            }
        }

        renderBarOverlays(graphics, player, absorptionAmount,
                          mainBarRect.x(), mainBarRect.y(), mainBarRect.width(), mainBarRect.height(), 
                          0,0);

        renderBackgroundOverlays(graphics, player, xPos, yPos, backgroundWidth, backgroundHeight);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Update text rendering to use draggable position
        if (shouldRenderHealthText(actualHealth, maxHealth, player)) {
            ScreenRect textRect = getSubElementRect(SubElementType.TEXT, player);
            int textX = textRect.x() + (textRect.width() / 2);
            int textY = textRect.y() + (textRect.height() / 2);
            
            int color = getHealthTextColor(actualHealth, maxHealth);
            HorizontalAlignment alignment = ModConfigManager.getClient().healthTextAlign;

            int baseX = textRect.x();
            if (alignment == HorizontalAlignment.CENTER) {
                baseX = textX;
            } else if (alignment == HorizontalAlignment.RIGHT) {
                baseX = textRect.x() + textRect.width();
            }

            RenderUtil.renderText(actualHealth, maxHealth,
                    graphics, baseX, textY, color, alignment);
        }

        if (absorptionAmount > 0 || EditModeManager.isEditModeEnabled()) {
            String absorptionText = "+" + (EditModeManager.isEditModeEnabled() && absorptionAmount == 0 ? "8" : absorptionAmount);
            
            ScreenRect absorptionRect = getSubElementRect(SubElementType.ABSORPTION_TEXT, player);
            int absorptionTextX = absorptionRect.x();
            int absorptionTextY = absorptionRect.y() + (absorptionRect.height() / 2);

            ClientConfig config = ModConfigManager.getClient();
            int baseAbsorptionColor = config.healthTextColor & 0xFFFFFF;
            int absorptionAlpha = (int) (config.healthTextOpacity * currentAlphaForRender);
            absorptionAlpha = Math.max(10, Math.min(255, absorptionAlpha)); // Ensure visibility
            int absorptionFinalColor = (absorptionAlpha << 24) | baseAbsorptionColor;

            RenderUtil.renderAdditionText(absorptionText, graphics, absorptionTextX, absorptionTextY, 
                    absorptionFinalColor);
        }

        if (EditModeManager.isEditModeEnabled()) {
            DraggableElement currentBarType = DraggableElement.HEALTH_BAR;
            if (EditModeManager.getFocusedElement() == currentBarType) {
                int focusedBorderColor = 0xA0FFFF00;
                ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
                graphics.renderOutline(bgRect.x()-1, bgRect.y()-1, bgRect.width()+2, bgRect.height()+2, focusedBorderColor);
                
                ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
                graphics.renderOutline(barRect.x()-1, barRect.y()-1, barRect.width()+2, barRect.height()+2, 0xA000FF00);
                
                if (ModConfigManager.getClient().enableHealthForeground) {
                    ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND, player);
                    graphics.renderOutline(fgRect.x()-1, fgRect.y()-1, fgRect.width()+2, fgRect.height()+2, 0xA0FF00FF);
                }
                
                // Outline for absorption text
                ScreenRect absorptionRect = getSubElementRect(SubElementType.ABSORPTION_TEXT, player);
                if (absorptionRect != null && absorptionRect.width() > 0 && absorptionRect.height() > 0) {
                    graphics.renderOutline(absorptionRect.x() - 1, absorptionRect.y() - 1,
                                           absorptionRect.width() + 2, absorptionRect.height() + 2,
                                           0x60FFFFFF); // Same semi-transparent white as other text
                }
                
                graphics.renderOutline(complexRect.x()-2, complexRect.y()-2, complexRect.width()+4, complexRect.height()+4, 0x80FFFFFF);
            } else {
                int borderColor = 0x80FFFFFF;
                graphics.renderOutline(complexRect.x()-1, complexRect.y()-1, complexRect.width()+2, complexRect.height()+2, borderColor);
            }
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Reset shader color
        RenderSystem.disableBlend(); // Ensure blend is disabled after rendering this bar
    }
    
    private static void updateChunkTracking(Player player, float currentHealth, float maxHealth, float partialTicks) {
        // Clean up expired chunks and those covered by current fill
        Iterator<FadingChunk> it = fadingChunks.iterator();
        while (it.hasNext()) {
            FadingChunk chunk = it.next();
            // Remove if expired or if current health covers this chunk
            if (chunk.isExpired() || currentHealth >= chunk.endValue) {
                it.remove();
            }
        }
        
        // Check if we need to create a new chunk
        if (previousHealth > 0 && currentHealth < previousHealth && previousMaxHealth == maxHealth) {
            // Get the texture based on player state
            BarType barType = BarType.fromPlayerState(player);
            String texture = barType.getTexture();
            
            // Load animation data (all health bar variants share the same animation properties)
            AnimationMetadata.AnimationData animData = AnimationMetadataCache.getHealthBarAnimation();
            float ticks = player.tickCount + partialTicks;
            int animOffset = AnimationMetadata.calculateAnimationOffset(animData, ticks);
            
            // Create chunk for the lost portion, clamping to 0 minimum
            float chunkStart = Math.max(0, currentHealth);
            fadingChunks.add(new FadingChunk(chunkStart, previousHealth, maxHealth, texture, animOffset));
        }
        
        // Update tracking values
        previousHealth = currentHealth;
        previousMaxHealth = maxHealth;
    }
    
    private static void renderFadingChunks(GuiGraphics graphics, ScreenRect barRect, float currentHealth, float maxHealth,
                                          boolean isRightAnchored, float parentAlpha,
                                          AnimationMetadata.AnimationData animData) {
        if (fadingChunks.isEmpty()) return;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        FillDirection fillDirection = ModConfigManager.getClient().healthFillDirection;
        
        for (FadingChunk chunk : fadingChunks) {
            float alpha = chunk.getAlpha() * parentAlpha;
            if (alpha <= 0) continue;
            
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            
            ResourceLocation chunkTexture = DynamicResourceBars.loc("textures/gui/" + chunk.texture + ".png");
            
            if (fillDirection == FillDirection.VERTICAL) {
                // Calculate heights for the chunk
                float startRatio = Math.max(0.0f, Math.min(1.0f, chunk.startValue / chunk.maxValue));
                float endRatio = Math.max(0.0f, Math.min(1.0f, chunk.endValue / chunk.maxValue));
                int startHeight = (int) (barRect.height() * startRatio);
                int endHeight = (int) (barRect.height() * endRatio);
                int chunkHeight = endHeight - startHeight;
                
                if (chunkHeight > 0) {
                    int yPos = barRect.y() + (barRect.height() - endHeight);
                    int textureVOffset = chunk.animOffset + (barRect.height() - endHeight);

                    RenderUtil.blitWithBinding(graphics, chunkTexture,
                        barRect.x(), yPos,
                        0, textureVOffset,
                        barRect.width(), chunkHeight,
                        animData.textureWidth, animData.textureHeight
                    );
                }
            } else { // HORIZONTAL
                // Calculate widths for the chunk
                float startRatio = Math.max(0.0f, Math.min(1.0f, chunk.startValue / chunk.maxValue));
                float endRatio = Math.max(0.0f, Math.min(1.0f, chunk.endValue / chunk.maxValue));
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

                    RenderUtil.blitWithBinding(graphics, chunkTexture,
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

    private static void renderBaseBar(GuiGraphics graphics, Player player, float maxHealth, float actualHealth,
                                      int barAbsX, int barAbsY, int barAbsWidth, int barAbsHeight, 
                                      int barXOffsetWithinTexture, int barYOffsetWithinTexture,
                                      int animOffset, boolean isRightAnchored,
                                      AnimationMetadata.AnimationData animData) {
        BarType barType = BarType.fromPlayerState(player);
        ResourceLocation barTexture = DynamicResourceBars.loc("textures/gui/" + barType.getTexture() + ".png");
        float currentHealthRatio = (maxHealth == 0) ? 0.0f : Math.max(0.0f, Math.min(1.0f, actualHealth / maxHealth));

        FillDirection fillDirection = ModConfigManager.getClient().healthFillDirection;

        if (fillDirection == FillDirection.VERTICAL) {
            int partialBarHeight = (int) (barAbsHeight * currentHealthRatio);
            if (partialBarHeight <= 0 && actualHealth > 0) partialBarHeight = 1;
            if (partialBarHeight > 0) {
                RenderUtil.blitWithBinding(graphics, barTexture,
                        barAbsX, barAbsY + (barAbsHeight - partialBarHeight), // Adjust Y to fill from bottom up
                        barXOffsetWithinTexture, animOffset + barYOffsetWithinTexture + (barAbsHeight - partialBarHeight), // Adjust texture V to match
                        barAbsWidth, partialBarHeight, // Use full width, partial height
                        animData.textureWidth, animData.textureHeight
                );
            }
        } else { // HORIZONTAL 
            int partialBarWidth = (int) (barAbsWidth * currentHealthRatio);
            if (partialBarWidth <= 0 && actualHealth > 0) partialBarWidth = 1;
            if (partialBarWidth > 0) {
                int renderBarX = barAbsX;
                int uTexOffset = barXOffsetWithinTexture;

                if (isRightAnchored) {
                    renderBarX = barAbsX + barAbsWidth - partialBarWidth;
                    uTexOffset = barXOffsetWithinTexture + barAbsWidth - partialBarWidth;
                }

                RenderUtil.blitWithBinding(graphics, barTexture,
                        renderBarX, barAbsY,
                        uTexOffset, animOffset + barYOffsetWithinTexture,
                        partialBarWidth, barAbsHeight,
                        animData.textureWidth, animData.textureHeight
                );
            }
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
            float pulseAlpha = 0.5f + (TickHandler.getOverlayFlashAlpha() * 0.5f);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulseAlpha);
            
            ResourceLocation absorptionTexture = DynamicResourceBars.loc("textures/gui/absorption_overlay.png");
            AnimationMetadata.ScalingInfo absorptionScaling = AnimationMetadataCache.getAbsorptionOverlayScaling();
            AnimationMetadata.TextureDimensions absorptionDims = AnimationMetadataCache.getTextureDimensions(absorptionTexture);
            NineSliceRenderer.renderWithScaling(graphics, absorptionTexture, absorptionScaling,
                    barAbsX + barXOffset, barAbsY + barYOffset,
                    barAbsWidth, barAbsHeight,
                    absorptionDims.width, absorptionDims.height
            );
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        if (PlatformUtil.isModLoaded("farmersdelight") && hasComfortEffect(player)) {
            if (player.getFoodData().getSaturationLevel() == 0.0F && player.isHurt() && !player.hasEffect(MobEffects.REGENERATION)) {
                float pulseAlpha = TickHandler.getOverlayFlashAlpha();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulseAlpha);
                
                ResourceLocation comfortTexture = DynamicResourceBars.loc("textures/gui/comfort_overlay.png");
                AnimationMetadata.ScalingInfo comfortScaling = AnimationMetadataCache.getComfortOverlayScaling();
                AnimationMetadata.TextureDimensions comfortDims = AnimationMetadataCache.getTextureDimensions(comfortTexture);
                NineSliceRenderer.renderWithScaling(graphics, comfortTexture, comfortScaling,
                        barAbsX + barXOffset, barAbsY + barYOffset,
                        barAbsWidth, barAbsHeight,
                        comfortDims.width, comfortDims.height
                );
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
        }

        if (player.hasEffect(MobEffects.REGENERATION)) {
            float pulseAlpha = TickHandler.getOverlayFlashAlpha();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulseAlpha);
            
            ResourceLocation regenTexture = DynamicResourceBars.loc("textures/gui/regeneration_overlay.png");
            AnimationMetadata.ScalingInfo regenScaling = AnimationMetadataCache.getRegenerationOverlayScaling();
            AnimationMetadata.TextureDimensions regenDims = AnimationMetadataCache.getTextureDimensions(regenTexture);
            NineSliceRenderer.renderWithScaling(graphics, regenTexture, regenScaling,
                    barAbsX + barXOffset, barAbsY + barYOffset,
                    barAbsWidth, barAbsHeight,
                    regenDims.width, regenDims.height
            );
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        
        RenderSystem.disableBlend();
    }

    private static void renderBackgroundOverlays(GuiGraphics graphics, Player player,
                                             int complexX, int complexY, int backgroundWidth, int backgroundHeight) {
        if (player.level().getLevelData().isHardcore()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            ResourceLocation hardcoreTexture = DynamicResourceBars.loc("textures/gui/hardcore_overlay.png");
            AnimationMetadata.ScalingInfo hardcoreScaling = AnimationMetadataCache.getHardcoreOverlayScaling();
            AnimationMetadata.TextureDimensions hardcoreDims = AnimationMetadataCache.getTextureDimensions(hardcoreTexture);
            NineSliceRenderer.renderWithScaling(graphics, hardcoreTexture, hardcoreScaling,
                    complexX, complexY,
                    backgroundWidth, backgroundHeight,
                    hardcoreDims.width, hardcoreDims.height
            );
            RenderSystem.disableBlend();
        }

        float wetScale = getWetnessScale(player);
        if (wetScale > 0) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, wetScale);
            ResourceLocation wetnessTexture = DynamicResourceBars.loc("textures/gui/wetness_overlay.png");
            AnimationMetadata.ScalingInfo wetnessScaling = AnimationMetadataCache.getWetnessOverlayScaling();
            AnimationMetadata.TextureDimensions wetnessDims = AnimationMetadataCache.getTextureDimensions(wetnessTexture);
            NineSliceRenderer.renderWithScaling(graphics, wetnessTexture, wetnessScaling,
                    complexX, complexY,
                    backgroundWidth, backgroundHeight,
                    wetnessDims.width, wetnessDims.height
            );
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }

        if (ModConfigManager.getClient().enableHealthForeground) {
            ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND, player);
            ResourceLocation fgTexture = DynamicResourceBars.loc("textures/gui/health_foreground.png");
            AnimationMetadata.ScalingInfo fgScaling = AnimationMetadataCache.getHealthForegroundScaling();
            AnimationMetadata.TextureDimensions fgDims = AnimationMetadataCache.getTextureDimensions(fgTexture);
            NineSliceRenderer.renderWithScaling(graphics, fgTexture, fgScaling,
                    fgRect.x(), fgRect.y(), fgRect.width(), fgRect.height(),
                    fgDims.width, fgDims.height
            );
        }
    }

    private static void renderTemperatureOverlay(GuiGraphics graphics, float tempScale,
                                                 int xPos, int yPos, int barWidth, int barHeight,
                                                 int barXOffset, int barYOffset) {
        if (tempScale > 0) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, getHealthBarAlpha()); // Apply alpha to heat overlay
            int heatWidth = (int) (barWidth * tempScale);
            ResourceLocation heatTexture = DynamicResourceBars.loc("textures/gui/heat_overlay.png");
            AnimationMetadata.ScalingInfo heatScaling = AnimationMetadataCache.getHeatOverlayScaling();
            AnimationMetadata.TextureDimensions heatDims = AnimationMetadataCache.getTextureDimensions(heatTexture);
            NineSliceRenderer.renderWithScaling(graphics, heatTexture, heatScaling,
                    xPos + barXOffset, yPos + barYOffset,
                    heatWidth, barHeight,
                    heatDims.width, heatDims.height
            );
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Reset after heat overlay
        } else if (tempScale < 0) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, getHealthBarAlpha()); // Apply alpha to cold overlay
            int coldWidth = (int) (barWidth * -tempScale);
            ResourceLocation coldTexture = DynamicResourceBars.loc("textures/gui/cold_overlay.png");
            AnimationMetadata.ScalingInfo coldScaling = AnimationMetadataCache.getColdOverlayScaling();
            AnimationMetadata.TextureDimensions coldDims = AnimationMetadataCache.getTextureDimensions(coldTexture);
            NineSliceRenderer.renderWithScaling(graphics, coldTexture, coldScaling,
                    xPos + barXOffset, yPos + barYOffset,
                    coldWidth, barHeight,
                    coldDims.width, coldDims.height
            );
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Reset after cold overlay
        }
    }

    private static float getWetnessScale(Player player) {

        if (PlatformUtil.isModLoaded("thermoo")) {
            try {
                Method getWetScale = player.getClass().getMethod("thermoo$getSoakedScale");
                return (float) getWetScale.invoke(player);
            } catch (Exception e) {
                return 0.0f;
            }
        }
        return 0.0f;
    }

    private static boolean isScorched(Player player) {

        if (PlatformUtil.isModLoaded("thermoo")) {
            try {
                Method getMaxTemp = player.getClass().getMethod("thermoo$getMaxTemperature");
                Method getTemp = player.getClass().getMethod("thermoo$getTemperature");

                int maxTemperature = (int) getMaxTemp.invoke(player);
                int temperature = (int) getTemp.invoke(player);

                return temperature > 0.5 && temperature >= maxTemperature - 1;
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    private static boolean isFrozen(Player player) {
        if (player.isFullyFrozen()) {
            return true;
        }

        if (PlatformUtil.isModLoaded("thermoo")) {
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
        TextBehavior behavior = ModConfigManager.getClient().showHealthText;
        ClientConfig config = ModConfigManager.getClient();
        
        // Use configured text color instead of hardcoded white
        int baseColor = config.healthTextColor & 0xFFFFFF;

        int alpha = config.healthTextOpacity;
        if (behavior == TextBehavior.WHEN_NOT_FULL && currentHealth >= maxHealth) {
            long timeSinceFull = System.currentTimeMillis() - fullHealthStartTime;
            alpha = (int)(alpha * (RenderUtil.calculateTextAlpha(timeSinceFull) / (float)RenderUtil.BASE_TEXT_ALPHA));
        }
        alpha = (int) (alpha * getHealthBarAlpha()); // Modulate with bar alpha
        alpha = Math.max(10, Math.min(255, alpha));

        return (alpha << 24) | baseColor;
    }

    private static boolean shouldRenderHealthText(float currentHealth, float maxHealth, Player player) {
        TextBehavior behavior = ModConfigManager.getClient().showHealthText;

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
    
    // AppleSkin compatibility methods
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
    
    private static void renderHealthRestoredOverlay(GuiGraphics graphics, Player player, ItemStack heldFood,
                                                   float currentHealth, float maxHealth,
                                                   ScreenRect barRect, int animOffset, boolean isRightAnchored,
                                                   AnimationMetadata.AnimationData animData) {
        if (!PlatformUtil.isModLoaded("appleskin")) {
            return;
        }
        
        float healthRestoration = AppleSkinCompat.getEstimatedHealthRestoration(heldFood, player);
        if (healthRestoration <= 0 || currentHealth >= maxHealth) {
            return;
        }
        
        float restoredHealth = Math.min(maxHealth, currentHealth + healthRestoration);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, TickHandler.getOverlayFlashAlpha());
        
        FillDirection fillDirection = ModConfigManager.getClient().healthFillDirection;
        // Use the bar type based on the player's current state
        BarType barType = BarType.fromPlayerState(player);
        ResourceLocation barTexture = DynamicResourceBars.loc("textures/gui/" + barType.getTexture() + ".png");
        
        if (fillDirection == FillDirection.VERTICAL) {
            int currentHeight = (int) (barRect.height() * Math.max(0.0f, Math.min(1.0f, currentHealth / maxHealth)));
            int restoredHeight = (int) (barRect.height() * Math.max(0.0f, Math.min(1.0f, restoredHealth / maxHealth)));
            int overlayHeight = restoredHeight - currentHeight;
            
            if (overlayHeight > 0) {
                int yPos = barRect.y() + (barRect.height() - restoredHeight);
                int textureVOffset = animOffset + (barRect.height() - restoredHeight);

                RenderUtil.blitWithBinding(graphics, barTexture,
                    barRect.x(), yPos,
                    0, textureVOffset,
                    barRect.width(), overlayHeight,
                    animData.textureWidth, animData.textureHeight
                );
            }
        } else { // HORIZONTAL
            int currentWidth = (int) (barRect.width() * Math.max(0.0f, Math.min(1.0f, currentHealth / maxHealth)));
            int restoredWidth = (int) (barRect.width() * Math.max(0.0f, Math.min(1.0f, restoredHealth / maxHealth)));
            int overlayWidth = restoredWidth - currentWidth;
            
            if (overlayWidth > 0) {
                int xDrawPos;
                int uTexOffset;

                if (isRightAnchored) {
                    // Overlay is to the left of the current filled portion, extending further left.
                    // The restored portion effectively starts at (barRect.width - restoredWidth)
                    // The current portion starts at (barRect.width - currentWidth)
                    // The overlay starts at (barRect.width - restoredWidth) and has width (restoredWidth - currentWidth)
                    xDrawPos = barRect.x() + barRect.width() - restoredWidth;
                    // We want to sample the portion of the texture that represents the *additional* health.
                    // This would be the segment from (barRect.width - restoredWidth) to (barRect.width - currentWidth) in the texture.
                    uTexOffset = barRect.width() - restoredWidth; 
                } else {
                    xDrawPos = barRect.x() + currentWidth;
                    uTexOffset = currentWidth; // Sample from where the current health ends in the texture
                }

                RenderUtil.blitWithBinding(graphics, barTexture,
                    xDrawPos, barRect.y(),
                    uTexOffset, animOffset,
                    overlayWidth, barRect.height(),
                    animData.textureWidth, animData.textureHeight
                );
            }
        }
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
    
    private static boolean hasComfortEffect(Player player) {
        #if UPTO_20_1
            // 1.20.1 Forge: RegistryObject<MobEffect>
            // 1.20.1 Fabric: Supplier<MobEffect>
        var comfortEffect = ModEffects.COMFORT.get();
        return player.hasEffect(comfortEffect);
        #elif NEWER_THAN_20_1
            // 1.21.1 Fabric/NeoForge - Holder<MobEffect>
        var comfortEffect = ModEffects.COMFORT;
        return player.hasEffect(comfortEffect);
        #else
        return false;
        #endif
    }
}