package dev.muon.dynamic_resource_bars.client;

import com.mojang.logging.LogUtils;
import dev.muon.combat_attributes.client.HudBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.ManaBarBehavior;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.function.UnaryOperator;

/**
 * Replaces Combat Attributes' stamina + mana HUD elements so that, in CA mode, our DRB-styled
 * stamina bar takes over CA's slot. Other modes pass through to CA's own pip render.
 *
 * <p>Without this hook, our stamina renderer never fires in CA mode — the {@code FOOD_BAR}
 * wrap only runs for {@code StaminaBarBehavior.FOOD}. Routing through
 * {@link HudBarOrchestrator#renderStaminaFromCASlot} keeps the loader-side branching identical
 * between Fabric and NeoForge.
 *
 * <p>References {@code dev.muon.combat_attributes.client.HudBars} statically; the caller
 * must guard {@link #install()} with a {@code Services.PLATFORM.isModLoaded("combat_attributes")}
 * check so the JVM never resolves CA classes when the mod is absent.
 *
 * <p>{@code replaceElement} throws if the target layer hasn't been registered yet. Mod
 * init order isn't guaranteed across Fabric entrypoints, and CA versions don't always ship
 * the same set of layer IDs — wrap each replace in {@link #safeReplace} so a missing layer
 * just logs a warning instead of crashing the client.
 */
public final class CombatAttributesSuppressionFabric {

    private static final Logger LOGGER = LogUtils.getLogger();

    private CombatAttributesSuppressionFabric() {}

    public static void install() {
        safeReplace(HudBars.STAMINA_ELEMENT, vanilla ->
                (graphics, deltaTracker) -> {
                    if (HudBarOrchestrator.renderStaminaFromCASlot(graphics, deltaTracker)) return;
                    vanilla.extractRenderState(graphics, deltaTracker);
                });
        safeReplace(HudBars.MANA_ELEMENT, vanilla ->
                (graphics, deltaTracker) -> {
                    if (ModConfigManager.getClient().manaBarBehavior == ManaBarBehavior.COMBAT_ATTRIBUTES) return;
                    vanilla.extractRenderState(graphics, deltaTracker);
                });
    }

    private static void safeReplace(Identifier id, UnaryOperator<HudElement> replacer) {
        try {
            HudElementRegistry.replaceElement(id, replacer);
        } catch (IllegalArgumentException missing) {
            LOGGER.warn("Skipping CA HUD suppression for {}: {}", id, missing.getMessage());
        }
    }
}
