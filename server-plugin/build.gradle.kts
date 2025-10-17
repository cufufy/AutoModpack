import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.Copy

plugins {
    java
    id("com.gradleup.shadow")
}

group = providers.gradleProperty("mod.group").get()
version = providers.gradleProperty("mod_version").get()
description = "Bridge plugin that exposes AutoModpack hosting to Paper/Spigot servers"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven("https://repo1.maven.org/maven2/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.papermc.io/repository/maven-snapshots/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    implementation(project(":core"))
}

sourceSets {
    create("stubs") {
        java.setSrcDirs(listOf("src/stub/java"))
    }

    named("main") {
        compileClasspath += sourceSets["stubs"].output
    }

    named("test") {
        compileClasspath += sourceSets["stubs"].output
        runtimeClasspath += sourceSets["stubs"].output
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("compileStubsJava")
    classpath += sourceSets["stubs"].output
}

tasks.named<JavaCompile>("compileTestJava") {
    dependsOn("compileStubsJava")
    classpath += sourceSets["stubs"].output
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.withType<Test>().configureEach {
    enabled = false
}

val pluginBaseName = "AutoModpackPlugin"

tasks.withType<Jar>().configureEach {
    archiveBaseName.set(pluginBaseName)
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("plain")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    archiveBaseName.set(pluginBaseName)
    mergeServiceFiles()
}

tasks.processResources {
    val props = mapOf(
        "pluginVersion" to project.version.toString(),
        "automodpackVersion" to project.version.toString()
    )
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml", "paper-plugin.yml", "config.yml")) {
        expand(props)
    }
}

val pluginDistribution = tasks.register<Copy>("pluginDistribution") {
    dependsOn("shadowJar")
    from(tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile })
    into(layout.buildDirectory.dir("distributions/AutoModpackPlugin"))
}

tasks.named("assemble") {
    dependsOn("shadowJar", pluginDistribution)
}

tasks.named("build") {
    dependsOn(pluginDistribution)
}
