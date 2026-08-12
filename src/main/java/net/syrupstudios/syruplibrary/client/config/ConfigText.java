package net.syrupstudios.syruplibrary.client.config;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import net.syrupstudios.syruplibrary.config.ConfigSchemaNode;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;
import net.syrupstudios.syruplibrary.config.value.DoubleConfigValue;
import net.syrupstudios.syruplibrary.config.value.IntConfigValue;
import net.syrupstudios.syruplibrary.config.value.LongConfigValue;
import net.syrupstudios.syruplibrary.config.value.StringListConfigValue;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Shared text helpers for the config editor. */
final class ConfigText {
    private static final Json5 JSON5 = new Json5();

    private ConfigText() {
    }

    /** Returns a readable name for a config value. */
    static Component valueName(ConfigValue<?> value) {
        return Component.literal(displayName(lastSegment(value.path())));
    }

    /** Returns a readable name for a config section. */
    static Component sectionName(ConfigSchemaNode section) {
        return Component.literal(displayName(lastSegment(section.path())));
    }

    /** Converts a snake-case key to a readable display name, e.g. {@code render_distance -> Render Distance}. */
    static String displayName(String key) {
        StringBuilder result = new StringBuilder();
        boolean capitalize = true;
        for (int index = 0; index < key.length(); index++) {
            char character = key.charAt(index);
            if (character == '_' || character == '-') {
                capitalize = true;
                if (result.length() > 0) {
                    result.append(' ');
                }
                continue;
            }
            if (capitalize && Character.isLetter(character)) {
                result.append(Character.toUpperCase(character));
            } else {
                result.append(character);
            }
            capitalize = false;
        }
        return result.length() == 0 ? key : result.toString();
    }

    /** Returns the last dotted path segment of a config path. */
    private static String lastSegment(String path) {
        int index = path.lastIndexOf('.');
        return index >= 0 ? path.substring(index + 1) : path;
    }

    /** Serializes a draft value to text for a text field. */
    static String toText(Object draft) {
        if (draft instanceof List<?> list) {
            Json5Array array = new Json5Array();
            list.forEach(item -> array.add((String) item));
            try {
                return JSON5.serialize(array);
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("Could not display a string list", exception);
            }
        }
        return String.valueOf(draft);
    }

    /** Parses text from a text field back into a typed candidate. */
    static Object parseText(ConfigValue<?> value, String text) {
        if (value instanceof IntConfigValue) {
            try {
                return Integer.valueOf(text.trim());
            } catch (NumberFormatException exception) {
                return text;
            }
        }
        if (value instanceof LongConfigValue) {
            try {
                return Long.valueOf(text.trim());
            } catch (NumberFormatException exception) {
                return text;
            }
        }
        if (value instanceof DoubleConfigValue) {
            try {
                return Double.valueOf(text.trim());
            } catch (NumberFormatException exception) {
                return text;
            }
        }
        if (value instanceof StringListConfigValue) {
            try {
                Json5Element parsed = JSON5.parse(text);
                if (!parsed.isJson5Array()) return text;
                List<String> result = new ArrayList<>();
                for (Json5Element item : parsed.getAsJson5Array()) {
                    if (!item.isJson5Primitive() || !item.getAsJson5Primitive().isString()) return text;
                    result.add(item.getAsString());
                }
                return List.copyOf(result);
            } catch (RuntimeException exception) {
                return text;
            }
        }
        return text;
    }
}
