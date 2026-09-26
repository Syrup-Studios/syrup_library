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
    stonecutter.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}
val fabricMinecraftRange: String = stonecutter.properties["mod.fabric_mc_range"]

version = "${property("mod.version")}+$minecraftVersion-fabric"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://maven.terraformersmc.com/", "Terraformers", "com.terraformersmc")
    strictMaven("https://repo.maven.apache.org/maven2/", "Maven Central", "me.lucko")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    loomx.applyMojangMappings()
    val json5Dependency = "de.marhali:json5-java:" + property("deps.json5")
    implementation(json5Dependency)
    add("include", json5Dependency)

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    findProperty("deps.lucko_permission_api")?.let {
        val permissionApi = "me.lucko:fabric-permissions-api:$it"
        modImplementation(permissionApi)
        include(permissionApi)
    }
    // Runtime variant exposes Fabric API types referenced by widened Minecraft signatures.
    val modMenu = "com.terraformersmc:modmenu:${property("deps.mod_menu")}"
    modCompileOnly(modMenu) {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
        }
    }
    modLocalRuntime(modMenu)
}

loom {
    fabricModJsonPath.set(rootProject.file("src/main/resources/fabric.mod.json"))
    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }
    runConfigs.configureEach {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run")
        jvmArguments.add("-Dmixin.debug.export=true")
    }
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
        "version" to project.property("mod.version"),
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
    description = "Builds mod jars and copies results to `build/libs/{mod version}/`"
    inputs.property("version", project.property("mod.version"))
    from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
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
val outputFile = loomx.modJar.flatMap { it.archiveFile }
val changelogText = providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOG.md")).asText
val curseForgeToken = providers.environmentVariable("CURSEFORGE_TOKEN")
val modrinthToken = providers.environmentVariable("MODRINTH_TOKEN")

publishMods {
    file.set(outputFile)
    dryRun = curseForgeToken.isPresent.not() || modrinthToken.isPresent.not()
    version = project.version.toString()
    displayName = "${project.property("mod.name")} ${project.property("mod.version")} - Fabric ${minecraftVersion}"
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
