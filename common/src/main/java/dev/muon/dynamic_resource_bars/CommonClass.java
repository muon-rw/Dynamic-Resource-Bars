package dev.muon.dynamic_resource_bars;

import dev.muon.dynamic_resource_bars.compat.ManaProviderManager;
import dev.muon.dynamic_resource_bars.compat.StaminaProviderManager;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.platform.Services;

/** Loader-agnostic client-side init entry, called from each loader's mod entry. */
public class CommonClass {

    public static void initClient() {
        Constants.LOG.info("Initializing Dynamic Resource Bars on {}", Services.PLATFORM.getPlatformName());
        ModConfigManager.initializeConfig();
        ManaProviderManager.initialize();
        StaminaProviderManager.initialize();
    }
}
