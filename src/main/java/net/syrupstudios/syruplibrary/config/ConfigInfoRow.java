package net.syrupstudios.syruplibrary.config;

import java.util.Objects;
import java.util.Optional;

/** Non-persistent information shown in a config section. */
public final class ConfigInfoRow implements ConfigScreenElement {
    private final String id;
    private final String text;
    private final ConfigInfoStyle style;
    private final String link;
    private final ConfigPresentation presentation;

    ConfigInfoRow(String id, String text, ConfigInfoStyle style, String link,
                  ConfigPresentation presentation) {
        this.id = Objects.requireNonNull(id, "id");
        this.text = text == null ? "" : text;
        this.style = Objects.requireNonNull(style, "style");
        this.link = link;
        this.presentation = Objects.requireNonNull(presentation, "presentation");
    }

    public String id() { return id; }

    public String text() { return text; }

    public ConfigInfoStyle style() { return style; }

    public Optional<String> link() { return Optional.ofNullable(link); }

    @Override
    public ConfigPresentation presentation() { return presentation; }
}
