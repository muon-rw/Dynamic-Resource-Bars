package dev.muon.dynamic_resource_bars.render;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.Position;
import dev.muon.dynamic_resource_bars.util.RenderUtil;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

#if NEWER_THAN_20_1
import net.minecraft.client.DeltaTracker;
#endif

import dev.muon.dynamic_resource_bars.util.SubElementType;

public class ArmorBarRenderer {
    private static long armorTextStartTime = 0;
    private static boolean shouldShowArmorText = false;

    private enum ArmorIcon {
        NONE("tier_0"),
        LIGHT("tier_1"),
        MEDIUM("tier_2"),
        HEAVY("tier_3"),
        PLATE("tier_4"),
        REINFORCED("tier_5"),
        ENHANCED("tier_6"),
        MASTERWORK("tier_7"),
        LEGENDARY("tier_8"),
        MYTHICAL("tier_9"),
        DIVINE("tier_10");

        private final String texture;

        ArmorIcon(String texture) {
            this.texture = texture;
        }

        public String getTexture() {
            return texture;
        }

        public static ArmorIcon fromArmorValue(int armorValue) {
            if (armorValue <= 0) {
                return NONE;
            }

            int maxArmor = ModConfigManager.getClient().maxExpectedArmor;
            int tier = Math.max(1, Math.min(10, (int) ((float) armorValue / maxArmor * 10)));
            return values()[tier];
        }
    }

    public static ScreenRect getScreenRect(Player player) {
        if (player == null) return new ScreenRect(0,0,0,0);
        var config = ModConfigManager.getClient();
        Position anchorPos = HUDPositioning.getPositionFromAnchor(config.armorBarAnchor);
        
        Position finalPos = anchorPos.offset(config.armorTotalXOffset, config.armorTotalYOffset);
        int backgroundWidth = config.armorBackgroundWidth;
        int backgroundHeight = config.armorBackgroundHeight;
        return new ScreenRect(finalPos.x(), finalPos.y(), backgroundWidth, backgroundHeight);
    }

    public static ScreenRect getSubElementRect(SubElementType type, Player player) {
        ScreenRect complexRect = getScreenRect(player);
        if (complexRect == null || (complexRect.width() == 0 && complexRect.height() == 0)) {
            return new ScreenRect(0, 0, 0, 0);
        }

        ClientConfig config = ModConfigManager.getClient();
        int x = complexRect.x();
        int y = complexRect.y();

        switch (type) {
            case BACKGROUND:
                return new ScreenRect(x, y,
                                      config.armorBackgroundWidth,
                                      config.armorBackgroundHeight);
            case BAR_MAIN:
                return new ScreenRect(x + config.armorBarXOffset,
                                      y + config.armorBarYOffset,
                                      config.armorBarWidth,
                                      config.armorBarHeight);
            default:
                return new ScreenRect(0, 0, 0, 0);
        }
    }

    public static void render(GuiGraphics graphics, Player player) {
        ClientConfig config = ModConfigManager.getClient();
        if (config.armorBarBehavior != BarRenderBehavior.CUSTOM) {
            return;
        }
        Position armorPos = HUDPositioning.getPositionFromAnchor(config.armorBarAnchor)
                .offset(config.armorTotalXOffset, config.armorTotalYOffset);

        int backgroundWidth = config.armorBackgroundWidth;
        int backgroundHeight = config.armorBackgroundHeight;
        int barWidth = config.armorBarWidth;
        int barHeight = config.armorBarHeight;
        int barOnlyXOffset = config.armorBarXOffset;
        int barOnlyYOffset = config.armorBarYOffset;
        int iconSize = config.armorIconSize;
        boolean isRightAnchored = config.armorBarAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT;

        int xPos = armorPos.x();
        int yPos = armorPos.y();

        graphics.blit(
                DynamicResourceBars.loc("textures/gui/armor_background.png"), xPos, yPos, 0, 0, backgroundWidth, backgroundHeight, 256, 256
        );

        int armorValue = player.getArmorValue();
        float armorPercent = Math.min(1.0f, (float) armorValue / config.maxExpectedArmor);

        int filledWidth = Math.round((barWidth - (float) iconSize / 2) * armorPercent);
        if (filledWidth > 0) {
            int barX = xPos + barOnlyXOffset;
            if (config.enableArmorIcon) {
                if (isRightAnchored) {
                    barX += barWidth - filledWidth - iconSize / 2;
                } else {
                    barX += iconSize / 2;
                }
            }

            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/armor_bar.png"),
                    barX,
                    yPos + barOnlyYOffset,
                    0, 0,
                    filledWidth,
                    barHeight,
                    256, 256
            );
        }

