package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.ConfigValue;
import net.syrupstudios.syruplibrary.loaders.Platform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** A client-neutral condition used to control entry visibility or editing. */
public final class ConfigCondition {
    private enum Operation {
        ALWAYS,
        EQUALS,
        ONE_OF,
        MOD_LOADED,
        ALL_OF,
        ANY_OF,
        NOT
    }

    private static final ConfigCondition ALWAYS = new ConfigCondition(
            Operation.ALWAYS, null, List.of(), null, List.of());

    private final Operation operation;
    private final ConfigValue<?> value;
    private final List<Object> expectedValues;
    private final String modId;
    private final List<ConfigCondition> children;
    private final Set<ConfigValue<?>> dependencies;

    private ConfigCondition(Operation operation, ConfigValue<?> value, List<Object> expectedValues,
                            String modId, List<ConfigCondition> children) {
        this.operation = operation;
        this.value = value;
        this.expectedValues = List.copyOf(expectedValues);
        this.modId = modId;
        this.children = List.copyOf(children);
        LinkedHashSet<ConfigValue<?>> dependencies = new LinkedHashSet<>();
        if (value != null) dependencies.add(value);
        for (ConfigCondition child : children) dependencies.addAll(child.dependencies);
        this.dependencies = Set.copyOf(dependencies);
    }

    public static ConfigCondition always() { return ALWAYS; }

    public static ConfigCondition isTrue(ConfigValue<Boolean> value) {
        return equals(value, true);
    }

    public static <T> ConfigCondition equals(ConfigValue<T> value, T expected) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(expected, "expected");
        return new ConfigCondition(Operation.EQUALS, value, List.of(value.cast(expected)), null, List.of());
    }

    public static <T> ConfigCondition oneOf(ConfigValue<T> value, Collection<? extends T> expected) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(expected, "expected");
        if (expected.isEmpty()) throw new IllegalArgumentException("A one-of condition needs at least one value");
        List<Object> values = new ArrayList<>();
        for (T candidate : expected) values.add(value.cast(Objects.requireNonNull(candidate, "expected value")));
        return new ConfigCondition(Operation.ONE_OF, value, values, null, List.of());
    }

    public static ConfigCondition modLoaded(String modId) {
        Objects.requireNonNull(modId, "modId");
        if (!modId.matches("[a-z][a-z0-9_-]*")) {
            throw new IllegalArgumentException("Invalid mod ID: " + modId);
        }
        return new ConfigCondition(Operation.MOD_LOADED, null, List.of(), modId, List.of());
    }

    public static ConfigCondition allOf(ConfigCondition... conditions) {
        return compound(Operation.ALL_OF, conditions);
    }

    public static ConfigCondition anyOf(ConfigCondition... conditions) {
        return compound(Operation.ANY_OF, conditions);
    }

    public static ConfigCondition not(ConfigCondition condition) {
        return new ConfigCondition(Operation.NOT, null, List.of(), null,
                List.of(Objects.requireNonNull(condition, "condition")));
    }

    private static ConfigCondition compound(Operation operation, ConfigCondition[] conditions) {
        Objects.requireNonNull(conditions, "conditions");
        if (conditions.length == 0) throw new IllegalArgumentException("A compound condition cannot be empty");
        List<ConfigCondition> children = new ArrayList<>();
        for (ConfigCondition condition : conditions) {
            children.add(Objects.requireNonNull(condition, "condition"));
        }
        return new ConfigCondition(operation, null, List.of(), null, children);
    }

    /** Returns every config value read by this condition. */
    public Set<ConfigValue<?>> dependencies() { return dependencies; }

    /** Evaluates this condition with values supplied by a snapshot or edit session. */
    public boolean test(Function<ConfigValue<?>, Object> values) {
        Objects.requireNonNull(values, "values");
        return switch (operation) {
            case ALWAYS -> true;
            case EQUALS -> Objects.equals(values.apply(value), expectedValues.get(0));
            case ONE_OF -> expectedValues.contains(values.apply(value));
            case MOD_LOADED -> Platform.INSTANCE.isModLoaded(modId);
            case ALL_OF -> children.stream().allMatch(child -> child.test(values));
            case ANY_OF -> children.stream().anyMatch(child -> child.test(values));
            case NOT -> !children.get(0).test(values);
        };
    }
}
