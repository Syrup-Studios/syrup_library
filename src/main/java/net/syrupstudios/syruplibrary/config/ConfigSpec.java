package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.ConfigValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/** Root declaration and runtime state for one typed configuration file. */
public final class ConfigSpec extends ConfigContainer {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9_-]{0,63}");
    private static final Pattern KEY_PATTERN = Pattern.compile("[a-z][a-z0-9_]*");

    private final String id;
    private final String ownerId;
    private final List<String> header;
    private final SchemaNode root = new SchemaNode("", "", List.of());
    private final List<ConfigValue<?>> values = new ArrayList<>();
    private final AtomicBoolean registered = new AtomicBoolean();
    private volatile ConfigState state;
    private volatile boolean sealed;

    private ConfigSpec(String id, String ownerId, List<String> header) {
        this.id = validateId(id);
        this.ownerId = validateId(ownerId);
        this.header = List.copyOf(header);
        ConfigSnapshot empty = new ConfigSnapshot(Map.of());
        this.state = new ConfigState(empty, empty, empty);
    }

    /** Starts a spec declaration for a validated config ID. */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** Returns the file/config ID. */
    public String id() { return id; }

    /** Returns the mod ID that owns this configuration. */
    public String ownerId() { return ownerId; }

    /** Returns immutable file header lines. */
    public List<String> header() { return header; }

    /** Returns values in schema declaration order. */
    public List<ConfigValue<?>> values() { return List.copyOf(values); }

    @Override
    ConfigSpec spec() { return this; }

    @Override
    SchemaNode node() { return root; }

    ConfigSection addSection(SchemaNode parent, String key, List<String> description) {
        ensureMutable();
        validateKey(key);
        if (parent.children.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate config path: " + childPath(parent, key));
        }
        SchemaNode child = new SchemaNode(key, childPath(parent, key), description);
        parent.children.put(key, child);
        return new ConfigSection(this, child);
    }

    <T extends ConfigValue<?>> T addValue(SchemaNode parent, String key, T value) {
        ensureMutable();
        validateKey(key);
        if (parent.children.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate config path: " + childPath(parent, key));
        }
        SchemaNode child = new SchemaNode(key, childPath(parent, key), value.description());
        child.value = value;
        parent.children.put(key, child);
        values.add(value);
        state = null;
        return value;
    }

    String childPath(SchemaNode parent, String key) {
        return parent.path.isEmpty() ? key : parent.path + "." + key;
    }

    ConfigState currentState() {
        ConfigState current = state;
        if (current == null) {
            synchronized (this) {
                current = state;
                if (current == null) {
                    publishDefaults();
                    current = state;
                }
            }
        }
        return current;
    }

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

    void publish(ConfigState newState) { state = newState; }

    synchronized void sealForRegistration() {
        if (!registered.compareAndSet(false, true)) {
            throw new IllegalStateException("Config spec is already registered: " + id);
        }
        sealed = true;
        currentState();
    }

    SchemaNode root() { return root; }

    private void publishDefaults() {
        Map<ConfigValue<?>, Object> defaults = new LinkedHashMap<>();
        for (ConfigValue<?> value : values) {
            defaults.put(value, value.defaultValue());
        }
        ConfigSnapshot snapshot = new ConfigSnapshot(defaults);
        state = new ConfigState(snapshot, snapshot, snapshot);
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
        private String ownerId;
        private final List<String> header = new ArrayList<>();

        private Builder(String id) {
            this.id = validateId(id);
            this.ownerId = this.id;
        }

        /** Sets the owning mod ID used by loader config integrations. */
        public Builder owner(String ownerId) {
            this.ownerId = validateId(ownerId);
            return this;
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
            return new ConfigSpec(id, ownerId, header);
        }
    }
}
