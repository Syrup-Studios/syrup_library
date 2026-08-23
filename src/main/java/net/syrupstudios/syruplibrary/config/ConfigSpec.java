package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.ConfigValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/** Root declaration and runtime state for one typed configuration file. */
public final class ConfigSpec extends ConfigContainer {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9_-]{0,63}");
    private static final Pattern KEY_PATTERN = Pattern.compile("[a-z][a-z0-9_]*");

    private final String id;
    private final List<String> header;
    private final ConfigSchemaNode root;
    private final List<ConfigValue<?>> values = new ArrayList<>();
    private final AtomicBoolean registered = new AtomicBoolean();
    private final AtomicReference<ConfigState> state;
    private volatile boolean sealed;
    private final String translationPrefix;

    private ConfigSpec(String id, List<String> header, String translationPrefix) {
        this.id = validateId(id);
        this.header = List.copyOf(header);
        this.translationPrefix = translationPrefix;
        this.root = new ConfigSchemaNode(this, "", "", List.of());
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

    /** Returns the optional client translation-key prefix. */
    public Optional<String> translationPrefix() { return Optional.ofNullable(translationPrefix); }

    /** Returns an immutable public view of the schema tree. */
    public ConfigSchemaNode schema() { return root; }

    /** Returns values in schema declaration order. */
    public List<ConfigValue<?>> values() { return List.copyOf(values); }

    @Override
    ConfigSpec spec() { return this; }

    @Override
    ConfigSchemaNode node() { return root; }

    ConfigSection addSection(ConfigSchemaNode parent, String key, List<String> description) {
        return addSection(parent, key, description, ConfigPresentation.DEFAULT);
    }

    ConfigSection addSection(ConfigSchemaNode parent, String key, List<String> description,
                             ConfigPresentation presentation) {
        ensureMutable();
        validateKey(key);
        if (parent.children.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate config path: " + childPath(parent, key));
        }
        ConfigSchemaNode child = new ConfigSchemaNode(
                this, key, childPath(parent, key), description, Objects.requireNonNull(presentation, "presentation"));
        parent.children.put(key, child);
        parent.screenElements.add(child);
        return new ConfigSection(this, child);
    }

    <T extends ConfigValue<?>> T addValue(ConfigSchemaNode parent, String key, T value) {
        return addValue(parent, key, value, ConfigPresentation.DEFAULT);
    }

    <T extends ConfigValue<?>> T addValue(ConfigSchemaNode parent, String key, T value,
                                          ConfigPresentation presentation) {
        ensureMutable();
        validateKey(key);
        if (parent.children.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate config path: " + childPath(parent, key));
        }
        ConfigSchemaNode child = new ConfigSchemaNode(
                this, key, childPath(parent, key), value.description(), Objects.requireNonNull(presentation, "presentation"));
        child.value = value;
        parent.children.put(key, child);
        parent.screenElements.add(child);
        values.add(value);
        publishDefaults();
        return value;
    }

    ConfigInfoRow addInfo(ConfigSchemaNode parent, ConfigInfoRow row) {
        ensureMutable();
        Objects.requireNonNull(row, "row");
        validateKey(row.id());
        boolean duplicate = parent.screenElements.stream()
                .anyMatch(element -> element instanceof ConfigInfoRow info && info.id().equals(row.id()));
        if (duplicate || parent.children.containsKey(row.id())) {
            throw new IllegalArgumentException("Duplicate screen element ID in " + displayPath(parent) + ": " + row.id());
        }
        parent.screenElements.add(row);
        return row;
    }

    ConfigGroup addGroup(ConfigSchemaNode parent, String id) {
        ensureMutable();
        validateKey(id);
        if (parent.groups.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate config group in " + displayPath(parent) + ": " + id);
        }
        ConfigGroup group = new ConfigGroup(this, parent, id);
        parent.groups.put(id, group);
        return group;
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
        validatePresentation();
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

    void ensureMutable() {
        if (sealed) {
            throw new IllegalStateException("Config spec is sealed after registration: " + id);
        }
    }

    private void validatePresentation() {
        validateNodePresentation(root);
        Map<ConfigValue<?>, Set<ConfigValue<?>>> graph = new LinkedHashMap<>();
        collectConditionGraph(root, graph);
        detectConditionCycles(graph);
    }

    private void validateNodePresentation(ConfigSchemaNode parent) {
        for (ConfigGroup group : parent.groups.values()) {
            validateCondition(group.visibilityCondition(), "group " + group.id());
        }
        for (ConfigScreenElement element : parent.screenElements) {
            ConfigPresentation presentation = element.presentation();
            String target;
            if (element instanceof ConfigSchemaNode node) {
                target = node.path();
                validateGroup(presentation, parent, target);
                validateCondition(presentation.visibilityCondition(), target);
                validateCondition(presentation.enabledCondition(), target);
                if (node.value() != null) validateEditor(node.value(), presentation.editor());
                if (node.isSection()) validateNodePresentation(node);
            } else {
                ConfigInfoRow info = (ConfigInfoRow) element;
                target = "information row " + info.id();
                validateGroup(presentation, parent, target);
                validateCondition(presentation.visibilityCondition(), target);
            }
        }
    }

    private void validateGroup(ConfigPresentation presentation, ConfigSchemaNode parent, String target) {
        presentation.group().ifPresent(group -> {
            if (group.spec() != this || group.owner() != parent) {
                throw new IllegalArgumentException("Config group for " + target + " belongs to another section");
            }
        });
    }

    private void validateCondition(ConfigCondition condition, String target) {
        for (ConfigValue<?> dependency : condition.dependencies()) {
            if (!containsIdentity(values, dependency)) {
                throw new IllegalArgumentException("Condition for " + target + " references another config spec");
            }
        }
    }

    private static boolean containsIdentity(List<ConfigValue<?>> values, ConfigValue<?> candidate) {
        for (ConfigValue<?> value : values) if (value == candidate) return true;
        return false;
    }

    private static void validateEditor(ConfigValue<?> value, ConfigEditorHint editor) {
        switch (editor.kind()) {
            case AUTO, CUSTOM -> {
            }
            case SLIDER -> {
                if (!(value instanceof net.syrupstudios.syruplibrary.config.value.IntConfigValue)
                        && !(value instanceof net.syrupstudios.syruplibrary.config.value.LongConfigValue)
                        && !(value instanceof net.syrupstudios.syruplibrary.config.value.DoubleConfigValue)) {
                    throw new IllegalArgumentException("Slider editor requires a numeric value: " + value.path());
                }
                Number step = editor.step();
                if (step != null && value instanceof net.syrupstudios.syruplibrary.config.value.IntConfigValue
                        && (step.doubleValue() != step.intValue() || step.intValue() <= 0)) {
                    throw new IllegalArgumentException("Integer slider step must be a positive integer: " + value.path());
                }
                if (step != null && value instanceof net.syrupstudios.syruplibrary.config.value.LongConfigValue
                        && (step.doubleValue() != step.longValue() || step.longValue() <= 0)) {
                    throw new IllegalArgumentException("Long slider step must be a positive whole number: " + value.path());
                }
            }
            case COLOR, MULTILINE, PATH -> {
                if (!(value instanceof net.syrupstudios.syruplibrary.config.value.StringConfigValue)) {
                    throw new IllegalArgumentException(editor.kind() + " editor requires a string value: " + value.path());
                }
                if (editor.kind() == ConfigEditorHint.Kind.COLOR) {
                    String color = (String) value.defaultValue();
                    String pattern = editor.alpha() ? "#[0-9a-fA-F]{8}" : "#[0-9a-fA-F]{6}";
                    if (!color.matches(pattern)) {
                        throw new IllegalArgumentException("Color default must match " + pattern + ": " + value.path());
                    }
                }
            }
        }
    }

    private void collectConditionGraph(ConfigSchemaNode parent,
                                       Map<ConfigValue<?>, Set<ConfigValue<?>>> graph) {
        for (ConfigScreenElement element : parent.screenElements) {
            if (!(element instanceof ConfigSchemaNode node)) continue;
            if (node.value() != null) {
                LinkedHashSet<ConfigValue<?>> dependencies = new LinkedHashSet<>();
                dependencies.addAll(node.presentation().visibilityCondition().dependencies());
                dependencies.addAll(node.presentation().enabledCondition().dependencies());
                if (dependencies.contains(node.value())) {
                    throw new IllegalArgumentException("Config entry condition depends on itself: " + node.path());
                }
                graph.put(node.value(), Set.copyOf(dependencies));
            } else {
                collectConditionGraph(node, graph);
            }
        }
    }

    private static void detectConditionCycles(Map<ConfigValue<?>, Set<ConfigValue<?>>> graph) {
        Set<ConfigValue<?>> visiting = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Set<ConfigValue<?>> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (ConfigValue<?> value : graph.keySet()) visit(value, graph, visiting, visited);
    }

    private static void visit(ConfigValue<?> value, Map<ConfigValue<?>, Set<ConfigValue<?>>> graph,
                              Set<ConfigValue<?>> visiting, Set<ConfigValue<?>> visited) {
        if (visited.contains(value)) return;
        if (!visiting.add(value)) {
            throw new IllegalArgumentException("Config entry conditions contain a cycle at " + value.path());
        }
        for (ConfigValue<?> dependency : graph.getOrDefault(value, Set.of())) {
            if (graph.containsKey(dependency)) visit(dependency, graph, visiting, visited);
        }
        visiting.remove(value);
        visited.add(value);
    }

    private static String displayPath(ConfigSchemaNode node) {
        return node.path.isEmpty() ? "root" : node.path;
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
        private String translationPrefix;

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

        /** Sets the translation-key prefix used only by config screens. */
        public Builder translationPrefix(String prefix) {
            this.translationPrefix = ConfigPresentation.translationKey(prefix);
            return this;
        }

        /** Builds an unregistered spec; values and sections may be declared until registration. */
        public ConfigSpec build() {
            return new ConfigSpec(id, header, translationPrefix);
        }
    }
}
