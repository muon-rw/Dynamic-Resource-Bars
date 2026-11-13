package dev.muon.dynamic_resource_bars;

import dev.muon.dynamic_resource_bars.client.AppleSkinCancellationNeoForge;
import dev.muon.dynamic_resource_bars.client.NeoForgeAnimationCacheReloadListener;
import dev.muon.dynamic_resource_bars.client.NeoForgeCompatSuppression;
import dev.muon.dynamic_resource_bars.client.NeoForgeHudWiring;
import dev.muon.dynamic_resource_bars.compat.AppleSkinCompat;
import dev.muon.dynamic_resource_bars.compat.AppleSkinCompatNeoForge;
import dev.muon.dynamic_resource_bars.compat.ArsNouveauManaProvider;
import dev.muon.dynamic_resource_bars.compat.IronsSpellbooksManaProvider;
import dev.muon.dynamic_resource_bars.compat.LsoCompat;
import dev.muon.dynamic_resource_bars.compat.LsoCompatNeoForge;
import dev.muon.dynamic_resource_bars.compat.ManaProviderManager;
import dev.muon.dynamic_resource_bars.compat.ParaglidersStaminaProvider;
import dev.muon.dynamic_resource_bars.compat.StaminaProviderManager;
import dev.muon.dynamic_resource_bars.compat.VampireBloodCompat;
import dev.muon.dynamic_resource_bars.compat.WitcheryAbilityCompat;
import dev.muon.dynamic_resource_bars.compat.WitcheryCompatNeoForge;
import dev.muon.dynamic_resource_bars.config.gui.ModConfigScreen;
import dev.muon.dynamic_resource_bars.harness.UiHarness;
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.util.TickHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
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
        if (UiHarness.isEnabled()) {
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> UiHarness.onClientTick());
        }

        if (Services.PLATFORM.isModLoaded(AppleSkinCompat.MOD_ID)) {
            AppleSkinCompat.setProvider(new AppleSkinCompatNeoForge());
            AppleSkinCancellationNeoForge.install();
        }

        // Paragliders is NeoForge-only — install the provider here so common's StaminaProviderManager
        // can route StaminaBarBehavior.PARAGLIDERS through it without referencing Paragliders classes.
        if (Services.PLATFORM.isModLoaded(StaminaProviderManager.PARAGLIDERS_MOD_ID)) {
            StaminaProviderManager.setParaglidersProvider(new ParaglidersStaminaProvider());
        }
        if (Services.PLATFORM.isModLoaded(ManaProviderManager.IRONS_SPELLBOOKS_MOD_ID)) {
            ManaProviderManager.setIronsProvider(new IronsSpellbooksManaProvider());
        }
        if (Services.PLATFORM.isModLoaded(ManaProviderManager.ARS_NOUVEAU_MOD_ID)) {
            ManaProviderManager.setArsProvider(new ArsNouveauManaProvider());
        }
        if (Services.PLATFORM.isModLoaded(LsoCompat.MOD_ID)) {
            LsoCompat.setProvider(new LsoCompatNeoForge());
        }
        if (Services.PLATFORM.isModLoaded(VampireBloodCompat.WITCHERY_MOD_ID)) {
            WitcheryCompatNeoForge witcheryCompat = new WitcheryCompatNeoForge();
            VampireBloodCompat.setProvider(witcheryCompat);
            WitcheryAbilityCompat.setProvider(witcheryCompat);
        }
        NeoForgeCompatSuppression.install();
    }

    private void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(CommonClass::initClient);
    }

    private void onAddClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new NeoForgeAnimationCacheReloadListener());
    }
}
