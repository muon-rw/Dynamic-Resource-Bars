package dev.muon.dynamic_resource_bars.util;

import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;

public class TickHandler {
    // TODO: Maybe tie this to deltaTracker/frameTime instead. Not sure.
    /**
     * Called once per client tick to update time-based animations and effects
     */
    public static void onClientTick() {
        StaminaBarRenderer.updateFlashAlpha();
        HealthBarRenderer.updateFlashAlpha();
    }
    
    /**
     * Get the current flash alpha for overlay pulsing effects
     * @return Flash alpha value between 0.0 and 1.0
     */
    public static float getOverlayFlashAlpha() {
        return StaminaBarRenderer.getFlashAlpha();
    }
} 