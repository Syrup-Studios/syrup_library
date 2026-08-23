package net.syrupstudios.syruplibrary.config;

import java.util.List;
import java.util.Objects;

/** Builder for a nested config section and its screen metadata. */
public final class SectionBuilder {
    private final ConfigSpec spec;
    private final ConfigSchemaNode parent;
    private final String key;
    private final ConfigPresentation.Builder presentation = ConfigPresentation.builder();
    private List<String> description = List.of();
    private boolean built;

    SectionBuilder(ConfigSpec spec, ConfigSchemaNode parent, String key) {
        this.spec = spec;
        this.parent = parent;
        this.key = key;
    }

    public SectionBuilder description(String description) {
        this.description = ConfigSpec.description(description);
        return this;
    }

    public SectionBuilder labelTranslationKey(String key) {
        presentation.labelTranslationKey(key);
        return this;
    }

    public SectionBuilder descriptionTranslationKey(String key) {
        presentation.descriptionTranslationKey(key);
        return this;
    }

    public SectionBuilder group(ConfigGroup group) {
        presentation.group(group);
        return this;
    }

    public SectionBuilder visibleWhen(ConfigCondition condition) {
        presentation.visibleWhen(Objects.requireNonNull(condition, "condition"));
        return this;
    }

    public ConfigSection build() {
        if (built) throw new IllegalStateException("Config section is already built: " + key);
        built = true;
        return spec.addSection(parent, key, description, presentation.build());
    }
}
