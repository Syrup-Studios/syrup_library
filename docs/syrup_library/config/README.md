---
title: Configs
description: Learn how to create, register, read, validate, and reload a JSON5 config with Syrup Library.
---

# Use configs in your mod

This guide explains how to use the Syrup Library config system. First, [add Syrup Library to your mod](../README.md#add-syrup-library).

A config specification defines the sections, values, defaults, and limits in one config file. This guide uses the term **specification** for this definition.

## Create a config specification

Put the config specification in one class. Each value method returns a value handle. A value handle is a Java object that supplies one config value.

This example creates `config/example_mod.json5`:

```java
package com.example.examplemod;

import net.syrupstudios.syruplibrary.config.ConfigSection;
import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RegisteredConfig;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import net.syrupstudios.syruplibrary.config.SyrupConfigManager;
import net.syrupstudios.syruplibrary.config.value.BooleanConfigValue;
import net.syrupstudios.syruplibrary.config.value.EnumConfigValue;
import net.syrupstudios.syruplibrary.config.value.IntConfigValue;
import net.syrupstudios.syruplibrary.config.value.StringConfigValue;
import net.syrupstudios.syruplibrary.config.value.StringListConfigValue;

import java.util.List;

public final class ModConfig {
    public enum LogLevel {
        QUIET,
        NORMAL,
        VERBOSE
    }

    private static final ConfigSpec SPEC = ConfigSpec.builder("example_mod")
            .header(
                    "Example Mod configuration.",
                    "Edit this file, then use the mod's reload command."
            )
            .build();

    private static final ConfigSection GAMEPLAY = SPEC.section(
            "gameplay",
            "Settings that change gameplay."
    );

    public static final BooleanConfigValue ENABLED = GAMEPLAY.booleanValue(
            "enabled",
            true,
            "Enable the main feature."
    );

    public static final IntConfigValue SEARCH_RADIUS = GAMEPLAY.intValue(
            "search_radius",
            16,
            1,
            128,
            "Maximum search radius in blocks."
    );

    public static final StringConfigValue CHANNEL = GAMEPLAY.validatedStringValue(
            "channel",
            "global",
            "Channel name used by the feature.",
            value -> value.matches("[a-z0-9_]+"),
            "Use lowercase letters, numbers, and underscores"
    );

    public static final StringListConfigValue BLOCKED_WORLDS = GAMEPLAY.stringListValue(
            "blocked_worlds",
            List.of("minecraft:the_end"),
            "Dimension IDs where the feature is disabled."
    );

    private static final ConfigSection ADVANCED = SPEC.section(
            "advanced",
            "Settings for server administrators."
    );

    public static final EnumConfigValue<LogLevel> LOG_LEVEL = ADVANCED.enumValue(
            "log_level",
            LogLevel.class,
            LogLevel.NORMAL,
            "Control how much information the mod writes to the log."
    );

    public static final StringConfigValue STORAGE_MODE = ADVANCED.stringValue(
            "storage_mode",
            "safe",
            "Storage system that the mod uses.",
            RestartRequirement.REQUIRED
    );

    public static final RegisteredConfig FILE =
            SyrupConfigManager.getInstance().register(SPEC);

    private ModConfig() {
    }

    /** Call this one time from the mod initializer. */
    public static void initialize() {
        // This method loads and registers the config specification.
    }
}
```

Syrup Library writes sections and values in their declaration order. The `register(SPEC)` call locks the specification. Declare all sections and values before this call.

Config IDs and keys have these rules:

| Item | Rule | Example |
| --- | --- | --- |
| Config ID | Start with a lowercase letter. Use lowercase letters, numbers, `_`, and `-`. Use a maximum of 64 characters. | `example_mod` |
| Section or value key | Start with a lowercase letter. Use lowercase letters, numbers, and `_`. | `search_radius` |

Do not put a path or a file extension in the config ID. Syrup Library uses this file path: `<config-directory>/<config-id>.json5`.

## Register the config

Call your config class one time from your mod initializer:

```java
package com.example.examplemod;

import net.fabricmc.api.ModInitializer;

public final class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ModConfig.initialize();
    }
}
```

If the file does not exist, the `register` method creates it. Then, the method loads the file. Register each config ID only one time.

The library creates this type of file from the example specification:

```json5
/*
 * Example Mod configuration.
 * Edit this file, then use the mod's reload command.
 */
{
  // Settings that change gameplay.
  gameplay: {
    /*
     * Enable the main feature.
     * Default: true
     */
    enabled: true,

    /*
     * Maximum search radius in blocks.
     * Default: 16 | Range: 1 ~ 128
     */
    search_radius: 16,

    /*
     * Channel name used by the feature.
     * Default: "global"
     */
    channel: "global",

    /*
     * Dimension IDs where the feature is disabled.
     * Default: ["minecraft:the_end"]
     */
    blocked_worlds: ["minecraft:the_end"]
  },

  // Settings for server administrators.
  advanced: {
    /*
     * Control how much information the mod writes to the log.
     * Default: "normal"
     */
    log_level: "normal",

    /*
     * Storage system that the mod uses.
     * Requires a server restart.
     * Default: "safe"
     */
    storage_mode: "safe"
  }
}
```

JSON5 supports comments, object keys without quotation marks, and commas after the last value.

## Read values

Call `get()` on a value handle. The method returns the declared Java type:

```java
if (ModConfig.ENABLED.get()) {
    int radius = ModConfig.SEARCH_RADIUS.get();
    String channel = ModConfig.CHANNEL.get();
    ModConfig.LogLevel level = ModConfig.LOG_LEVEL.get();
    List<String> blockedWorlds = ModConfig.BLOCKED_WORLDS.get();
}
```

You cannot change a string list that the library returns. Make a copy before you change the list.

### Read one fixed set of values

A snapshot is a fixed set of config values. A snapshot does not change after the library creates it.

During a reload, the library makes all new values active at the same time. Use a snapshot when one operation reads two or more related values:

```java
import net.syrupstudios.syruplibrary.config.ConfigSnapshot;

ConfigSnapshot config = ModConfig.FILE.snapshot();
boolean enabled = config.get(ModConfig.ENABLED);
int radius = config.get(ModConfig.SEARCH_RADIUS);
```

Use `get()` to read one independent value. Use `snapshot()` when all reads must use the same set of values.

## Reload the file

Syrup Library does not reload a config automatically. Call `reload()` from your command, file monitor, or other reload action:

```java
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssue;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigLoadResult;

ConfigLoadResult result = ModConfig.FILE.reload();

if (!result.successful()) {
    // The previous valid config is still active.
}

for (ConfigIssue issue : result.issues()) {
    System.out.println(issue.severity() + " " + issue.path() + ": " + issue.message());
}
```

Use `initialResult()` to get the result of the first load:

```java
ConfigLoadResult firstLoad = ModConfig.FILE.initialResult();
```

A successful load result can contain warnings. Use `hasWarnings()` if your command must report changed or rejected values.

## Restart-only values

By default, values use `RestartRequirement.NONE`. A reload makes these values active immediately.

Use `RestartRequirement.REQUIRED` for a value that must stay unchanged until the next game start:

```java
public static final StringConfigValue STORAGE_MODE = ADVANCED.stringValue(
        "storage_mode",
        "safe",
        "Storage system that the mod uses.",
        RestartRequirement.REQUIRED
);
```

For a value that requires a restart:

- `get()` returns the value that is active now.
- `configuredValue()` returns the most recent valid value from the file.
- `startupValue()` returns the value that was loaded at game start.

After a reload, `get()` and `startupValue()` stay unchanged. The configured value becomes active after a restart.

## Available value types

You can add values to a `ConfigSpec` or to a `ConfigSection`.

| Method | Java type | Notes |
| --- | --- | --- |
| `booleanValue` | `Boolean` | Accepts `true` or `false`. |
| `intValue` | `Integer` | Requires an inclusive minimum and maximum. |
| `longValue` | `Long` | Requires an inclusive minimum and maximum. |
| `doubleValue` | `Double` | Requires a default, minimum, and maximum. These numbers cannot be infinity or `NaN`. |
| `stringValue` | `String` | Accepts any string. |
| `validatedStringValue` | `String` | Uses your validation function and message. |
| `stringListValue` | `List<String>` | Returns a list that you cannot change. |
| `enumValue` | Your enum type | Stores enum names as lowercase strings. It accepts uppercase and lowercase input. |

For each value method, you can also specify a `RestartRequirement`.

A default number must be in its specified range. If the default is not valid, the library throws an exception when it creates the specification.

## Invalid and missing data

Syrup Library treats a value error differently from a file error.

| Input problem | Result |
| --- | --- |
| A value is missing | The library uses the default. It adds the missing value to the file. |
| A number is outside its range | The library changes the number to the nearest limit. It also adds a warning. |
| A value has the wrong type | The library uses the default. It also adds a warning. |
| A string does not pass its validation function | The library uses the default. It also adds a warning. |
| An enum name is not known | The library uses the default. It also adds a warning. |
| A key is not in the specification | The library ignores the key. It also adds an information message. |
| The JSON5 file does not load | The reload fails. The last valid values stay active. |

The library uses each default as a safe replacement value. It does not return `null` for a declared value.

## Use more than one config file

Create and register a different `ConfigSpec` for each file. Give each specification a unique config ID:

```java
ConfigSpec clientSpec = ConfigSpec.builder("example_mod_client").build();
ConfigSpec serverSpec = ConfigSpec.builder("example_mod_server").build();

RegisteredConfig clientConfig = SyrupConfigManager.getInstance().register(clientSpec);
RegisteredConfig serverConfig = SyrupConfigManager.getInstance().register(serverSpec);
```

This code creates `example_mod_client.json5` and `example_mod_server.json5`. The files are in the config directory of the active loader.
