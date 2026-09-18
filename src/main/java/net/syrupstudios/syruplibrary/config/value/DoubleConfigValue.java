package net.syrupstudios.syruplibrary.config.value;

import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import java.util.List;

/** @deprecated Use the generic ConfigValue declaration methods. */
@Deprecated
public final class DoubleConfigValue extends ConfigValue<Double> {
    public DoubleConfigValue(ConfigSpec spec, String key, String path, double defaultValue,
                            double minimum, double maximum, List<String> description,
                            RestartRequirement restartRequirement) {
        super(spec, key, path, ConfigType.DOUBLE, defaultValue, description, restartRequirement,
                List.of(ConfigConstraint.range(minimum, maximum)));
    }

    public double minimum() { return range().minimum(); }
    public double maximum() { return range().maximum(); }
    private ConfigConstraint.Range<Double> range() {
        return (ConfigConstraint.Range<Double>) constraints().get(0);
    }
}
