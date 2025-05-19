package dev.muon.dynamic_resource_bars.config;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import net.minecraft.client.Minecraft; // Needed for client-side checks, though platform specifics handle it better
import java.nio.file.Path;

// Platform specific imports for environment type and config path
#if FABRIC
    import net.fabricmc.loader.api.FabricLoader;
    import net.fabricmc.api.EnvType;
#elif FORGE
    import net.minecraftforge.fml.loading.FMLPaths;
    import net.minecraftforge.fml.loading.FMLEnvironment;
    import net.minecraftforge.api.distmarker.Dist;
#elif NEO
    import net.neoforged.fml.loading.FMLPaths;
    import net.neoforged.fml.loading.FMLEnvironment;
    import net.neoforged.api.distmarker.Dist;
#endif

public class ModConfigManager {

    // No more ClientConfig field here, it's a singleton in ClientConfig itself.
    // No more Spec fields.

    public static ClientConfig getClient() {
        return ClientConfig.getInstance();
    }

    // The parameters for modContainer/context are removed as they were for spec registration.
    public static void initializeConfig() {
        boolean isClient = false;
        Path configPath = null;

        #if FABRIC
            isClient = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
            if (isClient) {
                configPath = FabricLoader.getInstance().getConfigDir().resolve(DynamicResourceBars.ID + "-client.json");
            }
        #elif FORGELIKE // Covers both Forge and NeoForge with similar logic
            isClient = FMLEnvironment.dist == Dist.CLIENT;
            if (isClient) {
                configPath = FMLPaths.CONFIGDIR.get().resolve(DynamicResourceBars.ID + "-client.json");
            }
        #endif

        if (!isClient) {
            DynamicResourceBars.LOGGER.info("Not on the client, skipping client config initialization.");
            return;
        }

        if (configPath == null) {
            DynamicResourceBars.LOGGER.error("Could not determine config path for GSON client config. This is a critical error.");
            // In this state, ClientConfig.getInstance() will also log an error and use a non-persistent default.
            // We could throw an exception here, but allowing the game to load with a default (non-saving) config might be preferable.
            ClientConfig.getInstance(); // Ensure it tries to init and logs its own path error
            return;
        }

        ClientConfig.setConfigPath(configPath);
        ClientConfig.getInstance(); // This triggers the first load or default creation & save
        DynamicResourceBars.LOGGER.info("Client config initialized with GSON at: {}", configPath);
    }
} 