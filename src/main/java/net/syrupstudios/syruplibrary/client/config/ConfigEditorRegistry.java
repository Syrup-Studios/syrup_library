package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.SyrupLibrary;
import net.syrupstudios.syruplibrary.config.ConfigEditorHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;

/** Client-only registry for custom config value editors. */
public final class ConfigEditorRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyrupLibrary.MOD_ID + "/config-editor");
    private static final Map<String, ConfigEditorFactory> CUSTOM_EDITORS = new LinkedHashMap<>();
    private static final Set<String> WARNED_MISSING = new LinkedHashSet<>();
    private static boolean frozen;

    private ConfigEditorRegistry() {
    }

    public static synchronized void register(String id, ConfigEditorFactory factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        if (frozen) throw new IllegalStateException("Config editor registry is already frozen");
        if (CUSTOM_EDITORS.putIfAbsent(id, factory) != null) {
            throw new IllegalArgumentException("A config editor is already registered with ID " + id);
        }
    }

    static synchronized ConfigEditorHandle create(ConfigEditorContext context) {
        frozen = true;
        ConfigEditorHint hint = context.node().presentation().editor();
        if (hint.kind() == ConfigEditorHint.Kind.CUSTOM) {
            ConfigEditorFactory factory = CUSTOM_EDITORS.get(hint.customEditorId());
            if (factory != null) return Objects.requireNonNull(factory.create(context), "custom editor handle");
            if (WARNED_MISSING.add(hint.customEditorId())) {
                LOGGER.warn("Custom config editor {} is not registered for {}; using the automatic editor",
                        hint.customEditorId(), context.valueDefinition().path());
            }
        }
        return BuiltInConfigEditors.create(context);
    }
}
