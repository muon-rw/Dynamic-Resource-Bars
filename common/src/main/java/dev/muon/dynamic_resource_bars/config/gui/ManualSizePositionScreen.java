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
    private EditBox bgX, bgY;
    private EditBox totalX, totalY;
    private EditBox iconX, iconY, iconSize;
    private EditBox absorptionTextX, absorptionTextY;

    private final boolean hasIcon;
    private final boolean hasAbsorptionText;

    public ManualSizePositionScreen(Screen parent, DraggableElement element) {
        super(Component.translatable("gui.dynamic_resource_bars.manual_size_position.title"));
        this.parentScreen = parent;
        this.element = element;
        this.access = BarFieldAccess.forElement(element);
        this.hasIcon = element == DraggableElement.ARMOR_BAR || element == DraggableElement.AIR_BAR;
        this.hasAbsorptionText = element == DraggableElement.HEALTH_BAR;
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
            iconSize = intBox(rightEditX, yRight, boxWidth, boxHeight, access.iconSize(c), 1, 64);
        }

        if (hasAbsorptionText) {
            yRight += boxHeight + groupGap;
            absorptionTextX = intBox(rightEditX, yRight, boxWidth, boxHeight, access.absorptionTextX(c), Integer.MIN_VALUE, Integer.MAX_VALUE);
            yRight += boxHeight + rowSpacing;
            absorptionTextY = intBox(rightEditX, yRight, boxWidth, boxHeight, access.absorptionTextY(c), Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        addRenderableWidget(bgWidth);
        addRenderableWidget(bgHeight);
        addRenderableWidget(barWidth);
        addRenderableWidget(barHeight);
        addRenderableWidget(overlayWidth);
        addRenderableWidget(overlayHeight);
        addRenderableWidget(bgX);
        addRenderableWidget(bgY);
        addRenderableWidget(totalX);
        addRenderableWidget(totalY);
        if (hasIcon) {
            addRenderableWidget(iconX);
            addRenderableWidget(iconY);
            addRenderableWidget(iconSize);
        }
        if (hasAbsorptionText) {
            addRenderableWidget(absorptionTextX);
            addRenderableWidget(absorptionTextY);
        }

        int doneWidth = 100;
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        b -> { applyAll(); onClose(); })
                .bounds(this.width / 2 - doneWidth / 2, this.height - boxHeight - 16, doneWidth, boxHeight)
                .build());
    }

    private EditBox intBox(int x, int y, int w, int h, int initial, int min, int max) {
        EditBox box = new EditBox(this.font, x, y, w, h, Component.empty());
        box.setValue(String.valueOf(initial));
        box.setResponder(text -> {
            try {
                int v = Integer.parseInt(text);
                box.setTextColor(v >= min && v <= max ? 0xE0E0E0 : 0xFF5555);
            } catch (NumberFormatException e) {
                box.setTextColor(0xFF5555);
            }
        });
        return box;
    }

    private int parseSafely(EditBox box, int defaultVal, int min, int max) {
        try {
            int v = Integer.parseInt(box.getValue());
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
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
        access.setTotalX(c, parseSafely(totalX, access.totalX(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
        access.setTotalY(c, parseSafely(totalY, access.totalY(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
        if (hasIcon) {
            access.setIconX(c, parseSafely(iconX, access.iconX(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
            access.setIconY(c, parseSafely(iconY, access.iconY(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
            access.setIconSize(c, parseSafely(iconSize, access.iconSize(c), 1, 64));
        }
        if (hasAbsorptionText) {
            access.setAbsorptionTextX(c, parseSafely(absorptionTextX, access.absorptionTextX(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
            access.setAbsorptionTextY(c, parseSafely(absorptionTextY, access.absorptionTextY(c), Integer.MIN_VALUE, Integer.MAX_VALUE));
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

        int rightLabelX = bgX.getX() - 5 - 100;
        drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.background_x_offset", rightLabelX, bgX);
        drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.background_y_offset", rightLabelX, bgY);
        drawLabel(graphics, "gui.dynamic_resource_bars.manual.x_offset", rightLabelX, totalX);
        drawLabel(graphics, "gui.dynamic_resource_bars.manual.y_offset", rightLabelX, totalY);
        if (hasIcon) {
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.icon_x_offset", rightLabelX, iconX);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.icon_y_offset", rightLabelX, iconY);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.icon_size", rightLabelX, iconSize);
        }
        if (hasAbsorptionText) {
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.absorption_text_x_offset", rightLabelX, absorptionTextX);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.absorption_text_y_offset", rightLabelX, absorptionTextY);
        }
    }

    private void drawLabel(GuiGraphicsExtractor graphics, String key, int x, EditBox box) {
        int y = box.getY() + (box.getHeight() - this.font.lineHeight) / 2;
        graphics.text(this.font, Component.translatable(key), x, y, 0xFFFFFFFF, true);
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
