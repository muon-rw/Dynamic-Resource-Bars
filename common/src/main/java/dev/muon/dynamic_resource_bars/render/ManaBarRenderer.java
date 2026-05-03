package dev.muon.dynamic_resource_bars.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.compat.ManaProviderManager;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.ManaProvider;
import dev.muon.dynamic_resource_bars.util.AnimationMetadata;
import dev.muon.dynamic_resource_bars.util.AnimationMetadataCache;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.RenderUtil;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class ManaBarRenderer extends AbstractBarRenderer {

    public static final ManaBarRenderer INSTANCE = new ManaBarRenderer();

    private static final int RESERVED_MANA_COLOR = 0x232323;

    @Override protected DraggableElement draggable() { return DraggableElement.MANA_BAR; }
    @Override protected int editModeBarOutlineColor() { return 0xA000FFFF; }

    @Override
    protected BarConfig config() {
        ClientConfig c = ModConfigManager.getClient();
        return new BarConfig(
                c.manaBackgroundWidth, c.manaBackgroundHeight,
                c.manaBackgroundXOffset, c.manaBackgroundYOffset,
                c.manaBarWidth, c.manaBarHeight, c.manaBarXOffset, c.manaBarYOffset,
                c.manaOverlayWidth, c.manaOverlayHeight, c.manaOverlayXOffset, c.manaOverlayYOffset,
                c.manaTextXOffset, c.manaTextYOffset, c.manaTextWidth, c.manaTextHeight,
                c.manaTextColor, c.manaTextOpacity, c.manaTextAlign,
                c.manaTotalXOffset, c.manaTotalYOffset,
                c.manaBarAnchor,
                c.enableManaBackground, c.enableManaForeground, c.manaBarVisibility,
                c.manaFillDirection, c.showManaText
        );
    }

    @Override protected Identifier backgroundTexture() { return Constants.loc("textures/gui/mana_background.png"); }
    @Override protected Identifier foregroundTexture() { return Constants.loc("textures/gui/mana_foreground.png"); }
    @Override protected Identifier barTexture(Player p, float c, float m) { return Constants.loc("textures/gui/mana_bar.png"); }
    @Override protected AnimationMetadata.AnimationData barAnimation() { return AnimationMetadataCache.getManaBarAnimation(); }
    @Override protected AnimationMetadata.ScalingInfo backgroundScaling() { return AnimationMetadataCache.getManaBackgroundScaling(); }
    @Override protected AnimationMetadata.ScalingInfo foregroundScaling() { return AnimationMetadataCache.getManaForegroundScaling(); }

    @Override
    protected float currentValue(Player player) {
        ManaProvider p = ManaProviderManager.getCurrentProvider();
        return p == null ? 0f : (float) p.getCurrentMana();
    }

    @Override
    protected float maxValue(Player player) {
        ManaProvider p = ManaProviderManager.getCurrentProvider();
        if (p == null) return 0f;
        // Effective max accounts for reserved mana so the bar fill stays in proportion.
        return p.getMaxMana() * (1f + p.getReservedMana());
    }

    /** Provider-driven visibility: lets compat providers override the simple "fade when full" rule. */
    @Override
    protected boolean shouldFadeWhenFull(Player player, float current, float max) {
        ClientConfig cfg = ModConfigManager.getClient();
        switch (cfg.manaBarVisibility) {
            case ALWAYS -> { return false; }
            case NEVER -> { return true; }
            case SMART_FADE -> {} // fall through to provider-aware logic
        }
        ManaProvider provider = ManaProviderManager.getCurrentProvider();
        if (provider == null) return current >= max;
        if (provider.hasSpecificVisibilityLogic()) {
            return !provider.shouldDisplayBarOverride(player);
        }
        boolean providerForcesShow = provider.forceShowBarConditions(player);
        boolean manaFull = provider.getCurrentMana() >= provider.getMaxMana();
        return !providerForcesShow && manaFull;
    }

    @Override
    protected void renderBetweenBarAndForeground(GuiGraphicsExtractor graphics, Player player,
                                                 float current, float max, ScreenRect barRect,
                                                 int animOffset, AnimationMetadata.AnimationData animData) {
        ManaProvider provider = ManaProviderManager.getCurrentProvider();
        if (provider == null) return;
        float reservedFraction = provider.getReservedMana();
        if (reservedFraction <= 0) return;

        int reservedWidth = (int) (barRect.width() * (reservedFraction / (1f + reservedFraction)));
        if (reservedWidth <= 0) return;

        int x = barRect.x() + barRect.width() - reservedWidth;
        // Tint the mana texture with the reserved-mana grey at full alpha.
        int tint = 0xFF000000 | RESERVED_MANA_COLOR;
        RenderUtil.blitWithBinding(graphics, Constants.loc("textures/gui/mana_bar.png"),
                x, barRect.y(), 0, animOffset, reservedWidth, barRect.height(),
                animData.textureWidth, animData.textureHeight, tint);
    }

}
