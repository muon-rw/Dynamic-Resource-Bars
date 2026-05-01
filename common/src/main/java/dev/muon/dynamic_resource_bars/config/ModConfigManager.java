package dev.muon.dynamic_resource_bars.config;

import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.platform.Services;

import java.nio.file.Path;

public class ModConfigManager {

    public static ClientConfig getClient() {
        return ClientConfig.getInstance();
    }

    public static void initializeConfig() {
        if (!Services.PLATFORM.isClient()) {
            Constants.LOG.info("Not on the client, skipping client config initialization.");
            return;
        }

        Path configPath = Services.PLATFORM.getConfigDir().resolve(Constants.MOD_ID + "-client.json");
        ClientConfig.setConfigPath(configPath);
        ClientConfig.getInstance(); // Triggers first load or default creation & save
        Constants.LOG.info("Client config initialized with GSON at: {}", configPath);
    }
}
