package dev.muon.dynamic_resource_bars.client;

import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.compat.StaminaProviderManager;
import dev.muon.dynamic_resource_bars.compat.VampireBloodCompat;
import dev.muon.dynamic_resource_bars.compat.WitcheryCompatNeoForge;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Wires {@link HudBarOrchestrator} into NeoForge's GUI layer registry.
 *
 * <p>Resource bars are registered as separate layers above their vanilla counterparts while
 * {@link NeoForgeCompatSuppression} cancels the vanilla layer at {@code HIGH} priority when DRB
 * owns it, preventing third-party {@code RenderGuiLayerEvent.Pre} replacers (e.g. OverflowingBars)
 * from swallowing our bars.
 *
 * <p>Optional offsets to {@code Gui.leftHeight} / {@code rightHeight} are applied via
 * {@link ClientConfig#leftHudLayerOffset} and {@link ClientConfig#rightHudLayerOffset}.
 */
public final class NeoForgeHudWiring {

    private static final ResourceLocation HUD_LAYER_OFFSETS = Constants.loc("hud_layer_offsets");
    private static final ResourceLocation HEALTH_BAR = Constants.loc("health_bar");
    private static final ResourceLocation STAMINA_BAR = Constants.loc("stamina_bar");
    private static final ResourceLocation ARMOR_BAR = Constants.loc("armor_bar");
    private static final ResourceLocation AIR_BAR = Constants.loc("air_bar");
    private static final ResourceLocation MANA_BAR = Constants.loc("mana_bar");
    private static final ResourceLocation STAMINA_PARAGLIDERS = Constants.loc("stamina_paragliders");
    private static final ResourceLocation BAR_TEXT = Constants.loc("bar_text");
    private static final ResourceLocation WITCHERY_ABILITY_HUD = Constants.loc("witchery_ability_hud");

    private NeoForgeHudWiring() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.wrapLayer(VanillaGuiLayers.HOTBAR, vanilla -> (graphics, deltaTracker) -> {
            if (!HudBarOrchestrator.renderHotbar(graphics, deltaTracker)) {
                vanilla.render(graphics, deltaTracker);
            }
        });
        event.wrapLayer(VanillaGuiLayers.EXPERIENCE_BAR, vanilla -> (graphics, deltaTracker) -> {
            if (!HudBarOrchestrator.renderInfoBar(graphics, deltaTracker)) {
                vanilla.render(graphics, deltaTracker);
            }
        });
        event.wrapLayer(VanillaGuiLayers.JUMP_METER, vanilla -> (graphics, deltaTracker) -> {
            if (!HudBarOrchestrator.renderInfoBar(graphics, deltaTracker)) {
                vanilla.render(graphics, deltaTracker);
            }
        });
        event.wrapLayer(VanillaGuiLayers.EXPERIENCE_LEVEL, vanilla -> (graphics, deltaTracker) -> {
            if (!HudBarOrchestrator.renderExperienceLevel(graphics, deltaTracker)) {
                vanilla.render(graphics, deltaTracker);
            }
        });

        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, HUD_LAYER_OFFSETS, (graphics, deltaTracker) -> {
            ClientConfig config = ModConfigManager.getClient();
            Gui gui = Minecraft.getInstance().gui;
            gui.leftHeight += config.leftHudLayerOffset;
            gui.rightHeight += config.rightHudLayerOffset;
        });

        registerBar(event, VanillaGuiLayers.PLAYER_HEALTH, HEALTH_BAR, HudBarOrchestrator::renderHealth);
        registerBar(event, VanillaGuiLayers.FOOD_LEVEL, STAMINA_BAR, HudBarOrchestrator::renderFood);
        registerBar(event, VanillaGuiLayers.ARMOR_LEVEL, ARMOR_BAR, HudBarOrchestrator::renderArmor);
        registerBar(event, VanillaGuiLayers.AIR_LEVEL, AIR_BAR, HudBarOrchestrator::renderAir);

        // Mana is free-standing (no vanilla equivalent); register above HEALTH_BAR so it draws on top of health.
        event.registerAbove(HEALTH_BAR, MANA_BAR, HudBarOrchestrator::renderMana);

        // Paragliders source: free-standing trigger above HEALTH_BAR (mirrors mana). Paragliders'
        // own stamina wheel is suppressed via DynamicResourceBarsParagliderPlugin (StaminaPlugin
        // API), not via wrapLayer — wrapping their layer hit ordering issues that left the bar
        // invisible AND the wheel still showing.
        if (Services.PLATFORM.isModLoaded(StaminaProviderManager.PARAGLIDERS_MOD_ID)) {
            event.registerAbove(HEALTH_BAR, STAMINA_PARAGLIDERS, HudBarOrchestrator::renderStaminaForParagliders);
        }

        event.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, BAR_TEXT, HudBarOrchestrator::renderAllText);

        if (Services.PLATFORM.isModLoaded(VampireBloodCompat.WITCHERY_MOD_ID)) {
            event.registerAbove(BAR_TEXT, WITCHERY_ABILITY_HUD, WitcheryCompatNeoForge::renderAbilityHud);
        }
    }

    private static void registerBar(RegisterGuiLayersEvent event, ResourceLocation anchor, ResourceLocation id,
                                    OrchestratorCall call) {
        event.registerAbove(anchor, id, call::run);
    }

    @FunctionalInterface
    private interface OrchestratorCall {
        boolean run(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker);
    }
}
