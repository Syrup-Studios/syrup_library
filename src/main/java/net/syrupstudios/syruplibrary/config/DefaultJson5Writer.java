package net.syrupstudios.syruplibrary.config;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Primitive;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigLoadResult;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;
import net.syrupstudios.syruplibrary.config.value.DoubleConfigValue;
import net.syrupstudios.syruplibrary.config.value.IntConfigValue;
import net.syrupstudios.syruplibrary.config.value.LongConfigValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class DefaultJson5Writer {
    private static final Json5 JSON5 = new Json5();

    private DefaultJson5Writer() {
    }

    static boolean createIfMissing(ConfigSpec spec, Path path) throws IOException {
        if (Files.exists(path)) {
            return false;
        }
        Path parent = path.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "." + spec.id() + "-", ".tmp");
        try {
            Files.writeString(temporary, render(spec), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path);
            }
            return true;
        } catch (FileAlreadyExistsException exception) {
            return false;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static boolean fillMissing(ConfigSpec spec, Path path, Json5Object existing) throws IOException {
        Json5Object defaults = JSON5.parse(render(spec)).getAsJson5Object();
        if (!mergeMissing(existing, defaults)) {
            return false;
        }

        writeAtomically(path, JSON5.serialize(existing));
        return true;
    }

    /**
     * Safely updates draft values in an existing config file.
     *
     * <p>Only the supplied paths are replaced; untouched known and unknown values are preserved.
     * The file is replaced only when its current bytes match {@code originalFile} both when
     * it is read and again immediately before replacement, closing the main check/read race.
     * Comments attached to replaced values are transferred to their replacements. Serialization may
     * normalize whitespace.
     */
    static boolean updateValues(ConfigSpec spec, Path path, Map<ConfigValue<?>, Object> values,
                                byte[] originalFile) throws IOException {
        byte[] currentBytes = Files.readAllBytes(path);
        if (!Arrays.equals(currentBytes, originalFile)) return false;

        Json5Object document;
        try {
            Json5Element parsed = JSON5.parse(new String(currentBytes, StandardCharsets.UTF_8));
            if (parsed == null || !parsed.isJson5Object()) {
                throw new IllegalStateException("The JSON5 document root must be an object");
            }
            document = parsed.getAsJson5Object();
        } catch (RuntimeException exception) {
            throw new IOException("The config document could not be parsed", exception);
        }

        Json5Object candidate = document.deepCopy();
        replaceValues(candidate, spec, values);

        String serialized;
        try {
            serialized = JSON5.serialize(candidate);
        } catch (RuntimeException exception) {
            throw new IOException("Could not serialize the updated config document", exception);
        }

        Json5Object parsedCandidate;
        try {
            Json5Element parsed = JSON5.parse(serialized);
            if (parsed == null || !parsed.isJson5Object()) {
                throw new IllegalStateException("The serialized document root must be an object");
            }
            parsedCandidate = parsed.getAsJson5Object();
        } catch (RuntimeException exception) {
            throw new IOException("The serialized candidate failed to parse", exception);
        }

        ConfigLoadResult validation = ConfigLoader.validateSerializedValues(parsedCandidate, values);
        if (!validation.successful()) {
            throw new IOException("An edited value failed JSON5 validation");
        }

        if (!Arrays.equals(Files.readAllBytes(path), originalFile)) return false;

        writeAtomically(path, serialized);
        return true;
    }

    private static void replaceValues(Json5Object root, ConfigSpec spec, Map<ConfigValue<?>, Object> values) {
        for (ConfigValue<?> value : spec.values()) {
            Object replacement = values.get(value);
            if (replacement == null) {
                continue;
            }
            Json5Element newElement = toJson5(replacement);
            String[] segments = value.path().split("\\.");
            Json5Object parent = root;
            for (int index = 0; index < segments.length - 1; index++) {
                Json5Element next = parent.get(segments[index]);
                if (next == null || !next.isJson5Object()) {
                    Json5Object created = new Json5Object();
                    if (next != null) {
                        created.setComment(next.getComment());
                    }
                    parent.add(segments[index], created);
                    next = created;
                }
                parent = next.getAsJson5Object();
            }
            String leaf = segments[segments.length - 1];
            Json5Element old = parent.get(leaf);
            if (old != null) {
                newElement.setComment(old.getComment());
            }
            parent.add(leaf, newElement);
        }
    }

    private static Json5Element toJson5(Object value) {
        if (value instanceof String string) {
            return Json5Primitive.fromString(string);
        }
        if (value instanceof Boolean bool) {
            return Json5Primitive.fromBoolean(bool);
        }
        if (value instanceof Number number) {
            return Json5Primitive.fromNumber(number);
        }
        if (value instanceof Enum<?> enumValue) {
            return Json5Primitive.fromString(enumValue.name().toLowerCase(java.util.Locale.ROOT));
        }
        if (value instanceof List<?> list) {
            Json5Array array = new Json5Array();
            for (Object item : list) {
                array.add((String) item);
            }
            return array;
        }
        throw new IllegalArgumentException("Unsupported config value type " + value.getClass().getName());
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

    private static void writeAtomically(Path path, String contents) throws IOException {
        writeBytesAtomically(path, contents.getBytes(StandardCharsets.UTF_8));
    }

    /** Writes bytes through a temporary file and an atomic replacement. */
    static void writeBytesAtomically(Path path, byte[] bytes) throws IOException {
        Path parent = path.getParent();
        Path temporary = Files.createTempFile(parent, "." + path.getFileName() + "-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static String render(ConfigSpec spec) {
        StringBuilder output = new StringBuilder();
        if (!spec.header().isEmpty()) {
            blockComment(output, 0, spec.header());
        }
        output.append("{\n");
        renderChildren(output, spec.root(), 1);
        output.append("}\n");
        return output.toString();
    }

    private static void renderChildren(StringBuilder output, ConfigSchemaNode parent, int depth) {
        Iterator<ConfigSchemaNode> iterator = parent.children.values().iterator();
        while (iterator.hasNext()) {
            ConfigSchemaNode child = iterator.next();
            if (child.value == null) {
                sectionComment(output, depth, child.description);
                indent(output, depth).append(child.key).append(": {\n");
                renderChildren(output, child, depth + 1);
                indent(output, depth).append('}');
            } else {
                valueComment(output, depth, child.value);
                indent(output, depth).append(child.key).append(": ")
                        .append(renderValue(child.value.defaultValue()));
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
        String metadata = "Default: " + renderValue(value.defaultValue());
        if (value instanceof IntConfigValue integer) {
            metadata += " | Range: " + integer.minimum() + " ~ " + integer.maximum();
        } else if (value instanceof LongConfigValue longValue) {
            metadata += " | Range: " + longValue.minimum() + " ~ " + longValue.maximum();
        } else if (value instanceof DoubleConfigValue doubleValue) {
            metadata += " | Range: " + doubleValue.minimum() + " ~ " + doubleValue.maximum();
        }
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

    private static String renderValue(Object value) {
        if (value instanceof String string) {
            return quote(string);
        }
        if (value instanceof Enum<?> enumValue) {
            return quote(enumValue.name().toLowerCase(java.util.Locale.ROOT));
        }
        if (value instanceof List<?> list) {
            return list.stream().map(item -> quote((String) item))
                    .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
        }
        return String.valueOf(value);
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    private static StringBuilder indent(StringBuilder output, int depth) {
        return output.append("  ".repeat(depth));
    }
}
