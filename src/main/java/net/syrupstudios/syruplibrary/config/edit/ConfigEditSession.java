package net.syrupstudios.syruplibrary.config.edit;

import net.syrupstudios.syruplibrary.config.RegisteredConfig;
import net.syrupstudios.syruplibrary.config.ConfigSnapshot;
import net.syrupstudios.syruplibrary.config.ConfigCondition;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssueSeverity;
import net.syrupstudios.syruplibrary.config.value.BooleanConfigValue;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;
import net.syrupstudios.syruplibrary.config.value.DoubleConfigValue;
import net.syrupstudios.syruplibrary.config.value.EnumConfigValue;
import net.syrupstudios.syruplibrary.config.value.IntConfigValue;
import net.syrupstudios.syruplibrary.config.value.LongConfigValue;
import net.syrupstudios.syruplibrary.config.value.StringConfigValue;
import net.syrupstudios.syruplibrary.config.value.StringListConfigValue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Collection;

/**
 * Client-neutral edit session over one registered config.
 *
 * <p>The session starts from the latest configured snapshot so restart-pending values are never
 * overwritten by the editor. Widgets write drafts here; only {@link #save()} touches the file.
 *
 * <p>A draft is stored only while it differs from the captured configured value, so dirty state is
 * value-based: editing a value and then changing it back clears the draft.
 */
public final class ConfigEditSession {
    private final RegisteredConfig config;
    private final ConfigSnapshot configuredSnapshot;
    private final byte[] originalFile;
    private final Map<ConfigValue<?>, Object> drafts = new LinkedHashMap<>();
    private final Map<ConfigValue<?>, Object> normalizations = new LinkedHashMap<>();
    private final Map<ConfigValue<?>, String> errors = new LinkedHashMap<>();

    private ConfigEditSession(RegisteredConfig config, byte[] originalFile) {
        this.config = config;
        this.configuredSnapshot = config.configuredSnapshot();
        this.originalFile = originalFile.clone();
        for (ConfigValue<?> value : config.spec().values()) {
            boolean correctedDuringLoad = config.latestResult().issues().stream()
                    .anyMatch(issue -> issue.severity() == ConfigIssueSeverity.WARNING
                            && issue.path().equals(value.path()));
            if (correctedDuringLoad) {
                normalizations.put(value, configuredSnapshot.get(value));
            }
        }
    }

    /** Opens an edit session and captures the current file for conflict detection. */
    public static ConfigEditSession open(RegisteredConfig config) {
        try {
            return new ConfigEditSession(config, Files.readAllBytes(config.path()));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read config file " + config.path() + " while opening the editor", exception);
        }
    }

    /** Returns the target registered config. */
    public RegisteredConfig config() {
        return config;
    }

    /** Returns the file bytes captured when the session opened. */
    public byte[] originalFile() {
        return originalFile.clone();
    }

    /** Returns the current draft value, falling back to the configured value. */
    public <T> T value(ConfigValue<T> value) {
        Object draft = drafts.get(value);
        if (draft != null) {
            return value.cast(draft);
        }
        return configuredSnapshot.get(value);
    }

    /** Returns whether a presentation condition matches the current draft values. */
    public boolean matches(ConfigCondition condition) {
        return condition.test(this::untypedValue);
    }

    private Object untypedValue(ConfigValue<?> value) {
        Object draft = drafts.get(value);
        return draft != null ? draft : configuredSnapshot.get(value);
    }

    /** Validates and stores a candidate. Returns whether it was accepted. */
    public boolean setValue(ConfigValue<?> value, Object candidate) {
        if (!belongs(value)) {
            return false;
        }
        String error = validationError(value, candidate);
        if (error == null) {
            Object accepted = value.cast(candidate);
            if (Objects.equals(accepted, configuredSnapshot.get(value))) {
                drafts.remove(value);
            } else {
                drafts.put(value, accepted);
            }
            errors.remove(value);
            return true;
        }
        drafts.remove(value);
        errors.put(value, error);
        return false;
    }

    /** Resets a value and explicitly writes its default on Save. */
    public void reset(ConfigValue<?> value) {
        drafts.put(value, value.defaultValue());
        errors.remove(value);
    }

