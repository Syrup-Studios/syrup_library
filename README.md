# Syrup Library

Shared, typed configuration code for Syrup Studios mods.

See the [setup and config guide](docs/syrup_library/README.md) for integration details.

Stonecutter metadata and version dependencies are defined in `stonecutter.properties.yaml`.
`gradle.properties` only contains shared Gradle runtime settings.

Supported Minecraft and loader targets:

- Minecraft 1.20.1: Fabric and Forge
- Minecraft 1.21.1: Fabric and NeoForge
- Minecraft 1.21.11: Fabric and NeoForge
- Minecraft 26.2: Fabric and NeoForge
- Minecraft 26.3: Fabric and NeoForge

## Maven coordinates

Version format:

```text
net.syrupstudios:syrup_library:0.3.0+<minecraft>-<loader>
```

Add the Minecraft 1.20.1 Fabric build to a Fabric Loom project:

```kotlin
repositories {
    maven("https://maven.syrupstudios.net/releases/")
}

dependencies {
    modImplementation("net.syrupstudios:syrup_library:0.3.0+1.20.1-fabric")
}
```

## Remote publishing

Publishing notes for future me.

The Maven repository is `https://maven.syrupstudios.net/releases/`. Set credentials before
running `publish`:

```shell
ORG_GRADLE_PROJECT_syrupStudiosUsername=your-username \
ORG_GRADLE_PROJECT_syrupStudiosPassword=your-password \
./gradlew :1.20.1-fabric:publish
```

The native Gradle credential names are `syrupStudiosUsername` and `syrupStudiosPassword`.
Put them in the user-level Gradle properties file, or provide
`ORG_GRADLE_PROJECT_syrupStudiosUsername` and `ORG_GRADLE_PROJECT_syrupStudiosPassword`.

## CurseForge and Modrinth publishing

Project IDs are in `stonecutter.properties.yaml`. Provide API tokens with environment variables:

```shell
CURSEFORGE_TOKEN=your-token \
MODRINTH_TOKEN=your-token \
./gradlew publishMods --no-parallel
```

Both tokens are required for a real upload. When either token is missing, publishing runs in
dry-run mode automatically:

```shell
env -u CURSEFORGE_TOKEN -u MODRINTH_TOKEN ./gradlew publishMods --no-parallel
```

The release type is beta. The changelog comes from the root `CHANGELOGS.md`; the file must exist
before a publish task runs.
