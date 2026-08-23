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
        return sectionEntry(key).description(description).build();
    }

    /** Starts a fluent declaration for a nested section. */
    public final SectionBuilder sectionEntry(String key) {
        return new SectionBuilder(spec(), node(), key);
    }

    /** Declares a presentation-only group in this section. */
    public final ConfigGroup group(String id) {
        return spec().addGroup(node(), id);
    }

    /** Starts a declaration for a non-persistent information row. */
    public final ConfigInfoBuilder info(String id) {
        return new ConfigInfoBuilder(spec(), node(), id);
    }

    /** Starts a fluent boolean declaration. */
    public final BooleanEntryBuilder booleanEntry(String key, boolean defaultValue) {
        return new BooleanEntryBuilder(spec(), node(), key, defaultValue);
    }

    /** Starts a fluent integer declaration. */
    public final IntEntryBuilder intEntry(String key, int defaultValue) {
        return new IntEntryBuilder(spec(), node(), key, defaultValue);
    }

    /** Starts a fluent long declaration. */
    public final LongEntryBuilder longEntry(String key, long defaultValue) {
        return new LongEntryBuilder(spec(), node(), key, defaultValue);
    }

    /** Starts a fluent double declaration. */
    public final DoubleEntryBuilder doubleEntry(String key, double defaultValue) {
        return new DoubleEntryBuilder(spec(), node(), key, defaultValue);
    }

    /** Starts a fluent string declaration. */
    public final StringEntryBuilder stringEntry(String key, String defaultValue) {
        return new StringEntryBuilder(spec(), node(), key, defaultValue);
    }

    /** Starts a fluent string-list declaration. */
    public final StringListEntryBuilder stringListEntry(String key, List<String> defaultValue) {
        return new StringListEntryBuilder(spec(), node(), key, defaultValue);
    }

    /** Starts a fluent enum declaration. */
    public final <E extends Enum<E>> EnumEntryBuilder<E> enumEntry(
            String key, Class<E> enumType, E defaultValue) {
        return new EnumEntryBuilder<>(spec(), node(), key, enumType, defaultValue);
    }

    /** Declares a reloadable boolean. */
    public final BooleanConfigValue booleanValue(String key, boolean defaultValue, String description) {
        return booleanValue(key, defaultValue, description, RestartRequirement.NONE);
    }

    /** Declares a boolean with restart metadata. */
    public final BooleanConfigValue booleanValue(String key, boolean defaultValue, String description,
                                                  RestartRequirement restartRequirement) {
        return booleanEntry(key, defaultValue).description(description)
                .restartRequirement(restartRequirement).build();
    }

    /** Declares a reloadable bounded integer. */
    public final IntConfigValue intValue(String key, int defaultValue, int minimum, int maximum,
                                         String description) {
        return intValue(key, defaultValue, minimum, maximum, description, RestartRequirement.NONE);
    }

    /** Declares a bounded integer with restart metadata. */
    public final IntConfigValue intValue(String key, int defaultValue, int minimum, int maximum,
                                         String description, RestartRequirement restartRequirement) {
        return intEntry(key, defaultValue).range(minimum, maximum).description(description)
                .restartRequirement(restartRequirement).build();
    }

    /** Declares a reloadable bounded long. */
    public final LongConfigValue longValue(String key, long defaultValue, long minimum, long maximum,
                                           String description) {
        return longValue(key, defaultValue, minimum, maximum, description, RestartRequirement.NONE);
    }

    /** Declares a bounded long with restart metadata. */
    public final LongConfigValue longValue(String key, long defaultValue, long minimum, long maximum,
                                           String description, RestartRequirement restartRequirement) {
        return longEntry(key, defaultValue).range(minimum, maximum).description(description)
                .restartRequirement(restartRequirement).build();
    }

    /** Declares a reloadable bounded double. */
    public final DoubleConfigValue doubleValue(String key, double defaultValue, double minimum, double maximum,
                                               String description) {
        return doubleValue(key, defaultValue, minimum, maximum, description, RestartRequirement.NONE);
    }

    /** Declares a bounded double with restart metadata. */
    public final DoubleConfigValue doubleValue(String key, double defaultValue, double minimum, double maximum,
                                               String description, RestartRequirement restartRequirement) {
        return doubleEntry(key, defaultValue).range(minimum, maximum).description(description)
                .restartRequirement(restartRequirement).build();
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
        return stringEntry(key, defaultValue).description(description)
                .restartRequirement(restartRequirement).validator(validator, validationMessage).build();
    }

    /** Declares a reloadable immutable string list. */
    public final StringListConfigValue stringListValue(String key, List<String> defaultValue, String description) {
        return stringListValue(key, defaultValue, description, RestartRequirement.NONE);
    }

    /** Declares an immutable string list with restart metadata. */
    public final StringListConfigValue stringListValue(String key, List<String> defaultValue, String description,
                                                       RestartRequirement restartRequirement) {
        return stringListEntry(key, defaultValue).description(description)
                .restartRequirement(restartRequirement).build();
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
        return enumEntry(key, enumType, defaultValue).description(description)
                .restartRequirement(restartRequirement).build();
    }
}
