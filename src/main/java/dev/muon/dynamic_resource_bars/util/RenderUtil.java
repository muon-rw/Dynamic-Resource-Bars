package dev.muon.dynamic_resource_bars.util;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class RenderUtil {
    public static final long TEXT_DISPLAY_DURATION = 2000L;
    public static final long TEXT_FADEOUT_DURATION = 500L;
    public static final int BASE_TEXT_ALPHA = 200;
    // Only used for the mana bar
    public static final long BAR_FADEOUT_DURATION = 1500L;

    public static void renderText(float current, float max, GuiGraphics graphics, int baseX, int baseY, int color, HorizontalAlignment alignment) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        float scalingFactor = (float) ModConfigManager.getClient().textScalingFactor;
        float globalTextSize = ModConfigManager.getClient().globalTextSize;
        float finalScale = scalingFactor * globalTextSize;

        // Apply scaling
        poseStack.scale(finalScale, finalScale, 1.0f);
        int scaledX = (int) (baseX / finalScale);
        // Adjust Y for vertical centering. Font height is 9, so half is 4.5, round to 4 or 5.
        // Minecraft's drawString typically treats y as the top of the text.
        // To center, we need to shift it up by half the text height.
        // Font.lineHeight is usually 9.
        int scaledY = (int) (baseY / finalScale) - (minecraft.font.lineHeight / 2);


        String currentText = String.valueOf((int)current);
        String maxText = String.valueOf((int)max);
        String slashText = " / "; // Added spaces for better readability
        Component fullTextComponent = Component.literal(currentText + slashText + maxText);
        String fullText = fullTextComponent.getString();
        int totalTextWidth = minecraft.font.width(fullText);

        int actualX = scaledX;
        if (alignment == HorizontalAlignment.CENTER) {
            actualX = scaledX - (totalTextWidth / 2);
        } else if (alignment == HorizontalAlignment.RIGHT) {
            actualX = scaledX - totalTextWidth;
        }

        graphics.drawString(minecraft.font, fullTextComponent, actualX, scaledY, color, true);

        poseStack.popPose();
    }

    // Overload for existing calls that don't specify alignment (defaults to CENTER)
    public static void renderText(float current, float max, GuiGraphics graphics, int baseX, int baseY, int color) {
        renderText(current, max, graphics, baseX, baseY, color, HorizontalAlignment.CENTER);
    }

    public static void renderAdditionText(String text, GuiGraphics graphics, int baseX, int baseY, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        float scalingFactor = (float) ModConfigManager.getClient().textScalingFactor;
        float globalTextSize = ModConfigManager.getClient().globalTextSize;
        float finalScale = scalingFactor * globalTextSize;

        int xPos = (int) (baseX / finalScale);
        int yPos = (int) (baseY / finalScale) - (minecraft.font.lineHeight / 2);
        poseStack.scale(finalScale, finalScale, 1.0f);

        graphics.drawString(minecraft.font, text, xPos, yPos, color, true);

        poseStack.popPose();
    }

    public static void renderArmorText(float value, GuiGraphics graphics, int baseX, int baseY, int color, HorizontalAlignment alignment) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        float scalingFactor = (float) ModConfigManager.getClient().textScalingFactor;
        float globalTextSize = ModConfigManager.getClient().globalTextSize;
        float finalScale = scalingFactor * globalTextSize;

        int scaledX = (int) (baseX / finalScale);
        int scaledY = (int) (baseY / finalScale) - (minecraft.font.lineHeight / 2); // Added vertical centering
        poseStack.scale(finalScale, finalScale, 1.0f);

        String text;
        if (Math.abs(value - Math.floor(value)) < 0.1f) {
            text = String.valueOf((int)value);
        } else {
            text = String.format("%.1f", value);
        }
        
        int textWidth = minecraft.font.width(text);
        int actualX = scaledX;
        if (alignment == HorizontalAlignment.CENTER) {
            actualX = scaledX - (textWidth / 2);
        } else if (alignment == HorizontalAlignment.RIGHT) {
            actualX = scaledX - textWidth;
        }

        graphics.drawString(minecraft.font, text, actualX, scaledY, color, true);

        poseStack.popPose();
    }

    public static void renderArmorText(float value, GuiGraphics graphics, int baseX, int baseY, int color) {
        renderArmorText(value, graphics, baseX, baseY, color, HorizontalAlignment.CENTER);
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
        // Values too close to 0 cause rendering artifacts
        return Math.max(10, Math.min(alpha, BASE_TEXT_ALPHA));
    }

}