package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.DoubleConfigValue;

/** Builder for a bounded double config value. */
public final class DoubleEntryBuilder extends ConfigEntryBuilder<Double, DoubleConfigValue, DoubleEntryBuilder> {
    private final double defaultValue;
    private double minimum = -Double.MAX_VALUE;
    private double maximum = Double.MAX_VALUE;

    DoubleEntryBuilder(ConfigSpec spec, ConfigSchemaNode parent, String key, double defaultValue) {
        super(spec, parent, key);
        this.defaultValue = defaultValue;
    }

    public DoubleEntryBuilder range(double minimum, double maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
        return this;
    }

    @Override protected DoubleEntryBuilder self() { return this; }

    @Override protected DoubleConfigValue createValue(String path) {
        return new DoubleConfigValue(spec, key, path, defaultValue, minimum, maximum, description, restartRequirement);
    }
}
