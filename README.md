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

## Config presentation API

The existing value methods remain supported. Mods can use the fluent entry API when they need
localized text, specialized controls, conditions, groups, scope metadata, or other screen behavior.

```java
ConfigSpec spec = ConfigSpec.builder("example_client")
        .translationPrefix("example_mod.config.client")
        .header("Example Mod client configuration")
        .build();

ConfigGroup general = spec.group("general");
ConfigGroup advancedGroup = spec.group("advanced");

BooleanConfigValue advanced = spec.booleanEntry("advanced", false)
        .description("Shows advanced settings.")
        .group(general)
        .build();

IntConfigValue volume = spec.intEntry("volume", 80)
        .range(0, 100)
        .description("Master volume from 0 to 100.")
        .group(general)
        .editor(ConfigEditor.slider(1))
        .build();

StringConfigValue backend = spec.stringEntry("backend", "automatic")
        .description("The rendering backend.")
        .group(advancedGroup)
        .visibleWhen(ConfigCondition.isTrue(advanced))
        .build();

spec.info("advanced_warning")
        .text("These settings can reduce performance.")
        .style(ConfigInfoStyle.WARNING)
        .group(advancedGroup)
        .visibleWhen(ConfigCondition.isTrue(advanced))
        .build();
```

Groups and information rows control only the screen. They do not create JSON5 fields or change
config paths.

### Localized config text

When a spec has a translation prefix, the editor checks these keys:

```text
<prefix>.title
<prefix>.<path>.name
<prefix>.<path>.description
<prefix>.group.<group-id>.name
<prefix>.group.<group-id>.description
<prefix>.info.<info-id>.text
<prefix>.<enum-path>.option.<serialized-value>
```

Example:

```json
{
  "example_mod.config.client.title": "Client Settings",
  "example_mod.config.client.volume.name": "Master Volume",
  "example_mod.config.client.volume.description": "Controls all mod sounds.",
  "example_mod.config.client.group.advanced.name": "Advanced"
}
```

Missing translations use the current readable-name and literal-description fallbacks. The literal
description still becomes a JSON5 comment. Changing the game language does not change the file.

### Built-in editors

Use `editor(...)` on a fluent entry builder:

```java
spec.doubleEntry("scale", 1.0)
        .range(0.5, 2.0)
        .editor(ConfigEditor.slider(0.1))
        .build();

spec.stringEntry("accent", "#55AAFF")
        .editor(ConfigEditor.color())
        .build();

spec.stringEntry("notes", "")
        .editor(ConfigEditor.multiline(6))
        .build();

spec.stringEntry("data_file", "example.json")
        .editor(ConfigEditor.path(ConfigPathMode.FILE, Set.of("json")))
        .build();
```

String lists automatically use a dedicated editor with add, edit, remove, and reorder actions.
List builders can also set size and item-validation rules.

The path editor stays inside the loader config directory and stores relative paths. The first
version does not accept absolute paths.

### Conditions

Conditions use typed value references and read the current edit-session drafts:

```java
.visibleWhen(ConfigCondition.equals(mode, Mode.ADVANCED))
.enabledWhen(ConfigCondition.modLoaded("optional_mod"))
```

Available composition methods include `oneOf`, `allOf`, `anyOf`, and `not`. Registration rejects
cross-spec references, self-dependencies, and condition cycles.

### Scope and edit policy

Values default to `ConfigScope.COMMON` and `ConfigEditPolicy.EDITABLE`.

```java
.scope(ConfigScope.CLIENT)
.scope(ConfigScope.SERVER)
.editPolicy(ConfigEditPolicy.READ_ONLY)
.editPolicy(ConfigEditPolicy.HIDDEN)
```

Server-scoped values are read-only in the local client editor by default. Scope metadata does not
provide network synchronization or remote server editing.

### Enum presentation

Enum serialization remains the lowercase constant name. A builder can change only the screen order,
labels, and offered choices:

```java
spec.enumEntry("quality", Quality.class, Quality.NORMAL)
        .optionOrder(Quality.LOW, Quality.NORMAL, Quality.HIGH)
        .optionLabel(Quality.HIGH, "example_mod.config.client.quality.option.high")
        .enabledOptions(value -> value != Quality.HIGH || highQualitySupported())
        .build();
```

### Custom client editors

Register custom editors during client setup:

```java
ConfigEditorRegistry.register(
        "example_mod:special",
        context -> new SpecialConfigEditor(context)
);
```

Declare the editor with `ConfigEditor.custom("example_mod:special")`. The registry freezes before
the first config screen opens. A missing editor logs a warning and uses the automatic editor. Custom
editors must send changes through the supplied `ConfigEditorContext`.

The full behavior and compatibility requirements are in
[`docs/CONFIG_EDITOR_DESIGN.md`](docs/CONFIG_EDITOR_DESIGN.md).

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
