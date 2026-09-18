package net.syrupstudios.syruplibrary.config.value;

import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import java.util.List;

/** @deprecated Use the generic ConfigValue declaration methods. */
@Deprecated
public final class LongConfigValue extends ConfigValue<Long> {
    public LongConfigValue(ConfigSpec spec, String key, String path, long defaultValue,
                            long minimum, long maximum, List<String> description,
                            RestartRequirement restartRequirement) {
        super(spec, key, path, ConfigType.LONG, defaultValue, description, restartRequirement,
                List.of(ConfigConstraint.range(minimum, maximum)));
    }

    public long minimum() { return range().minimum(); }
    public long maximum() { return range().maximum(); }
    private ConfigConstraint.Range<Long> range() {
        return (ConfigConstraint.Range<Long>) constraints().get(0);
    }
}
