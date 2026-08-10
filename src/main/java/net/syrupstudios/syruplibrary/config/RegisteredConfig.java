package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssue;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssueSeverity;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigLoadResult;
import net.syrupstudios.syruplibrary.SyrupLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

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
}
