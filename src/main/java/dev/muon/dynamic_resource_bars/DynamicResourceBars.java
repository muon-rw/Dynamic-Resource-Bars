package dev.muon.dynamic_resource_bars;

import dev.muon.dynamic_resource_bars.config.gui.ModConfigScreen;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
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
        import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.client.ConfigScreenFactoryRegistry;
    #endif
#endif

#if FORGE
    import net.minecraftforge.fml.common.Mod;
    import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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
@Mod(DynamicResourceBars.ID)
#endif
public class DynamicResourceBars #if FABRIC implements ClientModInitializer, ModMenuApi #endif
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

    // ModMenu Config Screen Factory (Fabric)
    #if FABRIC
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ModConfigScreen(parent);
    }
    #endif

    // Constructor: Primarily for Forge/NeoForge due to argument injection
    public DynamicResourceBars(#if NEO IEventBus modEventBus, ModContainer modContainer #elif FORGE FMLJavaModLoadingContext context #endif) {
        #if FORGE
            var forgeModEventBus = context.getModEventBus();
            ModConfigManager.registerConfigs(context);
            forgeModEventBus.addListener(this::clientSetup);
            context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new ModConfigScreen(screen)));
        #elif NEO
            ModConfigManager.registerConfigs(modContainer);
            modEventBus.addListener(this::clientSetup);
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, 
                (mc, screen) -> new ModConfigScreen(screen));
        #endif
        
        // Common event registrations for newer versions, independent of config loader
        #if NEWER_THAN_20_1
            RenderGuiLayerEvents.before(RenderGuiLayerEvents.PLAYER_HEALTH).register(CommonEvents::onRenderPlayerHealth);
            RenderGuiLayerEvents.before(RenderGuiLayerEvents.FOOD_LEVEL).register(CommonEvents::onRenderHunger);
            RenderGuiLayerEvents.before(RenderGuiLayerEvents.ARMOR_LEVEL).register(CommonEvents::onRenderArmor);
        #endif
    }

    // Fabric ClientModInitializer entry point
    #if FABRIC @Override #endif
    public void onInitializeClient() {
        // Called by Fabric directly.
        // Called by Forge/NeoForge via clientSetup listener.
        #if FABRIC
            ModConfigManager.registerConfigs();
            #if AFTER_21_1
                ConfigScreenFactoryRegistry.INSTANCE.register(DynamicResourceBars.ID, 
                    (minecraft, screen) -> new ModConfigScreen(screen) 
                );
            #endif
        #endif
        // Other client-specific initializations can go here.
    }

    // Forge/NeoForge specific setup methods that call the shared onInitialize/onInitializeClient
    #if FORGELIKE
    public void clientSetup(FMLClientSetupEvent event) { 
        this.onInitializeClient(); 
    }
    #endif
}
