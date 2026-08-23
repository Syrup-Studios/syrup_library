package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.IntConfigValue;

/** Builder for a bounded integer config value. */
public final class IntEntryBuilder extends ConfigEntryBuilder<Integer, IntConfigValue, IntEntryBuilder> {
    private final int defaultValue;
    private int minimum = Integer.MIN_VALUE;
    private int maximum = Integer.MAX_VALUE;

    IntEntryBuilder(ConfigSpec spec, ConfigSchemaNode parent, String key, int defaultValue) {
        super(spec, parent, key);
        this.defaultValue = defaultValue;
    }

    public IntEntryBuilder range(int minimum, int maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
        return this;
    }

    @Override protected IntEntryBuilder self() { return this; }

    @Override protected IntConfigValue createValue(String path) {
        return new IntConfigValue(spec, key, path, defaultValue, minimum, maximum, description, restartRequirement);
    }
}
