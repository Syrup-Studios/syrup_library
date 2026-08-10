import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.17.14" apply false
    id("net.fabricmc.fabric-loom") version "1.17.14" apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
    id("maven-publish")
}

val remappedMinecraft = stonecutter.eval(stonecutter.current.version, "<26")
val minecraftVersion = property("deps.minecraft") as String
val targetJavaVersion = (property("deps.java_version") as String).toInt()
val requiredJava = JavaVersion.toVersion(targetJavaVersion)

apply(plugin = if (remappedMinecraft) "net.fabricmc.fabric-loom-remap" else "net.fabricmc.fabric-loom")

version = "${property("mod.version")}+$minecraftVersion-fabric"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

val loomExtension = extensions.getByType<LoomGradleExtensionAPI>()

dependencies {
    add("minecraft", "com.mojang:minecraft:$minecraftVersion")
    if (remappedMinecraft) add("mappings", loomExtension.officialMojangMappings())
    val json5Dependency = "de.marhali:json5-java:" + property("deps.json5")
    implementation(json5Dependency)
    add("include", json5Dependency)

    val modConfiguration = if (remappedMinecraft) "modImplementation" else "implementation"
    add(modConfiguration, "net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    add(modConfiguration, "net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
}

loomExtension.apply {
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
        "mc" to minecraftVersion,
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

apply(from = rootProject.file("gradle/maven-publishing.gradle.kts"))
apply(from = rootProject.file("gradle/platform-publishing.gradle"))
