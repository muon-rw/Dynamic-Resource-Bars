package dev.muon.dynamic_resource_bars.client;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.function.IntSupplier;
import java.util.function.ObjIntConsumer;

/**
 * Wires {@link HudBarOrchestrator} into NeoForge's GUI layer registry.
 *
 * <h3>Vanilla layer fallback ({@code wrapLayer})</h3>
 * Each vanilla bar is wrapped via {@link RegisterGuiLayersEvent#wrapLayer}, which gives us the
 * pre-existing layer so the orchestrator can fall back to vanilla rendering for {@code VANILLA}
 * mode without us having to re-implement Mojang's bar pixel pushing.
 *
 * <h3>Why we mutate {@code Gui.leftHeight} / {@code rightHeight}</h3>
 * NeoForge patches Mojang's {@link Gui} with two running counters that subsequent vanilla layers
 * read to position themselves above (or below) the stack — armor at {@code H - leftHeight + 10},
 * food at {@code H - rightHeight}, and so on. Each vanilla bar increments the counter by ~10.
 * If our wrap renders a custom bar without bumping the counter, the next vanilla layer will draw
 * over us (e.g. vanilla armor sliding down into our health row). When the orchestrator returns
 * true (we either rendered or hid the bar) we add our background height to the counter; when it
 * returns false the vanilla layer draws and bumps the counter itself.
 */
public final class NeoForgeHudWiring {

    private NeoForgeHudWiring() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.wrapLayer(VanillaGuiLayers.PLAYER_HEALTH, vanilla -> orchestratorOrFallback(
                vanilla, HudBarOrchestrator::renderHealth, () -> ModConfigManager.getClient().healthBackgroundHeight, BUMP_LEFT));
        event.wrapLayer(VanillaGuiLayers.FOOD_LEVEL, vanilla -> orchestratorOrFallback(
                vanilla, HudBarOrchestrator::renderFood, () -> ModConfigManager.getClient().staminaBackgroundHeight, BUMP_RIGHT));
        event.wrapLayer(VanillaGuiLayers.ARMOR_LEVEL, vanilla -> orchestratorOrFallback(
                vanilla, HudBarOrchestrator::renderArmor, () -> ModConfigManager.getClient().armorBackgroundHeight, BUMP_LEFT));
        event.wrapLayer(VanillaGuiLayers.AIR_LEVEL, vanilla -> orchestratorOrFallback(
                vanilla, HudBarOrchestrator::renderAir, () -> ModConfigManager.getClient().airBackgroundHeight, BUMP_RIGHT));
        event.wrapLayer(VanillaGuiLayers.VEHICLE_HEALTH, vanilla -> (graphics, deltaTracker) -> {
            // When stamina is sourcing mount health, suppress vanilla's mount-health row.
            if (HudBarOrchestrator.suppressVehicleHealth()) return;
            vanilla.render(graphics, deltaTracker);
        });
        // Mana is free-standing (no vanilla equivalent) so it doesn't participate in the
        // height-counter dance — renderAbove just stacks it above health in the layer order.
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                        dev.muon.dynamic_resource_bars.Constants.MOD_ID, "mana_bar"),
                HudBarOrchestrator::renderMana);

        if (dev.muon.dynamic_resource_bars.platform.Services.PLATFORM.isModLoaded("combat_attributes")) {
            CombatAttributesSuppressionNeoForge.install(event);
        }
    }

    private static GuiLayer orchestratorOrFallback(GuiLayer vanilla, OrchestratorCall call,
                                                   IntSupplier bumpAmount, ObjIntConsumer<Gui> bumpFn) {
        return (graphics, deltaTracker) -> {
            if (call.run(graphics, deltaTracker)) {
                bumpFn.accept(Minecraft.getInstance().gui, bumpAmount.getAsInt());
            } else {
                vanilla.render(graphics, deltaTracker);
            }
        };
    }

    /** Adds to the left-side height counter (health, armor). */
    private static final ObjIntConsumer<Gui> BUMP_LEFT = (gui, n) -> gui.leftHeight += n;
    /** Adds to the right-side height counter (food, air, vehicle health). */
    private static final ObjIntConsumer<Gui> BUMP_RIGHT = (gui, n) -> gui.rightHeight += n;

    @FunctionalInterface
    private interface OrchestratorCall {
        boolean run(net.minecraft.client.gui.GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker);
    }
}
