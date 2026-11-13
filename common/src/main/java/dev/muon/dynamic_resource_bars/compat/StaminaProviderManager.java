package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import dev.muon.dynamic_resource_bars.provider.StaminaProvider;
import dev.muon.dynamic_resource_bars.provider.StaminaProviderRegistry;
import net.minecraft.client.Minecraft;

/**
 * Stamina provider registry. The food provider is always available; the Combat Attributes
 * provider is wired in only when {@code combat_attributes} is on the runtime classpath, so
 * the dependency stays optional (the JVM never resolves the CA classes when the mod is absent).
 *
 * <p>Loader-specific providers (e.g. Paragliders, NeoForge-only) install themselves via
 * {@link #setParaglidersProvider(StaminaProvider)} from the loader's mod entry. This keeps
 * the Paragliders classes off the common-module classpath while still letting common code
 * route the {@link StaminaBarBehavior#PARAGLIDERS} behavior through the registered impl.
 */
public class StaminaProviderManager {

    public static final String STAMINA_ATTRIBUTES_MOD_ID = "staminaattributes";
    public static final String PARAGLIDERS_MOD_ID = "paraglider";

    private static FoodStaminaProvider foodProvider;
    private static StaminaProvider staminaAttributesProvider;
    private static StaminaProvider paraglidersProvider;

    private static final StaminaProvider EMPTY_PROVIDER = new StaminaProvider() {
        @Override public float getCurrentStamina(net.minecraft.world.entity.player.Player p) { return 0; }
        @Override public float getMaxStamina(net.minecraft.world.entity.player.Player p) { return 0; }
        @Override public long getGameTime() {
            return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        }
    };

    public static void init() {
        StaminaProviderRegistry.clear();
        if (foodProvider == null) foodProvider = new FoodStaminaProvider();
        StaminaProviderRegistry.registerProvider(() -> foodProvider);
        if (staminaAttributesProvider != null) {
            StaminaProviderRegistry.registerProvider(() -> staminaAttributesProvider);
        }
        if (paraglidersProvider != null) {
            StaminaProviderRegistry.registerProvider(() -> paraglidersProvider);
        }
        StaminaProviderRegistry.registerProvider(() -> EMPTY_PROVIDER);
    }

    public static StaminaProvider getProviderForBehavior(StaminaBarBehavior behavior) {
        return switch (behavior) {
            case FOOD -> foodProvider != null ? foodProvider : EMPTY_PROVIDER;
            case STAMINA_ATTRIBUTES -> staminaAttributesProvider != null ? staminaAttributesProvider : EMPTY_PROVIDER;
            case PARAGLIDERS -> paraglidersProvider != null ? paraglidersProvider : EMPTY_PROVIDER;
            case OFF -> EMPTY_PROVIDER;
        };
    }

    public static StaminaProvider getCurrentProvider() {
        return getProviderForBehavior(ModConfigManager.getClient().staminaBarBehavior);
    }

    public static boolean isModLoaded(String modId) {
        return Services.PLATFORM.isModLoaded(modId);
    }

    /** True when the configured behavior has a real provider available (not just registered as a stub). */
    public static boolean isModLoaded(StaminaBarBehavior behavior) {
        return switch (behavior) {
            case FOOD -> true;
            case STAMINA_ATTRIBUTES -> staminaAttributesProvider != null;
            case PARAGLIDERS -> paraglidersProvider != null;
            case OFF -> true;
        };
    }

    public static boolean hasAnyStaminaMods() {
        return staminaAttributesProvider != null || paraglidersProvider != null;
    }

    public static void setStaminaAttributesProvider(StaminaProvider provider) {
        staminaAttributesProvider = provider;
    }

    /**
     * Installs the Paragliders provider from a loader-specific entry point. Paragliders is
     * NeoForge-only on this MC version, so its {@link StaminaProvider} impl can't live in
     * common — the loader's mod entry calls this before {@link #initialize()} runs.
     */
    public static void setParaglidersProvider(StaminaProvider provider) {
        paraglidersProvider = provider;
    }

    public static void initialize() {
        foodProvider = new FoodStaminaProvider();
        updateActiveProvider();
    }

    public static void updateActiveProvider() {
        StaminaBarBehavior behavior = ModConfigManager.getClient().staminaBarBehavior;
        if (behavior == null) behavior = StaminaBarBehavior.FOOD;
        StaminaProviderRegistry.setActiveProvider(getProviderForBehavior(behavior));
    }
}
