package dev.muon.dynamic_resource_bars.compat;

import net.minecraft.world.entity.player.Player;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureEnum;
import sfiomn.legendarysurvivaloverhaul.api.thirst.ThirstUtil;
import sfiomn.legendarysurvivaloverhaul.common.attachments.thirst.ThirstAttachment;
import sfiomn.legendarysurvivaloverhaul.config.Config;
import sfiomn.legendarysurvivaloverhaul.registry.MobEffectRegistry;
import sfiomn.legendarysurvivaloverhaul.util.AttachmentUtil;

public final class LsoCompatNeoForge implements LsoCompat.Provider {

    private static final float COLD_THRESHOLD = (TemperatureEnum.COLD.getValue() + TemperatureEnum.NORMAL.getValue()) / 2.0f; // 15.0f
    private static final float HEAT_THRESHOLD = (TemperatureEnum.NORMAL.getValue() + TemperatureEnum.HOT.getValue()) / 2.0f; // 25.0f
    private static final float MAX_WETNESS = 400.0f;

    @Override
    public float getTemperatureScale(Player player) {
        if (!Config.Baked.temperatureEnabled) return 0f;
        float temp = AttachmentUtil.getTempAttachment(player).getTemperatureLevel();
        if (temp < COLD_THRESHOLD) {
            float frostbite = TemperatureEnum.FROSTBITE.getValue(); // 5.0f
            float ratio = (COLD_THRESHOLD - temp) / (COLD_THRESHOLD - frostbite);
            return -Math.max(0f, Math.min(1f, ratio));
        }
        if (temp >= HEAT_THRESHOLD) {
            float heatStroke = TemperatureEnum.HEAT_STROKE.getValue(); // 35.0f
            float ratio = (temp - HEAT_THRESHOLD) / (heatStroke - HEAT_THRESHOLD);
            return Math.max(0f, Math.min(1f, ratio));
        }
        return 0f;
    }

    @Override
    public float getWetnessScale(Player player) {
        if (!Config.Baked.wetnessEnabled) return 0f;
        int wetness = AttachmentUtil.getWetnessAttachment(player).getWetness();
        return Math.max(0f, Math.min(1f, wetness / MAX_WETNESS));
    }

    @Override
    public boolean isFrozen(Player player) {
        if (!Config.Baked.temperatureEnabled) return false;
        return AttachmentUtil.getTempAttachment(player).getTemperatureLevel() <= TemperatureEnum.FROSTBITE.getValue();
    }

    @Override
    public boolean isScorched(Player player) {
        if (!Config.Baked.temperatureEnabled) return false;
        return AttachmentUtil.getTempAttachment(player).getTemperatureLevel() >= TemperatureEnum.HEAT_STROKE.getValue();
    }

    @Override
    public boolean isThirstActive(Player player) {
        return Config.Baked.thirstEnabled && ThirstUtil.isThirstActive(player);
    }

    @Override
    public float getCurrentThirst(Player player) {
        return AttachmentUtil.getThirstAttachment(player).getHydrationLevel();
    }

    @Override
    public float getMaxThirst(Player player) {
        return ThirstAttachment.MAX_HYDRATION;
    }

    @Override
    public boolean hasThirstEffect(Player player) {
        return isThirstActive(player) && player.hasEffect(MobEffectRegistry.THIRST);
    }

    @Override
    public boolean hasHeatThirstEffect(Player player) {
        return isThirstActive(player) && player.hasEffect(MobEffectRegistry.HEAT_THIRST);
    }
}
