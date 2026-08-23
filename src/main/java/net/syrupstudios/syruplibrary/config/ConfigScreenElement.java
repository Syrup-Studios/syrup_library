package net.syrupstudios.syruplibrary.config;

/** One ordered element shown in a config section screen. */
public sealed interface ConfigScreenElement permits ConfigSchemaNode, ConfigInfoRow {
    ConfigPresentation presentation();
}
