package dev.muon.dynamic_resource_bars.render;


import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.Position;
import dev.muon.dynamic_resource_bars.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif

import java.lang.reflect.Method;

import static dev.muon.dynamic_resource_bars.util.RenderUtil.TEXT_DISPLAY_DURATION;

public class HealthBarRenderer {

    private static float lastHealth = -1;
    private static long fullHealthStartTime = 0;

    private enum BarType {
        NORMAL("health_bar"),
        POISON("health_bar_poison"),
        WITHER("health_bar_wither"),
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

    public static void render(GuiGraphics graphics, Player player, float maxHealth, float actualHealth, int absorptionAmount, #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif) {

        if (!Minecraft.getInstance().gameMode.canHurtPlayer()) {
            return;
        }
        Position healthPos = HUDPositioning.getHealthAnchor()
                .offset(AllConfigs.client().healthBarXOffset.get(), AllConfigs.client().healthBarYOffset.get());

        // Configs
        int borderWidth = AllConfigs.client().healthBorderWidth.get();
        int borderHeight = AllConfigs.client().healthBorderHeight.get();
        int barWidth = AllConfigs.client().healthBarWidth.get();
        int barHeight = AllConfigs.client().healthBarHeight.get();
        int barXOffset = AllConfigs.client().healthBarXOffset.get();
        int barYOffset = AllConfigs.client().healthBarYOffset.get();
        int animationCycles = AllConfigs.client().healthBarAnimationCycles.get(); // Total frames in animation
        int frameHeight = AllConfigs.client().healthBarFrameHeight.get();      // Height of each frame in texture

        int xPos = healthPos.x();
        int yPos = healthPos.y();

        graphics.blit(
                DynamicResourceBars.loc("textures/gui/health_border.png"), xPos, yPos, 0, 0, borderWidth, borderHeight, 256, 256
        );

        int animOffset = (int) (((player.tickCount +#if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif) / 3) % animationCycles) * frameHeight;

        renderBaseBar(graphics, player, maxHealth, actualHealth, xPos, yPos, barWidth, barHeight, barXOffset, barYOffset, animOffset);
        renderBarOverlays(graphics, player, absorptionAmount, xPos, yPos, barWidth, barHeight, barXOffset, barYOffset);
        renderBorderOverlays(graphics, player, xPos, yPos, borderWidth, borderHeight);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);


        int textX = (xPos + (borderWidth / 2));
        int textY = (yPos + barYOffset);

        if (shouldRenderText(actualHealth, maxHealth)) {
            int color = getHealthTextColor();
            RenderUtil.renderText(actualHealth, maxHealth,
                    graphics, textX, textY, color);
        }
        if (absorptionAmount > 0) {
            String absorptionText = "+" + absorptionAmount;
            int absorptionX = xPos + borderWidth - barXOffset -
                    (Minecraft.getInstance().font.width(absorptionText) / 2);
            RenderUtil.renderAdditionText(absorptionText, graphics, absorptionX, textY, (RenderUtil.BASE_TEXT_ALPHA << 24) | 0xFFFFFF);
        }
    }

    private static void renderBaseBar(GuiGraphics graphics, Player player, float maxHealth, float actualHealth,
                                      int xPos, int yPos, int barWidth, int barHeight,
                                      int barXOffset, int barYOffset, int animOffset) {
        BarType barType = BarType.fromPlayerState(player);
        int partialBarWidth = (int) (barWidth * (actualHealth / maxHealth));

        graphics.blit(
                DynamicResourceBars.loc("textures/gui/" + barType.getTexture() + ".png"),
                xPos + barXOffset, yPos + barYOffset,
                0, animOffset, partialBarWidth, barHeight,
                256, 256
        );
    }

    private static void renderBarOverlays(GuiGraphics graphics, Player player, int absorptionAmount,
                                          int xPos, int yPos, int barWidth, int barHeight,
                                          int barXOffset, int barYOffset) {

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float tempScale = getTemperatureScale(player);
        renderTemperatureOverlay(graphics, tempScale, xPos, yPos, barWidth, barHeight, barXOffset, barYOffset);

        if (absorptionAmount > 0) {
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/absorption_overlay.png"),
                    xPos + barXOffset, yPos + barYOffset,
                    0, 0, barWidth, barHeight,
                    256, 256
            );
        }
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }


    private static void renderBorderOverlays(GuiGraphics graphics, Player player,
                                             int xPos, int yPos, int borderWidth, int borderHeight) {

        if (player.level().getLevelData().isHardcore()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/hardcore_overlay.png"),
                    xPos, yPos,
                    0, 0, borderWidth, borderHeight,
                    256, 256
            );
            RenderSystem.disableBlend();
        }

        float wetScale = getWetnessScale(player);
        if (wetScale > 0) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, wetScale);
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/wetness_overlay.png"),
                    xPos, yPos,
                    0, 0, borderWidth, borderHeight,
                    256, 256
            );
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }

        if (AllConfigs.client().healthDetailOverlay.get()) {
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/detail_overlay.png"),
                    xPos + AllConfigs.client().healthOverlayXOffset.get(),
                    yPos + AllConfigs.client().healthOverlayYOffset.get(),
                    0, 0, borderWidth, borderHeight,
                    256, 256
            );
        }
    }


    private static void renderTemperatureOverlay(GuiGraphics graphics, float tempScale,
                                                 int xPos, int yPos, int barWidth, int barHeight,
                                                 int barXOffset, int barYOffset) {
        if (tempScale > 0) {
            int heatWidth = (int) (barWidth * tempScale);
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/heat_overlay.png"),
                    xPos + barXOffset, yPos + barYOffset,
                    0, 0, heatWidth, barHeight,
                    256, 256
            );
        } else if (tempScale < 0) {
            int coldWidth = (int) (barWidth * -tempScale);
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/cold_overlay.png"),
                    xPos + barXOffset, yPos + barYOffset,
                    0, 0, coldWidth, barHeight,
                    256, 256
            );
        }
    }

    // TODO: Move to something less cursed / easier to extend
    private static float getWetnessScale(Player player) {
        try {
            Method getWetScale = player.getClass().getMethod("thermoo$getSoakedScale");
            return (float) getWetScale.invoke(player);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    // TODO: Move to something less cursed / easier to extend
    private static boolean isScorched(Player player) {
        try {
            Method getMaxTemp = player.getClass().getMethod("thermoo$getMaxTemperature");
            Method getTemp = player.getClass().getMethod("thermoo$getTemperature");

            int maxTemperature = (int) getMaxTemp.invoke(player);
            int temperature = (int) getTemp.invoke(player);

            return temperature >= maxTemperature - 1;
        } catch (Exception e) {
            // Thermoo not present
            return false;
        }
    }

    private static boolean isFrozen(Player player) {
        if (player.isFullyFrozen()) {
            return true;
        }

        // TODO: Move to something less cursed / easier to extend
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

        return false;
    }

    // TODO: Move to something less cursed / easier to extend
    private static float getTemperatureScale(Player player) {
        try {
            Method getTempScale = player.getClass().getMethod("thermoo$getTemperatureScale");
            return (float) getTempScale.invoke(player);
        } catch (Exception e) {
            return 0.0f;
        }
    }


    private static int getHealthTextColor() {
        long timeSinceFullHealth = fullHealthStartTime > 0 ?
                System.currentTimeMillis() - fullHealthStartTime : 0;

        int alpha = RenderUtil.calculateTextAlpha(timeSinceFullHealth);

        // Values lower than 10 cause rendering artifacts
        alpha = Math.max(10, alpha);

        return (alpha << 24) | 0xFFFFFF;
    }

    private static boolean shouldRenderText(float currentHealth, float maxHealth) {
        if (currentHealth >= maxHealth) {
            if (lastHealth < maxHealth) {
                fullHealthStartTime = System.currentTimeMillis();
            }
        } else {
            fullHealthStartTime = 0;
        }
        lastHealth = currentHealth;

        long timeSinceFullHealth = fullHealthStartTime > 0 ? System.currentTimeMillis() - fullHealthStartTime : 0;
        return currentHealth < maxHealth || (fullHealthStartTime > 0 && timeSinceFullHealth < TEXT_DISPLAY_DURATION);
    }
}