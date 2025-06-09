package dev.muon.dynamic_resource_bars.util;

import net.minecraft.world.entity.player.Player;

public interface ManaProvider {
    double getCurrentMana();
    float getMaxMana();
    float getReservedMana(); // For use by Ars Nouveau, not sure if other mana systems have similar concepts
    long getGameTime();  // For animation timing

    default boolean shouldDisplayBarOverride(Player player) {
        return true; // Default to true, meaning no override by default
    }

    default boolean hasSpecificVisibilityLogic() {
        return false; // Default to false, assuming generic provider
    }

    default boolean forceShowBarConditions(Player player) {
        return false; // Default to false, no additive conditions to force show
    }
}