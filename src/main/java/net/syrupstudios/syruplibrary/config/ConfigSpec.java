package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.ConfigValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/** Root declaration and runtime state for one typed configuration file. */
public final class ConfigSpec extends ConfigContainer {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9_-]{0,63}");
    private static final Pattern KEY_PATTERN = Pattern.compile("[a-z][a-z0-9_]*");

    private final String id;
    private final List<String> header;
    private final ConfigSchemaNode root = new ConfigSchemaNode("", "", List.of());
    private final List<ConfigValue<?>> values = new ArrayList<>();
    private final AtomicBoolean registered = new AtomicBoolean();
    private final AtomicReference<ConfigState> state;
    private volatile boolean sealed;

    private ConfigSpec(String id, List<String> header) {
        this.id = validateId(id);
        this.header = List.copyOf(header);
        ConfigSnapshot empty = new ConfigSnapshot(Map.of());
        this.state = new AtomicReference<>(new ConfigState(empty, empty, empty));
    }

    /** Starts a spec declaration for a validated config ID. */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** Returns the file/config ID. */
    public String id() { return id; }

    /** Returns immutable file header lines. */
    public List<String> header() { return header; }

    /** Returns an immutable public view of the schema tree. */
    public ConfigSchemaNode schema() { return root; }

    /** Returns values in schema declaration order. */
    public List<ConfigValue<?>> values() { return List.copyOf(values); }

    @Override
    ConfigSpec spec() { return this; }

    @Override
    ConfigSchemaNode node() { return root; }

    ConfigSection addSection(ConfigSchemaNode parent, String key, List<String> description) {
        ensureMutable();
        validateKey(key);
        if (parent.children.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate config path: " + childPath(parent, key));
        }
        ConfigSchemaNode child = new ConfigSchemaNode(key, childPath(parent, key), description);
        parent.children.put(key, child);
        return new ConfigSection(this, child);
    }

    <T extends ConfigValue<?>> T addValue(ConfigSchemaNode parent, String key, T value) {
        ensureMutable();
        validateKey(key);
        if (parent.children.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate config path: " + childPath(parent, key));
        }
        ConfigSchemaNode child = new ConfigSchemaNode(key, childPath(parent, key), value.description());
        child.value = value;
        parent.children.put(key, child);
        values.add(value);
        publishDefaults();
        return value;
    }

    String childPath(ConfigSchemaNode parent, String key) {
        return parent.path.isEmpty() ? key : parent.path + "." + key;
    }

    ConfigState currentState() { return state.get(); }

    /** Returns a typed value from the current effective snapshot. */
    public <T> T effectiveValue(ConfigValue<T> value) {
        return currentState().effective().get(value);
    }

    /** Returns a typed value from the latest configured snapshot. */
    public <T> T configuredValue(ConfigValue<T> value) {
        return currentState().configured().get(value);
    }

    /** Returns a typed value from the initial startup snapshot. */
    public <T> T startupValue(ConfigValue<T> value) {
        return currentState().startup().get(value);
    }

    void publish(ConfigState newState) { state.set(newState); }

    void sealForRegistration() {
        if (!registered.compareAndSet(false, true)) {
            throw new IllegalStateException("Config spec is already registered: " + id);
        }
        sealed = true;
        publishDefaults();
    }

    ConfigSchemaNode root() { return root; }

    private void publishDefaults() {
        Map<ConfigValue<?>, Object> defaults = new LinkedHashMap<>();
        for (ConfigValue<?> value : values) {
            defaults.put(value, value.defaultValue());
        }
        ConfigSnapshot snapshot = new ConfigSnapshot(defaults);
        state.set(new ConfigState(snapshot, snapshot, snapshot));
    }

    private void ensureMutable() {
        if (sealed) {
            throw new IllegalStateException("Config spec is sealed after registration: " + id);
        }
    }

    private static String validateId(String id) {
        Objects.requireNonNull(id, "id");
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "Invalid config ID '" + id + "'; expected lowercase letters, digits, '_' or '-' without paths");
        }
        return id;
    }

    private static void validateKey(String key) {
        Objects.requireNonNull(key, "key");
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid config key '" + key + "'; expected [a-z][a-z0-9_]*");
        }
    }

    static List<String> description(String description) {
        if (description == null || description.isBlank()) {
            return List.of();
        }
        return description.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
    }

    /** Builder for immutable spec identity and header metadata. */
    public static final class Builder {
        private final String id;
        private final List<String> header = new ArrayList<>();

        private Builder(String id) {
            this.id = validateId(id);
        }

        /** Replaces the generated file header with the supplied lines. */
        public Builder header(String... lines) {
            header.clear();
            for (String line : lines) {
                if (line != null && !line.isBlank()) {
                    header.add(line.strip());
                }
            }
            return this;
        }

        /** Builds an unregistered spec; values and sections may be declared until registration. */
        public ConfigSpec build() {
            return new ConfigSpec(id, header);
        }
    }
}
