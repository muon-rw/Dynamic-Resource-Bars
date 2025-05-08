package dev.muon.dynamic_resource_bars.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
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

public class ManaBarRenderer {
    private static float lastMana = -1;
    private static long fullManaStartTime = 0;
    private static long barEnabledStartTime = 0L;
    private static long barDisabledStartTime = 0L;
    private static boolean barSetVisible = false;

    private static final int RESERVED_MANA_COLOR = 0x232323;

    public static ScreenRect getScreenRect(Player player) {
        if (player == null && Minecraft.getInstance().player == null) return new ScreenRect(0,0,0,0);

        Position manaPosBase = HUDPositioning.getPositionFromAnchor(AllConfigs.client().manaBarAnchor.get());
        Position manaPos = manaPosBase.offset(AllConfigs.client().manaTotalXOffset.get(), AllConfigs.client().manaTotalYOffset.get());
        int backgroundWidth = AllConfigs.client().manaBackgroundWidth.get();
        int backgroundHeight = AllConfigs.client().manaBackgroundHeight.get();
        
        return new ScreenRect(manaPos.x(), manaPos.y(), backgroundWidth, backgroundHeight);
    }

    public static ScreenRect getSubElementRect(SubElementType type, Player player) {
        ScreenRect complexRect = getScreenRect(player); 
        if (complexRect == null || complexRect.width() == 0 && complexRect.height() == 0) return new ScreenRect(0,0,0,0);

        int x = complexRect.x();
        int y = complexRect.y();

        switch (type) {
            case BACKGROUND:
                return new ScreenRect(x, y, 
                                      AllConfigs.client().manaBackgroundWidth.get(), 
                                      AllConfigs.client().manaBackgroundHeight.get());
            case BAR_MAIN:
                return new ScreenRect(x + AllConfigs.client().manaBarXOffset.get(), 
                                      y + AllConfigs.client().manaBarYOffset.get(), 
                                      AllConfigs.client().manaBarWidth.get(), 
                                      AllConfigs.client().manaBarHeight.get());
            case FOREGROUND_DETAIL:
                return new ScreenRect(x + AllConfigs.client().manaOverlayXOffset.get(), 
                                      y + AllConfigs.client().manaOverlayYOffset.get(), 
                                      AllConfigs.client().manaOverlayWidth.get(),
                                      AllConfigs.client().manaOverlayHeight.get());
            default:
                return new ScreenRect(0,0,0,0); 
        }
    }

