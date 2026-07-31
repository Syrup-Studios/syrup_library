# Syrup Library

Syrup Library provides reusable, typed configuration for Syrup Studios mods. Its shared source is
structured for Fabric, Forge, and NeoForge builds.

## Maven coordinates

The current Fabric artifact is published as:

```text
net.syrupstudios:syrup_library:0.1.1+1.20.1-fabric
```

Consumers can then add it to a Fabric Loom project:

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
