package dev.muon.dynamic_resource_bars;

import dev.muon.dynamic_resource_bars.compat.AppleSkinFabricEventHandler;
import dev.muon.dynamic_resource_bars.config.gui.ModConfigScreen;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.TickHandler;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

#if after_21_1
    import fuzs.puzzleslib.api.client.event.v1.gui.RenderGuiLayerEvents;
    import dev.muon.dynamic_resource_bars.event.CommonEvents;
#endif

#if FABRIC
    import net.fabricmc.api.ClientModInitializer;
    import com.terraformersmc.modmenu.api.ConfigScreenFactory;
    import com.terraformersmc.modmenu.api.ModMenuApi;
    import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
#endif

#if FORGE
    import net.minecraftforge.fml.common.Mod;
    import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
    import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
    import net.minecraftforge.client.ConfigScreenHandler;
    import net.minecraftforge.common.MinecraftForge;
    import net.minecraftforge.event.TickEvent;
    import net.minecraftforge.eventbus.api.SubscribeEvent;
#endif

#if NEO
    import net.neoforged.fml.common.Mod;
    import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
    import net.neoforged.bus.api.IEventBus;
    import net.neoforged.fml.ModContainer;
    import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
    import net.neoforged.neoforge.client.event.ClientTickEvent;
    import net.neoforged.neoforge.common.NeoForge;
    import net.neoforged.bus.api.SubscribeEvent;
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

    #if FABRIC
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ModConfigScreen(parent);
    }
    #endif

    public DynamicResourceBars(#if NEO IEventBus modEventBus, ModContainer modContainer #elif FORGE FMLJavaModLoadingContext fmlContext #endif) {
        #if FORGE
            fmlContext.getModEventBus().addListener(this::clientSetup);
            fmlContext.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new ModConfigScreen(screen)));
        #elif NEO
            modEventBus.addListener(this::clientSetup);
             modContainer.registerExtensionPoint(IConfigScreenFactory.class, 
                (mc, screen) -> new ModConfigScreen(screen));
        #endif

        #if NEWER_THAN_20_1
            RenderGuiLayerEvents.before(RenderGuiLayerEvents.PLAYER_HEALTH).register(CommonEvents::onRenderPlayerHealth);
            RenderGuiLayerEvents.before(RenderGuiLayerEvents.FOOD_LEVEL).register(CommonEvents::onRenderHunger);
            RenderGuiLayerEvents.before(RenderGuiLayerEvents.ARMOR_LEVEL).register(CommonEvents::onRenderArmor);
        #endif
    }

    #if FABRIC @Override #endif
    public void onInitializeClient() {
        ModConfigManager.initializeConfig();
        #if FABRIC
        ClientTickEvents.END_CLIENT_TICK.register(client -> TickHandler.onClientTick());
            #if NEWER_THAN_20_1
            AppleSkinFabricEventHandler.init();
            #endif
        #endif
    }

    #if FORGELIKE
    public void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            onInitializeClient();
        });
        
        #if FORGE
        MinecraftForge.EVENT_BUS.register(this);
        #elif NEO
        NeoForge.EVENT_BUS.register(this);
        #endif
    }

    @SubscribeEvent
    #if FORGE
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
    #elif NEO
    public void onClientTick(ClientTickEvent.Post event) {
    #endif
            TickHandler.onClientTick();
    #if FORGE } #endif
    }
    #endif
}
