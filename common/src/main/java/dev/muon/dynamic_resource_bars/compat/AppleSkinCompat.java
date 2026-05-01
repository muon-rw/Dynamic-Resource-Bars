package dev.muon.dynamic_resource_bars.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * AppleSkin lookup helpers used by the bar overlays.
 *
 * <p>The Fabric and NeoForge AppleSkin jars expose differently-named helper internals
 * ({@code FoodHelper.QueriedFoodResult.modifiedFoodComponent} vs {@code modifiedFoodProperties},
 * single-arg vs two-arg {@code isFood}, etc.), so the actual lookups live in per-loader
 * {@link Provider} impls. Common code calls the static methods here, which delegate to whichever
 * provider was installed at startup. When AppleSkin isn't loaded the provider stays null and
 * every method returns an empty/zero result — no AppleSkin classes get touched.
 */
public final class AppleSkinCompat {

    public static final String MOD_ID = "appleskin";

    private static Provider provider;

    private AppleSkinCompat() {}

    public static void setProvider(Provider p) { provider = p; }
    public static boolean isAvailable() { return provider != null; }

    public static boolean canConsume(Player player, ItemStack stack) {
        return provider != null && provider.canConsume(player, stack);
    }

    public static float getEstimatedHealthRestoration(Player player, ItemStack stack) {
        return provider == null ? 0f : provider.getEstimatedHealthRestoration(player, stack);
    }

    public static int getFoodNutrition(Player player, ItemStack stack) {
        return provider == null ? 0 : provider.getFoodNutrition(player, stack);
    }

    public static float getFoodSaturation(Player player, ItemStack stack) {
        return provider == null ? 0f : provider.getFoodSaturation(player, stack);
    }

    /** Held food in main-hand, falling back to off-hand; empty if neither is consumable. */
    public static ItemStack pickHeldFood(Player player) {
        if (provider == null) return ItemStack.EMPTY;
        ItemStack main = player.getMainHandItem();
        if (provider.canConsume(player, main)) return main;
        ItemStack off = player.getOffhandItem();
        if (provider.canConsume(player, off)) return off;
        return ItemStack.EMPTY;
    }

    /** Per-loader implementation, installed by the loader's mod entry behind an isModLoaded guard. */
    public interface Provider {
        boolean canConsume(Player player, ItemStack stack);
        float getEstimatedHealthRestoration(Player player, ItemStack stack);
        int getFoodNutrition(Player player, ItemStack stack);
        float getFoodSaturation(Player player, ItemStack stack);
    }
}
