package net.syrupstudios.syruplibrary.client.config;

import net.minecraft.client.gui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.syrupstudios.syruplibrary.config.ConfigSnapshot;
import net.syrupstudios.syruplibrary.config.RegisteredConfig;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import net.syrupstudios.syruplibrary.config.SyrupConfigManager;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigUpdateResult;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Client-only generated editor. Draft values stay local until a successful save. */
public final class SyrupConfigScreen extends ConfigScreen {
    private static final String FAILURE_MESSAGE = "Save failed";
    private static final int FORM_WIDTH = 400;
    private static final int ROW_HEIGHT = 26;
    private static final int LIST_TOP = 32;
    private static final int FOOTER_HEIGHT = 32;

    private final RegisteredConfig config;
    private final List<ConfigValue<?>> values;
    private final Map<ConfigValue<?>, Object> drafts = new LinkedHashMap<>();
    private final Map<ConfigValue<?>, Object> original = new LinkedHashMap<>();
    private final Map<ConfigValue<?>, Integer> listIndices = new LinkedHashMap<>();
    private SettingsList settings;
    private ConfigRow activeRow;
    private boolean placingEditor;
    private int editorLeft;
    private int editorWidth;
    private String status = "Edits are not saved.";
    private String statusDetails = status;
    private boolean saved;
    private boolean statusActionable;
    private Button detailsButton;
    private Button closeButton;

    public SyrupConfigScreen(Screen parent, RegisteredConfig config) {
        super(parent, Component.literal(humanize(config.spec().id())));
        this.config = config;
        this.values = List.copyOf(config.spec().values());
        resetDrafts();
    }

    /** Creates a selector for all configs, including an empty-state screen. */
    public static Screen create(Screen parent) {
        return new SyrupConfigSelectionScreen(parent,
                SyrupConfigManager.getInstance().registeredConfigs().values(), "Syrup Library configs");
    }

    /** Creates the owning mod's editor, or a filtered selector; returns null when it has no configs. */
    public static Screen createForMod(Screen parent, String modId) {
        List<RegisteredConfig> configs = SyrupConfigManager.getInstance().registeredConfigs().values().stream()
                .filter(config -> config.spec().ownerId().equals(modId)).toList();
        if (configs.isEmpty()) return null;
        if (configs.size() == 1) return new SyrupConfigScreen(parent, configs.get(0));
        return new SyrupConfigSelectionScreen(parent, configs, modId + " configs");
    }

    /** Creates an editor for a registered ID, or returns null if it is absent. */
    public static Screen create(Screen parent, String configId) {
        return SyrupConfigManager.getInstance().find(configId)
                .map(config -> new SyrupConfigScreen(parent, config)).orElse(null);
    }

    private void resetDrafts() {
        ConfigSnapshot snapshot = config.configuredSnapshot();
        for (ConfigValue<?> value : values) {
            Object current = snapshot.get(value);
            original.put(value, ConfigEditorRegistry.draft(value, current));
            drafts.put(value, ConfigEditorRegistry.draft(value, current));
        }
    }

    @Override public int contentWidth() { return Math.min(FORM_WIDTH, width - 24); }

    @Override public <W extends AbstractWidget> W widget(W widget) {
        if (activeRow == null) return super.widget(widget);
        if (placingEditor) {
            int x = editorLeft + (widget.getX() - left()) * editorWidth / contentWidth();
            int width = widget.getWidth() * editorWidth / contentWidth();
            activeRow.add(widget, widget.getY() - 100, x, width);
        } else activeRow.add(widget, widget.getY() - activeRow.baseY, widget.getX(), widget.getWidth());
        return widget;
    }

    private int editorCoordinate(int controlX) {
        return left() + (controlX - editorLeft) * contentWidth() / editorWidth;
    }

    private int editorDimension(int controlWidth) {
        return controlWidth * contentWidth() / editorWidth;
    }

