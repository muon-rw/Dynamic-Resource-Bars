package dev.muon.dynamic_resource_bars.compat;

import com.github.thedeathlycow.thermoo.api.temperature.Soakable;
import com.github.thedeathlycow.thermoo.api.temperature.TemperatureAware;
import net.minecraft.world.entity.player.Player;

/**
 * Thin wrapper around Thermoo's {@code TemperatureAware} and {@code Soakable} interfaces,
 * which Thermoo injects onto every {@link net.minecraft.world.entity.LivingEntity} via mixin.
 *
 * <p>This class statically references Thermoo's API classes — call sites MUST gate access
 * with {@code Services.PLATFORM.isModLoaded("thermoo")} so the JVM never resolves these
 * symbols when Thermoo is absent. As long as no one outside that guard calls into here, the
 * optional-dependency story holds: this class is only loaded on first invocation.
 */
public final class ThermooCompat {

    public static final String MOD_ID = "thermoo";

    private ThermooCompat() {}

    /** Range [-1.0, 1.0]. Negative = cold, positive = hot. 0 when neutral. */
    public static float getTemperatureScale(Player player) {
        return ((TemperatureAware) player).thermoo$getTemperatureScale();
    }

    public static void setTemperatureScale(Player player, float scale) {
        TemperatureAware aware = (TemperatureAware) player;
        if (scale < 0f) {
            aware.thermoo$setTemperature(Math.round(-scale * aware.thermoo$getMinTemperature()));
        } else if (scale > 0f) {
            aware.thermoo$setTemperature(Math.round(scale * aware.thermoo$getMaxTemperature()));
        } else {
            aware.thermoo$setTemperature(0);
        }
    }

    /** Range [0.0, 1.0]. 1.0 when fully soaked. */
    public static float getSoakedScale(Player player) {
        return ((Soakable) player).thermoo$getSoakedScale();
    }

    /** True when the player is at or near max heat — used to swap the health bar to its scorched variant. */
    public static boolean isScorched(Player player) {
        TemperatureAware aware = (TemperatureAware) player;
        int temp = aware.thermoo$getTemperature();
        int max = aware.thermoo$getMaxTemperature();
        return temp > 0 && temp >= max - 1;
    }

    /**
     * True when the player is near max cold. Falls back to vanilla {@code isFullyFrozen} when the
     * Thermoo cold reading is non-negative (e.g. the player is hot/neutral but powder-snow-frozen).
     */
    public static boolean isFrozen(Player player) {
        if (player.isFullyFrozen()) return true;
        TemperatureAware aware = (TemperatureAware) player;
        if (aware.thermoo$getTemperature() < 0) {
            return aware.thermoo$getTemperatureScale() <= -0.99f;
        }
        return false;
    }
}
