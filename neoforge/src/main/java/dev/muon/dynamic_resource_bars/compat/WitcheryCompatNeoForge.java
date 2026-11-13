package dev.muon.dynamic_resource_bars.compat;

import dev.sterner.witchery.feature.ability.AbilityHudRenderer;
import dev.sterner.witchery.feature.ability.AbilityPlayerAttachment;
import dev.sterner.witchery.feature.affliction.vampire.VampirePlayerAttachment;
import dev.sterner.witchery.feature.blood_pool.BloodPoolAttachment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public final class WitcheryCompatNeoForge implements VampireBloodCompat.Provider, WitcheryAbilityCompat.Provider {

    private static boolean hoistedRenderActive;

    public static boolean isHoistedRenderActive() {
        return hoistedRenderActive;
    }

    public static void renderAbilityHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        hoistedRenderActive = true;
        try {
            AbilityHudRenderer.renderHud(graphics);
        } finally {
            hoistedRenderActive = false;
        }
    }

    @Override
    public boolean isVampire(Player player) {
        return VampirePlayerAttachment.getData(player).isVampire();
    }

    @Override
    public float getCurrentBlood(Player player) {
        return BloodPoolAttachment.getData(player).getBloodPool();
    }

    @Override
    public float getMaxBlood(Player player) {
        return BloodPoolAttachment.getData(player).getMaxBlood();
    }

    @Override
    public boolean hasSelectedAbility(Player player) {
        return AbilityPlayerAttachment.INSTANCE.getAbilityIndex(player) != -1;
    }
}
