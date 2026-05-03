package dev.muon.dynamic_resource_bars.render;

import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.AnimationMetadata;
import dev.muon.dynamic_resource_bars.util.AnimationMetadataCache;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.EditModeManager;
import dev.muon.dynamic_resource_bars.util.NineSliceRenderer;
import dev.muon.dynamic_resource_bars.util.RenderUtil;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import dev.muon.dynamic_resource_bars.util.TickHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class ArmorBarRenderer extends AbstractBarRenderer {

    public static final ArmorBarRenderer INSTANCE = new ArmorBarRenderer();

    /** Tier-based armor icon — 11 buckets indexed by {@code armorValue / maxExpectedArmor * 10}. */
    private enum ArmorIcon {
        NONE, LIGHT, MEDIUM, HEAVY, PLATE, REINFORCED, ENHANCED, MASTERWORK, LEGENDARY, MYTHICAL, DIVINE;

        public Identifier loc() {
            return Constants.loc("textures/gui/armors/tier_" + ordinal() + ".png");
        }

        static ArmorIcon forArmor(int armorValue, int maxExpectedArmor) {
            if (armorValue <= 0 || maxExpectedArmor <= 0) return NONE;
            int tier = (int) ((float) armorValue / maxExpectedArmor * 10f);
            return values()[Math.max(1, Math.min(10, tier))];
        }
    }

    @Override protected DraggableElement draggable() { return DraggableElement.ARMOR_BAR; }
    @Override protected int editModeBarOutlineColor() { return 0xA0C0C0C0; }

    @Override
    protected BarConfig config() {
        ClientConfig c = ModConfigManager.getClient();
        return new BarConfig(
                c.armorBackgroundWidth, c.armorBackgroundHeight,
                c.armorBackgroundXOffset, c.armorBackgroundYOffset,
                c.armorBarWidth, c.armorBarHeight, c.armorBarXOffset, c.armorBarYOffset,
                c.armorOverlayWidth, c.armorOverlayHeight, c.armorOverlayXOffset, c.armorOverlayYOffset,
                c.armorTextXOffset, c.armorTextYOffset, c.armorTextWidth, c.armorTextHeight,
                c.armorTextColor, c.armorTextOpacity, c.armorTextAlign,
                c.armorTotalXOffset, c.armorTotalYOffset,
                c.armorBarAnchor,
                c.enableArmorBackground, c.enableArmorForeground, c.armorBarVisibility,
                c.armorFillDirection, c.showArmorText
        );
    }

    @Override protected Identifier backgroundTexture() { return Constants.loc("textures/gui/armor_background.png"); }
    @Override protected Identifier foregroundTexture() { return Constants.loc("textures/gui/armor_foreground.png"); }
    @Override protected Identifier barTexture(Player p, float c, float m) { return Constants.loc("textures/gui/armor_bar.png"); }
    /** Armor bar is static — single-frame "animation" yields a 0 offset every frame. */
    @Override protected AnimationMetadata.AnimationData barAnimation() {
        return AnimationMetadataCache.getArmorBarAnimation();
    }
    @Override protected AnimationMetadata.ScalingInfo backgroundScaling() {
        return AnimationMetadataCache.getScalingOrLoad(
                Constants.loc("textures/gui/armor_background.png"),
                AnimationMetadata.TextureType.BACKGROUND);
    }
    @Override protected AnimationMetadata.ScalingInfo foregroundScaling() {
        return AnimationMetadataCache.getArmorForegroundScaling();
    }

    @Override
    protected float currentValue(Player player) {
        if (EditModeManager.isEditModeEnabled() && player.getArmorValue() == 0) {
            return ModConfigManager.getClient().maxExpectedArmor;
        }
        return player.getArmorValue();
    }

    @Override protected float maxValue(Player player) {
        return ModConfigManager.getClient().maxExpectedArmor;
    }

    /** Armor fades on the OPPOSITE rule — hide when empty (armor=0), not when full. */
    @Override
    protected boolean smartFadeTrigger(Player player, float current, float max) {
        return player.getArmorValue() <= 0;
    }

    @Override
    protected boolean shouldRender(Player player) {
        if (!super.shouldRender(player)) return false;
        return ModConfigManager.getClient().armorBarBehavior == BarRenderBehavior.CUSTOM
                || EditModeManager.isEditModeEnabled();
    }

    /** Protection-enchantment overlay (pulses) followed by the armor-tier icon. */
    @Override
    protected void renderBarOverlays(GuiGraphicsExtractor graphics, Player player,
                                     float current, float max, ScreenRect barRect, float alpha) {
        ClientConfig c = ModConfigManager.getClient();

        int totalProtection = totalProtectionLevel(player);
        if (totalProtection > 0) {
            float protRatio = Math.min(1f, (float) totalProtection / Math.max(1, c.maxExpectedProt));
            float pulse = 0.5f + (TickHandler.getOverlayFlashAlpha() * 0.5f);
            int iconReserve = c.enableArmorIcon ? c.armorIconWidth / 2 : 0;
            int adjustedW = barRect.width() - iconReserve;
            int overlayW = (int) (adjustedW * protRatio);
            int overlayX = barRect.x() + iconReserve;
            if (overlayW > 0) {
                Identifier tex = Constants.loc("textures/gui/protection_overlay.png");
                AnimationMetadata.TextureDimensions dims = AnimationMetadataCache.getTextureDimensions(tex);
                NineSliceRenderer.renderWithScaling(graphics, tex,
                        AnimationMetadataCache.getProtectionOverlayScaling(),
                        overlayX, barRect.y(), overlayW, barRect.height(),
                        dims.width, dims.height,
                        RenderUtil.whiteWithAlpha(pulse * alpha));
            }
        }

        if (c.enableArmorIcon || EditModeManager.isEditModeEnabled()) {
            ArmorIcon icon = ArmorIcon.forArmor((int) current, c.maxExpectedArmor);
            ScreenRect rect = getSubElementRect(SubElementType.ICON, player);
            if (rect.width() > 0 && rect.height() > 0) {
                RenderUtil.blitWithBinding(graphics, icon.loc(),
                        rect.x(), rect.y(), 0, 0, rect.width(), rect.height(),
                        rect.width(), rect.height(),
                        RenderUtil.whiteWithAlpha(alpha));
            }
        }
    }

    @Override
    protected ScreenRect getCustomSubElementRect(SubElementType type, Player player, ScreenRect complexRect) {
        if (type == SubElementType.ICON) {
            ClientConfig c = ModConfigManager.getClient();
            return new ScreenRect(
                    complexRect.x() + c.armorIconXOffset,
                    complexRect.y() + c.armorIconYOffset,
                    c.armorIconWidth, c.armorIconHeight);
        }
        return super.getCustomSubElementRect(type, player, complexRect);
    }

    /**
     * TODO(26.1.2): protection enchantment lookup needs porting. Vanilla 26.1.2 uses
     * {@code ItemEnchantments} on the data-component side; the old {@code EnchantmentHelper.getItemEnchantmentLevel}
     * is gone. Until the new path is in, the protection overlay never renders (returns 0).
     */
    private static int totalProtectionLevel(Player player) {
        return 0;
    }
}
