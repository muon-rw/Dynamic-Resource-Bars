package dev.muon.dynamic_resource_bars.client;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import squeek.appleskin.api.event.HUDOverlayEvent;

/**
 * Cancels AppleSkin's vanilla-bar HUD overlays when our own bars take over the rendering,
 * so the saturation/restoration previews don't double up. AppleSkin still ticks
 * exhaustion / tooltip overlays normally — we only suppress what would draw over our bars.
 *
 * <p>Caller must guard {@link #install()} with
 * {@code Services.PLATFORM.isModLoaded("appleskin")} so AppleSkin classes are only resolved
 * when the mod is present.
 */
public final class AppleSkinCancellationFabric {

    private AppleSkinCancellationFabric() {}

    public static void install() {
        // Hunger restore + saturation overlays sit on top of the vanilla food row. When our
        // stamina bar is sourcing food, we render those overlays ourselves on the stamina bar.
        HUDOverlayEvent.HungerRestored.EVENT.register(event -> {
            if (suppressFoodOverlays()) event.isCanceled = true;
        });
        HUDOverlayEvent.Saturation.EVENT.register(event -> {
            if (suppressFoodOverlays()) event.isCanceled = true;
        });
        // Health restored sits on the vanilla heart row. When our health bar is custom, we
        // render the estimated-health-on-eat preview ourselves on top of the bar fill.
        HUDOverlayEvent.HealthRestored.EVENT.register(event -> {
            if (suppressHealthOverlays()) event.isCanceled = true;
        });
    }

    private static boolean suppressFoodOverlays() {
        ClientConfig c = ModConfigManager.getClient();
        return c.staminaBarBehavior == StaminaBarBehavior.FOOD;
    }

    private static boolean suppressHealthOverlays() {
        return ModConfigManager.getClient().healthBarBehavior == dev.muon.dynamic_resource_bars.util.BarRenderBehavior.CUSTOM;
    }
}
