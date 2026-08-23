package net.syrupstudios.syruplibrary.config;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Factories for client-neutral config editor hints. */
public final class ConfigEditor {
    private static final Pattern EDITOR_ID = Pattern.compile("[a-z][a-z0-9_.-]*:[a-z][a-z0-9_./-]*");
    private static final ConfigEditorHint AUTO = new ConfigEditorHint(
            ConfigEditorHint.Kind.AUTO, null, false, 0, null, Set.of(), null);

    private ConfigEditor() {
    }

    public static ConfigEditorHint auto() { return AUTO; }

    public static ConfigEditorHint slider() {
        return new ConfigEditorHint(ConfigEditorHint.Kind.SLIDER, null, false, 0, null, Set.of(), null);
    }

    public static ConfigEditorHint slider(Number step) {
        Objects.requireNonNull(step, "step");
        if (step.doubleValue() <= 0 || !Double.isFinite(step.doubleValue())) {
            throw new IllegalArgumentException("Slider step must be a positive finite number");
        }
        return new ConfigEditorHint(ConfigEditorHint.Kind.SLIDER, step, false, 0, null, Set.of(), null);
    }

    public static ConfigEditorHint color() { return color(false); }

    public static ConfigEditorHint color(boolean alpha) {
        return new ConfigEditorHint(ConfigEditorHint.Kind.COLOR, null, alpha, 0, null, Set.of(), null);
    }

    public static ConfigEditorHint multiline(int visibleLines) {
        if (visibleLines < 2 || visibleLines > 20) {
            throw new IllegalArgumentException("Visible multiline rows must be between 2 and 20");
        }
        return new ConfigEditorHint(
                ConfigEditorHint.Kind.MULTILINE, null, false, visibleLines, null, Set.of(), null);
    }

    public static ConfigEditorHint path(ConfigPathMode mode, Set<String> extensions) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(extensions, "extensions");
        Set<String> normalized = new LinkedHashSet<>();
        for (String extension : extensions) {
            Objects.requireNonNull(extension, "extension");
            String value = extension.toLowerCase(Locale.ROOT);
            if (value.startsWith(".")) value = value.substring(1);
            if (!value.equals("*") && !value.matches("[a-z0-9][a-z0-9_-]*")) {
                throw new IllegalArgumentException("Invalid file extension: " + extension);
            }
            normalized.add(value);
        }
        if (normalized.isEmpty()) normalized.add("*");
        return new ConfigEditorHint(
                ConfigEditorHint.Kind.PATH, null, false, 0, mode, normalized, null);
    }

    public static ConfigEditorHint custom(String editorId) {
        Objects.requireNonNull(editorId, "editorId");
        if (!EDITOR_ID.matcher(editorId).matches()) {
            throw new IllegalArgumentException("Invalid custom editor ID: " + editorId);
        }
        return new ConfigEditorHint(
                ConfigEditorHint.Kind.CUSTOM, null, false, 0, null, Set.of(), editorId);
    }
}
