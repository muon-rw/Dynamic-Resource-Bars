package dev.muon.dynamic_resource_bars;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.muon.dynamic_resource_bars.client.gui.ModConfigScreen;
import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


#if after_21_1
    import fuzs.puzzleslib.api.client.event.v1.gui.RenderGuiLayerEvents;
    import dev.muon.dynamic_resource_bars.event.CommonEvents;
#endif

#if FABRIC
    import net.fabricmc.api.ClientModInitializer;
    import net.fabricmc.api.ModInitializer;
    #if after_21_1
    import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
    import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.client.ConfigScreenFactoryRegistry;
    import net.neoforged.neoforge.client.gui.ConfigurationScreen;
    #endif

    #if current_20_1
    import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
    #endif
#endif

#if FORGE
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
#endif


#if NEO
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
#endif


#if FORGELIKE
@Mod("dynamic_resource_bars")
#endif
public class DynamicResourceBars #if FABRIC implements ModInitializer, ClientModInitializer, ModMenuApi #endif
{
    public static final String MODNAME = "Dynamic RPG Resource Bars";
    public static final String ID = "dynamic_resource_bars";
    public static final Logger LOGGER = LogManager.getLogger(MODNAME);

    public static ResourceLocation loc(String path) {
        #if NEWER_THAN_20_1
            return ResourceLocation.fromNamespaceAndPath(ID, path);
        #else
            return new ResourceLocation(ID, path);
        #endif
    }

    #if FABRIC
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ModConfigScreen(parent);
    }
    #endif

    public DynamicResourceBars(#if NEO IEventBus modEventBus, ModContainer modContainer #endif) {
        #if FORGE
        var context = FMLJavaModLoadingContext.get();
        var modEventBus = context.getModEventBus();
        #endif

        #if FORGELIKE
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        AllConfigs.register((type, spec) -> {
            #if FORGE
            // ModLoadingContext.get().registerConfig(type, spec);
            #elif NEO
            // modContainer.registerConfig(type, spec); // Config registration itself is fine
            #endif
        });
        
        // Config Screen Registration (Example for NeoForge)
        #if NEO
        modContainer.registerConfig(AllConfigs.CLIENT_CONFIG.getRight(), AllConfigs.CLIENT_CONFIG.getLeft()); // Example actual registration
        // Replace the default config screen with our custom one
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, 
            (minecraft, screen) -> new ModConfigScreen(screen)
        );
        #endif
        #endif

        // 1.20.1 Forge is handled via ForgeEvents
        // 1.20.1 Fabric is handled via mixin
        #if NEWER_THAN_20_1
        RenderGuiLayerEvents.before(RenderGuiLayerEvents.PLAYER_HEALTH)
                .register(CommonEvents::onRenderPlayerHealth);
        RenderGuiLayerEvents.before(RenderGuiLayerEvents.FOOD_LEVEL)
                .register(CommonEvents::onRenderHunger);
        RenderGuiLayerEvents.before(RenderGuiLayerEvents.ARMOR_LEVEL)
                .register(CommonEvents::onRenderArmor);
        #endif
    }


    #if FABRIC @Override #endif
    public void onInitialize() {
        #if FABRIC
            AllConfigs.register((type, spec) -> {
                #if AFTER_21_1
                NeoForgeConfigRegistry.INSTANCE.register(DynamicResourceBars.ID, type, spec);
                #else
                // For 1.20.1 Fabric, ForgeConfigRegistry is used for config file, ModMenuApi for screen
                ForgeConfigRegistry.INSTANCE.register(DynamicResourceBars.ID, type, spec);
                #endif
            });
        #endif
    }

    #if FABRIC @Override #endif
    public void onInitializeClient() {
        #if FABRIC
            #if AFTER_21_1
            // This uses ForgeConfigApiPort's registry for newer versions
            ConfigScreenFactoryRegistry.INSTANCE.register(DynamicResourceBars.ID, 
                (minecraft, screen) -> new ModConfigScreen(screen) 
            );
            #else
            // For 1.20.1 Fabric, ModMenu finds the config screen via the ModMenuApi interface implemented by this class.
            // No explicit registration needed here for ModMenu itself if getModConfigScreenFactory() is present.
            // The ForgeConfigRegistry call in onInitialize handles the config *file*.
            System.out.println(ID + ": ModMenu will use getModConfigScreenFactory() for config screen on this version.");
            #endif
        #endif
    }

    #if FORGELIKE
    public void commonSetup(FMLCommonSetupEvent event) { onInitialize(); }
    public void clientSetup(FMLClientSetupEvent event) { 
        onInitializeClient(); 
    }
    #endif
}
