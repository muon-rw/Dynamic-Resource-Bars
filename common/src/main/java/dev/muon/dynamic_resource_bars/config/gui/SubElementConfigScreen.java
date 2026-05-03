package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Auto-generated per-sub-element editor: shows X/Y always, plus W/H if {@link SubElementType#isResizable()}.
 * Drives {@link BarFieldAccess#subElementX subElement*} accessors, so adding a new sub-element type
 * (or flipping {@code isResizable}) requires no changes here.
 */
public class SubElementConfigScreen extends Screen {

    private final Screen parent;
    private final SubElementType sub;
    private final BarFieldAccess access;

    private EditBox xBox, yBox;
    private EditBox widthBox, heightBox;

    public SubElementConfigScreen(Screen parent, DraggableElement element, SubElementType sub) {
        super(Component.translatable("gui.dynamic_resource_bars.context.sub_title",
                Component.translatable(element.getTranslationKey()),
                Component.translatable(sub.getTranslationKey())));
        this.parent = parent;
        this.sub = sub;
        this.access = BarFieldAccess.forElement(element);
    }

    @Override
    protected void init() {
        super.init();
        if (access == null) {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
            return;
        }
        ClientConfig c = ModConfigManager.getClient();

        int boxWidth = 60;
        int boxHeight = 18;
        int labelWidth = 100;
        int rowSpacing = 6;
        int totalRowWidth = labelWidth + 5 + boxWidth;
        int labelX = (this.width - totalRowWidth) / 2;
        int editX = labelX + labelWidth + 5;
        int y = 50;

        xBox = intBox(editX, y, boxWidth, boxHeight, access.subElementX(c, sub), Integer.MIN_VALUE, Integer.MAX_VALUE);
        addRenderableWidget(xBox);
        y += boxHeight + rowSpacing;
        yBox = intBox(editX, y, boxWidth, boxHeight, access.subElementY(c, sub), Integer.MIN_VALUE, Integer.MAX_VALUE);
        addRenderableWidget(yBox);

        if (sub.isResizable()) {
            y += boxHeight + rowSpacing;
            widthBox = intBox(editX, y, boxWidth, boxHeight, access.subElementWidth(c, sub), 1, 256);
            addRenderableWidget(widthBox);
            y += boxHeight + rowSpacing;
            heightBox = intBox(editX, y, boxWidth, boxHeight, access.subElementHeight(c, sub), 1, 256);
            addRenderableWidget(heightBox);
        }

        int doneWidth = 100;
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        b -> { applyAll(); onClose(); })
                .bounds(this.width / 2 - doneWidth / 2, this.height - boxHeight - 16, doneWidth, boxHeight)
                .build());
    }

    private EditBox intBox(int x, int y, int w, int h, int initial, int min, int max) {
        return FieldEditorUtils.intBox(this.font, x, y, w, h, initial, min, max);
    }

    private int parseSafely(EditBox box, int defaultVal, int min, int max) {
        return FieldEditorUtils.parseSafely(box, defaultVal, min, max);
    }

    private void applyAll() {
        ClientConfig c = ModConfigManager.getClient();
        access.setSubElementX(c, sub, parseSafely(xBox, access.subElementX(c, sub), Integer.MIN_VALUE, Integer.MAX_VALUE));
        access.setSubElementY(c, sub, parseSafely(yBox, access.subElementY(c, sub), Integer.MIN_VALUE, Integer.MAX_VALUE));
        if (sub.isResizable()) {
            access.setSubElementWidth(c, sub, parseSafely(widthBox, access.subElementWidth(c, sub), 1, 256));
            access.setSubElementHeight(c, sub, parseSafely(heightBox, access.subElementHeight(c, sub), 1, 256));
        }
        c.save();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

        if (xBox == null) return;
        int labelX = xBox.getX() - 5 - 100;
        drawLabel(graphics, "gui.dynamic_resource_bars.manual.x_offset", labelX, xBox);
        drawLabel(graphics, "gui.dynamic_resource_bars.manual.y_offset", labelX, yBox);
        if (sub.isResizable()) {
            drawLabel(graphics, "gui.dynamic_resource_bars.manual.width", labelX, widthBox);
            drawLabel(graphics, "gui.dynamic_resource_bars.manual.height", labelX, heightBox);
        }
    }

    private void drawLabel(GuiGraphicsExtractor graphics, String key, int x, EditBox box) {
        FieldEditorUtils.drawLabel(graphics, this.font, key, x, box);
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
