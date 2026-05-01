package dev.muon.dynamic_resource_bars;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.muon.dynamic_resource_bars.client.AppleSkinCancellationFabric;
import dev.muon.dynamic_resource_bars.client.CombatAttributesSuppressionFabric;
import dev.muon.dynamic_resource_bars.client.FabricAnimationCacheReloadListener;
import dev.muon.dynamic_resource_bars.client.FabricHudWiring;
import dev.muon.dynamic_resource_bars.client.FabricStatusBarHeights;
import dev.muon.dynamic_resource_bars.compat.AppleSkinCompat;
import dev.muon.dynamic_resource_bars.compat.AppleSkinCompatFabric;
import dev.muon.dynamic_resource_bars.config.gui.ModConfigScreen;
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.util.TickHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

public class DynamicResourceBars implements ClientModInitializer, ModMenuApi {

    @Override
    public void onInitializeClient() {
        CommonClass.initClient();

        ClientTickEvents.END_CLIENT_TICK.register(client -> TickHandler.onClientTick());

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new FabricAnimationCacheReloadListener());

        FabricHudWiring.register();
        // Tell Fabric how tall our wrapped bars are so vanilla layers above us reposition
        // correctly. Must run before CLIENT_STARTED — the height registry freezes there.
        FabricStatusBarHeights.install();

        if (Services.PLATFORM.isModLoaded(AppleSkinCompat.MOD_ID)) {
            AppleSkinCompat.setProvider(new AppleSkinCompatFabric());
            AppleSkinCancellationFabric.install();
        }

        // CA registers its HUD layers in its own onInitializeClient; entrypoint order across
        // mods is not deterministic, so we defer our wrap until CLIENT_STARTED — that fires
        // from Minecraft.run() after every mod's onInitializeClient has completed, so CA's
        // layers are guaranteed to be in the registry by then.
        if (Services.PLATFORM.isModLoaded("combat_attributes")) {
            ClientLifecycleEvents.CLIENT_STARTED.register(client -> CombatAttributesSuppressionFabric.install());
        }
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModConfigScreen::new;
    }
}
