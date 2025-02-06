package dev.muon.dynamic_resource_bars.event;

#if NEWER_THAN_20_1
import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import fuzs.puzzleslib.api.event.v1.data.MutableInt;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import fuzs.puzzleslib.api.client.core.v1.ClientAbstractions;
import fuzs.puzzleslib.api.event.v1.core.EventResult;
import net.minecraft.client.DeltaTracker;

// 1.21 only
public class CommonEvents {
    public static EventResult onRenderPlayerHealth(Minecraft minecraft, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return EventResult.PASS;
        }

        float absorptionAmount = player.getAbsorptionAmount();

        HealthBarRenderer.render(guiGraphics, player,
                player.getMaxHealth(), player.getHealth(),
                (int) absorptionAmount, #if NEWER_THAN_20_1 deltaTracker #else partialTicks #endif );
        ClientAbstractions.INSTANCE.addGuiLeftHeight(minecraft.gui, AllConfigs.client().healthBackgroundHeight.get() + 1);
        return EventResult.INTERRUPT;
    }

    public static EventResult onRenderHunger(Minecraft minecraft, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return EventResult.PASS;
        }

        StaminaBarRenderer.render(guiGraphics, player, #if NEWER_THAN_20_1 deltaTracker #else partialTicks #endif );
        ClientAbstractions.INSTANCE.addGuiRightHeight(minecraft.gui, AllConfigs.client().staminaBackgroundHeight.get() + 1);

        return EventResult.INTERRUPT;
    }

    public static EventResult onRenderArmor(Minecraft minecraft, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return EventResult.PASS;
        }

        ArmorBarRenderer.render(guiGraphics, player);
        ClientAbstractions.INSTANCE.addGuiRightHeight(minecraft.gui, AllConfigs.client().armorBackgroundHeight.get() + 1);

        return EventResult.INTERRUPT;
    }
}
#endif