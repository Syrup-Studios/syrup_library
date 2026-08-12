package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.ConfigSchemaNode;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import net.syrupstudios.syruplibrary.config.edit.ConfigEditSession;
import net.syrupstudios.syruplibrary.config.value.BooleanConfigValue;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;
import net.syrupstudios.syruplibrary.config.value.EnumConfigValue;
//? if >=26 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
 *///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One row in the config editor, either a nested section or an editable value. */
final class ConfigEntryWidget extends ContainerObjectSelectionList.Entry<ConfigEntryWidget> {
    private static final int COLOR_NAME = 0xFFFFFFFF;
    private static final int COLOR_ERROR = 0xFFFF5555;
    private static final int COLOR_RESTART = 0xFFFFFF55;

    private final ConfigSectionScreen screen;
    private final ConfigEditSession session;
    private final boolean sectionEntry;
    private final ConfigSchemaNode section;
    private final ConfigValue<?> value;
    private final List<AbstractWidget> childWidgets = new ArrayList<>();

    private AbstractWidget editor;
    private EditBox editBox;
    private Button resetButton;
    private boolean syncingEditor;

    ConfigEntryWidget(ConfigSectionScreen screen, ConfigSchemaNode schema) {
        this.screen = screen;
        this.session = screen.session();
        if (schema.isSection()) {
            this.sectionEntry = true;
            this.section = schema;
            this.value = null;
            editor = Button.builder(sectionLabel(), button -> openSection())
                    .bounds(0, 0, 80, 18)
                    .build();
            childWidgets.add(editor);
        } else {
            this.sectionEntry = false;
            this.section = null;
            this.value = schema.value();
            createEditor();
        }
    }

