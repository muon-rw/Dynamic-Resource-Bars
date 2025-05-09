package dev.muon.dynamic_resource_bars.client.gui;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// For NeoForge, this would be the screen returned by your IConfigScreenFactory
// For Forge (older systems) or Fabric (with ForgeConfigApiPort), how it's invoked might differ slightly.
public class ModConfigScreen extends Screen {

    private final Screen parentScreen; // The screen that opened this one (e.g., mods list)

    public ModConfigScreen(Screen parent) {
        super(Component.literal(DynamicResourceBars.MODNAME + " Configuration"));
        this.parentScreen = parent;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int currentY = this.height / 4;

        // Button to open the HUD Editor
        Minecraft mc = Minecraft.getInstance();
        boolean canEditHUD = mc.level != null; // Check if world is loaded

        Button editHudButton = Button.builder(
                Component.translatable("gui.dynamic_resource_bars.config.button.open_hud_editor"),
                (button) -> {
                    if (button.active) {
                        this.minecraft.setScreen(new HudEditorScreen(this));
                    }
                })
                .bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight)
                .build();
        
        editHudButton.active = canEditHUD; 
        
        // Add tooltip if disabled
        if (!canEditHUD) {
             editHudButton.setTooltip(Tooltip.create(Component.translatable("gui.dynamic_resource_bars.config.tooltip.hud_editor_disabled")));
        }

        this.addRenderableWidget(editHudButton);

        currentY += buttonHeight + 5;

        // Optional: Button to open ToniLib's default config screen (if it provides one directly)
        // This requires knowing how ToniLib would normally open its screen.
        // For example, if it registers its own screen factory with a specific ID or uses Forge's ConfigurationScreen.
        // If AllConfigs.getScreenFactory() existed and returned a BiFunction<Minecraft, Screen, Screen>:
        /*
        java.util.function.BiFunction<Minecraft, Screen, Screen> toniLibScreenFactory = AllConfigs.getToniLibScreenFactory();
        if (toniLibScreenFactory != null) {
            this.addRenderableWidget(Button.builder(
                Component.literal("Advanced Settings (ToniLib)"),
                (button) -> {
                    this.minecraft.setScreen(toniLibScreenFactory.apply(this.minecraft, this));
                })
                .bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight)
                .build());
            currentY += buttonHeight + 5;
        }
        */

        // Done Button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                (button) -> this.onClose())
                .bounds(centerX - buttonWidth / 2, this.height - buttonHeight - 20, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        #if NEWER_THAN_20_1
            this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        #else
            this.renderBackground(graphics);
        #endif
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        // Render widgets (buttons)
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {    if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
} 