    @Override protected void init() {
        centeredLabel(title.getString(), left(), 10, contentWidth());
        double scrollAmount = settings == null ? 0 : settings.currentScrollAmount();
        settings = new SettingsList(width, height, LIST_TOP, height - FOOTER_HEIGHT, ROW_HEIGHT);
        if (hasGeneralSection()) {
            settings.addRow(new ConfigRow("General", "", LIST_TOP + 4, true));
        }
        if (values.isEmpty()) settings.addRow(new ConfigRow("This config has no settings.", "", LIST_TOP));
        for (ConfigValue<?> value : values) {
            ConfigRow row = new ConfigRow(humanize(value.path())
                    + (value.restartRequirement() == RestartRequirement.REQUIRED ? " (Restart)" : ""),
                    ConfigEditorRegistry.metadata(value), LIST_TOP + 4 + settings.children().size() * ROW_HEIGHT);
            activeRow = row;
            editorLeft = left() + contentWidth() / 2 + 8;
            editorWidth = left() + contentWidth() - editorLeft;
            placingEditor = true;
            editorTooltip(Component.literal(ConfigEditorRegistry.metadata(value)));
            try {
                ConfigEditorRegistry.create(this, value, drafts.get(value), next -> drafts.put(value, next),
                        this::changed, this::rebuildWidgets);
            } finally {
                editorTooltip(null);
                placingEditor = false;
                activeRow = null;
            }
            settings.addRow(row);
        }
        settings.setScrollAmount(scrollAmount);
        addRenderableWidget(settings);

        int y = height - 28;
        int actionWidth = statusActionable ? 58 : 0;
        if (statusActionable) detailsButton = button("Details", left(), y, actionWidth,
                () -> details("Save status", statusDetails));
        int gap = statusActionable ? 4 : 0;
        int buttonWidth = (contentWidth() - actionWidth - gap - 4) / 2;
        button("Save", left() + actionWidth + gap, y, buttonWidth, this::save);
        int closeX = left() + actionWidth + gap + buttonWidth + 4;
        closeButton = button(saved ? "Done" : "Cancel", closeX, y,
                left() + contentWidth() - closeX, this::onClose);
        if (saved) closeButton.setTooltip(Tooltip.create(Component.literal(status)));
    }

    private boolean hasGeneralSection() {
        return !values.isEmpty() && values.stream().allMatch(value -> !value.path().contains("."));
    }

    static String humanize(String path) {
        return java.util.Arrays.stream(path.split("\\."))
                .map(section -> java.util.Arrays.stream(section.replace('_', ' ').replace('-', ' ').split("\\s+"))
                        .filter(word -> !word.isEmpty())
                        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                        .collect(Collectors.joining(" ")))
                .collect(Collectors.joining(" / "));
    }

    @SuppressWarnings("unchecked")
    void listEditor(ConfigValue<?> value) {
        List<String> list = (List<String>) drafts.get(value);
        int selected = Math.max(0, Math.min(listIndices.getOrDefault(value, 0), list.size() - 1));
        listIndices.put(value, selected);
        int fieldWidth = editorWidth / 3;
        int gap = 2;
        int buttonWidth = (editorWidth - fieldWidth - gap * 5) / 4;
        if (list.isEmpty()) {
            StringWidget empty = new StringWidget(left(), 100, editorDimension(fieldWidth), 12,
                    Component.literal("Empty"), clientFont());
            widget(empty);
        } else {
            EditBox field = new EditBox(clientFont(), left(), 100, editorDimension(fieldWidth), 20,
                    Component.literal(value.path() + " item " + (selected + 1)));
            field.setMaxLength(Integer.MAX_VALUE);
            field.setValue(list.get(selected));
            field.setResponder(text -> { list.set(selected, text); changed(); });
            widget(field);
        }
        int controlsX = editorLeft + fieldWidth + gap;
        Button previous = button("‹", editorCoordinate(controlsX), 100, editorDimension(buttonWidth),
                () -> { listIndices.put(value, selected - 1); rebuildWidgets(); });
        previous.active = selected > 0;
        previous.setTooltip(Tooltip.create(Component.literal("Previous item")));
        Button next = button("›", editorCoordinate(controlsX + buttonWidth + gap), 100, editorDimension(buttonWidth),
                () -> { listIndices.put(value, selected + 1); rebuildWidgets(); });
        next.active = selected + 1 < list.size();
        next.setTooltip(Tooltip.create(Component.literal("Next item")));
        Button add = button("+", editorCoordinate(controlsX + (buttonWidth + gap) * 2), 100, editorDimension(buttonWidth),
                () -> { list.add(""); listIndices.put(value, list.size() - 1); changed(); rebuildWidgets(); });
        add.setTooltip(Tooltip.create(Component.literal("Add item")));
        Button remove = button("−", editorCoordinate(controlsX + (buttonWidth + gap) * 3), 100, editorDimension(buttonWidth),
                () -> { list.remove(selected); listIndices.put(value, Math.max(0, selected - 1)); changed(); rebuildWidgets(); });
        remove.active = !list.isEmpty();
        remove.setTooltip(Tooltip.create(Component.literal("Remove item")));
    }

