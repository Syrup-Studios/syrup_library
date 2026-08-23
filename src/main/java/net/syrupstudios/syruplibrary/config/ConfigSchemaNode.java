package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.ConfigValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable node in the public config schema tree. */
public final class ConfigSchemaNode implements ConfigScreenElement {
    final ConfigSpec spec;
    final String key;
    final String path;
    final List<String> description;
    final Map<String, ConfigSchemaNode> children = new LinkedHashMap<>();
    final List<ConfigScreenElement> screenElements = new java.util.ArrayList<>();
    final Map<String, ConfigGroup> groups = new LinkedHashMap<>();
    ConfigValue<?> value;
    ConfigPresentation presentation;

    ConfigSchemaNode(ConfigSpec spec, String key, String path, List<String> description) {
        this(spec, key, path, description, ConfigPresentation.DEFAULT);
    }

    ConfigSchemaNode(ConfigSpec spec, String key, String path, List<String> description,
                     ConfigPresentation presentation) {
        this.spec = spec;
        this.key = key;
        this.path = path;
        this.description = List.copyOf(description);
        this.presentation = presentation;
    }

    public String key() { return key; }
    public ConfigSpec spec() { return spec; }
    public String path() { return path; }
    public List<String> description() { return description; }
    public List<ConfigSchemaNode> children() { return List.copyOf(children.values()); }
    public List<ConfigScreenElement> screenElements() { return List.copyOf(screenElements); }
    public List<ConfigGroup> groups() { return List.copyOf(groups.values()); }
    public ConfigValue<?> value() { return value; }

    @Override
    public ConfigPresentation presentation() { return presentation; }

    /** Returns whether this node represents a section instead of a value. */
    public boolean isSection() { return value == null; }
}
