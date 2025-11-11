package dev.muon.dynamic_resource_bars.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Utility for rendering textures with nine-slice scaling.
 * Supports both stretch and tile modes for edges and center.
 * 
 * Nine-slice regions:
 * ┌──┬─────┬──┐
 * │TL│ Top │TR│  TL/TR/BL/BR = corners (fixed size)
 * ├──┼─────┼──┤
 * │L │  C  │R │  L/R = left/right edges (scale/tile vertically)
 * ├──┼─────┼──┤  T/B = top/bottom edges (scale/tile horizontally)
 * │BL│ Bot │BR│  C = center (scale/tile both)
 * └──┴─────┴──┘
 */
public class NineSliceRenderer {
    
    /**
     * Render a texture with scaling based on ScalingInfo.
     * This is the main entry point that delegates to appropriate rendering method.
     */
    public static void renderWithScaling(GuiGraphics graphics, ResourceLocation texture,
                                        AnimationMetadata.ScalingInfo scalingInfo,
                                        int x, int y, int width, int height,
                                        int textureWidth, int textureHeight) {
        
        if (scalingInfo.mode == AnimationMetadata.ScalingMode.NONE) {
            // No scaling - 1:1 UV sampling (sample exactly what fits)
            if (scalingInfo.sourceRegion != null) {
                // Sample from source region
                AnimationMetadata.SourceRegion src = scalingInfo.sourceRegion;
                graphics.blit(texture, x, y, src.u, src.v, width, height, textureWidth, textureHeight);
            } else {
                // Sample from texture origin
                graphics.blit(texture, x, y, 0, 0, width, height, textureWidth, textureHeight);
            }
            
        } else if (scalingInfo.mode == AnimationMetadata.ScalingMode.STRETCH) {
            // Simple stretch - use stretching blit signature
            if (scalingInfo.sourceRegion != null) {
                // Stretch from source region
                AnimationMetadata.SourceRegion src = scalingInfo.sourceRegion;
                graphics.blit(texture, x, y, width, height, (float)src.u, (float)src.v, src.width, src.height, textureWidth, textureHeight);
            } else {
                // Stretch from full texture
                graphics.blit(texture, x, y, width, height, 0f, 0f, textureWidth, textureHeight, textureWidth, textureHeight);
            }
            
        } else if (scalingInfo.mode == AnimationMetadata.ScalingMode.TILE) {
            // Simple tile - repeat texture
            if (scalingInfo.sourceRegion != null) {
                // Tile from source region
                AnimationMetadata.SourceRegion src = scalingInfo.sourceRegion;
                renderTiled(graphics, texture, x, y, width, height, src.u, src.v, src.width, src.height, textureWidth, textureHeight);
            } else {
                // Tile from full texture
                renderTiled(graphics, texture, x, y, width, height, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
            }
            
        } else if (scalingInfo.isNineSlice()) {
            // Nine-slice with configurable modes
            renderNineSlice(graphics, texture, scalingInfo.nineSlice, 
                          x, y, width, height, textureWidth, textureHeight);
        } else {
            // Fallback
            graphics.blit(texture, x, y, 0, 0, width, height, textureWidth, textureHeight);
        }
    }
    
    /**
     * Render texture with simple tiling (repeat pattern)
     */
    private static void renderTiled(GuiGraphics graphics, ResourceLocation texture,
                                   int x, int y, int width, int height,
                                   int u, int v, int texWidth, int texHeight,
                                   int textureSheetWidth, int textureSheetHeight) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Calculate how many full tiles fit and remainder
        int tilesX = width / texWidth;
        int remainderX = width % texWidth;
        int tilesY = height / texHeight;
        int remainderY = height % texHeight;
        
        // Render full tiles
        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                graphics.blit(texture,
                    x + tx * texWidth,
                    y + ty * texHeight,
                    u, v,
                    texWidth, texHeight,
                    textureSheetWidth, textureSheetHeight
                );
            }
            
