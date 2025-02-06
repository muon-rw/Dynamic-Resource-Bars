package dev.muon.dynamic_resource_bars.event;

#if FORGE
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DynamicResourceBars.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "resource_bars", GuiOverlays.RESOURCE_BARS);
    }
}
#endif