package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.ConfigSchemaNode;
import net.syrupstudios.syruplibrary.config.edit.ConfigEditSession;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

/** Context supplied to built-in and custom client editor factories. */
public final class ConfigEditorContext {
    private final ConfigSectionScreen screen;
    private final ConfigSchemaNode node;

    ConfigEditorContext(ConfigSectionScreen screen, ConfigSchemaNode node) {
        this.screen = screen;
        this.node = node;
    }

    public ConfigSchemaNode node() { return node; }

    public ConfigValue<?> valueDefinition() { return node.value(); }

    public ConfigEditSession session() { return screen.session(); }

    public Object value() { return untypedValue(node.value()); }

    private Object untypedValue(ConfigValue<?> value) {
        return session().value(value);
    }

    public boolean setValue(Object candidate) {
        boolean accepted = session().setValue(node.value(), candidate);
        screen.markChanged(node.value());
        return accepted;
    }

    public void reset() {
        session().reset(node.value());
        screen.markChanged(node.value());
    }

    public Font font() { return screen.fontInstance(); }

    public Screen screen() { return screen; }

    public void openScreen(Screen next) { screen.openScreen(next); }
}