    private void changed() {
        saved = false;
        status = "Edits are not saved.";
        statusDetails = status;
        statusActionable = false;
        if (detailsButton != null) detailsButton.visible = false;
        if (closeButton != null) {
            closeButton.setMessage(Component.literal("Cancel"));
            closeButton.setTooltip(Tooltip.create(Component.literal("Cancel")));
        }
    }

    private void save() {
        Map<ConfigValue<?>, Object> updates = new LinkedHashMap<>();
        for (ConfigValue<?> value : values) {
            Object draft = drafts.get(value);
            if (Objects.equals(draft, original.get(value))) continue;
            try {
                updates.put(value, ConfigEditorRegistry.normalize(value, draft));
            } catch (RuntimeException exception) {
                int invalidIndex = values.indexOf(value) + (hasGeneralSection() ? 1 : 0);
                status = "Invalid value";
                statusDetails = value.path() + ": " + usefulMessage(exception);
                statusActionable = true;
                rebuildWidgets();
                settings.setScrollAmount(invalidIndex * ROW_HEIGHT);
                return;
            }
        }
        ConfigUpdateResult result = config.updateAndSaveAll(updates);
        saved = result.successful();
        if (saved) {
            statusActionable = false;
            resetDrafts();
            boolean restart = values.stream().anyMatch(value -> value.restartRequirement() == RestartRequirement.REQUIRED
                    && !Objects.equals(value.configuredValue(), value.get()));
            status = restart ? "Saved. Restart required." : "Saved.";
            statusDetails = status;
        } else {
            status = FAILURE_MESSAGE;
            statusDetails = result.issues().stream().map(issue -> issue.path() + ": " + issue.message())
                    .collect(Collectors.joining("\n"));
            statusActionable = true;
        }
        rebuildWidgets();
    }

