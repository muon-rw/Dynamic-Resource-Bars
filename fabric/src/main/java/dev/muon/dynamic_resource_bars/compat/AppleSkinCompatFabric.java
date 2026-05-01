package dev.muon.dynamic_resource_bars.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import squeek.appleskin.helpers.ConsumableFood;
import squeek.appleskin.helpers.FoodHelper;

/**
 * Fabric AppleSkin {@link AppleSkinCompat.Provider}. Field names here ({@code modifiedFoodComponent},
 * etc.) match the Fabric jar; the NeoForge port renamed them to {@code modifiedFoodProperties} which
 * is why this code can't be shared across loaders.
 */
public final class AppleSkinCompatFabric implements AppleSkinCompat.Provider {

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
        return FoodHelper.getEstimatedHealthIncrement(player, new ConsumableFood(props, q.consumableComponent));
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
        if (stack.isEmpty() || !FoodHelper.isFood(stack)) return null;
        return FoodHelper.query(stack, player);
    }

    private static FoodProperties props(FoodHelper.QueriedFoodResult q) {
        return q.modifiedFoodComponent != null ? q.modifiedFoodComponent : q.defaultFoodComponent;
    }
}
