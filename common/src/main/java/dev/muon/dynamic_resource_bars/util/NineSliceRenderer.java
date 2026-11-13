package dev.muon.dynamic_resource_bars.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Utility for rendering textures with nine-slice scaling.
 * Supports both stretch and tile modes for edges and center.
 *
 * <pre>
 * Nine-slice regions:
 * ┌──┬─────┬──┐
 * │TL│ Top │TR│  TL/TR/BL/BR = corners (fixed size)
 * ├──┼─────┼──┤
 * │L │  C  │R │  L/R = left/right edges (scale/tile vertically)
 * ├──┼─────┼──┤  T/B = top/bottom edges (scale/tile horizontally)
 * │BL│ Bot │BR│  C = center (scale/tile both)
 * └──┴─────┴──┘
 * </pre>
 *
 * <p>All entry points come in two flavours: untinted (defaults to opaque white) and tinted
 * (caller supplies an ARGB color). The tint applies uniformly to every emitted blit so
 * fade-out alpha works.
 */
public class NineSliceRenderer {

    private static final int OPAQUE_WHITE = 0xFFFFFFFF;

    public static void renderWithScaling(GuiGraphics graphics, ResourceLocation texture,
                                         AnimationMetadata.ScalingInfo scalingInfo,
                                         int x, int y, int width, int height,
                                         int textureWidth, int textureHeight) {
        renderWithScaling(graphics, texture, scalingInfo, x, y, width, height,
                textureWidth, textureHeight, OPAQUE_WHITE);
    }