    /** Resets every value and explicitly writes all schema defaults on Save. */
    public void resetAll() {
        for (ConfigValue<?> value : config.spec().values()) {
            reset(value);
        }
    }

    /** Resets only the supplied values, for screen-aware reset actions. */
    public void resetValues(Collection<? extends ConfigValue<?>> values) {
        Objects.requireNonNull(values, "values");
        for (ConfigValue<?> value : values) {
            if (belongs(value)) reset(value);
        }
    }

    /** Returns whether the session has values to write. */
    public boolean isDirty() {
        return !drafts.isEmpty();
    }

    /** Returns whether no current draft is invalid. */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /** Returns the validation error for a value, or {@code null}. */
    public String errorFor(ConfigValue<?> value) {
        return errors.get(value);
    }

    private boolean belongs(ConfigValue<?> value) {
        for (ConfigValue<?> candidate : config.spec().values()) {
            if (candidate == value) {
                return true;
            }
        }
        return false;
    }

    private static String validationError(ConfigValue<?> value, Object candidate) {
        if (value instanceof BooleanConfigValue) return candidate instanceof Boolean ? null : "Expected a boolean";
        if (value instanceof IntConfigValue integer) {
            if (!(candidate instanceof Integer number)) return "Expected an integer";
            return inRange(number, integer.minimum(), integer.maximum());
        }
        if (value instanceof LongConfigValue longValue) {
            if (!(candidate instanceof Long number)) return "Expected a long";
            return inRange(number, longValue.minimum(), longValue.maximum());
        }
        if (value instanceof DoubleConfigValue doubleValue) {
            if (!(candidate instanceof Double number)) return "Expected a number";
            if (!Double.isFinite(number)) return "Number must be finite";
            return inRange(number, doubleValue.minimum(), doubleValue.maximum());
        }
        if (value instanceof StringConfigValue stringValue) {
            if (!(candidate instanceof String string)) return "Expected a string";
            try {
                return stringValue.isValid(string) ? null : stringValue.validationMessage();
            } catch (RuntimeException exception) {
                return stringValue.validationMessage();
            }
        }
        if (value instanceof StringListConfigValue listValue) {
            if (!(candidate instanceof List<?> list)) return "Expected a JSON5 array of strings";
            if (!list.stream().allMatch(String.class::isInstance)) return "List may contain only strings";
            @SuppressWarnings("unchecked") List<String> strings = (List<String>) list;
            if (strings.size() < listValue.minimumSize() || strings.size() > listValue.maximumSize()) {
                return "List size must be between " + listValue.minimumSize() + " and " + listValue.maximumSize();
            }
            for (String item : strings) {
                try {
                    if (!listValue.isItemValid(item)) return listValue.itemValidationMessage();
                } catch (RuntimeException exception) {
                    return listValue.itemValidationMessage();
                }
            }
            return null;
        }
        if (value instanceof EnumConfigValue<?> enumValue) {
            return candidate != null && enumValue.enumType().isInstance(candidate) ? null : "Unknown enum value";
        }
        return "Unsupported config value type";
    }

    private static String inRange(long number, long minimum, long maximum) {
        return number >= minimum && number <= maximum
                ? null : "Value must be between " + minimum + " and " + maximum;
    }

    private static String inRange(double number, double minimum, double maximum) {
        return number >= minimum && number <= maximum
                ? null : "Value must be between " + minimum + " and " + maximum;
    }

    /** Returns an immutable snapshot of the draft values. */
    public Map<ConfigValue<?>, Object> draftValues() {
        if (drafts.isEmpty()) {
            return Map.of();
        }
        Map<ConfigValue<?>, Object> values = new LinkedHashMap<>(normalizations);
        values.putAll(drafts);
        return Map.copyOf(values);
    }

    /** Attempts to save the drafts through the registered config. */
    public ConfigSaveResult save() {
        if (!isValid()) {
            return ConfigSaveResult.failure(false,
                    "One or more values are invalid and were not saved");
        }
        if (!isDirty()) {
            return ConfigSaveResult.ok();
        }
        return config.save(this);
    }
}
