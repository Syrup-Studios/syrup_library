package net.syrupstudios.syruplibrary.client.config;

/** Creates a client config editor for one schema value. */
@FunctionalInterface
public interface ConfigEditorFactory {
    ConfigEditorHandle create(ConfigEditorContext context);
}
