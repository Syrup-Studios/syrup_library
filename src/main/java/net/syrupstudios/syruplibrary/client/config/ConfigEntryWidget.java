package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.ConfigEditPolicy;
import net.syrupstudios.syruplibrary.config.ConfigSchemaNode;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import net.syrupstudios.syruplibrary.config.edit.ConfigEditSession;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;
//? if >=26 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
 *///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** One row in the config editor, either a nested section or an editable value. */
final class ConfigEntryWidget extends ConfigListEntry {
    private static final int COLOR_NAME = 0xFFFFFFFF;
    private static final int COLOR_ERROR = 0xFFFF5555;
    private static final int COLOR_RESTART = 0xFFFFFF55;
    private static final int COLOR_READ_ONLY = 0xFFA0A0A0;

    private final ConfigSectionScreen screen;
    private final ConfigEditSession session;
    private final ConfigSchemaNode schema;
    private final boolean sectionEntry;
    private final ConfigSchemaNode section;
    private final ConfigValue<?> value;
    private final List<AbstractWidget> childWidgets = new ArrayList<>();

    private AbstractWidget editor;
    private ConfigEditorHandle editorHandle;
    private Button resetButton;

    ConfigEntryWidget(ConfigSectionScreen screen, ConfigSchemaNode schema) {
        this.screen = screen;
        this.session = screen.session();
        this.schema = schema;
        if (schema.isSection()) {
            this.sectionEntry = true;
            this.section = schema;
            this.value = null;
            editor = Button.builder(sectionLabel(), button -> openSection())
                    .bounds(0, 0, 80, 18)
                    .build();
            setDescriptionTooltip(editor);
            childWidgets.add(editor);
        } else {
            this.sectionEntry = false;
            this.section = null;
            this.value = schema.value();
            createEditor();
        }
    }

    private void createEditor() {
        editorHandle = ConfigEditorRegistry.create(new ConfigEditorContext(screen, schema));
        editor = editorHandle.primaryWidget();
        for (AbstractWidget widget : editorHandle.widgets()) {
            setDescriptionTooltip(widget);
            childWidgets.add(widget);
        }
        resetButton = Button.builder(Component.translatable("syrup_library.config.reset"), button -> resetValue())
                .bounds(0, 0, 44, 16)
                .build();
        setDescriptionTooltip(resetButton);
        childWidgets.add(resetButton);
        refresh();
    }

    private void resetValue() {
        session.reset(value);
        refresh();
        screen.markChanged(value);
    }

    private void setDescriptionTooltip(AbstractWidget widget) {
        Component text = ConfigText.description(schema, schema.spec());
        if (!text.getString().isEmpty()) widget.setTooltip(Tooltip.create(text));
    }

    /** Pushes the current session values and active state into the widgets. */
    @Override void refresh() {
        if (editorHandle != null) editorHandle.refresh();
        updateActiveState();
    }

    void updateActiveState() {
        if (sectionEntry) return;
        boolean editable = schema.presentation().editPolicy() == ConfigEditPolicy.EDITABLE
                && session.matches(schema.presentation().enabledCondition());
        editorHandle.setActive(editable);
        if (resetButton != null) resetButton.active = editable;
    }

    @Override boolean isEditable() {
        return !sectionEntry
                && schema.presentation().editPolicy() == ConfigEditPolicy.EDITABLE
                && session.matches(schema.presentation().enabledCondition());
    }

    @Override ConfigValue<?> value() { return value; }

    @Override
    public List<? extends GuiEventListener> children() { return childWidgets; }

    @Override
    public List<? extends NarratableEntry> narratables() { return childWidgets; }

    private void openSection() {
        screen.openScreen(ConfigSectionScreen.withSession(screen, session, section));
    }

