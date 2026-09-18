package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.loaders.Platform;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Registry and path owner for independently registered config files. */
public final class SyrupConfigManager {
    private final Path configDirectory;
    private final Map<String, RegisteredConfig> configs = new LinkedHashMap<>();

    private SyrupConfigManager(Path configDirectory) {
        this.configDirectory = Objects.requireNonNull(configDirectory, "configDirectory")
                .toAbsolutePath().normalize();
    }

    /** Returns the process-wide config manager for the active mod loader. */
    public static SyrupConfigManager getInstance() {
        return Holder.INSTANCE;
    }

    /** Creates an isolated manager rooted at a supplied directory. */
    public static SyrupConfigManager create(Path configDirectory) {
        return new SyrupConfigManager(configDirectory);
    }

    /** Registers and immediately loads a spec, returning its runtime handle. */
    public synchronized RegisteredConfig register(ConfigSpec spec) {
        Objects.requireNonNull(spec, "spec");
        if (configs.containsKey(spec.id())) {
            throw new IllegalArgumentException("A configuration is already registered with ID " + spec.id());
        }
        Path path = configDirectory.resolve(spec.id() + ".json5").normalize();
        if (!path.getParent().equals(configDirectory)) {
            throw new IllegalArgumentException("Config path escapes the loader config directory: " + path);
        }
        spec.sealForRegistration();
        RegisteredConfig registered = new RegisteredConfig(spec, path);
        configs.put(spec.id(), registered);
        return registered;
    }

    /** Finds a registered config by ID. */
    public synchronized Optional<RegisteredConfig> find(String id) {
        return Optional.ofNullable(configs.get(id));
    }

    /** Returns the normalized config directory. */
    public Path configDirectory() { return configDirectory; }

    /** Returns a stable immutable view of registrations by ID. */
    public synchronized Map<String, RegisteredConfig> registeredConfigs() {
        return Map.copyOf(configs);
    }

    private static final class Holder {
        private static final SyrupConfigManager INSTANCE =
                new SyrupConfigManager(Platform.INSTANCE.configDirectory());
    }
}
