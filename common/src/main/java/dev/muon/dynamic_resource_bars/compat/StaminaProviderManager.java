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
 */
public class StaminaProviderManager {

    public static final String COMBAT_ATTRIBUTES_MOD_ID = "combat_attributes";

    private static FoodStaminaProvider foodProvider;
    private static StaminaProvider combatAttributesProvider;

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
        if (combatAttributesProvider != null) {
            StaminaProviderRegistry.registerProvider(() -> combatAttributesProvider);
        }
        StaminaProviderRegistry.registerProvider(() -> EMPTY_PROVIDER);
    }

    public static StaminaProvider getProviderForBehavior(StaminaBarBehavior behavior) {
        return switch (behavior) {
            case FOOD -> foodProvider != null ? foodProvider : EMPTY_PROVIDER;
            case COMBAT_ATTRIBUTES -> combatAttributesProvider != null ? combatAttributesProvider : EMPTY_PROVIDER;
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
            case COMBAT_ATTRIBUTES -> combatAttributesProvider != null;
            case OFF -> true;
        };
    }

    public static boolean hasAnyStaminaMods() {
        return combatAttributesProvider != null;
    }

    public static void initialize() {
        foodProvider = new FoodStaminaProvider();
        // Optional: only instantiate when CA is present, so the CA classes never get touched without the mod.
        if (Services.PLATFORM.isModLoaded(COMBAT_ATTRIBUTES_MOD_ID)) {
            combatAttributesProvider = new CombatAttributesStaminaProvider();
        }
        updateActiveProvider();
    }

    public static void updateActiveProvider() {
        StaminaBarBehavior behavior = ModConfigManager.getClient().staminaBarBehavior;
        if (behavior == null) behavior = StaminaBarBehavior.FOOD;
        StaminaProviderRegistry.setActiveProvider(getProviderForBehavior(behavior));
    }
}
