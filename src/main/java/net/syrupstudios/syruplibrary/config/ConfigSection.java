package net.syrupstudios.syruplibrary.config;

import java.util.List;

/** A named hierarchical section in a configuration schema. */
public final class ConfigSection extends ConfigContainer {
    private final ConfigSpec spec;
    private final ConfigSchemaNode node;

    ConfigSection(ConfigSpec spec, ConfigSchemaNode node) {
        this.spec = spec;
        this.node = node;
    }

    /** Returns the section's local key. */
    public String key() { return node.key; }

    /** Returns the section's dot-separated path. */
    public String path() { return node.path; }

    /** Returns immutable description lines. */
    public List<String> description() { return node.description; }

    @Override
    ConfigSpec spec() { return spec; }

    @Override
    ConfigSchemaNode node() { return node; }
}
