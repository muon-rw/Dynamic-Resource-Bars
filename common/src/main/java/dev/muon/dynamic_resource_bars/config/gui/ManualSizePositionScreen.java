package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Manual numeric editor for one bar's full geometry: background/bar/overlay dimensions,
 * the background-relative offsets, the bar's total position on screen, and per-bar extras
 * (armor/air icon X/Y/size, health absorption-text X/Y).
 *
 * <p>Two columns — sizes on the left, position offsets on the right — so the field stack
 * doesn't run off the bottom of the screen at higher GUI scales.
 */
public class ManualSizePositionScreen extends Screen {

    private final Screen parentScreen;
    private final DraggableElement element;
    private final BarFieldAccess access;

    private EditBox bgWidth, bgHeight;
    private EditBox barWidth, barHeight;
    private EditBox overlayWidth, overlayHeight;
    private EditBox textWidth, textHeight;
    private EditBox bgX, bgY;
    private EditBox totalX, totalY;
    private EditBox iconX, iconY, iconWidth, iconHeight;
    private EditBox absorptionTextX, absorptionTextY, absorptionTextWidth, absorptionTextHeight;

    private final boolean hasIcon;
    private final boolean hasAbsorptionText;

    public ManualSizePositionScreen(Screen parent, DraggableElement element) {
        super(Component.translatable("gui.dynamic_resource_bars.manual_size_position.title"));
        this.parentScreen = parent;
        this.element = element;
        this.access = BarFieldAccess.forElement(element);
        this.hasIcon = access != null && access.hasIcon();
        this.hasAbsorptionText = access != null && access.hasAbsorptionText();
    }

