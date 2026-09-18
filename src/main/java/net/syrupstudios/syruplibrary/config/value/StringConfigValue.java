package net.syrupstudios.syruplibrary.config.value;

import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import java.util.List;
import java.util.function.Predicate;

/** @deprecated Use the generic ConfigValue declaration methods. */
@Deprecated
public final class StringConfigValue extends ConfigValue<String> {
    public StringConfigValue(ConfigSpec spec, String key, String path, String defaultValue,
                             List<String> description, RestartRequirement restartRequirement,
                             Predicate<String> validator, String validationMessage) {
        super(spec, key, path, ConfigType.STRING, defaultValue, description, restartRequirement,
                List.of(ConfigConstraint.predicate(validator, validationMessage)));
    }

    public boolean isValid(String value) { return constraints().get(0).validate(value) == null; }
    public String validationMessage() { return constraints().get(0).description(); }
}
