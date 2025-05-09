package dev.muon.dynamic_resource_bars.foundation.config;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;

#if FABRIC
    #if AFTER_21_1
        import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
        import net.neoforged.fml.config.ModConfig;
        import net.neoforged.neoforge.common.ModConfigSpec;
    #else
        import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
        import net.minecraftforge.fml.config.ModConfig;
        import net.minecraftforge.common.ForgeConfigSpec;
    #endif
    import net.fabricmc.loader.api.FabricLoader;
    import net.fabricmc.api.EnvType;
#elif FORGE
    import net.minecraftforge.fml.ModLoadingContext;
    import net.minecraftforge.fml.config.ModConfig;
    import net.minecraftforge.common.ForgeConfigSpec;
    import net.minecraftforge.fml.loading.FMLEnvironment;
    import net.minecraftforge.api.distmarker.Dist;
#elif NEO
    // import net.neoforged.fml.ModLoadingContext; // For NeoForge, registration is usually via ModContainer
    import net.neoforged.fml.config.ModConfig;
    import net.neoforged.neoforge.common.ModConfigSpec;
    import net.neoforged.fml.loading.FMLEnvironment;
    import net.neoforged.api.distmarker.Dist;
    import net.neoforged.fml.ModContainer;
#endif

public class ModConfigManager {

    private static CClient clientConfig;
    #if FABRIC
        #if AFTER_21_1
            private static ModConfigSpec clientSpec;
        #else
            private static ForgeConfigSpec clientSpec;
        #endif
    #elif FORGE
        private static ForgeConfigSpec clientSpec;
    #elif NEO
        private static ModConfigSpec clientSpec;
    #endif

    public static CClient getClient() {
        return clientConfig;
    }

    // Call this method from your main mod class constructor or initialization phases
    public static void registerConfigs(#if NEO ModContainer modContainer #endif) {
        // Since it's a client-only mod, we don't need the isClientContext check as aggressively,
        // but it's good practice if common configs were ever introduced.
        #if FABRIC
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
                return;
            }
        #elif FORGELIKE // Covers FORGE and NEO
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
        #endif

        #if FABRIC
            #if AFTER_21_1
                ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
            #else
                ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
            #endif
        #elif FORGE
            ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        #elif NEO
            ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        #endif

        clientConfig = new CClient(clientBuilder);
        clientSpec = clientBuilder.build();

        #if FABRIC
            #if AFTER_21_1
                NeoForgeConfigRegistry.INSTANCE.register(DynamicResourceBars.ID, ModConfig.Type.CLIENT, clientSpec);
            #else
                ForgeConfigRegistry.INSTANCE.register(DynamicResourceBars.ID, ModConfig.Type.CLIENT, clientSpec);
            #endif
        #elif FORGE
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientSpec);
        #elif NEO
            modContainer.registerConfig(ModConfig.Type.CLIENT, clientSpec);
        #endif
    }
} 