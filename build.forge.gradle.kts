
plugins {
    id("net.neoforged.moddev.legacyforge")
    id("neoforge-mutex")
    id("me.modmuss50.mod-publish-plugin")
    `maven-publish`
}

val mcVersion = stonecutter.current.version
val forgeVersion = property("deps.forge_version") as String
val packFormat = (property("deps.pack_format") as String).toInt()
val requiredJava = when {
    stonecutter.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    stonecutter.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    stonecutter.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    stonecutter.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}
val json5Dependency = "de.marhali:json5-java:" + property("deps.json5")
val forgeMinecraftRange: String = stonecutter.properties["mod.forge_mc_range"]

version = "${property("mod.version")}+$mcVersion-forge"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

legacyForge {
    setVersion("$mcVersion-$forgeVersion")
    runs {
        create("client") {
            client()
            gameDirectory = rootProject.file("run")
        }
        create("server") {
            server()
            gameDirectory = rootProject.file("run")
        }
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
        "**/loaders/neoforge/**"
    )
}

java {
    withSourcesJar()
    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
}

tasks.processResources {
    val props = mapOf(
        "version" to project.property("mod.version"),
        "mc" to forgeMinecraftRange,
        "forge" to forgeVersion.substringBefore('.'),
        "packFormat" to packFormat,
        "modName" to project.property("mod.name"),
        "modId" to project.property("mod.id"),
        "modDescription" to project.property("mod.description"),
        "authors" to project.property("mod.authors"),
        "license" to project.property("mod.license")
    )
    inputs.properties(props)
    from(rootProject.file("src/main/templates")) {
        include("pack.mcmeta")
        expand(props)
    }
    filesMatching("META-INF/mods.toml") { expand(props) }
    exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds mod jars and copies results to `build/libs/{mod version}/`"
    inputs.property("version", project.property("mod.version"))
    from(tasks.named("jar"), tasks.named("sourcesJar"))
    into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = base.archivesName.get()
        }
    }
    repositories {
        maven {
            name = "syrupStudios"
            url = uri("https://maven.syrupstudios.net/releases/")
            credentials(PasswordCredentials::class)
        }
    }
}

val compatibleVersions = stonecutter.properties.rawOrNull("mod.mc_releases")?.asList()?.map { it.toString() }.orEmpty()
val outputFile = tasks.named<Jar>("jar").flatMap { it.archiveFile }
val changelogText = providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOG.md")).asText
val curseForgeToken = providers.environmentVariable("CURSEFORGE_TOKEN")
val modrinthToken = providers.environmentVariable("MODRINTH_TOKEN")

publishMods {
    file.set(outputFile)
    dryRun = curseForgeToken.isPresent.not() || modrinthToken.isPresent.not()
    version = project.version.toString()
    displayName = "${project.property("mod.name")} ${project.property("mod.version")} - Forge ${mcVersion}"
    changelog = changelogText
    type = BETA
    modLoaders.add(project.name.substringAfterLast('-'))
    curseforge {
        projectId = property("publish.curseforge").toString()
        accessToken = curseForgeToken
        compatibleVersions.forEach { minecraftVersions.add(it) }
        client = true
        server = true
    }
    modrinth {
        projectId = property("publish.modrinth").toString()
        accessToken = modrinthToken
        compatibleVersions.forEach { minecraftVersions.add(it) }
        environment = CLIENT_OR_SERVER
    }
}
