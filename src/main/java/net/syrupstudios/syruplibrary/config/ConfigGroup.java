package net.syrupstudios.syruplibrary.config;

import java.util.Objects;
import java.util.Optional;

/** Presentation-only group for entries in one config section. */
public final class ConfigGroup {
    private final ConfigSpec spec;
    private final ConfigSchemaNode owner;
    private final String id;
    private String labelTranslationKey;
    private String descriptionTranslationKey;
    private ConfigCondition visibilityCondition = ConfigCondition.always();

    ConfigGroup(ConfigSpec spec, ConfigSchemaNode owner, String id) {
        this.spec = spec;
        this.owner = owner;
        this.id = id;
    }

    public String id() { return id; }

    public Optional<String> labelTranslationKey() { return Optional.ofNullable(labelTranslationKey); }

    public Optional<String> descriptionTranslationKey() { return Optional.ofNullable(descriptionTranslationKey); }

    public ConfigCondition visibilityCondition() { return visibilityCondition; }

    public ConfigGroup labelTranslationKey(String key) {
        spec.ensureMutable();
        this.labelTranslationKey = ConfigPresentation.translationKey(key);
        return this;
    }

    public ConfigGroup descriptionTranslationKey(String key) {
        spec.ensureMutable();
        this.descriptionTranslationKey = ConfigPresentation.translationKey(key);
        return this;
    }

    public ConfigGroup visibleWhen(ConfigCondition condition) {
        spec.ensureMutable();
        this.visibilityCondition = Objects.requireNonNull(condition, "condition");
        return this;
    }

    ConfigSpec spec() { return spec; }

    ConfigSchemaNode owner() { return owner; }
}
