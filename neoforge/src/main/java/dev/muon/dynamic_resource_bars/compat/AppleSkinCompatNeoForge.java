package dev.muon.dynamic_resource_bars.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import squeek.appleskin.helpers.ConsumableFood;
import squeek.appleskin.helpers.FoodHelper;

/**
 * NeoForge AppleSkin {@link AppleSkinCompat.Provider}. The NeoForge port renamed
 * {@code modifiedFoodComponent} → {@code modifiedFoodProperties} (etc.) and made {@code isFood}
 * take a {@code Player} argument, so this code can't be shared with the Fabric impl.
 */
public final class AppleSkinCompatNeoForge implements AppleSkinCompat.Provider {

    @Override
    public boolean canConsume(Player player, ItemStack stack) {
        FoodHelper.QueriedFoodResult q = query(player, stack);
        if (q == null) return false;
        FoodProperties props = props(q);
        return props != null && FoodHelper.canConsume(player, props);
    }

    @Override
    public float getEstimatedHealthRestoration(Player player, ItemStack stack) {
        FoodHelper.QueriedFoodResult q = query(player, stack);
        if (q == null) return 0f;
        FoodProperties props = props(q);
        if (props == null) return 0f;
        return FoodHelper.getEstimatedHealthIncrement(player, new ConsumableFood(props, q.consumable));
    }

    @Override
    public int getFoodNutrition(Player player, ItemStack stack) {
        FoodHelper.QueriedFoodResult q = query(player, stack);
        if (q == null) return 0;
        FoodProperties props = props(q);
        return props == null ? 0 : props.nutrition();
    }

    @Override
    public float getFoodSaturation(Player player, ItemStack stack) {
        FoodHelper.QueriedFoodResult q = query(player, stack);
        if (q == null) return 0f;
        FoodProperties props = props(q);
        return props == null ? 0f : props.saturation();
    }

    private static FoodHelper.QueriedFoodResult query(Player player, ItemStack stack) {
        if (stack.isEmpty() || !FoodHelper.isFood(stack, player)) return null;
        return FoodHelper.query(stack, player);
    }

    private static FoodProperties props(FoodHelper.QueriedFoodResult q) {
        return q.modifiedFoodProperties != null ? q.modifiedFoodProperties : q.defaultFoodProperties;
    }
}
