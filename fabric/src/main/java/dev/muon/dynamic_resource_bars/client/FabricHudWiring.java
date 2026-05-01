package dev.muon.dynamic_resource_bars.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/**
 * Wires {@link HudBarOrchestrator} into Fabric's HUD element registry.
 *
 * <p>For each vanilla bar we replace the registered element with a wrapper that asks the
 * orchestrator first; if the orchestrator declines (i.e. the feature is disabled in config),
 * we fall back to the original vanilla element.
 */
public final class FabricHudWiring {

    private FabricHudWiring() {}

    public static void register() {
        HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, vanilla -> ours(vanilla, HudBarOrchestrator::renderHealth));
        HudElementRegistry.replaceElement(VanillaHudElements.FOOD_BAR, vanilla -> ours(vanilla, HudBarOrchestrator::renderFood));
        HudElementRegistry.replaceElement(VanillaHudElements.ARMOR_BAR, vanilla -> ours(vanilla, HudBarOrchestrator::renderArmor));
        HudElementRegistry.replaceElement(VanillaHudElements.AIR_BAR, vanilla -> ours(vanilla, HudBarOrchestrator::renderAir));
        HudElementRegistry.replaceElement(VanillaHudElements.MOUNT_HEALTH, vanilla ->
                (graphics, deltaTracker) -> {
                    // Fabric's FOOD_BAR virtual layer doesn't fire while the player is mounted on
                    // a hearted vehicle, so we ALSO drive the stamina render from MOUNT_HEALTH —
                    // the renderer detects the mount internally and shows mount HP.
                    if (HudBarOrchestrator.renderStaminaInMountSlot(graphics, deltaTracker)) return;
                    vanilla.extractRenderState(graphics, deltaTracker);
                });
        // Mana renders as a free-standing element since vanilla has no mana slot.
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HEALTH_BAR,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                        dev.muon.dynamic_resource_bars.Constants.MOD_ID, "mana_bar"),
                HudBarOrchestrator::renderMana);

        // CA suppression is deferred to ClientLifecycleEvents.CLIENT_STARTED — see
        // DynamicResourceBars.onInitializeClient. CA registers its layers in its own
        // onInitializeClient and entrypoint order is not guaranteed across mods, so calling
        // replaceElement here would race the registration and crash if we run first.
    }

    private static HudElement ours(HudElement vanilla, OrchestratorCall call) {
        return (graphics, deltaTracker) -> {
            if (!call.run(graphics, deltaTracker)) {
                vanilla.extractRenderState(graphics, deltaTracker);
            }
        };
    }

    @FunctionalInterface
    private interface OrchestratorCall {
        boolean run(net.minecraft.client.gui.GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker);
    }
}