    private void createEditor() {
        if (value instanceof BooleanConfigValue) {
            editor = Button.builder(Component.literal(""), button -> toggleBoolean())
                    .bounds(0, 0, 80, 18)
                    .build();
        } else if (value instanceof EnumConfigValue<?> enumValue) {
            editor = buildCycleButton(enumValue);
        } else {
            editBox = new EditBox(screen.fontInstance(), 0, 0, 80, 18, Component.literal(""));
            editBox.setMaxLength(Integer.MAX_VALUE);
            editBox.setResponder(text -> {
                if (syncingEditor) {
                    return;
                }
                Object candidate = ConfigText.parseText(value, text);
                session.setValue(value, candidate);
                screen.markChanged();
            });
            editor = editBox;
        }
        setDescriptionTooltip(editor);
        childWidgets.add(editor);
        resetButton = Button.builder(Component.translatable("syrup_library.config.reset"), button -> resetValue())
                .bounds(0, 0, 44, 16)
                .build();
        setDescriptionTooltip(resetButton);
        childWidgets.add(resetButton);
        syncEditorFromSession();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CycleButton buildCycleButton(EnumConfigValue<?> enumValue) {
        List constants = List.of(enumValue.enumType().getEnumConstants());
        Object initial = enumValue.enumType().cast(session.value(value));
        java.util.function.Function<Object, Component> label = constant -> Component.literal(
                ConfigText.displayName(((Enum<?>) constant).name().toLowerCase(Locale.ROOT)));
        CycleButton.Builder builder;
        //? if >=1.21.11 {
        /*builder = (CycleButton.Builder) CycleButton.builder(label, initial);
         *///?} else {
        builder = (CycleButton.Builder) CycleButton.builder(label).withInitialValue(initial);
        //?}
        return builder.withValues(constants)
                .displayOnlyValue()
                .create(0, 0, 80, 18, Component.literal(""), (button, constant) -> {
                    session.setValue(value, constant);
                    screen.markChanged();
                });
    }

    private void toggleBoolean() {
        boolean current = (Boolean) session.value(value);
        session.setValue(value, !current);
        refresh();
        screen.markChanged();
    }

    private void resetValue() {
        session.reset(value);
        refresh();
        screen.markChanged();
    }

    private void setDescriptionTooltip(AbstractWidget widget) {
        List<String> description = value.description();
        if (description.isEmpty()) {
            return;
        }
        Component text = Component.literal(String.join("\n", description));
        widget.setTooltip(Tooltip.create(text));
    }

    /** Pushes the current session values into the widgets without creating drafts. */
    void refresh() {
        syncEditorFromSession();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void syncEditorFromSession() {
        if (sectionEntry) {
            return;
        }
        syncingEditor = true;
        try {
            if (editBox != null) {
                editBox.setValue(ConfigText.toText(session.value(value)));
            } else if (editor instanceof CycleButton) {
                ((CycleButton) editor).setValue(session.value(value));
            } else {
                editor.setMessage(Component.literal(String.valueOf(session.value(value))));
            }
        } finally {
            syncingEditor = false;
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return childWidgets;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return childWidgets;
    }

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
        if (resetButton != null) {
            int resetX = resetRight - 48;
            int resetY = rowTop + (rowHeight - 16) / 2;
            resetButton.setX(resetX);
            resetButton.setY(resetY);
            editorWidth = Math.min(140, editorWidth);
        }
        int editorX = resetRight - 48 - 6 - editorWidth;
        int editorY = rowTop + (rowHeight - 18) / 2;
        editor.setX(editorX);
        editor.setY(editorY);
        editor.setWidth(editorWidth);
    }

    //? if >=26 {
    /*private void extractRow(GuiGraphicsExtractor graphics, int rowLeft, int rowRight, int rowTop, int rowHeight,
                             int mouseX, int mouseY, float partialTick) {
        if (sectionEntry) {
            editor.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }
        drawText(graphics, fittedName(rowLeft),
                rowLeft + 8, rowTop + 2, COLOR_NAME);
        String error = session.errorFor(value);
        if (error != null) {
            drawText(graphics, fittedText(error, rowLeft), rowLeft + 8, rowTop + 18, COLOR_ERROR);
        } else if (value.restartRequirement() == RestartRequirement.REQUIRED) {
            drawText(graphics, fittedText(Component.translatable("syrup_library.config.restart_required").getString(), rowLeft),
                    rowLeft + 8, rowTop + 18, COLOR_RESTART);
        }
        editor.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (resetButton != null) {
            resetButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }
     *///?} else {
    private void renderRow(GuiGraphics graphics, int rowLeft, int rowRight, int rowTop, int rowHeight,
                           int mouseX, int mouseY, float partialTick) {
        if (sectionEntry) {
            editor.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        drawText(graphics, fittedName(rowLeft),
                rowLeft + 8, rowTop + 2, COLOR_NAME);
        String error = session.errorFor(value);
        if (error != null) {
            drawText(graphics, fittedText(error, rowLeft), rowLeft + 8, rowTop + 18, COLOR_ERROR);
        } else if (value.restartRequirement() == RestartRequirement.REQUIRED) {
            drawText(graphics, fittedText(Component.translatable("syrup_library.config.restart_required").getString(), rowLeft),
                    rowLeft + 8, rowTop + 18, COLOR_RESTART);
        }
        editor.render(graphics, mouseX, mouseY, partialTick);
        if (resetButton != null) {
            resetButton.render(graphics, mouseX, mouseY, partialTick);
        }
    }
    //?}

    private Component sectionLabel() {
        return ConfigText.sectionName(section);
    }

    private Component fittedName(int rowLeft) {
        return fittedText(ConfigText.valueName(value).getString(), rowLeft);
    }

    private Component fittedText(String text, int rowLeft) {
        int maximumWidth = Math.max(20, editor.getX() - 16 - rowLeft);
        if (screen.fontInstance().width(text) <= maximumWidth) {
            return Component.literal(text);
        }
        String suffix = "...";
        int end = text.length();
        while (end > 0 && screen.fontInstance().width(text.substring(0, end) + suffix) > maximumWidth) {
            end--;
        }
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
