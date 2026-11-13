package dev.muon.dynamic_resource_bars.compat;

import com.github.theredbrain.manaattributes.entity.ManaUsingEntity;
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.provider.ManaProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.internals.casting.SpellCaster;

public class ManaAttributesManaProvider implements ManaProvider {

    @Override
    public double getCurrentMana() {
        Player player = Minecraft.getInstance().player;
        if (player instanceof ManaUsingEntity manaUsingEntity) {
            return manaUsingEntity.manaattributes$getMana();
        }
        return 0;
    }

    @Override
    public float getMaxMana() {
        Player player = Minecraft.getInstance().player;
        if (player instanceof ManaUsingEntity manaUsingEntity) {
            return manaUsingEntity.manaattributes$getMaxMana();
        }
        return 0;
    }

    @Override
    public float getReservedMana() {
        Player player = Minecraft.getInstance().player;
        if (player instanceof ManaUsingEntity manaUsingEntity) {
            // Mana Attributes exposes reserved mana as a percentage.
            return manaUsingEntity.manaattributes$getReservedMana() / 100f;
        }
        return 0;
    }

    @Override
    public long getGameTime() {
        if (Minecraft.getInstance().level != null) {
            return Minecraft.getInstance().level.getGameTime();
        }
        return 0;
    }

    @Override
    public boolean forceShowBarConditions(Player player) {
        return Services.PLATFORM.isModLoaded("spell_engine")
                && player instanceof SpellCaster.Client spellCaster
                && spellCaster.isCastingSpell();
    }
}