    private static String usefulMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Enter a valid " + exception.getClass().getSimpleName() + "." : exception.getMessage();
    }

    private final class ConfigRow extends ContainerObjectSelectionList.Entry<ConfigRow> {
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private final Map<AbstractWidget, Integer> yOffsets = new IdentityHashMap<>();
        private int baseY;

        private ConfigRow(String label, String tooltip, int baseY) {
            this(label, tooltip, baseY, false);
        }

        private ConfigRow(String label, String tooltip, int baseY, boolean heading) {
            this.baseY = baseY;
            int labelWidth = heading ? font.width(label) : Math.max(1, contentWidth() / 2 - 12);
            int labelX = heading ? left() + (contentWidth() - labelWidth) / 2 : left() + 4;
            String shown = font.plainSubstrByWidth(label, labelWidth);
            StringWidget labelWidget = new StringWidget(labelX, baseY + 7, labelWidth, 12,
                    Component.literal(shown), font);
            labelWidget.setWidth(font.width(shown));
            if (!tooltip.isBlank()) labelWidget.setTooltip(Tooltip.create(Component.literal(tooltip)));
            add(labelWidget, 7, labelWidget.getX(), labelWidget.getWidth());
        }

        private void add(AbstractWidget widget, int yOffset, int x, int width) {
            widget.setX(x);
            widget.setY(baseY + yOffset);
            widget.setWidth(width);
            widgets.add(widget);
            yOffsets.put(widget, yOffset);
        }

        private void position(int top) {
            for (AbstractWidget widget : widgets) widget.setY(top + yOffsets.get(widget));
        }

        private void setBaseY(int y) { baseY = y; }

        @Override public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return widgets;
        }

        @Override public List<? extends NarratableEntry> narratables() { return widgets; }

        //? if >=26.2 {
        @Override public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                             boolean hovered, float delta) {
            position(getY());
            for (AbstractWidget widget : widgets) widget.extractRenderState(graphics, mouseX, mouseY, delta);
        }
        //?} else if >=1.21.11 {
        /*@Override public void renderContent(GuiGraphics graphics, int mouseX, int mouseY,
                                             boolean hovered, float delta) {
            position(getY());
            for (AbstractWidget widget : widgets) widget.render(graphics, mouseX, mouseY, delta);
        }
        *///?} else {
        /*@Override public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                                       int mouseX, int mouseY, boolean hovered, float delta) {
            position(top);
            for (AbstractWidget widget : widgets) widget.render(graphics, mouseX, mouseY, delta);
        }
        *///?}
    }

    private final class SettingsList extends ContainerObjectSelectionList<ConfigRow> {
        private SettingsList(int width, int height, int top, int bottom, int rowHeight) {
            //? if <1.21.1 {
            /*super(Minecraft.getInstance(), width, height, top, bottom, rowHeight);
            setRenderTopAndBottom(true);
            *///?} else {
            super(Minecraft.getInstance(), width, bottom - top, top, rowHeight);
            //?}
        }

        private void addRow(ConfigRow row) {
            //? if >=1.21.11 {
            addEntry(row, ROW_HEIGHT);
            row.setBaseY(row.getY());
            //?} else {
            /*addEntry(row);
            *///?}
        }

        private double currentScrollAmount() {
            // Newer lists expose no scroll getter. Their entries track the native scroll position.
            //? if >=26.2 {
            return children().isEmpty() ? 0 : children().get(0).baseY - children().get(0).getY();
            //?} else if >=1.21.11 {
            /*return children().isEmpty() ? 0 : children().get(0).baseY - children().get(0).getY();
            *///?} else {
            /*return getScrollAmount();
            *///?}
        }

        //? if >=1.21.11 && <26.2 {
        /*@Override public int getRowWidth() { return Math.min(FORM_WIDTH, getWidth() - 24) - 12; }
        *///?}
        //? if <1.21.11 {
        /*@Override public int getRowWidth() {
            //? if <1.21.1 {
            return Math.min(FORM_WIDTH, width - 24) - 12;
            //?} else {
            return Math.min(FORM_WIDTH, getWidth() - 24) - 12;
            //?}
        }
        *///?}
        //? if <1.21.11 {
        /*
        @Override protected int getRowTop(int index) {
            //? if <1.21.1 {
            return y0 + 4 + index * ROW_HEIGHT - (int) getScrollAmount();
            //?} else {
            return getY() + 4 + index * ROW_HEIGHT - (int) getScrollAmount();
            //?}
        }
        @Override protected int getRowBottom(int index) { return getRowTop(index) + ROW_HEIGHT; }
        @Override protected int getMaxPosition() { return children().size() * ROW_HEIGHT; }
        *///?}

        //? if >=26.2 {
        @Override public int getRowWidth() { return Math.min(FORM_WIDTH, getWidth() - 24); }
        //?}
    }
}
