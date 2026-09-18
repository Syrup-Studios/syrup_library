package net.syrupstudios.syruplibrary.config.value;

import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import java.util.List;

/** @deprecated Use the generic ConfigValue declaration methods. */
@Deprecated
public final class IntConfigValue extends ConfigValue<Integer> {
    public IntConfigValue(ConfigSpec spec, String key, String path, int defaultValue,
                            int minimum, int maximum, List<String> description,
                            RestartRequirement restartRequirement) {
        super(spec, key, path, ConfigType.INTEGER, defaultValue, description, restartRequirement,
                List.of(ConfigConstraint.range(minimum, maximum)));
    }

    public int minimum() { return range().minimum(); }
    public int maximum() { return range().maximum(); }
    private ConfigConstraint.Range<Integer> range() {
        return (ConfigConstraint.Range<Integer>) constraints().get(0);
    }
}
