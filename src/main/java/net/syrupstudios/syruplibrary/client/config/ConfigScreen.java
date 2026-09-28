package net.syrupstudios.syruplibrary.client.config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Native widgets and version-specific screen calls shared by the config screens. */
public abstract class ConfigScreen extends Screen {
    protected final Screen parent;
    private Component editorTooltip;

    ConfigScreen(Screen parent, Component title) {
        super(title);
        this.parent = parent;
    }

    public int contentWidth() { return Math.min(360, width - 24); }
    public int left() { return (width - contentWidth()) / 2; }
    public net.minecraft.client.gui.Font clientFont() { return font; }
    public int clientHeight() { return height; }
    public <W extends net.minecraft.client.gui.components.AbstractWidget> W widget(W widget) {
        if (editorTooltip != null && !(widget instanceof Button)) widget.setTooltip(Tooltip.create(editorTooltip));
        return addRenderableWidget(widget);
    }
    protected void editorTooltip(Component tooltip) { editorTooltip = tooltip; }

    public Button button(String text, int x, int y, int width, Runnable action) {
        Button button = Button.builder(Component.literal(text), ignored -> action.run())
                .bounds(x, y, width, 20).build();
        button.setTooltip(Tooltip.create(Component.literal(text)));
        return widget(button);
    }

    public void label(String text, int y) {
        int width = contentWidth();
        String shown = font.plainSubstrByWidth(text, width);
        StringWidget widget = new StringWidget(left(), y, width, 12,
                Component.literal(shown), font);
        widget.setWidth(font.width(shown));
        widget(widget);
    }

    protected void centeredLabel(String text, int x, int y, int width) {
        String shown = font.plainSubstrByWidth(text, width);
        int textWidth = font.width(shown);
        StringWidget widget = new StringWidget(x + (width - textWidth) / 2, y, textWidth, 12,
                Component.literal(shown), font);
        addRenderableOnly(widget);
    }

    public void textField(String text, String label, int y, int fieldHeight, Consumer<String> changed) {
        Component name = Component.literal(label);
        //? if >=1.21.11 {
        MultiLineEditBox field = MultiLineEditBox.builder().setX(left()).setY(y)
                .build(font, contentWidth(), fieldHeight, name);
        //?} else {
        /*MultiLineEditBox field = new MultiLineEditBox(font, left(), y,
                contentWidth(), fieldHeight,
                Component.empty(), name);
        *///?}
        field.setCharacterLimit(Integer.MAX_VALUE);
        field.setValue(text);
        field.setValueListener(changed);
        widget(field);
    }

    protected void show(Screen screen) {
        //? if >=26 {
        minecraft.setScreenAndShow(screen);
        //?} else
        //minecraft.setScreen(screen);
    }

    protected void details(String title, String text) {
        show(new DetailsScreen(this, title, text));
    }

    @Override public void onClose() { show(parent); }

    private static final class DetailsScreen extends ConfigScreen {
        private final String text;
        private int page;

        DetailsScreen(Screen parent, String title, String text) {
            super(parent, Component.literal(title));
            this.text = text;
        }

        @Override protected void init() {
            List<String> lines = new ArrayList<>();
            font.split(Component.literal(text), contentWidth()).forEach(line -> {
                StringBuilder plain = new StringBuilder();
                line.accept((index, style, codePoint) -> { plain.appendCodePoint(codePoint); return true; });
                lines.add(plain.toString());
            });
            int rows = Math.max(1, (height - 92) / 12);
            int pages = Math.max(1, (lines.size() + rows - 1) / rows);
            page = Math.min(page, pages - 1);
            label(title.getString(), 12);
            for (int i = page * rows; i < Math.min(lines.size(), (page + 1) * rows); i++) {
                label(lines.get(i), 34 + (i % rows) * 12);
            }
            button("Previous", left(), height - 52, 90, () -> { page--; rebuildWidgets(); }).active = page > 0;
            button("Next", left() + contentWidth() - 90, height - 52, 90,
                    () -> { page++; rebuildWidgets(); }).active = page + 1 < pages;
            button("Back", left(), height - 28, contentWidth(), this::onClose);
        }
    }
}
