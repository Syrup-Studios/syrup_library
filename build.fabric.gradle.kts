import net.fabricmc.loom.api.LoomGradleExtensionAPI
import me.modmuss50.mpp.ReleaseType
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.17.14" apply false
    id("net.fabricmc.fabric-loom") version "1.17.14" apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
    id("maven-publish")
}

val remappedMinecraft = stonecutter.eval(stonecutter.current.version, "<26")
val minecraftVersion = stonecutter.current.version
val targetJavaVersion = (property("deps.java_version") as String).toInt()
val requiredJava = JavaVersion.toVersion(targetJavaVersion)
val fabricMinecraftRange: String = stonecutter.properties["mod.fabric_mc_range"]

apply(plugin = if (remappedMinecraft) "net.fabricmc.fabric-loom-remap" else "net.fabricmc.fabric-loom")

version = "${property("mod.version")}+$minecraftVersion-fabric"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

val loomExtension = extensions.getByType<LoomGradleExtensionAPI>()

repositories {
    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
}

dependencies {
    add("minecraft", "com.mojang:minecraft:$minecraftVersion")
    if (remappedMinecraft) add("mappings", loomExtension.officialMojangMappings())
    val json5Dependency = "de.marhali:json5-java:" + property("deps.json5")
    implementation(json5Dependency)
    add("include", json5Dependency)

    val modConfiguration = if (remappedMinecraft) "modImplementation" else "implementation"
    add(modConfiguration, "net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    val modMenu = "com.terraformersmc:modmenu:${property("deps.mod_menu")}"
    if (remappedMinecraft) {
        add("modCompileOnly", modMenu)
        add("modLocalRuntime", modMenu)
    } else {
        add("compileOnly", modMenu)
        add("localRuntime", modMenu)
    }
}

loomExtension.apply {
    enableTransitiveAccessWideners.set(false)
    fabricModJsonPath.set(rootProject.file("src/main/resources/fabric.mod.json"))
    if (remappedMinecraft) {
        decompilerOptions.named("vineflower") {
            options.put("mark-corresponding-synthetics", "1")
        }
    }
    runConfigs.configureEach { runDir = "run" }
}

sourceSets.main {
    java.exclude(
        "**/loaders/forge/**",
        "**/loaders/neoforge/**"
    )
}

java {
    withSourcesJar()
    withJavadocJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "mc" to fabricMinecraftRange,
        "modName" to project.property("mod.name"),
        "modId" to project.property("mod.id"),
        "modDescription" to project.property("mod.description"),
        "authors" to project.property("mod.authors"),
        "license" to project.property("mod.license"),
        "fl" to project.property("deps.fabric_loader"),
        "java" to targetJavaVersion
    )

    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
    exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    val productionJar = if (remappedMinecraft) "remapJar" else "jar"
    val sourceJar = if (remappedMinecraft) "remapSourcesJar" else "sourcesJar"
    from(tasks.named(productionJar), tasks.named(sourceJar))
    into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    dependsOn("build")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

val mavenRepositoryUrl = providers.gradleProperty("mavenRepositoryUrl")
    .orElse(providers.environmentVariable("MAVEN_REPOSITORY_URL"))
    .orElse("https://maven.syrupstudios.net/releases/")
val mavenRepositoryUsername = providers.gradleProperty("mavenRepositoryUsername")
    .orElse(providers.environmentVariable("MAVEN_REPOSITORY_USERNAME"))
val mavenRepositoryPassword = providers.gradleProperty("mavenRepositoryPassword")
    .orElse(providers.environmentVariable("MAVEN_REPOSITORY_PASSWORD"))
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
val outputFile = tasks.named<AbstractArchiveTask>(if (remappedMinecraft) "remapJar" else "jar").flatMap { it.archiveFile }
val changelogText = providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOGS.md")).asText
val releaseTypeName = providers.gradleProperty("publish.release_type")
    .orElse(providers.provider { project.property("publish.release_type").toString() }).get().lowercase()
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
    tasks.named(taskName).configure { doFirst { logger.lifecycle("Publishing ${project.version} for Minecraft $minecraftVersion (fabric)") } }
}
