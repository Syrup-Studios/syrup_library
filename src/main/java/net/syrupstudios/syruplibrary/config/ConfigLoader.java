package net.syrupstudios.syruplibrary.config;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Primitive;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssue;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssueSeverity;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigLoadResult;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                configuredValues.put(value, parse(value, element, registered.currentState().configured(), issues));
            }
            findUnknown(root, spec.root(), "", issues);
            if (issues.stream().anyMatch(issue -> issue.severity() == ConfigIssueSeverity.ERROR)) {
                return new ConfigLoadResult(false, issues, null);
            }
            DefaultJson5Writer.fillMissing(spec, registered.path(), root);

            ConfigSnapshot configured = new ConfigSnapshot(configuredValues);
            if (initial) {
                registered.publish(new ConfigState(configured, configured, configured));
            } else {
                registered.publish(registered.stateFor(configured, issues));
            }
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

    private static <T> T parse(ConfigValue<T> value, Json5Element element,
                               ConfigSnapshot previous, List<ConfigIssue> issues) {
        if (element == null) {
            issues.add(new ConfigIssue(value.path(), ConfigIssueSeverity.INFORMATION,
                    "Value is missing; using the schema default", null, value.defaultValue()));
            return value.defaultValue();
        }
        try {
            return value.validate(value.type().decode(element));
        } catch (RuntimeException exception) {
            T retained = previous.get(value);
            issues.add(new ConfigIssue(value.path(), ConfigIssueSeverity.ERROR,
                    usefulMessage(exception) + "; configuration rejected; previous values remain configured",
                    toJava(element), retained));
            return retained;
        }
    }

    private static void findUnknown(Json5Object object, SchemaNode schema, String parentPath,
                                    List<ConfigIssue> issues) {
        for (Map.Entry<String, Json5Element> entry : object.entrySet()) {
            String path = parentPath.isEmpty() ? entry.getKey() : parentPath + "." + entry.getKey();
            SchemaNode known = schema.children.get(entry.getKey());
            if (known == null) {
                issues.add(new ConfigIssue(path, ConfigIssueSeverity.INFORMATION,
                        "Unknown value was ignored", toJava(entry.getValue()), null));
            } else if (known.value == null) {
                if (entry.getValue().isJson5Object()) {
                    findUnknown(entry.getValue().getAsJson5Object(), known, path, issues);
                } else {
                    issues.add(new ConfigIssue(path, ConfigIssueSeverity.ERROR,
                            "Expected an object for the section", toJava(entry.getValue()), null));
                }
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

    static String usefulMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
