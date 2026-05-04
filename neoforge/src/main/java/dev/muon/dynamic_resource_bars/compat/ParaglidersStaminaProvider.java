package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.provider.StaminaProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import tictim.paraglider.api.stamina.Stamina;

/**
 * Stamina source backed by Paragliders' {@link Stamina} API.
 *
 * <p>Paragliders is NeoForge-only on this MC version, so this provider lives in the
 * neoforge module and is installed from {@code DynamicResourceBars} behind an
 * {@code isModLoaded("paraglider")} guard. As long as the common code never references
 * this class statically, the JVM never resolves the Paragliders classes when the mod is
 * absent.
 *
 * <p>Paragliders stores stamina in units of {@link Stamina#STAMINA_PER_WHEEL} (1000 per
 * wheel). The values are doubles in the thousands, but the bar renderer only cares about
 * the current/max ratio, so the raw values are fine.
 *
 * <p>{@link Stamina#extraStamina()} (BotW-style temporary overshield) is exposed via
 * {@link #getExtraStamina(Player)} so the renderer can squeeze the live fill into
 * {@code current / (max + extra)} and paint the trailing slice with the absorption-bar
 * texture — same treatment the health bar gives vanilla absorption in SQUEEZE mode.
 */
public class ParaglidersStaminaProvider implements StaminaProvider {

    @Override
    public float getCurrentStamina(Player player) {
        return (float) Stamina.get(player).stamina();
    }

    @Override
    public float getMaxStamina(Player player) {
        return (float) Stamina.get(player).maxStamina();
    }

    @Override
    public float getExtraStamina(Player player) {
        return (float) Stamina.get(player).extraStamina();
    }

    @Override
    public long getGameTime() {
        if (Minecraft.getInstance().level != null) {
            return Minecraft.getInstance().level.getGameTime();
        }
        return 0;
    }
}