            // Render partial tile at right edge
            if (remainderX > 0) {
                graphics.blit(texture,
                    x + tilesX * texWidth,
                    y + ty * texHeight,
                    u, v,
                    remainderX, texHeight,
                    textureSheetWidth, textureSheetHeight
                );
            }
        }
        
        // Render bottom row (partial height)
        if (remainderY > 0) {
            for (int tx = 0; tx < tilesX; tx++) {
                graphics.blit(texture,
                    x + tx * texWidth,
                    y + tilesY * texHeight,
                    u, v,
                    texWidth, remainderY,
                    textureSheetWidth, textureSheetHeight
                );
            }
            
            // Bottom-right corner partial tile
            if (remainderX > 0) {
                graphics.blit(texture,
                    x + tilesX * texWidth,
                    y + tilesY * texHeight,
                    u, v,
                    remainderX, remainderY,
                    textureSheetWidth, textureSheetHeight
                );
            }
        }
        
        RenderSystem.disableBlend();
    }
    
    /**
     * Render texture with nine-slice scaling.
     * Supports both tile and stretch modes for edges and center.
     * Supports source region for textures that don't fill entire sheet.
     */
    private static void renderNineSlice(GuiGraphics graphics, ResourceLocation texture,
                                       AnimationMetadata.NineSliceData nineSlice,
                                       int x, int y, int width, int height,
                                       int textureWidth, int textureHeight) {
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Get source region (defaults to full texture if not specified)
        AnimationMetadata.SourceRegion source = nineSlice.sourceRegion;
        if (source == null) {
            source = AnimationMetadata.SourceRegion.fullTexture(textureWidth, textureHeight);
        }
        
        int l = nineSlice.left;
        int r = nineSlice.right;
        int t = nineSlice.top;
        int b = nineSlice.bottom;
        
        // Calculate region dimensions
        int centerWidth = width - l - r;
        int centerHeight = height - t - b;
        int texCenterWidth = source.width - l - r;
        int texCenterHeight = source.height - t - b;
        
        // Validate dimensions
        if (centerWidth < 0 || centerHeight < 0) {
            // Borders too large for render area, just stretch whole texture from source region
            graphics.blit(texture, x, y, source.u, source.v, width, height, textureWidth, textureHeight);
            RenderSystem.disableBlend();
            return;
        }
        
        if (texCenterWidth <= 0 || texCenterHeight <= 0) {
            // Borders too large for source region, can't nine-slice
            dev.muon.dynamic_resource_bars.DynamicResourceBars.LOGGER.warn(
                "Nine-slice borders too large for source region! Texture: {}, Source: {}x{}, Borders: ({},{},{},{}), Center would be: {}x{}",
                texture, source.width, source.height, l, r, t, b, texCenterWidth, texCenterHeight
            );
            // Just stretch the entire source region
            graphics.blit(texture, x, y, source.u, source.v, width, height, textureWidth, textureHeight);
            RenderSystem.disableBlend();
            return;
        }
        
        // === Render 9 regions ===
        // All UV coordinates are offset by source region
        
        // 1. Top-left corner (always fixed)
        if (l > 0 && t > 0) {
            graphics.blit(texture, x, y, source.u, source.v, l, t, textureWidth, textureHeight);
        }
        
        // 2. Top edge
        if (t > 0 && centerWidth > 0) {
            renderRegion(graphics, texture, nineSlice.edgeMode,
                x + l, y, centerWidth, t,
                source.u + l, source.v, texCenterWidth, t,
                textureWidth, textureHeight);
        }
        
        // 3. Top-right corner (always fixed)
        if (r > 0 && t > 0) {
            graphics.blit(texture, x + width - r, y, 
                source.u + source.width - r, source.v, r, t, textureWidth, textureHeight);
        }
        
        // 4. Left edge
        if (l > 0 && centerHeight > 0) {
            renderRegion(graphics, texture, nineSlice.edgeMode,
                x, y + t, l, centerHeight,
                source.u, source.v + t, l, texCenterHeight,
                textureWidth, textureHeight);
        }
        
        // 5. Center
        if (centerWidth > 0 && centerHeight > 0) {
            renderRegion(graphics, texture, nineSlice.centerMode,
                x + l, y + t, centerWidth, centerHeight,
                source.u + l, source.v + t, texCenterWidth, texCenterHeight,
                textureWidth, textureHeight);
        }
        
        // 6. Right edge
        if (r > 0 && centerHeight > 0) {
            renderRegion(graphics, texture, nineSlice.edgeMode,
                x + width - r, y + t, r, centerHeight,
                source.u + source.width - r, source.v + t, r, texCenterHeight,
                textureWidth, textureHeight);
        }
        
        // 7. Bottom-left corner (always fixed)
        if (l > 0 && b > 0) {
            graphics.blit(texture, x, y + height - b, 
                source.u, source.v + source.height - b, l, b, textureWidth, textureHeight);
        }
        
        // 8. Bottom edge
        if (b > 0 && centerWidth > 0) {
            renderRegion(graphics, texture, nineSlice.edgeMode,
                x + l, y + height - b, centerWidth, b,
                source.u + l, source.v + source.height - b, texCenterWidth, b,
                textureWidth, textureHeight);
        }
        
        // 9. Bottom-right corner (always fixed)
        if (r > 0 && b > 0) {
            graphics.blit(texture, x + width - r, y + height - b,
                source.u + source.width - r, source.v + source.height - b, r, b, textureWidth, textureHeight);
        }
        
        RenderSystem.disableBlend();
    }
    
    /**
     * Render a region with either stretch or tile mode
     */
    private static void renderRegion(GuiGraphics graphics, ResourceLocation texture,
                                    AnimationMetadata.ScalingMode mode,
                                    int x, int y, int width, int height,
                                    int u, int v, int texWidth, int texHeight,
                                    int textureSheetWidth, int textureSheetHeight) {
        
        if (mode == AnimationMetadata.ScalingMode.TILE) {
            // Tile the region
            int tilesX = width / texWidth;
            int remainderX = width % texWidth;
            int tilesY = height / texHeight;
            int remainderY = height % texHeight;
            
            // Render full tiles
            for (int ty = 0; ty < tilesY; ty++) {
                for (int tx = 0; tx < tilesX; tx++) {
                    graphics.blit(texture,
                        x + tx * texWidth,
                        y + ty * texHeight,
                        u, v,
                        texWidth, texHeight,
                        textureSheetWidth, textureSheetHeight
                    );
                }
                
                // Partial tile at edge
                if (remainderX > 0) {
                    graphics.blit(texture,
                        x + tilesX * texWidth,
                        y + ty * texHeight,
                        u, v,
                        remainderX, texHeight,
                        textureSheetWidth, textureSheetHeight
                    );
                }
            }
            
            // Bottom row partial
            if (remainderY > 0) {
                for (int tx = 0; tx < tilesX; tx++) {
                    graphics.blit(texture,
                        x + tx * texWidth,
                        y + tilesY * texHeight,
                        u, v,
                        texWidth, remainderY,
                        textureSheetWidth, textureSheetHeight
                    );
                }
                
                // Bottom-right corner partial
                if (remainderX > 0) {
                    graphics.blit(texture,
                        x + tilesX * texWidth,
                        y + tilesY * texHeight,
                        u, v,
                        remainderX, remainderY,
                        textureSheetWidth, textureSheetHeight
                    );
                }
            }
        } else {
            // Stretch the region
            // This signature: blit(texture, x, y, width, height, uOffset, vOffset, uWidth, vHeight, textureSheetWidth, textureSheetHeight)
            // Maps texture region (u,v,texWidth,texHeight) to screen area (x,y,width,height)
            graphics.blit(texture, x, y, width, height, (float)u, (float)v, texWidth, texHeight, textureSheetWidth, textureSheetHeight);
        }
    }
}

