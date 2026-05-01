package dev.muon.dynamic_resource_bars.render;

import dev.muon.dynamic_resource_bars.util.BarVisibility;
import dev.muon.dynamic_resource_bars.util.FillDirection;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;
import dev.muon.dynamic_resource_bars.util.TextBehavior;

/**
 * Per-frame snapshot of all config values that drive bar layout & visibility.
 * Each {@link AbstractBarRenderer} subclass builds one of these from its own
 * config-prefixed {@link dev.muon.dynamic_resource_bars.config.ClientConfig} fields,
 * so the abstract render code never has to know which bar it is rendering.
 */
public record BarConfig(
        int backgroundWidth,
        int backgroundHeight,
        int backgroundXOffset,
        int backgroundYOffset,
        int barWidth,
        int barHeight,
        int barXOffset,
        int barYOffset,
        int overlayWidth,
        int overlayHeight,
        int overlayXOffset,
        int overlayYOffset,
        int textXOffset,
        int textYOffset,
        int textColor,
        int textOpacity,
        HorizontalAlignment textAlign,
        int totalXOffset,
        int totalYOffset,
        HUDPositioning.BarPlacement anchor,
        boolean enableBackground,
        boolean enableForeground,
        BarVisibility barVisibility,
        FillDirection fillDirection,
        TextBehavior textBehavior
) {}
