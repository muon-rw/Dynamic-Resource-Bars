package dev.muon.dynamic_resource_bars.util;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class RenderUtil {
    public static final long TEXT_DISPLAY_DURATION = 2000L;
    public static final long TEXT_FADEOUT_DURATION = 500L;
    public static final int BASE_TEXT_ALPHA = 200;
    // Only used for the mana bar
    public static final long BAR_FADEOUT_DURATION = 1500L;

    public static void renderText(float current, float max, GuiGraphics graphics, int baseX, int baseY, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        float scalingFactor = AllConfigs.client().textScalingFactor.getF();

        int xPos = (int) (baseX / scalingFactor);
        int yPos = (int) (baseY / scalingFactor);
        poseStack.scale(scalingFactor, scalingFactor, 1.0f);

        String currentText = String.valueOf((int)current);
        String maxText = String.valueOf((int)max);
        String slashText = "/";

        int slashWidth = minecraft.font.width(slashText);
        int currentWidth = minecraft.font.width(currentText);
        int slashX = xPos - (slashWidth / 2);

        graphics.drawString(minecraft.font, currentText, slashX - currentWidth, yPos, color, true);
        graphics.drawString(minecraft.font, slashText, slashX, yPos, color, true);
        graphics.drawString(minecraft.font, maxText, slashX + slashWidth, yPos, color, true);

        poseStack.popPose();
    }

    public static void renderAdditionText(String text, GuiGraphics graphics, int baseX, int baseY, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        float scalingFactor = AllConfigs.client().textScalingFactor.getF();

        int xPos = (int) (baseX / scalingFactor);
        int yPos = (int) (baseY / scalingFactor);
        poseStack.scale(scalingFactor, scalingFactor, 1.0f);

        graphics.drawString(minecraft.font, text, xPos, yPos, color, true);

        poseStack.popPose();
    }

    public static int calculateTextAlpha(long timeSinceEvent) {
        int alpha;
        if (timeSinceEvent < TEXT_DISPLAY_DURATION) {
            alpha = timeSinceEvent > TEXT_DISPLAY_DURATION - TEXT_FADEOUT_DURATION
                    ? (int)(BASE_TEXT_ALPHA * (TEXT_DISPLAY_DURATION - timeSinceEvent) / TEXT_FADEOUT_DURATION)
                    : BASE_TEXT_ALPHA;
        } else {
            alpha = 0;
        }
        // Alpha values too low cause rendering inconsistencies, not sure why
        return Math.max(10, Math.min(alpha, BASE_TEXT_ALPHA));
    }

}