    public static void render(GuiGraphics graphics, #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif, ManaProvider manaProvider, Player player) {
        if (!Minecraft.getInstance().gameMode.canHurtPlayer() && !EditModeManager.isEditModeEnabled()) {
            return;
        }

        if (AllConfigs.client().fadeManaWhenFull.get() && manaProvider.getCurrentMana() >= manaProvider.getMaxMana()) {
            setBarVisibility(false);
        } else {
            setBarVisibility(true);
        }

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
        int xPos = complexRect.x();
        int yPos = complexRect.y();

        int backgroundWidth = complexRect.width();
        int backgroundHeight = complexRect.height();
        int animationCycles = AllConfigs.client().manaBarAnimationCycles.get();
        int frameHeight = AllConfigs.client().manaBarFrameHeight.get();
        int animOffset = (int) (((player.tickCount + #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif) / 3) % animationCycles) * frameHeight;
        boolean isRightAnchored = AllConfigs.client().manaBarAnchor.get().getSide() == HUDPositioning.AnchorSide.RIGHT;

        if (AllConfigs.client().enableManaBackground.get()) {
             ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player); 
             graphics.blit(DynamicResourceBars.loc("textures/gui/mana_background.png"),
                     bgRect.x(), bgRect.y(), 0, 0, bgRect.width(), bgRect.height(), 256, 256);
        }

        ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
        renderManaBarItself(graphics, manaProvider, animOffset, barRect, isRightAnchored);

        renderReservedOverlay(graphics, manaProvider, animOffset, barRect);

        if (AllConfigs.client().enableManaForeground.get()) {
            ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
            graphics.blit(DynamicResourceBars.loc("textures/gui/detail_overlay.png"),
                    fgRect.x(), fgRect.y(),
                    0, 0, fgRect.width(), fgRect.height(), 256, 256);
        }

        // Text Rendering
        if (shouldRenderManaText(manaProvider.getCurrentMana(), manaProvider.getMaxMana())) {
            int color = getManaTextColor(manaProvider.getCurrentMana(), manaProvider.getMaxMana(), currentAlphaForRender);
            HorizontalAlignment alignment = AllConfigs.client().manaTextAlign.get();

            int baseX = barRect.x();
            if (alignment == HorizontalAlignment.CENTER) {
                baseX = barRect.x() + (barRect.width() / 2);
            } else if (alignment == HorizontalAlignment.RIGHT) {
                baseX = barRect.x() + barRect.width();
            }

            int baseY = barRect.y() + (barRect.height() / 2);

            RenderUtil.renderText((float) manaProvider.getCurrentMana(), manaProvider.getMaxMana(), graphics,
                                baseX, baseY, color, alignment);
        }
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

        if (EditModeManager.isEditModeEnabled()) {
            DraggableElement currentBarType = DraggableElement.MANA_BAR;
            if (EditModeManager.getFocusedElement() == currentBarType) {
                int focusedBorderColor = 0xA0FFFF00;
                ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
                if (AllConfigs.client().enableManaBackground.get()) {
                     graphics.renderOutline(bgRect.x()-1, bgRect.y()-1, bgRect.width()+2, bgRect.height()+2, focusedBorderColor);
                }
                
                ScreenRect barRectOutline = getSubElementRect(SubElementType.BAR_MAIN, player);
                graphics.renderOutline(barRectOutline.x()-1, barRectOutline.y()-1, barRectOutline.width()+2, barRectOutline.height()+2, 0xA000FFFF);
                
                if (AllConfigs.client().enableManaForeground.get()) {
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

    private static void renderManaBarItself(GuiGraphics graphics, ManaProvider manaProvider,
                                          int animOffset, ScreenRect barAreaRect, boolean isRightAnchored) {
        float maxManaTotal = manaProvider.getMaxMana() * (1.0f + manaProvider.getReservedMana());
        if (maxManaTotal <= 0) maxManaTotal = 1;
        double currentMana = manaProvider.getCurrentMana();
        int totalBarWidth = barAreaRect.width();
        int barHeight = barAreaRect.height();

        int filledWidth = (int) (totalBarWidth * (currentMana / maxManaTotal));
        if (filledWidth <= 0 && currentMana > 0) filledWidth = 1;
        if (filledWidth > totalBarWidth) filledWidth = totalBarWidth;

        int barX = barAreaRect.x();
        int barY = barAreaRect.y();

        if (isRightAnchored) {
            barX = barAreaRect.x() + totalBarWidth - filledWidth;
        }

        if (filledWidth > 0) {
             graphics.blit(DynamicResourceBars.loc("textures/gui/mana_bar.png"),
                    barX, barY, 
                    0, animOffset,
                    filledWidth, barHeight,
                    256, 256);
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
        long timeSinceFullMana = fullManaStartTime > 0 ?
                System.currentTimeMillis() - fullManaStartTime : 0;
        int alpha = RenderUtil.BASE_TEXT_ALPHA;
        TextBehavior textBehavior = AllConfigs.client().showManaText.get();
        if (textBehavior == TextBehavior.WHEN_NOT_FULL) {
            if (currentMana >= maxMana) { 
                alpha = RenderUtil.calculateTextAlpha(timeSinceFullMana);
            }
        } else if (textBehavior == TextBehavior.NEVER) {
            alpha = 0; 
        }
        alpha = (int)(alpha * barRenderAlpha);
        alpha = Math.max(10, alpha);
        return (alpha << 24) | 0xFFFFFF;
    }

    private static int getManaTextColor(double currentMana, float maxMana) {
        return getManaTextColor(currentMana, maxMana, getCurrentAlpha());
    }

    private static boolean shouldRenderManaText(double currentMana, float maxMana) {
        if (currentMana >= maxMana) {
            if (lastMana < maxMana) {
                fullManaStartTime = System.currentTimeMillis();
            }
        } else {
            fullManaStartTime = 0;
        }
        lastMana = (float) currentMana;

        dev.muon.dynamic_resource_bars.util.TextBehavior textBehavior = AllConfigs.client().showManaText.get();
        switch (textBehavior) {
            case ALWAYS:
                return getCurrentAlpha() > 0.05f;
            case NEVER:
                return false;
            case WHEN_NOT_FULL:
                long timeSinceFullMana = fullManaStartTime > 0 ?
                        System.currentTimeMillis() - fullManaStartTime : 0;
                return (currentMana < maxMana || (fullManaStartTime > 0 && timeSinceFullMana < RenderUtil.TEXT_DISPLAY_DURATION))
                        && getCurrentAlpha() > 0.05f;
            default:
                return false;
        }
    }
}