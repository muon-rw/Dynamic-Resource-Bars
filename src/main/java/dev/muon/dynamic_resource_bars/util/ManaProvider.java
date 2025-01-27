package dev.muon.dynamic_resource_bars.util;

public interface ManaProvider {
    double getCurrentMana();
    float getMaxMana();
    float getReservedMana(); // For use by Ars Nouveau, not sure if other mana systems have similar concepts
    long getGameTime();  // For animation timing
}