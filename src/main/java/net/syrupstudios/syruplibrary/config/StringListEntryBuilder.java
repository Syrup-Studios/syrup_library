package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.StringListConfigValue;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Builder for an immutable list-of-strings config value. */
public final class StringListEntryBuilder
        extends ConfigEntryBuilder<List<String>, StringListConfigValue, StringListEntryBuilder> {
    private final List<String> defaultValue;
    private int minimumSize;
    private int maximumSize = Integer.MAX_VALUE;
    private Predicate<String> itemValidator = ignored -> true;
    private String itemValidationMessage = "Invalid list item";

    StringListEntryBuilder(ConfigSpec spec, ConfigSchemaNode parent, String key, List<String> defaultValue) {
        super(spec, parent, key);
        this.defaultValue = List.copyOf(defaultValue);
    }

    public StringListEntryBuilder minimumSize(int minimumSize) {
        this.minimumSize = minimumSize;
        return this;
    }

    public StringListEntryBuilder maximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
        return this;
    }

    public StringListEntryBuilder itemValidator(Predicate<String> validator, String validationMessage) {
        this.itemValidator = Objects.requireNonNull(validator, "validator");
        this.itemValidationMessage = Objects.requireNonNull(validationMessage, "validationMessage");
        return this;
    }

    @Override protected StringListEntryBuilder self() { return this; }

    @Override protected StringListConfigValue createValue(String path) {
        return new StringListConfigValue(spec, key, path, defaultValue, description, restartRequirement,
                minimumSize, maximumSize, itemValidator, itemValidationMessage);
    }
}
