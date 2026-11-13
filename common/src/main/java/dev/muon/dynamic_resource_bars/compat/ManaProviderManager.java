package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.provider.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.provider.ManaProvider;
import dev.muon.dynamic_resource_bars.provider.ManaProviderRegistry;
import net.minecraft.client.Minecraft;

/**
 * Mana provider registry. Loader-specific providers (Iron's Spellbooks, Ars Nouveau on NeoForge;
 * Mana Attributes on Fabric) are installed from each loader's mod entry behind {@code isModLoaded}
 * guards so common code never references loader-exclusive mod classes.
 */
public class ManaProviderManager {

    public static final String IRONS_SPELLBOOKS_MOD_ID = "irons_spellbooks";
    public static final String ARS_NOUVEAU_MOD_ID = "ars_nouveau";
    public static final String RPG_MANA_MOD_ID = "rpgmana";
    public static final String MANA_ATTRIBUTES_MOD_ID = "manaattributes";

    private static ManaProvider ironsProvider;
    private static ManaProvider arsProvider;
    private static ManaProvider rpgManaProvider;
    private static ManaProvider manaAttributesProvider;

    private static final ManaProvider EMPTY_PROVIDER = new ManaProvider() {
        @Override public double getCurrentMana() { return 0; }
        @Override public float getMaxMana() { return 0; }
        @Override public float getReservedMana() { return 0; }
        @Override public long getGameTime() {
            return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        }
    };

    public static void setIronsProvider(ManaProvider provider) {
        ironsProvider = provider;
    }

    public static void setArsProvider(ManaProvider provider) {
        arsProvider = provider;
    }

    public static void setManaAttributesProvider(ManaProvider provider) {
        manaAttributesProvider = provider;
    }

    public static void init() {
        ManaProviderRegistry.clear();
        ManaProviderRegistry.registerProvider(() -> EMPTY_PROVIDER);
        if (ironsProvider != null) ManaProviderRegistry.registerProvider(() -> ironsProvider);
        if (arsProvider != null) ManaProviderRegistry.registerProvider(() -> arsProvider);
        if (rpgManaProvider != null) ManaProviderRegistry.registerProvider(() -> rpgManaProvider);
        if (manaAttributesProvider != null) ManaProviderRegistry.registerProvider(() -> manaAttributesProvider);
    }

    public static ManaProvider getProviderForBehavior(ManaBarBehavior behavior) {
        return switch (behavior) {
            case IRONS_SPELLBOOKS -> ironsProvider != null ? ironsProvider : EMPTY_PROVIDER;
            case ARS_NOUVEAU -> arsProvider != null ? arsProvider : EMPTY_PROVIDER;
            case RPG_MANA -> rpgManaProvider != null ? rpgManaProvider : EMPTY_PROVIDER;
            case MANA_ATTRIBUTES -> manaAttributesProvider != null ? manaAttributesProvider : EMPTY_PROVIDER;
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
            case IRONS_SPELLBOOKS -> ironsProvider != null;
            case ARS_NOUVEAU -> arsProvider != null;
            case RPG_MANA -> rpgManaProvider != null;
            case MANA_ATTRIBUTES -> manaAttributesProvider != null;
            case OFF -> true;
        };
    }

    public static boolean hasAnyManaMods() {
        return ironsProvider != null
                || arsProvider != null
                || rpgManaProvider != null
                || manaAttributesProvider != null;
    }

    public static void initialize() {
        if (Services.PLATFORM.isModLoaded(RPG_MANA_MOD_ID)) {
            rpgManaProvider = new RPGManaManaProvider();
        }
        updateActiveProvider();
    }

    public static void updateActiveProvider() {
        ManaBarBehavior behavior = ModConfigManager.getClient().manaBarBehavior;
        if (behavior == null) behavior = ManaBarBehavior.OFF;
        ManaProviderRegistry.setActiveProvider(getProviderForBehavior(behavior));
    }
}
