package net.syrupstudios.syruplibrary.client.config;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.List;

/** One-widget editor handle used by standard config controls. */
final class SimpleConfigEditorHandle implements ConfigEditorHandle {
    private final AbstractWidget widget;
    private final Runnable refresh;

    SimpleConfigEditorHandle(AbstractWidget widget, Runnable refresh) {
        this.widget = widget;
        this.refresh = refresh;
    }

    @Override public List<? extends AbstractWidget> widgets() { return List.of(widget); }

    @Override public AbstractWidget primaryWidget() { return widget; }

    @Override public void setBounds(int x, int y, int width, int height) {
        widget.setX(x);
        widget.setY(y);
        widget.setWidth(width);
    }

    @Override public void refresh() { refresh.run(); }
}
