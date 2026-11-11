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

public class ManualSizePositionScreen extends Screen {

    private static final float MIN_SCALE = 0.1f;
    private static final float MAX_SCALE = 5.0f;

    private enum Page { SIZES, OFFSETS }

    private final Screen parentScreen;
    private final DraggableElement element;
    private final BarFieldAccess access;

    private Page activePage = Page.SIZES;

    private EditBox bgWidth, bgHeight;
    private EditBox barWidth, barHeight;
    private EditBox overlayWidth, overlayHeight;
    private EditBox textWidth, textHeight;
    private EditBox textScale;
    private EditBox iconWidth, iconHeight;
    private EditBox absorptionTextWidth, absorptionTextHeight;
    private EditBox absorptionTextScale;

    private EditBox totalX, totalY;
    private EditBox bgX, bgY;
    private EditBox barX, barY;
    private EditBox overlayX, overlayY;
    private EditBox textX, textY;
    private EditBox iconX, iconY;
    private EditBox absorptionTextX, absorptionTextY;

    private final boolean hasIcon;
    private final boolean hasAbsorptionText;

    private final ClientConfig initialSnapshot;

    public ManualSizePositionScreen(Screen parent, DraggableElement element) {
        super(Component.translatable("gui.dynamic_resource_bars.resize.title_format",
                Component.translatable(element.getTranslationKey())));
        this.parentScreen = parent;
        this.element = element;
        this.access = BarFieldAccess.forElement(element);
        this.hasIcon = access != null && access.hasIcon();
        this.hasAbsorptionText = access != null && access.hasAbsorptionText();
        this.initialSnapshot = captureSnapshot(ModConfigManager.getClient(), this.access);
    }

    private static ClientConfig captureSnapshot(ClientConfig src, BarFieldAccess acc) {
        ClientConfig copy = ClientConfig.createDefaults();
        if (src == null || acc == null) return copy;
        for (SubElementType sub : SubElementType.values()) {
            acc.resetSubElement(copy, src, sub);
        }
        acc.setTotalX(copy, acc.totalX(src));
        acc.setTotalY(copy, acc.totalY(src));
        return copy;
    }

    private void revertAndClose() {
        if (access != null && initialSnapshot != null) {
            ClientConfig c = ModConfigManager.getClient();
            for (SubElementType sub : SubElementType.values()) {
                access.resetSubElement(c, initialSnapshot, sub);
            }
            access.setTotalX(c, access.totalX(initialSnapshot));
            access.setTotalY(c, access.totalY(initialSnapshot));
            c.save();
        }
        onClose();
    }

