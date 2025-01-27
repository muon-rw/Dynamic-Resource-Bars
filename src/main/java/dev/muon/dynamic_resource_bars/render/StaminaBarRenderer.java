package dev.muon.dynamic_resource_bars.render;


import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.Position;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif

public class StaminaBarRenderer {
    private static final float SPRINT_THRESHOLD = 6.0f;
    private static final float CRITICAL_THRESHOLD = 2.0f;

    private enum BarType {
        NORMAL("stamina_bar"),
        HUNGER("stamina_bar_hunger"),
        WARNING("stamina_bar_warning"),
        CRITICAL("stamina_bar_critical");

        private final String texture;

        BarType(String texture) {
            this.texture = texture;
        }

        public String getTexture() {
            return texture;
        }

        public static BarType fromPlayerState(Player player, float stamina) {
            if (player.hasEffect(MobEffects.HUNGER)) return HUNGER;
            if (stamina <= CRITICAL_THRESHOLD) return CRITICAL;
            if (stamina <= SPRINT_THRESHOLD) return WARNING;
            return NORMAL;
        }
    }

    public static void render(GuiGraphics graphics, Player player, #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif) {
        Position staminaPos = HUDPositioning.getHungerAnchor()
                .offset(AllConfigs.client().manaBarXOffset.get(), AllConfigs.client().manaBarYOffset.get());

        int borderWidth = AllConfigs.client().staminaBorderWidth.get();
        int borderHeight = AllConfigs.client().staminaBorderHeight.get();
        int barWidth = AllConfigs.client().staminaBarWidth.get();
        int barHeight = AllConfigs.client().staminaBarHeight.get();
        int barXOffset = AllConfigs.client().staminaBarXOffset.get();
        int barYOffset = AllConfigs.client().staminaBarYOffset.get();
        int animationCycles = AllConfigs.client().staminaBarAnimationCycles.get(); // Total frames in animation
        int frameHeight = AllConfigs.client().staminaBarFrameHeight.get();      // Height of each frame in texture

        int xPos = staminaPos.x() - borderWidth;
        int yPos = staminaPos.y();

        graphics.blit(
                DynamicResourceBars.loc("textures/gui/stamina_border.png"),
                xPos, yPos, 0, 0, borderWidth, borderHeight, 256, 256
        );

        float maxStamina = 20f;
        float currentStamina = player.getFoodData().getFoodLevel();

        int animOffset = (int) (((player.tickCount + #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif) / 3) % animationCycles) * frameHeight;

        renderBaseBar(graphics, player, currentStamina, maxStamina, xPos, yPos,
                barWidth, barHeight, barXOffset, barYOffset,
                animOffset);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (AllConfigs.client().staminaDetailOverlay.get()) {
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/detail_overlay.png"),
                    xPos + AllConfigs.client().staminaOverlayXOffset.get(),
                    yPos + AllConfigs.client().staminaOverlayYOffset.get(),
                    0, 0, borderWidth, borderHeight,
                    256, 256
            );
        }
    }

    private static void renderBaseBar(GuiGraphics graphics, Player player, float currentStamina, float maxStamina,
                                      int xPos, int yPos, int barWidth, int barHeight,
                                      int barXOffset, int barYOffset, int animOffset) {
        BarType barType = BarType.fromPlayerState(player, currentStamina);
        int partialBarWidth = (int) (barWidth * (currentStamina / maxStamina));

        graphics.blit(
                DynamicResourceBars.loc("textures/gui/" + barType.getTexture() + ".png"),
                xPos + barXOffset, yPos + barYOffset,
                0, animOffset, partialBarWidth, barHeight,
                256, 256
        );
    }
}