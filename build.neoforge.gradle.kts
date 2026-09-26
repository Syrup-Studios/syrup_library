
plugins {
    id("net.neoforged.moddev")
    id("neoforge-mutex")
    id("me.modmuss50.mod-publish-plugin")
    `maven-publish`
}

val minecraftVersion = stonecutter.current.version
val neoForgeVersion = property("deps.neoforge_version") as String
val requiredJava = when {
    stonecutter.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    stonecutter.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    stonecutter.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    stonecutter.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}
val json5Dependency = "de.marhali:json5-java:" + property("deps.json5")
val neoForgeMinecraftRange: String = stonecutter.properties["mod.neoforge_mc_range"]

version = "${property("mod.version")}+$minecraftVersion-neoforge"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

neoForge {
    version = neoForgeVersion
    runs {
        create("client") { client(); gameDirectory = rootProject.file("run") }
        create("server") { server(); gameDirectory = rootProject.file("run") }
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
val curseForgeToken = providers.gradleProperty("publish.curseforge_token")
    .orElse(providers.gradleProperty("CURSEFORGE_TOKEN"))
    .orElse(providers.environmentVariable("CURSEFORGE_TOKEN"))
val modrinthToken = providers.gradleProperty("publish.modrinth_token")
    .orElse(providers.gradleProperty("MODRINTH_TOKEN"))
    .orElse(providers.environmentVariable("MODRINTH_TOKEN"))

publishMods {
    file.set(outputFile)
    dryRun = curseForgeToken.isPresent.not() || modrinthToken.isPresent.not()
    version = project.version.toString()
    displayName = "${project.property("mod.name")} ${project.property("mod.version")} - Neoforge ${minecraftVersion}"
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
