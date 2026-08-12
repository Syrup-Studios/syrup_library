package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssue;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssueSeverity;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigLoadResult;
import net.syrupstudios.syruplibrary.config.edit.ConfigEditSession;
import net.syrupstudios.syruplibrary.config.edit.ConfigSaveResult;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;
import net.syrupstudios.syruplibrary.SyrupLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

/** Runtime handle for one registered and initially loaded configuration. */
public final class RegisteredConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyrupLibrary.MOD_ID + "/config");

    private final String ownerModId;
    private final ConfigSpec spec;
    private final Path path;
    private final ConfigLoadResult initialResult;
    private volatile ConfigLoadResult latestResult;

    RegisteredConfig(String ownerModId, ConfigSpec spec, Path path) {
        this.ownerModId = ownerModId;
        this.spec = spec;
        this.path = path;
        this.initialResult = load(true);
        this.latestResult = initialResult;
    }

    /** Returns the mod ID that owns this configuration. */
    public String ownerModId() {
        return ownerModId;
    }

    /** Reloads, validates, and atomically publishes this config. */
    public synchronized ConfigLoadResult reload() {
        ConfigLoadResult result = load(false);
        latestResult = result;
        return result;
    }

    /**
     * Saves the drafts of an edit session, reloads the config, and publishes a new snapshot.
     *
     * <p>The save refuses to overwrite a file that changed while the session was open and never
     * publishes a partial snapshot.
     */
    public synchronized ConfigSaveResult save(ConfigEditSession session) {
        if (session == null || session.config() != this) {
            return ConfigSaveResult.failure(false,
                    "The edit session does not belong to this config");
        }
        return saveValues(session.draftValues(), session.originalFile());
    }

    /**
     * Writes the supplied draft values into the config file and publishes the reloaded snapshot.
     *
     * <p>Only the supplied paths are updated; untouched known and unknown values are preserved.
     * The map may contain only values belonging to this spec, and every draft must validate.
     */
    synchronized ConfigSaveResult saveValues(Map<ConfigValue<?>, Object> values,
                                             byte[] originalFile) {
        for (ConfigValue<?> candidate : values.keySet()) {
            if (!containsValue(spec, candidate)) {
                return ConfigSaveResult.failure(false,
                        "A value does not belong to this config: " + candidate.path());
            }
        }
        try {
            if (!DefaultJson5Writer.updateValues(spec, path, values, originalFile)) {
                String message = "The config file changed outside the game. Reload the editor before saving.";
                return ConfigSaveResult.failure(true, message);
            }
        } catch (java.io.IOException exception) {
            LOGGER.error("Could not save config {} to {}", spec.id(), path, exception);
            return ConfigSaveResult.failure(false,
                    exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected failure while saving config {} to {}", spec.id(), path, exception);
            return ConfigSaveResult.failure(false,
                    "Unexpected failure while saving");
        }

        ConfigLoadResult reload = reload();
        if (!reload.successful()) {
            boolean restored = restoreOriginal(originalFile);
            return ConfigSaveResult.failure(false,
                    restored
                            ? "The saved file did not reload cleanly; the original file was restored"
                            : "The saved file did not reload cleanly and the original file could not be restored");
        }
        return ConfigSaveResult.ok();
    }

    private static boolean containsValue(ConfigSpec spec, ConfigValue<?> candidate) {
        for (ConfigValue<?> value : spec.values()) {
            if (value == candidate) {
                return true;
            }
        }
        return false;
    }

    /** Restores the file atomically; returns whether the restoration succeeded. */
    private boolean restoreOriginal(byte[] originalBytes) {
        try {
            DefaultJson5Writer.writeBytesAtomically(path, originalBytes);
            return true;
        } catch (java.io.IOException exception) {
            LOGGER.error("Could not restore the previous config file {}", path, exception);
            return false;
        }
    }

    /** Returns the result produced during registration's initial load. */
    public ConfigLoadResult initialResult() { return initialResult; }

    /** Returns diagnostics from the most recent load or reload. */
    public ConfigLoadResult latestResult() { return latestResult; }

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
}