    @Override
    protected void init() {
        super.init();
        if (access == null) {
            if (this.minecraft != null) this.minecraft.setScreen(parentScreen);
            return;
        }
        ClientConfig c = ModConfigManager.getClient();

        int boxWidth = 50;
        int boxHeight = 18;
        int labelWidth = 100;
        int rowSpacing = 4;
        int colSpacing = 24;
        int columnWidth = labelWidth + 5 + boxWidth;
        int totalRowWidth = 2 * columnWidth + colSpacing;
        int leftColLabelX = (this.width - totalRowWidth) / 2;
        int leftEditX = leftColLabelX + labelWidth + 5;
        int rightColLabelX = leftColLabelX + columnWidth + colSpacing;
        int rightEditX = rightColLabelX + labelWidth + 5;
        int yStart = 36;
        int groupGap = rowSpacing * 2;

        // Left column — sizes (background, bar, overlay)
        int yLeft = yStart;
        bgWidth = intBox(leftEditX, yLeft, boxWidth, boxHeight, access.bgWidth(c), 1, Integer.MAX_VALUE);
        yLeft += boxHeight + rowSpacing;
        bgHeight = intBox(leftEditX, yLeft, boxWidth, boxHeight, access.bgHeight(c), 1, Integer.MAX_VALUE);
        yLeft += boxHeight + groupGap;
        barWidth = intBox(leftEditX, yLeft, boxWidth, boxHeight, access.barWidth(c), 1, 256);
        yLeft += boxHeight + rowSpacing;
        barHeight = intBox(leftEditX, yLeft, boxWidth, boxHeight, access.barHeight(c), 1, 32);
        yLeft += boxHeight + groupGap;
        overlayWidth = intBox(leftEditX, yLeft, boxWidth, boxHeight, access.overlayWidth(c), 1, 256);
        yLeft += boxHeight + rowSpacing;
        overlayHeight = intBox(leftEditX, yLeft, boxWidth, boxHeight, access.overlayHeight(c), 1, 256);
        yLeft += boxHeight + groupGap;
        textWidth = intBox(leftEditX, yLeft, boxWidth, boxHeight, access.textWidth(c), 1, 256);
        yLeft += boxHeight + rowSpacing;
        textHeight = intBox(leftEditX, yLeft, boxWidth, boxHeight, access.textHeight(c), 1, 64);

        // Right column — positions + per-bar extras
        int yRight = yStart;
        bgX = intBox(rightEditX, yRight, boxWidth, boxHeight, access.bgX(c), Integer.MIN_VALUE, Integer.MAX_VALUE);
        yRight += boxHeight + rowSpacing;
        bgY = intBox(rightEditX, yRight, boxWidth, boxHeight, access.bgY(c), Integer.MIN_VALUE, Integer.MAX_VALUE);
        yRight += boxHeight + groupGap;
        totalX = intBox(rightEditX, yRight, boxWidth, boxHeight, access.totalX(c), Integer.MIN_VALUE, Integer.MAX_VALUE);
        yRight += boxHeight + rowSpacing;
        totalY = intBox(rightEditX, yRight, boxWidth, boxHeight, access.totalY(c), Integer.MIN_VALUE, Integer.MAX_VALUE);

        if (hasIcon) {
            yRight += boxHeight + groupGap;
            iconX = intBox(rightEditX, yRight, boxWidth, boxHeight, access.iconX(c), Integer.MIN_VALUE, Integer.MAX_VALUE);
            yRight += boxHeight + rowSpacing;
            iconY = intBox(rightEditX, yRight, boxWidth, boxHeight, access.iconY(c), Integer.MIN_VALUE, Integer.MAX_VALUE);
            yRight += boxHeight + rowSpacing;
            iconWidth = intBox(rightEditX, yRight, boxWidth, boxHeight, access.iconWidth(c), 1, 64);
            yRight += boxHeight + rowSpacing;
            iconHeight = intBox(rightEditX, yRight, boxWidth, boxHeight, access.iconHeight(c), 1, 64);
        }

        if (hasAbsorptionText) {
            yRight += boxHeight + groupGap;
            absorptionTextX = intBox(rightEditX, yRight, boxWidth, boxHeight, access.absorptionTextX(c), Integer.MIN_VALUE, Integer.MAX_VALUE);
            yRight += boxHeight + rowSpacing;
            absorptionTextY = intBox(rightEditX, yRight, boxWidth, boxHeight, access.absorptionTextY(c), Integer.MIN_VALUE, Integer.MAX_VALUE);
            yRight += boxHeight + rowSpacing;
            absorptionTextWidth = intBox(rightEditX, yRight, boxWidth, boxHeight, access.absorptionTextWidth(c), 1, 256);
            yRight += boxHeight + rowSpacing;
            absorptionTextHeight = intBox(rightEditX, yRight, boxWidth, boxHeight, access.absorptionTextHeight(c), 1, 64);
        }

        addRenderableWidget(bgWidth);
        addRenderableWidget(bgHeight);
        addRenderableWidget(barWidth);
        addRenderableWidget(barHeight);
        addRenderableWidget(overlayWidth);
        addRenderableWidget(overlayHeight);
        addRenderableWidget(textWidth);
        addRenderableWidget(textHeight);
        addRenderableWidget(bgX);
        addRenderableWidget(bgY);
        addRenderableWidget(totalX);
        addRenderableWidget(totalY);
        if (hasIcon) {
            addRenderableWidget(iconX);
            addRenderableWidget(iconY);
            addRenderableWidget(iconWidth);
            addRenderableWidget(iconHeight);
        }
        if (hasAbsorptionText) {
            addRenderableWidget(absorptionTextX);
            addRenderableWidget(absorptionTextY);
            addRenderableWidget(absorptionTextWidth);
            addRenderableWidget(absorptionTextHeight);
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
        access.setBgWidth(c, parseSafely(bgWidth, access.bgWidth(c), 1, Integer.MAX_VALUE));
        access.setBgHeight(c, parseSafely(bgHeight, access.bgHeight(c), 1, Integer.MAX_VALUE));
        access.setBgX(c, parseSafely(bgX, access.bgX(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
        access.setBgY(c, parseSafely(bgY, access.bgY(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
        access.setBarWidth(c, parseSafely(barWidth, access.barWidth(c), 1, 256));
        access.setBarHeight(c, parseSafely(barHeight, access.barHeight(c), 1, 32));
        access.setOverlayWidth(c, parseSafely(overlayWidth, access.overlayWidth(c), 1, 256));
        access.setOverlayHeight(c, parseSafely(overlayHeight, access.overlayHeight(c), 1, 256));
        access.setTextWidth(c, parseSafely(textWidth, access.textWidth(c), 1, 256));
        access.setTextHeight(c, parseSafely(textHeight, access.textHeight(c), 1, 64));
        access.setTotalX(c, parseSafely(totalX, access.totalX(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
        access.setTotalY(c, parseSafely(totalY, access.totalY(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
        if (hasIcon) {
            access.setIconX(c, parseSafely(iconX, access.iconX(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
            access.setIconY(c, parseSafely(iconY, access.iconY(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
            access.setIconWidth(c, parseSafely(iconWidth, access.iconWidth(c), 1, 64));
            access.setIconHeight(c, parseSafely(iconHeight, access.iconHeight(c), 1, 64));
        }
        if (hasAbsorptionText) {
            access.setAbsorptionTextX(c, parseSafely(absorptionTextX, access.absorptionTextX(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
            access.setAbsorptionTextY(c, parseSafely(absorptionTextY, access.absorptionTextY(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
            access.setAbsorptionTextWidth(c, parseSafely(absorptionTextWidth, access.absorptionTextWidth(c), 1, 256));
            access.setAbsorptionTextHeight(c, parseSafely(absorptionTextHeight, access.absorptionTextHeight(c), 1, 64));
        }
        c.save();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        graphics.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);

        if (bgWidth == null) return;
        int leftLabelX = bgWidth.getX() - 5 - 100;
        drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.background_width", leftLabelX, bgWidth);
        drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.background_height", leftLabelX, bgHeight);
        drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.bar_width", leftLabelX, barWidth);
        drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.bar_height", leftLabelX, barHeight);
        drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.overlay_width", leftLabelX, overlayWidth);
        drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.overlay_height", leftLabelX, overlayHeight);
        drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.text_width", leftLabelX, textWidth);
        drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.text_height", leftLabelX, textHeight);

        int rightLabelX = bgX.getX() - 5 - 100;
        drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.background_x_offset", rightLabelX, bgX);
        drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.background_y_offset", rightLabelX, bgY);
        drawLabel(graphics, "gui.dynamic_resource_bars.manual.x_offset", rightLabelX, totalX);
        drawLabel(graphics, "gui.dynamic_resource_bars.manual.y_offset", rightLabelX, totalY);
        if (hasIcon) {
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.icon_x_offset", rightLabelX, iconX);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.icon_y_offset", rightLabelX, iconY);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.icon_width", rightLabelX, iconWidth);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.icon_height", rightLabelX, iconHeight);
        }
        if (hasAbsorptionText) {
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.absorption_text_x_offset", rightLabelX, absorptionTextX);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.absorption_text_y_offset", rightLabelX, absorptionTextY);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.absorption_text_width", rightLabelX, absorptionTextWidth);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.absorption_text_height", rightLabelX, absorptionTextHeight);
        }
    }

    private void drawLabel(GuiGraphicsExtractor graphics, String key, int x, EditBox box) {
        FieldEditorUtils.drawLabel(graphics, this.font, key, x, box);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
