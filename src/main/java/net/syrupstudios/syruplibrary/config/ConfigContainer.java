package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.BooleanConfigValue;
import net.syrupstudios.syruplibrary.config.value.DoubleConfigValue;
import net.syrupstudios.syruplibrary.config.value.EnumConfigValue;
import net.syrupstudios.syruplibrary.config.value.IntConfigValue;
import net.syrupstudios.syruplibrary.config.value.LongConfigValue;
import net.syrupstudios.syruplibrary.config.value.StringConfigValue;
import net.syrupstudios.syruplibrary.config.value.StringListConfigValue;

import java.util.List;
import java.util.function.Predicate;

/** Common declaration API shared by a root spec and nested section. */
public abstract class ConfigContainer {
    abstract ConfigSpec spec();

    abstract ConfigSchemaNode node();

    /** Declares a nested section. */
    public final ConfigSection section(String key, String description) {
        return spec().addSection(node(), key, ConfigSpec.description(description));
    }

    /** Declares a reloadable boolean. */
    public final BooleanConfigValue booleanValue(String key, boolean defaultValue, String description) {
        return booleanValue(key, defaultValue, description, RestartRequirement.NONE);
    }

    /** Declares a boolean with restart metadata. */
    public final BooleanConfigValue booleanValue(String key, boolean defaultValue, String description,
                                                  RestartRequirement restartRequirement) {
        String path = spec().childPath(node(), key);
        return spec().addValue(node(), key, new BooleanConfigValue(
                spec(), key, path, defaultValue, ConfigSpec.description(description), restartRequirement));
    }

    /** Declares a reloadable bounded integer. */
    public final IntConfigValue intValue(String key, int defaultValue, int minimum, int maximum,
                                         String description) {
        return intValue(key, defaultValue, minimum, maximum, description, RestartRequirement.NONE);
    }

    /** Declares a bounded integer with restart metadata. */
    public final IntConfigValue intValue(String key, int defaultValue, int minimum, int maximum,
                                         String description, RestartRequirement restartRequirement) {
        String path = spec().childPath(node(), key);
        return spec().addValue(node(), key, new IntConfigValue(
                spec(), key, path, defaultValue, minimum, maximum,
                ConfigSpec.description(description), restartRequirement));
    }

    /** Declares a reloadable bounded long. */
    public final LongConfigValue longValue(String key, long defaultValue, long minimum, long maximum,
                                           String description) {
        return longValue(key, defaultValue, minimum, maximum, description, RestartRequirement.NONE);
    }

    /** Declares a bounded long with restart metadata. */
    public final LongConfigValue longValue(String key, long defaultValue, long minimum, long maximum,
                                           String description, RestartRequirement restartRequirement) {
        String path = spec().childPath(node(), key);
        return spec().addValue(node(), key, new LongConfigValue(
                spec(), key, path, defaultValue, minimum, maximum,
                ConfigSpec.description(description), restartRequirement));
    }

    /** Declares a reloadable bounded double. */
    public final DoubleConfigValue doubleValue(String key, double defaultValue, double minimum, double maximum,
                                               String description) {
        return doubleValue(key, defaultValue, minimum, maximum, description, RestartRequirement.NONE);
    }

    /** Declares a bounded double with restart metadata. */
    public final DoubleConfigValue doubleValue(String key, double defaultValue, double minimum, double maximum,
                                               String description, RestartRequirement restartRequirement) {
        String path = spec().childPath(node(), key);
        return spec().addValue(node(), key, new DoubleConfigValue(
                spec(), key, path, defaultValue, minimum, maximum,
                ConfigSpec.description(description), restartRequirement));
    }

    /** Declares a reloadable string. */
    public final StringConfigValue stringValue(String key, String defaultValue, String description) {
        return stringValue(key, defaultValue, description, RestartRequirement.NONE);
    }

    /** Declares a string with restart metadata. */
    public final StringConfigValue stringValue(String key, String defaultValue, String description,
                                               RestartRequirement restartRequirement) {
        return validatedStringValue(key, defaultValue, description, restartRequirement, ignored -> true,
                "Value failed validation");
    }

    /** Declares a validated reloadable string. Invalid input falls back to the default. */
    public final StringConfigValue validatedStringValue(String key, String defaultValue, String description,
                                                        Predicate<String> validator, String validationMessage) {
        return validatedStringValue(key, defaultValue, description, RestartRequirement.NONE,
                validator, validationMessage);
    }

    /** Declares a validated string with restart metadata. */
    public final StringConfigValue validatedStringValue(
            String key,
            String defaultValue,
            String description,
            RestartRequirement restartRequirement,
            Predicate<String> validator,
            String validationMessage
    ) {
        String path = spec().childPath(node(), key);
        return spec().addValue(node(), key, new StringConfigValue(
                spec(), key, path, defaultValue, ConfigSpec.description(description),
                restartRequirement, validator, validationMessage));
    }

    /** Declares a reloadable immutable string list. */
    public final StringListConfigValue stringListValue(String key, List<String> defaultValue, String description) {
        return stringListValue(key, defaultValue, description, RestartRequirement.NONE);
    }

    /** Declares an immutable string list with restart metadata. */
    public final StringListConfigValue stringListValue(String key, List<String> defaultValue, String description,
                                                       RestartRequirement restartRequirement) {
        String path = spec().childPath(node(), key);
        return spec().addValue(node(), key, new StringListConfigValue(
                spec(), key, path, defaultValue, ConfigSpec.description(description), restartRequirement));
    }

    /** Declares a reloadable enum serialized by lowercase constant name. */
    public final <E extends Enum<E>> EnumConfigValue<E> enumValue(
            String key, Class<E> enumType, E defaultValue, String description) {
        return enumValue(key, enumType, defaultValue, description, RestartRequirement.NONE);
    }

    /** Declares an enum with restart metadata. */
    public final <E extends Enum<E>> EnumConfigValue<E> enumValue(
            String key,
            Class<E> enumType,
            E defaultValue,
            String description,
            RestartRequirement restartRequirement
    ) {
        String path = spec().childPath(node(), key);
        return spec().addValue(node(), key, new EnumConfigValue<>(
                spec(), key, path, enumType, defaultValue, ConfigSpec.description(description), restartRequirement));
    }
}
