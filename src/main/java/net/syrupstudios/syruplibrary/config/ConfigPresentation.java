package net.syrupstudios.syruplibrary.config;

import java.util.Objects;
import java.util.Optional;

/** Immutable client-neutral presentation metadata for a config schema node. */
public final class ConfigPresentation {
    public static final ConfigPresentation DEFAULT = builder().build();

    private final String labelTranslationKey;
    private final String descriptionTranslationKey;
    private final ConfigEditorHint editor;
    private final ConfigGroup group;
    private final ConfigCondition visibilityCondition;
    private final ConfigCondition enabledCondition;
    private final ConfigScope scope;
    private final ConfigEditPolicy editPolicy;
    private final EnumPresentation enumPresentation;

    private ConfigPresentation(Builder builder) {
        this.labelTranslationKey = builder.labelTranslationKey;
        this.descriptionTranslationKey = builder.descriptionTranslationKey;
        this.editor = builder.editor;
        this.group = builder.group;
        this.visibilityCondition = builder.visibilityCondition;
        this.enabledCondition = builder.enabledCondition;
        this.scope = builder.scope;
        this.editPolicy = builder.editPolicy;
        this.enumPresentation = builder.enumPresentation;
    }

    public static Builder builder() { return new Builder(); }

    public Optional<String> labelTranslationKey() { return Optional.ofNullable(labelTranslationKey); }

    public Optional<String> descriptionTranslationKey() { return Optional.ofNullable(descriptionTranslationKey); }

    public ConfigEditorHint editor() { return editor; }

    public Optional<ConfigGroup> group() { return Optional.ofNullable(group); }

    public ConfigCondition visibilityCondition() { return visibilityCondition; }

    public ConfigCondition enabledCondition() { return enabledCondition; }

    public ConfigScope scope() { return scope; }

    public ConfigEditPolicy editPolicy() { return editPolicy; }

    public EnumPresentation enumPresentation() { return enumPresentation; }

    /** Mutable builder used while a config spec is being declared. */
    public static final class Builder {
        private String labelTranslationKey;
        private String descriptionTranslationKey;
        private ConfigEditorHint editor = ConfigEditor.auto();
        private ConfigGroup group;
        private ConfigCondition visibilityCondition = ConfigCondition.always();
        private ConfigCondition enabledCondition = ConfigCondition.always();
        private ConfigScope scope = ConfigScope.COMMON;
        private ConfigEditPolicy editPolicy = ConfigEditPolicy.EDITABLE;
        private boolean editPolicyExplicit;
        private EnumPresentation enumPresentation = EnumPresentation.DEFAULT;

        private Builder() {
        }

        public Builder labelTranslationKey(String key) {
            this.labelTranslationKey = translationKey(key);
            return this;
        }

        public Builder descriptionTranslationKey(String key) {
            this.descriptionTranslationKey = translationKey(key);
            return this;
        }

        public Builder editor(ConfigEditorHint editor) {
            this.editor = Objects.requireNonNull(editor, "editor");
            return this;
        }

        public Builder group(ConfigGroup group) {
            this.group = Objects.requireNonNull(group, "group");
            return this;
        }

        public Builder visibleWhen(ConfigCondition condition) {
            this.visibilityCondition = Objects.requireNonNull(condition, "condition");
            return this;
        }

        public Builder enabledWhen(ConfigCondition condition) {
            this.enabledCondition = Objects.requireNonNull(condition, "condition");
            return this;
        }

        public Builder scope(ConfigScope scope) {
            this.scope = Objects.requireNonNull(scope, "scope");
            if (!editPolicyExplicit) {
                this.editPolicy = scope == ConfigScope.SERVER
                        ? ConfigEditPolicy.READ_ONLY
                        : ConfigEditPolicy.EDITABLE;
            }
            return this;
        }

        public Builder editPolicy(ConfigEditPolicy editPolicy) {
            this.editPolicy = Objects.requireNonNull(editPolicy, "editPolicy");
            this.editPolicyExplicit = true;
            return this;
        }

        public Builder enumPresentation(EnumPresentation enumPresentation) {
            this.enumPresentation = Objects.requireNonNull(enumPresentation, "enumPresentation");
            return this;
        }

        public ConfigPresentation build() { return new ConfigPresentation(this); }
    }

    static String translationKey(String key) {
        Objects.requireNonNull(key, "translation key");
        if (!key.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid translation key: " + key);
        }
        return key;
    }
}
