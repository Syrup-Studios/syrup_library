import me.modmuss50.mpp.ReleaseType
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
    id("maven-publish")
}

val minecraftVersion = stonecutter.current.version
val neoForgeVersion = property("deps.neoforge_version") as String
val targetJavaVersion = (property("deps.java_version") as String).toInt()
val json5Dependency = "de.marhali:json5-java:" + property("deps.json5")
val neoForgeMinecraftRange: String = stonecutter.properties["mod.neoforge_mc_range"]

version = "${property("mod.version")}+$minecraftVersion-neoforge"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

neoForge {
    version = neoForgeVersion
    runs {
        create("client") { client(); gameDirectory = project.file("run") }
    }
    mods.create(property("mod.id") as String) { sourceSet(sourceSets.main.get()) }
}

dependencies {
    implementation(json5Dependency)
    jarJar(json5Dependency)
}

sourceSets.main {
    java.exclude(
        "**/loaders/fabric/**",
        "**/loaders/forge/**"
    )
}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    sourceCompatibility = JavaVersion.toVersion(targetJavaVersion)
    targetCompatibility = JavaVersion.toVersion(targetJavaVersion)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "mc" to neoForgeMinecraftRange,
        "modName" to project.property("mod.name"),
        "modId" to project.property("mod.id"),
        "modDescription" to project.property("mod.description"),
        "authors" to project.property("mod.authors"),
        "license" to project.property("mod.license")
    )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
    exclude("fabric.mod.json", "META-INF/mods.toml")
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.named("jar"), tasks.named("sourcesJar"))
    into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    dependsOn("build")
}

val mavenRepositoryUrl = providers.gradleProperty("mavenRepositoryUrl").orElse(providers.environmentVariable("MAVEN_REPOSITORY_URL")).orElse("https://maven.syrupstudios.net/releases/")
val mavenRepositoryUsername = providers.gradleProperty("mavenRepositoryUsername").orElse(providers.environmentVariable("MAVEN_REPOSITORY_USERNAME"))
val mavenRepositoryPassword = providers.gradleProperty("mavenRepositoryPassword").orElse(providers.environmentVariable("MAVEN_REPOSITORY_PASSWORD"))
val archiveName = extensions.getByType<BasePluginExtension>().archivesName.get()
val javaComponent = components["java"]

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(javaComponent)
            artifactId = archiveName
            pom {
                name.set("${project.property("mod.name")} ($archiveName)")
                description.set(project.property("mod.description") as String)
                url.set("https://github.com/Syrup-Studios/syrup_library")
                licenses {
                    license {
                        name.set(project.property("mod.license") as String)
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("SodaSyrup")
                        name.set(project.property("mod.authors") as String)
                        organization.set("Syrup Studios")
                        organizationUrl.set("https://github.com/Syrup-Studios")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/Syrup-Studios/syrup_library.git")
                    developerConnection.set("scm:git:ssh://git@github.com/Syrup-Studios/syrup_library.git")
                    url.set("https://github.com/Syrup-Studios/syrup_library")
                }
                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/Syrup-Studios/syrup_library/issues")
                }
            }
        }
    }
    repositories {
        maven {
            name = "syrupStudios"
            url = uri(mavenRepositoryUrl.get())
            if (mavenRepositoryUsername.isPresent || mavenRepositoryPassword.isPresent) {
                credentials {
                    username = mavenRepositoryUsername.orNull
                    password = mavenRepositoryPassword.orNull
                }
                authentication { create<BasicAuthentication>("basic") }
            }
        }
    }
}

val compatibleVersions = stonecutter.properties.rawOrNull("mod.mc_releases")?.asList()?.map { it.toString() } ?: listOf(minecraftVersion)
val outputFile = tasks.named<AbstractArchiveTask>("jar").flatMap { it.archiveFile }
val changelogText = providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOGS.md")).asText
val releaseTypeName = providers.gradleProperty("publish.release_type").orElse(providers.provider { project.property("publish.release_type").toString() }).get().lowercase()
val curseForgeToken = providers.gradleProperty("publish.curseforge_token").orElse(providers.environmentVariable("CURSEFORGE_TOKEN"))
val modrinthToken = providers.gradleProperty("publish.modrinth_token").orElse(providers.environmentVariable("MODRINTH_TOKEN"))

publishMods {
    file.set(outputFile)
    dryRun = providers.gradleProperty("publish.dry_run").map { it.toBoolean() }.orElse(false)
    version = project.version.toString()
    displayName = "${project.property("mod.name")} ${project.version}"
    changelog = changelogText
    type = when (releaseTypeName) {
        "stable" -> ReleaseType.STABLE
        "beta" -> ReleaseType.BETA
        "alpha" -> ReleaseType.ALPHA
        else -> throw GradleException("publish.release_type must be stable, beta, or alpha")
    }
    modLoaders.add(project.name.substringAfterLast('-'))
    curseforge {
        projectId = providers.gradleProperty("publish.curseforge_project_id").orElse(providers.provider { project.property("publish.curseforge_project_id").toString() })
        accessToken = curseForgeToken
        compatibleVersions.forEach { minecraftVersions.add(it) }
        client = true
        server = true
    }
    modrinth {
        projectId = providers.gradleProperty("publish.modrinth_project_id").orElse(providers.provider { project.property("publish.modrinth_project_id").toString() })
        accessToken = modrinthToken
        compatibleVersions.forEach { minecraftVersions.add(it) }
    }
}

listOf("publishCurseforge", "publishModrinth").forEach { taskName ->
    tasks.named(taskName).configure { doFirst { logger.lifecycle("Publishing ${project.version} for Minecraft $minecraftVersion (neoforge)") } }
}