        if (shouldRenderText()) {
            int textX = (xPos + (backgroundWidth / 2));
            int textY = (yPos + barOnlyYOffset);
            int color = getTextColor();

            RenderUtil.renderArmorText(armorValue,
                    graphics, textX, textY, color);
        }
        renderProtectionOverlay(graphics, player, config, xPos, yPos, barWidth, barHeight, barOnlyXOffset, barOnlyYOffset, iconSize);

        if (config.enableArmorIcon) {
            ArmorIcon icon = ArmorIcon.fromArmorValue(armorValue);
            int iconX = isRightAnchored ?
                    xPos + backgroundWidth - iconSize + config.armorIconXOffset :
                    xPos - 1 + config.armorIconXOffset;

            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/armors/" + icon.getTexture() + ".png"),
                    iconX,
                    yPos + (backgroundHeight - iconSize) / 2 - 2 + config.armorIconYOffset,
                    0, 0,
                    iconSize, iconSize,
                    iconSize, iconSize
            );
        }
    }

    private static void renderProtectionOverlay(GuiGraphics graphics, Player player, ClientConfig config,
                                                int xPos, int yPos, int barWidth, int barHeight,
                                                int barOnlyXOffset, int barOnlyYOffset, int iconSize) {
        int totalProtection = 0;
        // TODO: 1.21+
        #if UPTO_20_1
        for (ItemStack armorPiece : player.getArmorSlots()) {
            totalProtection += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, armorPiece);
        }
        #endif

        if (totalProtection <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int frameHeight = config.protOverlayFrameHeight;
        int animationCycles = config.protOverlayAnimationCycles;
        int maxProtection = config.maxExpectedProt;

        int animOffset = (int)(((player.tickCount + #if NEWER_THAN_20_1 Minecraft.getInstance().getTimer().getGameTimeDeltaTicks() #else Minecraft.getInstance().getFrameTime() #endif) / 3) % animationCycles) * frameHeight;

        float protectionScale = Math.min(1.0f, (float)totalProtection / maxProtection);
        int adjustedBarWidth = config.enableArmorIcon ?
                barWidth - (iconSize / 2) : barWidth;
        int overlayWidth = (int)(adjustedBarWidth * protectionScale);

        graphics.blit(
                DynamicResourceBars.loc("textures/gui/protection_overlay.png"),
                xPos + (config.enableArmorIcon ? barOnlyXOffset + iconSize / 2 : barOnlyXOffset),
                yPos + barOnlyYOffset,
                0, animOffset,
                overlayWidth, barHeight,
                256, 256
        );

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }


    private static int getTextColor() {
        long timeSinceTextTrigger = armorTextStartTime > 0 ?
                System.currentTimeMillis() - armorTextStartTime : 0;

        int alpha = RenderUtil.calculateTextAlpha(timeSinceTextTrigger);
        alpha = Math.max(10, alpha);

        return (alpha << 24) | 0xFFFFFF;
    }

    private static boolean shouldRenderText() {
        long timeSinceTextTrigger = armorTextStartTime > 0 ?
                System.currentTimeMillis() - armorTextStartTime : 0;
        return shouldShowArmorText ||
                (armorTextStartTime > 0 && timeSinceTextTrigger < RenderUtil.TEXT_DISPLAY_DURATION);
    }

    public static void triggerTextDisplay() {
        armorTextStartTime = System.currentTimeMillis();
        shouldShowArmorText = true;
    }

    public static void stopTextDisplay() {
        shouldShowArmorText = false;
    }

    public static boolean isArmorRelevantItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stack.getItem() instanceof ArmorItem) return true;

        // TODO: 1.21+
        #if UPTO_20_1
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(slot);
            if (modifiers.get(Attributes.ARMOR).stream()
                    .mapToDouble(AttributeModifier::getAmount)
                    .sum() > 0) {
                return true;
            }
        }
        #endif
        return false;
    }
}