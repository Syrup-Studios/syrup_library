plugins {
    id("net.neoforged.moddev.legacyforge") version "2.0.143"
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
    id("maven-publish")
}

val mcVersion = property("deps.minecraft") as String
val forgeVersion = property("deps.forge_version") as String
val packFormat = (property("deps.pack_format") as String).toInt()
val targetJavaVersion = (property("deps.java_version") as String).toInt()
val json5Dependency = "de.marhali:json5-java:" + property("deps.json5")

version = "${property("mod.version")}+$mcVersion-forge"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

legacyForge {
    setVersion("$mcVersion-$forgeVersion")
    runs {
        create("client") {
            client()
            gameDirectory = project.file("run")
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
        "mc" to mcVersion,
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
    from(tasks.named("jar"), tasks.named("sourcesJar"))
    into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    dependsOn("build")
}

apply(from = rootProject.file("gradle/maven-publishing.gradle.kts"))
apply(from = rootProject.file("gradle/platform-publishing.gradle"))