    public static void renderWithScaling(GuiGraphics graphics, ResourceLocation texture,
                                         AnimationMetadata.ScalingInfo scalingInfo,
                                         int x, int y, int width, int height,
                                         int textureWidth, int textureHeight, int color) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(r, g, b, a);
        try {
            if (scalingInfo.mode == AnimationMetadata.ScalingMode.NONE) {
                if (scalingInfo.sourceRegion != null) {
                    AnimationMetadata.SourceRegion src = scalingInfo.sourceRegion;
                    graphics.blit(texture, x, y, src.u, src.v, width, height, textureWidth, textureHeight);
                } else {
                    graphics.blit(texture, x, y, 0, 0, width, height, textureWidth, textureHeight);
                }
            } else if (scalingInfo.mode == AnimationMetadata.ScalingMode.STRETCH) {
                if (scalingInfo.sourceRegion != null) {
                    AnimationMetadata.SourceRegion src = scalingInfo.sourceRegion;
                    graphics.blit(texture, x, y, width, height, (float) src.u, (float) src.v, src.width, src.height, textureWidth, textureHeight);
                } else {
                    graphics.blit(texture, x, y, width, height, 0f, 0f, textureWidth, textureHeight, textureWidth, textureHeight);
                }
            } else if (scalingInfo.mode == AnimationMetadata.ScalingMode.TILE) {
                if (scalingInfo.sourceRegion != null) {
                    AnimationMetadata.SourceRegion src = scalingInfo.sourceRegion;
                    renderTiled(graphics, texture, x, y, width, height, src.u, src.v, src.width, src.height, textureWidth, textureHeight);
                } else {
                    renderTiled(graphics, texture, x, y, width, height, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
                }
            } else if (scalingInfo.isNineSlice()) {
                renderNineSlice(graphics, texture, scalingInfo.nineSlice, x, y, width, height, textureWidth, textureHeight);
            } else {
                graphics.blit(texture, x, y, 0, 0, width, height, textureWidth, textureHeight);
            }
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }

    private static void renderTiled(GuiGraphics graphics, ResourceLocation texture,
                                    int x, int y, int width, int height,
                                    int u, int v, int texWidth, int texHeight,
                                    int textureSheetWidth, int textureSheetHeight) {
        int tilesX = width / texWidth;
        int remainderX = width % texWidth;
        int tilesY = height / texHeight;
        int remainderY = height % texHeight;

        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                graphics.blit(texture,
                        x + tx * texWidth, y + ty * texHeight,
                        u, v, texWidth, texHeight,
                        textureSheetWidth, textureSheetHeight);
            }
            if (remainderX > 0) {
                graphics.blit(texture,
                        x + tilesX * texWidth, y + ty * texHeight,
                        u, v, remainderX, texHeight,
                        textureSheetWidth, textureSheetHeight);
            }
        }
        if (remainderY > 0) {
            for (int tx = 0; tx < tilesX; tx++) {
                graphics.blit(texture,
                        x + tx * texWidth, y + tilesY * texHeight,
                        u, v, texWidth, remainderY,
                        textureSheetWidth, textureSheetHeight);
            }
            if (remainderX > 0) {
                graphics.blit(texture,
                        x + tilesX * texWidth, y + tilesY * texHeight,
                        u, v, remainderX, remainderY,
                        textureSheetWidth, textureSheetHeight);
            }
        }
    }

    private static void renderNineSlice(GuiGraphics graphics, ResourceLocation texture,
                                        AnimationMetadata.NineSliceData nineSlice,
                                        int x, int y, int width, int height,
                                        int textureWidth, int textureHeight) {
        AnimationMetadata.SourceRegion source = nineSlice.sourceRegion;
        if (source == null) {
            source = AnimationMetadata.SourceRegion.fullTexture(textureWidth, textureHeight);
        }

        int l = nineSlice.left;
        int r = nineSlice.right;
        int t = nineSlice.top;
        int b = nineSlice.bottom;

        int centerWidth = width - l - r;
        int centerHeight = height - t - b;
        int texCenterWidth = source.width - l - r;
        int texCenterHeight = source.height - t - b;

        if (centerWidth < 0 || centerHeight < 0) {
            graphics.blit(texture, x, y, source.u, source.v, width, height, textureWidth, textureHeight);
            return;
        }
        if (texCenterWidth <= 0 || texCenterHeight <= 0) {
            dev.muon.dynamic_resource_bars.Constants.LOG.warn(
                    "Nine-slice borders too large for source region! Texture: {}, Source: {}x{}, Borders: ({},{},{},{}), Center would be: {}x{}",
                    texture, source.width, source.height, l, r, t, b, texCenterWidth, texCenterHeight);
            graphics.blit(texture, x, y, source.u, source.v, width, height, textureWidth, textureHeight);
            return;
        }

        if (l > 0 && t > 0) {
            graphics.blit(texture, x, y, source.u, source.v, l, t, textureWidth, textureHeight);
        }
        if (t > 0 && centerWidth > 0) {
            renderRegion(graphics, texture, nineSlice.edgeMode,
                    x + l, y, centerWidth, t,
                    source.u + l, source.v, texCenterWidth, t,
                    textureWidth, textureHeight);
        }
        if (r > 0 && t > 0) {
            graphics.blit(texture, x + width - r, y,
                    source.u + source.width - r, source.v, r, t, textureWidth, textureHeight);
        }
        if (l > 0 && centerHeight > 0) {
            renderRegion(graphics, texture, nineSlice.edgeMode,
                    x, y + t, l, centerHeight,
                    source.u, source.v + t, l, texCenterHeight,
                    textureWidth, textureHeight);
        }
        if (centerWidth > 0 && centerHeight > 0) {
            renderRegion(graphics, texture, nineSlice.centerMode,
                    x + l, y + t, centerWidth, centerHeight,
                    source.u + l, source.v + t, texCenterWidth, texCenterHeight,
                    textureWidth, textureHeight);
        }
        if (r > 0 && centerHeight > 0) {
            renderRegion(graphics, texture, nineSlice.edgeMode,
                    x + width - r, y + t, r, centerHeight,
                    source.u + source.width - r, source.v + t, r, texCenterHeight,
                    textureWidth, textureHeight);
        }
        if (l > 0 && b > 0) {
            graphics.blit(texture, x, y + height - b,
                    source.u, source.v + source.height - b, l, b, textureWidth, textureHeight);
        }
        if (b > 0 && centerWidth > 0) {
            renderRegion(graphics, texture, nineSlice.edgeMode,
                    x + l, y + height - b, centerWidth, b,
                    source.u + l, source.v + source.height - b, texCenterWidth, b,
                    textureWidth, textureHeight);
        }
        if (r > 0 && b > 0) {
            graphics.blit(texture, x + width - r, y + height - b,
                    source.u + source.width - r, source.v + source.height - b, r, b, textureWidth, textureHeight);
        }
    }

    private static void renderRegion(GuiGraphics graphics, ResourceLocation texture,
                                     AnimationMetadata.ScalingMode mode,
                                     int x, int y, int width, int height,
                                     int u, int v, int texWidth, int texHeight,
                                     int textureSheetWidth, int textureSheetHeight) {
        if (mode == AnimationMetadata.ScalingMode.TILE) {
            renderTiled(graphics, texture, x, y, width, height, u, v, texWidth, texHeight,
                    textureSheetWidth, textureSheetHeight);
        } else {
            graphics.blit(texture, x, y, width, height, (float) u, (float) v, texWidth, texHeight, textureSheetWidth, textureSheetHeight);
        }
    }
}
