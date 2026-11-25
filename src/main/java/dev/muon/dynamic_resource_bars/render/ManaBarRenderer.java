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
    private static final ResourceLocation MANA_BAR_TEX = DynamicResourceBars.loc("textures/gui/mana_bar.png");

    private static final List<OrbRenderHelper.DrainingChunk> fadingChunks = new ArrayList<>();
    private static final OrbRenderHelper.FillSmoother fillSmoother = new OrbRenderHelper.FillSmoother();
    private static double previousMana = -1;
    private static float previousMaxMana = -1;
    private static ManaProvider cachedProvider = null;

    public static ScreenRect getScreenRect(Player player) {
        if (player == null && Minecraft.getInstance().player == null) return new ScreenRect(0, 0, 0, 0);

        Position manaPosBase = HUDPositioning.getPositionFromAnchor(ModConfigManager.getClient().manaBarAnchor);
        Position manaPos = manaPosBase.offset(ModConfigManager.getClient().manaTotalXOffset, ModConfigManager.getClient().manaTotalYOffset);
        int backgroundWidth = ModConfigManager.getClient().manaBackgroundWidth;
        int backgroundHeight = ModConfigManager.getClient().manaBackgroundHeight;
        return new ScreenRect(manaPos.x(), manaPos.y(), backgroundWidth, backgroundHeight);
    }

    public static ScreenRect getSubElementRect(SubElementType type, Player player) {
        ScreenRect complexRect = getScreenRect(player);
        if (complexRect == null || complexRect.width() == 0 && complexRect.height() == 0) return new ScreenRect(0, 0, 0, 0);

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
                return new ScreenRect(x + ModConfigManager.getClient().manaTextXOffset,
                        y + ModConfigManager.getClient().manaTextYOffset,
                        ModConfigManager.getClient().manaBarWidth,
                        ModConfigManager.getClient().manaBarHeight);
            default:
                return new ScreenRect(0, 0, 0, 0);
        }
    }

    public static void render(GuiGraphics graphics, #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif, ManaProvider manaProvider, Player player) {
    cachedProvider = manaProvider;
}

public static void renderFromOverlay(GuiGraphics graphics, float partialTicks, Player player) {
    if (cachedProvider == null) return;
    if (ModConfigManager.getClient().manaBarBehavior == ManaBarBehavior.OFF) return;
    renderInternal(graphics, partialTicks, cachedProvider, player);
}

