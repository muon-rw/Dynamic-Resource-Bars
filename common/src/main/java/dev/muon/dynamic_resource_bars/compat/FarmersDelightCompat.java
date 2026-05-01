package dev.muon.dynamic_resource_bars.compat;

import net.minecraft.world.entity.player.Player;
import vectorwing.farmersdelight.common.registry.ModEffects;

/**
 * Farmer's Delight effect lookups (Refabricated edition for Fabric 26.1+).
 *
 * <p>Callers must guard with {@code Services.PLATFORM.isModLoaded("farmersdelight")} —
 * this class statically references FD's effect registry, so it must only load when the mod
 * is present. The dep is {@code compileOnly} in common/build.gradle (Fabric jar). When DRB
 * runs on NeoForge without FD installed, the {@code isModLoaded} guard skips load entirely.
 */
public final class FarmersDelightCompat {

    public static final String MOD_ID = "farmersdelight";

    private FarmersDelightCompat() {}

    /** Player has the {@code farmersdelight:nourishment} effect — drives the stamina_bar_nourished texture variant. */
    public static boolean hasNourishment(Player player) {
        return player.hasEffect(ModEffects.NOURISHMENT);
    }

    /** Player has the {@code farmersdelight:comfort} effect — drives the comfort overlay on the health bar background. */
    public static boolean hasComfort(Player player) {
        return player.hasEffect(ModEffects.COMFORT);
    }
}
