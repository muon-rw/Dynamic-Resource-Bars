package dev.muon.dynamic_resource_bars.harness;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side only: the integrated server never learns about these values, so it never sends a
 * correction packet, and effects like poison render without ticking real damage. We re-apply the
 * scalar values every tick because the client still ticks air, freeze, and fire on its own.
 */
record HarnessPlayerState(float maxHealth, Float health, float absorption, int food, float saturation,
                          Integer air, int armor, int frozenTicks, int fireTicks, List<String> effects) {

    static HarnessPlayerState parse(JsonObject p) {
        List<String> effects = new ArrayList<>();
        if (p.has("effects")) p.getAsJsonArray("effects").forEach(e -> effects.add(e.getAsString()));
        return new HarnessPlayerState(
                p.has("maxHealth") ? p.get("maxHealth").getAsFloat() : 20f,
                p.has("health") ? p.get("health").getAsFloat() : null,
                p.has("absorption") ? p.get("absorption").getAsFloat() : 0f,
                p.has("food") ? p.get("food").getAsInt() : 20,
                p.has("saturation") ? p.get("saturation").getAsFloat() : 5f,
                p.has("air") ? p.get("air").getAsInt() : null,
                p.has("armor") ? p.get("armor").getAsInt() : 0,
                p.has("frozenTicks") ? p.get("frozenTicks").getAsInt() : 0,
                p.has("fireTicks") ? p.get("fireTicks").getAsInt() : 0,
                effects);
    }

    void applyEffects(LocalPlayer player) {
        new ArrayList<>(player.getActiveEffectsMap().keySet()).forEach(player::removeEffectNoUpdate);
        for (String spec : effects) {
            String[] parts = spec.split("\\*");
            Identifier id = Identifier.parse(parts[0].trim());
            int amplifier = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.get(id)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown effect '" + id + "'"));
            player.forceAddEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, false, true), null);
        }
    }

    void pin(LocalPlayer player) {
        setBase(player, Attributes.MAX_HEALTH, maxHealth);
        setBase(player, Attributes.ARMOR, armor);
        // Vanilla clamps absorption to this attribute, which is 0 unless an effect raised it.
        setBase(player, Attributes.MAX_ABSORPTION, absorption);
        player.setHealth(health == null ? player.getMaxHealth() : health);
        player.setAbsorptionAmount(absorption);
        player.getFoodData().setFoodLevel(food);
        player.getFoodData().setSaturation(saturation);
        player.setAirSupply(air == null ? player.getMaxAirSupply() : air);
        player.setTicksFrozen(frozenTicks);
        player.setRemainingFireTicks(fireTicks);
        // Vanilla skips the empty first-person arm for invisible players, which keeps it out from behind the right-side bars.
        player.setInvisible(true);
    }

    static JsonObject describe(LocalPlayer player) {
        JsonObject out = new JsonObject();
        out.addProperty("health", player.getHealth());
        out.addProperty("maxHealth", player.getMaxHealth());
        out.addProperty("absorption", player.getAbsorptionAmount());
        out.addProperty("food", player.getFoodData().getFoodLevel());
        out.addProperty("saturation", player.getFoodData().getSaturationLevel());
        out.addProperty("air", player.getAirSupply());
        out.addProperty("armor", player.getArmorValue());
        out.addProperty("frozenTicks", player.getTicksFrozen());
        out.addProperty("fireTicks", player.getRemainingFireTicks());
        JsonArray effects = new JsonArray();
        player.getActiveEffectsMap().keySet().forEach(e -> effects.add(e.getRegisteredName()));
        out.add("effects", effects);
        return out;
    }

    private static void setBase(LocalPlayer player, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getBaseValue() != value) instance.setBaseValue(value);
    }
}
