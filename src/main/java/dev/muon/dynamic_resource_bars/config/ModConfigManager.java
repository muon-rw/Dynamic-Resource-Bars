package dev.muon.dynamic_resource_bars.config;

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
import net.minecraftforge.fml.config.ModConfig;
    import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    private static ClientConfig clientConfig;
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

    public static ClientConfig getClient() {
        return clientConfig;
    }

    public static void registerConfigs(#if NEO ModContainer modContainer #elif FORGE FMLJavaModLoadingContext context #endif) {
        #if FABRIC
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
                return;
            }
        #elif FORGELIKE
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

        clientConfig = new ClientConfig(clientBuilder);
        clientSpec = clientBuilder.build();

        #if FABRIC
            #if AFTER_21_1
                NeoForgeConfigRegistry.INSTANCE.register(DynamicResourceBars.ID, ModConfig.Type.CLIENT, clientSpec);
            #else
                ForgeConfigRegistry.INSTANCE.register(DynamicResourceBars.ID, ModConfig.Type.CLIENT, clientSpec);
            #endif
        #elif FORGE
            context.registerConfig(ModConfig.Type.CLIENT, clientSpec);
        #elif NEO
            modContainer.registerConfig(ModConfig.Type.CLIENT, clientSpec);
        #endif
    }
} 