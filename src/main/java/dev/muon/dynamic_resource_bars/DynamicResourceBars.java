package dev.muon.dynamic_resource_bars;


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
    import com.terraformersmc.modmenu.api.ConfigScreenFactory;
    import com.terraformersmc.modmenu.api.ModMenuApi;
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
import net.minecraftforge.client.ConfigScreenHandler;
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
        #endif
        
        AllConfigs.register((type, spec) -> {
        #if FORGE
            ModLoadingContext.get().registerConfig(type, spec);
        #elif NEO
        modContainer.registerConfig(type, spec);
        #endif
        });

        // --- Config SCREEN Registration (Forge) ---
        #if FORGE
        // CORRECT: Register the screen factory extension point separately
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, screen) -> new ModConfigScreen(screen)
                )
        );
        #endif

        // --- Config SCREEN Registration (NeoForge) ---
        #if NEO
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
            (minecraft, screen) -> new ModConfigScreen(screen)
        );
        #endif

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
                    ForgeConfigRegistry.INSTANCE.register(DynamicResourceBars.ID, type, spec);
                #endif
            });
        #endif
    }

    #if FABRIC @Override #endif
    public void onInitializeClient() {
        #if FABRIC
            #if AFTER_21_1
            ConfigScreenFactoryRegistry.INSTANCE.register(DynamicResourceBars.ID, 
                (minecraft, screen) -> new ModConfigScreen(screen) 
            );

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
