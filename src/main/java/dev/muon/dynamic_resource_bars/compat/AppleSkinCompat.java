package dev.muon.dynamic_resource_bars.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.muon.dynamic_resource_bars.util.PlatformUtil;

#if FABRIC && UPTO_20_1
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.helpers.FoodHelper;
#elif FABRIC && NEWER_THAN_20_1
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.helpers.FoodHelper;
import net.minecraft.world.food.FoodProperties;
#elif FORGE
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.helpers.FoodHelper;
import net.minecraftforge.common.MinecraftForge;
#elif NEO
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.helpers.FoodHelper;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.neoforge.common.NeoForge;
#endif

public class AppleSkinCompat {
    private static final boolean APPLESKIN_LOADED = PlatformUtil.isModLoaded("appleskin");

    public static boolean isLoaded() {
        return APPLESKIN_LOADED;
    }

    public static FoodData getFoodValues(ItemStack stack, Player player) {
        if (!APPLESKIN_LOADED || stack.isEmpty() || !canConsume(stack, player)) {
            return FoodData.EMPTY;
        }

        #if (FABRIC && UPTO_20_1) || FORGE
        FoodValues modifiedValues = FoodHelper.getModifiedFoodValues(stack, player);
        FoodValues defaultValues = FoodHelper.getDefaultFoodValues(stack#if FORGE, player#endif);

        FoodValuesEvent event = new FoodValuesEvent(player, stack, defaultValues, modifiedValues);

        #if FABRIC
        FoodValuesEvent.EVENT.invoker().interact(event);
        #elif FORGE
        MinecraftForge.EVENT_BUS.post(event);
        #endif

        modifiedValues = event.modifiedFoodValues;
        return new FoodData(modifiedValues.hunger, modifiedValues.getSaturationIncrement());

        #elif FABRIC && NEWER_THAN_20_1
        // 1.21.1 Fabric - the decompiled source shows it's already remapped to mojmap
        var queriedResult = FoodHelper.query(stack, player);
        if (queriedResult == null) return FoodData.EMPTY;

        return new FoodData(queriedResult.modifiedFoodComponent.nutrition(),
                           queriedResult.modifiedFoodComponent.saturation());

        #elif NEO
        var queriedResult = FoodHelper.query(stack, player);
        if (queriedResult == null) return FoodData.EMPTY;

        return new FoodData(queriedResult.modifiedFoodProperties.nutrition(),
                           queriedResult.modifiedFoodProperties.saturation());
        #else
        return FoodData.EMPTY;
        #endif
    }

    public static boolean canConsume(ItemStack stack, Player player) {
        if (!APPLESKIN_LOADED || stack.isEmpty()) {
            return false;
        }

        #if (FABRIC && UPTO_20_1) || FORGE
        return FoodHelper.canConsume(stack, player);
        #elif FABRIC && NEWER_THAN_20_1
        // 1.21.1 Fabric
        var result = FoodHelper.query(stack, player);
        return result != null && FoodHelper.canConsume(player, result.modifiedFoodComponent);
        #elif NEO
        var result = FoodHelper.query(stack, player);
        return result != null && FoodHelper.canConsume(player, result.modifiedFoodProperties);
        #else
        // Fallback to vanilla check
        return stack.isEdible() && player.canEat(false);
        #endif
    }
    
    public static float getEstimatedHealthRestoration(ItemStack stack, Player player) {
        if (!APPLESKIN_LOADED || !canConsume(stack, player)) {
            return 0f;
        }
        
        #if UPTO_20_1
        FoodValues modifiedValues = FoodHelper.getModifiedFoodValues(stack, player);
        return FoodHelper.getEstimatedHealthIncrement(stack, modifiedValues, player);
        
        #elif FABRIC && NEWER_THAN_20_1
        var queriedResult = FoodHelper.query(stack, player);
        if (queriedResult == null) return 0f;
        return FoodHelper.getEstimatedHealthIncrement(player, queriedResult.modifiedFoodComponent);
        
        #elif NEO
        var queriedResult = FoodHelper.query(stack, player);
        if (queriedResult == null) return 0f;
        return FoodHelper.getEstimatedHealthIncrement(player, queriedResult.modifiedFoodProperties);
        
        #else
        return 0f;
        #endif
    }
    
    public static class FoodData {
        public static final FoodData EMPTY = new FoodData(0, 0f);
        
        public final int hunger;
        public final float saturation;
        
        public FoodData(int hunger, float saturation) {
            this.hunger = hunger;
            this.saturation = saturation;
        }
        
        public boolean isEmpty() {
            return hunger <= 0 && saturation <= 0;
        }
    }
} 