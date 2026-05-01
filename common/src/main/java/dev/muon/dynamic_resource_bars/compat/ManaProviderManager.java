package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.provider.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.provider.ManaProvider;
import dev.muon.dynamic_resource_bars.provider.ManaProviderRegistry;
import net.minecraft.client.Minecraft;

/**
 * Mana provider registry. The Combat Attributes provider is wired in only when
 * {@code combat_attributes} is on the runtime classpath, so the dependency stays optional.
 */
public class ManaProviderManager {

    public static final String COMBAT_ATTRIBUTES_MOD_ID = "combat_attributes";

    private static ManaProvider combatAttributesProvider;

    private static final ManaProvider EMPTY_PROVIDER = new ManaProvider() {
        @Override public double getCurrentMana() { return 0; }
        @Override public float getMaxMana() { return 0; }
        @Override public float getReservedMana() { return 0; }
        @Override public long getGameTime() {
            return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        }
    };

    public static void init() {
        ManaProviderRegistry.clear();
        ManaProviderRegistry.registerProvider(() -> EMPTY_PROVIDER);
        if (combatAttributesProvider != null) {
            ManaProviderRegistry.registerProvider(() -> combatAttributesProvider);
        }
    }

    public static ManaProvider getProviderForBehavior(ManaBarBehavior behavior) {
        return switch (behavior) {
            case COMBAT_ATTRIBUTES -> combatAttributesProvider != null ? combatAttributesProvider : EMPTY_PROVIDER;
            case OFF -> EMPTY_PROVIDER;
        };
    }

    public static ManaProvider getCurrentProvider() {
        return getProviderForBehavior(ModConfigManager.getClient().manaBarBehavior);
    }

    public static boolean isModLoaded(String modId) {
        return Services.PLATFORM.isModLoaded(modId);
    }

    public static boolean isModLoaded(ManaBarBehavior behavior) {
        return switch (behavior) {
            case COMBAT_ATTRIBUTES -> combatAttributesProvider != null;
            case OFF -> true;
        };
    }

    public static boolean hasAnyManaMods() {
        return combatAttributesProvider != null;
    }

    public static void initialize() {
        if (Services.PLATFORM.isModLoaded(COMBAT_ATTRIBUTES_MOD_ID)) {
            combatAttributesProvider = new CombatAttributesManaProvider();
        }
        updateActiveProvider();
    }

    public static void updateActiveProvider() {
        ManaBarBehavior behavior = ModConfigManager.getClient().manaBarBehavior;
        if (behavior == null) behavior = ManaBarBehavior.OFF;
        ManaProviderRegistry.setActiveProvider(getProviderForBehavior(behavior));
    }
}
