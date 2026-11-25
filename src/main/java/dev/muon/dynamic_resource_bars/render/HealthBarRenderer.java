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
    private static boolean healthBarSetVisible = true;
    private static long healthBarDisabledStartTime = 0L;

    private static final List<OrbRenderHelper.DrainingChunk> fadingChunks = new ArrayList<>();
    private static final OrbRenderHelper.FillSmoother fillSmoother = new OrbRenderHelper.FillSmoother();
    private static float previousHealth = -1;
    private static float previousMaxHealth = -1;

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
        if (player == null) return new ScreenRect(0, 0, 0, 0);
        Position healthPos = HUDPositioning.getPositionFromAnchor(ModConfigManager.getClient().healthBarAnchor)
                .offset(ModConfigManager.getClient().healthTotalXOffset, ModConfigManager.getClient().healthTotalYOffset);
        int backgroundWidth = ModConfigManager.getClient().healthBackgroundWidth;
        int backgroundHeight = ModConfigManager.getClient().healthBackgroundHeight;
        return new ScreenRect(healthPos.x(), healthPos.y(), backgroundWidth, backgroundHeight);
    }

    public static ScreenRect getSubElementRect(SubElementType type, Player player) {
        ScreenRect complexRect = getScreenRect(player);
        if (complexRect == null || complexRect.width() == 0 && complexRect.height() == 0) return new ScreenRect(0, 0, 0, 0);

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
            case FOREGROUND_DETAIL:
                return new ScreenRect(x + ModConfigManager.getClient().healthOverlayXOffset,
                        y + ModConfigManager.getClient().healthOverlayYOffset,
                        ModConfigManager.getClient().healthOverlayWidth,
                        ModConfigManager.getClient().healthOverlayHeight);
            case TEXT:
                return new ScreenRect(x + ModConfigManager.getClient().healthTextXOffset,
                        y + ModConfigManager.getClient().healthTextYOffset,
                        ModConfigManager.getClient().healthBarWidth,
                        ModConfigManager.getClient().healthBarHeight);
            case ABSORPTION_TEXT:
                return new ScreenRect(x + ModConfigManager.getClient().healthAbsorptionTextXOffset,
                        y + ModConfigManager.getClient().healthAbsorptionTextYOffset,
                        50,
                        ModConfigManager.getClient().healthBarHeight);
            default:
                return new ScreenRect(0, 0, 0, 0);
        }
    }

    public static void render(GuiGraphics graphics, Player player, float maxHealth, float actualHealth, int absorptionAmount,
            #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif) {

        float partial = #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif;
        float ticks = player.tickCount + partial;

        updateChunkTracking(player, actualHealth, maxHealth, ticks);

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
            currentAlphaForRender = 1.0f;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);

        ScreenRect complexRect = getScreenRect(player);
        int xPos = complexRect.x();
        int yPos = complexRect.y();
        int backgroundWidth = ModConfigManager.getClient().healthBackgroundWidth;
        int backgroundHeight = ModConfigManager.getClient().healthBackgroundHeight;

        if (ModConfigManager.getClient().enableHealthBackground) {
            ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
            OrbRenderHelper.blitFullTexture(graphics,
                    DynamicResourceBars.loc("textures/gui/health_background.png"),
                    bgRect.x(), bgRect.y(), bgRect.width(), bgRect.height());
        }

        BarType barType = BarType.fromPlayerState(player);
        ResourceLocation barTex = DynamicResourceBars.loc("textures/gui/" + barType.getTexture() + ".png");
        OrbRenderHelper.AnimationData animData = OrbRenderHelper.getAnimationData(barTex);
        int animOffset = OrbRenderHelper.calculateAnimationOffset(animData, ticks);

        ScreenRect mainBarRect = getSubElementRect(SubElementType.BAR_MAIN, player);
        boolean isRightAnchored = ModConfigManager.getClient().healthBarAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT;

        renderBaseBar(graphics, barTex, animData, maxHealth, actualHealth, mainBarRect, animOffset, isRightAnchored, ticks);
        renderFadingChunks(graphics, mainBarRect, isRightAnchored, currentAlphaForRender, ticks);

        if (PlatformUtil.isModLoaded("appleskin")) {
            ItemStack heldFood = getHeldFood(player);
            if (!heldFood.isEmpty()) {
                renderHealthRestoredOverlay(graphics, player, heldFood, actualHealth, maxHealth, mainBarRect, animOffset, animData, isRightAnchored, currentAlphaForRender, ticks);
            }
        }

        renderBarOverlays(graphics, player, absorptionAmount, mainBarRect, currentAlphaForRender);

        if (ModConfigManager.getClient().enableHealthForeground) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);
            ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
            OrbRenderHelper.blitFullTexture(graphics,
                    DynamicResourceBars.loc("textures/gui/health_foreground.png"),
                    fgRect.x(), fgRect.y(), fgRect.width(), fgRect.height());
        }

        renderBackgroundOverlays(graphics, player, xPos, yPos, backgroundWidth, backgroundHeight, currentAlphaForRender);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

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

            RenderUtil.renderText(actualHealth, maxHealth, graphics, baseX, textY, color, alignment);
        }

        if (absorptionAmount > 0 || EditModeManager.isEditModeEnabled()) {
            String absorptionText = "+" + (EditModeManager.isEditModeEnabled() && absorptionAmount == 0 ? "8" : absorptionAmount);

            ScreenRect absorptionRect = getSubElementRect(SubElementType.ABSORPTION_TEXT, player);
            int absorptionTextX = absorptionRect.x();
            int absorptionTextY = absorptionRect.y() + (absorptionRect.height() / 2);

            ClientConfig config = ModConfigManager.getClient();
            int baseAbsorptionColor = config.healthTextColor & 0xFFFFFF;
            int absorptionAlpha = (int) (config.healthTextOpacity * currentAlphaForRender);
            absorptionAlpha = Math.max(10, Math.min(255, absorptionAlpha));
            int absorptionFinalColor = (absorptionAlpha << 24) | baseAbsorptionColor;

            RenderUtil.renderAdditionText(absorptionText, graphics, absorptionTextX, absorptionTextY, absorptionFinalColor);
        }

        if (EditModeManager.isEditModeEnabled()) {
            DraggableElement currentBarType = DraggableElement.HEALTH_BAR;
            if (EditModeManager.getFocusedElement() == currentBarType) {
                int focusedBorderColor = 0xA0FFFF00;
                ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
                graphics.renderOutline(bgRect.x() - 1, bgRect.y() - 1, bgRect.width() + 2, bgRect.height() + 2, focusedBorderColor);

                ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
                graphics.renderOutline(barRect.x() - 1, barRect.y() - 1, barRect.width() + 2, barRect.height() + 2, 0xA000FF00);

                if (ModConfigManager.getClient().enableHealthForeground) {
                    ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                    graphics.renderOutline(fgRect.x() - 1, fgRect.y() - 1, fgRect.width() + 2, fgRect.height() + 2, 0xA0FF00FF);
                }

                ScreenRect absorptionRect = getSubElementRect(SubElementType.ABSORPTION_TEXT, player);
                if (absorptionRect != null && absorptionRect.width() > 0 && absorptionRect.height() > 0) {
                    graphics.renderOutline(absorptionRect.x() - 1, absorptionRect.y() - 1,
                            absorptionRect.width() + 2, absorptionRect.height() + 2, 0x60FFFFFF);
                }

                graphics.renderOutline(complexRect.x() - 2, complexRect.y() - 2, complexRect.width() + 4, complexRect.height() + 4, 0x80FFFFFF);
            } else {
                graphics.renderOutline(complexRect.x() - 1, complexRect.y() - 1, complexRect.width() + 2, complexRect.height() + 2, 0x80FFFFFF);
            }
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static void updateChunkTracking(Player player, float currentHealth, float maxHealth, float ticks) {
        Iterator<OrbRenderHelper.DrainingChunk> it = fadingChunks.iterator();
        while (it.hasNext()) {
            OrbRenderHelper.DrainingChunk chunk = it.next();
            if (chunk.isExpired() || currentHealth >= chunk.endValue) {
                it.remove();
            }
        }

        if (previousHealth > 0 && currentHealth < previousHealth && previousMaxHealth == maxHealth) {
            String texture = BarType.fromPlayerState(player).getTexture();
            float chunkStart = Math.max(0f, currentHealth);
            OrbRenderHelper.DrainingChunk newest = fadingChunks.isEmpty() ? null : fadingChunks.get(fadingChunks.size() - 1);
            if (newest != null && newest.maxValue == maxHealth && newest.texture.equals(texture)) {
                newest.startValue = Math.min(newest.startValue, chunkStart);
            } else {
                if (fadingChunks.size() >= OrbRenderHelper.MAX_FADING_CHUNKS) fadingChunks.remove(0);
                ResourceLocation texLoc = DynamicResourceBars.loc("textures/gui/" + texture + ".png");
                OrbRenderHelper.AnimationData animData = OrbRenderHelper.getAnimationData(texLoc);
                int animOffset = OrbRenderHelper.calculateAnimationOffset(animData, ticks);
                fadingChunks.add(new OrbRenderHelper.DrainingChunk(chunkStart, previousHealth, maxHealth, texture, animOffset));
            }
        }

        previousHealth = currentHealth;
        previousMaxHealth = maxHealth;
    }

    private static void renderBaseBar(GuiGraphics graphics, ResourceLocation barTex, OrbRenderHelper.AnimationData animData,
                                      float maxHealth, float actualHealth, ScreenRect barRect,
                                      int animOffset, boolean isRightAnchored, float ticks) {
        float ratio = (maxHealth <= 0f) ? 0.0f : Math.max(0f, Math.min(1f, actualHealth / maxHealth));
        boolean vertical = ModConfigManager.getClient().healthFillDirection == FillDirection.VERTICAL;
        int span = vertical ? barRect.height() : barRect.width();

        int filled = (int) (span * ratio);
        if (filled <= 0 && actualHealth > 0) filled = 1;
        int liveEdge = fillSmoother.step(filled);
        if (liveEdge <= 0) return;

        OrbRenderHelper.paintFillSpan(graphics, barTex, barRect, vertical, isRightAnchored,
                animOffset, animData, 0, liveEdge, false, true, ticks);
    }

    private static void renderFadingChunks(GuiGraphics graphics, ScreenRect barRect,
                                           boolean isRightAnchored, float parentAlpha, float ticks) {
        if (fadingChunks.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        boolean vertical = ModConfigManager.getClient().healthFillDirection == FillDirection.VERTICAL;
        int span = vertical ? barRect.height() : barRect.width();
        int floorPx = fillSmoother.getLiveEdgePx();

        for (OrbRenderHelper.DrainingChunk chunk : fadingChunks) {
            if (chunk.maxValue <= 0f) continue;
            float startRatio = Math.max(0f, Math.min(1f, chunk.startValue / chunk.maxValue));
            float endRatio = Math.max(0f, Math.min(1f, chunk.endValue / chunk.maxValue));
            int bottom = Math.max(floorPx, (int) (span * startRatio));
            int top = (int) (span * endRatio) - chunk.drainedRows();
            if (top <= bottom) {
                chunk.drained = true;
                continue;
            }
            float alpha = chunk.getAlpha() * parentAlpha;
            if (alpha <= 0f) continue;

            ResourceLocation texLoc = DynamicResourceBars.loc("textures/gui/" + chunk.texture + ".png");
            OrbRenderHelper.AnimationData animData = OrbRenderHelper.getAnimationData(texLoc);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            OrbRenderHelper.paintFillSpan(graphics, texLoc, barRect, vertical, isRightAnchored,
                    chunk.animOffset, animData, bottom, top, true, true, ticks);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, parentAlpha);
    }

    private static void renderBarOverlays(GuiGraphics graphics, Player player, int absorptionAmount,
                                          ScreenRect barRect, float parentAlpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float tempScale = getTemperatureScale(player);
        renderTemperatureOverlay(graphics, tempScale, barRect.x(), barRect.y(), barRect.width(), barRect.height(), parentAlpha);

        if (absorptionAmount > 0) {
            float pulseAlpha = (0.5f + (TickHandler.getOverlayFlashAlpha() * 0.5f)) * parentAlpha;
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulseAlpha);
            OrbRenderHelper.blitFullTexture(graphics,
                    DynamicResourceBars.loc("textures/gui/absorption_overlay.png"),
                    barRect.x(), barRect.y(), barRect.width(), barRect.height());
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, parentAlpha);
        }

        if (PlatformUtil.isModLoaded("farmersdelight") && hasComfortEffect(player)) {
            if (player.getFoodData().getSaturationLevel() == 0.0F && player.isHurt() && !player.hasEffect(MobEffects.REGENERATION)) {
                float pulseAlpha = TickHandler.getOverlayFlashAlpha() * parentAlpha;
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulseAlpha);
                OrbRenderHelper.blitFullTexture(graphics,
                        DynamicResourceBars.loc("textures/gui/comfort_overlay.png"),
                        barRect.x(), barRect.y(), barRect.width(), barRect.height());
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, parentAlpha);
            }
        }

        if (player.hasEffect(MobEffects.REGENERATION)) {
            float pulseAlpha = TickHandler.getOverlayFlashAlpha() * parentAlpha;
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulseAlpha);
            OrbRenderHelper.blitFullTexture(graphics,
                    DynamicResourceBars.loc("textures/gui/regeneration_overlay.png"),
                    barRect.x(), barRect.y(), barRect.width(), barRect.height());
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, parentAlpha);
        }
    }

    private static void renderBackgroundOverlays(GuiGraphics graphics, Player player,
                                                 int complexX, int complexY, int backgroundWidth, int backgroundHeight,
                                                 float parentAlpha) {
        if (player.level().getLevelData().isHardcore()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, parentAlpha);
            OrbRenderHelper.blitFullTexture(graphics,
                    DynamicResourceBars.loc("textures/gui/hardcore_overlay.png"),
                    complexX, complexY, backgroundWidth, backgroundHeight);
        }

        float wetScale = getWetnessScale(player);
        if (wetScale > 0) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, wetScale * parentAlpha);
            OrbRenderHelper.blitFullTexture(graphics,
                    DynamicResourceBars.loc("textures/gui/wetness_overlay.png"),
                    complexX, complexY, backgroundWidth, backgroundHeight);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, parentAlpha);
        }
    }

    private static void renderTemperatureOverlay(GuiGraphics graphics, float tempScale,
                                                 int xPos, int yPos, int barWidth, int barHeight,
                                                 float parentAlpha) {
        if (tempScale > 0) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, parentAlpha);
            int heatWidth = (int) (barWidth * tempScale);
            OrbRenderHelper.blitFullTexture(graphics,
                    DynamicResourceBars.loc("textures/gui/heat_overlay.png"),
                    xPos, yPos, heatWidth, barHeight);
        } else if (tempScale < 0) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, parentAlpha);
            int coldWidth = (int) (barWidth * -tempScale);
            OrbRenderHelper.blitFullTexture(graphics,
                    DynamicResourceBars.loc("textures/gui/cold_overlay.png"),
                    xPos, yPos, coldWidth, barHeight);
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
        if (!PlatformUtil.isModLoaded("thermoo")) return 0.0f;
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
        int baseColor = config.healthTextColor & 0xFFFFFF;

        int alpha = config.healthTextOpacity;
        if (behavior == TextBehavior.WHEN_NOT_FULL && currentHealth >= maxHealth) {
            long timeSinceFull = System.currentTimeMillis() - fullHealthStartTime;
            alpha = (int) (alpha * (RenderUtil.calculateTextAlpha(timeSinceFull) / (float) RenderUtil.BASE_TEXT_ALPHA));
        }
        alpha = (int) (alpha * getHealthBarAlpha());
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
        if (behavior == TextBehavior.NEVER) return false;
        if (behavior == TextBehavior.ALWAYS) return true;

        boolean isFull = currentHealth >= maxHealth;
        if (isFull) {
            if (lastHealth < maxHealth || lastHealth == -1) {
                fullHealthStartTime = System.currentTimeMillis();
            }
            lastHealth = currentHealth;
            return (System.currentTimeMillis() - fullHealthStartTime) < RenderUtil.TEXT_DISPLAY_DURATION;
        } else {
            lastHealth = currentHealth;
            return true;
        }
    }

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
        if (isHealthBarVisible()) return 1.0f;
        long timeSinceDisabled = System.currentTimeMillis() - healthBarDisabledStartTime;
        if (timeSinceDisabled >= RenderUtil.BAR_FADEOUT_DURATION) return 0.0f;
        return Math.max(0.0f, 1.0f - (timeSinceDisabled / (float) RenderUtil.BAR_FADEOUT_DURATION));
    }

    private static ItemStack getHeldFood(Player player) {
        ItemStack mainHand = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (AppleSkinCompat.canConsume(mainHand, player)) return mainHand;

        ItemStack offHand = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);
        if (AppleSkinCompat.canConsume(offHand, player)) return offHand;

        return ItemStack.EMPTY;
    }

    private static void renderHealthRestoredOverlay(GuiGraphics graphics, Player player, ItemStack heldFood,
                                                    float currentHealth, float maxHealth,
                                                    ScreenRect barRect, int animOffset,
                                                    OrbRenderHelper.AnimationData animData,
                                                    boolean isRightAnchored, float parentAlpha, float ticks) {
        if (!PlatformUtil.isModLoaded("appleskin")) return;

        float healthRestoration = AppleSkinCompat.getEstimatedHealthRestoration(heldFood, player);
        if (healthRestoration <= 0 || currentHealth >= maxHealth || maxHealth <= 0) return;

        float restoredHealth = Math.min(maxHealth, currentHealth + healthRestoration);
        boolean vertical = ModConfigManager.getClient().healthFillDirection == FillDirection.VERTICAL;
        int span = vertical ? barRect.height() : barRect.width();

        int currentSize = (int) (span * (currentHealth / maxHealth));
        int restoredSize = (int) (span * (restoredHealth / maxHealth));
        int overlaySize = restoredSize - currentSize;
        if (overlaySize <= 0) return;

        int fromPx = vertical ? fillSmoother.getLiveEdgePx() : currentSize;
        int toPx = fromPx + overlaySize;

        ResourceLocation texLoc = DynamicResourceBars.loc("textures/gui/" + BarType.fromPlayerState(player).getTexture() + ".png");
        float pulse = (0.5f + (TickHandler.getOverlayFlashAlpha() * 0.5f)) * parentAlpha;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulse);
        OrbRenderHelper.paintFillSpan(graphics, texLoc, barRect, vertical, isRightAnchored,
                animOffset, animData, fromPx, toPx, true, true, ticks);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, parentAlpha);
    }

    private static boolean hasComfortEffect(Player player) {
        #if UPTO_20_1
        var comfortEffect = ModEffects.COMFORT.get();
        return player.hasEffect(comfortEffect);
        #elif NEWER_THAN_20_1
        var comfortEffect = ModEffects.COMFORT;
        return player.hasEffect(comfortEffect);
        #else
        return false;
        #endif
    }
}