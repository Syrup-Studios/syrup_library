package net.syrupstudios.syruplibrary.config.value;

import de.marhali.json5.Json5Primitive;
import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import java.util.List;

/** @deprecated Use the generic enumValue declaration method. */
@Deprecated
public final class EnumConfigValue<E extends Enum<E>> extends ConfigValue<E> {
    public EnumConfigValue(ConfigSpec spec, String key, String path, Class<E> enumType, E defaultValue,
                           List<String> description, RestartRequirement restartRequirement) {
        super(spec, key, path, ConfigType.enumType(enumType), defaultValue, description,
                restartRequirement, List.of());
    }

    public Class<E> enumType() { return defaultValue().getDeclaringClass(); }
    public String serialize(E value) { return type().encode(value).getAsString(); }
    public E parse(String value) {
        try { return type().decode(Json5Primitive.fromString(value)); }
        catch (IllegalArgumentException exception) { return null; }
    }
}
