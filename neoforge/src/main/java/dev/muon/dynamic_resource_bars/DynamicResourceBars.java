package dev.muon.dynamic_resource_bars;

import dev.muon.dynamic_resource_bars.client.AppleSkinCancellationNeoForge;
import dev.muon.dynamic_resource_bars.client.NeoForgeAnimationCacheReloadListener;
import dev.muon.dynamic_resource_bars.client.NeoForgeHudWiring;
import dev.muon.dynamic_resource_bars.compat.AppleSkinCompat;
import dev.muon.dynamic_resource_bars.compat.AppleSkinCompatNeoForge;
import dev.muon.dynamic_resource_bars.config.gui.ModConfigScreen;
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.util.TickHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class DynamicResourceBars {

    public DynamicResourceBars(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(NeoForgeHudWiring::register);
        modEventBus.addListener(this::onAddClientReloadListeners);
        modEventBus.addListener(this::onClientSetup);

        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new ModConfigScreen(parent));

        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> TickHandler.onClientTick());

        if (Services.PLATFORM.isModLoaded(AppleSkinCompat.MOD_ID)) {
            AppleSkinCompat.setProvider(new AppleSkinCompatNeoForge());
            AppleSkinCancellationNeoForge.install();
        }
    }

    private void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(CommonClass::initClient);
    }

    private void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Constants.loc("animation_cache"), new NeoForgeAnimationCacheReloadListener());
    }
}
