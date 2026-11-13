package dev.muon.dynamic_resource_bars.util;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class RenderUtil {
    public static final long BAR_FADEOUT_DURATION = 1500L;
    public static final long TEXT_FADEOUT_DURATION = 500L;
    public static final int BASE_TEXT_ALPHA = 200;

    /** Total time a bar's "smart fade" trigger keeps text drawable: hold + fadeout. */
    public static long textDisplayDuration() {
        return ModConfigManager.getClient().fadeHoldDuration + TEXT_FADEOUT_DURATION;
    }

    /**
     * Renders the "current / max" text vertically centered on {@code baseY}.
     * Final scale = {@code textScale × globalTextSize}.
     */
    public static void renderText(float current, float max, GuiGraphics graphics,
                                  int baseX, int baseY, float textScale, int color, HorizontalAlignment alignment) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        float finalScale = computeScale(textScale);

        pose.scale(finalScale, finalScale, 1.0f);
        int scaledX = (int) (baseX / finalScale);
        int scaledY = (int) (baseY / finalScale) - (minecraft.font.lineHeight / 2);

        String currentText = String.valueOf((int) current);
        String maxText = String.valueOf((int) max);
        Component fullTextComponent = Component.literal(currentText + " / " + maxText);
        int totalTextWidth = minecraft.font.width(fullTextComponent.getString());

        int actualX = switch (alignment) {
            case CENTER -> scaledX - (totalTextWidth / 2);
            case RIGHT -> scaledX - totalTextWidth;
            case LEFT -> scaledX;
        };

        graphics.drawString(minecraft.font, fullTextComponent, actualX, scaledY, color, true);

        pose.popPose();
    }

    public static void renderAdditionText(String text, GuiGraphics graphics,
                                          int baseX, int baseY, float textScale, int color,
                                          HorizontalAlignment alignment) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        float finalScale = computeScale(textScale);

        int scaledX = (int) (baseX / finalScale);
        int yPos = (int) (baseY / finalScale) - (minecraft.font.lineHeight / 2);
        pose.scale(finalScale, finalScale, 1.0f);

        int textWidth = minecraft.font.width(text);
        int actualX = switch (alignment) {
            case CENTER -> scaledX - (textWidth / 2);
            case RIGHT -> scaledX - textWidth;
            case LEFT -> scaledX;
        };

        graphics.drawString(minecraft.font, text, actualX, yPos, color, true);

        pose.popPose();
    }

    private static float computeScale(float textScale) {
        return Math.max(0.05f, textScale * ModConfigManager.getClient().globalTextSize);
    }

    public static int calculateTextAlpha(long timeSinceEvent) {
        long hold = ModConfigManager.getClient().fadeHoldDuration;
        long total = hold + TEXT_FADEOUT_DURATION;
        int alpha;
        if (timeSinceEvent < total) {
            alpha = timeSinceEvent > hold
                    ? (int) (BASE_TEXT_ALPHA * (total - timeSinceEvent) / TEXT_FADEOUT_DURATION)
                    : BASE_TEXT_ALPHA;
        } else {
            alpha = 0;
        }
        return Math.max(10, Math.min(alpha, BASE_TEXT_ALPHA));
    }

    /** Packs a 0..1 alpha into an ARGB int that tints white; used for fade-in/out animations. */
    public static int whiteWithAlpha(float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        return (a << 24) | 0xFFFFFF;
    }
}
