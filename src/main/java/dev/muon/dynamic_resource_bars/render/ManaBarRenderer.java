package dev.muon.dynamic_resource_bars.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif

import dev.muon.dynamic_resource_bars.config.ClientConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ManaBarRenderer {
    private static float lastMana = -1;
    private static long fullManaStartTime = 0;
    private static long barEnabledStartTime = 0L;
    private static long barDisabledStartTime = 0L;
    private static boolean barSetVisible = false;

    private static final int RESERVED_MANA_COLOR = 0x232323;
    
    // Fadeout chunking system
    private static final long CHUNK_FADEOUT_DURATION = RenderUtil.BAR_FADEOUT_DURATION / 3;
    private static final List<FadingChunk> fadingChunks = new ArrayList<>();
    private static double previousMana = -1;
    private static float previousMaxMana = -1;
    
    private static class FadingChunk {
        final double startValue;    // Where the chunk starts (in mana units)
        final double endValue;      // Where it ends
        final float maxValue;       // Max value at time of creation
        final long creationTime;    // When this chunk started fading
        final int animOffset;       // Animation frame at creation
        
        FadingChunk(double startValue, double endValue, float maxValue, int animOffset) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.maxValue = maxValue;
            this.creationTime = System.currentTimeMillis();
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

    public static ScreenRect getScreenRect(Player player) {
        if (player == null && Minecraft.getInstance().player == null) return new ScreenRect(0,0,0,0);

        Position manaPosBase = HUDPositioning.getPositionFromAnchor(ModConfigManager.getClient().manaBarAnchor);
        Position manaPos = manaPosBase.offset(ModConfigManager.getClient().manaTotalXOffset, ModConfigManager.getClient().manaTotalYOffset);
        int backgroundWidth = ModConfigManager.getClient().manaBackgroundWidth;
        int backgroundHeight = ModConfigManager.getClient().manaBackgroundHeight;
        
        return new ScreenRect(manaPos.x(), manaPos.y(), backgroundWidth, backgroundHeight);
    }

    public static ScreenRect getSubElementRect(SubElementType type, Player player) {
        ScreenRect complexRect = getScreenRect(player); 
        if (complexRect == null || complexRect.width() == 0 && complexRect.height() == 0) return new ScreenRect(0,0,0,0);

        int x = complexRect.x();
        int y = complexRect.y();

        switch (type) {
            case BACKGROUND:
                return new ScreenRect(x + ModConfigManager.getClient().manaBackgroundXOffset, 
                                      y + ModConfigManager.getClient().manaBackgroundYOffset, 
                                      ModConfigManager.getClient().manaBackgroundWidth, 
                                      ModConfigManager.getClient().manaBackgroundHeight);
            case BAR_MAIN:
                return new ScreenRect(x + ModConfigManager.getClient().manaBarXOffset, 
                                      y + ModConfigManager.getClient().manaBarYOffset, 
                                      ModConfigManager.getClient().manaBarWidth, 
                                      ModConfigManager.getClient().manaBarHeight);
            case FOREGROUND_DETAIL:
                return new ScreenRect(x + ModConfigManager.getClient().manaOverlayXOffset, 
                                      y + ModConfigManager.getClient().manaOverlayYOffset, 
                                      ModConfigManager.getClient().manaOverlayWidth,
                                      ModConfigManager.getClient().manaOverlayHeight);
            case TEXT:
                // Text area now positioned relative to complexRect, using manaBarWidth/Height for its dimensions
                return new ScreenRect(x + ModConfigManager.getClient().manaTextXOffset, 
                                      y + ModConfigManager.getClient().manaTextYOffset, 
                                      ModConfigManager.getClient().manaBarWidth, 
                                      ModConfigManager.getClient().manaBarHeight);
            default:
                return new ScreenRect(0,0,0,0); 
        }
    }

    public static void render(GuiGraphics graphics, #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif, ManaProvider manaProvider, Player player) {
        if (!Minecraft.getInstance().gameMode.canHurtPlayer() && !EditModeManager.isEditModeEnabled()) {
            return;
        }

        boolean visibilityDecision;
        ClientConfig clientConfig = ModConfigManager.getClient();

        if (!clientConfig.fadeManaWhenFull) {
            // Rule 1: fadeManaWhenFull is OFF - always show.
            visibilityDecision = true;
        } else {
            // Rule 2: fadeManaWhenFull is ON.
            if (manaProvider.hasSpecificVisibilityLogic()) {
                // Provider has a comprehensive method that dictates visibility (e.g., Ars Nouveau).
                // This method is expected to handle all its own conditions, including mana levels if relevant to its logic.
                visibilityDecision = manaProvider.shouldDisplayBarOverride(player);
            } else {
                // Generic provider or provider that wants to add conditions (e.g., RPGMana, ManaAttributes).
                // Show if (provider forces show via forceShowBarConditions) OR (mana is not full).
                boolean providerForcesShow = manaProvider.forceShowBarConditions(player);
                boolean manaIsNotFull = manaProvider.getCurrentMana() < manaProvider.getMaxMana();
                visibilityDecision = providerForcesShow || manaIsNotFull;
            }
        }
        setBarVisibility(visibilityDecision);
        
        // Track value changes for chunk creation
        updateChunkTracking(player, manaProvider, #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif);

        if (!isVisible() && !EditModeManager.isEditModeEnabled()) {
            return;
        }

        float currentAlphaForRender = getCurrentAlpha();
        if (EditModeManager.isEditModeEnabled() && !isVisible()) {
            currentAlphaForRender = 1.0f;
        }

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);

        ScreenRect complexRect = getScreenRect(player);

        // Load animation data and mask info from .mcmeta
        AnimationMetadata.AnimationData animData = AnimationMetadataCache.getManaBarAnimation();
        AnimationMetadata.MaskInfo maskInfo = AnimationMetadataCache.getManaBarMask();
        float ticks = player.tickCount + (#if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif);
        int animOffset = AnimationMetadata.calculateAnimationOffset(animData, ticks);
        boolean isRightAnchored = ModConfigManager.getClient().manaBarAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT;

        if (ModConfigManager.getClient().enableManaBackground) {
             ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
             AnimationMetadata.ScalingInfo bgScaling = AnimationMetadataCache.getManaBackgroundScaling();
             NineSliceRenderer.renderWithScaling(graphics, 
                     DynamicResourceBars.loc("textures/gui/mana_background.png"),
                     bgScaling,
                     bgRect.x(), bgRect.y(), bgRect.width(), bgRect.height(), 256, 256);
        }

        ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
        renderManaBar(graphics, manaProvider, animOffset, barRect, isRightAnchored, maskInfo, animData);
        
        // Render fading chunks after the main bar
        renderFadingChunks(graphics, barRect, manaProvider, isRightAnchored, currentAlphaForRender, maskInfo, animData);

        renderReservedOverlay(graphics, manaProvider, animOffset, barRect, maskInfo, animData);

        if (ModConfigManager.getClient().enableManaForeground) {
            ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
            AnimationMetadata.ScalingInfo fgScaling = AnimationMetadataCache.getManaForegroundScaling();
            NineSliceRenderer.renderWithScaling(graphics,
                    DynamicResourceBars.loc("textures/gui/mana_foreground.png"),
                    fgScaling,
                    fgRect.x(), fgRect.y(), fgRect.width(), fgRect.height(), 256, 256);
        }

        // Text Rendering
        if (shouldRenderManaText(manaProvider.getCurrentMana(), manaProvider.getMaxMana())) {
            ScreenRect textRect = getSubElementRect(SubElementType.TEXT, player);
            int textX = textRect.x() + (textRect.width() / 2);
            int textY = textRect.y() + (textRect.height() / 2);
            
            int color = getManaTextColor(manaProvider.getCurrentMana(), manaProvider.getMaxMana(), currentAlphaForRender);
            HorizontalAlignment alignment = ModConfigManager.getClient().manaTextAlign;

            int baseX = textRect.x();
            if (alignment == HorizontalAlignment.CENTER) {
                baseX = textX;
            } else if (alignment == HorizontalAlignment.RIGHT) {
                baseX = textRect.x() + textRect.width();
            }

            RenderUtil.renderText((float) manaProvider.getCurrentMana(), manaProvider.getMaxMana(), graphics,
                                baseX, textY, color, alignment);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

        if (EditModeManager.isEditModeEnabled()) {
            DraggableElement currentBarType = DraggableElement.MANA_BAR;
            if (EditModeManager.getFocusedElement() == currentBarType) {
                int focusedBorderColor = 0xA0FFFF00;
                ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
                if (ModConfigManager.getClient().enableManaBackground) {
                     graphics.renderOutline(bgRect.x()-1, bgRect.y()-1, bgRect.width()+2, bgRect.height()+2, focusedBorderColor);
                }
                
                ScreenRect barRectOutline = getSubElementRect(SubElementType.BAR_MAIN, player);
                graphics.renderOutline(barRectOutline.x()-1, barRectOutline.y()-1, barRectOutline.width()+2, barRectOutline.height()+2, 0xA000FFFF);
                
                if (ModConfigManager.getClient().enableManaForeground) {
                    ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                    graphics.renderOutline(fgRect.x()-1, fgRect.y()-1, fgRect.width()+2, fgRect.height()+2, 0xA0FF00FF);
                }
                graphics.renderOutline(complexRect.x()-2, complexRect.y()-2, complexRect.width()+4, complexRect.height()+4, 0x80FFFFFF);
            } else {
                int borderColor = 0x80FFFFFF; 
                graphics.renderOutline(complexRect.x()-1, complexRect.y()-1, complexRect.width()+2, complexRect.height()+2, borderColor);
            }
        }
    }

    private static float getCurrentAlpha() {
        if (barSetVisible) return 1.0f;
        long timeSinceHide = System.currentTimeMillis() - barDisabledStartTime;
        return Math.max(0, 1 - (timeSinceHide / (float) RenderUtil.BAR_FADEOUT_DURATION));
    }

    public static void setBarVisibility(boolean visible) {
        if (barSetVisible != visible) {
            if (visible) {
                barEnabledStartTime = System.currentTimeMillis();
            } else {
                barDisabledStartTime = System.currentTimeMillis();
            }
            barSetVisible = visible;
        }
    }

    private static void renderManaBar(GuiGraphics graphics, ManaProvider manaProvider,
                                      int animOffset, ScreenRect barAreaRect, boolean isRightAnchored,
                                      AnimationMetadata.MaskInfo maskInfo, AnimationMetadata.AnimationData animData) {
        float maxManaTotal = manaProvider.getMaxMana() * (1.0f + manaProvider.getReservedMana());
        if (maxManaTotal <= 0) maxManaTotal = 1;
        double currentMana = manaProvider.getCurrentMana();
        int totalBarWidth = barAreaRect.width();
        int barHeight = barAreaRect.height();
        
        ResourceLocation barTexture = DynamicResourceBars.loc("textures/gui/mana_bar.png");

        FillDirection fillDirection = ModConfigManager.getClient().manaFillDirection;

        if (fillDirection == FillDirection.VERTICAL) {
            int filledHeight = (int) (barHeight * (currentMana / maxManaTotal));
            if (filledHeight <= 0 && currentMana > 0) filledHeight = 1;
            if (filledHeight > barHeight) filledHeight = barHeight;

            int barX = barAreaRect.x();
            // Fill from bottom up, so Y is adjusted by the unfilled portion
            int barY = barAreaRect.y() + (barHeight - filledHeight);
            // Texture V offset needs to match the visible portion of the bar
            int textureVOffset = animOffset + (barHeight - filledHeight);

            if (filledHeight > 0) {
                MaskRenderUtil.renderWithMask(
                        graphics, barTexture, maskInfo,
                        barX, barY,
                        0, textureVOffset, // Use 0 for U, adjusted V for vertical fill
                        totalBarWidth, filledHeight, // Use full width, partial height
                        animData.textureWidth, animData.textureHeight);
            }
        } else { // HORIZONTAL
            int filledWidth = (int) (totalBarWidth * (currentMana / maxManaTotal));
            if (filledWidth <= 0 && currentMana > 0) filledWidth = 1;
            if (filledWidth > totalBarWidth) filledWidth = totalBarWidth;

            int barX = barAreaRect.x();
            int barY = barAreaRect.y();
            int uTexOffset = 0; // Default for left-anchored

            if (isRightAnchored) {
                barX = barAreaRect.x() + totalBarWidth - filledWidth;
                uTexOffset = totalBarWidth - filledWidth; // Sample the right part of the texture
            }
            if (uTexOffset < 0) uTexOffset = 0; // Prevent negative texture offset

            if (filledWidth > 0) {
                MaskRenderUtil.renderWithMask(
                        graphics, barTexture, maskInfo,
                        barX, barY,
                        uTexOffset, animOffset, // Use calculated uTexOffset
                        filledWidth, barHeight,
                        animData.textureWidth, animData.textureHeight);
            }
        }
    }

    private static void renderReservedOverlay(GuiGraphics graphics, ManaProvider manaProvider,
                                              int animOffset, ScreenRect barAreaRect, AnimationMetadata.MaskInfo maskInfo,
                                              AnimationMetadata.AnimationData animData) {
        float reservedManaFraction = manaProvider.getReservedMana();
        if (reservedManaFraction <= 0) return;

        float maxManaWithoutReserved = manaProvider.getMaxMana();
        if (maxManaWithoutReserved <= 0) maxManaWithoutReserved = 1;
        float maxManaTotal = maxManaWithoutReserved * (1.0f + reservedManaFraction);

        int totalBarWidth = barAreaRect.width();
        int barHeight = barAreaRect.height();

        int reserveManaPixelWidth = (int) (totalBarWidth * (reservedManaFraction / (1.0f + reservedManaFraction)));
        if (reserveManaPixelWidth <= 0) return;

        int reserveStartX = barAreaRect.x() + totalBarWidth - reserveManaPixelWidth;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(((RESERVED_MANA_COLOR >> 16) & 0xFF) / 255f,
                ((RESERVED_MANA_COLOR >> 8) & 0xFF) / 255f,
                (RESERVED_MANA_COLOR & 0xFF) / 255f,
                1.0f);

        ResourceLocation barTexture = DynamicResourceBars.loc("textures/gui/mana_bar.png");
        MaskRenderUtil.renderWithMask(
                graphics, barTexture, maskInfo,
                reserveStartX, barAreaRect.y(),
                0, animOffset,
                reserveManaPixelWidth, barHeight,
                animData.textureWidth, animData.textureHeight
        );

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    public static boolean isVisible() {
        if (!barSetVisible && System.currentTimeMillis() - barDisabledStartTime > RenderUtil.BAR_FADEOUT_DURATION) {
            return false;
        }
        return true;
    }

    private static int getManaTextColor(double currentMana, float maxMana, float barRenderAlpha) {
        TextBehavior textBehavior = ModConfigManager.getClient().showManaText;
        ClientConfig config = ModConfigManager.getClient();
        int baseColor = config.manaTextColor & 0xFFFFFF;
        int alpha = config.manaTextOpacity; // Start with base configured opacity

        if (textBehavior == TextBehavior.WHEN_NOT_FULL && currentMana >= maxMana) {
            long timeSinceFull = Minecraft.getInstance().level.getGameTime() - fullManaStartTime;
            // Calculate the fade factor for the text itself (0.0 to 1.0)
            float textOwnFadeMultiplier = RenderUtil.calculateTextAlpha(timeSinceFull) / (float)RenderUtil.BASE_TEXT_ALPHA;
            alpha = (int)(alpha * textOwnFadeMultiplier); // Apply text's own fade
        }

        // Now apply the bar's overall render alpha to the (potentially already faded) text alpha
        alpha = (int) (alpha * barRenderAlpha);

        alpha = Math.min(255, Math.max(0, alpha)); // Clamp alpha
        return (alpha << 24) | baseColor;
    }

    private static int getManaTextColor(double currentMana, float maxMana) {
        return getManaTextColor(currentMana, maxMana, getCurrentAlpha());
    }

    private static boolean shouldRenderManaText(double currentMana, float maxMana) {
        TextBehavior behavior = ModConfigManager.getClient().showManaText;
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
        boolean isFull = currentMana >= maxMana;
        if (isFull) {
            if (lastMana < maxMana || lastMana == -1) { // Just became full or first check
                fullManaStartTime = Minecraft.getInstance().level.getGameTime();
            }
            lastMana = (float)currentMana;
            // Show for a short duration after becoming full
            return (Minecraft.getInstance().level.getGameTime() - fullManaStartTime) < RenderUtil.TEXT_DISPLAY_DURATION;
        } else {
            lastMana = (float)currentMana;
            return true; // Not full, so show
        }
    }
    
    private static void updateChunkTracking(Player player, ManaProvider manaProvider, float partialTicks) {
        double currentMana = manaProvider.getCurrentMana();
        float maxMana = manaProvider.getMaxMana();
        
        // Clean up expired chunks and those covered by current fill
        Iterator<FadingChunk> it = fadingChunks.iterator();
        while (it.hasNext()) {
            FadingChunk chunk = it.next();
            // Remove if expired or if current mana covers this chunk
            if (chunk.isExpired() || currentMana >= chunk.endValue) {
                it.remove();
            }
        }
        
        // Check if we need to create a new chunk
        if (previousMana > 0 && currentMana < previousMana && previousMaxMana == maxMana) {
            // Load animation data from .mcmeta (or use config defaults)
            AnimationMetadata.AnimationData animData = AnimationMetadataCache.getManaBarAnimation();
            float ticks = player.tickCount + partialTicks;
            int animOffset = AnimationMetadata.calculateAnimationOffset(animData, ticks);
            
            // Create chunk for the lost portion, clamping to 0 minimum
            double chunkStart = Math.max(0, currentMana);
            fadingChunks.add(new FadingChunk(chunkStart, previousMana, maxMana, animOffset));
        }
        
        // Update tracking values
        previousMana = currentMana;
        previousMaxMana = maxMana;
    }
    
    private static void renderFadingChunks(GuiGraphics graphics, ScreenRect barRect, ManaProvider manaProvider,
                                          boolean isRightAnchored, float parentAlpha, AnimationMetadata.MaskInfo maskInfo,
                                          AnimationMetadata.AnimationData animData) {
        if (fadingChunks.isEmpty()) return;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        FillDirection fillDirection = ModConfigManager.getClient().manaFillDirection;
        ResourceLocation barTexture = DynamicResourceBars.loc("textures/gui/mana_bar.png");
        
        // Account for reserved mana in calculations
        float maxManaTotal = manaProvider.getMaxMana() * (1.0f + manaProvider.getReservedMana());
        
        for (FadingChunk chunk : fadingChunks) {
            float alpha = chunk.getAlpha() * parentAlpha;
            if (alpha <= 0) continue;
            
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            
            // Use the total max mana including reserved for proper scaling
            float effectiveMaxForChunk = chunk.maxValue * (1.0f + manaProvider.getReservedMana());
            
            if (fillDirection == FillDirection.VERTICAL) {
                // Calculate heights for the chunk
                float startRatio = (float)(chunk.startValue / effectiveMaxForChunk);
                float endRatio = (float)(chunk.endValue / effectiveMaxForChunk);
                int startHeight = (int) (barRect.height() * startRatio);
                int endHeight = (int) (barRect.height() * endRatio);
                int chunkHeight = endHeight - startHeight;
                
                if (chunkHeight > 0) {
                    int yPos = barRect.y() + (barRect.height() - endHeight);
                    int textureVOffset = chunk.animOffset + (barRect.height() - endHeight);
                    
                    MaskRenderUtil.renderWithMask(
                        graphics, barTexture, maskInfo,
                        barRect.x(), yPos,
                        0, textureVOffset,
                        barRect.width(), chunkHeight,
                        animData.textureWidth, animData.textureHeight
                    );
                }
            } else { // HORIZONTAL
                // Calculate widths for the chunk
                float startRatio = (float)(chunk.startValue / effectiveMaxForChunk);
                float endRatio = (float)(chunk.endValue / effectiveMaxForChunk);
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
                        graphics, barTexture, maskInfo,
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
}