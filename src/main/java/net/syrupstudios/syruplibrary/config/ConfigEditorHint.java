package net.syrupstudios.syruplibrary.config;

import java.util.Objects;
import java.util.Set;

/** Client-neutral description of the preferred editor for a config value. */
public final class ConfigEditorHint {
    public enum Kind {
        AUTO,
        SLIDER,
        COLOR,
        MULTILINE,
        PATH,
        CUSTOM
    }

    private final Kind kind;
    private final Number step;
    private final boolean alpha;
    private final int visibleLines;
    private final ConfigPathMode pathMode;
    private final Set<String> extensions;
    private final String customEditorId;

    ConfigEditorHint(Kind kind, Number step, boolean alpha, int visibleLines,
                     ConfigPathMode pathMode, Set<String> extensions, String customEditorId) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.step = step;
        this.alpha = alpha;
        this.visibleLines = visibleLines;
        this.pathMode = pathMode;
        this.extensions = Set.copyOf(extensions);
        this.customEditorId = customEditorId;
    }

    public Kind kind() { return kind; }

    public Number step() { return step; }

    public boolean alpha() { return alpha; }

    public int visibleLines() { return visibleLines; }

    public ConfigPathMode pathMode() { return pathMode; }

    public Set<String> extensions() { return extensions; }

    public String customEditorId() { return customEditorId; }
}
