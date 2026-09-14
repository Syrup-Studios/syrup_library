package net.syrupstudios.syruplibrary.config;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Object;
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
import java.util.Iterator;
import java.util.List;

final class DefaultJson5Writer {
    private static final Json5 JSON5 = new Json5();

    private DefaultJson5Writer() {
    }

    static void createIfMissing(ConfigSpec spec, Path path) throws IOException {
        if (Files.exists(path)) {
            return;
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
        Json5Object defaults = JSON5.parse(render(spec)).getAsJson5Object();
        if (!mergeMissing(existing, defaults)) {
            return;
        }

        writeAtomically(path, JSON5.serialize(existing));
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

    private static void writeAtomically(Path path, String contents) throws IOException {
        Path parent = path.getParent();
        Path temporary = Files.createTempFile(parent, "." + path.getFileName() + "-", ".tmp");
        try {
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
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

    private static void renderChildren(StringBuilder output, SchemaNode parent, int depth) {
        Iterator<SchemaNode> iterator = parent.children.values().iterator();
        while (iterator.hasNext()) {
            SchemaNode child = iterator.next();
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
