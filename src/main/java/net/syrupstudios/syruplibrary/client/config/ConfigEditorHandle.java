package net.syrupstudios.syruplibrary.client.config;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.List;

/** Runtime handle for widgets owned by one config value editor. */
public interface ConfigEditorHandle {
    List<? extends AbstractWidget> widgets();

    AbstractWidget primaryWidget();

    void setBounds(int x, int y, int width, int height);

    void refresh();

    default void setActive(boolean active) {
        for (AbstractWidget widget : widgets()) widget.active = active;
    }

    default void close() {
    }
}
