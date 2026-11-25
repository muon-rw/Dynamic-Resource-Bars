package dev.muon.dynamic_resource_bars.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "dynamic_resource_bars", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "resource_bars", GuiOverlays.RESOURCE_BARS);
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), "resource_bars_above_xp", GuiOverlays.RESOURCE_BARS_ABOVE_XP);
    }
}