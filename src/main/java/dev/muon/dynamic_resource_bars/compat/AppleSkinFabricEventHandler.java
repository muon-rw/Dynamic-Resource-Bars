package dev.muon.dynamic_resource_bars.compat;

#if FABRIC
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.StaminaBarBehavior;
import squeek.appleskin.api.event.HUDOverlayEvent;
#endif
public class AppleSkinFabricEventHandler {
    #if FABRIC

    public static void init() {
        if (!AppleSkinCompat.isLoaded()) {
            return;
        }

        HUDOverlayEvent.HungerRestored.EVENT.register(event -> {
            if (ModConfigManager.getClient().staminaBarBehavior.equals(StaminaBarBehavior.FOOD) ) {
                event.isCanceled = true;
            }
        });
        
        HUDOverlayEvent.Saturation.EVENT.register(event -> {
            if (ModConfigManager.getClient().staminaBarBehavior.equals(StaminaBarBehavior.FOOD) ) {
                event.isCanceled = true;
            }
        });
        
        HUDOverlayEvent.HealthRestored.EVENT.register(event -> {
            if (ModConfigManager.getClient().enableHealthBar) {
                event.isCanceled = true;
            }
        });
        
        HUDOverlayEvent.Exhaustion.EVENT.register(event -> {
            if (ModConfigManager.getClient().staminaBarBehavior.equals(StaminaBarBehavior.FOOD) ) {
                event.isCanceled = true;
            }
        });
    }
    #endif
}