private static void renderInternal(GuiGraphics graphics, float partialTicks, ManaProvider manaProvider, Player player) {
        if (!Minecraft.getInstance().gameMode.canHurtPlayer() && !EditModeManager.isEditModeEnabled()) {
            return;
        }

        boolean visibilityDecision;
        ClientConfig clientConfig = ModConfigManager.getClient();

        if (!clientConfig.fadeManaWhenFull) {
            visibilityDecision = true;
        } else {
            if (manaProvider.hasSpecificVisibilityLogic()) {
                visibilityDecision = manaProvider.shouldDisplayBarOverride(player);
            } else {
                boolean providerForcesShow = manaProvider.forceShowBarConditions(player);
                boolean manaIsNotFull = manaProvider.getCurrentMana() < manaProvider.getMaxMana();
                visibilityDecision = providerForcesShow || manaIsNotFull;
            }
        }
        setBarVisibility(visibilityDecision);

        float ticks = player.tickCount + partialTicks;

        updateChunkTracking(player, manaProvider, ticks);

        if (!isVisible() && !EditModeManager.isEditModeEnabled()) {
            return;
        }

        float currentAlphaForRender = getCurrentAlpha();
        if (EditModeManager.isEditModeEnabled() && !isVisible()) {
            currentAlphaForRender = 1.0f;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);

        ScreenRect complexRect = getScreenRect(player);
        OrbRenderHelper.AnimationData animData = OrbRenderHelper.getAnimationData(MANA_BAR_TEX);
        int animOffset = OrbRenderHelper.calculateAnimationOffset(animData, ticks);
        boolean isRightAnchored = ModConfigManager.getClient().manaBarAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT;

        if (ModConfigManager.getClient().enableManaBackground) {
            ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
            OrbRenderHelper.blitFullTexture(graphics,
                    DynamicResourceBars.loc("textures/gui/mana_background.png"),
                    bgRect.x(), bgRect.y(), bgRect.width(), bgRect.height());
        }

        ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
        renderManaBar(graphics, manaProvider, animOffset, animData, barRect, isRightAnchored, ticks);
        renderFadingChunks(graphics, barRect, manaProvider, animData, isRightAnchored, currentAlphaForRender, ticks);
        renderReservedOverlay(graphics, manaProvider, animOffset, animData, barRect, currentAlphaForRender, ticks);

        if (ModConfigManager.getClient().enableManaForeground) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);
            ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
            OrbRenderHelper.blitFullTexture(graphics,
                    DynamicResourceBars.loc("textures/gui/mana_foreground.png"),
                    fgRect.x(), fgRect.y(), fgRect.width(), fgRect.height());
        }

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
                    graphics.renderOutline(bgRect.x() - 1, bgRect.y() - 1, bgRect.width() + 2, bgRect.height() + 2, focusedBorderColor);
                }

                ScreenRect barRectOutline = getSubElementRect(SubElementType.BAR_MAIN, player);
                graphics.renderOutline(barRectOutline.x() - 1, barRectOutline.y() - 1, barRectOutline.width() + 2, barRectOutline.height() + 2, 0xA000FFFF);

                if (ModConfigManager.getClient().enableManaForeground) {
                    ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                    graphics.renderOutline(fgRect.x() - 1, fgRect.y() - 1, fgRect.width() + 2, fgRect.height() + 2, 0xA0FF00FF);
                }
                graphics.renderOutline(complexRect.x() - 2, complexRect.y() - 2, complexRect.width() + 4, complexRect.height() + 4, 0x80FFFFFF);
            } else {
                graphics.renderOutline(complexRect.x() - 1, complexRect.y() - 1, complexRect.width() + 2, complexRect.height() + 2, 0x80FFFFFF);
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
                                      int animOffset, OrbRenderHelper.AnimationData animData,
                                      ScreenRect barAreaRect, boolean isRightAnchored, float ticks) {
        float maxManaTotal = manaProvider.getMaxMana() * (1.0f + manaProvider.getReservedMana());
        if (maxManaTotal <= 0) maxManaTotal = 1;
        double currentMana = manaProvider.getCurrentMana();
        float ratio = (float) Math.max(0.0, Math.min(1.0, currentMana / maxManaTotal));

        boolean vertical = ModConfigManager.getClient().manaFillDirection == FillDirection.VERTICAL;
        int span = vertical ? barAreaRect.height() : barAreaRect.width();

        int filled = (int) (span * ratio);
        if (filled <= 0 && currentMana > 0) filled = 1;
        int liveEdge = fillSmoother.step(filled);
        if (liveEdge <= 0) return;

        OrbRenderHelper.paintFillSpan(graphics, MANA_BAR_TEX, barAreaRect, vertical, isRightAnchored,
                animOffset, animData, 0, liveEdge, false, true, ticks);
    }

    private static void renderReservedOverlay(GuiGraphics graphics, ManaProvider manaProvider,
                                              int animOffset, OrbRenderHelper.AnimationData animData,
                                              ScreenRect barAreaRect, float parentAlpha, float ticks) {
        float reservedManaFraction = manaProvider.getReservedMana();
        if (reservedManaFraction <= 0) return;

        boolean vertical = ModConfigManager.getClient().manaFillDirection == FillDirection.VERTICAL;
        int span = vertical ? barAreaRect.height() : barAreaRect.width();
        int reserveSize = (int) (span * (reservedManaFraction / (1.0f + reservedManaFraction)));
        if (reserveSize <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(((RESERVED_MANA_COLOR >> 16) & 0xFF) / 255f,
                ((RESERVED_MANA_COLOR >> 8) & 0xFF) / 255f,
                (RESERVED_MANA_COLOR & 0xFF) / 255f,
                parentAlpha);

        if (vertical) {
            OrbRenderHelper.paintFillSpan(graphics, MANA_BAR_TEX, barAreaRect, true, false,
                    animOffset, animData, span - reserveSize, span, false, false, ticks);
        } else {
            int reserveStartX = barAreaRect.x() + barAreaRect.width() - reserveSize;
            graphics.blit(MANA_BAR_TEX, reserveStartX, barAreaRect.y(),
                    0.0f, (float) animOffset, reserveSize, barAreaRect.height(),
                    animData.textureWidth, animData.textureHeight);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, parentAlpha);
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
        int alpha = config.manaTextOpacity;

        if (textBehavior == TextBehavior.WHEN_NOT_FULL && currentMana >= maxMana) {
            long timeSinceFull = Minecraft.getInstance().level.getGameTime() - fullManaStartTime;
            float textOwnFadeMultiplier = RenderUtil.calculateTextAlpha(timeSinceFull) / (float) RenderUtil.BASE_TEXT_ALPHA;
            alpha = (int) (alpha * textOwnFadeMultiplier);
        }

        alpha = (int) (alpha * barRenderAlpha);
        alpha = Math.min(255, Math.max(0, alpha));
        return (alpha << 24) | baseColor;
    }

    private static boolean shouldRenderManaText(double currentMana, float maxMana) {
        TextBehavior behavior = ModConfigManager.getClient().showManaText;
        if (EditModeManager.isEditModeEnabled()) {
            if (behavior == TextBehavior.ALWAYS || behavior == TextBehavior.WHEN_NOT_FULL) return true;
        }
        if (behavior == TextBehavior.NEVER) return false;
        if (behavior == TextBehavior.ALWAYS) return true;

        boolean isFull = currentMana >= maxMana;
        if (isFull) {
            if (lastMana < maxMana || lastMana == -1) {
                fullManaStartTime = Minecraft.getInstance().level.getGameTime();
            }
            lastMana = (float) currentMana;
            return (Minecraft.getInstance().level.getGameTime() - fullManaStartTime) < RenderUtil.TEXT_DISPLAY_DURATION;
        } else {
            lastMana = (float) currentMana;
            return true;
        }
    }

    private static void updateChunkTracking(Player player, ManaProvider manaProvider, float ticks) {
        double currentMana = manaProvider.getCurrentMana();
        float maxMana = manaProvider.getMaxMana();

        Iterator<OrbRenderHelper.DrainingChunk> it = fadingChunks.iterator();
        while (it.hasNext()) {
            OrbRenderHelper.DrainingChunk chunk = it.next();
            if (chunk.isExpired() || currentMana >= chunk.endValue) {
                it.remove();
            }
        }

        if (previousMana > 0 && currentMana < previousMana && previousMaxMana == maxMana) {
            float chunkStart = (float) Math.max(0.0, currentMana);
            OrbRenderHelper.DrainingChunk newest = fadingChunks.isEmpty() ? null : fadingChunks.get(fadingChunks.size() - 1);
            if (newest != null && newest.maxValue == maxMana) {
                newest.startValue = Math.min(newest.startValue, chunkStart);
            } else {
                if (fadingChunks.size() >= OrbRenderHelper.MAX_FADING_CHUNKS) fadingChunks.remove(0);
                OrbRenderHelper.AnimationData animData = OrbRenderHelper.getAnimationData(MANA_BAR_TEX);
                int animOffset = OrbRenderHelper.calculateAnimationOffset(animData, ticks);
                fadingChunks.add(new OrbRenderHelper.DrainingChunk(chunkStart, (float) previousMana, maxMana, "mana_bar", animOffset));
            }
        }

        previousMana = currentMana;
        previousMaxMana = maxMana;
    }

    private static void renderFadingChunks(GuiGraphics graphics, ScreenRect barRect, ManaProvider manaProvider,
                                           OrbRenderHelper.AnimationData animData,
                                           boolean isRightAnchored, float parentAlpha, float ticks) {
        if (fadingChunks.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        boolean vertical = ModConfigManager.getClient().manaFillDirection == FillDirection.VERTICAL;
        int span = vertical ? barRect.height() : barRect.width();
        int floorPx = fillSmoother.getLiveEdgePx();

        for (OrbRenderHelper.DrainingChunk chunk : fadingChunks) {
            float effectiveMaxForChunk = chunk.maxValue * (1.0f + manaProvider.getReservedMana());
            if (effectiveMaxForChunk <= 0f) continue;

            float startRatio = Math.max(0f, Math.min(1f, chunk.startValue / effectiveMaxForChunk));
            float endRatio = Math.max(0f, Math.min(1f, chunk.endValue / effectiveMaxForChunk));
            int bottom = Math.max(floorPx, (int) (span * startRatio));
            int top = (int) (span * endRatio) - chunk.drainedRows();
            if (top <= bottom) {
                chunk.drained = true;
                continue;
            }
            float alpha = chunk.getAlpha() * parentAlpha;
            if (alpha <= 0f) continue;

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            OrbRenderHelper.paintFillSpan(graphics, MANA_BAR_TEX, barRect, vertical, isRightAnchored,
                    chunk.animOffset, animData, bottom, top, true, true, ticks);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, parentAlpha);
    }
}