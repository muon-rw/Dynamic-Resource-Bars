package dev.muon.dynamic_resource_bars.render;


import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif

public class StaminaBarRenderer {
    private static final float CRITICAL_THRESHOLD = 6.0f;
    // TODO: Appleskin compat
    // TODO: Pulse when hunger drains a tick, similar to vanilla shake

    private enum BarType {
        NORMAL("stamina_bar"),
        HUNGER("stamina_bar_hunger"),
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
            return NORMAL;
        }
    }

    public static void render(GuiGraphics graphics, Player player, #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif) {
        if (!Minecraft.getInstance().gameMode.canHurtPlayer()) {
            return;
        }

        Position staminaPos = HUDPositioning.getPositionFromAnchor(AllConfigs.client().staminaBarAnchor.get());
        boolean isRightAnchored = AllConfigs.client().staminaBarAnchor.get().getSide() == HUDPositioning.AnchorSide.RIGHT;
        if (isRightAnchored) {
            staminaPos = staminaPos.offset(-AllConfigs.client().staminaBackgroundWidth.get(), 0);
        }
        staminaPos = staminaPos.offset(AllConfigs.client().staminaTotalXOffset.get(), AllConfigs.client().staminaTotalYOffset.get());

        int backgroundWidth = AllConfigs.client().staminaBackgroundWidth.get();
        int backgroundHeight = AllConfigs.client().staminaBackgroundHeight.get();
        int barWidth = AllConfigs.client().staminaBarWidth.get();
        int barHeight = AllConfigs.client().staminaBarHeight.get();
        int barXOffset = AllConfigs.client().staminaBarXOffset.get();
        int barYOffset = AllConfigs.client().staminaBarYOffset.get();
        int animationCycles = AllConfigs.client().staminaBarAnimationCycles.get(); // Total frames in animation
        int frameHeight = AllConfigs.client().staminaBarFrameHeight.get();      // Height of each frame in texture

        int xPos = staminaPos.x();
        int yPos = staminaPos.y();

        graphics.blit(
                DynamicResourceBars.loc("textures/gui/stamina_background.png"),
                xPos, yPos, 0, 0, backgroundWidth, backgroundHeight, 256, 256
        );

        float maxStamina = 20f;
        float currentStamina = player.getFoodData().getFoodLevel();

        int animOffset = (int) (((player.tickCount + #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif) / 3) % animationCycles) * frameHeight;


        renderBaseBar(graphics, player, currentStamina, maxStamina, xPos, yPos,
                barWidth, barHeight, barXOffset, barYOffset,
                animOffset, isRightAnchored);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (AllConfigs.client().staminaDetailOverlay.get()) {
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/detail_overlay.png"),
                    xPos + AllConfigs.client().staminaOverlayXOffset.get(),
                    yPos + AllConfigs.client().staminaOverlayYOffset.get(),
                    0, 0, backgroundWidth, backgroundHeight,
                    256, 256
            );
        }
    }

    private static void renderBaseBar(GuiGraphics graphics, Player player, float currentStamina, float maxStamina,
                                      int xPos, int yPos, int barWidth, int barHeight,
                                      int barXOffset, int barYOffset, int animOffset, boolean isRightAnchored) {
        BarType barType = BarType.fromPlayerState(player, currentStamina);
        int partialBarWidth = (int) (barWidth * (currentStamina / maxStamina));

        int barX = xPos + barXOffset;
        if (isRightAnchored) {
            barX = xPos + barWidth + barXOffset - partialBarWidth;
        }

        graphics.blit(
                DynamicResourceBars.loc("textures/gui/" + barType.getTexture() + ".png"),
                barX, yPos + barYOffset,
                0, animOffset, partialBarWidth, barHeight,
                256, 256
        );
    }
}