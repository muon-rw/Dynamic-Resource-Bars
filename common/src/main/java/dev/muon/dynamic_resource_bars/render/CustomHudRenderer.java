package dev.muon.dynamic_resource_bars.render;

import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public final class CustomHudRenderer {

    private static final Identifier HUD_HOTBAR = Constants.loc("textures/gui/hud_hotbar.png");
    private static final Identifier HUD_OFFHAND_LEFT = Constants.loc("textures/gui/hud_offhand_left.png");
    private static final Identifier HUD_OFFHAND_RIGHT = Constants.loc("textures/gui/hud_offhand_right.png");
    private static final Identifier HUD_XP_BACKGROUND = Constants.loc("textures/gui/hud_xp_background.png");
    private static final Identifier HUD_XP_BAR = Constants.loc("textures/gui/hud_xp_bar.png");
    private static final Identifier HUD_JUMP_BAR = Constants.loc("textures/gui/hud_jump_bar.png");
    private static final Identifier HUD_JUMP_BAR_COOLDOWN = Constants.loc("textures/gui/hud_jump_bar_cooldown.png");
    private static final Identifier HUD_LEVEL_BADGE = Constants.loc("textures/gui/hud_level_badge.png");

    private static final Identifier HOTBAR_SELECTION_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_selection");
    private static final Identifier HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_background");
    private static final Identifier HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_progress");

    private static final int HOTBAR_TEX_WIDTH = 194;
    private static final int HOTBAR_TEX_HEIGHT = 26;
    private static final int OFFHAND_TEX_WIDTH = 27;
    private static final int OFFHAND_TEX_HEIGHT = 26;
    private static final int XP_BG_WIDTH = 182;
    private static final int XP_BG_HEIGHT = 16;
    private static final int XP_FILL_WIDTH = 156;
    private static final int XP_FILL_HEIGHT = 3;
    private static final int LEVEL_BADGE_WIDTH = 36;
    private static final int LEVEL_BADGE_HEIGHT = 14;

    private CustomHudRenderer() {}

    public static boolean shouldReplaceHotbar() {
        if (!ModConfigManager.getClient().enableCustomHud) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.gameMode == null) return false;
        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return false;
        return mc.getCameraEntity() instanceof Player;
    }

    public static boolean shouldReplaceInfoBar() {
        if (!ModConfigManager.getClient().enableCustomHud) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.gameMode == null) return false;
        return mc.player.jumpableVehicle() != null || mc.gameMode.hasExperience();
    }

    public static boolean shouldReplaceExperienceLevel() {
        if (!ModConfigManager.getClient().enableCustomHud) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.gameMode == null) return false;
        return mc.gameMode.hasExperience() && mc.player.experienceLevel > 0;
    }

    public static void renderHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getCameraEntity() instanceof Player player)) return;

        ItemStack offhand = player.getOffhandItem();
        HumanoidArm offhandArm = player.getMainArm().getOpposite();
        int screenCenter = graphics.guiWidth() / 2;
        int guiHeight = graphics.guiHeight();

        graphics.blit(RenderPipelines.GUI_TEXTURED, HUD_HOTBAR,
                screenCenter - 97, guiHeight - 25, 0, 0,
                HOTBAR_TEX_WIDTH, HOTBAR_TEX_HEIGHT, HOTBAR_TEX_WIDTH, HOTBAR_TEX_HEIGHT, -1);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE,
                screenCenter - 91 - 1 + player.getInventory().getSelectedSlot() * 20,
                guiHeight - 22 - 1, 24, 23);

        if (!offhand.isEmpty()) {
            if (offhandArm == HumanoidArm.LEFT) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, HUD_OFFHAND_LEFT,
                        screenCenter - 123, guiHeight - 25, 0, 0,
                        OFFHAND_TEX_WIDTH, OFFHAND_TEX_HEIGHT, OFFHAND_TEX_WIDTH, OFFHAND_TEX_HEIGHT, -1);
            } else {
                graphics.blit(RenderPipelines.GUI_TEXTURED, HUD_OFFHAND_RIGHT,
                        screenCenter + 96, guiHeight - 25, 0, 0,
                        OFFHAND_TEX_WIDTH, OFFHAND_TEX_HEIGHT, OFFHAND_TEX_WIDTH, OFFHAND_TEX_HEIGHT, -1);
            }
        }

        int seed = 1;
        int itemY = guiHeight - 19;
        for (int i = 0; i < 9; i++) {
            int itemX = screenCenter - 90 + i * 20 + 2;
            renderSlot(graphics, deltaTracker, player, player.getInventory().getItem(i), itemX, itemY, seed++);
        }

        if (!offhand.isEmpty()) {
            int offhandX = offhandArm == HumanoidArm.LEFT ? screenCenter - 91 - 26 : screenCenter + 91 + 10;
            renderSlot(graphics, deltaTracker, player, offhand, offhandX, itemY, seed);
        }

        if (mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR && mc.player != null) {
            float strength = mc.player.getAttackStrengthScale(0.0F);
            if (strength < 1.0F) {
                int indY = guiHeight - 20;
                int indX = offhandArm == HumanoidArm.RIGHT ? screenCenter - 91 - 22 : screenCenter + 91 + 6;
                int progress = (int) (strength * 19.0F);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE, indX, indY, 18, 18);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE, 18, 18, 0, 18 - progress, indX, indY + 18 - progress, 18, progress);
            }
        }
    }

    public static void renderInfoBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;

        int screenCenter = graphics.guiWidth() / 2;
        int guiHeight = graphics.guiHeight();
        int bgLeft = screenCenter - 91;
        int bgTop = guiHeight - 39;
        int fillLeft = screenCenter - 78;
        int fillTop = guiHeight - 29;

        PlayerRideableJumping vehicle = player.jumpableVehicle();
        if (vehicle != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, HUD_XP_BACKGROUND,
                    bgLeft, bgTop, 0, 0,
                    XP_BG_WIDTH, XP_BG_HEIGHT, XP_BG_WIDTH, XP_BG_HEIGHT, -1);
            if (vehicle.getJumpCooldown() > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, HUD_JUMP_BAR_COOLDOWN,
                        fillLeft, fillTop, 0, 0,
                        XP_FILL_WIDTH, XP_FILL_HEIGHT, XP_FILL_WIDTH, XP_FILL_HEIGHT, -1);
            } else {
                int filled = Mth.lerpDiscrete(player.getJumpRidingScale(), 0, XP_FILL_WIDTH);
                if (filled > 0) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, HUD_JUMP_BAR,
                            fillLeft, fillTop, 0, 0,
                            filled, XP_FILL_HEIGHT, XP_FILL_WIDTH, XP_FILL_HEIGHT, -1);
                }
            }
            return;
        }

        if (mc.gameMode.hasExperience()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, HUD_XP_BACKGROUND,
                    bgLeft, bgTop, 0, 0,
                    XP_BG_WIDTH, XP_BG_HEIGHT, XP_BG_WIDTH, XP_BG_HEIGHT, -1);
            if (player.getXpNeededForNextLevel() > 0) {
                int filled = (int) (player.experienceProgress * (float) (XP_FILL_WIDTH + 1));
                filled = Math.min(XP_FILL_WIDTH, filled);
                if (filled > 0) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, HUD_XP_BAR,
                            fillLeft, fillTop, 0, 0,
                            filled, XP_FILL_HEIGHT, XP_FILL_WIDTH, XP_FILL_HEIGHT, -1);
                }
            }
        }
    }

    public static void renderExperienceLevel(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;
        int level = player.experienceLevel;
        if (!mc.gameMode.hasExperience() || level <= 0) return;

        int screenCenter = graphics.guiWidth() / 2;
        int guiHeight = graphics.guiHeight();

        graphics.blit(RenderPipelines.GUI_TEXTURED, HUD_LEVEL_BADGE,
                screenCenter - LEVEL_BADGE_WIDTH / 2, guiHeight - 45, 0, 0,
                LEVEL_BADGE_WIDTH, LEVEL_BADGE_HEIGHT, LEVEL_BADGE_WIDTH, LEVEL_BADGE_HEIGHT, -1);

        Font font = mc.font;
        Component text = Component.literal(String.valueOf(level));
        int textX = screenCenter - font.width(text) / 2;
        int textY = guiHeight - 42;
        graphics.text(font, text, textX + 1, textY, 0xFF10141F, false);
        graphics.text(font, text, textX - 1, textY, 0xFF10141F, false);
        graphics.text(font, text, textX, textY + 1, 0xFF10141F, false);
        graphics.text(font, text, textX, textY - 1, 0xFF10141F, false);
        graphics.text(font, text, textX, textY, 0xFFCFFF70, false);
    }

    private static void renderSlot(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
                                   Player player, ItemStack stack, int x, int y, int seed) {
        if (stack.isEmpty()) return;
        float pop = (float) stack.getPopTime() - deltaTracker.getGameTimeDeltaPartialTick(false);
        if (pop > 0.0F) {
            float squeeze = 1.0F + pop / 5.0F;
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) (x + 8), (float) (y + 12));
            graphics.pose().scale(1.0F / squeeze, (squeeze + 1.0F) / 2.0F);
            graphics.pose().translate((float) (-(x + 8)), (float) (-(y + 12)));
        }
        graphics.item(player, stack, x, y, seed);
        if (pop > 0.0F) {
            graphics.pose().popMatrix();
        }
        graphics.itemDecorations(Minecraft.getInstance().font, stack, x, y);
    }
}
