package dev.muon.dynamic_resource_bars.config.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Single-select picker for an enum-valued config option.
 *
 * <p>Renders one button per option and a Cancel button. The currently-selected option's
 * button is deactivated so the user can see what's already set without it being clickable.
 * Choosing any other option calls {@code setter}, which is responsible for saving the
 * config (and any side-effects like {@code updateActiveProvider}); the screen then closes
 * back to the parent.
 */
public class EnumSelectScreen<T> extends Screen {

    private final Screen parent;
    private final List<T> options;
    private final Function<T, Component> labelFn;
    private final Supplier<T> getter;
    private final Consumer<T> setter;

    public EnumSelectScreen(Screen parent, Component title, List<T> options,
                            Function<T, Component> labelFn,
                            Supplier<T> getter, Consumer<T> setter) {
        super(title);
        this.parent = parent;
        this.options = options;
        this.labelFn = labelFn;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 5;
        int x = (this.width - buttonWidth) / 2;
        int rows = options.size() + 1; // options + cancel
        int blockHeight = rows * buttonHeight + (rows - 1) * spacing + spacing;
        int startY = Math.max(40, (this.height - blockHeight) / 2);

        T current = getter.get();
        for (int i = 0; i < options.size(); i++) {
            T opt = options.get(i);
            int y = startY + i * (buttonHeight + spacing);
            Button btn = Button.builder(labelFn.apply(opt), b -> {
                setter.accept(opt);
                onClose();
            }).bounds(x, y, buttonWidth, buttonHeight).build();
            btn.active = !Objects.equals(opt, current);
            addRenderableWidget(btn);
        }

        int cancelY = startY + options.size() * (buttonHeight + spacing) + spacing;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x, cancelY, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
