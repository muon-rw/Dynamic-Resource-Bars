package dev.muon.dynamic_resource_bars.util;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

public class RenderUtil {
    public static final long BAR_FADEOUT_DURATION = 1500L;
    public static final long TEXT_FADEOUT_DURATION = 500L;
    public static final int BASE_TEXT_ALPHA = 200;

    /** Total time a bar's "smart fade" trigger keeps text drawable: hold + fadeout. */
    public static long textDisplayDuration() {
        return ModConfigManager.getClient().fadeHoldDuration + TEXT_FADEOUT_DURATION;
    }

    /** Vanilla font line height; used to derive font scale from a per-bar text height. */
    public static final int FONT_LINE_HEIGHT = 9;

    /**
     * Renders the "current / max" text at the given base position scaled to fit {@code textHeight}
     * pixels of vertical space. Final scale = {@code (textHeight / 9) × globalTextSize}.
     */
    public static void renderText(float current, float max, GuiGraphicsExtractor graphics,
                                  int baseX, int baseY, int textHeight, int color, HorizontalAlignment alignment) {
        Minecraft minecraft = Minecraft.getInstance();
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        float finalScale = computeScale(textHeight);

        pose.scale(finalScale, finalScale);
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

        graphics.text(minecraft.font, fullTextComponent, actualX, scaledY, color, true);

        pose.popMatrix();
    }

    public static void renderAdditionText(String text, GuiGraphicsExtractor graphics,
                                          int baseX, int baseY, int textHeight, int color,
                                          HorizontalAlignment alignment) {
        Minecraft minecraft = Minecraft.getInstance();
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        float finalScale = computeScale(textHeight);

        int scaledX = (int) (baseX / finalScale);
        int yPos = (int) (baseY / finalScale) - (minecraft.font.lineHeight / 2);
        pose.scale(finalScale, finalScale);

        int textWidth = minecraft.font.width(text);
        int actualX = switch (alignment) {
            case CENTER -> scaledX - (textWidth / 2);
            case RIGHT -> scaledX - textWidth;
            case LEFT -> scaledX;
        };

        graphics.text(minecraft.font, text, actualX, yPos, color, true);

        pose.popMatrix();
    }

    private static float computeScale(int textHeight) {
        float scale = (textHeight / (float) FONT_LINE_HEIGHT) * ModConfigManager.getClient().globalTextSize;
        // Clamp to a sane minimum so degenerate textHeight never produces invisible text.
        return Math.max(0.05f, scale);
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

    // TODO(26.1.2): bindForSetup was removed; if GeckoLib/AzureLib breaks textures again,
    //               re-introduce a binding hook against the new TextureManager API.
    public static void blitWithBinding(GuiGraphicsExtractor graphics, Identifier texture,
                                       int x, int y, int uOffset, int vOffset,
                                       int width, int height, int textureWidth, int textureHeight) {
        blitWithBinding(graphics, texture, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight, 0xFFFFFFFF);
    }

    /** Tinted blit. {@code color} is ARGB; pass {@code 0xFFFFFFFF} for untinted white. */
    public static void blitWithBinding(GuiGraphicsExtractor graphics, Identifier texture,
                                       int x, int y, int uOffset, int vOffset,
                                       int width, int height, int textureWidth, int textureHeight,
                                       int color) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, uOffset, vOffset,
                width, height, textureWidth, textureHeight, color);
    }

    /** Packs a 0..1 alpha into an ARGB int that tints white — for fade-in/out animations. */
    public static int whiteWithAlpha(float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        return (a << 24) | 0xFFFFFF;
    }
}
