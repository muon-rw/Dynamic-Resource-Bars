package dev.muon.dynamic_resource_bars.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.ManaProvider;
import dev.muon.dynamic_resource_bars.util.Position;
import dev.muon.dynamic_resource_bars.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif

public class ManaBarRenderer {
    private static float lastMana = -1;
    private static long fullManaStartTime = 0;
    private static long barEnabledStartTime = 0L;
    private static long barDisabledStartTime = 0L;
    private static boolean barSetVisible = false;

    private static final int RESERVED_MANA_COLOR = 0x232323;

    public static void render(GuiGraphics graphics, #if NEWER_THAN_20_1 DeltaTracker deltaTracker #else float partialTicks #endif, ManaProvider manaProvider, Player player) {

        float alpha = getCurrentAlpha();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        Position manaPos = HUDPositioning.getAboveUtilitiesAnchor()
                .offset(AllConfigs.client().manaBarXOffset.get(), AllConfigs.client().manaBarYOffset.get());

        // Configs from constants
        int borderWidth = AllConfigs.client().manaBorderWidth.get();
        int borderHeight = AllConfigs.client().manaBorderHeight.get();
        int barWidth = AllConfigs.client().manaBarWidth.get();
        int barHeight = AllConfigs.client().manaBarHeight.get();
        int barXOffset = AllConfigs.client().manaBarXOffset.get();
        int barYOffset = AllConfigs.client().manaBarYOffset.get();
        int animationCycles = AllConfigs.client().manaBarAnimationCycles.get(); // Total frames in animation
        int frameHeight = AllConfigs.client().manaBarFrameHeight.get();      // Height of each frame in texture

        int xPos = manaPos.x() - (borderWidth / 2);
        int yPos = manaPos.y();

        int animOffset = (int) (((player.tickCount + #if NEWER_THAN_20_1 deltaTracker.getGameTimeDeltaTicks() #else partialTicks #endif) / 3) % animationCycles) * frameHeight;

        renderMainBar(graphics, manaProvider, animOffset, xPos, yPos,
                borderWidth, borderHeight, barWidth, barHeight,
                barXOffset, barYOffset);

        renderReservedOverlay(graphics, manaProvider, animOffset,
                xPos, yPos, barWidth, barHeight,
                barXOffset, barYOffset);

        if (AllConfigs.client().manaDetailOverlay.get()) {
            graphics.blit(DynamicResourceBars.loc("textures/gui/detail_overlay.png"),
                    xPos + AllConfigs.client().manaOverlayXOffset.get(),
                    yPos + AllConfigs.client().manaOverlayYOffset.get(),
                    0, 0, borderWidth, borderHeight, 256, 256);
        }


        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int textX = (xPos + (borderWidth / 2));
        int textY = (yPos + barYOffset);
        if (shouldRenderText(manaProvider.getCurrentMana(), manaProvider.getMaxMana())) {
            int color = getManaTextColor();
            RenderUtil.renderText((float) manaProvider.getCurrentMana(), manaProvider.getMaxMana(),
                    graphics, textX, textY, color);
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

    private static void renderMainBar(GuiGraphics graphics, ManaProvider manaProvider,
                                      int animOffset, int xPos, int yPos,
                                      int borderWidth, int borderHeight, int barWidth, int barHeight,
                                      int barXOffset, int barYOffset) {
        // Render border
        graphics.blit(DynamicResourceBars.loc("textures/gui/mana_border.png"),
                xPos, yPos, 0, 0, borderWidth, borderHeight, 256, 256);

        // Render mana bar
        float maxMana = manaProvider.getMaxMana() * (1.0f + manaProvider.getReservedMana());
        double currentMana = manaProvider.getCurrentMana();
        int partialBarWidth = (int) (barWidth * (currentMana / maxMana));

        graphics.blit(DynamicResourceBars.loc("textures/gui/mana_bar.png"),
                xPos + barXOffset, yPos + barYOffset,
                0, animOffset, partialBarWidth, barHeight, 256, 256);
    }

    private static void renderReservedOverlay(GuiGraphics graphics, ManaProvider manaProvider,
                                              int animOffset, int xPos, int yPos,
                                              int barWidth, int barHeight, int barXOffset, int barYOffset) {
        float reservedMana = manaProvider.getReservedMana();
        if (reservedMana <= 0) return;

        int reserveManaLength = (int) (barWidth * reservedMana);
        int offset = barWidth - reserveManaLength;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(((RESERVED_MANA_COLOR >> 16) & 0xFF) / 255f,
                ((RESERVED_MANA_COLOR >> 8) & 0xFF) / 255f,
                (RESERVED_MANA_COLOR & 0xFF) / 255f,
                1.0f);

        graphics.blit(
                DynamicResourceBars.loc("textures/gui/mana_bar.png"), xPos + barXOffset + offset, yPos + barYOffset,
                0, animOffset, reserveManaLength, barHeight, 256, 256
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

    private static int getManaTextColor() {
        long timeSinceFullMana = fullManaStartTime > 0 ?
                System.currentTimeMillis() - fullManaStartTime : 0;

        int alpha = RenderUtil.calculateTextAlpha(timeSinceFullMana);

        if (!barSetVisible) {
            float barAlpha = getCurrentAlpha();
            alpha = (int)(alpha * barAlpha);
        }
        alpha = Math.max(10, alpha);

        return (alpha << 24) | 0xFFFFFF;
    }



    private static boolean shouldRenderText(double currentMana, float maxMana) {
        if (currentMana >= maxMana) {
            if (lastMana < maxMana) {
                fullManaStartTime = System.currentTimeMillis();
            }
        } else {
            fullManaStartTime = 0;
        }
        lastMana = (float) currentMana;

        long timeSinceFullMana = fullManaStartTime > 0 ?
                System.currentTimeMillis() - fullManaStartTime : 0;

        // Stop rendering before alpha would drop below 10
        return (currentMana < maxMana ||
                (fullManaStartTime > 0 && timeSinceFullMana < RenderUtil.TEXT_DISPLAY_DURATION))
                && getCurrentAlpha() > 0.05f;
    }
}