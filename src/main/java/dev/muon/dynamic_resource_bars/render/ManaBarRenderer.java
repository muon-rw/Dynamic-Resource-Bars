package dev.muon.dynamic_resource_bars.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.ManaProvider;
import dev.muon.dynamic_resource_bars.util.Position;
import dev.muon.dynamic_resource_bars.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif

import dev.muon.dynamic_resource_bars.util.EditModeManager;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import dev.muon.dynamic_resource_bars.util.TextBehavior;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;
import dev.muon.dynamic_resource_bars.util.FillDirection;
import dev.muon.dynamic_resource_bars.config.ClientConfig;

public class ManaBarRenderer {
    private static float lastMana = -1;
    private static long fullManaStartTime = 0;
    private static long barEnabledStartTime = 0L;
    private static long barDisabledStartTime = 0L;
    private static boolean barSetVisible = false;

    private static final int RESERVED_MANA_COLOR = 0x232323;

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

        int animationCycles = ModConfigManager.getClient().manaBarAnimationCycles;
        int frameHeight = ModConfigManager.getClient().manaBarFrameHeight;
        int animOffset = (int) (((player.tickCount + #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif) / 3) % animationCycles) * frameHeight;
        boolean isRightAnchored = ModConfigManager.getClient().manaBarAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT;

        if (ModConfigManager.getClient().enableManaBackground) {
             ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player); 
             graphics.blit(DynamicResourceBars.loc("textures/gui/mana_background.png"),
                     bgRect.x(), bgRect.y(), 0, 0, bgRect.width(), bgRect.height(), 256, 256);
        }

        ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
        renderManaBar(graphics, manaProvider, animOffset, barRect, isRightAnchored);

        renderReservedOverlay(graphics, manaProvider, animOffset, barRect);

        if (ModConfigManager.getClient().enableManaForeground) {
            ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
            graphics.blit(DynamicResourceBars.loc("textures/gui/mana_foreground.png"),
                    fgRect.x(), fgRect.y(),
                    0, 0, fgRect.width(), fgRect.height(), 256, 256);
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
                                      int animOffset, ScreenRect barAreaRect, boolean isRightAnchored) {
        float maxManaTotal = manaProvider.getMaxMana() * (1.0f + manaProvider.getReservedMana());
        if (maxManaTotal <= 0) maxManaTotal = 1;
        double currentMana = manaProvider.getCurrentMana();
        int totalBarWidth = barAreaRect.width();
        int barHeight = barAreaRect.height();

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
                graphics.blit(DynamicResourceBars.loc("textures/gui/mana_bar.png"),
                        barX, barY,
                        0, textureVOffset, // Use 0 for U, adjusted V for vertical fill
                        totalBarWidth, filledHeight, // Use full width, partial height
                        256, 1024);
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
                graphics.blit(DynamicResourceBars.loc("textures/gui/mana_bar.png"),
                        barX, barY,
                        uTexOffset, animOffset, // Use calculated uTexOffset
                        filledWidth, barHeight,
                        256, 1024); // Standard animated texture sheet size
            }
        }
    }

    private static void renderReservedOverlay(GuiGraphics graphics, ManaProvider manaProvider,
                                              int animOffset, ScreenRect barAreaRect) {
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

        graphics.blit(
                DynamicResourceBars.loc("textures/gui/mana_bar.png"),
                reserveStartX, barAreaRect.y(),
                0, animOffset,
                reserveManaPixelWidth, barHeight,
                256, 256
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
}