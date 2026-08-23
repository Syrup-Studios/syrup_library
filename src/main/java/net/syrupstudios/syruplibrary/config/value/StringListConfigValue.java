package net.syrupstudios.syruplibrary.config.value;

import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RestartRequirement;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Typed immutable list-of-strings configuration value. */
public final class StringListConfigValue extends ConfigValue<List<String>> {
    private final int minimumSize;
    private final int maximumSize;
    private final Predicate<String> itemValidator;
    private final String itemValidationMessage;

    public StringListConfigValue(ConfigSpec spec, String key, String path, List<String> defaultValue,
                                 List<String> description, RestartRequirement restartRequirement) {
        this(spec, key, path, defaultValue, description, restartRequirement,
                0, Integer.MAX_VALUE, ignored -> true, "Invalid list item");
    }

    public StringListConfigValue(ConfigSpec spec, String key, String path, List<String> defaultValue,
                                 List<String> description, RestartRequirement restartRequirement,
                                 int minimumSize, int maximumSize, Predicate<String> itemValidator,
                                 String itemValidationMessage) {
        super(spec, key, path, List.class, List.copyOf(defaultValue), description, restartRequirement);
        if (minimumSize < 0 || maximumSize < minimumSize) {
            throw new IllegalArgumentException("Invalid list size limits for " + path);
        }
        this.minimumSize = minimumSize;
        this.maximumSize = maximumSize;
        this.itemValidator = Objects.requireNonNull(itemValidator, "itemValidator");
        this.itemValidationMessage = Objects.requireNonNull(itemValidationMessage, "itemValidationMessage");
        if (!isValid(defaultValue)) {
            throw new IllegalArgumentException("Default list fails validation for " + path);
        }
    }

    public int minimumSize() { return minimumSize; }

    public int maximumSize() { return maximumSize; }

    public boolean isItemValid(String item) { return itemValidator.test(item); }

    public String itemValidationMessage() { return itemValidationMessage; }

    public boolean isValid(List<String> value) {
        if (value.size() < minimumSize || value.size() > maximumSize) return false;
        try {
            return value.stream().allMatch(itemValidator);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    protected List<String> copy(List<String> value) {
        return List.copyOf(value);
    }
}
