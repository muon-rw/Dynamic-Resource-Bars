package dev.muon.dynamic_resource_bars.util;

import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;

public class TickHandler {
    // Overlay pulsing animation state
    private static float unclampedFlashAlpha = 0f;
    private static float flashAlpha = 0f;
    private static byte alphaDir = 1;
    
    // TODO: Migrate to deltaTracker/frameTime instead of tickEvent
    /**
     * Called once per client tick to update time-based animations and effects
     */
    public static void onClientTick() {
        updateFlashAlpha();
    }
    
    /**
     * Updates the flash alpha for pulsing overlay effects
     */
    private static void updateFlashAlpha() {
        unclampedFlashAlpha += alphaDir * 0.0625F;
        if (unclampedFlashAlpha >= 1.5F) {
            alphaDir = -1;
        } else if (unclampedFlashAlpha <= -0.5F) {
            alphaDir = 1;
        }
        // Max alpha of 0.5 for the pulsing effect
        flashAlpha = Math.max(0F, Math.min(1F, unclampedFlashAlpha)) * 0.5F;
    }
    
    /**
     * Get the current flash alpha for overlay pulsing effects
     * @return Flash alpha value between 0.0 and 0.5
     */
    public static float getOverlayFlashAlpha() {
        return flashAlpha;
    }
    
    /**
     * Reset the flash animation to its initial state
     */
    public static void resetFlash() {
        unclampedFlashAlpha = flashAlpha = 0;
        alphaDir = 1;
    }
} 