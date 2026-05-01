package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Hub config screen — opens the HUD editor (only when in a world) and exposes the global
 * text knobs that aren't tied to a specific bar.
 */
public class ModConfigScreen extends Screen {

    private final Screen parentScreen;

    private EditBox textColorBox;
    private EditBox textOpacityBox;
    private EditBox textSizeBox;

    public ModConfigScreen(Screen parent) {
        super(Component.literal(Constants.MOD_NAME + " Configuration"));
        this.parentScreen = parent;
    }

    @Override
    protected void init() {
        super.init();
        ClientConfig config = ModConfigManager.getClient();

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int currentY = this.height / 4;

        Minecraft mc = Minecraft.getInstance();
        boolean canEditHUD = mc.level != null;

        Button editHud = Button.builder(
                        Component.translatable("gui.dynamic_resource_bars.config.button.open_hud_editor"),
                        b -> { if (b.active) Minecraft.getInstance().setScreen(new HudEditorScreen(this)); })
                .bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight)
                .build();
        editHud.active = canEditHUD;
        if (!canEditHUD) {
            editHud.setTooltip(Tooltip.create(
                    Component.translatable("gui.dynamic_resource_bars.config.tooltip.hud_editor_disabled")));
        }
        this.addRenderableWidget(editHud);

        currentY += buttonHeight + 15;

        int labelWidth = 80;
        int editBoxWidth = 60;
        int blockWidth = labelWidth + editBoxWidth + 5;
        int startX = centerX - blockWidth / 2;
        int editX = startX + labelWidth + 5;

        textColorBox = new EditBox(this.font, editX, currentY, editBoxWidth, buttonHeight, Component.empty());
        textColorBox.setValue(String.format("%06X", config.globalTextColor & 0xFFFFFF));
        textColorBox.setMaxLength(6);
        textColorBox.setResponder(text -> {
            try {
                int color = Integer.parseInt(text, 16);
                config.globalTextColor = color | 0xFF000000;
                textColorBox.setTextColor(0xE0E0E0);
            } catch (NumberFormatException e) {
                textColorBox.setTextColor(0xFF5555);
            }
        });
        this.addRenderableWidget(textColorBox);
        currentY += buttonHeight + 5;

        textOpacityBox = new EditBox(this.font, editX, currentY, editBoxWidth, buttonHeight, Component.empty());
        textOpacityBox.setValue(String.valueOf(config.globalTextOpacity));
        textOpacityBox.setMaxLength(3);
        textOpacityBox.setResponder(text -> {
            try {
                int opacity = Integer.parseInt(text);
                if (opacity >= 0 && opacity <= 255) {
                    config.globalTextOpacity = opacity;
                    textOpacityBox.setTextColor(0xE0E0E0);
                } else {
                    textOpacityBox.setTextColor(0xFF5555);
                }
            } catch (NumberFormatException e) {
                textOpacityBox.setTextColor(0xFF5555);
            }
        });
        this.addRenderableWidget(textOpacityBox);
        currentY += buttonHeight + 5;

        textSizeBox = new EditBox(this.font, editX, currentY, editBoxWidth, buttonHeight, Component.empty());
        textSizeBox.setValue(String.format("%.1f", config.globalTextSize));
        textSizeBox.setMaxLength(4);
        textSizeBox.setResponder(text -> {
            try {
                float size = Float.parseFloat(text);
                if (size >= 0.1f && size <= 5.0f) {
                    config.globalTextSize = size;
                    textSizeBox.setTextColor(0xE0E0E0);
                } else {
                    textSizeBox.setTextColor(0xFF5555);
                }
            } catch (NumberFormatException e) {
                textSizeBox.setTextColor(0xFF5555);
            }
        });
        this.addRenderableWidget(textSizeBox);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        b -> onClose())
                .bounds(centerX - buttonWidth / 2, this.height - buttonHeight - 20, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

        if (textColorBox == null) return;
        int labelX = textColorBox.getX() - 95;
        drawLabel(graphics, "gui.dynamic_resource_bars.config.label.text_color", labelX, textColorBox);
        drawLabel(graphics, "gui.dynamic_resource_bars.config.label.text_opacity", labelX, textOpacityBox);
        drawLabel(graphics, "gui.dynamic_resource_bars.config.label.text_size", labelX, textSizeBox);
    }

    private void drawLabel(GuiGraphicsExtractor graphics, String key, int x, EditBox box) {
        int y = box.getY() + (box.getHeight() - this.font.lineHeight) / 2;
        graphics.text(this.font, Component.translatable(key), x, y, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        ModConfigManager.getClient().save();
        if (this.minecraft != null) this.minecraft.setScreen(this.parentScreen);
    }
}
