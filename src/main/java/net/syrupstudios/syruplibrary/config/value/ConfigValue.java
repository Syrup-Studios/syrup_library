package net.syrupstudios.syruplibrary.config.value;

import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RestartRequirement;

import java.util.List;
import java.util.Objects;

/** Base type for a typed configuration value. */
public abstract sealed class ConfigValue<T>
        permits BooleanConfigValue, DoubleConfigValue, EnumConfigValue, IntConfigValue,
        LongConfigValue, StringConfigValue, StringListConfigValue {
    private final ConfigSpec spec;
    private final String key;
    private final String path;
    private final Class<?> declaredType;
    private final T defaultValue;
    private final List<String> description;
    private final RestartRequirement restartRequirement;

    protected ConfigValue(
            ConfigSpec spec,
            String key,
            String path,
            Class<?> declaredType,
            T defaultValue,
            List<String> description,
            RestartRequirement restartRequirement
    ) {
        this.spec = Objects.requireNonNull(spec, "spec");
        this.key = Objects.requireNonNull(key, "key");
        this.path = Objects.requireNonNull(path, "path");
        this.declaredType = Objects.requireNonNull(declaredType, "declaredType");
        this.defaultValue = copy(Objects.requireNonNull(defaultValue, "defaultValue"));
        this.description = List.copyOf(description);
        this.restartRequirement = Objects.requireNonNull(restartRequirement, "restartRequirement");
    }

    /** Returns the local stable key. */
    public final String key() {
        return key;
    }

    /** Returns the dot-separated path within the config. */
    public final String path() {
        return path;
    }

    /** Returns the declared Java type. */
    public final Class<?> declaredType() {
        return declaredType;
    }

    /** Returns the schema default. */
    public final T defaultValue() {
        return copy(defaultValue);
    }

    /** Returns immutable description lines. */
    public final List<String> description() {
        return description;
    }

    /** Returns restart metadata for this setting. */
    public final RestartRequirement restartRequirement() {
        return restartRequirement;
    }

    /** Returns the value currently effective in the running process. */
    public final T get() {
        return spec.effectiveValue(this);
    }

    /** Returns the latest successfully parsed value, including restart-pending edits. */
    public final T configuredValue() {
        return spec.configuredValue(this);
    }

    /** Returns the value captured during initial loading. */
    public final T startupValue() {
        return spec.startupValue(this);
    }

    @SuppressWarnings("unchecked")
    public final T cast(Object value) {
        return copy((T) value);
    }

    protected T copy(T value) {
        return value;
    }

    protected final ConfigSpec spec() {
        return spec;
    }

    /** Returns the config spec that owns this value. */
    public final ConfigSpec specReference() { return spec; }
}
