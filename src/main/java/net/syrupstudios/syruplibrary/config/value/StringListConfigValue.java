package net.syrupstudios.syruplibrary.config.value;

import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RestartRequirement;

import java.util.List;

/** Typed immutable list-of-strings configuration value. */
public final class StringListConfigValue extends ConfigValue<List<String>> {
    public StringListConfigValue(ConfigSpec spec, String key, String path, List<String> defaultValue,
                                 List<String> description, RestartRequirement restartRequirement) {
        super(spec, key, path, List.class, defaultValue, description, restartRequirement);
    }

    @Override
    protected List<String> copy(List<String> value) {
        return List.copyOf(value);
    }
}
