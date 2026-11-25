package dev.muon.dynamic_resource_bars.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
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
    private static boolean staminaBarSetVisible = true;
    private static long staminaBarDisabledStartTime = 0L;

    private static float lastMountHealth = -1;
    private static float lastMountMaxHealth = -1;
    private static long fullMountHealthStartTime = 0;

    private static final List<OrbRenderHelper.DrainingChunk> fadingChunks = new ArrayList<>();
    private static final OrbRenderHelper.FillSmoother fillSmoother = new OrbRenderHelper.FillSmoother();
    private static float previousValue = -1;
    private static float previousMax = -1;
    private static boolean wasMounted = false;

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
                return new ScreenRect(x + ModConfigManager.getClient().staminaTextXOffset,
                        y + ModConfigManager.getClient().staminaTextYOffset,
                        ModConfigManager.getClient().staminaBarWidth,
                        ModConfigManager.getClient().staminaBarHeight);
            default:
                return new ScreenRect(0, 0, 0, 0);
        }
    }

    public static void render(GuiGraphics graphics, Player player, #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif) {
        if (!Minecraft.getInstance().gameMode.canHurtPlayer() && !EditModeManager.isEditModeEnabled()) {
            return;
        }

        StaminaProvider staminaProvider = StaminaProviderManager.getCurrentProvider();
        if (staminaProvider == null) return;

        float partial = #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif;
        float ticks = player.tickCount + partial;

        BarValues values = getBarValues(player, staminaProvider);
        if (wasMounted != values.isMounted) {
            fillSmoother.reset();
        }
        updateChunkTracking(player, values, staminaProvider, ticks);

        boolean shouldFade = shouldBarFade(player, values);
        setStaminaBarVisibility(!shouldFade || EditModeManager.isEditModeEnabled());

        if (!isStaminaBarVisible() && !EditModeManager.isEditModeEnabled() && (System.currentTimeMillis() - staminaBarDisabledStartTime) > RenderUtil.BAR_FADEOUT_DURATION) {
            return;
        }

        float currentAlphaForRender = getStaminaBarAlpha();
        if (EditModeManager.isEditModeEnabled() && !isStaminaBarVisible()) {
            currentAlphaForRender = 1.0f;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);

        ScreenRect complexRect = getScreenRect(player);
        boolean isRightAnchored = ModConfigManager.getClient().staminaBarAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT;

        if (ModConfigManager.getClient().enableStaminaBackground) {
            ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
            OrbRenderHelper.blitFullTexture(graphics,
                    DynamicResourceBars.loc("textures/gui/stamina_background.png"),
                    bgRect.x(), bgRect.y(), bgRect.width(), bgRect.height());
        }

        String barTextureName = resolveBarTextureName(player, values, staminaProvider);
        ResourceLocation barTex = DynamicResourceBars.loc("textures/gui/" + barTextureName + ".png");
        OrbRenderHelper.AnimationData animData = OrbRenderHelper.getAnimationData(barTex);
        int animOffset = OrbRenderHelper.calculateAnimationOffset(animData, ticks);

        ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
        renderBaseBar(graphics, barTex, animData, values, barRect, animOffset, isRightAnchored, ticks);
        renderFadingChunks(graphics, barRect, values, isRightAnchored, currentAlphaForRender, ticks);

        if (staminaProvider.shouldShowOverlays() && !values.isMounted) {
            if (PlatformUtil.isModLoaded("appleskin")) {
                ItemStack heldFood = getHeldFood(player);
                renderHungerRestoredOverlay(graphics, player, heldFood, barRect, animOffset, isRightAnchored, currentAlphaForRender, ticks);
                renderSaturationOverlay(graphics, player, barRect, currentAlphaForRender);
            }

            if (PlatformUtil.isModLoaded("farmersdelight") && hasNourishmentEffect(player)) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                float pulseAlpha = TickHandler.getOverlayFlashAlpha() * currentAlphaForRender;
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulseAlpha);
                OrbRenderHelper.blitFullTexture(graphics,
                        DynamicResourceBars.loc("textures/gui/nourishment_overlay.png"),
                        barRect.x(), barRect.y(), barRect.width(), barRect.height());
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);
            }
        }

        if (ModConfigManager.getClient().enableStaminaForeground) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);
            ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
            OrbRenderHelper.blitFullTexture(graphics,
                    DynamicResourceBars.loc("textures/gui/stamina_foreground.png"),
                    fgRect.x(), fgRect.y(), fgRect.width(), fgRect.height());
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
                graphics.renderOutline(complexRect.x() - 1, complexRect.y() - 1, complexRect.width() + 2, complexRect.height() + 2, 0x80FFFFFF);
            }
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static String resolveBarTextureName(Player player, BarValues values, StaminaProvider provider) {
        if (values.isMounted) {
            float healthPercentage = (values.max <= 0f) ? 0f : (values.current / values.max);
            return healthPercentage <= 0.2f ? "stamina_bar_critical" : "stamina_bar_mounted";
        }
        return provider.getBarTexture(player, values.current);
    }

    private static void updateChunkTracking(Player player, BarValues values, StaminaProvider provider, float ticks) {
        Iterator<OrbRenderHelper.DrainingChunk> it = fadingChunks.iterator();
        while (it.hasNext()) {
            OrbRenderHelper.DrainingChunk chunk = it.next();
            if (chunk.isExpired() || (chunk.isMounted == values.isMounted && values.current >= chunk.endValue)) {
                it.remove();
            }
        }

        if (previousValue > 0 && values.current < previousValue &&
                wasMounted == values.isMounted && previousMax == values.max) {
            String texture = resolveBarTextureName(player, values, provider);
            float chunkStart = Math.max(0f, values.current);
            OrbRenderHelper.DrainingChunk newest = fadingChunks.isEmpty() ? null : fadingChunks.get(fadingChunks.size() - 1);
            if (newest != null && newest.isMounted == values.isMounted && newest.maxValue == values.max && newest.texture.equals(texture)) {
                newest.startValue = Math.min(newest.startValue, chunkStart);
            } else {
                if (fadingChunks.size() >= OrbRenderHelper.MAX_FADING_CHUNKS) fadingChunks.remove(0);
                ResourceLocation texLoc = DynamicResourceBars.loc("textures/gui/" + texture + ".png");
                OrbRenderHelper.AnimationData animData = OrbRenderHelper.getAnimationData(texLoc);
                int animOffset = OrbRenderHelper.calculateAnimationOffset(animData, ticks);
                fadingChunks.add(new OrbRenderHelper.DrainingChunk(chunkStart, previousValue, values.max, texture, animOffset, values.isMounted));
            }
        }

        previousValue = values.current;
        previousMax = values.max;
        wasMounted = values.isMounted;
    }

    private static void renderBaseBar(GuiGraphics graphics, ResourceLocation barTex, OrbRenderHelper.AnimationData animData,
                                      BarValues values, ScreenRect barAreaRect,
                                      int animOffset, boolean isRightAnchored, float ticks) {
        float ratio = (values.max <= 0f) ? 0.0f : Math.max(0f, Math.min(1f, values.current / values.max));
        boolean vertical = ModConfigManager.getClient().staminaFillDirection == FillDirection.VERTICAL;
        int span = vertical ? barAreaRect.height() : barAreaRect.width();

        int filled = (int) (span * ratio);
        if (filled <= 0 && values.current > 0) filled = 1;
        int liveEdge = fillSmoother.step(filled);
        if (liveEdge <= 0) return;

        OrbRenderHelper.paintFillSpan(graphics, barTex, barAreaRect, vertical, isRightAnchored,
                animOffset, animData, 0, liveEdge, false, true, ticks);
    }

    private static void renderFadingChunks(GuiGraphics graphics, ScreenRect barRect, BarValues currentValues,
                                           boolean isRightAnchored, float parentAlpha, float ticks) {
        if (fadingChunks.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        boolean vertical = ModConfigManager.getClient().staminaFillDirection == FillDirection.VERTICAL;
        int span = vertical ? barRect.height() : barRect.width();
        int floorPx = fillSmoother.getLiveEdgePx();

        for (OrbRenderHelper.DrainingChunk chunk : fadingChunks) {
            if (chunk.isMounted != currentValues.isMounted || chunk.maxValue <= 0f) continue;

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

    private static BarValues getBarValues(Player player, StaminaProvider provider) {
        if (ModConfigManager.getClient().mergeMountHealth && player.getVehicle() instanceof LivingEntity mount) {
            return new BarValues(mount.getHealth(), mount.getMaxHealth(), true);
        }
        return new BarValues(provider.getCurrentStamina(player), provider.getMaxStamina(player), false);
    }

    private static boolean shouldBarFade(Player player, BarValues values) {
        if (values.isMounted) {
            return ModConfigManager.getClient().fadeHealthWhenFull && values.current >= values.max;
        } else {
            return ModConfigManager.getClient().fadeStaminaWhenFull && values.current >= values.max;
        }
    }

    private static boolean shouldRenderStaminaText(float currentValue, float maxValue, Player player, boolean isMounted) {
        TextBehavior textBehavior = isMounted ?
                ModConfigManager.getClient().showHealthText :
                ModConfigManager.getClient().showStaminaText;

        if (EditModeManager.isEditModeEnabled()) {
            if (textBehavior == TextBehavior.ALWAYS || textBehavior == TextBehavior.WHEN_NOT_FULL) return true;
        }
        if (textBehavior == TextBehavior.NEVER) return false;
        if (textBehavior == TextBehavior.ALWAYS) return true;

        if (isMounted) {
            boolean isFull = currentValue >= maxValue;
            if (isFull) {
                if (lastMountHealth < maxValue || lastMountMaxHealth != maxValue || lastMountHealth == -1) {
                    fullMountHealthStartTime = System.currentTimeMillis();
                }
                lastMountHealth = currentValue;
                lastMountMaxHealth = maxValue;
                return (System.currentTimeMillis() - fullMountHealthStartTime) < RenderUtil.TEXT_DISPLAY_DURATION;
            } else {
                lastMountHealth = currentValue;
                lastMountMaxHealth = maxValue;
                return true;
            }
        } else {
            boolean isFull = currentValue >= maxValue;
            if (isFull) {
                if (lastStamina < maxValue || lastStamina == -1) {
                    fullStaminaStartTime = System.currentTimeMillis();
                }
                lastStamina = currentValue;
                return (System.currentTimeMillis() - fullStaminaStartTime) < RenderUtil.TEXT_DISPLAY_DURATION;
            } else {
                lastStamina = currentValue;
                return true;
            }
        }
    }

    private static int getStaminaTextColor(float currentValue, float maxValue, boolean isMounted) {
        TextBehavior textBehavior = isMounted ?
                ModConfigManager.getClient().showHealthText :
                ModConfigManager.getClient().showStaminaText;
        ClientConfig config = ModConfigManager.getClient();
        int baseColor = isMounted ? (config.healthTextColor & 0xFFFFFF) : (config.staminaTextColor & 0xFFFFFF);
        int alpha = isMounted ? config.healthTextOpacity : config.staminaTextOpacity;

        if (textBehavior == TextBehavior.WHEN_NOT_FULL && currentValue >= maxValue) {
            long timeSinceFull = System.currentTimeMillis() - (isMounted ? fullMountHealthStartTime : fullStaminaStartTime);
            alpha = (int) (alpha * (RenderUtil.calculateTextAlpha(timeSinceFull) / (float) RenderUtil.BASE_TEXT_ALPHA));
        }

        alpha = (int) (alpha * getStaminaBarAlpha());
        alpha = Math.max(10, Math.min(255, alpha));
        return (alpha << 24) | baseColor;
    }

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
        if (isStaminaBarVisible()) return 1.0f;
        long timeSinceDisabled = System.currentTimeMillis() - staminaBarDisabledStartTime;
        if (timeSinceDisabled >= RenderUtil.BAR_FADEOUT_DURATION) return 0.0f;
        return Math.max(0.0f, 1.0f - (timeSinceDisabled / (float) RenderUtil.BAR_FADEOUT_DURATION));
    }

    private static void renderSaturationOverlay(GuiGraphics graphics, Player player, ScreenRect barRect, float parentAlpha) {
        if (!PlatformUtil.isModLoaded("appleskin")) return;

        float saturation = player.getFoodData().getSaturationLevel();
        if (saturation <= 0) return;

        float saturationPercent = Math.min(1.0f, saturation / 20f);
        float pulseAlpha = (0.5f + (TickHandler.getOverlayFlashAlpha() * 0.5f)) * saturationPercent * parentAlpha;

        ResourceLocation satLoc = DynamicResourceBars.loc("textures/gui/saturation_overlay.png");
        if (!OrbRenderHelper.resourceExists(satLoc)) {
            satLoc = DynamicResourceBars.loc("textures/gui/protection_overlay.png");
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulseAlpha);
        OrbRenderHelper.blitFullTexture(graphics, satLoc, barRect.x(), barRect.y(), barRect.width(), barRect.height());
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, parentAlpha);
    }

    private static void renderHungerRestoredOverlay(GuiGraphics graphics, Player player, ItemStack heldFood,
                                                    ScreenRect barRect, int animOffset, boolean isRightAnchored,
                                                    float parentAlpha, float ticks) {
        if (!PlatformUtil.isModLoaded("appleskin")) return;

        AppleSkinCompat.FoodData foodData = AppleSkinCompat.getFoodValues(heldFood, player);
        if (foodData.isEmpty()) return;

        float currentHunger = player.getFoodData().getFoodLevel();
        float restoredHunger = Math.min(20f, currentHunger + foodData.hunger);
        if (restoredHunger <= currentHunger) return;

        boolean vertical = ModConfigManager.getClient().staminaFillDirection == FillDirection.VERTICAL;
        int span = vertical ? barRect.height() : barRect.width();
        int currentSize = (int) (span * (currentHunger / 20f));
        int restoredSize = (int) (span * (restoredHunger / 20f));
        int overlaySize = restoredSize - currentSize;
        if (overlaySize <= 0) return;

        StaminaProvider provider = StaminaProviderManager.getCurrentProvider();
        String barTexture = "stamina_bar";
        if (provider instanceof dev.muon.dynamic_resource_bars.compat.FoodStaminaProvider) {
            barTexture = provider.getBarTexture(player, restoredHunger);
        }

        ResourceLocation texLoc = DynamicResourceBars.loc("textures/gui/" + barTexture + ".png");
        OrbRenderHelper.AnimationData animData = OrbRenderHelper.getAnimationData(texLoc);
        int fromPx = vertical ? fillSmoother.getLiveEdgePx() : currentSize;
        int toPx = fromPx + overlaySize;
        float pulse = (0.5f + (TickHandler.getOverlayFlashAlpha() * 0.5f)) * parentAlpha;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulse);
        OrbRenderHelper.paintFillSpan(graphics, texLoc, barRect, vertical, isRightAnchored,
                animOffset, animData, fromPx, toPx, true, true, ticks);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, parentAlpha);
    }

    private static ItemStack getHeldFood(Player player) {
        ItemStack mainHand = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (AppleSkinCompat.canConsume(mainHand, player)) return mainHand;

        ItemStack offHand = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);
        if (AppleSkinCompat.canConsume(offHand, player)) return offHand;

        return ItemStack.EMPTY;
    }

    private static boolean hasNourishmentEffect(Player player) {
        #if UPTO_20_1
        var nourishmentEffect = ModEffects.NOURISHMENT.get();
        return player.hasEffect(nourishmentEffect);
        #elif NEWER_THAN_20_1
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
        return false;
    }
}