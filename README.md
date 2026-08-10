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
