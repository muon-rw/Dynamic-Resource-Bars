package dev.muon.dynamic_resource_bars.util;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Utility for rendering bars with mask layers applied using OpenGL stencil buffer.
 * Masks define the shape/silhouette of bars using alpha channels.
 * 
 * <p>The stencil buffer approach:
 * 1. Enable stencil testing
 * 2. Clear stencil buffer to 0
 * 3. Render mask to stencil buffer (writes 1 where mask alpha > threshold)
 * 4. Render bar texture only where stencil = 1
 * 5. Clean up stencil state
 */
public class MaskRenderUtil {
    
    /**
     * Render a bar texture with an optional mask applied using stencil buffer.
     * This is the recommended approach for cutout masks.
     * 
     * @param graphics The GUI graphics context
     * @param barTexture The bar texture to render
     * @param maskInfo The mask information (can be disabled)
     * @param x Screen X position
     * @param y Screen Y position
     * @param uOffset Texture U offset (for animation/partial fills)
     * @param vOffset Texture V offset (for animation)
     * @param width Width to render
     * @param height Height to render
     * @param textureWidth Total texture width
     * @param textureHeight Total texture height
     */
    public static void renderWithMask(GuiGraphics graphics, ResourceLocation barTexture, 
                                     AnimationMetadata.MaskInfo maskInfo,
                                     int x, int y, int uOffset, int vOffset,
                                     int width, int height, int textureWidth, int textureHeight) {
        
        if (!maskInfo.enabled || maskInfo.maskTexturePath == null) {
            // No mask, render normally
            graphics.blit(barTexture, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
            return;
        }
        
        // Parse mask texture location
        ResourceLocation maskTexture;
        try {
            maskTexture = parseMaskTexturePath(maskInfo.maskTexturePath);
        } catch (Exception e) {
            DynamicResourceBars.LOGGER.error("Invalid mask texture path: {}", maskInfo.maskTexturePath, e);
            graphics.blit(barTexture, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
            return;
        }
        
        // === STENCIL BUFFER APPROACH ===
        
        // Save current GL state
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        
        // Step 1: Enable stencil testing
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        
        // Step 2: Clear stencil buffer to 0
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, false);
        
        // Step 3: Configure stencil to write mask shape
        // - Always pass stencil test
        // - Write 1 to stencil where we render
        // - Don't write to color buffer yet (we only want to populate stencil)
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF); // Always pass, reference value = 1
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE); // Write 1 on pass
        RenderSystem.stencilMask(0xFF); // Enable writing to stencil buffer
        RenderSystem.colorMask(false, false, false, false); // Disable writing to color buffer
        
        // Render the mask (only writes to stencil buffer, not visible)
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        renderTextureQuad(graphics, maskTexture, x, y, width, height,
                         0, 0, width, height, width, height);
        
        // Step 4: Configure stencil to test (only render where stencil = 1)
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF); // Only pass where stencil = 1
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP); // Don't modify stencil
        RenderSystem.stencilMask(0x00); // Disable writing to stencil buffer
        RenderSystem.colorMask(true, true, true, true); // Re-enable writing to color buffer
        
        // Render the actual bar (only shows where mask allowed)
        graphics.blit(barTexture, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
        
        // Step 5: Clean up - disable stencil testing and restore state
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilMask(0xFF); // Reset stencil mask
        
        if (!blendWasEnabled) {
            RenderSystem.disableBlend();
        }
    }
    
    /**
     * Parse mask texture path, handling both namespaced and non-namespaced formats.
     */
    private static ResourceLocation parseMaskTexturePath(String path) {
        String[] parts = path.split(":");
        if (parts.length == 2) {
            #if NEWER_THAN_20_1
            return ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
            #else
            return new ResourceLocation(parts[0], parts[1]);
            #endif
        } else {
            // Assume same namespace (dynamic_resource_bars)
            return DynamicResourceBars.loc(path);
        }
    }
    
    /**
     * Render a simple textured quad. Used for rendering the mask to stencil buffer.
     * Uses GuiGraphics.blit() which is version-agnostic and handles buffer building internally.
     */
    private static void renderTextureQuad(GuiGraphics graphics, ResourceLocation texture,
                                         int x, int y, int width, int height,
                                         int u, int v, int uWidth, int vHeight,
                                         int textureWidth, int textureHeight) {

        graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
}

