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
import dev.muon.dynamic_resource_bars.util.TickHandler;
import dev.muon.dynamic_resource_bars.util.EditModeManager;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;
import dev.muon.dynamic_resource_bars.util.TextBehavior;

public class ArmorBarRenderer {
    private static long armorTextStartTime = 0;
    private static boolean shouldShowArmorText = false;
    
    // Fade behavior tracking
    private static boolean armorBarSetVisible = true;
    private static long armorBarDisabledStartTime = 0L;

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
                return new ScreenRect(x + config.armorBackgroundXOffset, 
                                      y + config.armorBackgroundYOffset,
                                      config.armorBackgroundWidth,
                                      config.armorBackgroundHeight);
            case BAR_MAIN:
                return new ScreenRect(x + config.armorBarXOffset,
                                      y + config.armorBarYOffset,
                                      config.armorBarWidth,
                                      config.armorBarHeight);
            case TEXT:
                // Text area now positioned relative to complexRect, using armorBarWidth/Height for its dimensions
                return new ScreenRect(x + config.armorTextXOffset, 
                                      y + config.armorTextYOffset, 
                                      config.armorBarWidth, 
                                      config.armorBarHeight);
            case ICON:
                // Icon positioned relative to complexRect top-left
                return new ScreenRect(x + config.armorIconXOffset, 
                                      y + config.armorIconYOffset, 
                                      config.armorIconSize, 
                                      config.armorIconSize);
            default:
                return new ScreenRect(0, 0, 0, 0);
        }
    }

    public static void render(GuiGraphics graphics, Player player) {
        ClientConfig config = ModConfigManager.getClient();
        if (config.armorBarBehavior != BarRenderBehavior.CUSTOM) {
            return;
        }
        
        int armorValue = player.getArmorValue();
        
        // Use a dummy value in edit mode for visibility
        if (EditModeManager.isEditModeEnabled() && armorValue == 0) {
            armorValue = config.maxExpectedArmor; 
        }
        
        // Set visibility based on armor value (fade when empty) unless in edit mode
        setArmorBarVisibility(armorValue > 0 || EditModeManager.isEditModeEnabled());
        
        // Don't render if fully faded and not in edit mode
        if (!isArmorBarVisible() && !EditModeManager.isEditModeEnabled() && 
            (System.currentTimeMillis() - armorBarDisabledStartTime) > RenderUtil.BAR_FADEOUT_DURATION) {
            return;
        }
        
        // Get current alpha for rendering
        float currentAlphaForRender = getArmorBarAlpha();
        if (EditModeManager.isEditModeEnabled() && !isArmorBarVisible()) {
            currentAlphaForRender = 1.0f; // Show fully in edit mode
        }
        
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentAlphaForRender);
        
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
                DynamicResourceBars.loc("textures/gui/armor_background.png"), 
                xPos + config.armorBackgroundXOffset, 
                yPos + config.armorBackgroundYOffset, 
                0, 0, backgroundWidth, backgroundHeight, 256, 256
        );

        float armorPercent = Math.min(1.0f, (float) armorValue / config.maxExpectedArmor);

        int filledWidth = Math.round((barWidth - (float) iconSize / 2) * armorPercent);
        if (filledWidth > 0) {
            int barX = xPos + barOnlyXOffset;
            // Determine the actual width of the bar texture portion, considering the icon.
            float actualBarTexturePortionWidth = config.armorBarWidth;
            if (config.enableArmorIcon) {
                actualBarTexturePortionWidth -= (float)config.armorIconSize / 2.0f;
            }

            int uTexOffset = 0;
            if (config.enableArmorIcon) {
                if (isRightAnchored) {
                    barX += barWidth - filledWidth - iconSize / 2;
                    uTexOffset = Math.round(actualBarTexturePortionWidth) - filledWidth;
                } else {
                    barX += iconSize / 2;
                    // uTexOffset remains 0 for left-anchored
                }
            } else { // No icon
                if (isRightAnchored) {
                    barX += barWidth - filledWidth;
                    uTexOffset = config.armorBarWidth - filledWidth;
                }
                // else: barX is xPos + barOnlyXOffset, uTexOffset is 0 for left-anchored
            }
            if (uTexOffset < 0) uTexOffset = 0; // Prevent negative texture offset

            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/armor_bar.png"),
                    barX,
                    yPos + barOnlyYOffset,
                    uTexOffset, 0, // Use calculated uTexOffset, vOffset is 0 for armor bar
                    filledWidth,
                    barHeight,
                    256, 256
            );
        }

        if (shouldRenderText() || EditModeManager.isEditModeEnabled()) {
            ScreenRect textRect = getSubElementRect(SubElementType.TEXT, player);
            int textX = textRect.x() + (textRect.width() / 2);
            int textY = textRect.y() + (textRect.height() / 2);
            int color = getTextColor();
            HorizontalAlignment alignment = config.armorTextAlign;
            
            int baseX = textRect.x();
            if (alignment == HorizontalAlignment.CENTER) {
                baseX = textX;
            } else if (alignment == HorizontalAlignment.RIGHT) {
                baseX = textRect.x() + textRect.width();
            }

            RenderUtil.renderArmorText(armorValue,
                    graphics, baseX, textY, color, alignment);
        }
        renderProtectionOverlay(graphics, player, config, xPos, yPos, barWidth, barHeight, barOnlyXOffset, barOnlyYOffset, iconSize);

        if (config.enableArmorIcon || EditModeManager.isEditModeEnabled()) {
            ArmorIcon icon = ArmorIcon.fromArmorValue(armorValue);
            ScreenRect iconRect = getSubElementRect(SubElementType.ICON, player);
            
            graphics.blit(
                    DynamicResourceBars.loc("textures/gui/armors/" + icon.getTexture() + ".png"),
                    iconRect.x(),
                    iconRect.y(),
                    0, 0,
                    iconRect.width(), iconRect.height(),
                    iconRect.width(), iconRect.height()
            );
        }
        
        // Add focus mode outline rendering
        if (EditModeManager.isEditModeEnabled()) {
            ScreenRect complexRect = getScreenRect(player);
            if (EditModeManager.getFocusedElement() == dev.muon.dynamic_resource_bars.util.DraggableElement.ARMOR_BAR) {
                int focusedBorderColor = 0xA0FFFF00;
                ScreenRect bgRect = getSubElementRect(SubElementType.BACKGROUND, player);
                graphics.renderOutline(bgRect.x()-1, bgRect.y()-1, bgRect.width()+2, bgRect.height()+2, focusedBorderColor);
                
                ScreenRect barRect = getSubElementRect(SubElementType.BAR_MAIN, player);
                graphics.renderOutline(barRect.x()-1, barRect.y()-1, barRect.width()+2, barRect.height()+2, 0xA0C0C0C0);
                
                graphics.renderOutline(complexRect.x()-2, complexRect.y()-2, complexRect.width()+4, complexRect.height()+4, 0x80FFFFFF);
            } else {
                int borderColor = 0x80FFFFFF;
                graphics.renderOutline(complexRect.x()-1, complexRect.y()-1, complexRect.width()+2, complexRect.height()+2, borderColor);
            }
        }
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
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

        int maxProtection = config.maxExpectedProt;
        float protectionScale = Math.min(1.0f, (float)totalProtection / maxProtection);
        
        // Use the same flash alpha system as AppleSkin overlays
        float pulseAlpha = 0.5f + (TickHandler.getOverlayFlashAlpha() * 0.5f); // Range from 0.5 to 1.0
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, pulseAlpha);
        
        int adjustedBarWidth = config.enableArmorIcon ?
                barWidth - (iconSize / 2) : barWidth;
        int overlayWidth = (int)(adjustedBarWidth * protectionScale);

        graphics.blit(
                DynamicResourceBars.loc("textures/gui/protection_overlay.png"),
                xPos + (config.enableArmorIcon ? barOnlyXOffset + iconSize / 2 : barOnlyXOffset),
                yPos + barOnlyYOffset,
                0, 0,
                overlayWidth, barHeight,
                256, 256
        );

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }


    private static int getTextColor() {
        ClientConfig config = ModConfigManager.getClient();
        long timeSinceTextTrigger = armorTextStartTime > 0 ?
                System.currentTimeMillis() - armorTextStartTime : 0;

        int baseColor = config.armorTextColor & 0xFFFFFF;
        int alpha = config.armorTextOpacity;
        
        if (!shouldShowArmorText && armorTextStartTime > 0) {
            alpha = (int)(alpha * (RenderUtil.calculateTextAlpha(timeSinceTextTrigger) / (float)RenderUtil.BASE_TEXT_ALPHA));
        }
        
        alpha = (int) (alpha * getArmorBarAlpha()); // Modulate with bar alpha
        alpha = Math.max(10, Math.min(255, alpha));

        return (alpha << 24) | baseColor;
    }

    private static boolean shouldRenderText() {
        if (EditModeManager.isEditModeEnabled()) {
            return true;
        }
        
        TextBehavior behavior = ModConfigManager.getClient().showArmorText;
        if (EditModeManager.isEditModeEnabled()) {
            if (behavior == TextBehavior.ALWAYS || behavior == TextBehavior.WHEN_NOT_FULL) {
                return true;
            }
        }
        if (behavior == TextBehavior.NEVER) {
            return false;
        }
        if (behavior == TextBehavior.ALWAYS) {
            return true;
        }
        
        // WHEN_NOT_FULL logic - for armor, we can interpret this as "when armor is not 0"
        int armorValue = Minecraft.getInstance().player.getArmorValue();
        boolean hasArmor = armorValue > 0;
        
        if (!hasArmor) {
            if (armorTextStartTime == 0) { // Just lost armor
                armorTextStartTime = System.currentTimeMillis();
            }
            // Show for a short duration after losing armor
            return (System.currentTimeMillis() - armorTextStartTime) < RenderUtil.TEXT_DISPLAY_DURATION;
        } else {
            armorTextStartTime = 0; // Reset timer when armor is present
            return true; // Has armor, so show
        }
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

    // New fade behavior methods
    private static void setArmorBarVisibility(boolean visible) {
        if (armorBarSetVisible != visible) {
            if (!visible) {
                armorBarDisabledStartTime = System.currentTimeMillis();
            }
            armorBarSetVisible = visible;
        }
    }

    private static boolean isArmorBarVisible() {
        return armorBarSetVisible;
    }

    private static float getArmorBarAlpha() {
        if (isArmorBarVisible()) {
            return 1.0f;
        }
        long timeSinceDisabled = System.currentTimeMillis() - armorBarDisabledStartTime;
        if (timeSinceDisabled >= RenderUtil.BAR_FADEOUT_DURATION) {
            return 0.0f;
        }
        return Math.max(0.0f, 1.0f - (timeSinceDisabled / (float) RenderUtil.BAR_FADEOUT_DURATION));
    }
}