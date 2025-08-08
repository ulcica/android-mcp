plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "1.9.20"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

group = "de.cadeda"
version = "1.2.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")

    // For process execution
    implementation("org.apache.commons:commons-exec:1.3")

    // Detekt plugins for additional rules
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation(kotlin("test"))
}


tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(22)
}
// Detekt Configuration
detekt {
    toolVersion = "1.23.6"
    config.setFrom("$projectDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false

    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"
}

// Generate version file from project version
tasks.register("generateVersionFile") {
    val versionFile = file("src/main/kotlin/de/cadeda/mcp/model/AppVersion.kt")
    outputs.file(versionFile)
    doLast {
        versionFile.parentFile.mkdirs()
        versionFile.writeText("""
package de.cadeda.mcp.model

/**
 * Application version information generated from build.gradle.kts
 */
object AppVersion {
    const val VERSION = "${project.version}"
}
        """.trimIndent())
    }
}

// Make compileKotlin and detekt depend on version generation
tasks.compileKotlin {
    dependsOn("generateVersionFile")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    dependsOn("generateVersionFile")
}

tasks.jar {
    // Use version-less name for better deployment experience
    archiveFileName.set("android-mcp.jar")
    
    manifest {
        attributes["Main-Class"] = "de.cadeda.mcp.MainKt"
        attributes["Implementation-Version"] = project.version
        attributes["Implementation-Title"] = "Android MCP Server"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}