package dev.muon.dynamic_resource_bars.render;


import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.muon.dynamic_resource_bars.compat.AppleSkinCompat;
#if UPTO_20_1 && FABRIC
import moriyashiine.bewitchment.api.BewitchmentAPI;
import moriyashiine.bewitchment.common.registry.BWComponents;
import moriyashiine.bewitchment.api.component.BloodComponent;
#endif

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif


import vectorwing.farmersdelight.common.registry.ModEffects;
import dev.muon.dynamic_resource_bars.config.ClientConfig;

public class StaminaBarRenderer {
    private static final float CRITICAL_THRESHOLD = 6.0f;
    private static float lastStamina = -1;
    private static long fullStaminaStartTime = 0;
    private static boolean staminaBarSetVisible = true; // Default to visible
    private static long staminaBarDisabledStartTime = 0L;
    
    // Mount health tracking
    private static float lastMountHealth = -1;
    private static float lastMountMaxHealth = -1;
    private static long fullMountHealthStartTime = 0;

    private enum BarType {
        NORMAL("stamina_bar"),
        BLOOD("stamina_bar_blood"),
        NOURISHED("stamina_bar_nourished"),
        HUNGER("stamina_bar_hunger"),
        CRITICAL("stamina_bar_critical"),
        MOUNTED("stamina_bar_mounted");

        private final String texture;

        BarType(String texture) {
            this.texture = texture;
        }

        public String getTexture() {
            return texture;
        }

        public static BarType fromPlayerState(Player player, float value) {
            if (player.getVehicle() instanceof LivingEntity mount) {
                float healthPercentage = value / mount.getMaxHealth();
                if (healthPercentage <= 0.2f) {
                    return CRITICAL;
                }
                return MOUNTED;
            }
            #if UPTO_20_1 && FABRIC
            if (PlatformUtil.isModLoaded("bewitchment") && BewitchmentAPI.isVampire(player, true)) {
                return BLOOD;
            }
            #endif
            if (PlatformUtil.isModLoaded("farmersdelight") && hasNourishmentEffect(player)) {
                return NOURISHED;
            }
            if (player.hasEffect(MobEffects.HUNGER)) return HUNGER;
            if (value <= CRITICAL_THRESHOLD) return CRITICAL;
            return NORMAL;
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
        // Determine the bar values based on player state
        BarValues values = getBarValues(player);
        
        // Determine fade behavior
        boolean shouldFade = shouldBarFade(player, values);
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
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);

        ScreenRect complexRect = getScreenRect(player);

