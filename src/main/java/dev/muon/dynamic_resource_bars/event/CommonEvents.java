package dev.muon.dynamic_resource_bars.event;

#if NEWER_THAN_20_1
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.render.AirBarRenderer;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import fuzs.puzzleslib.api.client.core.v1.ClientAbstractions;
import fuzs.puzzleslib.api.event.v1.core.EventResult;
import net.minecraft.client.DeltaTracker;

// 1.21.1 only
public class CommonEvents {
    public static EventResult onRenderPlayerHealth(Minecraft minecraft, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        var config = ModConfigManager.getClient();
        if (!config.enableHealthBar) {
            return EventResult.PASS; // Let vanilla render
        }

        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return EventResult.PASS;
        }

        float absorptionAmount = player.getAbsorptionAmount();
        HealthBarRenderer.render(guiGraphics, player,
                player.getMaxHealth(), player.getHealth(),
                (int) absorptionAmount, deltaTracker );
        ClientAbstractions.INSTANCE.addGuiLeftHeight(minecraft.gui, config.healthBackgroundHeight + 1);
        return EventResult.INTERRUPT;
    }

    public static EventResult onRenderHunger(Minecraft minecraft, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        var config = ModConfigManager.getClient();
        if (!config.enableStaminaBar) {
            return EventResult.PASS;
        }

        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return EventResult.PASS;
        }

        StaminaBarRenderer.render(guiGraphics, player, deltaTracker );
        ClientAbstractions.INSTANCE.addGuiRightHeight(minecraft.gui, config.staminaBackgroundHeight + 1);

        return EventResult.INTERRUPT;
    }

    public static EventResult onRenderArmor(Minecraft minecraft, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        var config = ModConfigManager.getClient();
        BarRenderBehavior armorBehavior = config.armorBarBehavior;

        if (armorBehavior == BarRenderBehavior.VANILLA) {
            return EventResult.PASS;
        }

        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return EventResult.INTERRUPT;
        }

        if (armorBehavior == BarRenderBehavior.CUSTOM) {
            ArmorBarRenderer.render(guiGraphics, player);
            ClientAbstractions.INSTANCE.addGuiLeftHeight(minecraft.gui, config.armorBackgroundHeight + 1);
        }
        return EventResult.INTERRUPT;
    }

    public static EventResult onRenderAir(Minecraft minecraft, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        var config = ModConfigManager.getClient();
        BarRenderBehavior airBehavior = config.airBarBehavior;

        if (airBehavior == BarRenderBehavior.VANILLA) {
            return EventResult.PASS;
        }

        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return EventResult.INTERRUPT;
        }

        if (airBehavior == BarRenderBehavior.CUSTOM) {
            AirBarRenderer.render(guiGraphics, player);
            ClientAbstractions.INSTANCE.addGuiRightHeight(minecraft.gui, config.airBackgroundHeight + 1);
        }
        return EventResult.INTERRUPT;
    }

    public static EventResult onRenderMountHealth(Minecraft minecraft, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        var config = ModConfigManager.getClient();
        // Cancel mount health rendering if stamina bar is enabled (since it handles mount health)
        if (!config.enableStaminaBar) {
            return EventResult.PASS;
        }

        return EventResult.INTERRUPT;
    }
}
#endif