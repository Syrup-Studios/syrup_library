package net.syrupstudios.syruplibrary.client.config;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.syrupstudios.syruplibrary.config.ConfigSnapshot;
import net.syrupstudios.syruplibrary.config.RegisteredConfig;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import net.syrupstudios.syruplibrary.config.SyrupConfigManager;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigUpdateResult;
import net.syrupstudios.syruplibrary.config.value.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Client-only generated editor. Draft values stay local until a successful save. */
public final class SyrupConfigScreen extends ConfigScreen {
    private final RegisteredConfig config;
    private final List<ConfigValue<?>> values;
    private final Map<ConfigValue<?>, Object> drafts = new LinkedHashMap<>();
    private final Map<ConfigValue<?>, Object> original = new LinkedHashMap<>();
    private int page;
    private int listIndex;
    private String status = "Edits are not saved.";
    private String statusDetails = status;
    private boolean saved;
    private Button statusButton;
    private Button closeButton;

    public SyrupConfigScreen(Screen parent, RegisteredConfig config) {
        super(parent, Component.literal(config.spec().id()));
        this.config = config;
        this.values = List.copyOf(config.spec().values());
        resetDrafts();
    }

    /** Creates a selector for all configs, including an empty-state screen. */
    public static Screen create(Screen parent) {
        return new SyrupConfigSelectionScreen(parent);
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
            Object draft = current instanceof Number ? current.toString() : current;
            original.put(value, draft);
            drafts.put(value, draft instanceof List<?> list ? new ArrayList<>(list) : draft);
        }
    }

    @Override protected void init() {
        label(title.getString(), 10);
        int w = contentWidth();
        button("Previous setting", left(), 28, w / 2 - 2,
                () -> changePage(-1)).active = page > 0;
        button("Next setting", left() + w / 2 + 2, 28, w / 2 - 2,
                () -> changePage(1)).active = page + 1 < values.size();
        if (values.isEmpty()) {
            label("This config has no settings.", 60);
        } else {
            ConfigValue<?> value = values.get(page);
            label((page + 1) + "/" + values.size() + "  " + value.path(), 54);
            String metadata = metadata(value);
            button("Description and limits", left(), 72, w,
                    () -> details(value.path(), metadata));
            Object draft = drafts.get(value);
            if (value instanceof BooleanConfigValue) {
                button(value.path() + ": " + draft, left(), 100, w, () -> {
                    drafts.put(value, !(Boolean) drafts.get(value));
                    changed();
                    rebuildWidgets();
                });
            } else if (value instanceof EnumConfigValue<?> enumValue) {
                button(value.path() + ": " + ((Enum<?>) draft).name(), left(), 100, w, () -> {
                    Enum<?>[] choices = enumValue.enumType().getEnumConstants();
                    drafts.put(value, choices[(((Enum<?>) drafts.get(value)).ordinal() + 1) % choices.length]);
                    changed();
                    rebuildWidgets();
                });
            } else if (value instanceof StringListConfigValue) {
                listEditor(value);
            } else if (value instanceof StringConfigValue) {
                textField((String) draft, value.path(), 100, Math.max(32, height - 184),
                        text -> { drafts.put(value, text); changed(); });
            } else {
                EditBox field = new EditBox(font, left(), 100, w, 20, Component.literal(value.path()));
                field.setMaxLength(Integer.MAX_VALUE);
                field.setValue((String) draft);
                field.setResponder(text -> { drafts.put(value, text); changed(); });
                addRenderableWidget(field);
            }
            label(value.restartRequirement() == RestartRequirement.REQUIRED
                    ? "This setting requires a restart." : "This setting applies after Save.", height - 52);
        }
        statusButton = button(status, left(), height - 76, w, () -> details("Save status", statusDetails));
        button("Save", left(), height - 28, w / 2 - 2, this::save);
        closeButton = button(saved ? "Done" : "Cancel", left() + w / 2 + 2, height - 28, w / 2 - 2, this::onClose);
    }

    private void changePage(int delta) {
        page += delta;
        listIndex = 0;
        rebuildWidgets();
    }

    @SuppressWarnings("unchecked")
    private void listEditor(ConfigValue<?> value) {
        List<String> list = (List<String>) drafts.get(value);
        listIndex = Math.max(0, Math.min(listIndex, list.size() - 1));
        int selected = listIndex;
        if (list.isEmpty()) label("Empty list. Use Add item.", 102);
        else textField(list.get(selected), value.path() + " item " + (selected + 1), 98,
                Math.max(24, height - 210), text -> { list.set(selected, text); changed(); });
        int cell = contentWidth() / 4;
        int y = height - 108;
        button("< " + (list.isEmpty() ? 0 : selected + 1), left(), y, cell - 2,
                () -> { listIndex--; rebuildWidgets(); }).active = selected > 0;
        button(list.size() + " >", left() + cell, y, cell - 2,
                () -> { listIndex++; rebuildWidgets(); }).active = selected + 1 < list.size();
        button("Add item", left() + cell * 2, y, cell - 2, () -> {
            list.add(""); listIndex = list.size() - 1; changed(); rebuildWidgets();
        });
        button("Remove item", left() + cell * 3, y, cell - 2, () -> {
            list.remove(selected); changed(); rebuildWidgets();
        }).active = !list.isEmpty();
    }

    private void changed() {
        saved = false;
        status = "Edits are not saved.";
        statusDetails = status;
        if (statusButton != null) {
            statusButton.setMessage(Component.literal(status));
            statusButton.setTooltip(Tooltip.create(Component.literal(status)));
        }
        if (closeButton != null) {
            closeButton.setMessage(Component.literal("Cancel"));
            closeButton.setTooltip(Tooltip.create(Component.literal("Cancel")));
        }
    }

    private String metadata(ConfigValue<?> value) {
        String limits = "";
        if (value instanceof IntConfigValue number) limits = "\nRange: " + number.minimum() + " to " + number.maximum();
        if (value instanceof LongConfigValue number) limits = "\nRange: " + number.minimum() + " to " + number.maximum();
        if (value instanceof DoubleConfigValue number) limits = "\nRange: " + number.minimum() + " to " + number.maximum();
        if (value instanceof EnumConfigValue<?> choice) limits = "\nChoices: " + java.util.Arrays.toString(choice.enumType().getEnumConstants());
        return value.path() + "\n" + String.join("\n", value.description()) + limits
                + (value.restartRequirement() == RestartRequirement.REQUIRED
                ? "\nRequires restart.\nActive value: " + value.get() + "\nConfigured value: " + value.configuredValue() : "");
    }

    private void save() {
        Map<ConfigValue<?>, Object> updates = new LinkedHashMap<>();
        for (ConfigValue<?> value : values) {
            Object draft = drafts.get(value);
            if (Objects.equals(draft, original.get(value))) continue;
            try {
                Object parsed = draft;
                if (value instanceof IntConfigValue) parsed = Integer.valueOf(((String) draft).trim());
                if (value instanceof LongConfigValue) parsed = Long.valueOf(((String) draft).trim());
                if (value instanceof DoubleConfigValue) parsed = Double.valueOf(((String) draft).trim());
                updates.put(value, parsed);
            } catch (NumberFormatException exception) {
                page = values.indexOf(value);
                status = "Invalid number. Select for details.";
                statusDetails = value.path() + ": Enter a valid " + value.declaredType().getSimpleName() + ".";
                rebuildWidgets();
                return;
            }
        }
        ConfigUpdateResult result = config.updateAndSaveAll(updates);
        saved = result.successful();
        if (saved) {
            resetDrafts();
            boolean restart = values.stream().anyMatch(value -> value.restartRequirement() == RestartRequirement.REQUIRED
                    && !Objects.equals(value.configuredValue(), value.get()));
            status = restart ? "Saved. Restart required." : "Saved.";
            statusDetails = status;
        } else {
            status = "Save failed. Select for details.";
            statusDetails = result.issues().stream().map(issue -> issue.path() + ": " + issue.message())
                    .collect(Collectors.joining("\n"));
        }
        rebuildWidgets();
    }
}
