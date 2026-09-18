---
title: Configs
description: Learn how to create, register, read, validate, and reload a JSON5 config with Syrup Library.
---

# Use configs in your mod

This guide explains how to use the Syrup Library config system. First, [add Syrup Library to your mod](../README.md#add-syrup-library).

This guide documents the 0.3.0 generic config API. The typed wrapper API was
removed in 0.3.0. Existing consumers must stay on 0.2.0 or migrate and rebuild.

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
import net.syrupstudios.syruplibrary.config.value.ConfigType;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;

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

    public static final ConfigValue<Boolean> ENABLED = GAMEPLAY.bool(
            "enabled",
            true,
            "Enable the main feature."
    );

    public static final ConfigValue<Integer> SEARCH_RADIUS = GAMEPLAY.integer(
            "search_radius",
            16,
            1,
            128,
            "Maximum search radius in blocks."
    );

    public static final ConfigValue<String> CHANNEL = GAMEPLAY.string(
            "channel",
            "global",
            "Channel name used by the feature."
    );

    public static final ConfigValue<List<String>> BLOCKED_WORLDS = GAMEPLAY.stringList(
            "blocked_worlds",
            List.of("minecraft:the_end"),
            "Dimension IDs where the feature is disabled."
    );

    private static final ConfigSection ADVANCED = SPEC.section(
            "advanced",
            "Settings for server administrators."
    );

    public static final ConfigValue<LogLevel> LOG_LEVEL = ADVANCED.enumValue(
            "log_level", LogLevel.NORMAL,
            "Control how much information the mod writes to the log."
    );

    public static final ConfigValue<String> STORAGE_MODE = ADVANCED.value(
            "storage_mode",
            ConfigType.STRING,
            "safe",
            "Storage system that the mod uses.",
            RestartRequirement.REQUIRED,
            List.of()
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

## Update and save values

Use a value handle to validate and update one value. Use `updateAll()` when several values must change together. The whole batch stays unchanged when one value is invalid.

```java
import java.util.Map;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigUpdateResult;

ConfigUpdateResult update = ModConfig.FILE.updateAndSave(ModConfig.SEARCH_RADIUS, 32);
if (update.successful()) {
    // The new value is active and saved.
}

ConfigUpdateResult batch = ModConfig.FILE.updateAll(Map.of(
        ModConfig.ENABLED, false,
        ModConfig.SEARCH_RADIUS, 8
));
```

Updates use the declared type, range, string validation, enum values, and string-list member checks. `update()` and `updateAll()` change memory only; use `updateAndSave()` or `updateAndSaveAll()` to save before publishing. A failed save leaves both the previous file and the active state unchanged. `save()` saves already-published configured values. Saves regenerate the schema output, including schema comments; custom file comments and unknown keys are removed. If the file system does not support atomic replacement, the save fails safely and leaves the previous file intact.

## Restart-only values

By default, values use `RestartRequirement.NONE`. An update or reload makes these values active immediately.

Use `RestartRequirement.REQUIRED` for a value that must stay unchanged until the next game start:

```java
public static final ConfigValue<String> STORAGE_MODE = ADVANCED.value(
        "storage_mode",
        ConfigType.STRING,
        "safe",
        "Storage system that the mod uses.",
        RestartRequirement.REQUIRED,
        List.of()
);
```

For a value that requires a restart:

- `get()` returns the value that is active now.
- `configuredValue()` returns the most recent valid configured value, including unsaved programmatic updates.
- `startupValue()` returns the value that was loaded at game start.

After a reload or programmatic update, `get()` and `startupValue()` stay unchanged. The configured value becomes active after a restart.

## Available value types

The library provides generic convenience factories. These factories use the
same schema and validation rules as the full `value(...)` factory:

```java
ConfigValue<Boolean> enabled = gameplay.bool("enabled", true, "Enable the feature.");
ConfigValue<Integer> radius = gameplay.integer("radius", 16, 1, 128, "Search radius.");
ConfigValue<String> mode = gameplay.string("mode", "safe", "Storage mode.");
ConfigValue<List<String>> blocked = gameplay.stringList(
        "blocked_worlds", List.of(), "Dimensions where the feature is disabled.");
ConfigValue<LogLevel> level = advanced.enumValue(
        "log_level", LogLevel.NORMAL, "Control log output.");
```

Use the full generic factory when a value needs restart metadata or
custom constraints. A custom `ConfigType<T>` supplies a JSON5 decoder, encoder,
and normalizer. The normalizer must return an independent value for mutable
types. Syrup calls it for defaults, decoded values, updates, and snapshots.

You can add values to a `ConfigSpec` or to a `ConfigSection`.

| Method | Java type | Notes |
| --- | --- | --- |
| `bool` | `Boolean` | Accepts `true` or `false`. |
| `integer` | `Integer` | Requires an inclusive minimum and maximum. |
| `longValue` | `Long` | Accepts a long value. |
| `doubleValue` | `Double` | Accepts a finite double value. |
| `string` | `String` | Accepts any string. |
| `stringList` | `List<String>` | Returns an immutable list. |
| `enumValue` | Your enum type | Stores enum names as lowercase strings. It accepts uppercase and lowercase input. |
| `value` | Any declared type | Accepts restart metadata and constraints. |

A default number must be in its specified range. If the default is not valid, the library throws an exception when it creates the specification.

## Invalid and missing data

Syrup Library treats a value error differently from a file error.

| Input problem | Result |
| --- | --- |
| A value is missing | The library uses the default. It adds the missing value to the file. |
| A present number is outside its range | The candidate is rejected. The previous configured and active values stay unchanged. It adds an error. |
| A present value has the wrong type | The candidate is rejected. The previous configured and active values stay unchanged. It adds an error. |
| A present string does not pass its validation function | The candidate is rejected. The previous configured and active values stay unchanged. It adds an error. |
| A present enum name is not known | The candidate is rejected. The previous configured and active values stay unchanged. It adds an error. |
| A key is not in the specification | The library ignores the key. It also adds an information message. |
| The JSON5 file does not load | The reload fails. The last valid values stay active. |

On initial load, a present invalid value leaves the declared default active. On a
later reload, a present invalid value leaves the previous valid configured value
active. Missing values still use the declared default and are added to the
file. A candidate is never partly applied: if one value is invalid, the whole
candidate is rejected. The library does not return `null` for a declared value.

## Use more than one config file

Create and register a different `ConfigSpec` for each file. Give each specification a unique config ID:

```java
ConfigSpec clientSpec = ConfigSpec.builder("example_mod_client")
        .owner("example_mod").build();
ConfigSpec serverSpec = ConfigSpec.builder("example_mod_server")
        .owner("example_mod").build();

RegisteredConfig clientConfig = SyrupConfigManager.getInstance().register(clientSpec);
RegisteredConfig serverConfig = SyrupConfigManager.getInstance().register(serverSpec);
```

This code creates `example_mod_client.json5` and `example_mod_server.json5`. The
files are in the config directory of the active loader.

## GUI integration

The optional client screen edits every registered value with the same typed
validation and save API. Open a config from another client screen with:

```java
import net.minecraft.client.Minecraft;
import net.syrupstudios.syruplibrary.client.config.SyrupConfigScreen;

Minecraft.getInstance().setScreen(SyrupConfigScreen.create(parent, "example_mod"));
```

On Minecraft 26.2, use `Minecraft.getInstance().setScreenAndShow(...)` for
the returned screen.

`create(parent)` opens a selection screen for every registered config.
`create(parent, "example_mod")` opens one config by ID and returns `null` when
the ID is not registered. The public constructor accepts a `RegisteredConfig`
when a caller already holds that handle.

The screen keeps edits local until `updateAndSaveAll` succeeds. Save keeps the
editor open and displays `Saved` plus any restart status. Done returns to the
parent after a successful save. Cancel discards edits since the last successful
save. Save errors and validation errors remain on the screen.
Restart-required values show their normal configured/effective behavior after
save: the configured value changes, while the effective value waits for restart.

Use Previous setting and Next setting to move through settings. Select
Description and limits to read the full field path, description, and limits.
Select the status button to read validation, save, and restart messages.
Booleans and enums use native controls. Numeric
and string values use text fields, with multiline text in the native multiline
editor. String lists provide item add and remove controls.

Syrup Library registers isolated config factories for the owning mod on Fabric,
Forge, and NeoForge. A consuming mod does not need to add a second bridge. The
default owner is the config ID, so a spec named `example_mod` appears under
that mod. For a different filename, set the owner explicitly:

```java
ConfigSpec.builder("example_mod_client")
        .owner("example_mod").build();
```

Register every config spec before loader setup completes. A mod with multiple
specs receives one config button; `SyrupConfigScreen.createForMod(parent,
"example_mod")` opens the only config directly or shows a selector containing
only that mod's configs. The existing `create(parent)` selector remains
available for a deliberate library-wide diagnostic screen. Do not call client
classes from a dedicated-server entrypoint.

Supported targets are Fabric 1.20.1, 1.21.1, 1.21.11, and 26.2; Forge
1.20.1; and NeoForge 1.21.1, 1.21.11, and 26.2. Fabric's optional ModMenu
development versions are 7.2.2, 11.0.3, 17.0.0, and 20.0.1 for those Fabric
targets. ModMenu is compile-only and is never bundled.