    //? if >=26 {
    /*@Override
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        int rowLeft = getX();
        int rowRight = getX() + getWidth();
        positionWidgets(rowLeft, rowRight, getY(), getHeight());
        extractRow(graphics, rowLeft, rowRight, getY(), getHeight(), mouseX, mouseY, partialTick);
    }
     *///?} elif >=1.21.11 {
    /*@Override
    public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        int rowLeft = getX();
        int rowRight = getX() + getWidth();
        positionWidgets(rowLeft, rowRight, getY(), getHeight());
        renderRow(graphics, rowLeft, rowRight, getY(), getHeight(), mouseX, mouseY, partialTick);
    }
     *///?} else {
    @Override
    public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                       int mouseX, int mouseY, boolean hovered, float partialTick) {
        int rowLeft = left;
        int rowRight = left + width;
        positionWidgets(rowLeft, rowRight, top, height);
        renderRow(graphics, rowLeft, rowRight, top, height, mouseX, mouseY, partialTick);
    }
    //?}

    private void positionWidgets(int rowLeft, int rowRight, int rowTop, int rowHeight) {
        if (sectionEntry) {
            editor.setX(rowLeft + 4);
            editor.setY(rowTop + (rowHeight - 18) / 2);
            editor.setWidth(rowRight - rowLeft - 8);
            return;
        }
        int editorWidth = Math.min(150, Math.max(80, (rowRight - rowLeft) / 3));
        int resetRight = rowRight - 4;
        int resetX = resetRight - 48;
        int resetY = rowTop + (rowHeight - 16) / 2;
        resetButton.setX(resetX);
        resetButton.setY(resetY);
        editorWidth = Math.min(140, editorWidth);
        int editorX = resetX - 6 - editorWidth;
        int editorY = rowTop + (rowHeight - 18) / 2;
        editorHandle.setBounds(editorX, editorY, editorWidth, 18);
    }

    //? if >=26 {
    /*private void extractRow(GuiGraphicsExtractor graphics, int rowLeft, int rowRight, int rowTop, int rowHeight,
                             int mouseX, int mouseY, float partialTick) {
        if (sectionEntry) {
            editor.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }
        drawText(graphics, fittedName(rowLeft), rowLeft + 8, rowTop + 2, COLOR_NAME);
        drawSecondaryText(graphics, rowLeft, rowTop);
        for (AbstractWidget widget : editorHandle.widgets()) {
            widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        resetButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSecondaryText(GuiGraphicsExtractor graphics, int rowLeft, int rowTop) {
        String error = session.errorFor(value);
        if (error != null) {
            drawText(graphics, fittedText(error, rowLeft), rowLeft + 8, rowTop + 18, COLOR_ERROR);
        } else if (schema.presentation().editPolicy() == ConfigEditPolicy.READ_ONLY) {
            drawText(graphics, fittedText(Component.translatable("syrup_library.config.read_only").getString(), rowLeft),
                    rowLeft + 8, rowTop + 18, COLOR_READ_ONLY);
        } else if (!session.matches(schema.presentation().enabledCondition())) {
            drawText(graphics, fittedText(Component.translatable("syrup_library.config.disabled").getString(), rowLeft),
                    rowLeft + 8, rowTop + 18, COLOR_READ_ONLY);
        } else if (value.restartRequirement() == RestartRequirement.REQUIRED) {
            drawText(graphics, fittedText(Component.translatable("syrup_library.config.restart_required").getString(), rowLeft),
                    rowLeft + 8, rowTop + 18, COLOR_RESTART);
        }
    }
     *///?} else {
    private void renderRow(GuiGraphics graphics, int rowLeft, int rowRight, int rowTop, int rowHeight,
                           int mouseX, int mouseY, float partialTick) {
        if (sectionEntry) {
            editor.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        drawText(graphics, fittedName(rowLeft), rowLeft + 8, rowTop + 2, COLOR_NAME);
        drawSecondaryText(graphics, rowLeft, rowTop);
        for (AbstractWidget widget : editorHandle.widgets()) {
            widget.render(graphics, mouseX, mouseY, partialTick);
        }
        resetButton.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSecondaryText(GuiGraphics graphics, int rowLeft, int rowTop) {
        String error = session.errorFor(value);
        if (error != null) {
            drawText(graphics, fittedText(error, rowLeft), rowLeft + 8, rowTop + 18, COLOR_ERROR);
        } else if (schema.presentation().editPolicy() == ConfigEditPolicy.READ_ONLY) {
            drawText(graphics, fittedText(Component.translatable("syrup_library.config.read_only").getString(), rowLeft),
                    rowLeft + 8, rowTop + 18, COLOR_READ_ONLY);
        } else if (!session.matches(schema.presentation().enabledCondition())) {
            drawText(graphics, fittedText(Component.translatable("syrup_library.config.disabled").getString(), rowLeft),
                    rowLeft + 8, rowTop + 18, COLOR_READ_ONLY);
        } else if (value.restartRequirement() == RestartRequirement.REQUIRED) {
            drawText(graphics, fittedText(Component.translatable("syrup_library.config.restart_required").getString(), rowLeft),
                    rowLeft + 8, rowTop + 18, COLOR_RESTART);
        }
    }
    //?}

    private Component sectionLabel() { return ConfigText.sectionName(section); }

    private Component fittedName(int rowLeft) {
        return fittedText(ConfigText.valueName(schema).getString(), rowLeft);
    }

    private Component fittedText(String text, int rowLeft) {
        int maximumWidth = Math.max(20, editor.getX() - 16 - rowLeft);
        if (screen.fontInstance().width(text) <= maximumWidth) return Component.literal(text);
        String suffix = "...";
        int end = text.length();
        while (end > 0 && screen.fontInstance().width(text.substring(0, end) + suffix) > maximumWidth) end--;
        return Component.literal(text.substring(0, end) + suffix);
    }

    //? if >=26 {
    /*private void drawText(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        graphics.text(screen.fontInstance(), text, x, y, color);
    }
     *///?} else {
    private void drawText(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.drawString(screen.fontInstance(), text, x, y, color);
    }
    //?}
}
