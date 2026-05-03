package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Per-bar visibility toggle screen — one button per {@link SubElementType#isToggleable()}
 * sub-element that the bar actually exposes. Sub-elements that opt out (BAR_MAIN, TEXT,
 * ABSORPTION_TEXT) don't appear here; text in particular has its own {@code TextBehavior}
 * setting reachable from the bar context menu, hence the help line at the bottom.
 *
 * <p>ICON is only shown for bars that have an icon (armor/air) — for icon-less bars the
 * default no-op {@code BarFieldAccess.iconWidth} returns 0, which we use as the
 * "this bar has no such layer" signal.
 */
public class ToggleLayersScreen extends Screen {

    private final Screen parent;
    private final BarFieldAccess access;

    public ToggleLayersScreen(Screen parent, DraggableElement element) {
        super(Component.translatable("gui.dynamic_resource_bars.toggle_layers.title",
                Component.translatable(element.getTranslationKey())));
        this.parent = parent;
        this.access = BarFieldAccess.forElement(element);
    }

    @Override
    protected void init() {
        super.init();
        if (access == null) {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
            return;
        }

        int buttonWidth = 220;
        int buttonHeight = 20;
        int spacing = 5;
        int x = (this.width - buttonWidth) / 2;
        int y = 50;

        for (SubElementType sub : SubElementType.values()) {
            if (!sub.isToggleable()) continue;
            if (sub == SubElementType.ICON && !access.hasIcon()) continue;
            if (sub == SubElementType.ABSORPTION_TEXT && !access.hasAbsorptionText()) continue;
            Button btn = Button.builder(layerLabel(sub), b -> {
                ClientConfig live = ModConfigManager.getClient();
                access.setEnableSubElement(live, sub, !access.enableSubElement(live, sub));
                live.save();
                b.setMessage(layerLabel(sub));
            }).bounds(x, y, buttonWidth, buttonHeight).build();
            addRenderableWidget(btn);
            y += buttonHeight + spacing;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, this.height - buttonHeight - 16, buttonWidth, buttonHeight)
                .build());
    }

    private Component layerLabel(SubElementType sub) {
        Component name = Component.translatable(sub.getTranslationKey());
        Component status = Component.translatable(access.enableSubElement(ModConfigManager.getClient(), sub)
                ? "gui.dynamic_resource_bars.context.status.shown"
                : "gui.dynamic_resource_bars.context.status.hidden");
        return Component.translatable("gui.dynamic_resource_bars.context.layer_status", name, status);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        // Sit just above the Done button (button top = height - 36, button height 20).
        graphics.centeredText(this.font,
                Component.translatable("gui.dynamic_resource_bars.toggle_layers.text_note"),
                this.width / 2, this.height - 50, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

