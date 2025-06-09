package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;

#if FORGE
    import net.minecraftforge.api.distmarker.Dist;
    import net.minecraftforge.eventbus.api.SubscribeEvent;
    import net.minecraftforge.fml.common.Mod;
    import net.minecraftforge.common.MinecraftForge;
#elif NEO
    import net.neoforged.api.distmarker.Dist;
    import net.neoforged.bus.api.SubscribeEvent;
    import net.neoforged.fml.common.EventBusSubscriber;
    import net.neoforged.neoforge.common.NeoForge;
#endif
#if FORGELIKE
    import squeek.appleskin.api.event.HUDOverlayEvent;
#endif

// Moved event registration to PlatformUtil#isModLoaded-wrapped call in DynamicResourceBars init
#if FORGE
// Removed @Mod.EventBusSubscriber(modid = DynamicResourceBars.ID, value = Dist.CLIENT)
#elif NEO
// Removed @EventBusSubscriber(modid = DynamicResourceBars.ID, value = Dist.CLIENT)
#endif
public class AppleSkinEventHandler {
    
    #if FORGELIKE
    @SubscribeEvent
    public static void onHungerOverlay(HUDOverlayEvent.HungerRestored event) {
        if (AppleSkinCompat.isLoaded() && ModConfigManager.getClient().enableStaminaBar) {
            event.setCanceled(true);
        }
    }
    
    @SubscribeEvent
    public static void onSaturationOverlay(HUDOverlayEvent.Saturation event) {
        if (AppleSkinCompat.isLoaded() && ModConfigManager.getClient().enableStaminaBar) {
            event.setCanceled(true);
        }
    }
    
    @SubscribeEvent
    public static void onHealthOverlay(HUDOverlayEvent.HealthRestored event) {
        if (AppleSkinCompat.isLoaded() && ModConfigManager.getClient().enableHealthBar) {
            event.setCanceled(true);
        }
    }
    
    @SubscribeEvent
    public static void onExhaustionOverlay(HUDOverlayEvent.Exhaustion event) {
        if (AppleSkinCompat.isLoaded() && ModConfigManager.getClient().enableStaminaBar) {
            event.setCanceled(true);
        }
    }

    public static void register() {
        #if FORGE
            MinecraftForge.EVENT_BUS.register(AppleSkinEventHandler.class);
        #elif NEO
            NeoForge.EVENT_BUS.register(AppleSkinEventHandler.class);
        #endif
    }
    #endif
} 