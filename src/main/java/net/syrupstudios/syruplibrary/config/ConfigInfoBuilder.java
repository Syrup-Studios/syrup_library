package net.syrupstudios.syruplibrary.config;

import java.net.URI;
import java.util.Objects;

/** Builder for one non-persistent information row. */
public final class ConfigInfoBuilder {
    private final ConfigSpec spec;
    private final ConfigSchemaNode parent;
    private final String id;
    private final ConfigPresentation.Builder presentation = ConfigPresentation.builder();
    private String text = "";
    private ConfigInfoStyle style = ConfigInfoStyle.TEXT;
    private String link;
    private boolean built;

    ConfigInfoBuilder(ConfigSpec spec, ConfigSchemaNode parent, String id) {
        this.spec = spec;
        this.parent = parent;
        this.id = id;
    }

    public ConfigInfoBuilder text(String text) {
        this.text = Objects.requireNonNull(text, "text");
        return this;
    }

    public ConfigInfoBuilder translationKey(String key) {
        presentation.labelTranslationKey(key);
        return this;
    }

    public ConfigInfoBuilder style(ConfigInfoStyle style) {
        this.style = Objects.requireNonNull(style, "style");
        return this;
    }

    public ConfigInfoBuilder link(String link) {
        Objects.requireNonNull(link, "link");
        URI uri = URI.create(link);
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("Config links must use HTTP or HTTPS");
        }
        this.link = link;
        this.style = ConfigInfoStyle.LINK;
        return this;
    }

    public ConfigInfoBuilder group(ConfigGroup group) {
        presentation.group(group);
        return this;
    }

    public ConfigInfoBuilder visibleWhen(ConfigCondition condition) {
        presentation.visibleWhen(condition);
        return this;
    }

    public ConfigInfoRow build() {
        if (built) throw new IllegalStateException("Information row is already built: " + id);
        built = true;
        if (style == ConfigInfoStyle.LINK && link == null) {
            throw new IllegalStateException("Link information row has no link: " + id);
        }
        return spec.addInfo(parent, new ConfigInfoRow(id, text, style, link, presentation.build()));
    }
}
