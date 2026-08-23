package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.StringConfigValue;

import java.util.Objects;
import java.util.function.Predicate;

/** Builder for a validated string config value. */
public final class StringEntryBuilder extends ConfigEntryBuilder<String, StringConfigValue, StringEntryBuilder> {
    private final String defaultValue;
    private Predicate<String> validator = ignored -> true;
    private String validationMessage = "Value failed validation";

    StringEntryBuilder(ConfigSpec spec, ConfigSchemaNode parent, String key, String defaultValue) {
        super(spec, parent, key);
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
    }

    public StringEntryBuilder validator(Predicate<String> validator, String validationMessage) {
        this.validator = Objects.requireNonNull(validator, "validator");
        this.validationMessage = Objects.requireNonNull(validationMessage, "validationMessage");
        return this;
    }

    @Override protected StringEntryBuilder self() { return this; }

    @Override protected StringConfigValue createValue(String path) {
        return new StringConfigValue(spec, key, path, defaultValue, description, restartRequirement,
                validator, validationMessage);
    }
}
