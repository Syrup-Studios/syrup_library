# Syrup Library

Syrup Library provides reusable, typed configuration for Syrup Studios mods. Its shared source
supports these Minecraft and loader targets:

- Minecraft 1.20.1: Fabric and Forge
- Minecraft 1.21.1: Fabric and NeoForge
- Minecraft 1.21.11: Fabric and NeoForge
- Minecraft 26.2: Fabric and NeoForge

## Maven coordinates

Each build uses this artifact version format:

```text
net.syrupstudios:syrup_library:0.2.0+<minecraft>-<loader>
```

For example, consumers can add the Minecraft 1.20.1 Fabric build to a Fabric Loom project:

```kotlin
repositories {
    maven("https://maven.syrupstudios.net/releases/")
}

dependencies {
    modImplementation("net.syrupstudios:syrup_library:0.2.0+1.20.1-fabric")
}
```

## Config screens

Syrup Library ships an in-game editor for the configs it registers. It opens through the Fabric
Mod Menu "Config" button and through the NeoForge mod options page.

### Registering a config owner

Use the new overload so the editor knows which mod owns each config:

```java
manager.register("example_mod", spec);
```

The old one-argument form still works; it uses the config ID as the owner:

```java
manager.register(spec); // owner falls back to spec.id()
```

If you already use your mod ID as the config ID, `register(spec)` is equivalent to
`register(spec.id(), spec)` and needs no change.

### Fabric Mod Menu

Mod Menu is an optional, compile-time-only integration. When Mod Menu is installed, Syrup Library
provides a config screen factory for every mod that owns registered configs, so the Config button
appears on the Mod Menu page automatically. When Mod Menu is absent, normal config loading is
unaffected and no Mod Menu classes are loaded.

### NeoForge

Syrup Library attaches each screen to its owning mod during client setup. Consumers do not need
loader-specific registration code. The registrar is client-only, so dedicated servers do not load
Minecraft client classes.

### Editing behavior

- A mod with a single config opens the editor directly; multiple configs show a file-selection page.
- Saving writes only the draft values you changed. Untouched valid values, unknown keys, and
  sections stay as they are, and comments attached to edited values are kept. If loading had to
  correct an invalid known value, the next real GUI save also writes that safe value back to disk.
- String lists use JSON5 array text, such as `["one", "two, three", ""]`. Commas, spaces, and
  empty strings inside elements are preserved.
- Values that require a restart keep the startup value effective until the game restarts. After
  saving such a value the editor shows a restart notice, and a later save works without a false
  conflict.
- Formatting can be normalized after a GUI save because the file is re-serialized.
- If the file changes outside the game while the editor is open, saving refuses to overwrite it and
  shows a reload button.

The editor changes the local JSON5 file. It does not send configuration changes to a remote server.

## Remote publishing

This part is more for me, since i know i will forget

The remote repository defaults to `https://maven.syrupstudios.net/releases/`. Set its credentials
through environment variables before running `publish`:

```shell
MAVEN_REPOSITORY_USERNAME=your-username \
MAVEN_REPOSITORY_PASSWORD=your-password \
./gradlew :1.20.1-fabric:publish
```

The equivalent Gradle properties are `mavenRepositoryUsername` and `mavenRepositoryPassword`.
You can override the repository with `MAVEN_REPOSITORY_URL` or `mavenRepositoryUrl`. Keep
credentials in the user-level Gradle properties file, not in this repository.

## CurseForge and Modrinth publishing

Set `publish.curseforge_project_id` and `publish.modrinth_project_id` in the root
`gradle.properties` file. Put the API tokens in `~/.gradle/gradle.properties`:

```properties
publish.curseforge_token=your-token
publish.modrinth_token=your-token
```

You can use the `CURSEFORGE_TOKEN` and `MODRINTH_TOKEN` environment variables instead.
Test all upload data without sending files:

```shell
./gradlew publishMods --no-parallel -Ppublish.dry_run=true
```

Publish all eight builds to both sites:

```shell
./gradlew publishMods --no-parallel
```

Use `publishCurseforge` or `publishModrinth` to publish to only one site. The release type and
changelog come from `publish.release_type` and the root `CHANGELOGS.md` file. Create or replace
`CHANGELOGS.md` before each release. Markdown is supported.
