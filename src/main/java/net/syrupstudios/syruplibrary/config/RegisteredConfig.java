package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssue;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssueSeverity;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigLoadResult;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigSaveResult;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigUpdateResult;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;
import net.syrupstudios.syruplibrary.config.value.DoubleConfigValue;
import net.syrupstudios.syruplibrary.config.value.IntConfigValue;
import net.syrupstudios.syruplibrary.config.value.LongConfigValue;
import net.syrupstudios.syruplibrary.config.value.StringConfigValue;
import net.syrupstudios.syruplibrary.config.value.StringListConfigValue;
import net.syrupstudios.syruplibrary.SyrupLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runtime handle for one registered and initially loaded configuration. */
public final class RegisteredConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyrupLibrary.MOD_ID + "/config");

    private final ConfigSpec spec;
    private final Path path;
    private final ConfigLoadResult initialResult;

    RegisteredConfig(ConfigSpec spec, Path path) {
        this.spec = spec;
        this.path = path;
        this.initialResult = load(true);
    }

    /** Reloads, validates, and atomically publishes this config. */
    public synchronized ConfigLoadResult reload() {
        return load(false);
    }

    /** Validates and publishes one configured value. */
    public synchronized <T> ConfigUpdateResult update(ConfigValue<T> value, T next) {
        Map<ConfigValue<T>, T> updates = new LinkedHashMap<>();
        updates.put(value, next);
        return updateAll(updates);
    }

    /** Validates and publishes a group of values as one operation. */
    public synchronized ConfigUpdateResult updateAll(Map<? extends ConfigValue<?>, ?> updates) {
        return apply(updates, false);
    }

    /** Validates, atomically saves, and then publishes one configured value. */
    public synchronized <T> ConfigUpdateResult updateAndSave(ConfigValue<T> value, T next) {
        Map<ConfigValue<T>, T> updates = new LinkedHashMap<>();
        updates.put(value, next);
        return updateAndSaveAll(updates);
    }

    /** Validates, atomically saves, and then publishes a batch of configured values. */
    public synchronized ConfigUpdateResult updateAndSaveAll(Map<? extends ConfigValue<?>, ?> updates) {
        return apply(updates, true);
    }

    private ConfigUpdateResult apply(Map<? extends ConfigValue<?>, ?> updates, boolean save) {
        List<ConfigIssue> issues = new ArrayList<>();
        Map<ConfigValue<?>, Object> current = spec.currentState().configured().values();
        Map<ConfigValue<?>, Object> values = new LinkedHashMap<>(current);
        if (updates == null) return new ConfigUpdateResult(false,
                List.of(error(spec.id(), "Updates must not be null", null)), null);
        for (Map.Entry<? extends ConfigValue<?>, ?> entry : updates.entrySet()) {
            ConfigValue<?> value = entry.getKey();
            Object next = entry.getValue();
            if (value == null || !current.containsKey(value)) issues.add(error(value == null ? spec.id() : value.path(), "Value does not belong to this configuration", next));
            else {
                int issueStart = issues.size();
                Object normalized = ConfigLoader.validateUpdate(value, next, issues);
                for (int index = issueStart; index < issues.size(); index++) {
                    ConfigIssue issue = issues.get(index);
                    issues.set(index, new ConfigIssue(issue.path(), issue.severity(), issue.message(),
                            issue.originalValue(), current.get(value)));
                }
                if (normalized != ConfigLoader.INVALID_UPDATE) values.put(value, value.cast(normalized));
            }
        }
        if (!issues.isEmpty()) return new ConfigUpdateResult(false, issues, null);
        ConfigState nextState = ConfigLoader.stateFor(spec, new ConfigSnapshot(values), issues);
        if (!save) {
            spec.publish(nextState);
            return new ConfigUpdateResult(true, issues, null);
        }
        try {
            DefaultJson5Writer.writeAtomically(path, DefaultJson5Writer.render(spec, values));
        } catch (Exception exception) {
            issues.add(error(path.toString(), "Could not save configuration: " + exception.getMessage(), null));
            return new ConfigUpdateResult(false, issues, exception);
        }
        spec.publish(nextState);
        return new ConfigUpdateResult(true, issues, null);
    }

    /** Saves the latest configured values with an atomic file replacement. */
    public synchronized ConfigSaveResult save() {
        try {
            Files.createDirectories(path.getParent());
            DefaultJson5Writer.writeAtomically(path,
                    DefaultJson5Writer.render(spec, spec.currentState().configured().values()));
            return new ConfigSaveResult(true, List.of(), null);
        } catch (Exception exception) {
            ConfigIssue issue = error(path.toString(), "Could not save configuration: " + exception.getMessage(), null);
            return new ConfigSaveResult(false, List.of(issue), exception);
        }
    }

    /** Returns the result produced during registration's initial load. */
    public ConfigLoadResult initialResult() { return initialResult; }

    /** Returns the immutable schema. */
    public ConfigSpec spec() { return spec; }

    /** Returns the absolute JSON5 file path. */
    public Path path() { return path; }

    /** Returns one consistent effective-value snapshot. */
    public ConfigSnapshot snapshot() { return spec.currentState().effective(); }

    /** Returns one consistent latest-configured snapshot. */
    public ConfigSnapshot configuredSnapshot() { return spec.currentState().configured(); }

    /** Returns one consistent initial startup snapshot. */
    public ConfigSnapshot startupSnapshot() { return spec.currentState().startup(); }

    private ConfigLoadResult load(boolean initial) {
        ConfigLoadResult result = ConfigLoader.load(this, initial);
        if (result.successful()) {
            LOGGER.info("Loaded configuration {} from {}", spec.id(), path);
        } else {
            LOGGER.error("Could not load configuration {} from {}; keeping the previous valid snapshot",
                    spec.id(), path, result.cause());
        }
        for (ConfigIssue issue : result.issues()) {
            if (issue.severity() == ConfigIssueSeverity.WARNING) {
                LOGGER.warn("Config {}: {}", issue.path(), issue.message());
            }
        }
        return result;
    }

    private static ConfigIssue error(String path, String message, Object original) {
        return new ConfigIssue(path, ConfigIssueSeverity.ERROR, message, original, null);
    }

}
