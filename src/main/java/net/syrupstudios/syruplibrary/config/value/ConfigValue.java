package net.syrupstudios.syruplibrary.config.value;

import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RestartRequirement;

import java.util.List;
import java.util.Objects;

/** Immutable definition of one setting. Runtime values belong to its registered config. */
public sealed class ConfigValue<T> permits BooleanConfigValue, IntConfigValue, LongConfigValue,
        DoubleConfigValue, StringConfigValue, StringListConfigValue, EnumConfigValue {
    private final ConfigSpec spec;
    private final String key;
    private final String path;
    private final ConfigType<T> type;
    private final T defaultValue;
    private final List<String> description;
    private final RestartRequirement restartRequirement;
    private final List<ConfigConstraint<T>> constraints;

    public ConfigValue(ConfigSpec spec, String key, String path, ConfigType<T> type, T defaultValue,
                       List<String> description, RestartRequirement restartRequirement,
                       List<ConfigConstraint<T>> constraints) {
        this.spec = Objects.requireNonNull(spec, "spec");
        this.key = Objects.requireNonNull(key, "key");
        this.path = Objects.requireNonNull(path, "path");
        this.type = Objects.requireNonNull(type, "type");
        this.description = List.copyOf(description);
        this.restartRequirement = Objects.requireNonNull(restartRequirement, "restartRequirement");
        this.constraints = List.copyOf(constraints);
        this.defaultValue = validate(defaultValue);
    }

    public final String key() { return key; }
    public final String path() { return path; }
    public final Class<?> declaredType() { return type.javaType(); }
    public final ConfigType<T> type() { return type; }
    public final List<ConfigConstraint<T>> constraints() { return constraints; }
    public final T defaultValue() { return copy(defaultValue); }
    public final List<String> description() { return description; }
    public final RestartRequirement restartRequirement() { return restartRequirement; }
    public final T get() { return spec.effectiveValue(this); }
    public final T configuredValue() { return spec.configuredValue(this); }
    public final T startupValue() { return spec.startupValue(this); }

    /** Checks a Java candidate and returns an independent, normalized value. */
    public final T validate(Object candidate) {
        T normalized = type.normalize(candidate);
        for (ConfigConstraint<T> constraint : constraints) {
            String failure = constraint.validate(normalized);
            if (failure != null) throw new IllegalArgumentException(failure);
        }
        return normalized;
    }

    /** Copies an already validated snapshot value. Does not re-run constraints. */
    public final T cast(Object value) { return type.normalize(value); }
    protected T copy(T value) { return type.normalize(value); }
    protected final ConfigSpec spec() { return spec; }
}
