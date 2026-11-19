package dev.muon.dynamic_resource_bars.client;

import dev.muon.dynamic_resource_bars.compat.WitcherySoulFormCompat;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import dev.muon.dynamic_resource_bars.render.AirBarRenderer;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import dev.muon.dynamic_resource_bars.render.CustomHudRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.ManaBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

    public static boolean renderHealth(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ClientConfig config = ModConfigManager.getClient();
        if (!config.enableMod) return false;
        BarRenderBehavior behavior = config.healthBarBehavior;
        if (behavior == BarRenderBehavior.VANILLA) return false;
        Player player = clientPlayer();
        if (player == null) return behavior == BarRenderBehavior.HIDDEN;
        if (WitcherySoulFormCompat.isSoulFormActive(player)) return true;
        if (behavior == BarRenderBehavior.CUSTOM) HealthBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
        return true;
    }

    public static boolean renderFood(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ClientConfig config = ModConfigManager.getClient();
        if (!config.enableMod || !StaminaBarBehavior.FOOD.equals(config.staminaBarBehavior)) return false;
        Player player = clientPlayer();
        if (player == null) return false;
        if (WitcherySoulFormCompat.shouldHideFood(player)) return true;
        StaminaBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
        return true;
    }

    /**
     * Three-state behavior: VANILLA → fall through, CUSTOM → render ours, HIDDEN → suppress
     * the vanilla layer and render nothing.
     */
    public static boolean renderArmor(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ClientConfig config = ModConfigManager.getClient();
        if (!config.enableMod) return false;
        BarRenderBehavior behavior = config.armorBarBehavior;
        if (behavior == BarRenderBehavior.VANILLA) return false;
        Player player = clientPlayer();
        if (player == null) return behavior == BarRenderBehavior.HIDDEN; // suppress vanilla even with no player
        if (WitcherySoulFormCompat.isSoulFormActive(player)) return true;
        if (behavior == BarRenderBehavior.CUSTOM) ArmorBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
        return true;
    }

    public static boolean renderAir(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ClientConfig config = ModConfigManager.getClient();
        if (!config.enableMod) return false;
        BarRenderBehavior behavior = config.airBarBehavior;
        if (behavior == BarRenderBehavior.VANILLA) return false;
        Player player = clientPlayer();
        if (player == null) return behavior == BarRenderBehavior.HIDDEN;
        if (behavior == BarRenderBehavior.CUSTOM) AirBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
        return true;
    }

    /** Free-standing mana render — no vanilla equivalent. Loader wirings register this above PLAYER_HEALTH. */
    public static void renderMana(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!ModConfigManager.getClient().enableMod) return;
        Player player = clientPlayer();
        if (player == null) return;
        ManaBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
    }

    /** Vehicle/mount health is suppressed when our stamina bar takes over the right side. */
    public static boolean suppressVehicleHealth() {
        ClientConfig config = ModConfigManager.getClient();
        return config.enableMod && config.mergeMountHealth && config.enableMountHealth;
    }

    /**
     * Renders the stamina bar from the mount-health slot when the player is mounted on a hearted
     * vehicle. The stamina renderer detects the mount internally and switches to mount HP.
     */
    public static boolean renderStaminaInMountSlot(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!suppressVehicleHealth()) return false;
        Player player = clientPlayer();
        if (player == null) return false;
        StaminaBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
        return true;
    }

    public static boolean renderStaminaForStaminaAttributes(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ClientConfig config = ModConfigManager.getClient();
        if (!config.enableMod || config.staminaBarBehavior != StaminaBarBehavior.STAMINA_ATTRIBUTES) return false;
        Player player = clientPlayer();
        if (player == null) return false;
        StaminaBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
        return true;
    }

    /**
     * Free-standing trigger for the Paragliders-sourced stamina bar (NeoForge-only).
     */
    public static void renderStaminaForParagliders(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ClientConfig config = ModConfigManager.getClient();
        if (!config.enableMod || config.staminaBarBehavior != StaminaBarBehavior.PARAGLIDERS) return;
        Player player = clientPlayer();
        if (player == null) return;
        StaminaBarRenderer.INSTANCE.render(graphics, player, deltaTracker);
    }

    public static boolean renderHotbar(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!CustomHudRenderer.shouldReplaceHotbar()) return false;
        CustomHudRenderer.renderHotbar(graphics, deltaTracker);
        return true;
    }

    public static boolean renderInfoBar(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!CustomHudRenderer.shouldReplaceInfoBar()) return false;
        CustomHudRenderer.renderInfoBar(graphics, deltaTracker);
        return true;
    }

    public static boolean renderExperienceLevel(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!CustomHudRenderer.shouldReplaceExperienceLevel()) return false;
        CustomHudRenderer.renderExperienceLevel(graphics, deltaTracker);
        return true;
    }

    public static void renderAllText(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ClientConfig config = ModConfigManager.getClient();
        if (!config.enableMod) {
            CustomHudRenderer.flushDeferredOffhandTasks();
            return;
        }
        if (CustomHudRenderer.shouldReplaceHotbar()) {
            CustomHudRenderer.renderOffhand(graphics, deltaTracker);
        } else {
            CustomHudRenderer.flushDeferredOffhandTasks();
        }
        Player player = clientPlayer();
        if (player == null) return;
        // Items render around Z=150..200 in 1.21.1, so elevate bar text above them.
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 250.0F);
        try {
            boolean soulForm = WitcherySoulFormCompat.isSoulFormActive(player);
            if (!soulForm && config.healthBarBehavior == BarRenderBehavior.CUSTOM) {
                HealthBarRenderer.INSTANCE.renderTextOverlay(graphics, player);
            }
            ManaBarRenderer.INSTANCE.renderTextOverlay(graphics, player);
            if (config.staminaBarBehavior != StaminaBarBehavior.OFF
                    && !(config.staminaBarBehavior == StaminaBarBehavior.FOOD && WitcherySoulFormCompat.shouldHideFood(player))) {
                StaminaBarRenderer.INSTANCE.renderTextOverlay(graphics, player);
            }
            if (!soulForm && config.armorBarBehavior == BarRenderBehavior.CUSTOM) {
                ArmorBarRenderer.INSTANCE.renderTextOverlay(graphics, player);
            }
            if (config.airBarBehavior == BarRenderBehavior.CUSTOM) {
                AirBarRenderer.INSTANCE.renderTextOverlay(graphics, player);
            }
        } finally {
            graphics.pose().popPose();
        }
    }
}
