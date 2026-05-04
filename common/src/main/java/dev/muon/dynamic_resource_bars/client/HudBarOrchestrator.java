package dev.muon.dynamic_resource_bars.client;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import dev.muon.dynamic_resource_bars.render.AirBarRenderer;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.ManaBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

/**
 * Loader-agnostic decisions for whether to render our custom bar or defer to vanilla.
 * Each method returns {@code true} when our renderer drew the bar (vanilla should be skipped),
 * {@code false} when the caller should fall back to the vanilla layer/element.
 *
 * <p>Mana renders independently (no vanilla equivalent) and is invoked from the loader-side
 * HUD wiring as a free-standing layer rather than a vanilla-replacement.
 */
public final class HudBarOrchestrator {

    private HudBarOrchestrator() {}

    private static Player clientPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return null;
        return mc.player;
    }

    public static boolean renderHealth(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        BarRenderBehavior behavior = ModConfigManager.getClient().healthBarBehavior;
        if (behavior == BarRenderBehavior.VANILLA) return false;
        Player player = clientPlayer();
        if (player == null) return behavior == BarRenderBehavior.HIDDEN;
        if (behavior == BarRenderBehavior.CUSTOM) HealthBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
        return true;
    }

    public static boolean renderFood(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        ClientConfig config = ModConfigManager.getClient();
        if (!StaminaBarBehavior.FOOD.equals(config.staminaBarBehavior)) return false;
        Player player = clientPlayer();
        if (player == null) return false;
        StaminaBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
        return true;
    }

    /**
     * Three-state behavior: VANILLA → fall through, CUSTOM → render ours, HIDDEN → suppress
     * the vanilla layer and render nothing.
     */
    public static boolean renderArmor(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        BarRenderBehavior behavior = ModConfigManager.getClient().armorBarBehavior;
        if (behavior == BarRenderBehavior.VANILLA) return false;
        Player player = clientPlayer();
        if (player == null) return behavior == BarRenderBehavior.HIDDEN; // suppress vanilla even with no player
        if (behavior == BarRenderBehavior.CUSTOM) ArmorBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
        return true;
    }

    public static boolean renderAir(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        BarRenderBehavior behavior = ModConfigManager.getClient().airBarBehavior;
        if (behavior == BarRenderBehavior.VANILLA) return false;
        Player player = clientPlayer();
        if (player == null) return behavior == BarRenderBehavior.HIDDEN;
        if (behavior == BarRenderBehavior.CUSTOM) AirBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
        return true;
    }

    /** Free-standing mana render — no vanilla equivalent. Loader wirings register this above PLAYER_HEALTH. */
    public static void renderMana(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Player player = clientPlayer();
        if (player == null) return;
        ManaBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
    }

    /** Vehicle/mount health is suppressed when our stamina bar takes over the right side. */
    public static boolean suppressVehicleHealth() {
        ClientConfig config = ModConfigManager.getClient();
        return config.mergeMountHealth && config.enableMountHealth;
    }

    /**
     * Renders the stamina bar from the mount-health slot. Used by Fabric, where
     * {@code FOOD_BAR} is a virtual layer that doesn't fire when the player is mounted on a
     * hearted vehicle (Mojang skips the {@code extractFood} call). The stamina renderer detects
     * the mount internally and switches to mount HP — this just routes the render through the
     * layer that DOES fire while mounted. NeoForge doesn't need this hook because its
     * {@code FOOD_LEVEL} layer always fires.
     */
    public static boolean renderStaminaInMountSlot(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!suppressVehicleHealth()) return false;
        Player player = clientPlayer();
        if (player == null) return false;
        StaminaBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
        return true;
    }

    /**
     * Drives the render from Combat Attributes' stamina layer slot. When our stamina source is
     * CA, our DRB-styled bar takes over CA's slot (CA's own pip-bar render is replaced); otherwise
     * the caller falls through to CA's own render. The stamina-bar position itself comes from
     * {@code staminaBarAnchor} in config — this hook only decides which layer triggers the render.
     */
    public static boolean renderStaminaFromCASlot(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (ModConfigManager.getClient().staminaBarBehavior != StaminaBarBehavior.COMBAT_ATTRIBUTES) return false;
        Player player = clientPlayer();
        if (player == null) return false;
        StaminaBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
        return true;
    }

    /**
     * Free-standing trigger for the Paragliders-sourced stamina bar (NeoForge-only). Registered
     * via {@code event.registerAbove(PLAYER_HEALTH, ...)} like {@link #renderMana}, since wrapping
     * Paragliders' own {@code paraglider:stamina_wheel} layer fails on event-listener ordering
     * (Paragliders registers that layer in a default-priority listener that runs after ours).
     * The wheel itself is suppressed via the {@code @ParagliderPlugin} on the loader side.
     *
     * <p>Gates on {@link StaminaBarBehavior#PARAGLIDERS} so the renderer doesn't double-fire when
     * the user has FOOD or CA selected — those modes already trigger via the food-bar wrap and the
     * CA-bar wrap respectively.
     */
    public static void renderStaminaForParagliders(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (ModConfigManager.getClient().staminaBarBehavior != StaminaBarBehavior.PARAGLIDERS) return;
        Player player = clientPlayer();
        if (player == null) return;
        StaminaBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
    }
}
