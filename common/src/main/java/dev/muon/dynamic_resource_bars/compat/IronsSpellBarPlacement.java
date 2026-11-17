package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import dev.muon.dynamic_resource_bars.render.AirBarRenderer;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.ManaBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import dev.muon.dynamic_resource_bars.util.Position;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class IronsSpellBarPlacement {

    public static final String MOD_ID = "irons_spellbooks";

    // Other mods often draw something just outside the slots, so we keep that space clear of our bars too.
    // Todo: maybe a registry, or user config value
    private static final int VERTICAL_PADDING = 10;
    private static final int HORIZONTAL_PADDING = 2;

    private static ScreenRect lastSlotsRect;

    private IronsSpellBarPlacement() {}

    public static ScreenRect lastSlotsRect() {
        return lastSlotsRect;
    }

    public static void forgetLastSlotsRect() {
        lastSlotsRect = null;
    }

    public static Position offsetClearOfBars(ScreenRect slots, Player player, int screenWidth, int screenHeight) {
        Position offset = ModConfigManager.getClient().keepIronsSpellBarClearOfBars
                ? offsetClearOf(slots, drawnBarRects(player), screenWidth, screenHeight)
                : new Position(0, 0);
        lastSlotsRect = new ScreenRect(slots.x() + offset.x(), slots.y() + offset.y(), slots.width(), slots.height());
        return offset;
    }

    public static List<ScreenRect> drawnBarRects(Player player) {
        ClientConfig config = ModConfigManager.getClient();
        List<ScreenRect> rects = new ArrayList<>();
        if (config.healthBarBehavior == BarRenderBehavior.CUSTOM) rects.add(HealthBarRenderer.INSTANCE.getScreenRect(player));
        if (config.staminaBarBehavior != StaminaBarBehavior.OFF) rects.add(StaminaBarRenderer.INSTANCE.getScreenRect(player));
        if (config.manaBarBehavior != ManaBarBehavior.OFF) rects.add(ManaBarRenderer.INSTANCE.getScreenRect(player));
        if (config.armorBarBehavior == BarRenderBehavior.CUSTOM) rects.add(ArmorBarRenderer.INSTANCE.getScreenRect(player));
        if (config.airBarBehavior == BarRenderBehavior.CUSTOM) rects.add(AirBarRenderer.INSTANCE.getScreenRect(player));
        return rects;
    }

    static Position offsetClearOf(ScreenRect slots, List<ScreenRect> obstacles, int screenWidth, int screenHeight) {
        ScreenRect padded = new ScreenRect(slots.x() - HORIZONTAL_PADDING, slots.y() - VERTICAL_PADDING,
                slots.width() + 2 * HORIZONTAL_PADDING, slots.height() + 2 * VERTICAL_PADDING);
        if (firstOverlap(padded, obstacles) == null) return new Position(0, 0);

        ScreenRect left = slideLeft(padded, obstacles);
        if (left != null) return new Position(left.x() - padded.x(), 0);

        int onScreenX = Math.max(0, Math.min(padded.x(), screenWidth - padded.width()));
        ScreenRect above = slideUp(new ScreenRect(onScreenX, padded.y(), padded.width(), padded.height()), obstacles);
        if (above != null) return new Position(above.x() - padded.x(), above.y() - padded.y());

        return new Position(0, 0);
    }

    private static ScreenRect slideLeft(ScreenRect rect, List<ScreenRect> obstacles) {
        for (int i = 0; i <= obstacles.size(); i++) {
            ScreenRect hit = firstOverlap(rect, obstacles);
            if (hit == null) return rect;
            rect = new ScreenRect(hit.x() - rect.width(), rect.y(), rect.width(), rect.height());
            if (rect.x() < 0) return null;
        }
        return null;
    }

    private static ScreenRect slideUp(ScreenRect rect, List<ScreenRect> obstacles) {
        for (int i = 0; i <= obstacles.size(); i++) {
            ScreenRect hit = firstOverlap(rect, obstacles);
            if (hit == null) return rect;
            rect = new ScreenRect(rect.x(), hit.y() - rect.height(), rect.width(), rect.height());
            if (rect.y() < 0) return null;
        }
        return null;
    }

    private static ScreenRect firstOverlap(ScreenRect rect, List<ScreenRect> obstacles) {
        for (ScreenRect obstacle : obstacles) {
            if (overlaps(rect, obstacle)) return obstacle;
        }
        return null;
    }

    public static boolean overlaps(ScreenRect a, ScreenRect b) {
        return a.width() > 0 && a.height() > 0 && b.width() > 0 && b.height() > 0
                && a.x() < b.x() + b.width() && b.x() < a.x() + a.width()
                && a.y() < b.y() + b.height() && b.y() < a.y() + a.height();
    }
}
