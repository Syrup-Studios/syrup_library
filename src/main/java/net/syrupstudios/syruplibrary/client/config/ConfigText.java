package net.syrupstudios.syruplibrary.client.config;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import net.syrupstudios.syruplibrary.config.ConfigSchemaNode;
import net.syrupstudios.syruplibrary.config.ConfigGroup;
import net.syrupstudios.syruplibrary.config.ConfigInfoRow;
import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RegisteredConfig;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;
import net.syrupstudios.syruplibrary.config.value.DoubleConfigValue;
import net.syrupstudios.syruplibrary.config.value.IntConfigValue;
import net.syrupstudios.syruplibrary.config.value.LongConfigValue;
import net.syrupstudios.syruplibrary.config.value.StringListConfigValue;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;

import java.util.ArrayList;
import java.util.List;

/** Shared text helpers for the config editor. */
final class ConfigText {
    private static final Json5 JSON5 = new Json5();

    private ConfigText() {
    }

    /** Returns a readable name for a config value. */
    static Component configTitle(RegisteredConfig config) {
        ConfigSpec spec = config.spec();
        String automatic = spec.translationPrefix().map(prefix -> prefix + ".title").orElse(null);
        return translatedOrLiteral(automatic, displayName(spec.id()));
    }

    static Component valueName(ConfigSchemaNode node) {
        ConfigValue<?> value = node.value();
        String explicit = node.presentation().labelTranslationKey().orElse(null);
        String automatic = translationKey(value.specReference(), value.path(), "name");
        return translatedOrLiteral(firstExisting(explicit, automatic),
                displayName(lastSegment(value.path())));
    }

    /** Returns a readable name for a config section. */
    static Component sectionName(ConfigSchemaNode section) {
        String explicit = section.presentation().labelTranslationKey().orElse(null);
        ConfigSpec spec = section.spec();
        String automatic = translationKey(spec, section.path(), "name");
        return translatedOrLiteral(firstExisting(explicit, automatic),
                displayName(lastSegment(section.path())));
    }

    static Component description(ConfigSchemaNode node, ConfigSpec spec) {
        String explicit = node.presentation().descriptionTranslationKey().orElse(null);
        String automatic = translationKey(spec, node.path(), "description");
        String fallback = String.join("\n", node.description());
        return translatedOrLiteral(firstExisting(explicit, automatic), fallback);
    }

    static Component groupName(ConfigSpec spec, ConfigGroup group) {
        String explicit = group.labelTranslationKey().orElse(null);
        String automatic = spec.translationPrefix()
                .map(prefix -> prefix + ".group." + group.id() + ".name").orElse(null);
        return translatedOrLiteral(firstExisting(explicit, automatic), displayName(group.id()));
    }

    static Component groupDescription(ConfigSpec spec, ConfigGroup group) {
        String explicit = group.descriptionTranslationKey().orElse(null);
        String automatic = spec.translationPrefix()
                .map(prefix -> prefix + ".group." + group.id() + ".description").orElse(null);
        return translatedOrLiteral(firstExisting(explicit, automatic), "");
    }

    static Component infoText(ConfigSpec spec, ConfigInfoRow info) {
        String explicit = info.presentation().labelTranslationKey().orElse(null);
        String automatic = spec.translationPrefix()
                .map(prefix -> prefix + ".info." + info.id() + ".text").orElse(null);
        return translatedOrLiteral(firstExisting(explicit, automatic), info.text());
    }

    static Component enumChoice(ConfigSchemaNode node, Enum<?> choice) {
        ConfigValue<?> value = node.value();
        String serialized = choice.name().toLowerCase(java.util.Locale.ROOT);
        String explicit = node.presentation().enumPresentation().translationKeys().get(serialized);
        String automatic = value.specReference().translationPrefix()
                .map(prefix -> prefix + "." + value.path() + ".option." + serialized).orElse(null);
        return translatedOrLiteral(firstExisting(explicit, automatic), displayName(serialized));
    }

    private static String translationKey(ConfigSpec spec, String path, String suffix) {
        return spec.translationPrefix().map(prefix -> prefix + (path.isEmpty() ? "" : "." + path)
                + "." + suffix).orElse(null);
    }

    private static String firstExisting(String first, String second) {
        if (has(first)) return first;
        return has(second) ? second : null;
    }

    private static Component translatedOrLiteral(String key, String fallback) {
        return key != null && has(key) ? Component.translatable(key) : Component.literal(fallback);
    }

    private static boolean has(String key) {
        return key != null && Language.getInstance().has(key);
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
