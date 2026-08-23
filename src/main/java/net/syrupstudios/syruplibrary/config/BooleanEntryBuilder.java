package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.BooleanConfigValue;

/** Builder for a boolean config value. */
public final class BooleanEntryBuilder extends ConfigEntryBuilder<Boolean, BooleanConfigValue, BooleanEntryBuilder> {
    private final boolean defaultValue;

    BooleanEntryBuilder(ConfigSpec spec, ConfigSchemaNode parent, String key, boolean defaultValue) {
        super(spec, parent, key);
        this.defaultValue = defaultValue;
    }

    @Override protected BooleanEntryBuilder self() { return this; }

    @Override protected BooleanConfigValue createValue(String path) {
        return new BooleanConfigValue(spec, key, path, defaultValue, description, restartRequirement);
    }
}
