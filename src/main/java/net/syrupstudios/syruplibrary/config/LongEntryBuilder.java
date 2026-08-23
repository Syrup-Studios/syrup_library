package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.LongConfigValue;

/** Builder for a bounded long config value. */
public final class LongEntryBuilder extends ConfigEntryBuilder<Long, LongConfigValue, LongEntryBuilder> {
    private final long defaultValue;
    private long minimum = Long.MIN_VALUE;
    private long maximum = Long.MAX_VALUE;

    LongEntryBuilder(ConfigSpec spec, ConfigSchemaNode parent, String key, long defaultValue) {
        super(spec, parent, key);
        this.defaultValue = defaultValue;
    }

    public LongEntryBuilder range(long minimum, long maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
        return this;
    }

    @Override protected LongEntryBuilder self() { return this; }

    @Override protected LongConfigValue createValue(String path) {
        return new LongConfigValue(spec, key, path, defaultValue, minimum, maximum, description, restartRequirement);
    }
}
