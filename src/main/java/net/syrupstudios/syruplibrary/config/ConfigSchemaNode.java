package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.ConfigValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable node in the public config schema tree. */
public final class ConfigSchemaNode {
    final String key;
    final String path;
    final List<String> description;
    final Map<String, ConfigSchemaNode> children = new LinkedHashMap<>();
    ConfigValue<?> value;

    ConfigSchemaNode(String key, String path, List<String> description) {
        this.key = key;
        this.path = path;
        this.description = List.copyOf(description);
    }

    public String key() { return key; }
    public String path() { return path; }
    public List<String> description() { return description; }
    public List<ConfigSchemaNode> children() { return List.copyOf(children.values()); }
    public ConfigValue<?> value() { return value; }

    /** Returns whether this node represents a section instead of a value. */
    public boolean isSection() { return value == null; }
}
