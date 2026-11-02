package net.syrupstudios.syruplibrary.config;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Element;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class DefaultJson5Writer {
    private static final Json5 JSON5 = new Json5();

    private DefaultJson5Writer() {
    }

    static void createIfMissing(ConfigSpec spec, Path path) throws IOException {
        if (Files.exists(path)) {
            return;
        }
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "." + spec.id() + "-", ".tmp");
        try {
            Files.writeString(temporary, render(spec), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path);
            }
        } catch (FileAlreadyExistsException exception) {
            // Another writer created the config file.
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static void fillMissing(ConfigSpec spec, Path path, Json5Object existing) throws IOException {
        if (!hasMissing(existing, spec.root())) {
            return;
        }
        Json5Object defaults = Json5InputGuard.parse(render(spec)).getAsJson5Object();
        if (!mergeMissing(existing, defaults)) {
            return;
        }

        writeAtomically(path, serialize(existing));
    }

    static void save(ConfigSpec spec, Path path, Map<ConfigValue<?>, Object> values) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Json5Object document;
        if (Files.exists(path)) {
            Json5Element parsed = Json5InputGuard.parse(Files.readString(path, StandardCharsets.UTF_8));
            if (parsed == null || !parsed.isJson5Object()) {
                throw new IOException("The JSON5 document root must be an object");
            }
            document = parsed.getAsJson5Object();
        } else {
            document = Json5InputGuard.parse(render(spec)).getAsJson5Object();
        }
        for (ConfigValue<?> value : spec.values()) {
            Json5Element encoded = encode(value, values.get(value));
            set(document, value.path().split("\\."), 0, encoded);
        }
        writeAtomically(path, serialize(document));
    }

    private static <T> Json5Element encode(ConfigValue<T> value, Object raw) {
        return value.type().encode(value.cast(raw));
    }

    private static void set(Json5Object object, String[] path, int index, Json5Element value) throws IOException {
        String key = path[index];
        if (index + 1 < path.length) {
            if (object.has(key) && !object.get(key).isJson5Object()) {
                throw new IOException("Expected an object for config section: " + String.join(".", path));
            }
            Json5Object child = object.has(key) ? object.getAsJson5Object(key) : new Json5Object();
            object.add(key, child);
            set(child, path, index + 1, value);
            return;
        }
        if (object.has(key)) {
            copyComments(object.get(key), value);
        }
        object.add(key, value);
    }

    private static void copyComments(Json5Element source, Json5Element target) {
        if (source.hasComment()) target.setComment(source.getComment());
        if (source.isJson5Object() && target.isJson5Object()) {
            Json5Object from = source.getAsJson5Object();
            Json5Object to = target.getAsJson5Object();
            for (String key : to.keySet()) {
                if (from.has(key)) copyComments(from.get(key), to.get(key));
            }
        } else if (source.isJson5Array() && target.isJson5Array()) {
            var from = source.getAsJson5Array();
            var to = target.getAsJson5Array();
            for (int index = 0; index < Math.min(from.size(), to.size()); index++) {
                copyComments(from.get(index), to.get(index));
            }
        }
    }

    private static boolean hasMissing(Json5Object existing, SchemaNode schema) {
        for (SchemaNode child : schema.children.values()) {
            if (!existing.has(child.key)) {
                return true;
            }
            Json5Object section = child.value == null && existing.get(child.key).isJson5Object()
                    ? existing.getAsJson5Object(child.key) : null;
            if (section != null && hasMissing(section, child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean mergeMissing(Json5Object existing, Json5Object defaults) {
        boolean changed = false;
        for (String key : defaults.keySet()) {
            if (!existing.has(key)) {
                existing.add(key, defaults.get(key).deepCopy());
                changed = true;
                continue;
            }

            if (existing.get(key).isJson5Object() && defaults.get(key).isJson5Object()) {
                changed |= mergeMissing(existing.getAsJson5Object(key), defaults.getAsJson5Object(key));
            }
        }
        return changed;
    }

    static void writeAtomically(Path path, String contents) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "." + path.getFileName() + "-", ".tmp");
        boolean moved = false;
        try {
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                moved = true;
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("Atomic file replacement is not supported", exception);
            }
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupFailure) {
                if (!moved) throw cleanupFailure;
            }
        }
    }

    static String render(ConfigSpec spec) {
        Map<ConfigValue<?>, Object> values = new java.util.LinkedHashMap<>();
        for (ConfigValue<?> value : spec.values()) values.put(value, value.defaultValue());
        return render(spec, values);
    }

    static String render(ConfigSpec spec, Map<ConfigValue<?>, Object> values) {
        StringBuilder output = new StringBuilder();
        if (!spec.header().isEmpty()) {
            blockComment(output, 0, spec.header());
        }
        output.append("{\n");
        renderChildren(output, spec.root(), 1, values);
        output.append("}\n");
        return output.toString();
    }

    private static void renderChildren(StringBuilder output, SchemaNode parent, int depth,
                                       Map<ConfigValue<?>, Object> values) {
        Iterator<SchemaNode> iterator = parent.children.values().iterator();
        while (iterator.hasNext()) {
            SchemaNode child = iterator.next();
            if (child.value == null) {
                sectionComment(output, depth, child.description);
                indent(output, depth).append(child.key).append(": {\n");
                renderChildren(output, child, depth + 1, values);
                indent(output, depth).append('}');
            } else {
                valueComment(output, depth, child.value);
                indent(output, depth).append(child.key).append(": ")
                        .append(renderValue(child.value, values.get(child.value)));
            }
            if (iterator.hasNext()) {
                output.append(',');
            }
            output.append("\n");
            if (iterator.hasNext()) {
                output.append("\n");
            }
        }
    }

    private static void sectionComment(StringBuilder output, int depth, List<String> lines) {
        if (lines.isEmpty()) {
            return;
        }
        if (lines.size() == 1) {
            indent(output, depth).append("// ").append(sanitizeComment(lines.get(0))).append('\n');
        } else {
            blockComment(output, depth, lines);
        }
    }

    private static void valueComment(StringBuilder output, int depth, ConfigValue<?> value) {
        List<String> lines = new ArrayList<>(value.description());
        if (value.restartRequirement() == RestartRequirement.REQUIRED) {
            lines.add("Requires a server restart.");
        }
        String metadata = "Default: " + renderValue(value, value.defaultValue());
        for (var constraint : value.constraints()) metadata += " | " + constraint.description();
        lines.add(metadata);
        blockComment(output, depth, lines);
    }

    private static void blockComment(StringBuilder output, int depth, List<String> lines) {
        indent(output, depth).append("/*\n");
        for (String line : lines) {
            indent(output, depth).append(" * ").append(sanitizeComment(line)).append('\n');
        }
        indent(output, depth).append(" */\n");
    }

    private static String sanitizeComment(String value) {
        return value.replace("*/", "* /");
    }

    private static <T> String renderValue(ConfigValue<T> definition, Object value) {
        try {
            return serialize(definition.type().encode(definition.cast(value))).strip();
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    private static String serialize(Json5Element element) throws IOException {
        return JSON5.serialize(plainNumbers(element));
    }

    private static Json5Element plainNumbers(Json5Element element) {
        if (element.isJson5Primitive() && element.getAsJson5Primitive().isNumber()) {
            try {
                var number = de.marhali.json5.Json5Primitive.fromNumber(new PlainDecimal(element.getAsBigDecimal()));
                if (element.hasComment()) number.setComment(element.getComment());
                return number;
            } catch (NumberFormatException ignored) {
                return element; // Keep unknown NaN/infinity entries when filling missing values.
            }
        }
        if (element.isJson5Object()) {
            for (var entry : element.getAsJson5Object().entrySet()) entry.setValue(plainNumbers(entry.getValue()));
        } else if (element.isJson5Array()) {
            var array = element.getAsJson5Array();
            for (int index = 0; index < array.size(); index++) array.set(index, plainNumbers(array.get(index)));
        }
        return element;
    }

    /** The library writer uses Number.toString(); plain decimals avoid its exponent parser bug. */
    private static final class PlainDecimal extends BigDecimal {
        PlainDecimal(BigDecimal value) { super(value.unscaledValue(), value.scale()); }
        @Override public String toString() { return toPlainString(); }
    }

    private static StringBuilder indent(StringBuilder output, int depth) {
        return output.append("  ".repeat(depth));
    }
}
