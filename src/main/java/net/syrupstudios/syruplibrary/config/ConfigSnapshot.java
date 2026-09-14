package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.ConfigValue;

import java.util.Map;

/** Immutable, internally consistent view of all values in one config spec. */
public final class ConfigSnapshot {
    private final Map<ConfigValue<?>, Object> values;

    ConfigSnapshot(Map<ConfigValue<?>, Object> values) {
        this.values = Map.copyOf(values);
    }

    /** Returns a value from this exact snapshot. */
    public <T> T get(ConfigValue<T> value) {
        if (!values.containsKey(value)) {
            throw new IllegalArgumentException("Value does not belong to this snapshot: " + value.path());
        }
        return value.cast(values.get(value));
    }

    Map<ConfigValue<?>, Object> values() {
        return values;
    }
}