    @Override
    protected void init() {
        super.init();
        if (access == null) {
            if (this.minecraft != null) this.minecraft.setScreen(parentScreen);
            return;
        }
        ClientConfig c = ModConfigManager.getClient();

        int tabWidth = 100;
        int tabHeight = 16;
        int tabGap = 6;
        int tabStartX = (this.width - (tabWidth * 2 + tabGap)) / 2;
        int tabY = 22;

        Button sizesTab = Button.builder(
                        Component.translatable("gui.dynamic_resource_bars.manual.tab.sizes"),
                        b -> switchPage(Page.SIZES))
                .bounds(tabStartX, tabY, tabWidth, tabHeight)
                .build();
        sizesTab.active = activePage != Page.SIZES;
        addRenderableWidget(sizesTab);

        Button offsetsTab = Button.builder(
                        Component.translatable("gui.dynamic_resource_bars.manual.tab.offsets"),
                        b -> switchPage(Page.OFFSETS))
                .bounds(tabStartX + tabWidth + tabGap, tabY, tabWidth, tabHeight)
                .build();
        offsetsTab.active = activePage != Page.OFFSETS;
        addRenderableWidget(offsetsTab);

        int boxWidth = 50;
        int boxHeight = 16;
        int rowSpacing = 4;
        int labelSlot = 116;
        int colSpacing = 18;
        int columnWidth = labelSlot + boxWidth;
        int totalRowWidth = 2 * columnWidth + colSpacing;
        int leftColStart = (this.width - totalRowWidth) / 2;
        int leftEditX = leftColStart + labelSlot;
        int rightEditX = leftColStart + columnWidth + colSpacing + labelSlot;
        int yStart = 44;

        int yL = yStart;
        bgWidth = intBox(leftEditX, yL, boxWidth, boxHeight, access.bgWidth(c), 1, 512);
        yL += boxHeight + rowSpacing;
        bgHeight = intBox(leftEditX, yL, boxWidth, boxHeight, access.bgHeight(c), 1, 512);
        yL += boxHeight + rowSpacing;
        barWidth = intBox(leftEditX, yL, boxWidth, boxHeight, access.barWidth(c), 1, 256);
        yL += boxHeight + rowSpacing;
        barHeight = intBox(leftEditX, yL, boxWidth, boxHeight, access.barHeight(c), 1, 256);
        yL += boxHeight + rowSpacing;
        overlayWidth = intBox(leftEditX, yL, boxWidth, boxHeight, access.overlayWidth(c), 1, 256);
        yL += boxHeight + rowSpacing;
        overlayHeight = intBox(leftEditX, yL, boxWidth, boxHeight, access.overlayHeight(c), 1, 256);

        int yR = yStart;
        textWidth = intBox(rightEditX, yR, boxWidth, boxHeight, access.textWidth(c), 1, 256);
        yR += boxHeight + rowSpacing;
        textHeight = intBox(rightEditX, yR, boxWidth, boxHeight, access.textHeight(c), 1, 128);
        yR += boxHeight + rowSpacing;
        textScale = FieldEditorUtils.floatBox(this.font, rightEditX, yR, boxWidth, boxHeight,
                access.textScale(c), MIN_SCALE, MAX_SCALE);

        if (hasIcon) {
            yR += boxHeight + rowSpacing;
            iconWidth = intBox(rightEditX, yR, boxWidth, boxHeight, access.iconWidth(c), 1, 64);
            yR += boxHeight + rowSpacing;
            iconHeight = intBox(rightEditX, yR, boxWidth, boxHeight, access.iconHeight(c), 1, 64);
        }

        if (hasAbsorptionText) {
            yR += boxHeight + rowSpacing;
            absorptionTextWidth = intBox(rightEditX, yR, boxWidth, boxHeight, access.absorptionTextWidth(c), 1, 256);
            yR += boxHeight + rowSpacing;
            absorptionTextHeight = intBox(rightEditX, yR, boxWidth, boxHeight, access.absorptionTextHeight(c), 1, 128);
            yR += boxHeight + rowSpacing;
            absorptionTextScale = FieldEditorUtils.floatBox(this.font, rightEditX, yR, boxWidth, boxHeight,
                    access.absorptionTextScale(c), MIN_SCALE, MAX_SCALE);
        }

        int yOffL = yStart;
        totalX = intBox(leftEditX, yOffL, boxWidth, boxHeight, access.totalX(c), -2048, 2048);
        yOffL += boxHeight + rowSpacing;
        totalY = intBox(leftEditX, yOffL, boxWidth, boxHeight, access.totalY(c), -2048, 2048);
        yOffL += boxHeight + rowSpacing;
        bgX = intBox(leftEditX, yOffL, boxWidth, boxHeight, access.bgX(c), -2048, 2048);
        yOffL += boxHeight + rowSpacing;
        bgY = intBox(leftEditX, yOffL, boxWidth, boxHeight, access.bgY(c), -2048, 2048);
        yOffL += boxHeight + rowSpacing;
        barX = intBox(leftEditX, yOffL, boxWidth, boxHeight, access.barX(c), -2048, 2048);
        yOffL += boxHeight + rowSpacing;
        barY = intBox(leftEditX, yOffL, boxWidth, boxHeight, access.barY(c), -2048, 2048);

        int yOffR = yStart;
        overlayX = intBox(rightEditX, yOffR, boxWidth, boxHeight, access.overlayX(c), -2048, 2048);
        yOffR += boxHeight + rowSpacing;
        overlayY = intBox(rightEditX, yOffR, boxWidth, boxHeight, access.overlayY(c), -2048, 2048);
        yOffR += boxHeight + rowSpacing;
        textX = intBox(rightEditX, yOffR, boxWidth, boxHeight, access.textX(c), -2048, 2048);
        yOffR += boxHeight + rowSpacing;
        textY = intBox(rightEditX, yOffR, boxWidth, boxHeight, access.textY(c), -2048, 2048);

        if (hasIcon) {
            yOffR += boxHeight + rowSpacing;
            iconX = intBox(rightEditX, yOffR, boxWidth, boxHeight, access.iconX(c), -2048, 2048);
            yOffR += boxHeight + rowSpacing;
            iconY = intBox(rightEditX, yOffR, boxWidth, boxHeight, access.iconY(c), -2048, 2048);
        }

        if (hasAbsorptionText) {
            yOffR += boxHeight + rowSpacing;
            absorptionTextX = intBox(rightEditX, yOffR, boxWidth, boxHeight, access.absorptionTextX(c), -2048, 2048);
            yOffR += boxHeight + rowSpacing;
            absorptionTextY = intBox(rightEditX, yOffR, boxWidth, boxHeight, access.absorptionTextY(c), -2048, 2048);
        }

        registerBox(bgWidth, Page.SIZES);
        registerBox(bgHeight, Page.SIZES);
        registerBox(barWidth, Page.SIZES);
        registerBox(barHeight, Page.SIZES);
        registerBox(overlayWidth, Page.SIZES);
        registerBox(overlayHeight, Page.SIZES);
        registerBox(textWidth, Page.SIZES);
        registerBox(textHeight, Page.SIZES);
        registerBox(textScale, Page.SIZES);
        registerBox(iconWidth, Page.SIZES);
        registerBox(iconHeight, Page.SIZES);
        registerBox(absorptionTextWidth, Page.SIZES);
        registerBox(absorptionTextHeight, Page.SIZES);
        registerBox(absorptionTextScale, Page.SIZES);

        registerBox(totalX, Page.OFFSETS);
        registerBox(totalY, Page.OFFSETS);
        registerBox(bgX, Page.OFFSETS);
        registerBox(bgY, Page.OFFSETS);
        registerBox(barX, Page.OFFSETS);
        registerBox(barY, Page.OFFSETS);
        registerBox(overlayX, Page.OFFSETS);
        registerBox(overlayY, Page.OFFSETS);
        registerBox(textX, Page.OFFSETS);
        registerBox(textY, Page.OFFSETS);
        registerBox(iconX, Page.OFFSETS);
        registerBox(iconY, Page.OFFSETS);
        registerBox(absorptionTextX, Page.OFFSETS);
        registerBox(absorptionTextY, Page.OFFSETS);

        int btnW = 86;
        int btnH = 18;
        int btnGap = 6;
        int totalBtnsW = btnW * 3 + btnGap * 2;
        int btnStartX = (this.width - totalBtnsW) / 2;
        int btnY = this.height - btnH - 8;

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.dynamic_resource_bars.context.reset_element"),
                        b -> resetElementToDefaults())
                .bounds(btnStartX, btnY, btnW, btnH)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cancel"),
                        b -> revertAndClose())
                .bounds(btnStartX + btnW + btnGap, btnY, btnW, btnH)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        b -> { applyAll(); onClose(); })
                .bounds(btnStartX + (btnW + btnGap) * 2, btnY, btnW, btnH)
                .build());
    }

    private void registerBox(EditBox box, Page page) {
        if (box == null) return;
        box.visible = activePage == page;
        box.setEditable(activePage == page);
        addRenderableWidget(box);
    }

    private void switchPage(Page next) {
        if (activePage == next) return;
        applyAll();
        activePage = next;
        rebuildWidgets();
    }

    private void resetElementToDefaults() {
        ClientConfig defaults = ClientConfig.createDefaults();
        ClientConfig c = ModConfigManager.getClient();
        for (SubElementType sub : SubElementType.values()) {
            access.resetSubElement(c, defaults, sub);
        }
        access.setTotalX(c, access.totalX(defaults));
        access.setTotalY(c, access.totalY(defaults));
        c.save();
        rebuildWidgets();
    }

    private EditBox intBox(int x, int y, int w, int h, int initial, int min, int max) {
        return FieldEditorUtils.intBox(this.font, x, y, w, h, initial, min, max);
    }

    private int parseSafely(EditBox box, int defaultVal, int min, int max) {
        return FieldEditorUtils.parseSafely(box, defaultVal, min, max);
    }

    private void applyAll() {
        ClientConfig c = ModConfigManager.getClient();
        access.setBgWidth(c, parseSafely(bgWidth, access.bgWidth(c), 1, 512));
        access.setBgHeight(c, parseSafely(bgHeight, access.bgHeight(c), 1, 512));
        access.setBarWidth(c, parseSafely(barWidth, access.barWidth(c), 1, 256));
        access.setBarHeight(c, parseSafely(barHeight, access.barHeight(c), 1, 256));
        access.setOverlayWidth(c, parseSafely(overlayWidth, access.overlayWidth(c), 1, 256));
        access.setOverlayHeight(c, parseSafely(overlayHeight, access.overlayHeight(c), 1, 256));
        access.setTextWidth(c, parseSafely(textWidth, access.textWidth(c), 1, 256));
        access.setTextHeight(c, parseSafely(textHeight, access.textHeight(c), 1, 128));
        access.setTextScale(c, FieldEditorUtils.parseFloatSafely(textScale, access.textScale(c), MIN_SCALE, MAX_SCALE));

        access.setTotalX(c, parseSafely(totalX, access.totalX(c), -2048, 2048));
        access.setTotalY(c, parseSafely(totalY, access.totalY(c), -2048, 2048));
        access.setBgX(c, parseSafely(bgX, access.bgX(c), -2048, 2048));
        access.setBgY(c, parseSafely(bgY, access.bgY(c), -2048, 2048));
        access.setBarX(c, parseSafely(barX, access.barX(c), -2048, 2048));
        access.setBarY(c, parseSafely(barY, access.barY(c), -2048, 2048));
        access.setOverlayX(c, parseSafely(overlayX, access.overlayX(c), -2048, 2048));
        access.setOverlayY(c, parseSafely(overlayY, access.overlayY(c), -2048, 2048));
        access.setTextX(c, parseSafely(textX, access.textX(c), -2048, 2048));
        access.setTextY(c, parseSafely(textY, access.textY(c), -2048, 2048));

        if (hasIcon) {
            access.setIconX(c, parseSafely(iconX, access.iconX(c), -2048, 2048));
            access.setIconY(c, parseSafely(iconY, access.iconY(c), -2048, 2048));
            access.setIconWidth(c, parseSafely(iconWidth, access.iconWidth(c), 1, 64));
            access.setIconHeight(c, parseSafely(iconHeight, access.iconHeight(c), 1, 64));
        }
        if (hasAbsorptionText) {
            access.setAbsorptionTextX(c, parseSafely(absorptionTextX, access.absorptionTextX(c), -2048, 2048));
            access.setAbsorptionTextY(c, parseSafely(absorptionTextY, access.absorptionTextY(c), -2048, 2048));
            access.setAbsorptionTextWidth(c, parseSafely(absorptionTextWidth, access.absorptionTextWidth(c), 1, 256));
            access.setAbsorptionTextHeight(c, parseSafely(absorptionTextHeight, access.absorptionTextHeight(c), 1, 128));
            access.setAbsorptionTextScale(c, FieldEditorUtils.parseFloatSafely(
                    absorptionTextScale, access.absorptionTextScale(c), MIN_SCALE, MAX_SCALE));
        }
        c.save();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

        if (bgWidth == null) return;
        if (activePage == Page.SIZES) {
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.background_width", bgWidth);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.background_height", bgHeight);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.bar_width", barWidth);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.bar_height", barHeight);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.overlay_width", overlayWidth);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.overlay_height", overlayHeight);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.text_width", textWidth);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.text_height", textHeight);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.text_scale", textScale);
            if (hasIcon) {
                drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.icon_width", iconWidth);
                drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.icon_height", iconHeight);
            }
            if (hasAbsorptionText) {
                drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.absorption_text_width", absorptionTextWidth);
                drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.absorption_text_height", absorptionTextHeight);
                drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.absorption_text_scale", absorptionTextScale);
            }
        } else {
            drawLabel(graphics, "gui.dynamic_resource_bars.manual.x_offset", totalX);
            drawLabel(graphics, "gui.dynamic_resource_bars.manual.y_offset", totalY);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.background_x_offset", bgX);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.background_y_offset", bgY);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.bar_x_offset", barX);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.bar_y_offset", barY);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.overlay_x_offset", overlayX);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.overlay_y_offset", overlayY);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.text_x_offset", textX);
            drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.text_y_offset", textY);
            if (hasIcon) {
                drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.icon_x_offset", iconX);
                drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.icon_y_offset", iconY);
            }
            if (hasAbsorptionText) {
                drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.absorption_text_x_offset", absorptionTextX);
                drawLabel(graphics, "gui.dynamic_resource_bars.resize.label.absorption_text_y_offset", absorptionTextY);
            }
        }
    }

    private void drawLabel(GuiGraphicsExtractor graphics, String key, EditBox box) {
        FieldEditorUtils.drawLabel(graphics, this.font, key, 0, box);
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
