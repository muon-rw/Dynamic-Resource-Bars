package dev.muon.dynamic_resource_bars.client;

import com.mojang.logging.LogUtils;
import dev.muon.combat_attributes.client.HudBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.ManaBarBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.slf4j.Logger;

import java.util.function.UnaryOperator;

/**
 * Wraps Combat Attributes' stamina + mana GUI layers so that, when our bars source those
 * values from CA, our DRB-styled bar takes over CA's slot. Otherwise, the wrap passes through
 * to CA's own render so CA's pip bar still appears for non-CA modes.
 *
 * <p>Without this hook our stamina renderer never fires in CA mode — {@code FOOD_LEVEL} only
 * triggers our render for {@code StaminaBarBehavior.FOOD}, leaving the bar invisible if CA's
 * own layer is also suppressed. Routing through {@link HudBarOrchestrator#renderStaminaFromCASlot}
 * keeps the loader-side branching identical between Fabric and NeoForge.
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
            if (HudBarOrchestrator.renderStaminaFromCASlot(graphics, deltaTracker)) {
                // CA's original layer bumps rightHeight unconditionally when it draws — mirror that
                // so vanilla layers above us (air, etc.) don't slide down when DRB takes over.
                Minecraft.getInstance().gui.rightHeight += HudBars.BAR_HEIGHT;
                return;
            }
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
