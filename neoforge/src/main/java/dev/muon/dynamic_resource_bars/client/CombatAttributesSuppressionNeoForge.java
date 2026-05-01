package dev.muon.dynamic_resource_bars.client;

import com.mojang.logging.LogUtils;
import dev.muon.combat_attributes.client.HudBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.slf4j.Logger;

import java.util.function.UnaryOperator;

/**
 * Wraps Combat Attributes' stamina + mana GUI layers so that, when our bars are sourcing
 * those values from CA, CA's own bars are suppressed. The {@link RegisterGuiLayersEvent#wrapLayer}
 * call gives us the original CA layer so we can still pass through to it when the user has
 * configured a different source for our bar (otherwise CA's bars would disappear entirely).
 *
 * <p>References {@code dev.muon.combat_attributes.client.HudBars}; the caller must guard
 * {@link #install(RegisterGuiLayersEvent)} with
 * {@code Services.PLATFORM.isModLoaded("combat_attributes")}. Each {@code wrapLayer} call is
 * wrapped in {@link #safeWrap} so a missing CA layer (different version, renamed ID) just logs
 * a warning instead of crashing the client.
 */
public final class CombatAttributesSuppressionNeoForge {

    private static final Logger LOGGER = LogUtils.getLogger();

    private CombatAttributesSuppressionNeoForge() {}

    public static void install(RegisterGuiLayersEvent event) {
        safeWrap(event, HudBars.STAMINA_ELEMENT, vanilla -> (graphics, deltaTracker) -> {
            if (ModConfigManager.getClient().staminaBarBehavior == StaminaBarBehavior.COMBAT_ATTRIBUTES) return;
            vanilla.render(graphics, deltaTracker);
        });
        safeWrap(event, HudBars.MANA_ELEMENT, vanilla -> (graphics, deltaTracker) -> {
            if (ModConfigManager.getClient().manaBarBehavior == ManaBarBehavior.COMBAT_ATTRIBUTES) return;
            vanilla.render(graphics, deltaTracker);
        });
    }

    private static void safeWrap(RegisterGuiLayersEvent event, Identifier id, UnaryOperator<GuiLayer> wrapper) {
        try {
            event.wrapLayer(id, wrapper);
        } catch (IllegalArgumentException missing) {
            LOGGER.warn("Skipping CA GUI suppression for {}: {}", id, missing.getMessage());
        }
    }
}
