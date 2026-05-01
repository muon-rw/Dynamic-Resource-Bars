package dev.muon.dynamic_resource_bars.client;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.StatusBarHeightProvider;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.FluidTags;

/**
 * Registers Fabric {@link HudStatusBarHeightRegistry} providers for our wrapped HUD layers.
 *
 * <p>Fabric's vanilla {@link net.minecraft.client.gui.Gui} hard-codes status bar y-coordinates
 * around {@code guiHeight - 39}; the height registry compensates by translating each layer's
 * pose by the cumulative difference between vanilla's expected height and the registered height
 * of every bar below it. So when our custom health bar is taller (or shorter) than the vanilla
 * heart row, we register that height here and Fabric pushes vanilla armor/air/etc. above us
 * automatically. (NeoForge solves the same problem with the {@code Gui.leftHeight}/{@code rightHeight}
 * counters that {@code NeoForgeHudWiring} bumps after each render.)
 *
 * <p>The registry freezes at {@code ClientLifecycleEvents.CLIENT_STARTED}, so {@link #install()}
 * must be invoked from {@code ClientModInitializer.onInitializeClient}, not later.
 */
public final class FabricStatusBarHeights {

    private FabricStatusBarHeights() {}

    public static void install() {
        HudStatusBarHeightRegistry.addLeft(VanillaHudElements.HEALTH_BAR, FabricStatusBarHeights::healthHeight);
        HudStatusBarHeightRegistry.addLeft(VanillaHudElements.ARMOR_BAR, FabricStatusBarHeights::armorHeight);
        HudStatusBarHeightRegistry.addRight(VanillaHudElements.FOOD_BAR, FabricStatusBarHeights::foodHeight);
        HudStatusBarHeightRegistry.addRight(VanillaHudElements.AIR_BAR, FabricStatusBarHeights::airHeight);
        // MOUNT_HEALTH stays vanilla — when our stamina is in mount-health mode we suppress its
        // render through HudBarOrchestrator.suppressVehicleHealth(), so the height contribution
        // already aligns with what's drawn.
    }

    private static int healthHeight(Player player) {
        ClientConfig c = ModConfigManager.getClient();
        return switch (c.healthBarBehavior) {
            case CUSTOM, HIDDEN -> c.healthBackgroundHeight;
            case VANILLA -> vanillaHealthRowHeight(player);
        };
    }

    private static int armorHeight(Player player) {
        ClientConfig c = ModConfigManager.getClient();
        return switch (c.armorBarBehavior) {
            case CUSTOM, HIDDEN -> c.armorBackgroundHeight;
            case VANILLA -> player.getArmorValue() > 0 ? 10 : 0;
        };
    }

    private static int foodHeight(Player player) {
        ClientConfig c = ModConfigManager.getClient();
        // Vanilla skips the food row when the player is on a vehicle with hearts; mirror that
        // by returning 0 so the air bar slots into the would-be food row.
        if (vehicleHasHearts(player)) return 0;
        if (c.staminaBarBehavior == StaminaBarBehavior.OFF) return 10; // vanilla food rendering
        return c.staminaBackgroundHeight;
    }

    private static int airHeight(Player player) {
        ClientConfig c = ModConfigManager.getClient();
        boolean wantsAir = wantsAirBar(player);
        return switch (c.airBarBehavior) {
            case CUSTOM, HIDDEN -> wantsAir ? c.airBackgroundHeight : 0;
            case VANILLA -> wantsAir ? 10 : 0;
        };
    }

    /** Replicates Mojang's heart-row height calculation so VANILLA mode keeps positioning vanilla armor identically to base game. */
    private static int vanillaHealthRowHeight(Player player) {
        float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), player.getHealth());
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        int rows = Mth.ceil((maxHealth + (float) absorption) / 2.0F / 10.0F);
        int rowShift = Math.max(10 - (rows - 2), 3);
        return 10 + (rows - 1) * rowShift;
    }

    private static boolean vehicleHasHearts(Player player) {
        return player.getVehicle() instanceof LivingEntity v && v.showVehicleHealth();
    }

    /** Mirrors Mojang's air-bar visibility predicate: shown when in water or below max air. */
    private static boolean wantsAirBar(Player player) {
        int maxAir = player.getMaxAirSupply();
        int air = Math.clamp((long) player.getAirSupply(), 0, maxAir);
        boolean inWater = player.isEyeInFluid(FluidTags.WATER);
        return inWater || air < maxAir;
    }
}
