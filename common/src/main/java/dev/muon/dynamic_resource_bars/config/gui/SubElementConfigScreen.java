package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class SubElementConfigScreen extends Screen {

    private static final float SCALE_STEP = 0.05f;
    private static final float MIN_SCALE = 0.1f;
    private static final float MAX_SCALE = 5.0f;

    private final Screen parent;
    private final DraggableElement element;
    private final SubElementType sub;
    private final BarFieldAccess access;

    private EditBox xBox, yBox;
    private EditBox widthBox, heightBox;
    private EditBox scaleBox;

    private final int origX, origY, origW, origH;
    private final float origScale;

    public SubElementConfigScreen(Screen parent, DraggableElement element, SubElementType sub) {
        super(Component.translatable("gui.dynamic_resource_bars.context.sub_title",
                Component.translatable(element.getTranslationKey()),
                Component.translatable(sub.getTranslationKey())));
        this.parent = parent;
        this.element = element;
        this.sub = sub;
        this.access = BarFieldAccess.forElement(element);
        ClientConfig c = ModConfigManager.getClient();
        if (this.access != null) {
            this.origX = access.subElementX(c, sub);
            this.origY = access.subElementY(c, sub);
            this.origW = access.subElementWidth(c, sub);
            this.origH = access.subElementHeight(c, sub);
            this.origScale = access.subElementTextScale(c, sub);
        } else {
            this.origX = 0;
            this.origY = 0;
            this.origW = 1;
            this.origH = 1;
            this.origScale = ClientConfig.DEFAULT_TEXT_SCALE;
        }
    }

    @Override
    protected void init() {
        super.init();
        if (access == null) {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
            return;
        }
        ClientConfig c = ModConfigManager.getClient();

        int boxWidth = 56;
        int boxHeight = 18;
        int rowSpacing = 4;
        boolean hasScale = access.hasTextScale(sub);
        int rows = 2 + (sub.isResizable() ? 2 : 0) + (hasScale ? 1 : 0);
        int totalRowsHeight = rows * boxHeight + (rows - 1) * rowSpacing;
        int topLimit = 30;
        int bottomLimit = this.height - 36;
        int y = Math.max(topLimit, topLimit + (bottomLimit - topLimit - totalRowsHeight) / 2);
        int editX = this.width / 2 - 4;

        xBox = intBox(editX, y, boxWidth, boxHeight, access.subElementX(c, sub), -2048, 2048);
        addRenderableWidget(xBox);
        y += boxHeight + rowSpacing;

        yBox = intBox(editX, y, boxWidth, boxHeight, access.subElementY(c, sub), -2048, 2048);
        addRenderableWidget(yBox);

        if (sub.isResizable()) {
            y += boxHeight + rowSpacing;
            widthBox = intBox(editX, y, boxWidth, boxHeight, access.subElementWidth(c, sub), 1, 512);
            addRenderableWidget(widthBox);
            y += boxHeight + rowSpacing;
            heightBox = intBox(editX, y, boxWidth, boxHeight, access.subElementHeight(c, sub), 1, 512);
            addRenderableWidget(heightBox);
        }

        if (hasScale) {
            y += boxHeight + rowSpacing;
            scaleBox = FieldEditorUtils.floatBox(this.font, editX, y, boxWidth, boxHeight,
                    access.subElementTextScale(c, sub), MIN_SCALE, MAX_SCALE);
            addRenderableWidget(scaleBox);

            int stepBtnW = 18;
            addRenderableWidget(Button.builder(Component.literal("-"), b -> stepScale(-SCALE_STEP))
                    .bounds(editX + boxWidth + 3, y, stepBtnW, boxHeight)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> stepScale(SCALE_STEP))
                    .bounds(editX + boxWidth + 3 + stepBtnW + 2, y, stepBtnW, boxHeight)
                    .build());
        }

        int btnWidth = 76;
        int btnGap = 6;
        int totalBtnsW = btnWidth * 3 + btnGap * 2;
        int btnStartX = (this.width - totalBtnsW) / 2;
        int btnY = this.height - boxHeight - 10;

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.dynamic_resource_bars.context.reset_sub_element"),
                        b -> resetToDefaults())
                .bounds(btnStartX, btnY, btnWidth, boxHeight)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cancel"),
                        b -> revertAndClose())
                .bounds(btnStartX + btnWidth + btnGap, btnY, btnWidth, boxHeight)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        b -> { applyAll(); onClose(); })
                .bounds(btnStartX + (btnWidth + btnGap) * 2, btnY, btnWidth, boxHeight)
                .build());
    }

    private void stepScale(float delta) {
        if (scaleBox == null) return;
        ClientConfig c = ModConfigManager.getClient();
        float current = FieldEditorUtils.parseFloatSafely(scaleBox, access.subElementTextScale(c, sub), MIN_SCALE, MAX_SCALE);
        float next = Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.round((current + delta) * 20f) / 20f));
        scaleBox.setValue(FieldEditorUtils.formatScale(next));
        access.setSubElementTextScale(c, sub, next);
    }

    private void resetToDefaults() {
        ClientConfig defaults = ClientConfig.createDefaults();
        ClientConfig c = ModConfigManager.getClient();
        access.resetSubElement(c, defaults, sub);
        if (xBox != null) xBox.setValue(String.valueOf(access.subElementX(c, sub)));
        if (yBox != null) yBox.setValue(String.valueOf(access.subElementY(c, sub)));
        if (widthBox != null) widthBox.setValue(String.valueOf(access.subElementWidth(c, sub)));
        if (heightBox != null) heightBox.setValue(String.valueOf(access.subElementHeight(c, sub)));
        if (scaleBox != null) scaleBox.setValue(FieldEditorUtils.formatScale(access.subElementTextScale(c, sub)));
    }

    private void revertAndClose() {
        ClientConfig c = ModConfigManager.getClient();
        access.setSubElementX(c, sub, origX);
        access.setSubElementY(c, sub, origY);
        if (sub.isResizable()) {
            access.setSubElementWidth(c, sub, origW);
            access.setSubElementHeight(c, sub, origH);
        }
        if (access.hasTextScale(sub)) {
            access.setSubElementTextScale(c, sub, origScale);
        }
        c.save();
        onClose();
    }

    private EditBox intBox(int x, int y, int w, int h, int initial, int min, int max) {
        return FieldEditorUtils.intBox(this.font, x, y, w, h, initial, min, max);
    }

    private int parseSafely(EditBox box, int defaultVal, int min, int max) {
        return FieldEditorUtils.parseSafely(box, defaultVal, min, max);
    }

    private void applyAll() {
        ClientConfig c = ModConfigManager.getClient();
        access.setSubElementX(c, sub, parseSafely(xBox, access.subElementX(c, sub), -2048, 2048));
        access.setSubElementY(c, sub, parseSafely(yBox, access.subElementY(c, sub), -2048, 2048));
        if (sub.isResizable()) {
            access.setSubElementWidth(c, sub, parseSafely(widthBox, access.subElementWidth(c, sub), 1, 512));
            access.setSubElementHeight(c, sub, parseSafely(heightBox, access.subElementHeight(c, sub), 1, 512));
        }
        if (access.hasTextScale(sub)) {
            access.setSubElementTextScale(c, sub,
                    FieldEditorUtils.parseFloatSafely(scaleBox, access.subElementTextScale(c, sub), MIN_SCALE, MAX_SCALE));
        }
        c.save();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        if (xBox == null) return;
        FieldEditorUtils.drawLabel(graphics, this.font, "gui.dynamic_resource_bars.manual.x_offset", 0, xBox);
        FieldEditorUtils.drawLabel(graphics, this.font, "gui.dynamic_resource_bars.manual.y_offset", 0, yBox);
        if (sub.isResizable()) {
            FieldEditorUtils.drawLabel(graphics, this.font, "gui.dynamic_resource_bars.manual.width", 0, widthBox);
            FieldEditorUtils.drawLabel(graphics, this.font, "gui.dynamic_resource_bars.manual.height", 0, heightBox);
        }
        if (scaleBox != null) {
            FieldEditorUtils.drawLabel(graphics, this.font, "gui.dynamic_resource_bars.manual.text_scale", 0, scaleBox);
        }
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
