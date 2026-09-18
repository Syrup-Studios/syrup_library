package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.ConfigValue;
import net.syrupstudios.syruplibrary.config.value.ConfigType;
import net.syrupstudios.syruplibrary.config.value.ConfigConstraint;

import java.util.List;

/** Common declaration API shared by a root spec and nested section. */
public abstract class ConfigContainer {
    abstract ConfigSpec spec();

    abstract SchemaNode node();

    /** Declares a generic value with no additional constraints. */
    public final <T> ConfigValue<T> value(String key, ConfigType<T> type, T defaultValue, String description) {
        return value(key, type, defaultValue, description, RestartRequirement.NONE, List.of());
    }

    /** Declares a built-in or custom type with shared validation and restart metadata. */
    public final <T> ConfigValue<T> value(String key, ConfigType<T> type, T defaultValue, String description,
                                          RestartRequirement restart, List<ConfigConstraint<T>> constraints) {
        return spec().addValue(node(), key, new ConfigValue<>(spec(), key, spec().childPath(node(), key),
                type, defaultValue, ConfigSpec.description(description), restart, constraints));
    }

    public final ConfigValue<Boolean> bool(String key, boolean defaultValue, String description) {
        return value(key, ConfigType.BOOLEAN, defaultValue, description);
    }

    public final ConfigValue<Integer> integer(String key, int defaultValue, int minimum, int maximum,
                                               String description) {
        return value(key, ConfigType.INTEGER, defaultValue, description, RestartRequirement.NONE,
                List.of(ConfigConstraint.range(minimum, maximum)));
    }

    public final ConfigValue<Long> longValue(String key, long defaultValue, String description) {
        return value(key, ConfigType.LONG, defaultValue, description);
    }

    public final ConfigValue<Double> doubleValue(String key, double defaultValue, String description) {
        return value(key, ConfigType.DOUBLE, defaultValue, description);
    }

    public final ConfigValue<String> string(String key, String defaultValue, String description) {
        return value(key, ConfigType.STRING, defaultValue, description);
    }

    public final ConfigValue<List<String>> stringList(String key, List<String> defaultValue, String description) {
        return value(key, ConfigType.STRING_LIST, defaultValue, description);
    }

    public final <E extends Enum<E>> ConfigValue<E> enumValue(String key, E defaultValue, String description) {
        return value(key, ConfigType.enumType(defaultValue.getDeclaringClass()), defaultValue, description);
    }

    /** Declares a nested section. */
    public final ConfigSection section(String key, String description) {
        return spec().addSection(node(), key, ConfigSpec.description(description));
    }

}
