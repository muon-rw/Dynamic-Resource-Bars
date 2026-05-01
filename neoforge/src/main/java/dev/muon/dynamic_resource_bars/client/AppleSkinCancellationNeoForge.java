package dev.muon.dynamic_resource_bars.client;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import net.neoforged.neoforge.common.NeoForge;
import squeek.appleskin.api.event.HUDOverlayEvent;

/**
 * Cancels AppleSkin's HUD overlays on NeoForge when our bars take over the rendering.
 *
 * <p>NeoForge AppleSkin {@code HUDOverlayEvent} implements {@link net.neoforged.bus.api.ICancellableEvent}
 * (different mechanism than Fabric's {@code event.isCanceled} field). Caller must guard
 * {@link #install()} with {@code Services.PLATFORM.isModLoaded("appleskin")}.
 */
public final class AppleSkinCancellationNeoForge {

    private AppleSkinCancellationNeoForge() {}

    public static void install() {
        NeoForge.EVENT_BUS.addListener(HUDOverlayEvent.HungerRestored.class, event -> {
            if (suppressFoodOverlays()) event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener(HUDOverlayEvent.Saturation.class, event -> {
            if (suppressFoodOverlays()) event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener(HUDOverlayEvent.HealthRestored.class, event -> {
            if (suppressHealthOverlays()) event.setCanceled(true);
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
