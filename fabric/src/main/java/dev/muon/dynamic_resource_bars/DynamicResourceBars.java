package dev.muon.dynamic_resource_bars;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.muon.dynamic_resource_bars.client.AppleSkinCancellationFabric;
import dev.muon.dynamic_resource_bars.client.FabricAnimationCacheReloadListener;
import dev.muon.dynamic_resource_bars.compat.AppleSkinCompat;
import dev.muon.dynamic_resource_bars.compat.AppleSkinCompatFabric;
import dev.muon.dynamic_resource_bars.compat.ManaAttributesManaProvider;
import dev.muon.dynamic_resource_bars.compat.ManaProviderManager;
import dev.muon.dynamic_resource_bars.compat.StaminaAttributesProvider;
import dev.muon.dynamic_resource_bars.compat.StaminaProviderManager;
import dev.muon.dynamic_resource_bars.config.gui.ModConfigScreen;
import dev.muon.dynamic_resource_bars.harness.UiHarness;
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.util.TickHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

public class DynamicResourceBars implements ClientModInitializer, ModMenuApi {

    @Override
    public void onInitializeClient() {
        CommonClass.initClient();

        ClientTickEvents.END_CLIENT_TICK.register(client -> TickHandler.onClientTick());
        if (UiHarness.isEnabled()) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> UiHarness.onClientTick());
        }

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new FabricAnimationCacheReloadListener());

        if (Services.PLATFORM.isModLoaded(AppleSkinCompat.MOD_ID)) {
            AppleSkinCompat.setProvider(new AppleSkinCompatFabric());
            AppleSkinCancellationFabric.install();
        }

        if (Services.PLATFORM.isModLoaded(ManaProviderManager.MANA_ATTRIBUTES_MOD_ID)) {
            ManaProviderManager.setManaAttributesProvider(new ManaAttributesManaProvider());
        }
        if (Services.PLATFORM.isModLoaded(StaminaProviderManager.STAMINA_ATTRIBUTES_MOD_ID)) {
            StaminaProviderManager.setStaminaAttributesProvider(new StaminaAttributesProvider());
        }
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModConfigScreen::new;
    }
}
