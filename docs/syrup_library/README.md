---
title: Syrup Library
description: Add Syrup Library to your mod and learn about its config system.
---

# Syrup Library

Syrup Library supplies a JSON5 config system for mods. Each config value has a specified Java data type.

The config system can:

- Create a config file from a specification.
- Add comments to a new config file.
- Load and validate values.
- Add missing values to an existing file.
- Reload values while the game is open.
- Keep restart-required values unchanged until the next game start.
- Keep the last valid values active if a file does not load.

The same config code works with all supported mod loaders.

## Add Syrup Library

For Fabric and Minecraft 1.20.1, use version `0.1.1+1.20.1-fabric`.

Add the Syrup Studios Maven repository and the dependency to your `build.gradle.kts` file:

```kotlin
repositories {
    maven("https://maven.syrupstudios.net/releases/")
}

dependencies {
    modImplementation("net.syrupstudios:syrup_library:0.1.1+1.20.1-fabric")
}
```

Also add Syrup Library to the `depends` object in `fabric.mod.json`:

```json
{
  "depends": {
    "fabricloader": ">=0.19.2",
    "minecraft": "1.20.1",
    "java": ">=17",
    "fabric-api": "*",
    "syrup_library": ">=0.1.1"
  }
}
```

Users must install Syrup Library with your mod. If your build includes the library, users do not need a separate copy.

## Config documentation

Use the [config guide](config/README.md) to create and use configs in your mod.
