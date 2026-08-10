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
net.syrupstudios:syrup_library:0.1.1+<minecraft>-<loader>
```

For example, consumers can add the Minecraft 1.20.1 Fabric build to a Fabric Loom project:

```kotlin
repositories {
    maven("https://maven.syrupstudios.net/releases/")
}

dependencies {
    modImplementation("net.syrupstudios:syrup_library:0.1.1+1.20.1-fabric")
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
