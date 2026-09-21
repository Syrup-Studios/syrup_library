plugins {
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
    `maven-publish`
}

val minecraftVersion = stonecutter.current.version
val requiredJava = when {
    stonecutter.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    stonecutter.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    stonecutter.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    else -> JavaVersion.VERSION_1_8
}
val fabricMinecraftRange: String = stonecutter.properties["mod.fabric_mc_range"]

version = "${property("mod.version")}+$minecraftVersion-fabric"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

repositories {
    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    loomx.applyMojangMappings()
    val json5Dependency = "de.marhali:json5-java:" + property("deps.json5")
    implementation(json5Dependency)
    add("include", json5Dependency)

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    val modMenu = "com.terraformersmc:modmenu:${property("deps.mod_menu")}"
    modCompileOnly(modMenu)
    modLocalRuntime(modMenu)
}

loom {
    enableTransitiveAccessWideners.set(false)
    fabricModJsonPath.set(rootProject.file("src/main/resources/fabric.mod.json"))
    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }
    runConfigs.configureEach { runDirectory = rootProject.file("run") }
}

sourceSets.main {
    java.exclude(
        "**/loaders/forge/**",
        "**/loaders/neoforge/**"
    )
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
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
        "java" to requiredJava.majorVersion
    )

    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
    exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(requiredJava.majorVersion.toInt())
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

val compatibleVersions = stonecutter.properties.rawOrNull("mod.mc_releases")?.asList()?.map { it.toString() } ?: listOf(minecraftVersion)
val outputFile = loomx.modJar.flatMap { it.archiveFile }
val changelogText = providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOGS.md")).asText
val curseForgeToken = providers.environmentVariable("CURSEFORGE_TOKEN")
val modrinthToken = providers.environmentVariable("MODRINTH_TOKEN")

publishMods {
    file.set(outputFile)
    dryRun = curseForgeToken.isPresent.not() || modrinthToken.isPresent.not()
    version = project.version.toString()
    displayName = "${project.property("mod.name")} ${project.version}"
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
