package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.ConfigValue;

import java.util.List;
import java.util.Objects;

/** Shared fluent declaration API for stored config entries. */
public abstract class ConfigEntryBuilder<T, V extends ConfigValue<T>, B extends ConfigEntryBuilder<T, V, B>> {
    protected final ConfigSpec spec;
    protected final ConfigSchemaNode parent;
    protected final String key;
    protected final ConfigPresentation.Builder presentation = ConfigPresentation.builder();
    protected List<String> description = List.of();
    protected RestartRequirement restartRequirement = RestartRequirement.NONE;
    private boolean built;

    ConfigEntryBuilder(ConfigSpec spec, ConfigSchemaNode parent, String key) {
        this.spec = spec;
        this.parent = parent;
        this.key = key;
    }

    protected abstract B self();

    protected abstract V createValue(String path);

    protected void preparePresentation() {
    }

    public B description(String description) {
        this.description = ConfigSpec.description(description);
        return self();
    }

    public B restartRequirement(RestartRequirement requirement) {
        this.restartRequirement = Objects.requireNonNull(requirement, "requirement");
        return self();
    }

    public B labelTranslationKey(String translationKey) {
        presentation.labelTranslationKey(translationKey);
        return self();
    }

    public B descriptionTranslationKey(String translationKey) {
        presentation.descriptionTranslationKey(translationKey);
        return self();
    }

    public B editor(ConfigEditorHint editor) {
        presentation.editor(editor);
        return self();
    }

    public B group(ConfigGroup group) {
        presentation.group(group);
        return self();
    }

    public B visibleWhen(ConfigCondition condition) {
        presentation.visibleWhen(condition);
        return self();
    }

    public <U> B visibleWhen(ConfigValue<U> value, U expected) {
        return visibleWhen(ConfigCondition.equals(value, expected));
    }

    public B enabledWhen(ConfigCondition condition) {
        presentation.enabledWhen(condition);
        return self();
    }

    public <U> B enabledWhen(ConfigValue<U> value, U expected) {
        return enabledWhen(ConfigCondition.equals(value, expected));
    }

    public B scope(ConfigScope scope) {
        presentation.scope(scope);
        return self();
    }

    public B editPolicy(ConfigEditPolicy policy) {
        presentation.editPolicy(policy);
        return self();
    }

    public final V build() {
        if (built) throw new IllegalStateException("Config entry is already built: " + key);
        built = true;
        spec.ensureMutable();
        preparePresentation();
        String path = spec.childPath(parent, key);
        return spec.addValue(parent, key, createValue(path), presentation.build());
    }
}
