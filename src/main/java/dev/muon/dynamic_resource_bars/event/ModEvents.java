package dev.muon.dynamic_resource_bars.event;

#if FORGE
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 1.20.1 Forge Only. See CommonEvents for 1.21.1, GuiMixin for 1.20.1 Fabric
@Mod.EventBusSubscriber(modid = DynamicResourceBars.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "resource_bars", GuiOverlays.RESOURCE_BARS);
    }
    
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new dev.muon.dynamic_resource_bars.util.AnimationCacheReloadListener());
    }
}
#endif