        int animationCycles = ModConfigManager.getClient().staminaBarAnimationCycles;
        int frameHeight = ModConfigManager.getClient().staminaBarFrameHeight;
        int animOffset = (int) (((player.tickCount + #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif ) / 3) % animationCycles) * frameHeight;
        boolean isRightAnchored = ModConfigManager.getClient().staminaBarAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT;

        if (ModConfigManager.getClient().enableStaminaBackground) {
            ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/stamina_background.png"),
                    bgRect.x(), bgRect.y(), 0, 0, bgRect.width(), bgRect.height(), 256, 256
            );
        }

        ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
        renderBaseBar(graphics, player, values.current, values.max,
                barRect,
                animOffset, isRightAnchored);

        // Overlays should not show for vampires or when mounted
        if (values.type == BarValueType.FOOD && !values.isMounted) {
            if (AppleSkinCompat.isLoaded()) {
                ItemStack heldFood = getHeldFood(player);
                renderHungerRestoredOverlay(graphics, player, heldFood, barRect, #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif , animOffset, isRightAnchored);
                renderSaturationOverlay(graphics, player, barRect, animOffset, isRightAnchored);
            }

            if (PlatformUtil.isModLoaded("farmersdelight") && hasNourishmentEffect(player)) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                float pulseAlpha = TickHandler.getOverlayFlashAlpha();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulseAlpha);

                graphics.blit(
                        DynamicResourceBars.loc("textures/gui/nourishment_overlay.png"),
                        barRect.x(), barRect.y(),
                        0, 0, barRect.width(), barRect.height(),
                        256, 256
                );
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.disableBlend();
            }
        }

        if (ModConfigManager.getClient().enableStaminaForeground) {
            ScreenRect fgRect = getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/stamina_foreground.png"),
                    fgRect.x(), fgRect.y(),
                    0, 0, fgRect.width(), fgRect.height(),
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

    // Helper class to hold bar values
    private static class BarValues {
        float current;
        float max;
        BarValueType type;
        boolean isMounted;
        
        BarValues(float current, float max, BarValueType type, boolean isMounted) {
            this.current = current;
            this.max = max;
            this.type = type;
            this.isMounted = isMounted;
        }
    }
    
    private enum BarValueType {
        FOOD,
        BLOOD,
        MOUNT_HEALTH
    }
    
    // Clean method to determine bar values based on player state
    private static BarValues getBarValues(Player player) {
        // Check if mounted first (highest priority)
        if (player.getVehicle() instanceof LivingEntity mount) {
            return new BarValues(mount.getHealth(), mount.getMaxHealth(), BarValueType.MOUNT_HEALTH, true);
        }
        
        // Check if vampire
        #if UPTO_20_1 && FABRIC
        if (PlatformUtil.isModLoaded("bewitchment") && BewitchmentAPI.isVampire(player, true)) {
            float bloodCurrent = BWComponents.BLOOD_COMPONENT.get(player).getBlood();
            float bloodMax = BloodComponent.MAX_BLOOD;
            return new BarValues(bloodCurrent, bloodMax, BarValueType.BLOOD, false);
        }
        #endif
        
        // Default to food
        return new BarValues(player.getFoodData().getFoodLevel(), 20f, BarValueType.FOOD, false);
    }
    
    // Clean method to determine fade behavior
    private static boolean shouldBarFade(Player player, BarValues values) {
        switch (values.type) {
            case MOUNT_HEALTH:
                return ModConfigManager.getClient().fadeHealthWhenFull && values.current >= values.max;
            case BLOOD:
            case FOOD:
                return ModConfigManager.getClient().fadeStaminaWhenFull && values.current >= values.max;
            default:
                return false;
        }
    }

    private static void renderBaseBar(GuiGraphics graphics, Player player, float currentStamina, float maxStamina,
                                      ScreenRect barAreaRect,
                                      int animOffset, boolean isRightAnchored) {
        BarType barType = BarType.fromPlayerState(player, currentStamina);
        int totalBarWidth = barAreaRect.width();
        int barHeight = barAreaRect.height();
        float currentStaminaRatio = (maxStamina == 0) ? 0.0f : (currentStamina / maxStamina);

        FillDirection fillDirection = ModConfigManager.getClient().staminaFillDirection;

        if (fillDirection == FillDirection.VERTICAL) {
            int partialBarHeight = (int) (barHeight * currentStaminaRatio);
            if (partialBarHeight <= 0 && currentStamina > 0) partialBarHeight = 1;
            if (partialBarHeight > barHeight) partialBarHeight = barHeight;

            int barX = barAreaRect.x();
            int barY = barAreaRect.y() + (barHeight - partialBarHeight); // Fill from bottom up
            // Adjust texture V offset to draw the correct part of the texture
            int textureVOffset = animOffset + (barHeight - partialBarHeight);

            if (partialBarHeight > 0) {
                graphics.blit(
                        DynamicResourceBars.loc("textures/gui/" + barType.getTexture() + ".png"),
                        barX, barY,
                        0, textureVOffset, // Use 0 for U, adjusted V for vertical fill
                        totalBarWidth, partialBarHeight, // Use full width, partial height
                        256, 1024
                );
            }
        } else { // HORIZONTAL
            int partialBarWidth = (int) (totalBarWidth * currentStaminaRatio);
            if (partialBarWidth <= 0 && currentStamina > 0) partialBarWidth = 1;
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
                graphics.blit(
                        DynamicResourceBars.loc("textures/gui/" + barType.getTexture() + ".png"),
                        barRenderX, barRenderY,
                        uTexOffset, animOffset, // Use calculated uTexOffset
                        partialBarWidth, barHeight,
                        256, 1024
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

    private static void renderSaturationOverlay(GuiGraphics graphics, Player player, ScreenRect barRect, int animOffset, boolean isRightAnchored) {
        if (!AppleSkinCompat.isLoaded()) {
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

        if (fillDirection == FillDirection.VERTICAL) {
            int overlayHeight = (int) (barRect.height() * saturationPercent);
            if (overlayHeight > 0) {
                graphics.blit(
                        DynamicResourceBars.loc("textures/gui/protection_overlay.png"), // Placeholder texture
                        barRect.x(),
                        barRect.y() + (barRect.height() - overlayHeight),
                        0, 0,
                        barRect.width(), overlayHeight,
                        256, 256
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

                graphics.blit(
                        DynamicResourceBars.loc("textures/gui/protection_overlay.png"), // Placeholder texture
                        xPos, barRect.y(),
                        uTexOffset, 0, // Use calculated uTexOffset, vOffset usually 0 for horizontal overlays unless animated differently
                        overlayWidth, barRect.height(),
                        256, 256
                );
            }
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static void renderHungerRestoredOverlay(GuiGraphics graphics, Player player, ItemStack heldFood,
                                                    ScreenRect barRect, float partialTicks, int animOffset, boolean isRightAnchored) {
        if (!AppleSkinCompat.isLoaded()) {
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
        // Use the bar type that would apply at the restored hunger level
        BarType barType = BarType.fromPlayerState(player, restoredHunger);

        if (fillDirection == FillDirection.VERTICAL) {
            int currentHeight = (int) (barRect.height() * (currentHunger / 20f));
            int restoredHeight = (int) (barRect.height() * (restoredHunger / 20f));
            int overlayHeight = restoredHeight - currentHeight;

            if (overlayHeight > 0) {
                int yPos = barRect.y() + (barRect.height() - restoredHeight);
                int textureVOffset = animOffset + (barRect.height() - restoredHeight);

                graphics.blit(
                        DynamicResourceBars.loc("textures/gui/" + barType.getTexture() + ".png"),
                        barRect.x(), yPos,
                        0, textureVOffset,
                        barRect.width(), overlayHeight,
                        256, 1024
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

                graphics.blit(
                        DynamicResourceBars.loc("textures/gui/" + barType.getTexture() + ".png"),
                        xDrawPos, barRect.y(),
                        uTexOffset, animOffset, // Use the calculated uTexOffset
                        overlayWidth, barRect.height(),
                        256, 1024
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

    private static boolean isVampire(Player player) {
        #if UPTO_20_1 && FABRIC
            return BewitchmentAPI.isVampire(player, true);
        #else
            // TODO: More vampire transformation mods here
            return false;
        #endif
    }
}