package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.value.StringListConfigValue;
//? if >=26 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
 *///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Edits one list item at a time and applies one immutable draft on Done. */
final class StringListEditorScreen extends AbstractConfigScreen {
    private final ConfigEditorContext context;
    private final StringListConfigValue definition;
    private final List<String> items;
    private int selected;
    private EditBox valueBox;
    private Button previousButton;
    private Button nextButton;
    private Button addButton;
    private Button removeButton;
    private Button upButton;
    private Button downButton;
    private Button doneButton;
    private Component error;
    private boolean syncing;

    @SuppressWarnings("unchecked")
    StringListEditorScreen(ConfigEditorContext context) {
        super(Component.translatable("syrup_library.config.list_editor"));
        this.context = context;
        this.definition = (StringListConfigValue) context.valueDefinition();
        this.items = new ArrayList<>((List<String>) context.value());
        this.selected = items.isEmpty() ? -1 : 0;
    }

    @Override
    protected void init() {
        int center = width / 2;
        valueBox = new EditBox(font, center - 150, 54, 300, 20, Component.empty());
        valueBox.setMaxLength(Integer.MAX_VALUE);
        valueBox.setResponder(this::editSelected);
        addRenderableWidget(valueBox);

        previousButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> select(selected - 1))
                .bounds(center - 150, 28, 28, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> select(selected + 1))
                .bounds(center + 122, 28, 28, 20).build());
        addButton = addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.list_add"), button -> add())
                .bounds(center - 150, 82, 70, 20).build());
        removeButton = addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.list_remove"), button -> remove())
                .bounds(center - 74, 82, 70, 20).build());
        upButton = addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.list_up"), button -> move(-1))
                .bounds(center + 4, 82, 70, 20).build());
        downButton = addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.list_down"), button -> move(1))
                .bounds(center + 80, 82, 70, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.cancel"), button -> closeToParent())
                .bounds(center - 154, height - 28, 150, 20).build());
        doneButton = addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.done"), button -> done())
                .bounds(center + 4, height - 28, 150, 20).build());
        syncSelection();
    }

    private void editSelected(String value) {
        if (syncing || selected < 0) return;
        items.set(selected, value);
        validate();
    }

    private void select(int index) {
        if (items.isEmpty()) selected = -1;
        else selected = Math.floorMod(index, items.size());
        syncSelection();
    }

    private void add() {
        if (items.size() >= definition.maximumSize()) return;
        items.add("");
        selected = items.size() - 1;
        syncSelection();
        valueBox.setFocused(true);
    }

    private void remove() {
        if (selected < 0 || items.size() <= definition.minimumSize()) return;
        items.remove(selected);
        if (selected >= items.size()) selected = items.size() - 1;
        syncSelection();
    }

    private void move(int direction) {
        int target = selected + direction;
        if (selected < 0 || target < 0 || target >= items.size()) return;
        String item = items.remove(selected);
        items.add(target, item);
        selected = target;
        syncSelection();
    }

    private void syncSelection() {
        syncing = true;
        try {
            valueBox.setValue(selected < 0 ? "" : items.get(selected));
        } finally {
            syncing = false;
        }
        valueBox.active = selected >= 0;
        previousButton.active = items.size() > 1;
        nextButton.active = items.size() > 1;
        addButton.active = items.size() < definition.maximumSize();
        removeButton.active = selected >= 0 && items.size() > definition.minimumSize();
        upButton.active = selected > 0;
        downButton.active = selected >= 0 && selected < items.size() - 1;
        validate();
    }

    private void validate() {
        error = null;
        if (items.size() < definition.minimumSize() || items.size() > definition.maximumSize()) {
            error = Component.translatable("syrup_library.config.list_size",
                    definition.minimumSize(), definition.maximumSize());
        } else {
            for (String item : items) {
                if (!definition.isItemValid(item)) {
                    error = Component.literal(definition.itemValidationMessage());
                    break;
                }
            }
        }
        if (doneButton != null) doneButton.active = error == null;
    }

    private void done() {
        validate();
        if (error == null && context.setValue(List.copyOf(items))) closeToParent();
    }

    private void closeToParent() { openScreen(context.screen()); }

    @Override public void onClose() { closeToParent(); }

    //? if >=26 {
    /*@Override
    protected void extractConfigContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawCenteredText(graphics, title, width / 2, 8, 0xFFFFFFFF);
        Component position = selected < 0 ? Component.translatable("syrup_library.config.list_empty")
                : Component.translatable("syrup_library.config.list_position", selected + 1, items.size());
        drawCenteredText(graphics, position, width / 2, 34, 0xFFA0A0A0);
        if (error != null) drawCenteredText(graphics, error, width / 2, 108, 0xFFFF5555);
    }
     *///?} else {
    @Override
    protected void renderConfigContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawCenteredText(graphics, title, width / 2, 8, 0xFFFFFFFF);
        Component position = selected < 0 ? Component.translatable("syrup_library.config.list_empty")
                : Component.translatable("syrup_library.config.list_position", selected + 1, items.size());
        drawCenteredText(graphics, position, width / 2, 34, 0xFFA0A0A0);
        if (error != null) drawCenteredText(graphics, error, width / 2, 108, 0xFFFF5555);
    }
    //?}
}
