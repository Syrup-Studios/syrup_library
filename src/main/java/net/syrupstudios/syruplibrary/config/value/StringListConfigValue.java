package net.syrupstudios.syruplibrary.config.value;

import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import java.util.List;

/** @deprecated Use the generic ConfigValue declaration methods. */
@Deprecated
public final class StringListConfigValue extends ConfigValue<List<String>> {
    public StringListConfigValue(ConfigSpec spec, String key, String path, List<String> defaultValue,
                              List<String> description, RestartRequirement restartRequirement) {
        super(spec, key, path, ConfigType.STRING_LIST, defaultValue, description, restartRequirement, List.of());
    }
}
