package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.loaders.Platform;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Registry and path owner for independently registered config files. */
public final class SyrupConfigManager {
    private static final Pattern OWNER_PATTERN = Pattern.compile("[a-z][a-z0-9_-]*");

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

    /** Creates an isolated manager rooted at a supplied directory, primarily for tools and tests. */
    public static SyrupConfigManager create(Path configDirectory) {
        return new SyrupConfigManager(configDirectory);
    }

    /** Registers and immediately loads a spec, using the config ID as the owning mod ID. */
    public synchronized RegisteredConfig register(ConfigSpec spec) {
        return register(spec.id(), spec);
    }

    /** Registers and immediately loads a spec on behalf of an owning mod ID. */
    public synchronized RegisteredConfig register(String ownerModId, ConfigSpec spec) {
        Objects.requireNonNull(spec, "spec");
        validateOwner(ownerModId);
        if (configs.containsKey(spec.id())) {
            throw new IllegalArgumentException("A configuration is already registered with ID " + spec.id());
        }
        Path path = configDirectory.resolve(spec.id() + ".json5").normalize();
        if (!path.getParent().equals(configDirectory)) {
            throw new IllegalArgumentException("Config path escapes the loader config directory: " + path);
        }
        spec.sealForRegistration();
        RegisteredConfig registered = new RegisteredConfig(ownerModId, spec, path);
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

    /** Returns the configs registered by a specific owning mod, keyed by config ID. */
    public synchronized Map<String, RegisteredConfig> registeredConfigs(String ownerModId) {
        Objects.requireNonNull(ownerModId, "ownerModId");
        Map<String, RegisteredConfig> owned = new LinkedHashMap<>();
        for (Map.Entry<String, RegisteredConfig> entry : configs.entrySet()) {
            if (entry.getValue().ownerModId().equals(ownerModId)) {
                owned.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(owned);
    }

    /** Returns the set of mod IDs that own at least one registered config. */
    public synchronized Set<String> registeredOwnerIds() {
        Set<String> owners = new LinkedHashSet<>();
        for (RegisteredConfig config : configs.values()) {
            owners.add(config.ownerModId());
        }
        return Set.copyOf(owners);
    }

    private static void validateOwner(String ownerModId) {
        Objects.requireNonNull(ownerModId, "ownerModId");
        if (!OWNER_PATTERN.matcher(ownerModId).matches()) {
            throw new IllegalArgumentException(
                    "Invalid mod ID '" + ownerModId + "'; expected [a-z][a-z0-9_-]*");
        }
    }

    private static final class Holder {
        private static final SyrupConfigManager INSTANCE =
                new SyrupConfigManager(Platform.INSTANCE.configDirectory());
    }
}
