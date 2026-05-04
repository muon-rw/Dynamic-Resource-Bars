package dev.muon.dynamic_resource_bars.provider;

import net.minecraft.world.entity.player.Player;

public interface StaminaProvider {
    /**
     * @return Current stamina value
     */
    float getCurrentStamina(Player player);
    
    /**
     * @return Maximum stamina value
     */
    float getMaxStamina(Player player);

    /**
     * Bonus stamina that sits above the base pool (e.g. Paragliders' BotW-style overshield).
     * The bar renderer uses this to squeeze the live fill into {@code current / (max + extra)}
     * and paint the freed-up space with the absorption-bar texture, matching the health bar's
     * SQUEEZE absorption mode.
     *
     * @return extra (overshield) stamina, or {@code 0} if the source has no concept of it
     */
    default float getExtraStamina(Player player) {
        return 0f;
    }

    /**
     * @return The game time
     */
    long getGameTime();
    
    /**
     * Override to provide custom visibility logic.
     * Return true if this method fully determines visibility, false to use default logic.
     */
    default boolean hasSpecificVisibilityLogic() {
        return false;
    }
    
    /**
     * Used when hasSpecificVisibilityLogic() returns true.
     * Determines whether the stamina bar should be displayed.
     */
    default boolean shouldDisplayBarOverride(Player player) {
        return true;
    }
    
    /**
     * Used when hasSpecificVisibilityLogic() returns false.
     * Additional conditions that force the bar to show even when full.
     */
    default boolean forceShowBarConditions(Player player) {
        return false;
    }
    
    /**
     * Get the texture suffix for the bar (e.g., "stamina_bar", "stamina_bar_blood")
     * This allows providers to change bar appearance based on state
     */
    default String getBarTexture(Player player, float currentValue) {
        return "stamina_bar";
    }
    
    /**
     * Whether this provider should show overlay effects (like saturation)
     */
    default boolean shouldShowOverlays() {
        return true;
    }
} 