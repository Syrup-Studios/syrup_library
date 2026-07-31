package net.syrupstudios.syruplibrary.config;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Primitive;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssue;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssueSeverity;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigLoadResult;
import net.syrupstudios.syruplibrary.config.value.BooleanConfigValue;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;
import net.syrupstudios.syruplibrary.config.value.DoubleConfigValue;
import net.syrupstudios.syruplibrary.config.value.EnumConfigValue;
import net.syrupstudios.syruplibrary.config.value.IntConfigValue;
import net.syrupstudios.syruplibrary.config.value.LongConfigValue;
import net.syrupstudios.syruplibrary.config.value.StringConfigValue;
import net.syrupstudios.syruplibrary.config.value.StringListConfigValue;

import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ConfigLoader {
    private static final Json5 JSON5 = new Json5();

    private ConfigLoader() {
    }

    static ConfigLoadResult load(RegisteredConfig registered, boolean initial) {
        ConfigSpec spec = registered.spec();
        List<ConfigIssue> issues = new ArrayList<>();
        try {
            DefaultJson5Writer.createIfMissing(spec, registered.path());
            Json5Element document;
            try (Reader reader = Files.newBufferedReader(registered.path(), StandardCharsets.UTF_8)) {
                document = JSON5.parse(reader);
            }
            if (document == null || !document.isJson5Object()) {
                throw new IllegalArgumentException("The JSON5 document root must be an object");
            }

            Json5Object root = document.getAsJson5Object();
            Map<ConfigValue<?>, Object> configuredValues = new LinkedHashMap<>();
            for (ConfigValue<?> value : spec.values()) {
                Json5Element element = find(root, value.path());
                configuredValues.put(value, parse(value, element, issues));
            }
            findUnknown(root, spec.root(), "", issues);
            DefaultJson5Writer.fillMissing(spec, registered.path(), root);

            ConfigSnapshot configured = new ConfigSnapshot(configuredValues);
            ConfigSnapshot startup;
            ConfigSnapshot effective;
            if (initial) {
                startup = configured;
                effective = configured;
            } else {
                startup = spec.currentState().startup();
                Map<ConfigValue<?>, Object> effectiveValues = new LinkedHashMap<>();
                for (ConfigValue<?> value : spec.values()) {
                    Object parsed = configuredValues.get(value);
                    if (value.restartRequirement() == RestartRequirement.REQUIRED) {
                        Object running = startup.values().get(value);
                        effectiveValues.put(value, running);
                        if (!Objects.equals(parsed, running)) {
                            issues.add(new ConfigIssue(
                                    value.path(),
                                    ConfigIssueSeverity.INFORMATION,
                                    "Configured value requires a restart; the startup value remains effective",
                                    parsed,
                                    running
                            ));
                        }
                    } else {
                        effectiveValues.put(value, parsed);
                    }
                }
                effective = new ConfigSnapshot(effectiveValues);
            }
            spec.publish(new ConfigState(configured, effective, startup));
            return new ConfigLoadResult(true, issues, null);
        } catch (Exception exception) {
            issues.add(new ConfigIssue(
                    registered.path().toString(),
                    ConfigIssueSeverity.ERROR,
                    "Could not load JSON5 document; the previous valid configuration remains active: "
                            + usefulMessage(exception),
                    null,
                    null
            ));
            return new ConfigLoadResult(false, issues, exception);
        }
    }

    private static Json5Element find(Json5Object root, String path) {
        Json5Element current = root;
        for (String segment : path.split("\\.")) {
            if (!current.isJson5Object()) {
                return current;
            }
            Json5Object object = current.getAsJson5Object();
            if (!object.has(segment)) {
                return null;
            }
            current = object.get(segment);
        }
        return current;
    }

    private static Object parse(ConfigValue<?> value, Json5Element element, List<ConfigIssue> issues) {
        if (element == null) {
            issues.add(new ConfigIssue(value.path(), ConfigIssueSeverity.INFORMATION,
                    "Value is missing; using the schema default", null, value.defaultValue()));
            return value.defaultValue();
        }
        try {
            if (value instanceof BooleanConfigValue) {
                Json5Primitive primitive = primitive(element, value);
                if (!primitive.isBoolean()) {
                    return wrongType(value, element, "boolean", issues);
                }
                return primitive.getAsBoolean();
            }
            if (value instanceof IntConfigValue integer) {
                BigDecimal number = number(element, value, issues);
                if (number == null) return value.defaultValue();
                int parsed = number.intValueExact();
                int clamped = Math.max(integer.minimum(), Math.min(integer.maximum(), parsed));
                return ranged(value, parsed, clamped, issues);
            }
            if (value instanceof LongConfigValue longValue) {
                BigDecimal number = number(element, value, issues);
                if (number == null) return value.defaultValue();
                long parsed = number.longValueExact();
                long clamped = Math.max(longValue.minimum(), Math.min(longValue.maximum(), parsed));
                return ranged(value, parsed, clamped, issues);
            }
            if (value instanceof DoubleConfigValue doubleValue) {
                BigDecimal number = number(element, value, issues);
                if (number == null) return value.defaultValue();
                double parsed = number.doubleValue();
                if (!Double.isFinite(parsed)) {
                    return invalid(value, parsed, "Number must be finite", issues);
                }
                double clamped = Math.max(doubleValue.minimum(), Math.min(doubleValue.maximum(), parsed));
                return ranged(value, parsed, clamped, issues);
            }
            if (value instanceof StringConfigValue stringValue) {
                Json5Primitive primitive = primitive(element, value);
                if (!primitive.isString()) {
                    return wrongType(value, element, "string", issues);
                }
                String parsed = primitive.getAsString();
                boolean valid;
                try {
                    valid = stringValue.isValid(parsed);
                } catch (RuntimeException exception) {
                    valid = false;
                }
                return valid ? parsed : invalid(value, parsed, stringValue.validationMessage(), issues);
            }
            if (value instanceof StringListConfigValue) {
                if (!element.isJson5Array()) {
                    return wrongType(value, element, "array of strings", issues);
                }
                List<String> strings = new ArrayList<>();
                for (Json5Element item : element.getAsJson5Array()) {
                    if (!item.isJson5Primitive() || !item.getAsJson5Primitive().isString()) {
                        return wrongType(value, element, "array containing only strings", issues);
                    }
                    strings.add(item.getAsString());
                }
                return List.copyOf(strings);
            }
            if (value instanceof EnumConfigValue<?> enumValue) {
                Json5Primitive primitive = primitive(element, value);
                if (!primitive.isString()) {
                    return wrongType(value, element, "lowercase enum name", issues);
                }
                Enum<?> parsed = enumValue.parse(primitive.getAsString());
                return parsed != null
                        ? parsed
                        : invalid(value, primitive.getAsString(), "Unknown enum name", issues);
            }
            throw new IllegalStateException("Unsupported config value class " + value.getClass().getName());
        } catch (ArithmeticException exception) {
            return invalid(value, toJava(element), "Number is not an exact " + value.declaredType().getSimpleName(), issues);
        } catch (IllegalStateException exception) {
            return wrongType(value, element, value.declaredType().getSimpleName(), issues);
        }
    }

    private static Json5Primitive primitive(Json5Element element, ConfigValue<?> value) {
        if (!element.isJson5Primitive()) {
            throw new IllegalStateException("Expected primitive for " + value.path());
        }
        return element.getAsJson5Primitive();
    }

    private static BigDecimal number(Json5Element element, ConfigValue<?> value, List<ConfigIssue> issues) {
        if (!element.isJson5Primitive() || !element.getAsJson5Primitive().isNumber()) {
            wrongType(value, element, "number", issues);
            return null;
        }
        return element.getAsBigDecimal();
    }

    private static Object ranged(ConfigValue<?> value, Object parsed, Object clamped, List<ConfigIssue> issues) {
        if (!Objects.equals(parsed, clamped)) {
            issues.add(new ConfigIssue(value.path(), ConfigIssueSeverity.WARNING,
                    "Number is outside the declared range and was clamped", parsed, clamped));
        }
        return clamped;
    }

    private static Object wrongType(ConfigValue<?> value, Json5Element element, String expected,
                                    List<ConfigIssue> issues) {
        return invalid(value, toJava(element), "Expected " + expected, issues);
    }

    private static Object invalid(ConfigValue<?> value, Object original, String message,
                                  List<ConfigIssue> issues) {
        Object fallback = value.defaultValue();
        issues.add(new ConfigIssue(value.path(), ConfigIssueSeverity.WARNING,
                message + "; using the schema default", original, fallback));
        return fallback;
    }

    private static void findUnknown(Json5Object object, SchemaNode schema, String parentPath,
                                    List<ConfigIssue> issues) {
        for (Map.Entry<String, Json5Element> entry : object.entrySet()) {
            String path = parentPath.isEmpty() ? entry.getKey() : parentPath + "." + entry.getKey();
            SchemaNode known = schema.children.get(entry.getKey());
            if (known == null) {
                issues.add(new ConfigIssue(path, ConfigIssueSeverity.INFORMATION,
                        "Unknown value was ignored", toJava(entry.getValue()), null));
            } else if (known.value == null && entry.getValue().isJson5Object()) {
                findUnknown(entry.getValue().getAsJson5Object(), known, path, issues);
            }
        }
    }

    private static Object toJava(Json5Element element) {
        if (element == null || element.isJson5Null()) return null;
        if (element.isJson5Primitive()) {
            Json5Primitive primitive = element.getAsJson5Primitive();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isNumber()) return primitive.getAsNumber();
            return primitive.getAsString();
        }
        if (element.isJson5Array()) {
            List<Object> values = new ArrayList<>();
            for (Json5Element item : element.getAsJson5Array()) values.add(toJava(item));
            return Collections.unmodifiableList(values);
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, Json5Element> entry : element.getAsJson5Object().entrySet()) {
            values.put(entry.getKey(), toJava(entry.getValue()));
        }
        return Collections.unmodifiableMap(values);
    }

    private static String usefulMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
